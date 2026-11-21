package com.example.data.remote

import androidx.annotation.Keep
import com.example.model.CartItem
import com.example.model.DailyGoldPrice
import com.example.model.GoldOrder
import com.example.model.KycStatus
import com.example.model.MoneyUtils
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Shop
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
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

@Keep
@IgnoreExtraProperties
data class GoldShopLocationRecord(
    @DocumentId val id: String = "",
    val name: String = "",
    val city: String = "القاهرة",
    val area: String = "",
    val address: String = "",
    val phone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val coordinates: GeoPoint? = null,
    val rating: Double = 4.8,
    val reviews: Long = 0L,
    val distanceKm: Double = 1.5,
    val verified: Boolean = false,
    val closesAt: String = "9:00 م",
    val makingFeePerGram: Double = 90.0,
    val merchantId: String? = null,
    val ownerUid: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun resolvedLatitude(): Double = coordinates?.latitude ?: latitude

    fun resolvedLongitude(): Double = coordinates?.longitude ?: longitude

    fun toShop(): Shop = Shop(
        id = id,
        name = name.ifBlank { "محل ذهب شريك" },
        area = area.ifBlank { city },
        address = address.ifBlank { "$area، $city" },
        phone = phone.ifBlank { "+201001234567" },
        rating = rating.coerceIn(0.0, 5.0),
        reviews = reviews.toInt().coerceAtLeast(0),
        distanceKm = distanceKm.coerceAtLeast(0.0),
        verified = verified,
        closesAt = closesAt.ifBlank { "9:00 م" },
        makingFeePerGram = makingFeePerGram.coerceAtLeast(0.0),
        ownerUid = ownerUid,
        isDynamicMerchant = !merchantId.isNullOrBlank(),
        city = city.ifBlank { "القاهرة" },
        latitude = resolvedLatitude(),
        longitude = resolvedLongitude(),
    )

    fun toCreateMap(): Map<String, Any> {
        val lat = resolvedLatitude()
        val lng = resolvedLongitude()
        val raw = mutableMapOf<String, Any?>(
            "name" to name.trim(),
            "city" to city.trim().ifBlank { "القاهرة" },
            "area" to area.trim(),
            "address" to address.trim(),
            "phone" to phone.trim(),
            "latitude" to lat,
            "longitude" to lng,
            "coordinates" to coordinates,
            "rating" to rating,
            "reviews" to reviews,
            "distanceKm" to distanceKm,
            "verified" to verified,
            "closesAt" to closesAt.trim().ifBlank { "9:00 م" },
            "makingFeePerGram" to makingFeePerGram,
            "merchantId" to merchantId?.takeIf { it.isNotBlank() },
            "ownerUid" to ownerUid,
            "active" to active,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val lat = resolvedLatitude()
        val lng = resolvedLongitude()
        val raw = mutableMapOf<String, Any?>(
            "name" to name.trim(),
            "city" to city.trim().ifBlank { "القاهرة" },
            "area" to area.trim(),
            "address" to address.trim(),
            "phone" to phone.trim(),
            "latitude" to lat,
            "longitude" to lng,
            "coordinates" to coordinates,
            "closesAt" to closesAt.trim().ifBlank { "9:00 م" },
            "makingFeePerGram" to makingFeePerGram,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    companion object {
        fun fromShop(
            shop: Shop,
            ownerUid: String,
            merchantId: String? = null,
            active: Boolean = true,
            includeGeoPoint: Boolean = true,
        ): GoldShopLocationRecord = GoldShopLocationRecord(
            id = shop.id,
            name = shop.name,
            city = shop.city,
            area = shop.area,
            address = shop.address,
            phone = shop.phone,
            latitude = shop.latitude,
            longitude = shop.longitude,
            coordinates = if (includeGeoPoint && shop.latitude in -85.0..85.0 && shop.longitude in -180.0..180.0 && !(shop.latitude == 0.0 && shop.longitude == 0.0)) {
                GeoPoint(shop.latitude, shop.longitude)
            } else {
                null
            },
            rating = shop.rating,
            reviews = shop.reviews.toLong(),
            distanceKm = shop.distanceKm,
            verified = shop.verified,
            closesAt = shop.closesAt,
            makingFeePerGram = shop.makingFeePerGram,
            merchantId = merchantId,
            ownerUid = ownerUid,
            active = active,
        )
    }
}

@Keep
@IgnoreExtraProperties
data class DailyGoldPriceRecord(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val dayLabel: String = "",
    val price24kPerGramPiasters: Long = 620000L,
    val price21kPerGramPiasters: Long = 542500L,
    val price18kPerGramPiasters: Long = 465000L,
    val source: String = "SEED",
    val published: Boolean = true,
    val updatedByAdminUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDailyGoldPrice(): DailyGoldPrice {
        val resolvedKey = dateKey.ifBlank { id }
        val resolvedLabel = dayLabel.ifBlank { resolvedKey }
        return DailyGoldPrice(
            dateKey = resolvedKey,
            dayLabel = resolvedLabel,
            price24kEgp = MoneyUtils.piastersToEgp(price24kPerGramPiasters),
            price21kEgp = MoneyUtils.piastersToEgp(price21kPerGramPiasters),
            price18kEgp = MoneyUtils.piastersToEgp(price18kPerGramPiasters),
            source = source,
        )
    }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "dateKey" to dateKey.trim(),
        "dayLabel" to dayLabel.trim().ifBlank { dateKey.trim() },
        "price24kPerGramPiasters" to price24kPerGramPiasters,
        "price21kPerGramPiasters" to price21kPerGramPiasters,
        "price18kPerGramPiasters" to price18kPerGramPiasters,
        "source" to source.ifBlank { "ADMIN_PRICING" },
        "published" to published,
        "updatedByAdminUid" to updatedByAdminUid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "dayLabel" to dayLabel.trim().ifBlank { dateKey.trim() },
        "price24kPerGramPiasters" to price24kPerGramPiasters,
        "price21kPerGramPiasters" to price21kPerGramPiasters,
        "price18kPerGramPiasters" to price18kPerGramPiasters,
        "source" to source.ifBlank { "ADMIN_PRICING" },
        "published" to published,
        "updatedByAdminUid" to updatedByAdminUid,
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    companion object {
        fun fromDailyGoldPrice(
            daily: DailyGoldPrice,
            updatedByAdminUid: String,
            published: Boolean = true,
        ): DailyGoldPriceRecord {
            val p24 = MoneyUtils.egpToPiasters(daily.price24kEgp).coerceAtLeast(100000L)
            val p21 = MoneyUtils.egpToPiasters(daily.price21kEgp).coerceIn(100000L, p24 - 1L)
            val p18 = MoneyUtils.egpToPiasters(daily.price18kEgp).coerceIn(100000L, p21 - 1L)
            return DailyGoldPriceRecord(
                id = daily.dateKey,
                dateKey = daily.dateKey,
                dayLabel = daily.dayLabel,
                price24kPerGramPiasters = p24,
                price21kPerGramPiasters = p21,
                price18kPerGramPiasters = p18,
                source = daily.source.takeIf { it in setOf("SEED", "ADMIN_PRICING", "GLOBAL_FEED") } ?: "ADMIN_PRICING",
                published = published,
                updatedByAdminUid = updatedByAdminUid,
            )
        }
    }
}

