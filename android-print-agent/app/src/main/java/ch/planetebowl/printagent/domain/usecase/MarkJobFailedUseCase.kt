package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.domain.model.PrintJob
import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.repository.QueueRepository
import javax.inject.Inject

/**
 * Enregistre explicitement un echec d'impression pour [job]. Utilise par PrintJobUseCase
 * dans le flux normal, et expose separement pour permettre a l'ecran Reglages de marquer
 * un job en echec depuis un diagnostic manuel sans dupliquer la logique de backoff.
 */
class MarkJobFailedUseCase @Inject constructor(
    private val queueRepository: QueueRepository,
) {
    suspend operator fun invoke(job: PrintJob, printResult: PrintResult) {
        queueRepository.recordPrintFailure(job, printResult)
    }
}
