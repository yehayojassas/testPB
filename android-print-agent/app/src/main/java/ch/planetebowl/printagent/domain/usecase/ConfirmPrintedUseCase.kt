package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.common.AppResult
import ch.planetebowl.printagent.domain.model.PrintJob
import ch.planetebowl.printagent.domain.repository.QueueRepository
import javax.inject.Inject

/**
 * Confirme au backend qu'un ticket PRINTED_PENDING_ACK a bien ete imprime. N'envoie
 * JAMAIS un nouvel octet a l'imprimante — en cas d'echec reseau, le job reste
 * PRINTED_PENDING_ACK et sera retente (par la boucle de polling ou AckRetryWorker),
 * jamais reimprime.
 */
class ConfirmPrintedUseCase @Inject constructor(
    private val queueRepository: QueueRepository,
) {
    suspend operator fun invoke(job: PrintJob): AppResult<Unit> = queueRepository.confirmPrinted(job)

    suspend fun confirmAllPending(): List<AppResult<Unit>> =
        queueRepository.findAllPendingAck().map { job -> invoke(job) }
}
