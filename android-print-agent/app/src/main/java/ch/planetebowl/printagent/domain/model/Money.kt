package ch.planetebowl.printagent.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Montant en CHF. Toujours construit a partir d'une chaine de caracteres (le contrat API
 * renvoie des strings, jamais des nombres flottants) pour eviter toute erreur d'arrondi
 * binaire IEEE-754 sur de l'argent — voir contrat GET /jobs (unitPriceChf, totalChf).
 */
@JvmInline
value class Money(val amount: BigDecimal) {

    fun formatChf(): String = "CHF ${amount.setScale(2, RoundingMode.HALF_UP)}"

    operator fun times(quantity: Int): Money = Money(amount.multiply(BigDecimal(quantity)))

    companion object {
        /** @throws NumberFormatException si [raw] n'est pas un decimal valide. */
        fun fromApiString(raw: String): Money = Money(BigDecimal(raw.trim()))
    }
}
