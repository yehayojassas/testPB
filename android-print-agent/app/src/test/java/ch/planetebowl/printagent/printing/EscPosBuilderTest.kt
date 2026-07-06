package ch.planetebowl.printagent.printing

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class EscPosBuilderTest {

    @Test
    fun `init then bold text then cut produces exact expected byte sequence`() {
        val bytes = EscPosBuilder()
            .init()
            .bold(true)
            .text("AB")
            .bold(false)
            .newline()
            .cutPaper()
            .build()

        val expected = EscPosCommands.INIT +
            EscPosCommands.BOLD_ON +
            byteArrayOf('A'.code.toByte(), 'B'.code.toByte()) +
            EscPosCommands.BOLD_OFF +
            EscPosCommands.LINE_FEED +
            EscPosCommands.CUT_PAPER

        assertArrayEquals(expected, bytes)
    }

    @Test
    fun `sanitized text strips accents before writing bytes`() {
        val bytes = EscPosBuilder().sanitizedText("Café").build()
        val expected = "Cafe".toByteArray(Charsets.US_ASCII)
        assertArrayEquals(expected, bytes)
    }

    @Test
    fun `cash drawer pulse command has the documented ESC p structure`() {
        val bytes = EscPosBuilder().openCashDrawer().build()
        assertArrayEquals(EscPosCommands.cashDrawerPulse(), bytes)
        // ESC 'p' m t1 t2 : 5 octets exactement.
        org.junit.Assert.assertEquals(5, bytes.size)
        org.junit.Assert.assertEquals(EscPosCommands.ESC.toByte(), bytes[0])
        org.junit.Assert.assertEquals('p'.code.toByte(), bytes[1])
    }

    @Test
    fun `align center then align left round trip`() {
        val bytes = EscPosBuilder().alignCenter().alignLeft().build()
        assertArrayEquals(EscPosCommands.ALIGN_CENTER + EscPosCommands.ALIGN_LEFT, bytes)
    }
}
