package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.domain.model.Customer
import ch.planetebowl.printagent.domain.model.Customizations
import ch.planetebowl.printagent.domain.model.Money
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.OrderItem
import ch.planetebowl.printagent.domain.model.OrderType
import ch.planetebowl.printagent.domain.model.PrintJob
import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.model.PrintStatus
import ch.planetebowl.printagent.domain.model.PrinterSettings
import ch.planetebowl.printagent.printing.TicketFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class PrintJobUseCaseTest {

    private val order = Order(
        orderId = "order-1",
        orderCode = "PB-1",
        payloadVersion = 1,
        createdAt = Instant.now(),
        orderType = OrderType.TAKEAWAY,
        pickupSlot = null,
        customer = Customer("Client", null),
        address = null,
        items = listOf(OrderItem("Bowl", 1, Money(BigDecimal("10.00")), Customizations.EMPTY)),
        total = Money(BigDecimal("10.00")),
        kitchenNote = null,
        restaurantId = "restaurant-1",
    )

    private fun claimedJob() = PrintJob(
        id = 1,
        orderId = order.orderId,
        order = order,
        payloadJson = "{}",
        status = PrintStatus.PRINTING,
        attemptCount = 0,
        lastErrorCode = null,
        lastErrorMessage = null,
        receivedAt = Instant.now(),
        nextRetryAt = null,
        printedAt = null,
        acknowledgedAt = null,
        claimToken = "claim-token",
        claimExpiresAt = Instant.now().plusSeconds(90),
    )

    @Test
    fun `successful print transitions the job to PRINTED_PENDING_ACK`() = runTest {
        val repository = FakeQueueRepository(order)
        val useCase = PrintJobUseCase(repository, FakePrinterClient(PrintResult.Success), TicketFormatter())

        val result = useCase(claimedJob(), PrinterSettings.defaults().copy(printerIp = "192.168.1.50"))

        assertEquals(PrintResult.Success, result)
        assertEquals(1, repository.recordPrintSuccessCalls)
        assertEquals(0, repository.recordPrintFailureCalls)
        assertEquals(PrintStatus.PRINTED_PENDING_ACK, repository.lastKnownStatus())
    }

    @Test
    fun `failed print records a failure and never marks the job printed`() = runTest {
        val repository = FakeQueueRepository(order)
        val useCase = PrintJobUseCase(repository, FakePrinterClient(PrintResult.ConnectionRefusedOrPortClosed), TicketFormatter())

        val result = useCase(claimedJob(), PrinterSettings.defaults().copy(printerIp = "192.168.1.50"))

        assertEquals(PrintResult.ConnectionRefusedOrPortClosed, result)
        assertEquals(0, repository.recordPrintSuccessCalls)
        assertEquals(1, repository.recordPrintFailureCalls)
        assertEquals(PrintStatus.RETRY_WAIT, repository.lastKnownStatus())
    }
}
