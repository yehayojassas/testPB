package ch.planetebowl.printagent.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.planetebowl.printagent.common.Validators
import ch.planetebowl.printagent.domain.model.PrinterSettings
import ch.planetebowl.printagent.domain.model.TicketWidth
import ch.planetebowl.printagent.domain.repository.SettingsRepository
import ch.planetebowl.printagent.domain.usecase.TestConnectionUseCase
import ch.planetebowl.printagent.domain.usecase.TestPrintUseCase
import ch.planetebowl.printagent.service.AgentController
import ch.planetebowl.printagent.service.PrinterForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val agentController: AgentController,
    private val testConnectionUseCase: TestConnectionUseCase,
    private val testPrintUseCase: TestPrintUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadInitialSettings()
        observeAgentStatus()
        observeSyncAndErrors()
    }

    private fun loadInitialSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.value = _uiState.value.copy(
                apiBaseUrl = settings.apiBaseUrl,
                restaurantId = settings.restaurantId,
                storeName = settings.storeName,
                printerIp = settings.printerIp,
                printerPortText = settings.printerPort.toString(),
                pollingIntervalSeconds = settings.pollingIntervalSeconds,
                cutPaperEnabled = settings.cutPaperEnabled,
                cashDrawerEnabled = settings.cashDrawerEnabled,
                ticketWidth = settings.ticketWidth,
                developerModeEnabled = settings.developerModeEnabled,
                hasStoredToken = settingsRepository.hasToken(),
            )
        }
    }

    private fun observeAgentStatus() {
        viewModelScope.launch {
            agentController.status.collect { status ->
                _uiState.value = _uiState.value.copy(agentStatus = status)
            }
        }
    }

    private fun observeSyncAndErrors() {
        viewModelScope.launch {
            settingsRepository.observeLastSyncAt().combine(settingsRepository.observeLastError()) { sync, error ->
                sync to error
            }.collect { (sync, error) ->
                _uiState.value = _uiState.value.copy(lastSyncAt = sync, lastError = error)
            }
        }
    }

    fun onApiBaseUrlChange(value: String) {
        val validation = Validators.validateApiBaseUrl(value, _uiState.value.developerModeEnabled)
        _uiState.value = _uiState.value.copy(
            apiBaseUrl = value,
            apiBaseUrlError = (validation as? Validators.ValidationResult.Invalid)?.let { FieldError(it.reason) } ?: FieldError.NONE,
        )
    }

    fun onTokenChange(value: String) {
        _uiState.value = _uiState.value.copy(tokenInput = value)
    }

    fun onToggleTokenVisibility() {
        _uiState.value = _uiState.value.copy(tokenVisible = !_uiState.value.tokenVisible)
    }

    fun onRestaurantIdChange(value: String) {
        val valid = Validators.isValidUuid(value)
        _uiState.value = _uiState.value.copy(
            restaurantId = value,
            restaurantIdError = if (valid || value.isBlank()) FieldError.NONE else FieldError("Format UUID invalide."),
        )
    }

    fun onStoreNameChange(value: String) {
        _uiState.value = _uiState.value.copy(storeName = value)
    }

    fun onPrinterIpChange(value: String) {
        val valid = Validators.isValidIpv4(value)
        _uiState.value = _uiState.value.copy(
            printerIp = value,
            printerIpError = if (valid || value.isBlank()) FieldError.NONE else FieldError("Adresse IPv4 invalide (ex: 192.168.1.50)."),
        )
    }

    fun onPrinterPortChange(value: String) {
        val port = value.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            printerPortText = value,
            printerPortError = if (port != null && Validators.isValidPort(port)) FieldError.NONE else FieldError("Port entre 1 et 65535."),
        )
    }

    fun onPollingIntervalChange(seconds: Int) {
        _uiState.value = _uiState.value.copy(
            pollingIntervalSeconds = seconds.coerceIn(
                PrinterSettings.MIN_POLLING_INTERVAL_SECONDS,
                PrinterSettings.MAX_POLLING_INTERVAL_SECONDS,
            ),
        )
    }

    fun onCutPaperToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(cutPaperEnabled = enabled)
    }

    fun onCashDrawerToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(cashDrawerEnabled = enabled)
    }

    fun onTicketWidthChange(width: TicketWidth) {
        _uiState.value = _uiState.value.copy(ticketWidth = width)
    }

    fun onDeveloperModeToggle(enabled: Boolean) {
        // Defense en profondeur : meme si ce ViewModel etait un jour reutilise depuis un
        // ecran qui oublierait de verifier le build type, un APK release ne peut jamais
        // activer le clear-text HTTP depuis l'UI — BuildConfig.ALLOW_CLEARTEXT_HTTP est
        // figé par build.gradle.kts (debug=true, release=false), pas par une preference
        // modifiable. network_security_config.xml applique la meme regle au niveau OS.
        val effectiveEnabled = enabled && ch.planetebowl.printagent.BuildConfig.ALLOW_CLEARTEXT_HTTP
        _uiState.value = _uiState.value.copy(developerModeEnabled = effectiveEnabled)
        onApiBaseUrlChange(_uiState.value.apiBaseUrl)
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.isFormValid) return
        viewModelScope.launch {
            settingsRepository.updateSettings(
                PrinterSettings(
                    apiBaseUrl = state.apiBaseUrl.trim(),
                    restaurantId = state.restaurantId.trim(),
                    storeName = state.storeName.ifBlank { PrinterSettings.defaults().storeName },
                    printerIp = state.printerIp.trim(),
                    printerPort = state.printerPortText.toIntOrNull() ?: PrinterSettings.DEFAULT_PRINTER_PORT,
                    pollingIntervalSeconds = state.pollingIntervalSeconds,
                    cutPaperEnabled = state.cutPaperEnabled,
                    cashDrawerEnabled = state.cashDrawerEnabled,
                    ticketWidth = state.ticketWidth,
                    developerModeEnabled = state.developerModeEnabled,
                ),
            )
            if (state.tokenInput.isNotBlank()) {
                settingsRepository.setToken(state.tokenInput.trim())
            }
            _uiState.value = _uiState.value.copy(
                tokenInput = "",
                hasStoredToken = settingsRepository.hasToken(),
                savedConfirmationVisible = true,
            )
        }
    }

    fun onSavedConfirmationShown() {
        _uiState.value = _uiState.value.copy(savedConfirmationVisible = false)
    }

    fun onTestConnection() {
        val state = _uiState.value
        val port = state.printerPortText.toIntOrNull() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testConnectionInProgress = true, testConnectionResult = null)
            val result = testConnectionUseCase(state.printerIp.trim(), port)
            _uiState.value = _uiState.value.copy(testConnectionInProgress = false, testConnectionResult = result.userMessage())
        }
    }

    fun onTestPrint() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testPrintInProgress = true, testPrintResult = null)
            val settings = settingsRepository.getSettings()
            val result = testPrintUseCase(settings)
            _uiState.value = _uiState.value.copy(testPrintInProgress = false, testPrintResult = result.userMessage())
        }
    }

    fun onStartAgent() {
        onSave()
        ContextCompat.startForegroundService(context, Intent(context, PrinterForegroundService::class.java))
    }

    fun onStopAgent() {
        val intent = Intent(context, PrinterForegroundService::class.java).apply {
            action = PrinterForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
