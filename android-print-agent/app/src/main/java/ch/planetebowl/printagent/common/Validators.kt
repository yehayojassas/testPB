package ch.planetebowl.printagent.common

import java.util.UUID

/**
 * Validations partagees par l'ecran Reglages (feedback immediat dans l'UI) et par les
 * repositories (defense en profondeur avant persistence/envoi reseau).
 */
object Validators {

    private val IPV4_REGEX = Regex(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )

    fun isValidIpv4(value: String): Boolean = IPV4_REGEX.matches(value.trim())

    fun isValidPort(value: Int): Boolean = value in 1..65535

    fun isValidUuid(value: String): Boolean = try {
        UUID.fromString(value.trim())
        true
    } catch (e: IllegalArgumentException) {
        false
    }

    /**
     * Valide l'URL de base de l'API. En dehors du mode developpement, http:// est refuse
     * meme si l'URL est par ailleurs bien formee : c'est la barriere applicative qui
     * complete network_security_config.xml (laquelle bloque deja le clear-text en release
     * au niveau OS).
     */
    fun validateApiBaseUrl(value: String, developerModeEnabled: Boolean): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ValidationResult.Invalid("L'URL ne peut pas être vide.")
        val uri = try {
            java.net.URI(trimmed)
        } catch (e: Exception) {
            return ValidationResult.Invalid("URL mal formée.")
        }
        return when (uri.scheme?.lowercase()) {
            "https" -> ValidationResult.Valid
            "http" -> if (developerModeEnabled) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid("http:// est refusé hors mode développement, utilisez https://.")
            }
            else -> ValidationResult.Invalid("L'URL doit commencer par https:// (ou http:// en mode développement).")
        }
    }

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
