package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.repository.PrinterClient

class FakePrinterClient(private val result: PrintResult = PrintResult.Success) : PrinterClient {
    var sendCallCount = 0
        private set

    override suspend fun send(host: String, port: Int, ticketBytes: ByteArray): PrintResult {
        sendCallCount++
        return result
    }
}
