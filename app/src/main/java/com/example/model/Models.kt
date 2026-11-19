package com.example.model

enum class ProductCategory(val arabicLabel: String) {
    COINS_AND_BARS("جنيهات وسبائك"),
    RINGS("خواتم"),
    BRACELETS("أساور"),
    CHAINS("سلاسل"),
}

data class KaratPrice(
    val karat: Int, // 24, 21, 18, or 0 for Sovereign Gold Pound
    val label: String,
    val pricePerGram: Double,
    val changePercent: Double,
)

data class GoldPriceHistoryPoint(
    val dayLabel: String,
    val pricePerGram: Double,
)

data class Shop(
    val id: String,
    val name: String,
    val area: String,
    val address: String,
    val phone: String,
    val rating: Double,
    val reviews: Int,
    val distanceKm: Double,
    val verified: Boolean,
    val closesAt: String,
    val workingHours: String = "السبت – الخميس: 10:00 ص – $closesAt · الجمعة: 2:00 م – $closesAt",
    val makingFeePerGram: Double,
    val ownerUid: String = "",
    val isDynamicMerchant: Boolean = false,
    val city: String = "القاهرة",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

data class Product(
    val id: String,
    val shopId: String,
    val name: String,
    val category: String,
    val karat: Int,
    val weightGrams: Double,
    val stamped: Boolean,
    val availablePieces: Int,
    val sellingPricePiasters: Long = 0L,
    val description: String = "",
    val imageThumbnails: List<String> = emptyList(),
    val merchantOwnerUid: String = "",
)

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
)

enum class OrderStatus(val arabicLabel: String) {
    PAID("تم الدفع"),
    PREPARING("قيد التجهيز"),
    READY_FOR_PICKUP("جاهز للاستلام"),
    COMPLETED("مكتمل"),
    DISPUTED("تحت المراجعة"),
}

data class GoldOrder(
    val id: String,
    val shop: Shop,
    val items: List<CartItem>,
    val goldValue: Double,
    val makingFee: Double,
    val serviceFee: Double,
    val pickupWindow: String,
    val status: OrderStatus = OrderStatus.PAID,
    val ratingGiven: Int? = null,
    val disputeReason: String? = null,
    val totalPiasters: Long = 0L,
    val paymentSource: String = "ESCROW_METHOD",
) {
    val total: Double
        get() = if (totalPiasters > 0L) MoneyUtils.piastersToEgp(totalPiasters) else goldValue + makingFee + serviceFee
}

enum class ShopSortOption(val label: String) {
    NEAREST("الأقرب"),
    HIGHEST_RATED("الأعلى تقييماً"),
    LOWEST_MAKING_FEE("أقل مصنعية"),
}

enum class KycStatus(val label: String) {
    NOT_STARTED("غير موثّق"),
    PENDING_REVIEW("قيد المراجعة"),
    VERIFIED("موثّق ✓"),
}

data class PaymentMethod(
    val id: String,
    val title: String,
    val subtitle: String,
)
