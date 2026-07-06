package ch.planetebowl.printagent.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO miroir exact du contrat GET /jobs (voir cahier des charges backend). Les montants
 * restent des String jusqu'au mapping domain (JobMapper) : ne JAMAIS les typer en Double
 * ici, Gson les deserialiserait sans erreur mais on perdrait la garantie decimale exacte.
 */
data class JobDto(
    val orderId: String,
    val orderCode: String,
    val payloadVersion: Int,
    val createdAt: String,
    val orderType: String,
    val pickupSlot: String?,
    val customerName: String,
    val phone: String?,
    val address: String?,
    val items: List<OrderItemDto>,
    val totalChf: String,
    val kitchenNote: String?,
    val restaurantId: String,
)

data class OrderItemDto(
    val name: String,
    val quantity: Int,
    val unitPriceChf: String,
    val customizations: CustomizationsDto?,
)

/**
 * Champs connus du contrat actuel. base/protein sont des choix uniques (String), les
 * autres groupes sont multi-valeurs (List<String>). Le JSON brut complet est de toute
 * facon conserve tel quel dans PrintJobEntity.payloadJson : un futur champ non mappe ici
 * n'est donc pas perdu, seulement pas encore affiche sur le ticket.
 */
data class CustomizationsDto(
    val base: String? = null,
    val protein: String? = null,
    val garnish: List<String>? = null,
    @SerializedName("extra_protein") val extraProtein: List<String>? = null,
    val topping: List<String>? = null,
    val sauce: List<String>? = null,
)
