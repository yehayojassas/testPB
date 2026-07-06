package ch.planetebowl.printagent.domain.usecase

import ch.planetebowl.printagent.common.AppFailure
import ch.planetebowl.printagent.common.AppResult
import ch.planetebowl.printagent.common.asFailure
import ch.planetebowl.printagent.domain.model.Customer
import ch.planetebowl.printagent.domain.model.Customizations
import ch.planetebowl.printagent.domain.model.Money
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.OrderItem
import ch.planetebowl.printagent.domain.model.OrderType
import ch.planetebowl.printagent.domain.model.PrintJob
import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.model.PrintStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ConfirmPrintedUseCaseTest {

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

    private fun printedPendingAckJob() = PrintJob(
        id = 1,
        orderId = order.orderId,
        order = order,
        payloadJson = "{}",
        status = PrintStatus.PRINTED_PENDING_ACK,
        attemptCount = 0,
        lastErrorCode = null,
        lastErrorMessage = null,
        receivedAt = Instant.now(),
        nextRetryAt = null,
        printedAt = Instant.now(),
        acknowledgedAt = null,
        claimToken = "claim-token",
        claimExpiresAt = Instant.now().plusSeconds(90),
    )

    @Test
    fun `ack failure never triggers a new print attempt`() = runTest {
        val printerClient = FakePrinterClient(PrintResult.Success)
        val repository = FakeQueueRepository(order).apply {
            confirmPrintedResult = AppFailure.Network("timeout").asFailure()
        }
        val confirmUseCase = ConfirmPrintedUseCase(repository)

        val result = confirmUseCase(printedPendingAckJob())

        assertTrue(result is AppResult.Failure)
        assertEquals(1, repository.confirmPrintedCalls)
        // Le point critique : jamais un octet ne doit repartir vers l'imprimante suite a un
        // simple echec d'accuse serveur.
        assertEquals(0, printerClient.sendCallCount)
    }

    @Test
    fun `successful ack does not touch the printer either`() = runTest {
        val printerClient = FakePrinterClient(PrintResult.Success)
        val repository = FakeQueueRepository(order)
        val confirmUseCase = ConfirmPrintedUseCase(repository)

        val result = confirmUseCase(printedPendingAckJob())

        assertTrue(result is AppResult.Success)
        assertEquals(0, printerClient.sendCallCount)
    }

    @Test
    fun `confirmAllPending replays every job still awaiting acknowledgement`() = runTest {
        val repository = FakeQueueRepository(order)
        repository.recordPrintSuccess(printedPendingAckJob())
        val confirmUseCase = ConfirmPrintedUseCase(repository)

        val results = confirmUseCase.confirmAllPending()

        assertEquals(1, results.size)
        assertEquals(1, repository.confirmPrintedCalls)
    }
}
