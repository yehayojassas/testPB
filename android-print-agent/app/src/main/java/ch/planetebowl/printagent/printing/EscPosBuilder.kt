package ch.planetebowl.printagent.printing

import ch.planetebowl.printagent.common.TextSanitizer
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Accumulateur d'octets ESC/POS. Tout texte passe par TextSanitizer.toAsciiPrintable avant
 * d'etre encode : l'imprimante generique visee n'a pas de table de caracteres accentues
 * garantie, on reste donc en ASCII pur plutot que de risquer des caracteres corrompus.
 */
class EscPosBuilder {
    private val buffer = ByteArrayOutputStream()

    fun raw(bytes: ByteArray): EscPosBuilder = apply { buffer.write(bytes) }

    fun init(): EscPosBuilder = raw(EscPosCommands.INIT)

    fun alignCenter(): EscPosBuilder = raw(EscPosCommands.ALIGN_CENTER)
    fun alignLeft(): EscPosBuilder = raw(EscPosCommands.ALIGN_LEFT)

    fun bold(enabled: Boolean): EscPosBuilder = raw(if (enabled) EscPosCommands.BOLD_ON else EscPosCommands.BOLD_OFF)
    fun doubleSize(enabled: Boolean): EscPosBuilder =
        raw(if (enabled) EscPosCommands.DOUBLE_SIZE_ON else EscPosCommands.DOUBLE_SIZE_OFF)

    /** Ecrit [text] tel quel (deja sanitized par l'appelant), sans saut de ligne final. */
    fun text(text: String): EscPosBuilder = apply {
        buffer.write(text.toByteArray(StandardCharsets.US_ASCII))
    }

    /** Sanitize puis ecrit [text], sans saut de ligne final. */
    fun sanitizedText(text: String): EscPosBuilder = text(TextSanitizer.toAsciiPrintable(text))

    fun newline(count: Int = 1): EscPosBuilder = apply {
        repeat(count.coerceAtLeast(0)) { buffer.write(EscPosCommands.LINE_FEED) }
    }

    /** Sanitize, decoupe en lignes de [width] colonnes et ecrit chaque ligne suivie d'un saut. */
    fun sanitizedWrappedLine(text: String, width: Int): EscPosBuilder = apply {
        val sanitized = TextSanitizer.toAsciiPrintable(text)
        val lines = TextSanitizer.wordWrap(sanitized, width)
        if (lines.isEmpty()) {
            newline()
        } else {
            lines.forEach { line -> text(line).newline() }
        }
    }

    fun cutPaper(): EscPosBuilder = raw(EscPosCommands.CUT_PAPER)

    fun openCashDrawer(): EscPosBuilder = raw(EscPosCommands.cashDrawerPulse())

    fun build(): ByteArray = buffer.toByteArray()
}
