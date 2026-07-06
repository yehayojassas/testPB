package ch.planetebowl.printagent.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactingLoggingInterceptorTest {

    private val secretToken = "sk_live_super_secret_token_98765"

    @Test
    fun `bearer token is never present in a redacted log line`() {
        val logLine = "Authorization: Bearer $secretToken"
        val redacted = RedactingLoggingInterceptor.redact(logLine)

        assertFalse(redacted.contains(secretToken))
        assertTrue(redacted.contains("Bearer REDACTED"))
    }

    @Test
    fun `redaction is case sensitive on the Bearer keyword but leaves the rest of the line intact`() {
        val logLine = "--> POST /jobs/123/claim\nAuthorization: Bearer $secretToken\nIdempotency-Key: 123:1"
        val redacted = RedactingLoggingInterceptor.redact(logLine)

        assertFalse(redacted.contains(secretToken))
        assertTrue(redacted.contains("--> POST /jobs/123/claim"))
        assertTrue(redacted.contains("Idempotency-Key: 123:1"))
    }

    @Test
    fun `lines without a bearer token are left unchanged`() {
        val logLine = "--> GET /jobs?restaurantId=abc&limit=20"
        assertTrue(RedactingLoggingInterceptor.redact(logLine) == logLine)
    }
}
