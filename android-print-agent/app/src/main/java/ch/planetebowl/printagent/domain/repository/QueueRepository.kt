package ch.planetebowl.printagent.domain.repository

import ch.planetebowl.printagent.common.AppResult
import ch.planetebowl.printagent.domain.model.ApiError
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.PrintJob
import ch.planetebowl.printagent.domain.model.PrintResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Frontiere entre les use cases et la persistence locale + le backend. Implementee par
 * QueueRepositoryImpl (data/repository), qui orchestre Room + BackendApi.
 */
interface QueueRepository {

    /** GET /jobs puis insertion locale (IGNORE sur orderId deja connu). Retourne le nombre
     * de jobs reellement nouveaux inseres. */
    suspend fun pollAndEnqueueJobs(restaurantId: String, limit: Int = 20): AppResult<Int>

    suspend fun findNextClaimable(now: Instant = Instant.now()): PrintJob?

    sealed class ClaimOutcome {
        data class Claimed(val job: PrintJob) : ClaimOutcome()
        data class RejectedTerminal(val apiError: ApiError) : ClaimOutcome()
        data class Failed(val reason: String) : ClaimOutcome()
    }

    /** POST /claim puis persistence locale : status=PRINTING AVANT tout envoi TCP (voir
     * machine a etats), ou transition terminale locale si 409. */
    suspend fun claimJob(job: PrintJob): ClaimOutcome

    /** Persiste le resultat d'un envoi TCP echoue : incremente attemptCount, calcule
     * RETRY_WAIT ou FAILED_MANUAL_REVIEW selon BackoffPolicy.MAX_PRINT_ATTEMPTS, et
     * declenche POST /failed pour liberer le claim serveur. */
    suspend fun recordPrintFailure(job: PrintJob, printResult: PrintResult)

    /** Marque PRINTED_PENDING_ACK localement (le ticket est physiquement parti). */
    suspend fun recordPrintSuccess(job: PrintJob, printedAt: Instant = Instant.now())

    /** POST /printed. En cas d'echec reseau/5xx, le job RESTE PRINTED_PENDING_ACK — jamais
     * de nouvel envoi TCP declenche depuis ce chemin. */
    suspend fun confirmPrinted(job: PrintJob): AppResult<Unit>

    suspend fun findAllPendingAck(): List<PrintJob>

    fun observeRecentJobs(limit: Int = 50): Flow<List<PrintJob>>

    /** Sweep de demarrage : toute ligne PRINTING -> FAILED_MANUAL_REVIEW (voir
     * PrinterForegroundService pour la justification de ce choix conservateur). */
    suspend fun failAllStuckInPrinting(): Int

    suspend fun deserializeOrder(payloadJson: String): Order
}
