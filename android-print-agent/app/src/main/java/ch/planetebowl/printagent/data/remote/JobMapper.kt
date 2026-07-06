package ch.planetebowl.printagent.data.remote

import ch.planetebowl.printagent.data.remote.dto.CustomizationsDto
import ch.planetebowl.printagent.data.remote.dto.JobDto
import ch.planetebowl.printagent.domain.model.Customer
import ch.planetebowl.printagent.domain.model.CustomizationGroup
import ch.planetebowl.printagent.domain.model.Customizations
import ch.planetebowl.printagent.domain.model.DeliveryAddress
import ch.planetebowl.printagent.domain.model.Money
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.OrderItem
import ch.planetebowl.printagent.domain.model.OrderType
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Mapping DTO (JSON du contrat) -> modele domain. Isole ici pour que ni le domain ni l'UI
 * n'aient a connaitre la forme JSON exacte (SerializedName, structure des customizations...).
 */
object JobMapper {

    fun toDomain(dto: JobDto): Order = Order(
        orderId = dto.orderId,
        orderCode = dto.orderCode,
        payloadVersion = dto.payloadVersion,
        createdAt = parseInstantOrEpoch(dto.createdAt),
        orderType = OrderType.fromApiValue(dto.orderType),
        pickupSlot = dto.pickupSlot,
        customer = Customer(name = dto.customerName, phone = dto.phone),
        address = dto.address?.takeIf { it.isNotBlank() }?.let { DeliveryAddress(it) },
        items = dto.items.map { itemDto ->
            OrderItem(
                name = itemDto.name,
                quantity = itemDto.quantity,
                unitPrice = Money.fromApiString(itemDto.unitPriceChf),
                customizations = itemDto.customizations?.let(::toCustomizations) ?: Customizations.EMPTY,
            )
        },
        total = Money.fromApiString(dto.totalChf),
        kitchenNote = dto.kitchenNote,
        restaurantId = dto.restaurantId,
    )

    private fun toCustomizations(dto: CustomizationsDto): Customizations {
        val groups = buildList {
            dto.base?.takeIf { it.isNotBlank() }?.let { add(CustomizationGroup("Base", listOf(it))) }
            dto.protein?.takeIf { it.isNotBlank() }?.let { add(CustomizationGroup("Protéine", listOf(it))) }
            dto.garnish?.takeIf { it.isNotEmpty() }?.let { add(CustomizationGroup("Garniture", it)) }
            dto.extraProtein?.takeIf { it.isNotEmpty() }?.let { add(CustomizationGroup("Supplément protéine", it)) }
            dto.topping?.takeIf { it.isNotEmpty() }?.let { add(CustomizationGroup("Topping", it)) }
            dto.sauce?.takeIf { it.isNotEmpty() }?.let { add(CustomizationGroup("Sauce", it)) }
        }
        return Customizations(groups)
    }

    /** Le contrat garantit un ISO8601 valide ; on ne fait jamais planter le mapping pour
     * autant — une date de reception epoch(0) reste un signal visible plutot qu'un crash
     * du poll qui bloquerait toute la file. */
    private fun parseInstantOrEpoch(raw: String): Instant = try {
        Instant.parse(raw)
    } catch (e: DateTimeParseException) {
        Instant.EPOCH
    }
}
