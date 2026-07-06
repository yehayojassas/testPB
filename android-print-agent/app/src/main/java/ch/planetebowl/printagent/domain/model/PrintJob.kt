package ch.planetebowl.printagent.domain.model

import java.time.Instant

/**
 * Modele domain d'une ligne de la file d'impression locale. Miroir de PrintJobEntity mais
 * sans dependance Room, avec [order] deja deserialise depuis [payloadJson] pour que les
 * use cases n'aient jamais a parser du JSON eux-memes.
 */
data class PrintJob(
    val id: Long,
    val orderId: String,
    val order: Order,
    val payloadJson: String,
    val status: PrintStatus,
    val attemptCount: Int,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
    val receivedAt: Instant,
    val nextRetryAt: Instant?,
    val printedAt: Instant?,
    val acknowledgedAt: Instant?,
    val claimToken: String?,
    val claimExpiresAt: Instant?,
)
