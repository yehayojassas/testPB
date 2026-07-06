package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.common.AppResult
import ch.planetebowl.printagent.domain.repository.QueueRepository
import javax.inject.Inject

class PollJobsUseCase @Inject constructor(
    private val queueRepository: QueueRepository,
) {
    suspend operator fun invoke(restaurantId: String, limit: Int = 20): AppResult<Int> =
        queueRepository.pollAndEnqueueJobs(restaurantId, limit)
}
