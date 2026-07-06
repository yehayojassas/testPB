package ch.planetebowl.printagent.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stockage chiffre (Android Keystore via MasterKey AES256-GCM) du token API. Volontairement
 * separe des reglages DataStore (PrinterSettings) : le token ne doit jamais transiter par
 * un backup non chiffre, un log de reglages, ou une table Room.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
    masterKey: MasterKey,
) {

    private val preferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getToken(): String? = preferences.getString(KEY_TOKEN, null)

    fun setToken(token: String) {
        preferences.edit().putString(KEY_TOKEN, token).apply()
    }

    fun clearToken() {
        preferences.edit().remove(KEY_TOKEN).apply()
    }

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    private companion object {
        const val FILE_NAME = "print_agent_secure_prefs"
        const val KEY_TOKEN = "api_bearer_token"
    }
}
