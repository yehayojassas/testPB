package ch.planetebowl.printagent.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ch.planetebowl.printagent.domain.model.PrintStatus
import java.time.Instant

/**
 * Ligne de la file d'impression locale. `payloadJson` conserve le JSON brut recu de
 * GET /jobs pour cette commande : meme si le mapping domain (JobMapper) ignore un champ,
 * rien n'est perdu et un futur ticket "duplique" reste reconstructible a l'identique.
 */
@Entity(
    tableName = "print_jobs",
    indices = [Index(value = ["orderId"], unique = true), Index(value = ["status"])],
)
data class PrintJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,
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
