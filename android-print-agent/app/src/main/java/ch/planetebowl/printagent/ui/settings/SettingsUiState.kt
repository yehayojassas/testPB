package ch.planetebowl.printagent.ui.settings

import ch.planetebowl.printagent.domain.model.TicketWidth
import ch.planetebowl.printagent.service.AgentStatus
import java.time.Instant

data class FieldError(val message: String?) {
    val hasError: Boolean get() = message != null

    companion object {
        val NONE = FieldError(null)
    }
}

/** Etat immutable de l'ecran Reglages. Le token n'est jamais expose en clair dans cet
 * etat au-dela de la saisie en cours : [tokenInput] reflete la zone de texte, mais
 * [hasStoredToken] (pas la valeur) indique si un token est deja enregistre. */
data class SettingsUiState(
    val apiBaseUrl: String = "",
    val apiBaseUrlError: FieldError = FieldError.NONE,
    val tokenInput: String = "",
    val tokenVisible: Boolean = false,
    val hasStoredToken: Boolean = false,
    val restaurantId: String = "",
    val restaurantIdError: FieldError = FieldError.NONE,
    val storeName: String = "",
    val printerIp: String = "",
    val printerIpError: FieldError = FieldError.NONE,
    val printerPortText: String = "9100",
    val printerPortError: FieldError = FieldError.NONE,
    val pollingIntervalSeconds: Int = 8,
    val cutPaperEnabled: Boolean = true,
    val cashDrawerEnabled: Boolean = false,
    val ticketWidth: TicketWidth = TicketWidth.WIDE_48,
    val developerModeEnabled: Boolean = false,
    val agentStatus: AgentStatus = AgentStatus.STOPPED,
    val lastSyncAt: Instant? = null,
    val lastError: String? = null,
    val testConnectionInProgress: Boolean = false,
    val testConnectionResult: String? = null,
    val testPrintInProgress: Boolean = false,
    val testPrintResult: String? = null,
    val savedConfirmationVisible: Boolean = false,
) {
    val isFormValid: Boolean
        get() = !apiBaseUrlError.hasError && !restaurantIdError.hasError &&
            !printerIpError.hasError && !printerPortError.hasError
}
