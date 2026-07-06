package ch.planetebowl.printagent.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class PrintLogEventType {
    POLL, CLAIM, PRINT_ATTEMPT, PRINT_SUCCESS, PRINT_FAILURE, ACK_SUCCESS, ACK_FAILURE, MANUAL_REVIEW
}

enum class PrintLogResult { SUCCESS, FAILURE, INFO }

/**
 * Journal d'audit local, consultable pour le diagnostic sans jamais exposer le token
 * (message toujours passe par TextSanitizer.sanitizeForLog avant insertion).
 */
@Entity(tableName = "print_logs", indices = [Index(value = ["orderId"]), Index(value = ["timestamp"])])
data class PrintLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,
    val eventType: PrintLogEventType,
    val result: PrintLogResult,
    val message: String,
    val timestamp: Instant,
)
