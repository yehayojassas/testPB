package ch.planetebowl.printagent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.planetebowl.printagent.data.local.entity.PrintJobEntity
import ch.planetebowl.printagent.domain.model.PrintStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface PrintJobDao {

    /**
     * IGNORE sur la contrainte unique(orderId) : un job deja recu via un poll precedent
     * ne doit jamais etre reinsere/duplique, c'est l'implementation de la transition 1
     * (nouveau job recu -> insert Room, ignore si orderId deja present).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(entity: PrintJobEntity): Long

    @Update
    suspend fun update(entity: PrintJobEntity)

    @Query("SELECT * FROM print_jobs WHERE orderId = :orderId LIMIT 1")
    suspend fun findByOrderId(orderId: String): PrintJobEntity?

    @Query("SELECT * FROM print_jobs WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PrintJobEntity?

    @Query(
        """
        SELECT * FROM print_jobs
        WHERE status = :pending
           OR (status = :retryWait AND (nextRetryAt IS NULL OR nextRetryAt <= :now))
        ORDER BY receivedAt ASC
        LIMIT 1
        """
    )
    suspend fun findNextClaimable(
        now: Instant,
        pending: PrintStatus = PrintStatus.PENDING,
        retryWait: PrintStatus = PrintStatus.RETRY_WAIT,
    ): PrintJobEntity?

    @Query("SELECT * FROM print_jobs WHERE status = :status ORDER BY receivedAt ASC")
    suspend fun findAllByStatus(status: PrintStatus): List<PrintJobEntity>

    @Query("SELECT * FROM print_jobs WHERE status = :status ORDER BY receivedAt ASC")
    fun observeByStatus(status: PrintStatus): Flow<List<PrintJobEntity>>

    @Query("SELECT * FROM print_jobs ORDER BY receivedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<PrintJobEntity>>

    /** Sweep de demarrage : voir PrinterForegroundService pour le commentaire sur la
     * limite assumee (pas de retry automatique apres un crash pendant l'impression). */
    @Query("UPDATE print_jobs SET status = :failedManualReview WHERE status = :printing")
    suspend fun failAllStuckInPrinting(
        printing: PrintStatus = PrintStatus.PRINTING,
        failedManualReview: PrintStatus = PrintStatus.FAILED_MANUAL_REVIEW,
    ): Int

    /**
     * nextRetryAt est reutilise pour l'ack (pas de colonne dediee dans le schema) : NULL
     * juste apres l'impression (premiere tentative d'accuse immediate), puis repousse par
     * BackoffPolicy a chaque echec d'accuse. Voir QueueRepositoryImpl.confirmPrinted.
     */
    @Query(
        """
        SELECT * FROM print_jobs
        WHERE status = :status AND (nextRetryAt IS NULL OR nextRetryAt <= :now)
        ORDER BY receivedAt ASC
        """
    )
    suspend fun findPendingAckReady(now: Instant, status: PrintStatus = PrintStatus.PRINTED_PENDING_ACK): List<PrintJobEntity>
}
