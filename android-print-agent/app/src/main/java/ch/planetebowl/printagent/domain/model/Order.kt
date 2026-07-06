package ch.planetebowl.printagent.domain.model

import java.time.Instant

data class Order(
    val orderId: String,
    val orderCode: String,
    val payloadVersion: Int,
    val createdAt: Instant,
    val orderType: OrderType,
    val pickupSlot: String?,
    val customer: Customer,
    val address: DeliveryAddress?,
    val items: List<OrderItem>,
    val total: Money,
    val kitchenNote: String?,
    val restaurantId: String,
)
