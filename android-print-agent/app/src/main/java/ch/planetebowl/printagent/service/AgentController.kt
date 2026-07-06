package ch.planetebowl.printagent.service

import ch.planetebowl.printagent.common.TextSanitizer
import ch.planetebowl.printagent.domain.model.PrinterSettings
import ch.planetebowl.printagent.domain.repository.QueueRepository
import ch.planetebowl.printagent.domain.repository.SettingsRepository
import ch.planetebowl.printagent.domain.usecase.ClaimJobUseCase
import ch.planetebowl.printagent.domain.usecase.ConfirmPrintedUseCase
import ch.planetebowl.printagent.domain.usecase.PollJobsUseCase
import ch.planetebowl.printagent.domain.usecase.PrintJobUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class AgentStatus { STOPPED, RUNNING, ERROR }

/**
 * Boucle metier de l'agent : poll -> claim -> impression -> accuse, en continu tant que
 * demarre. Ne connait pas Android (pas de Context/Service ici) : PrinterForegroundService
 * se contente de piloter start()/stop() et de refleter [status] dans sa notification, ce
 * qui garde cette classe testable sans instrumentation.
 */
@Singleton
class AgentController @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pollJobsUseCase: PollJobsUseCase,
    private val claimJobUseCase: ClaimJobUseCase,
    private val printJobUseCase: PrintJobUseCase,
    private val confirmPrintedUseCase: ConfirmPrintedUseCase,
) {
    private val _status = MutableStateFlow(AgentStatus.STOPPED)
    val status: StateFlow<AgentStatus> = _status.asStateFlow()

    /** Jobs claimes/imprimes/confirmes durant le cycle courant, pour affichage diagnostic. */
    private val _lastCycleSummary = MutableStateFlow("")
    val lastCycleSummary: StateFlow<String> = _lastCycleSummary.asStateFlow()

    private var loopJob: Job? = null
    private val supervisorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Nombre max de jobs traites par cycle : borne le temps passe hors polling meme si
     * la file locale a beaucoup de retard a rattraper (limit de GET /jobs est deja 20). */
    private val maxJobsPerCycle = 20

    fun isRunning(): Boolean = _status.value == AgentStatus.RUNNING

    fun start() {
        if (loopJob?.isActive == true) return
        _status.value = AgentStatus.RUNNING
        loopJob = supervisorScope.launch { runLoop() }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        _status.value = AgentStatus.STOPPED
    }

    private suspend fun runLoop() {
        while (true) {
            val settings = settingsRepository.getSettings()
            runCycle(settings)
            delay(settings.pollingIntervalSeconds.coerceIn(
                PrinterSettings.MIN_POLLING_INTERVAL_SECONDS,
                PrinterSettings.MAX_POLLING_INTERVAL_SECONDS,
            ) * 1000L)
        }
    }

    private suspend fun runCycle(settings: PrinterSettings) {
        var claimedCount = 0
        var printedCount = 0
        var confirmedCount = 0
        try {
            pollJobsUseCase(settings.restaurantId)
                .onSuccess { settingsRepository.setLastSyncAt(Instant.now()); settingsRepository.setLastError(null) }
                .onFailure { failure -> settingsRepository.setLastError(TextSanitizer.sanitizeForLog(failure.userMessage)) }

            repeat(maxJobsPerCycle) {
                val outcome = claimJobUseCase() ?: return@repeat
                when (outcome) {
                    is QueueRepository.ClaimOutcome.Claimed -> {
                        claimedCount++
                        val result = printJobUseCase(outcome.job, settings)
                        if (result.isSuccess) printedCount++ else settingsRepository.setLastError(result.userMessage())
                    }
                    is QueueRepository.ClaimOutcome.RejectedTerminal -> Unit
                    is QueueRepository.ClaimOutcome.Failed -> {
                        settingsRepository.setLastError("Réservation impossible : ${outcome.reason}")
                        return
                    }
                }
            }

            confirmPrintedUseCase.confirmAllPending().forEach { result ->
                result.onSuccess { confirmedCount++ }
            }
            _status.value = AgentStatus.RUNNING
        } catch (e: Exception) {
            // Une boucle d'agent ne doit jamais mourir silencieusement sur une exception
            // inattendue : on la trace comme erreur visible et on continue au cycle suivant
            // plutot que de laisser le service tourner sans plus jamais rien faire.
            _status.value = AgentStatus.ERROR
            settingsRepository.setLastError(TextSanitizer.sanitizeForLog(e.message ?: e::class.java.simpleName))
        } finally {
            _lastCycleSummary.value = "Réclamés: $claimedCount · Imprimés: $printedCount · Confirmés: $confirmedCount"
        }
    }
}
