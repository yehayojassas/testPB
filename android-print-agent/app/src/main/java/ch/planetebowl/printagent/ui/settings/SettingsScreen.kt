package ch.planetebowl.printagent.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.planetebowl.printagent.R
import ch.planetebowl.printagent.domain.model.PrinterSettings
import ch.planetebowl.printagent.domain.model.TicketWidth
import ch.planetebowl.printagent.service.AgentStatus
import ch.planetebowl.printagent.ui.settings.components.AgentStatusBanner
import ch.planetebowl.printagent.ui.settings.components.LabeledTextField
import ch.planetebowl.printagent.ui.settings.components.SectionCard
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.savedConfirmationVisible) {
        if (state.savedConfirmationVisible) {
            kotlinx.coroutines.delay(2000)
            viewModel.onSavedConfirmationShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AgentStatusBanner(status = state.agentStatus)

            StatusSummarySection(state)

            SectionCard(title = "Connexion API") {
                LabeledTextField(
                    label = "URL de l'API",
                    value = state.apiBaseUrl,
                    onValueChange = viewModel::onApiBaseUrlChange,
                    errorMessage = state.apiBaseUrlError.message,
                    supportingText = "Ex : https://xxxx.supabase.co/functions/v1/print-agent",
                    keyboardType = KeyboardType.Uri,
                )
                val tokenVisible = state.tokenVisible
                LabeledTextField(
                    label = if (state.hasStoredToken) "Nouveau token (optionnel)" else "Token API",
                    value = state.tokenInput,
                    onValueChange = viewModel::onTokenChange,
                    isPassword = !tokenVisible,
                    supportingText = if (state.hasStoredToken) "Un token est déjà enregistré et masqué." else "Fourni par votre prestataire technique.",
                    trailingIcon = {
                        IconButton(onClick = viewModel::onToggleTokenVisibility) {
                            Icon(
                                imageVector = if (tokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Afficher/masquer le token",
                            )
                        }
                    },
                )
                LabeledTextField(
                    label = "Identifiant restaurant (UUID)",
                    value = state.restaurantId,
                    onValueChange = viewModel::onRestaurantIdChange,
                    errorMessage = state.restaurantIdError.message,
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(checked = state.developerModeEnabled, onCheckedChange = viewModel::onDeveloperModeToggle)
                    Text(
                        text = "Mode développement (autorise http://)",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }

            SectionCard(title = "Imprimante") {
                LabeledTextField(
                    label = "Adresse IP de l'imprimante",
                    value = state.printerIp,
                    onValueChange = viewModel::onPrinterIpChange,
                    errorMessage = state.printerIpError.message,
                    supportingText = "Ex : 192.168.1.50",
                    keyboardType = KeyboardType.Decimal,
                )
                LabeledTextField(
                    label = "Port TCP",
                    value = state.printerPortText,
                    onValueChange = viewModel::onPrinterPortChange,
                    errorMessage = state.printerPortError.message,
                    supportingText = "Par défaut : 9100",
                    keyboardType = KeyboardType.Number,
                )
                Text("Largeur du ticket", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(TicketWidth.NARROW_42, TicketWidth.WIDE_48).forEachIndexed { index, width ->
                        SegmentedButton(
                            selected = state.ticketWidth == width,
                            onClick = { viewModel.onTicketWidthChange(width) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) {
                            Text("${width.columns} caractères")
                        }
                    }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(checked = state.cutPaperEnabled, onCheckedChange = viewModel::onCutPaperToggle)
                    Text("Coupe papier automatique", modifier = Modifier.padding(start = 12.dp))
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(checked = state.cashDrawerEnabled, onCheckedChange = viewModel::onCashDrawerToggle)
                    Text("Ouverture tiroir-caisse", modifier = Modifier.padding(start = 12.dp))
                }
            }

            SectionCard(title = "Point de vente et synchronisation") {
                LabeledTextField(
                    label = "Nom du point de vente (sur le ticket)",
                    value = state.storeName,
                    onValueChange = viewModel::onStoreNameChange,
                )
                Text(
                    "Intervalle de synchronisation : ${state.pollingIntervalSeconds}s",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = state.pollingIntervalSeconds.toFloat(),
                    onValueChange = { viewModel.onPollingIntervalChange(it.toInt()) },
                    valueRange = PrinterSettings.MIN_POLLING_INTERVAL_SECONDS.toFloat()..PrinterSettings.MAX_POLLING_INTERVAL_SECONDS.toFloat(),
                    steps = PrinterSettings.MAX_POLLING_INTERVAL_SECONDS - PrinterSettings.MIN_POLLING_INTERVAL_SECONDS - 1,
                )
            }

            SectionCard(title = "Actions") {
                Button(
                    onClick = viewModel::onSave,
                    enabled = state.isFormValid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.savedConfirmationVisible) "Enregistré ✓" else "Enregistrer les réglages") }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = viewModel::onTestConnection,
                        enabled = !state.testConnectionInProgress && !state.printerIpError.hasError,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.testConnectionInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Tester la connexion")
                        }
                    }
                    OutlinedButton(
                        onClick = viewModel::onTestPrint,
                        enabled = !state.testPrintInProgress && !state.printerIpError.hasError,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.testPrintInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("Tester l'impression")
                        }
                    }
                }
                state.testConnectionResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                state.testPrintResult?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

                if (state.agentStatus == AgentStatus.STOPPED) {
                    Button(onClick = viewModel::onStartAgent, modifier = Modifier.fillMaxWidth(), enabled = state.isFormValid) {
                        Text("Démarrer l'agent")
                    }
                } else {
                    Button(onClick = viewModel::onStopAgent, modifier = Modifier.fillMaxWidth()) {
                        Text("Arrêter l'agent")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSummarySection(state: SettingsUiState) {
    SectionCard(title = "Diagnostic") {
        val lastSyncText = state.lastSyncAt?.let {
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault()).format(it)
        } ?: "Jamais synchronisé"
        Text("Dernière synchronisation : $lastSyncText", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Dernière erreur : ${state.lastError ?: "Aucune"}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (state.lastError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
