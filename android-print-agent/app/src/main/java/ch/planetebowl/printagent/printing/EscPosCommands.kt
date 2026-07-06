package ch.planetebowl.printagent.printing

/**
 * Sous-ensemble ESC/POS generique (compatible Epson TM-x et la plupart des clones) utilise
 * par EscPosBuilder. Pas de commande proprietaire Epson avancee : uniquement ce qui est
 * documente comme standard sur a peu pres tout controleur ESC/POS.
 */
object EscPosCommands {
    const val ESC: Int = 0x1B
    const val GS: Int = 0x1D

    val INIT = byteArrayOf(ESC.toByte(), '@'.code.toByte())

    val ALIGN_LEFT = byteArrayOf(ESC.toByte(), 'a'.code.toByte(), 0x00)
    val ALIGN_CENTER = byteArrayOf(ESC.toByte(), 'a'.code.toByte(), 0x01)
    val ALIGN_RIGHT = byteArrayOf(ESC.toByte(), 'a'.code.toByte(), 0x02)

    val BOLD_ON = byteArrayOf(ESC.toByte(), 'E'.code.toByte(), 0x01)
    val BOLD_OFF = byteArrayOf(ESC.toByte(), 'E'.code.toByte(), 0x00)

    /** GS ! n : n=0x11 -> double largeur + double hauteur. n=0x00 -> taille normale. */
    val DOUBLE_SIZE_ON = byteArrayOf(GS.toByte(), '!'.code.toByte(), 0x11)
    val DOUBLE_SIZE_OFF = byteArrayOf(GS.toByte(), '!'.code.toByte(), 0x00)

    val LINE_FEED = byteArrayOf(0x0A)

    /** GS V 1 : coupe partielle (feed + cut), plus tolerante materiellement que la coupe totale. */
    val CUT_PAPER = byteArrayOf(GS.toByte(), 'V'.code.toByte(), 0x01)

    /** ESC p m t1 t2 : impulsion tiroir-caisse generique sur le connecteur RJ11 de
     * l'imprimante. m=0 (broche 2), t1/t2 = duree on/off en unites de 2ms. */
    fun cashDrawerPulse(pin: Int = 0, onDurationUnits: Int = 25, offDurationUnits: Int = 250): ByteArray =
        byteArrayOf(ESC.toByte(), 'p'.code.toByte(), pin.toByte(), onDurationUnits.toByte(), offDurationUnits.toByte())
}
