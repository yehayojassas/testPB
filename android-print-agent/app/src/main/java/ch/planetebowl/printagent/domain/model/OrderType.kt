package ch.planetebowl.printagent.domain.model

/**
 * Le backend n'emet aujourd'hui que TAKEAWAY. DINE_IN et DELIVERY sont prevus pour une
 * evolution future (voir cahier des charges) mais n'ont pas encore d'UI/formatage dedie :
 * ne pas construire d'ecran pour un type que le serveur ne produit pas encore.
 */
enum class OrderType {
    TAKEAWAY,
    DINE_IN,
    DELIVERY,
    UNKNOWN;

    companion object {
        fun fromApiValue(raw: String?): OrderType = when (raw) {
            "TAKEAWAY" -> TAKEAWAY
            "DINE_IN" -> DINE_IN
            "DELIVERY" -> DELIVERY
            else -> UNKNOWN
        }
    }
}
