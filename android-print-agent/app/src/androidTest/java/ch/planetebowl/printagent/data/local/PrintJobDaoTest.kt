package ch.planetebowl.printagent.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ch.planetebowl.printagent.data.local.entity.PrintJobEntity
import ch.planetebowl.printagent.domain.model.PrintStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PrintJobDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PrintJobDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.printJobDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(orderId: String, status: PrintStatus = PrintStatus.PENDING) = PrintJobEntity(
        orderId = orderId,
        payloadJson = "{}",
        status = status,
        attemptCount = 0,
        lastErrorCode = null,
        lastErrorMessage = null,
        receivedAt = Instant.now(),
        nextRetryAt = null,
        printedAt = null,
        acknowledgedAt = null,
        claimToken = null,
        claimExpiresAt = null,
    )

    @Test
    fun insertIgnoringDuplicates_neverDuplicatesAnOrderId() = runBlocking {
        val firstRowId = dao.insertIgnoringDuplicates(entity("order-1"))
        val secondRowId = dao.insertIgnoringDuplicates(entity("order-1"))

        assertNotNull(firstRowId)
        assertEquals(-1L, secondRowId) // OnConflictStrategy.IGNORE : deuxieme insertion ignorée.

        val stored = dao.findByOrderId("order-1")
        assertNotNull(stored)
    }

    @Test
    fun failAllStuckInPrinting_movesOnlyPrintingJobsToManualReview() = runBlocking {
        dao.insertIgnoringDuplicates(entity("order-printing", PrintStatus.PRINTING))
        dao.insertIgnoringDuplicates(entity("order-pending", PrintStatus.PENDING))

        val affected = dao.failAllStuckInPrinting()

        assertEquals(1, affected)
        assertEquals(PrintStatus.FAILED_MANUAL_REVIEW, dao.findByOrderId("order-printing")?.status)
        assertEquals(PrintStatus.PENDING, dao.findByOrderId("order-pending")?.status)
    }

    @Test
    fun findNextClaimable_returnsOldestPendingOrDueRetry() = runBlocking {
        val now = Instant.now()
        dao.insertIgnoringDuplicates(entity("order-a").copy(receivedAt = now.minusSeconds(60)))
        dao.insertIgnoringDuplicates(entity("order-b").copy(receivedAt = now.minusSeconds(30)))

        val next = dao.findNextClaimable(now)
        assertEquals("order-a", next?.orderId)
    }

    @Test
    fun findNextClaimable_ignoresRetryWaitNotYetDue() = runBlocking {
        val now = Instant.now()
        dao.insertIgnoringDuplicates(
            entity("order-future-retry", PrintStatus.RETRY_WAIT).copy(nextRetryAt = now.plusSeconds(60)),
        )

        val next = dao.findNextClaimable(now)
        assertNull(next)
    }
}
