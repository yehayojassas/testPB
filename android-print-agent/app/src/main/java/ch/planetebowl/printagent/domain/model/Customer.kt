package ch.planetebowl.printagent.domain.model

data class Customer(
    val name: String,
    val phone: String?,
)

/** Prevu par le contrat (`address` peut etre non-null) mais non utilise cote metier
 * actuellement : seul TAKEAWAY existe aujourd'hui, sans notion de livraison. */
data class DeliveryAddress(
    val rawText: String,
)
