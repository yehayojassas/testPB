package ch.planetebowl.printagent.printing

import ch.planetebowl.printagent.common.TextSanitizer
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.OrderItem
import ch.planetebowl.printagent.domain.model.PrinterSettings
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Construit le flux d'octets ESC/POS complet d'un ticket a partir d'une [Order] et des
 * [PrinterSettings] courants (largeur de colonnes, coupe papier, tiroir-caisse).
 */
class TicketFormatter {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun format(order: Order, settings: PrinterSettings): ByteArray {
        val width = settings.ticketWidth.columns
        val builder = EscPosBuilder().init()

        appendHeader(builder, settings.storeName, width)
        appendOrderMeta(builder, order, width)
        appendSeparator(builder, width)
        appendItems(builder, order.items, width)
        appendSeparator(builder, width)
        appendTotal(builder, order, width)
        appendKitchenNote(builder, order.kitchenNote, width)
        appendFooter(builder)

        if (settings.cutPaperEnabled) builder.cutPaper()
        if (settings.cashDrawerEnabled) builder.openCashDrawer()

        return builder.build()
    }

    private fun appendHeader(builder: EscPosBuilder, storeName: String, width: Int) {
        builder.alignCenter()
            .doubleSize(true)
            .bold(true)
            .sanitizedWrappedLine(storeName, (width / 2).coerceAtLeast(1))
            .bold(false)
            .doubleSize(false)
            .newline()
        builder.alignLeft()
    }

    private fun appendOrderMeta(builder: EscPosBuilder, order: Order, width: Int) {
        val localDateTime = order.createdAt.atZone(ZoneId.systemDefault())
        builder.bold(true).sanitizedWrappedLine("Commande ${order.orderCode}", width).bold(false)
        builder.sanitizedWrappedLine(dateFormatter.format(localDateTime), width)
        builder.sanitizedWrappedLine(orderTypeLabel(order), width)
        order.pickupSlot?.takeIf { it.isNotBlank() }?.let {
            builder.sanitizedWrappedLine("Heure souhaitée : $it", width)
        }
        builder.sanitizedWrappedLine("Client : ${order.customer.name}", width)
        order.customer.phone?.takeIf { it.isNotBlank() }?.let {
            builder.sanitizedWrappedLine("Tél : $it", width)
        }
        // Adresse prevue par le contrat (livraison future) mais non exploitee : orderType
        // ne vaut aujourd'hui que TAKEAWAY cote backend, voir OrderType.
        order.address?.rawText?.takeIf { it.isNotBlank() }?.let {
            builder.sanitizedWrappedLine("Adresse : $it", width)
        }
    }

    private fun orderTypeLabel(order: Order): String = when (order.orderType) {
        ch.planetebowl.printagent.domain.model.OrderType.TAKEAWAY -> "À EMPORTER"
        ch.planetebowl.printagent.domain.model.OrderType.DINE_IN -> "SUR PLACE"
        ch.planetebowl.printagent.domain.model.OrderType.DELIVERY -> "LIVRAISON"
        ch.planetebowl.printagent.domain.model.OrderType.UNKNOWN -> "COMMANDE"
    }

    private fun appendItems(builder: EscPosBuilder, items: List<OrderItem>, width: Int) {
        items.forEach { item ->
            appendPricedLine(
                builder = builder,
                left = "${item.quantity}x ${TextSanitizer.toAsciiPrintable(item.name)}",
                right = item.lineTotal.formatChf(),
                width = width,
            )
            item.customizations.groups.forEach { group ->
                val line = "  - ${group.label}: ${group.values.joinToString(", ")}"
                builder.sanitizedWrappedLine(line, width)
            }
        }
    }

    private fun appendTotal(builder: EscPosBuilder, order: Order, width: Int) {
        builder.bold(true).doubleSize(true)
        appendPricedLine(builder, "TOTAL", order.total.formatChf(), width / 2, forceSanitize = false)
        builder.doubleSize(false).bold(false)
    }

    private fun appendKitchenNote(builder: EscPosBuilder, kitchenNote: String?, width: Int) {
        if (kitchenNote.isNullOrBlank()) return
        builder.newline()
        builder.bold(true).sanitizedWrappedLine("Note cuisine :", width).bold(false)
        builder.sanitizedWrappedLine(kitchenNote, width)
    }

    private fun appendFooter(builder: EscPosBuilder) {
        builder.newline(3)
    }

    private fun appendSeparator(builder: EscPosBuilder, width: Int) {
        builder.sanitizedWrappedLine("-".repeat(width), width)
    }

    /**
     * Aligne [left] et [right] sur une meme ligne de [width] colonnes ; si la combinaison
     * ne tient pas, [right] passe sur sa propre ligne, alignee a droite.
     */
    private fun appendPricedLine(
        builder: EscPosBuilder,
        left: String,
        right: String,
        width: Int,
        forceSanitize: Boolean = true,
    ) {
        val sanitizedLeft = if (forceSanitize) TextSanitizer.toAsciiPrintable(left) else left
        val sanitizedRight = if (forceSanitize) TextSanitizer.toAsciiPrintable(right) else right
        val gap = width - sanitizedLeft.length - sanitizedRight.length
        if (gap >= 1) {
            builder.text(sanitizedLeft).text(" ".repeat(gap)).text(sanitizedRight).newline()
        } else {
            builder.sanitizedWrappedLine(sanitizedLeft, width)
            val padding = (width - sanitizedRight.length).coerceAtLeast(0)
            builder.text(" ".repeat(padding)).text(sanitizedRight).newline()
        }
    }
}
