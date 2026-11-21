package com.example.data.remote

import androidx.annotation.Keep
import com.example.model.CartItem
import com.example.model.MerchantAuditLog
import com.example.model.MerchantOrderView
import com.example.model.MerchantPrivateDocs
import com.example.model.MerchantProduct
import com.example.model.MerchantProductStatus
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.model.OrderStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserCartRecord(
    val userId: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val merchantOwnerUid: String? = null,
    val items: List<OrderItemRecord> = emptyList(),
    val pickupSlotIndex: Long = 0L,
    val priceLockExpiresAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "shopId" to shopId,
            "shopName" to shopName,
            "merchantOwnerUid" to merchantOwnerUid?.takeIf { it.isNotBlank() },
            "items" to items.map { it.toMap() },
            "pickupSlotIndex" to pickupSlotIndex,
            "priceLockExpiresAtMs" to priceLockExpiresAtMs,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "shopId" to shopId,
            "shopName" to shopName,
            "merchantOwnerUid" to merchantOwnerUid?.takeIf { it.isNotBlank() },
            "items" to items.map { it.toMap() },
            "pickupSlotIndex" to pickupSlotIndex,
            "priceLockExpiresAtMs" to priceLockExpiresAtMs,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class MerchantStoreRecord(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val shopName: String = "",
    val area: String = "",
    val address: String = "",
    val contactPhone: String = "",
    val verifiedEmail: String = "",
    val description: String = "",
    val rating: Double = 5.0,
    val reviewsCount: Long = 0L,
    val status: String = MerchantStatus.PENDING.name,
    val rejectionReason: String? = null,
    val prepaidMakingApproved: Boolean = false,
    val prepaidMakingBudgetPiasters: Long = 0L,
    val pricingPolicyNote: String? = null,
    val city: String = "القاهرة",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): MerchantStore = MerchantStore(
        id = id,
        ownerUid = ownerUid,
        shopName = shopName,
        area = area,
        address = address,
        contactPhone = contactPhone,
        verifiedEmail = verifiedEmail,
        description = description,
        rating = rating,
        reviewsCount = reviewsCount.toInt(),
        status = enumValues<MerchantStatus>().firstOrNull { it.name == status } ?: MerchantStatus.PENDING,
        rejectionReason = rejectionReason,
        prepaidMakingApproved = prepaidMakingApproved,
        prepaidMakingBudgetPiasters = prepaidMakingBudgetPiasters,
        pricingPolicyNote = pricingPolicyNote,
        city = city.ifBlank { "القاهرة" },
        latitude = latitude,
        longitude = longitude,
    )

    fun toCreateMap(): Map<String, Any> {
        val hasValidCoords = latitude in -85.0..85.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)
        val raw = mutableMapOf<String, Any?>(
            "ownerUid" to ownerUid,
            "shopName" to shopName,
            "area" to area,
            "address" to address,
            "contactPhone" to contactPhone,
            "verifiedEmail" to verifiedEmail,
            "description" to description,
            "rating" to rating,
            "reviewsCount" to reviewsCount,
            "status" to MerchantStatus.PENDING.name,
            "prepaidMakingApproved" to false,
            "prepaidMakingBudgetPiasters" to 0L,
            "city" to city.takeIf { it.isNotBlank() },
            "latitude" to if (hasValidCoords) latitude else null,
            "longitude" to if (hasValidCoords) longitude else null,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class MerchantPrivateDocsRecord(
    val ownerUid: String = "",
    val legalFullName: String = "",
    val nationalIdNumber: String = "",
    val commercialRegNumber: String = "",
    val taxCardNumber: String = "",
    val authorizationNotes: String = "",
    val documentThumbnails: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): MerchantPrivateDocs = MerchantPrivateDocs(
        ownerUid = ownerUid,
        legalFullName = legalFullName,
        nationalIdNumber = nationalIdNumber,
        commercialRegNumber = commercialRegNumber,
        taxCardNumber = taxCardNumber,
        authorizationNotes = authorizationNotes,
        documentThumbnails = documentThumbnails,
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "ownerUid" to ownerUid,
        "legalFullName" to legalFullName,
        "nationalIdNumber" to nationalIdNumber,
        "commercialRegNumber" to commercialRegNumber,
        "taxCardNumber" to taxCardNumber,
        "authorizationNotes" to authorizationNotes,
        "documentThumbnails" to documentThumbnails.take(4),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class MerchantAuditLogRecord(
    @DocumentId val id: String = "",
    val merchantId: String = "",
    val merchantOwnerUid: String = "",
    val actorUid: String = "",
    val action: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toDomain(): MerchantAuditLog = MerchantAuditLog(
        id = id,
        merchantId = merchantId,
        merchantOwnerUid = merchantOwnerUid,
        actorUid = actorUid,
        action = action,
        note = note,
        createdAtMs = createdAt?.toDate()?.time ?: 0L,
    )
}

@Keep
@IgnoreExtraProperties
data class MerchantProductRecord(
    @DocumentId val id: String = "",
    val merchantId: String = "",
    val ownerUid: String = "",
    val merchantName: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val karat: Long = 21L,
    val weightMg: Long = 0L,
    val weightGrams: Double = 0.0,
    val sellingPricePiasters: Long = 0L,
    val stockQuantity: Long = 0L,
    val stamped: Boolean = true,
    val imageThumbnails: List<String> = emptyList(),
    val status: String = MerchantProductStatus.PENDING_REVIEW.name,
    val rejectionReason: String? = null,
    val lastOrderId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): MerchantProduct = MerchantProduct(
        id = id,
        merchantId = merchantId,
        ownerUid = ownerUid,
        merchantName = merchantName,
        name = name,
        description = description,
        category = category,
        karat = karat.toInt(),
        weightMg = weightMg,
        weightGrams = weightGrams,
        sellingPricePiasters = sellingPricePiasters,
        stockQuantity = stockQuantity.toInt(),
        stamped = stamped,
        imageThumbnails = imageThumbnails,
        status = enumValues<MerchantProductStatus>().firstOrNull { it.name == status }
            ?: MerchantProductStatus.PENDING_REVIEW,
        rejectionReason = rejectionReason,
    )

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "merchantId" to merchantId,
            "ownerUid" to ownerUid,
            "merchantName" to merchantName,
            "name" to name,
            "description" to description,
            "category" to category,
            "karat" to karat,
            "weightMg" to weightMg,
            "weightGrams" to weightGrams,
            "sellingPricePiasters" to sellingPricePiasters,
            "stockQuantity" to stockQuantity,
            "stamped" to stamped,
            "imageThumbnails" to imageThumbnails.take(4),
            "status" to status,
            "rejectionReason" to rejectionReason,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class MerchantOrderRecord(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val merchantId: String = "",
    val merchantOwnerUid: String = "",
    val buyerUid: String = "",
    val items: List<OrderItemRecord> = emptyList(),
    val totalPiasters: Long = 0L,
    val pickupWindow: String = "",
    val status: String = OrderStatus.READY_FOR_PICKUP.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): MerchantOrderView = MerchantOrderView(
        orderId = orderId.ifBlank { id },
        merchantId = merchantId,
        merchantOwnerUid = merchantOwnerUid,
        buyerUid = buyerUid,
        items = items.map { it.toCartItem() },
        totalPiasters = totalPiasters,
        pickupWindow = pickupWindow,
        status = enumValues<OrderStatus>().firstOrNull { it.name == status } ?: OrderStatus.READY_FOR_PICKUP,
        createdAtMs = createdAt?.toDate()?.time ?: 0L,
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "orderId" to orderId,
        "merchantId" to merchantId,
        "merchantOwnerUid" to merchantOwnerUid,
        "buyerUid" to buyerUid,
        "items" to items.map { it.toMap() },
        "totalPiasters" to totalPiasters,
        "pickupWindow" to pickupWindow,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}
