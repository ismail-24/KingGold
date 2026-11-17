package com.example.data.remote

import com.example.model.CartItem
import com.example.model.GoldOrder
import com.example.model.LedgerTxType
import com.example.model.MerchantAuditLog
import com.example.model.MerchantOrderView
import com.example.model.MerchantPrivateDocs
import com.example.model.MerchantProduct
import com.example.model.MerchantProductStatus
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.model.MoneyUtils
import com.example.model.OrderStatus
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreMarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول أولاً قبل إجراء العمليات.")

    private fun <T> wrapQueryFlow(
        path: String,
        op: OperationType = OperationType.LIST,
        source: () -> Flow<T>,
    ): Flow<T> = flow {
        emitAll(
            source().catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val toLog = fsErr ?: (error as? Exception)
                if (toLog != null && auth.currentUser != null) {
                    handleFirestoreError(toLog, op, path)
                }
                throw (fsErr ?: error)
            },
        )
    }

    fun observeApprovedMerchants(): Flow<List<MerchantStore>> =
        wrapQueryFlow("merchants?status=APPROVED") {
            db.collection("merchants")
                .whereEqualTo("status", MerchantStatus.APPROVED.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantStoreRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).map { it.toDomain() }
                }
        }

    fun observeOwnMerchants(uid: String): Flow<List<MerchantStore>> =
        wrapQueryFlow("merchants?ownerUid=$uid") {
            db.collection("merchants")
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantStoreRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).map { it.toDomain() }
                }
        }

    fun observeAllMerchantsAdmin(): Flow<List<MerchantStore>> =
        wrapQueryFlow("merchants") {
            db.collection("merchants")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantStoreRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observePendingMerchantsAdmin(): Flow<List<MerchantStore>> =
        wrapQueryFlow("merchants?status=PENDING") {
            db.collection("merchants")
                .whereEqualTo("status", MerchantStatus.PENDING.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantStoreRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeActiveProducts(): Flow<List<MerchantProduct>> =
        wrapQueryFlow("products?status=ACTIVE") {
            db.collection("products")
                .whereEqualTo("status", MerchantProductStatus.ACTIVE.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantProductRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).map { it.toDomain() }
                }
        }

    fun observeOwnMerchantProducts(uid: String): Flow<List<MerchantProduct>> =
        wrapQueryFlow("products?ownerUid=$uid") {
            db.collection("products")
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantProductRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeAllProductsAdmin(): Flow<List<MerchantProduct>> =
        wrapQueryFlow("products") {
            db.collection("products")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantProductRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeMerchantOrders(uid: String): Flow<List<MerchantOrderView>> =
        wrapQueryFlow("merchant_orders?merchantOwnerUid=$uid") {
            db.collection("merchant_orders")
                .whereEqualTo("merchantOwnerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantOrderRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeMerchantAuditLogs(merchantId: String): Flow<List<MerchantAuditLog>> =
        wrapQueryFlow("merchants/$merchantId/audit_logs") {
            db.collection("merchants").document(merchantId).collection("audit_logs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MerchantAuditLogRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    suspend fun getMerchantPrivateDocs(merchantId: String): Result<MerchantPrivateDocs?> {
        val path = "merchants/$merchantId/private/kyc"
        return try {
            requireUserId()
            val snap = db.collection("merchants").document(merchantId)
                .collection("private").document("kyc").get().await()
            val obj = if (snap.exists()) {
                snap.toObject(MerchantPrivateDocsRecord::class.java)?.toDomain()
            } else null
            Result.success(obj)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun submitMerchantRegistration(
        shopName: String,
        area: String,
        address: String,
        contactPhone: String,
        verifiedEmail: String,
        description: String,
        legalFullName: String,
        nationalIdNumber: String,
        commercialRegNumber: String,
        taxCardNumber: String,
        authorizationNotes: String,
        documentThumbnails: List<String>,
    ): Result<String> {
        val uid = requireUserId()
        val merchantId = "m_${uid.take(12)}_${System.currentTimeMillis()}"
        val logId = "mlog_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val mRef = db.collection("merchants").document(merchantId)
        val kycRef = mRef.collection("private").document("kyc")
        val logRef = mRef.collection("audit_logs").document(logId)

        val storeRecord = MerchantStoreRecord(
            ownerUid = uid,
            shopName = shopName.trim(),
            area = area.trim(),
            address = address.trim(),
            contactPhone = contactPhone.trim(),
            verifiedEmail = verifiedEmail.trim(),
            description = description.trim(),
        )
        val docsRecord = MerchantPrivateDocsRecord(
            ownerUid = uid,
            legalFullName = legalFullName.trim(),
            nationalIdNumber = nationalIdNumber.trim(),
            commercialRegNumber = commercialRegNumber.trim(),
            taxCardNumber = taxCardNumber.trim(),
            authorizationNotes = authorizationNotes.trim(),
            documentThumbnails = documentThumbnails.take(4),
        )
        return try {
            db.runBatch { batch ->
                batch.set(mRef, storeRecord.toCreateMap())
                batch.set(kycRef, docsRecord.toCreateMap())
                batch.set(
                    logRef,
                    mapOf(
                        "merchantId" to merchantId,
                        "merchantOwnerUid" to uid,
                        "actorUid" to uid,
                        "action" to "SUBMITTED",
                        "note" to "تم إرسال طلب تسجيل التاجر والمستندات للمراجعة",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(merchantId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, mRef.path)
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateMerchantProduct(
        merchant: MerchantStore,
        existingProductId: String?,
        name: String,
        description: String,
        category: String,
        karat: Int,
        weightGrams: Double,
        sellingPricePiasters: Long,
        stockQuantity: Int,
        stamped: Boolean,
        imageThumbnails: List<String>,
        resubmitForReview: Boolean = false,
    ): Result<String> {
        val uid = requireUserId()
        val weightMg = MoneyUtils.gramsStringToMg(weightGrams.toString())
            ?: (weightGrams * 1000.0).toLong().coerceAtLeast(1L)
        val col = db.collection("products")
        return try {
            if (existingProductId.isNullOrBlank()) {
                val prodId = "prod_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                val rec = MerchantProductRecord(
                    merchantId = merchant.id,
                    ownerUid = uid,
                    merchantName = merchant.shopName,
                    name = name.trim(),
                    description = description.trim(),
                    category = category.trim(),
                    karat = karat.toLong(),
                    weightMg = weightMg,
                    weightGrams = weightGrams,
                    sellingPricePiasters = sellingPricePiasters,
                    stockQuantity = stockQuantity.toLong(),
                    stamped = stamped,
                    imageThumbnails = imageThumbnails.take(4),
                    status = MerchantProductStatus.PENDING_REVIEW.name,
                )
                col.document(prodId).set(rec.toCreateMap()).await()
                Result.success(prodId)
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "name" to name.trim(),
                    "description" to description.trim(),
                    "category" to category.trim(),
                    "karat" to karat.toLong(),
                    "weightMg" to weightMg,
                    "weightGrams" to weightGrams,
                    "sellingPricePiasters" to sellingPricePiasters,
                    "stockQuantity" to stockQuantity.toLong(),
                    "stamped" to stamped,
                    "imageThumbnails" to imageThumbnails.take(4),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (resubmitForReview) {
                    updateMap["status"] = MerchantProductStatus.PENDING_REVIEW.name
                }
                col.document(existingProductId).update(updateMap).await()
                Result.success(existingProductId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
            Result.failure(e)
        }
    }

    suspend fun toggleMerchantProductPause(product: MerchantProduct): Result<Unit> {
        requireUserId()
        val targetStatus = if (product.status == MerchantProductStatus.ACTIVE) {
            MerchantProductStatus.PAUSED.name
        } else {
            MerchantProductStatus.PENDING_REVIEW.name
        }
        val ref = db.collection("products").document(product.id)
        return try {
            ref.update(mapOf("status" to targetStatus, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun updateMerchantOrderStatus(orderId: String, nextStatus: OrderStatus): Result<Unit> {
        requireUserId()
        val ref = db.collection("merchant_orders").document(orderId)
        return try {
            ref.update(
                mapOf(
                    "status" to nextStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun syncBuyerOrderStatusToMerchantOrder(orderId: String, nextStatus: OrderStatus) {
        val ref = db.collection("merchant_orders").document(orderId)
        try {
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "status" to nextStatus.name,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
        } catch (_: Exception) {
            // Non-merchant or already finalized; ignore cleanly without breaking buyer action.
        }
    }

    suspend fun placeMarketplaceOrderAtomic(
        order: GoldOrder,
        payWithWallet: Boolean,
        dynamicCartProductIds: List<CartItem>,
    ): Result<String> {
        val uid = requireUserId()
        val orderRef = db.collection("users").document(uid).collection("orders").document(order.id)
        val totalPiasters = order.totalPiasters.takeIf { it > 0L }
            ?: MoneyUtils.egpToPiasters(order.total)
        val idempotencyKey = "ord_tx_${order.id.replace("-", "_")}"

        return try {
            db.runTransaction { tx ->
                // 1. Verify and read dynamic merchant products for atomic stock decrement
                val prodSnaps = if (order.shop.isDynamicMerchant) {
                    dynamicCartProductIds.map { item ->
                        val pRef = db.collection("products").document(item.product.id)
                        val snap = tx.get(pRef)
                        Triple(pRef, snap, item.quantity)
                    }
                } else {
                    emptyList()
                }

                // 2. If wallet payment selected, read wallet
                val walletRef = db.collection("users").document(uid).collection("wallet").document("summary")
                val walletSnap = if (payWithWallet) tx.get(walletRef) else null

                if (payWithWallet) {
                    val prev = walletSnap?.toObject(WalletSummaryRecord::class.java)
                        ?: throw IllegalStateException("المحفظة غير مهيأة بعد.")
                    if (prev.cashAvailablePiasters < totalPiasters) {
                        throw IllegalStateException("الرصيد المتاح بالمحفظة غير كافٍ لإتمام الطلب.")
                    }
                    val ledgerRef = db.collection("users").document(uid).collection("ledger").document(idempotencyKey)
                    tx.set(
                        ledgerRef,
                        LedgerEntryRecord(
                            userId = uid,
                            idempotencyKey = idempotencyKey,
                            txType = LedgerTxType.MARKETPLACE_ORDER_PAID.name,
                            referenceId = order.id,
                            karat = 0L,
                            cashDeltaPiasters = -totalPiasters,
                            unitPricePerGramPiasters = 0L,
                            notes = "دفع طلب من ${order.shop.name} (${order.id})",
                        ).toCreateMap(),
                    )
                    tx.update(
                        walletRef,
                        mapOf(
                            "cashAvailablePiasters" to (prev.cashAvailablePiasters - totalPiasters),
                            "cashTotalDebitedPiasters" to (prev.cashTotalDebitedPiasters + totalPiasters),
                            "lastSettlementRef" to idempotencyKey,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }

                // 3. Write buyer order (zero extra making charges)
                val record = GoldOrderRecord.fromGoldOrder(
                    userId = uid,
                    order = order.copy(
                        makingFee = 0.0,
                        serviceFee = 0.0,
                        totalPiasters = totalPiasters,
                        paymentSource = if (payWithWallet) "CASH_WALLET" else "ESCROW_METHOD",
                    ),
                    idempotencyKey = idempotencyKey,
                )
                tx.set(orderRef, record.toCreateMap())

                // 4. Mirror merchant order & decrement inventory atomically for approved merchant stores
                if (order.shop.isDynamicMerchant && order.shop.ownerUid.isNotBlank()) {
                    val mOrderRef = db.collection("merchant_orders").document(order.id)
                    tx.set(
                        mOrderRef,
                        MerchantOrderRecord(
                            orderId = order.id,
                            merchantId = order.shop.id,
                            merchantOwnerUid = order.shop.ownerUid,
                            buyerUid = uid,
                            items = order.items.map { OrderItemRecord.fromCartItem(it) },
                            totalPiasters = totalPiasters,
                            pickupWindow = order.pickupWindow,
                            status = order.status.name,
                        ).toCreateMap(),
                    )
                    for ((pRef, snap, qty) in prodSnaps) {
                        val currentStock = snap.getLong("stockQuantity") ?: 0L
                        if (!snap.exists() || currentStock < qty) {
                            throw IllegalStateException("الكمية المطلوبة غير متوفرة حالياً في مخزون التاجر.")
                        }
                        tx.update(
                            pRef,
                            mapOf(
                                "stockQuantity" to (currentStock - qty),
                                "lastOrderId" to order.id,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                }
            }.await()
            Result.success(order.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminReviewMerchant(
        merchant: MerchantStore,
        newStatus: MerchantStatus,
        prepaidApproved: Boolean,
        prepaidBudgetPiasters: Long,
        reasonOrNote: String,
    ): Result<Unit> {
        val adminUid = requireUserId()
        val mRef = db.collection("merchants").document(merchant.id)
        val mLogId = "mlog_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val aLogId = "alog_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        return try {
            db.runBatch { batch ->
                val updates = mutableMapOf<String, Any>(
                    "status" to newStatus.name,
                    "prepaidMakingApproved" to prepaidApproved,
                    "prepaidMakingBudgetPiasters" to prepaidBudgetPiasters,
                    "pricingPolicyNote" to reasonOrNote.ifBlank { "سعر نهائي بدون مصنعية على المشتري" },
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (newStatus == MerchantStatus.REJECTED || newStatus == MerchantStatus.SUSPENDED) {
                    updates["rejectionReason"] = reasonOrNote.ifBlank { "مخالفة شروط الاعتماد" }
                }
                batch.update(mRef, updates)
                batch.set(
                    mRef.collection("audit_logs").document(mLogId),
                    mapOf(
                        "merchantId" to merchant.id,
                        "merchantOwnerUid" to merchant.ownerUid,
                        "actorUid" to adminUid,
                        "action" to newStatus.name,
                        "note" to reasonOrNote.ifBlank { "تحديث حالة التاجر إلى ${newStatus.arabicLabel}" },
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
                batch.set(
                    db.collection("audit_logs").document(aLogId),
                    mapOf(
                        "actorUid" to adminUid,
                        "actorRole" to "ADMIN",
                        "action" to "MERCHANT_${newStatus.name}",
                        "targetPath" to mRef.path,
                        "details" to "${merchant.shopName}: $reasonOrNote",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, mRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminReviewProduct(
        product: MerchantProduct,
        newStatus: MerchantProductStatus,
        reason: String,
    ): Result<Unit> {
        val adminUid = requireUserId()
        val pRef = db.collection("products").document(product.id)
        val aLogId = "alog_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        return try {
            db.runBatch { batch ->
                val updates = mutableMapOf<String, Any>(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (newStatus == MerchantProductStatus.REJECTED) {
                    updates["rejectionReason"] = reason.ifBlank { "يرجى مراجعة بيانات المنتج" }
                }
                batch.update(pRef, updates)
                batch.set(
                    db.collection("audit_logs").document(aLogId),
                    mapOf(
                        "actorUid" to adminUid,
                        "actorRole" to "ADMIN",
                        "action" to "PRODUCT_${newStatus.name}",
                        "targetPath" to pRef.path,
                        "details" to "${product.name}: ${reason.ifBlank { newStatus.arabicLabel }}",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, pRef.path)
            Result.failure(e)
        }
    }
}
