package ch.planetebowl.printagent.data.remote

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Fabrique un HttpLoggingInterceptor qui ne peut jamais afficher le token en clair, meme
 * si un futur changement de niveau de log (ex: passage a BODY par erreur) rendait le
 * corps ou d'autres en-tetes visibles.
 *
 * Deux niveaux de defense :
 *  1. `redactHeader("Authorization")` : mecanisme standard OkHttp, remplace la valeur de
 *     l'en-tete par "██" dans les logs, quel que soit le Level configure.
 *  2. Un [HttpLoggingInterceptor.Logger] custom qui repasse en plus une regex sur CHAQUE
 *     ligne loggee pour ecraser tout ce qui ressemble a "Bearer xxx" — au cas ou le token
 *     apparaitrait ailleurs qu'un en-tete Authorization standard (ex: dans une query
 *     string de debug ajoutee par erreur plus tard).
 */
object RedactingLoggingInterceptor {

    private val BEARER_TOKEN_PATTERN = Regex("Bearer\\s+\\S+")

    /** Extrait pour etre testable unitairement sans dependre d'android.util.Log. */
    fun redact(message: String): String = BEARER_TOKEN_PATTERN.replace(message, "Bearer REDACTED")

    fun create(level: HttpLoggingInterceptor.Level, tag: String = "PrintAgentHttp"): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor.Logger { message -> Log.d(tag, redact(message)) }
        return HttpLoggingInterceptor(logger).apply {
            setLevel(level)
            redactHeader("Authorization")
        }
    }
}
