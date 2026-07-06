package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.domain.repository.QueueRepository
import java.time.Instant
import javax.inject.Inject

/** Selectionne le prochain job PENDING/RETRY_WAIT eligible et tente de le reclamer. */
class ClaimJobUseCase @Inject constructor(
    private val queueRepository: QueueRepository,
) {
    suspend operator fun invoke(now: Instant = Instant.now()): QueueRepository.ClaimOutcome? {
        val nextJob = queueRepository.findNextClaimable(now) ?: return null
        return queueRepository.claimJob(nextJob)
    }
}
