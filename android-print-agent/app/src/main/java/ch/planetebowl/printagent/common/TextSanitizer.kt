package ch.planetebowl.printagent.common

import java.text.Normalizer

/**
 * Normalisation texte partagee par le ticket ESC/POS (fallback ASCII, l'imprimante generique
 * ne supporte pas forcement une table de caracteres accentues) et par les logs (on tronque
 * systematiquement pour eviter qu'un champ client trop long ne pollue print_logs).
 */
object TextSanitizer {

    private const val MAX_LOG_MESSAGE_LENGTH = 500

    /**
     * Retire les accents (NFD + suppression des marques diacritiques) puis filtre tout
     * caractere hors de l'intervalle ASCII imprimable [0x20-0x7E]. Reprend la logique de
     * l'ancien script PowerShell qui alimentait l'imprimante Windows, portee en Kotlin.
     */
    fun toAsciiPrintable(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutDiacritics = decomposed.replace(Regex("\\p{Mn}+"), "")
        return withoutDiacritics.filter { it.code in 0x20..0x7E }
    }

    /** Coupe proprement un texte a [width] colonnes en respectant les frontieres de mots. */
    fun wordWrap(input: String, width: Int): List<String> {
        if (width <= 0) return listOf(input)
        val words = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidateWord = if (word.length > width) word.chunked(width) else listOf(word)
            for (chunk in candidateWord) {
                if (current.isEmpty()) {
                    current.append(chunk)
                } else if (current.length + 1 + chunk.length <= width) {
                    current.append(' ').append(chunk)
                } else {
                    lines.add(current.toString())
                    current = StringBuilder(chunk)
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    /**
     * Nettoie un message destine a print_logs/lastErrorMessage : jamais de token, jamais de
     * ligne interminable. Ne fait PAS de detection de secrets par contenu — c'est la
     * responsabilite de l'appelant de ne jamais passer le token brut ici ; voir
     * [RedactingLoggingInterceptor][ch.planetebowl.printagent.data.remote.RedactingLoggingInterceptor]
     * pour le cas HTTP specifique.
     */
    fun sanitizeForLog(message: String?): String {
        if (message.isNullOrBlank()) return ""
        val singleLine = message.replace(Regex("\\s+"), " ").trim()
        return if (singleLine.length > MAX_LOG_MESSAGE_LENGTH) {
            singleLine.take(MAX_LOG_MESSAGE_LENGTH) + "…"
        } else {
            singleLine
        }
    }
}
