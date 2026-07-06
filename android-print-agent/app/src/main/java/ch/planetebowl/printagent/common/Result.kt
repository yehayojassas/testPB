package ch.planetebowl.printagent.common

/**
 * Resultat scelle generique utilise dans toute la couche domain/data pour eviter les
 * exceptions non typees a travers les frontieres de couches. Chaque cas d'usage renvoie
 * un [AppResult] plutot que de laisser fuir une exception vers l'appelant.
 *
 * onSuccess/onFailure/map sont des extensions top-level (et non des membres du sealed
 * class) : ecrites comme membres, le smart-cast de `value`/`error` vers le type generique
 * exact de l'appelant serait ambigu pour le compilateur — en extension generique, la
 * relation entre le T du receveur et celui de Success<T> est explicite et sans ambiguite.
 */
sealed class AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>()
    data class Failure(val error: AppFailure) : AppResult<Nothing>()
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(this.value)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppFailure) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(this.error)
    return this
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(this.value))
    is AppResult.Failure -> this
}

fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)
fun AppFailure.asFailure(): AppResult<Nothing> = AppResult.Failure(this)

/** Erreurs applicatives typees — jamais un message brut d'exception affiche a l'utilisateur. */
sealed class AppFailure(val userMessage: String) {
    data class Network(val detail: String) : AppFailure("Réseau indisponible : $detail")
    data class Http(val code: Int, val apiErrorCode: String?) : AppFailure(
        "Erreur serveur (HTTP $code${apiErrorCode?.let { " · $it" } ?: ""})"
    )
    data class Unexpected(val detail: String) : AppFailure("Erreur inattendue : $detail")
    data class Validation(val field: String, val reason: String) : AppFailure("$field : $reason")
    data object ClaimExpiredOrMismatch : AppFailure("Le jeton de réservation du ticket a expiré ou ne correspond plus.")
}
