package ch.planetebowl.printagent.domain.repository

import ch.planetebowl.printagent.domain.model.PrinterSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<PrinterSettings>
    suspend fun getSettings(): PrinterSettings
    suspend fun updateSettings(settings: PrinterSettings)

    fun getToken(): String?
    fun setToken(token: String)
    fun hasToken(): Boolean

    suspend fun setLastSyncAt(instant: java.time.Instant)
    fun observeLastSyncAt(): Flow<java.time.Instant?>

    suspend fun setLastError(sanitizedMessage: String?)
    fun observeLastError(): Flow<String?>
}
