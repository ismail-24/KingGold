package com.example.data.remote

import com.example.model.CartItem
import com.example.model.GoldOrder
import com.example.model.KycStatus
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Shop
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class OrderItemRecord(
    val productId: String = "",
    val shopId: String = "",
    val name: String = "",
    val category: String = "",
    val karat: Long = 21L,
    val weightGrams: Double = 0.0,
    val stamped: Boolean = true,
    val quantity: Long = 1L,
    val sellingPricePiasters: Long = 0L,
) {
    fun toCartItem(): CartItem = CartItem(
        product = Product(
            id = productId,
            shopId = shopId,
            name = name,
            category = category,
            karat = karat.toInt(),
            weightGrams = weightGrams,
            stamped = stamped,
            availablePieces = quantity.toInt().coerceAtLeast(1),
            sellingPricePiasters = sellingPricePiasters,
        ),
        quantity = quantity.toInt().coerceAtLeast(1),
    )

    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "shopId" to shopId,
        "name" to name,
        "category" to category,
        "karat" to karat,
        "weightGrams" to weightGrams,
        "stamped" to stamped,
        "quantity" to quantity,
        "sellingPricePiasters" to sellingPricePiasters,
    )

    companion object {
        fun fromCartItem(item: CartItem): OrderItemRecord = OrderItemRecord(
            productId = item.product.id,
            shopId = item.product.shopId,
            name = item.product.name,
            category = item.product.category,
            karat = item.product.karat.toLong(),
            weightGrams = item.product.weightGrams,
            stamped = item.product.stamped,
            quantity = item.quantity.toLong(),
            sellingPricePiasters = item.product.sellingPricePiasters,
        )
    }
}

@IgnoreExtraProperties
data class GoldOrderRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val merchantOwnerUid: String? = null,
    val items: List<OrderItemRecord> = emptyList(),
    val goldValue: Double = 0.0,
    val makingFee: Double = 0.0,
    val serviceFee: Double = 0.0,
    val totalPiasters: Long? = null,
    val paymentSource: String? = null,
    val idempotencyKey: String? = null,
    val pickupWindow: String = "",
    val status: String = OrderStatus.READY_FOR_PICKUP.name,
    val ratingGiven: Long? = null,
    val disputeReason: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toGoldOrder(shops: List<Shop>): GoldOrder {
        val matchedShop = shops.firstOrNull { it.id == shopId } ?: Shop(
            id = shopId,
            name = shopName.ifBlank { "محل شريك" },
            area = "القاهرة",
            address = "فرع معتمد",
            phone = "+201001234567",
            rating = 4.8,
            reviews = 50,
            distanceKm = 2.0,
            verified = true,
            closesAt = "9:00 م",
            makingFeePerGram = 0.0,
            ownerUid = merchantOwnerUid.orEmpty(),
        )
        val parsedStatus = runCatching { OrderStatus.valueOf(status) }
            .getOrDefault(OrderStatus.READY_FOR_PICKUP)
        return GoldOrder(
            id = id,
            shop = matchedShop,
            items = items.map { it.toCartItem() },
            goldValue = goldValue,
            makingFee = makingFee,
            serviceFee = serviceFee,
            pickupWindow = pickupWindow,
            status = parsedStatus,
            ratingGiven = ratingGiven?.toInt(),
            disputeReason = disputeReason,
            totalPiasters = totalPiasters ?: 0L,
            paymentSource = paymentSource ?: "ESCROW_METHOD",
        )
    }

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "shopId" to shopId,
            "shopName" to shopName,
            "merchantOwnerUid" to merchantOwnerUid?.takeIf { it.isNotBlank() },
            "items" to items.map { it.toMap() },
            "goldValue" to goldValue,
            "makingFee" to makingFee,
            "serviceFee" to serviceFee,
            "totalPiasters" to totalPiasters,
            "paymentSource" to paymentSource?.takeIf { it.isNotBlank() },
            "idempotencyKey" to idempotencyKey?.takeIf { it.isNotBlank() },
            "pickupWindow" to pickupWindow,
            "status" to status,
            "ratingGiven" to ratingGiven,
            "disputeReason" to disputeReason,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    companion object {
        fun fromGoldOrder(
            userId: String,
            order: GoldOrder,
            idempotencyKey: String? = null,
        ): GoldOrderRecord = GoldOrderRecord(
            id = order.id,
            userId = userId,
            shopId = order.shop.id,
            shopName = order.shop.name,
            merchantOwnerUid = order.shop.ownerUid.takeIf { it.isNotBlank() },
            items = order.items.map { OrderItemRecord.fromCartItem(it) },
            goldValue = order.goldValue,
            makingFee = order.makingFee,
            serviceFee = order.serviceFee,
            totalPiasters = order.totalPiasters.takeIf { it > 0L },
            paymentSource = order.paymentSource.takeIf { it.isNotBlank() },
            idempotencyKey = idempotencyKey,
            pickupWindow = order.pickupWindow,
            status = order.status.name,
            ratingGiven = order.ratingGiven?.toLong(),
            disputeReason = order.disputeReason,
        )
    }
}

@IgnoreExtraProperties
data class UserPreferencesRecord(
    val userId: String = "",
    val kycFrontDone: Boolean = true,
    val kycBackDone: Boolean = false,
    val kycSelfieDone: Boolean = false,
    val kycStatus: String = KycStatus.NOT_STARTED.name,
    val selectedPaymentMethodId: String = "card",
    val priceAlertEnabled: Boolean = true,
    val priceAlertAbove: Boolean = false,
    val priceAlertKarat: Long = 24L,
    val priceAlertTarget: String = "6000",
    val fcmToken: String? = null,
    val favoriteShopIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun parsedKycStatus(): KycStatus =
        runCatching { KycStatus.valueOf(kycStatus) }.getOrDefault(KycStatus.NOT_STARTED)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "kycFrontDone" to kycFrontDone,
            "kycBackDone" to kycBackDone,
            "kycSelfieDone" to kycSelfieDone,
            "kycStatus" to kycStatus,
            "selectedPaymentMethodId" to selectedPaymentMethodId,
            "priceAlertEnabled" to priceAlertEnabled,
            "priceAlertAbove" to priceAlertAbove,
            "priceAlertKarat" to priceAlertKarat,
            "priceAlertTarget" to priceAlertTarget,
            "fcmToken" to fcmToken?.takeIf { it.isNotBlank() },
            "favoriteShopIds" to favoriteShopIds,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "kycFrontDone" to kycFrontDone,
            "kycBackDone" to kycBackDone,
            "kycSelfieDone" to kycSelfieDone,
            "kycStatus" to kycStatus,
            "selectedPaymentMethodId" to selectedPaymentMethodId,
            "priceAlertEnabled" to priceAlertEnabled,
            "priceAlertAbove" to priceAlertAbove,
            "priceAlertKarat" to priceAlertKarat,
            "priceAlertTarget" to priceAlertTarget,
            "fcmToken" to fcmToken?.takeIf { it.isNotBlank() },
            "favoriteShopIds" to favoriteShopIds,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}
