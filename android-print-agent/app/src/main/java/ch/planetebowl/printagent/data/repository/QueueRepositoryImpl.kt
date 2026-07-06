package ch.planetebowl.printagent.data.repository

import ch.planetebowl.printagent.common.AppFailure
import ch.planetebowl.printagent.common.AppResult
import ch.planetebowl.printagent.common.BackoffPolicy
import ch.planetebowl.printagent.common.TextSanitizer
import ch.planetebowl.printagent.common.asFailure
import ch.planetebowl.printagent.common.asSuccess
import ch.planetebowl.printagent.data.local.PrintJobDao
import ch.planetebowl.printagent.data.local.PrintLogDao
import ch.planetebowl.printagent.data.local.entity.PrintJobEntity
import ch.planetebowl.printagent.data.local.entity.PrintLogEntity
import ch.planetebowl.printagent.data.local.entity.PrintLogEventType
import ch.planetebowl.printagent.data.local.entity.PrintLogResult
import ch.planetebowl.printagent.data.remote.BackendApi
import ch.planetebowl.printagent.data.remote.JobMapper
import ch.planetebowl.printagent.data.remote.dto.ApiErrorDto
import ch.planetebowl.printagent.data.remote.dto.FailedRequestDto
import ch.planetebowl.printagent.data.remote.dto.JobDto
import ch.planetebowl.printagent.data.remote.dto.PrintedRequestDto
import ch.planetebowl.printagent.domain.model.ApiError
import ch.planetebowl.printagent.domain.model.Order
import ch.planetebowl.printagent.domain.model.PrintJob
import ch.planetebowl.printagent.domain.model.PrintResult
import ch.planetebowl.printagent.domain.model.PrintStatus
import ch.planetebowl.printagent.domain.repository.QueueRepository
import ch.planetebowl.printagent.domain.repository.QueueRepository.ClaimOutcome
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class QueueRepositoryImpl @Inject constructor(
    private val printJobDao: PrintJobDao,
    private val printLogDao: PrintLogDao,
    private val backendApi: BackendApi,
    private val gson: Gson,
) : QueueRepository {

    override suspend fun pollAndEnqueueJobs(restaurantId: String, limit: Int): AppResult<Int> {
        val jobs = try {
            backendApi.getJobs(restaurantId, limit)
        } catch (e: HttpException) {
            log(orderId = "-", type = PrintLogEventType.POLL, result = PrintLogResult.FAILURE, message = "HTTP ${e.code()}")
            return AppFailure.Http(e.code(), null).asFailure()
        } catch (e: IOException) {
            log(orderId = "-", type = PrintLogEventType.POLL, result = PrintLogResult.FAILURE, message = e.message ?: "IOException")
            return AppFailure.Network(e.message ?: "réseau indisponible").asFailure()
        }

        var insertedCount = 0
        for (dto in jobs) {
            val entity = PrintJobEntity(
                orderId = dto.orderId,
                // Snapshot reconstruit via Gson a partir du DTO deserialise (pas le texte
                // brut du corps HTTP, non conserve par Retrofit) : fidele champ a champ au
                // contrat, suffisant pour rejouer le formatage du ticket plus tard.
                payloadJson = gson.toJson(dto),
                status = PrintStatus.PENDING,
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
            val rowId = printJobDao.insertIgnoringDuplicates(entity)
            if (rowId != -1L) insertedCount++
        }
        log(
            orderId = "-",
            type = PrintLogEventType.POLL,
            result = PrintLogResult.SUCCESS,
            message = "${jobs.size} job(s) reçu(s), $insertedCount nouveau(x)",
        )
        return insertedCount.asSuccess()
    }

    override suspend fun findNextClaimable(now: Instant): PrintJob? =
        // `let { toDomain(it) }` plutot que `let(::toDomain)` : toDomain est suspend, une
        // reference de fonction ne peut pas etre passee a un parametre non-suspend meme si
        // `let` est inline — seul un lambda litteral permet l'appel suspendu transparent.
        printJobDao.findNextClaimable(now)?.let { toDomain(it) }

    override suspend fun claimJob(job: PrintJob): ClaimOutcome {
        val attemptNumber = job.attemptCount + 1
        val idempotencyKey = "${job.orderId}:$attemptNumber"
        return try {
            val response = backendApi.claimJob(job.orderId, idempotencyKey)
            when {
                response.isSuccessful -> {
                    val body = response.body() ?: return ClaimOutcome.Failed("Réponse claim vide.")
                    val updated = job.copy(
                        status = PrintStatus.PRINTING,
                        claimToken = body.claimToken,
                        claimExpiresAt = parseInstantSafely(body.expiresAt),
                    )
                    persist(updated)
                    log(job.orderId, PrintLogEventType.CLAIM, PrintLogResult.SUCCESS, "Ticket réclamé")
                    ClaimOutcome.Claimed(updated)
                }
                response.code() == 409 -> handleClaimConflict(job, response.errorBody())
                else -> {
                    log(job.orderId, PrintLogEventType.CLAIM, PrintLogResult.FAILURE, "HTTP ${response.code()}")
                    ClaimOutcome.Failed("HTTP ${response.code()}")
                }
            }
        } catch (e: IOException) {
            log(job.orderId, PrintLogEventType.CLAIM, PrintLogResult.FAILURE, e.message ?: "IOException")
            ClaimOutcome.Failed(e.message ?: "réseau indisponible")
        }
    }

    private suspend fun handleClaimConflict(job: PrintJob, errorBody: ResponseBody?): ClaimOutcome {
        val apiError = parseApiError(errorBody)
        return when (apiError) {
            ApiError.ALREADY_CLAIMED, ApiError.ALREADY_PRINTED -> {
                persist(job.copy(status = PrintStatus.COMPLETED))
                log(job.orderId, PrintLogEventType.CLAIM, PrintLogResult.INFO, "Déjà traité ailleurs (${apiError.name})")
                ClaimOutcome.RejectedTerminal(apiError)
            }
            ApiError.MAX_ATTEMPTS_REACHED -> {
                persist(job.copy(status = PrintStatus.FAILED_MANUAL_REVIEW))
                log(job.orderId, PrintLogEventType.MANUAL_REVIEW, PrintLogResult.INFO, "Nombre maximal de tentatives atteint côté serveur")
                ClaimOutcome.RejectedTerminal(apiError)
            }
            else -> {
                log(job.orderId, PrintLogEventType.CLAIM, PrintLogResult.FAILURE, "409 inattendu (${apiError.name})")
                ClaimOutcome.Failed("409 inattendu")
            }
        }
    }

    override suspend fun recordPrintFailure(job: PrintJob, printResult: PrintResult) {
        val newAttemptCount = job.attemptCount + 1
        val sanitizedMessage = TextSanitizer.sanitizeForLog(printResult.userMessage())
        val terminal = newAttemptCount >= BackoffPolicy.MAX_PRINT_ATTEMPTS
        val updated = job.copy(
            attemptCount = newAttemptCount,
            lastErrorCode = printResult.errorCode(),
            lastErrorMessage = sanitizedMessage,
            status = if (terminal) PrintStatus.FAILED_MANUAL_REVIEW else PrintStatus.RETRY_WAIT,
            nextRetryAt = if (terminal) null else Instant.ofEpochMilli(
                BackoffPolicy.nextRetryAtEpochMillis(newAttemptCount, Instant.now().toEpochMilli())
            ),
        )
        persist(updated)
        log(
            job.orderId,
            if (terminal) PrintLogEventType.MANUAL_REVIEW else PrintLogEventType.PRINT_FAILURE,
            PrintLogResult.FAILURE,
            sanitizedMessage,
        )
        releaseServerClaimBestEffort(job, printResult)
    }

    /** Best-effort : que /failed reussisse ou non, l'etat local (calcule ci-dessus) reste
     * la source de verite pour le retry cote tablette — on ne bloque jamais le retry local
     * sur la disponibilite reseau de cet appel de liberation. */
    private suspend fun releaseServerClaimBestEffort(job: PrintJob, printResult: PrintResult) {
        val claimToken = job.claimToken ?: return
        try {
            backendApi.markFailed(
                job.orderId,
                FailedRequestDto(
                    claimToken = claimToken,
                    errorCode = printResult.errorCode() ?: "UNKNOWN",
                    errorMessage = TextSanitizer.sanitizeForLog(printResult.userMessage()),
                ),
            )
        } catch (e: IOException) {
            log(job.orderId, PrintLogEventType.PRINT_FAILURE, PrintLogResult.INFO, "Libération serveur du claim impossible (réseau)")
        }
    }

    override suspend fun recordPrintSuccess(job: PrintJob, printedAt: Instant) {
        val updated = job.copy(status = PrintStatus.PRINTED_PENDING_ACK, printedAt = printedAt, nextRetryAt = null)
        persist(updated)
        log(job.orderId, PrintLogEventType.PRINT_SUCCESS, PrintLogResult.SUCCESS, "Ticket envoyé à l'imprimante")
    }

    override suspend fun confirmPrinted(job: PrintJob): AppResult<Unit> {
        val claimToken = job.claimToken
            ?: return AppFailure.Unexpected("claimToken manquant pour ${job.orderId}").asFailure()
        val idempotencyKey = "${job.orderId}:ack:${job.attemptCount}"
        return try {
            val response = backendApi.markPrinted(
                job.orderId,
                idempotencyKey,
                PrintedRequestDto(claimToken = claimToken, printedAt = (job.printedAt ?: Instant.now()).toString()),
            )
            when {
                response.isSuccessful -> {
                    persist(job.copy(status = PrintStatus.COMPLETED, acknowledgedAt = Instant.now()))
                    log(job.orderId, PrintLogEventType.ACK_SUCCESS, PrintLogResult.SUCCESS, "Accusé confirmé")
                    Unit.asSuccess()
                }
                response.code() == 409 -> {
                    // CLAIM_MISMATCH : incoherence serveur/local apres une impression deja
                    // effectuee physiquement — jamais de reimpression, on escalade pour
                    // verification humaine plutot que de deviner.
                    persist(job.copy(status = PrintStatus.FAILED_MANUAL_REVIEW))
                    log(job.orderId, PrintLogEventType.MANUAL_REVIEW, PrintLogResult.FAILURE, "CLAIM_MISMATCH lors de l'accusé")
                    AppFailure.ClaimExpiredOrMismatch.asFailure()
                }
                else -> {
                    scheduleAckRetry(job)
                    log(job.orderId, PrintLogEventType.ACK_FAILURE, PrintLogResult.FAILURE, "HTTP ${response.code()}")
                    AppFailure.Http(response.code(), null).asFailure()
                }
            }
        } catch (e: IOException) {
            scheduleAckRetry(job)
            log(job.orderId, PrintLogEventType.ACK_FAILURE, PrintLogResult.FAILURE, e.message ?: "IOException")
            AppFailure.Network(e.message ?: "réseau indisponible").asFailure()
        }
    }

    /**
     * Ni la table ni le cahier des charges ne prevoient de compteur d'essais dedie a
     * l'accuse : on reutilise nextRetryAt avec un delai fixe jitte (BackoffPolicy avec
     * attemptNumber=1 a chaque echec) plutot que d'escalader indefiniment, pour rester dans
     * le schema Room specifie. Le rythme reste borne par la boucle de polling elle-meme
     * (5-15s) et par AckRetryWorker si le service est tue.
     */
    private suspend fun scheduleAckRetry(job: PrintJob) {
        val nextRetry = Instant.ofEpochMilli(BackoffPolicy.nextRetryAtEpochMillis(1, Instant.now().toEpochMilli()))
        persist(job.copy(nextRetryAt = nextRetry))
    }

    override suspend fun findAllPendingAck(): List<PrintJob> =
        printJobDao.findPendingAckReady(Instant.now()).map { toDomain(it) }

    override fun observeRecentJobs(limit: Int): Flow<List<PrintJob>> =
        printJobDao.observeRecent(limit).map { entities -> entities.map { entity -> toDomain(entity) } }

    override suspend fun failAllStuckInPrinting(): Int = printJobDao.failAllStuckInPrinting()

    override suspend fun deserializeOrder(payloadJson: String): Order =
        JobMapper.toDomain(gson.fromJson(payloadJson, JobDto::class.java))

    private suspend fun persist(job: PrintJob) {
        printJobDao.update(toEntity(job))
    }

    private suspend fun toDomain(entity: PrintJobEntity): PrintJob = PrintJob(
        id = entity.id,
        orderId = entity.orderId,
        order = deserializeOrder(entity.payloadJson),
        payloadJson = entity.payloadJson,
        status = entity.status,
        attemptCount = entity.attemptCount,
        lastErrorCode = entity.lastErrorCode,
        lastErrorMessage = entity.lastErrorMessage,
        receivedAt = entity.receivedAt,
        nextRetryAt = entity.nextRetryAt,
        printedAt = entity.printedAt,
        acknowledgedAt = entity.acknowledgedAt,
        claimToken = entity.claimToken,
        claimExpiresAt = entity.claimExpiresAt,
    )

    private fun toEntity(job: PrintJob): PrintJobEntity = PrintJobEntity(
        id = job.id,
        orderId = job.orderId,
        payloadJson = job.payloadJson,
        status = job.status,
        attemptCount = job.attemptCount,
        lastErrorCode = job.lastErrorCode,
        lastErrorMessage = job.lastErrorMessage,
        receivedAt = job.receivedAt,
        nextRetryAt = job.nextRetryAt,
        printedAt = job.printedAt,
        acknowledgedAt = job.acknowledgedAt,
        claimToken = job.claimToken,
        claimExpiresAt = job.claimExpiresAt,
    )

    private fun parseApiError(errorBody: ResponseBody?): ApiError {
        val raw = try {
            errorBody?.string()
        } catch (e: IOException) {
            null
        } ?: return ApiError.UNKNOWN
        return try {
            ApiError.fromApiValue(gson.fromJson(raw, ApiErrorDto::class.java)?.code)
        } catch (e: Exception) {
            ApiError.UNKNOWN
        }
    }

    private fun parseInstantSafely(raw: String): Instant = try {
        Instant.parse(raw)
    } catch (e: Exception) {
        Instant.now().plusSeconds(90)
    }

    private suspend fun log(orderId: String, type: PrintLogEventType, result: PrintLogResult, message: String?) {
        printLogDao.insert(
            PrintLogEntity(
                orderId = orderId,
                eventType = type,
                result = result,
                message = TextSanitizer.sanitizeForLog(message),
                timestamp = Instant.now(),
            )
        )
    }
}
