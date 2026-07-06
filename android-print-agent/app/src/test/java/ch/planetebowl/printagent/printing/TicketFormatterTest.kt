package ch.planetebowl.printagent.printing

import ch.planetebowl.printagent.domain.model.Customer
import ch.planetebowl.printagent.domain.model.CustomizationGroup
import ch.planetebowl.printagent.domain.model.Customizations
import ch.planetebowl.printagent.domain.model.DeliveryAddress
import ch.planetebowl.printagent.domain.model.Money
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.OrderItem
import ch.planetebowl.printagent.domain.model.OrderType
import ch.planetebowl.printagent.domain.model.PrinterSettings
import ch.planetebowl.printagent.domain.model.TicketWidth
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class TicketFormatterTest {

    private val formatter = TicketFormatter()

    private fun sampleOrder(): Order = Order(
        orderId = "order-1",
        orderCode = "PB-101",
        payloadVersion = 1,
        createdAt = Instant.parse("2026-07-06T11:32:00Z"),
        orderType = OrderType.TAKEAWAY,
        pickupSlot = "12:30",
        customer = Customer(name = "Jean Dupont", phone = "+41791234567"),
        address = DeliveryAddress("Rue de la Gare 1"),
        items = listOf(
            OrderItem(
                name = "Bowl Signature Teriyaki",
                quantity = 2,
                unitPrice = Money(BigDecimal("14.90")),
                customizations = Customizations(
                    listOf(
                        CustomizationGroup("Base", listOf("Riz")),
                        CustomizationGroup("Protéine", listOf("Poulet")),
                        CustomizationGroup("Sauce", listOf("Soja")),
                    ),
                ),
            ),
        ),
        total = Money(BigDecimal("29.80")),
        kitchenNote = "Sans coriandre",
        restaurantId = "restaurant-1",
    )

    private fun settings(width: TicketWidth) = PrinterSettings.defaults().copy(
        storeName = "PLANÈTE BOWL",
        ticketWidth = width,
        cutPaperEnabled = true,
        cashDrawerEnabled = true,
    )

    private fun decode(bytes: ByteArray): String = String(bytes, Charsets.ISO_8859_1)

    @Test
    fun `complete ticket contains every expected section`() {
        val text = decode(formatter.format(sampleOrder(), settings(TicketWidth.WIDE_48)))

        assertTrue(text.contains("PLANETE BOWL")) // accents supprimés au format ASCII
        assertTrue(text.contains("PB-101"))
        assertTrue(text.contains("A EMPORTER"))
        assertTrue(text.contains("Jean Dupont"))
        assertTrue(text.contains("+41791234567"))
        assertTrue(text.contains("12:30"))
        assertTrue(text.contains("Bowl Signature Teriyaki"))
        assertTrue(text.contains("Base: Riz"))
        assertTrue(text.contains("Sauce: Soja"))
        assertTrue(text.contains("CHF 29.80"))
        assertTrue(text.contains("Sans coriandre"))
    }

    @Test
    fun `cut paper and cash drawer commands are appended when enabled`() {
        val bytes = formatter.format(sampleOrder(), settings(TicketWidth.WIDE_48))
        val text = decode(bytes)
        assertTrue(text.endsWith(decode(EscPosCommands.cashDrawerPulse())))
        assertTrue(decode(bytes.dropLast(EscPosCommands.cashDrawerPulse().size).toByteArray()).endsWith(decode(EscPosCommands.CUT_PAPER)))
    }

    @Test
    fun `narrower width produces shorter separator line than wider width`() {
        val text42 = decode(formatter.format(sampleOrder(), settings(TicketWidth.NARROW_42)))
        val text48 = decode(formatter.format(sampleOrder(), settings(TicketWidth.WIDE_48)))

        assertTrue(text42.contains("-".repeat(42)))
        assertTrue(!text42.contains("-".repeat(43)))
        assertTrue(text48.contains("-".repeat(48)))
        assertTrue(!text48.contains("-".repeat(49)))
    }

    @Test
    fun `price line right aligns within configured width`() {
        val width = TicketWidth.WIDE_48.columns
        val text = decode(formatter.format(sampleOrder(), settings(TicketWidth.WIDE_48)))
        val itemLine = text.lines().first { it.contains("Bowl Signature Teriyaki") }
        assertTrue(itemLine.length <= width)
        assertTrue(itemLine.trimEnd().endsWith("29.80"))
    }
}
