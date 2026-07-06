package ch.planetebowl.printagent.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `valid ipv4 addresses are accepted`() {
        assertTrue(Validators.isValidIpv4("192.168.1.50"))
        assertTrue(Validators.isValidIpv4("0.0.0.0"))
        assertTrue(Validators.isValidIpv4("255.255.255.255"))
    }

    @Test
    fun `invalid ipv4 addresses are rejected`() {
        assertFalse(Validators.isValidIpv4("256.1.1.1"))
        assertFalse(Validators.isValidIpv4("192.168.1"))
        assertFalse(Validators.isValidIpv4("192.168.1.1.1"))
        assertFalse(Validators.isValidIpv4("not-an-ip"))
        assertFalse(Validators.isValidIpv4(""))
    }

    @Test
    fun `port range validation`() {
        assertTrue(Validators.isValidPort(1))
        assertTrue(Validators.isValidPort(9100))
        assertTrue(Validators.isValidPort(65535))
        assertFalse(Validators.isValidPort(0))
        assertFalse(Validators.isValidPort(65536))
        assertFalse(Validators.isValidPort(-1))
    }

    @Test
    fun `uuid validation`() {
        assertTrue(Validators.isValidUuid("123e4567-e89b-12d3-a456-426614174000"))
        assertFalse(Validators.isValidUuid("not-a-uuid"))
        assertFalse(Validators.isValidUuid(""))
    }

    @Test
    fun `https url is always valid`() {
        val result = Validators.validateApiBaseUrl("https://xxxx.supabase.co/functions/v1/print-agent", developerModeEnabled = false)
        assertTrue(result is Validators.ValidationResult.Valid)
    }

    @Test
    fun `http url is rejected outside developer mode`() {
        val result = Validators.validateApiBaseUrl("http://192.168.1.10:8080", developerModeEnabled = false)
        assertTrue(result is Validators.ValidationResult.Invalid)
    }

    @Test
    fun `http url is accepted in developer mode`() {
        val result = Validators.validateApiBaseUrl("http://192.168.1.10:8080", developerModeEnabled = true)
        assertTrue(result is Validators.ValidationResult.Valid)
    }

    @Test
    fun `blank or malformed url is always invalid`() {
        assertTrue(Validators.validateApiBaseUrl("", developerModeEnabled = true) is Validators.ValidationResult.Invalid)
        assertTrue(Validators.validateApiBaseUrl("ftp://example.com", developerModeEnabled = true) is Validators.ValidationResult.Invalid)
    }
}
