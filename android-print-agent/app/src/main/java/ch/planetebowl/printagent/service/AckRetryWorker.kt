package ch.planetebowl.printagent.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.planetebowl.printagent.domain.usecase.ConfirmPrintedUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Filet de securite WorkManager : rejoue les accuses (POST /printed) encore en attente si
 * le Foreground Service a ete tue par le systeme entre-temps. Ne renvoie JAMAIS un ticket a
 * l'imprimante — uniquement des confirmations pour des jobs deja PRINTED_PENDING_ACK.
 * La cadence normale de confirmation reste la boucle de l'agent (AgentController) tant que
 * le service tourne ; ce worker periodique (15 min, minimum impose par WorkManager) ne
 * couvre que le cas degrade ou l'app entiere a ete tuee.
 */
@HiltWorker
class AckRetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val confirmPrintedUseCase: ConfirmPrintedUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val results = confirmPrintedUseCase.confirmAllPending()
        val stillFailing = results.any { it is ch.planetebowl.printagent.common.AppResult.Failure }
        return if (stillFailing) Result.retry() else Result.success()
    }
}

object AckRetryScheduler {
    private const val UNIQUE_WORK_NAME = "ack_retry_safety_net"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<AckRetryWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
