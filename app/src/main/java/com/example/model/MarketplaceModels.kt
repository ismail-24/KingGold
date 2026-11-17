package com.example.model

enum class MerchantStatus(val arabicLabel: String) {
    PENDING("قيد المراجعة"),
    APPROVED("معتمد"),
    REJECTED("مرفوض"),
    SUSPENDED("موقوف"),
}

enum class MerchantProductStatus(val arabicLabel: String) {
    PENDING_REVIEW("قيد مراجعة الإدارة"),
    ACTIVE("منشور للبيع"),
    PAUSED("متوقف مؤقتاً"),
    REJECTED("مرفوض"),
}

data class MerchantStore(
    val id: String = "",
    val ownerUid: String = "",
    val shopName: String = "",
    val area: String = "",
    val address: String = "",
    val contactPhone: String = "",
    val verifiedEmail: String = "",
    val description: String = "",
    val rating: Double = 5.0,
    val reviewsCount: Int = 0,
    val status: MerchantStatus = MerchantStatus.PENDING,
    val rejectionReason: String? = null,
    val prepaidMakingApproved: Boolean = false,
    val prepaidMakingBudgetPiasters: Long = 0L,
    val pricingPolicyNote: String? = null,
) {
    fun toShop(): Shop = Shop(
        id = id,
        name = shopName,
        area = area,
        address = address,
        phone = contactPhone,
        rating = rating,
        reviews = reviewsCount,
        distanceKm = 2.0,
        verified = status == MerchantStatus.APPROVED,
        closesAt = "10:00 م",
        makingFeePerGram = 0.0,
        ownerUid = ownerUid,
        isDynamicMerchant = true,
    )
}

data class MerchantPrivateDocs(
    val ownerUid: String = "",
    val legalFullName: String = "",
    val nationalIdNumber: String = "",
    val commercialRegNumber: String = "",
    val taxCardNumber: String = "",
    val authorizationNotes: String = "",
    val documentThumbnails: List<String> = emptyList(),
)

data class MerchantAuditLog(
    val id: String = "",
    val merchantId: String = "",
    val merchantOwnerUid: String = "",
    val actorUid: String = "",
    val action: String = "",
    val note: String = "",
    val createdAtMs: Long = 0L,
)

data class MerchantProduct(
    val id: String = "",
    val merchantId: String = "",
    val ownerUid: String = "",
    val merchantName: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val karat: Int = 21,
    val weightMg: Long = 0L,
    val weightGrams: Double = 0.0,
    val sellingPricePiasters: Long = 0L,
    val stockQuantity: Int = 0,
    val stamped: Boolean = true,
    val imageThumbnails: List<String> = emptyList(),
    val status: MerchantProductStatus = MerchantProductStatus.PENDING_REVIEW,
    val rejectionReason: String? = null,
) {
    fun toProduct(): Product = Product(
        id = id,
        shopId = merchantId,
        name = name,
        category = category,
        karat = karat,
        weightGrams = weightGrams,
        stamped = stamped,
        availablePieces = stockQuantity,
        sellingPricePiasters = sellingPricePiasters,
        description = description,
        imageThumbnails = imageThumbnails,
        merchantOwnerUid = ownerUid,
    )
}

data class MerchantOrderView(
    val orderId: String = "",
    val merchantId: String = "",
    val merchantOwnerUid: String = "",
    val buyerUid: String = "",
    val items: List<CartItem> = emptyList(),
    val totalPiasters: Long = 0L,
    val pickupWindow: String = "",
    val status: OrderStatus = OrderStatus.READY_FOR_PICKUP,
    val createdAtMs: Long = 0L,
)
