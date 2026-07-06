package ch.planetebowl.printagent.domain.model

/**
 * Options librement structurees d'un article (base, proteine, garnitures, sauces...). La
 * forme exacte des cles depend du menu et peut evoluer cote backend ; on la modelise donc
 * comme une liste de groupes nom -> valeurs plutot que des champs fixes, pour rester
 * tolerant a l'ajout d'un futur groupe de personnalisation sans casser le ticket.
 */
data class Customizations(
    val groups: List<CustomizationGroup>,
) {
    companion object {
        val EMPTY = Customizations(emptyList())
    }
}

data class CustomizationGroup(
    val label: String,
    val values: List<String>,
)

data class OrderItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Money,
    val customizations: Customizations,
) {
    val lineTotal: Money get() = unitPrice * quantity
}
