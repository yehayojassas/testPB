package ch.planetebowl.printagent.common

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Politique de backoff exponentiel avec jitter, partagee par les retries d'impression
 * (attemptCount sur PrintJobEntity) et les retries d'accuse de reception (POST /printed).
 * Base 5s, doublement jusqu'a un plafond de 120s, +/- 20% de bruit pour eviter que toutes
 * les tablettes d'un meme reseau ne re-tentent exactement au meme instant.
 */
object BackoffPolicy {

    const val BASE_DELAY_MS = 5_000L
    const val MAX_DELAY_MS = 120_000L
    const val MAX_PRINT_ATTEMPTS = 5
    private const val JITTER_RATIO = 0.20

    /** [attemptNumber] demarre a 1 pour la premiere tentative echouee. */
    fun nextDelayMillis(attemptNumber: Int, random: Random = Random.Default): Long {
        require(attemptNumber >= 1) { "attemptNumber must be >= 1" }
        val exponent = (attemptNumber - 1).coerceAtMost(10)
        val rawDelay = BASE_DELAY_MS * 2.0.pow(exponent)
        val capped = min(rawDelay, MAX_DELAY_MS.toDouble())
        val jitterSpan = capped * JITTER_RATIO
        val jitter = random.nextDouble(-jitterSpan, jitterSpan)
        return (capped + jitter).toLong().coerceIn(0L, MAX_DELAY_MS + (MAX_DELAY_MS * JITTER_RATIO).toLong())
    }

    fun nextRetryAtEpochMillis(attemptNumber: Int, nowEpochMillis: Long, random: Random = Random.Default): Long =
        nowEpochMillis + nextDelayMillis(attemptNumber, random)
}
