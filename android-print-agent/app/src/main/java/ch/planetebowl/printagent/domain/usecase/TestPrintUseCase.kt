package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.domain.model.Customer
import ch.planetebowl.printagent.domain.model.Customizations
import ch.planetebowl.printagent.domain.model.Money
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.OrderItem
import ch.planetebowl.printagent.domain.model.OrderType
import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.model.PrinterSettings
import ch.planetebowl.printagent.domain.repository.PrinterClient
import ch.planetebowl.printagent.printing.TicketFormatter
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

/** Bouton "Tester l'impression" : imprime un ticket factice complet (avec coupe/tiroir
 * selon les reglages actuels) pour valider bout en bout le formatage et le materiel. */
class TestPrintUseCase @Inject constructor(
    private val printerClient: PrinterClient,
    private val ticketFormatter: TicketFormatter,
) {
    suspend operator fun invoke(settings: PrinterSettings): PrintResult {
        val ticketBytes = ticketFormatter.format(sampleOrder(settings.restaurantId), settings)
        return printerClient.send(settings.printerIp, settings.printerPort, ticketBytes)
    }

    private fun sampleOrder(restaurantId: String): Order = Order(
        orderId = "test-print",
        orderCode = "TEST-000",
        payloadVersion = 1,
        createdAt = Instant.now(),
        orderType = OrderType.TAKEAWAY,
        pickupSlot = "12:00",
        customer = Customer(name = "Client de test", phone = "+41 00 000 00 00"),
        address = null,
        items = listOf(
            OrderItem(
                name = "Ticket de test",
                quantity = 1,
                unitPrice = Money(BigDecimal("0.00")),
                customizations = Customizations.EMPTY,
            ),
        ),
        total = Money(BigDecimal("0.00")),
        kitchenNote = "Ceci est un test d'impression.",
        restaurantId = restaurantId.ifBlank { "test-restaurant" },
    )
}
