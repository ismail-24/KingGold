package com.example.model

enum class ProductCategory(
    val arabicLabel: String,
    val legacyAliases: Set<String> = emptySet(),
) {
    COINS("جنيهات وعملات ذهبية", setOf("جنيهات وسبائك", "جنيهات ذهب", "عملات")),
    BARS_BULLION("سبائك ذهبية", setOf("سبائك", "جنيهات وسبائك")),
    BRACELETS("أساور وغوايش", setOf("أساور", "غوايش")),
    RINGS("خواتم", setOf("دبل", "خواتم ذهب")),
    CHAINS("سلاسل وقلائد", setOf("سلاسل", "قلائد")),
    EARRINGS("حلقان وأقراط", setOf("حلقان", "أقراط")),
    JEWELRY_SETS("أطقم كاملة", setOf("أطقم", "شبكة")),
    DESIGNS("تصاميم وتعليقات", setOf("تعليقات", "بروشات", "تصاميم تراثية وعصرية"));

    fun matches(rawCategory: String): Boolean {
        val normalized = rawCategory.trim()
        if (normalized.isEmpty()) return false
        if (normalized.equals(arabicLabel, ignoreCase = true)) return true
        return legacyAliases.any { it.equals(normalized, ignoreCase = true) }
    }

    companion object {
        val allArabicLabels: List<String>
            get() = entries.map { it.arabicLabel }
    }
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

data class DailyGoldPrice(
    val dateKey: String,
    val dayLabel: String,
    val price24kEgp: Double,
    val price21kEgp: Double,
    val price18kEgp: Double,
    val source: String = "SEED",
) {
    fun priceForKarat(karat: Int): Double = when (karat) {
        24 -> price24kEgp
        21 -> price21kEgp
        18 -> price18kEgp
        else -> price24kEgp
    }
}

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
    val workingHours: String = if (closesAt.isNotBlank()) {
        "السبت – الخميس: 10:00 ص – $closesAt · الجمعة: 2:00 م – $closesAt"
    } else {
        ""
    },
    val makingFeePerGram: Double,
    val ownerUid: String = "",
    val isDynamicMerchant: Boolean = false,
    val city: String = "القاهرة",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
    val websiteUrl: String? = null,
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val hasVerifiedRating: Boolean = rating > 0.0 && reviews > 0,
    val hasVerifiedPhone: Boolean = phone.isNotBlank(),
    val hasVerifiedHours: Boolean = closesAt.isNotBlank() && workingHours.isNotBlank(),
    val verificationSourceLabel: String = when {
        isDynamicMerchant -> "تاجر معتمد على المنصة"
        verified -> "بيانات تجارية موثّقة"
        else -> "بيانات غير موثّقة"
    },
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
    val designStyle: String = "",
    val thumbnailUrl: String? = null,
    val imageUrl: String? = null,
    val galleryImageUrls: List<String> = emptyList(),
    val isPhotoVerifiedInventory: Boolean = imageThumbnails.isNotEmpty(),
    val imageSourceAttribution: String? = null,
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
