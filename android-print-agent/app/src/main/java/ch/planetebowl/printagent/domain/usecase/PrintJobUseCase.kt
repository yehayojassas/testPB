package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.domain.model.PrintJob
import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.model.PrinterSettings
import ch.planetebowl.printagent.domain.repository.PrinterClient
import ch.planetebowl.printagent.domain.repository.QueueRepository
import ch.planetebowl.printagent.printing.TicketFormatter
import javax.inject.Inject

/**
 * Formate puis envoie un job deja reclame (status=PRINTING) a l'imprimante, et persiste la
 * transition resultante (PRINTED_PENDING_ACK en cas de succes, RETRY_WAIT/FAILED_MANUAL_REVIEW
 * en cas d'echec — voir QueueRepositoryImpl.recordPrintFailure pour le detail du backoff).
 */
class PrintJobUseCase @Inject constructor(
    private val queueRepository: QueueRepository,
    private val printerClient: PrinterClient,
    private val ticketFormatter: TicketFormatter,
) {
    suspend operator fun invoke(job: PrintJob, settings: PrinterSettings): PrintResult {
        val ticketBytes = ticketFormatter.format(job.order, settings)
        val result = printerClient.send(settings.printerIp, settings.printerPort, ticketBytes)
        if (result.isSuccess) {
            queueRepository.recordPrintSuccess(job)
        } else {
            queueRepository.recordPrintFailure(job, result)
        }
        return result
    }
}
