package ch.planetebowl.printagent.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSanitizerTest {

    @Test
    fun `removes accents and diacritics`() {
        val result = TextSanitizer.toAsciiPrintable("Café Théâtre — Crème brûlée")
        assertEquals("Cafe Theatre  Creme brulee", result)
    }

    @Test
    fun `filters unsupported unicode characters like emoji`() {
        val result = TextSanitizer.toAsciiPrintable("Commande 😀 prête ✅")
        assertTrue(result.none { it.code !in 0x20..0x7E })
        assertEquals("Commande  prete ", result)
    }

    @Test
    fun `word wrap respects configured width`() {
        val lines = TextSanitizer.wordWrap("Bowl Signature Teriyaki avec supplement avocat", 20)
        assertTrue(lines.all { it.length <= 20 })
        assertEquals("Bowl Signature", lines.first())
    }

    @Test
    fun `word wrap breaks a single word longer than width`() {
        val lines = TextSanitizer.wordWrap("Supercalifragilisticexpialidocious", 10)
        assertTrue(lines.all { it.length <= 10 })
        assertEquals(4, lines.size)
    }

    @Test
    fun `sanitize for log truncates long messages`() {
        val longMessage = "x".repeat(1000)
        val result = TextSanitizer.sanitizeForLog(longMessage)
        assertTrue(result.length <= 501)
    }

    @Test
    fun `sanitize for log collapses whitespace and handles blank`() {
        assertEquals("", TextSanitizer.sanitizeForLog(null))
        assertEquals("", TextSanitizer.sanitizeForLog("   "))
        assertEquals("a b c", TextSanitizer.sanitizeForLog("a\n  b\tc"))
    }
}
