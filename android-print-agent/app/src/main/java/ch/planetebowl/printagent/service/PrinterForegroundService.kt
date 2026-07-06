package ch.planetebowl.printagent.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import ch.planetebowl.printagent.domain.repository.QueueRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service requis pour qu'Android ne tue pas le polling/l'impression quand
 * l'app passe en arriere-plan sur la tablette de caisse. Ne contient aucune logique
 * metier propre : delegue entierement a [AgentController], et se contente de piloter le
 * cycle de vie Android (notification permanente, sweep au demarrage).
 */
@AndroidEntryPoint
class PrinterForegroundService : LifecycleService() {

    @Inject lateinit var agentController: AgentController
    @Inject lateinit var notificationFactory: NotificationFactory
    @Inject lateinit var queueRepository: QueueRepository

    override fun onCreate() {
        super.onCreate()
        notificationFactory.ensureChannel()
        startForeground(NotificationFactory.NOTIFICATION_ID, notificationFactory.buildForegroundNotification("Démarrage…"))

        // Sweep de demarrage : voir PrintJobDao.failAllStuckInPrinting / PrintStatus.PRINTING
        // pour la justification (crash pendant l'impression = etat non fiable, jamais de
        // retry automatique aveugle qui risquerait un ticket en double).
        lifecycleScope.launch { queueRepository.failAllStuckInPrinting() }

        lifecycleScope.launch {
            agentController.status.collect { status ->
                val text = when (status) {
                    AgentStatus.RUNNING -> "Agent actif — surveillance des commandes"
                    AgentStatus.STOPPED -> "Agent à l'arrêt"
                    AgentStatus.ERROR -> "Agent actif — dernière tentative en erreur"
                }
                notificationFactory.ensureChannel()
                startForeground(NotificationFactory.NOTIFICATION_ID, notificationFactory.buildForegroundNotification(text))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                agentController.stop()
                stopSelf()
            }
            else -> agentController.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        agentController.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        const val ACTION_STOP = "ch.planetebowl.printagent.action.STOP_AGENT"
    }
}
