package ch.planetebowl.printagent.domain.model

/** Codes d'erreur metier renvoyes par le backend dans le corps JSON des 409. */
enum class ApiError {
    ALREADY_CLAIMED,
    ALREADY_PRINTED,
    MAX_ATTEMPTS_REACHED,
    CLAIM_MISMATCH,
    UNKNOWN;

    companion object {
        fun fromApiValue(raw: String?): ApiError = entries.find { it.name == raw } ?: UNKNOWN
    }
}
