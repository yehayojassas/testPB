package ch.planetebowl.printagent.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.planetebowl.printagent.data.security.SecureTokenStore
import ch.planetebowl.printagent.domain.model.PrinterSettings
import ch.planetebowl.printagent.domain.model.TicketWidth
import ch.planetebowl.printagent.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureTokenStore: SecureTokenStore,
) : SettingsRepository {

    private object Keys {
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val RESTAURANT_ID = stringPreferencesKey("restaurant_id")
        val STORE_NAME = stringPreferencesKey("store_name")
        val PRINTER_IP = stringPreferencesKey("printer_ip")
        val PRINTER_PORT = intPreferencesKey("printer_port")
        val POLLING_INTERVAL_SECONDS = intPreferencesKey("polling_interval_seconds")
        val CUT_PAPER_ENABLED = booleanPreferencesKey("cut_paper_enabled")
        val CASH_DRAWER_ENABLED = booleanPreferencesKey("cash_drawer_enabled")
        val TICKET_WIDTH_COLUMNS = intPreferencesKey("ticket_width_columns")
        val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val LAST_SYNC_AT_EPOCH_MILLIS = longPreferencesKey("last_sync_at_epoch_millis")
        val LAST_ERROR_MESSAGE = stringPreferencesKey("last_error_message")
    }

    override fun observeSettings(): Flow<PrinterSettings> = dataStore.data.map(::toSettings)

    override suspend fun getSettings(): PrinterSettings = toSettings(dataStore.data.first())

    override suspend fun updateSettings(settings: PrinterSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.API_BASE_URL] = settings.apiBaseUrl
            prefs[Keys.RESTAURANT_ID] = settings.restaurantId
            prefs[Keys.STORE_NAME] = settings.storeName
            prefs[Keys.PRINTER_IP] = settings.printerIp
            prefs[Keys.PRINTER_PORT] = settings.printerPort
            prefs[Keys.POLLING_INTERVAL_SECONDS] = settings.pollingIntervalSeconds
            prefs[Keys.CUT_PAPER_ENABLED] = settings.cutPaperEnabled
            prefs[Keys.CASH_DRAWER_ENABLED] = settings.cashDrawerEnabled
            prefs[Keys.TICKET_WIDTH_COLUMNS] = settings.ticketWidth.columns
            prefs[Keys.DEVELOPER_MODE_ENABLED] = settings.developerModeEnabled
        }
    }

    override fun getToken(): String? = secureTokenStore.getToken()
    override fun setToken(token: String) = secureTokenStore.setToken(token)
    override fun hasToken(): Boolean = secureTokenStore.hasToken()

    override suspend fun setLastSyncAt(instant: Instant) {
        dataStore.edit { prefs -> prefs[Keys.LAST_SYNC_AT_EPOCH_MILLIS] = instant.toEpochMilli() }
    }

    override fun observeLastSyncAt(): Flow<Instant?> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_SYNC_AT_EPOCH_MILLIS]?.let { Instant.ofEpochMilli(it) }
    }

    override suspend fun setLastError(sanitizedMessage: String?) {
        dataStore.edit { prefs ->
            if (sanitizedMessage.isNullOrBlank()) {
                prefs.remove(Keys.LAST_ERROR_MESSAGE)
            } else {
                prefs[Keys.LAST_ERROR_MESSAGE] = sanitizedMessage
            }
        }
    }

    override fun observeLastError(): Flow<String?> = dataStore.data.map { prefs -> prefs[Keys.LAST_ERROR_MESSAGE] }

    private fun toSettings(prefs: Preferences): PrinterSettings {
        val defaults = PrinterSettings.defaults()
        val widthColumns = prefs[Keys.TICKET_WIDTH_COLUMNS] ?: defaults.ticketWidth.columns
        return PrinterSettings(
            apiBaseUrl = prefs[Keys.API_BASE_URL] ?: defaults.apiBaseUrl,
            restaurantId = prefs[Keys.RESTAURANT_ID] ?: defaults.restaurantId,
            storeName = prefs[Keys.STORE_NAME] ?: defaults.storeName,
            printerIp = prefs[Keys.PRINTER_IP] ?: defaults.printerIp,
            printerPort = prefs[Keys.PRINTER_PORT] ?: defaults.printerPort,
            pollingIntervalSeconds = prefs[Keys.POLLING_INTERVAL_SECONDS] ?: defaults.pollingIntervalSeconds,
            cutPaperEnabled = prefs[Keys.CUT_PAPER_ENABLED] ?: defaults.cutPaperEnabled,
            cashDrawerEnabled = prefs[Keys.CASH_DRAWER_ENABLED] ?: defaults.cashDrawerEnabled,
            ticketWidth = TicketWidth.entries.find { it.columns == widthColumns } ?: defaults.ticketWidth,
            developerModeEnabled = prefs[Keys.DEVELOPER_MODE_ENABLED] ?: defaults.developerModeEnabled,
        )
    }
}
