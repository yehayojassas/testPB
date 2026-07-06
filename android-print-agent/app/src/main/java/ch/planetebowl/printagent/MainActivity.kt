package ch.planetebowl.printagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import ch.planetebowl.printagent.ui.settings.SettingsScreen
import ch.planetebowl.printagent.ui.theme.PrintAgentTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Point d'entree unique : une tablette de caisse n'a besoin que de l'ecran de reglages
 * (URL/token/IP/port/restaurantId, boutons de test, demarrage/arret de l'agent). Toute
 * la logique metier tourne dans PrinterForegroundService, independamment du cycle de vie
 * de cette activite.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrintAgentTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen()
                }
            }
        }
    }
}
