package dev.eliaschen.tasty.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Food(
    val id: Int, // 1001,
    val name: String, // "布里乳酪",
    val price: Float, // 33,
    val remark: String, // "店長激推招牌必點，網友好評回購款",
    @SerialName("type_id") val typeId: Int, // 1,
    @SerialName("image_url") val imageUrl: String, // "type_1_01.png",
    val stock: Boolean, // true
)

@Serializable
data class FoodType(
    val name: String, // "乳酪與麵包",
    val id: Int, // 1,
    val cover: String, // "bread.jpg",
    val description: String, // "傳承法式烘焙與經典乳酪工藝，提供口感扎實、乳香濃郁的每日現烤歐式麵包與嚴選乳酪拼盤。"
)

@Serializable
data class CartItem(
    val id: Int,
    val count: Int
)

enum class Payment(val displayName: String) {
    @SerialName("cash")
    Cash("現金"),

    @SerialName("credit_card")
    CreditCard("信用卡")
}

enum class OrderStatus(val displayName: String, val colorHex: Long) {
    @SerialName("pending")
    Pending("待處理", 0xFF9E9E9E),

    @SerialName("preparing")
    Preparing("製作中", 0xFFFF9800),

    @SerialName("delivering")
    Delivering("配送中", 0xFF42A5F5),

    @SerialName("completed")
    Completed("已完成", 0xFF43A047),
}

@Serializable
data class Order(
    val address: String,
    val note: String,
    val payment: Payment,
    val totalPrice: Float,
    val items: List<CartItem>,
)

@Serializable
data class PlacedOrder(
    val id: Int,
    val address: String = "",
    val note: String = "",
    val payment: Payment = Payment.Cash,
    val totalPrice: Float = 0f,
    val items: List<CartItem> = emptyList(),
    val status: OrderStatus? = null,
    val email: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AgentMessage(
    val author: String,
    val type: String,
    val content: String,
)

@Serializable
data class AgentChatRequest(
    val messages: List<AgentMessage>,
)

@Serializable
data class UpdateAddressRequest(
    val address: String,
)
