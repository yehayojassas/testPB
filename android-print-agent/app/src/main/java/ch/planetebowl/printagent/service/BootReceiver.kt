package ch.planetebowl.printagent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * Relance l'agent automatiquement apres un redemarrage de la tablette : une caisse n'a
 * personne pour rouvrir l'app manuellement le matin. Redemarre inconditionnellement le
 * service ; si les reglages (URL/token/IP) sont incomplets, la premiere boucle de
 * l'agent echouera proprement et l'affichera comme derniere erreur dans les reglages,
 * plutot que de faire planter ce receiver sur une lecture DataStore asynchrone.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val serviceIntent = Intent(context, PrinterForegroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
