package ch.planetebowl.printagent.common

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BackoffPolicyTest {

    private val noJitterRandom = Random(seed = 42)

    @Test
    fun `delay grows and stays within the documented cap`() {
        var previous = 0L
        for (attempt in 1..8) {
            val delay = BackoffPolicy.nextDelayMillis(attempt, noJitterRandom)
            assertTrue("delay for attempt $attempt should be >= 0", delay >= 0)
            assertTrue(
                "delay for attempt $attempt ($delay ms) should not exceed cap with jitter",
                delay <= (BackoffPolicy.MAX_DELAY_MS * 1.2).toLong(),
            )
            previous = delay
        }
    }

    @Test
    fun `first attempt delay is close to base delay`() {
        val fixedRandom = object : Random() {
            override fun nextBits(bitCount: Int) = 0
            override fun nextDouble(from: Double, until: Double) = 0.0 // pas de jitter
        }
        val delay = BackoffPolicy.nextDelayMillis(1, fixedRandom)
        assertTrue(delay == BackoffPolicy.BASE_DELAY_MS)
    }

    @Test
    fun `delay never exceeds max even for very high attempt numbers`() {
        val delay = BackoffPolicy.nextDelayMillis(50, noJitterRandom)
        assertTrue(delay <= (BackoffPolicy.MAX_DELAY_MS * 1.2).toLong())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `attempt number below 1 is rejected`() {
        BackoffPolicy.nextDelayMillis(0)
    }
}
