package ch.planetebowl.printagent.data.remote.dto

import ch.planetebowl.printagent.data.remote.JobMapper
import ch.planetebowl.printagent.domain.model.OrderType
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class JobMapperTest {

    private val gson = Gson()

    // JSON exact du contrat GET /jobs (cahier des charges).
    private val contractJson = """
        {
          "orderId": "11111111-1111-1111-1111-111111111111",
          "orderCode": "PB-101",
          "payloadVersion": 1,
          "createdAt": "2026-07-06T11:32:00Z",
          "orderType": "TAKEAWAY",
          "pickupSlot": "12:30",
          "customerName": "Jean Dupont",
          "phone": "+41791234567",
          "address": null,
          "items": [
            { "name": "Bowl Signature Teriyaki", "quantity": 2, "unitPriceChf": "14.90",
              "customizations": { "base": "Riz", "protein": "Poulet", "garnish": ["Avocat"], "extra_protein": [], "topping": ["Graines de sésame"], "sauce": ["Soja"] } }
          ],
          "totalChf": "29.80",
          "kitchenNote": null,
          "restaurantId": "22222222-2222-2222-2222-222222222222"
        }
    """.trimIndent()

    @Test
    fun `maps every contract field to the domain model`() {
        val dto = gson.fromJson(contractJson, JobDto::class.java)
        val order = JobMapper.toDomain(dto)

        assertEquals("11111111-1111-1111-1111-111111111111", order.orderId)
        assertEquals("PB-101", order.orderCode)
        assertEquals(Instant.parse("2026-07-06T11:32:00Z"), order.createdAt)
        assertEquals(OrderType.TAKEAWAY, order.orderType)
        assertEquals("12:30", order.pickupSlot)
        assertEquals("Jean Dupont", order.customer.name)
        assertEquals("+41791234567", order.customer.phone)
        assertEquals(null, order.address)
        assertEquals("22222222-2222-2222-2222-222222222222", order.restaurantId)
    }

    @Test
    fun `amounts are parsed as exact BigDecimal, never Double`() {
        val dto = gson.fromJson(contractJson, JobDto::class.java)
        val order = JobMapper.toDomain(dto)

        assertEquals(BigDecimal("29.80"), order.total.amount)
        assertEquals(BigDecimal("14.90"), order.items.single().unitPrice.amount)
        // Garantie qu'un flottant binaire n'a jamais ete introduit dans le chemin de parsing.
        assertEquals(0, BigDecimal("29.80").compareTo(order.total.amount))
    }

    @Test
    fun `customizations map to labeled groups, empty lists are omitted`() {
        val dto = gson.fromJson(contractJson, JobDto::class.java)
        val order = JobMapper.toDomain(dto)
        val groups = order.items.single().customizations.groups.associateBy { it.label }

        assertEquals(listOf("Riz"), groups.getValue("Base").values)
        assertEquals(listOf("Poulet"), groups.getValue("Protéine").values)
        assertEquals(listOf("Avocat"), groups.getValue("Garniture").values)
        assertEquals(listOf("Soja"), groups.getValue("Sauce").values)
        // "extra_protein": [] est vide dans le contrat : ne doit pas produire de groupe vide.
        assertTrue(!groups.containsKey("Supplément protéine"))
    }

    @Test
    fun `an unparseable date falls back to epoch instead of throwing`() {
        val brokenJson = contractJson.replace("\"2026-07-06T11:32:00Z\"", "\"not-a-date\"")
        val dto = gson.fromJson(brokenJson, JobDto::class.java)
        val order = JobMapper.toDomain(dto)
        assertEquals(Instant.EPOCH, order.createdAt)
    }

    @Test
    fun `unknown order type maps to UNKNOWN rather than crashing`() {
        val futureJson = contractJson.replace("\"TAKEAWAY\"", "\"DINE_IN_FUTURE_TYPE\"")
        val dto = gson.fromJson(futureJson, JobDto::class.java)
        val order = JobMapper.toDomain(dto)
        assertEquals(OrderType.UNKNOWN, order.orderType)
    }
}
