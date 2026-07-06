package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.common.AppResult
import ch.planetebowl.printagent.common.asSuccess
import ch.planetebowl.printagent.domain.model.ApiError
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.PrintJob
import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.model.PrintStatus
import ch.planetebowl.printagent.domain.repository.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/** Double de test en memoire pour QueueRepository — pas de Room, pas de reseau, juste de
 * quoi verifier quelles transitions ont ete demandees par les use cases. */
class FakeQueueRepository(private val order: Order) : QueueRepository {

    var recordPrintSuccessCalls = 0
        private set
    var recordPrintFailureCalls = 0
        private set
    var confirmPrintedCalls = 0
        private set
    var confirmPrintedResult: AppResult<Unit> = Unit.asSuccess()
    private val jobsFlow = MutableStateFlow<List<PrintJob>>(emptyList())

    private var lastKnownJob: PrintJob? = null

    override suspend fun pollAndEnqueueJobs(restaurantId: String, limit: Int): AppResult<Int> = 0.asSuccess()

    override suspend fun findNextClaimable(now: Instant): PrintJob? = null

    override suspend fun claimJob(job: PrintJob): QueueRepository.ClaimOutcome =
        QueueRepository.ClaimOutcome.Claimed(job)

    override suspend fun recordPrintFailure(job: PrintJob, printResult: PrintResult) {
        recordPrintFailureCalls++
        lastKnownJob = job.copy(status = PrintStatus.RETRY_WAIT, attemptCount = job.attemptCount + 1)
    }

    override suspend fun recordPrintSuccess(job: PrintJob, printedAt: Instant) {
        recordPrintSuccessCalls++
        lastKnownJob = job.copy(status = PrintStatus.PRINTED_PENDING_ACK, printedAt = printedAt)
    }

    override suspend fun confirmPrinted(job: PrintJob): AppResult<Unit> {
        confirmPrintedCalls++
        return confirmPrintedResult
    }

    override suspend fun findAllPendingAck(): List<PrintJob> = lastKnownJob
        ?.takeIf { it.status == PrintStatus.PRINTED_PENDING_ACK }
        ?.let { listOf(it) }
        ?: emptyList()

    override fun observeRecentJobs(limit: Int): StateFlow<List<PrintJob>> = jobsFlow

    override suspend fun failAllStuckInPrinting(): Int = 0

    override suspend fun deserializeOrder(payloadJson: String): Order = order

    fun lastKnownStatus(): PrintStatus? = lastKnownJob?.status
}
