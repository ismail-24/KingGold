package com.example.data.remote

import com.example.model.AuditLogRecord
import com.example.model.DepositRequest
import com.example.model.DepositStatus
import com.example.model.FractionalGoldOrder
import com.example.model.LedgerEntry
import com.example.model.LedgerTxType
import com.example.model.MoneyUtils
import com.example.model.PlatformPricingConfig
import com.example.model.SellGoldRequest
import com.example.model.SellRequestStatus
import com.example.model.WalletSummary
import com.example.model.WithdrawalDeliveryType
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreVaultRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول أولاً قبل إجراء العمليات.")

    private fun walletDoc(uid: String) =
        db.collection("users").document(uid).collection("wallet").document("summary")

    private fun ledgerCollection(uid: String) =
        db.collection("users").document(uid).collection("ledger")

    private fun <T> wrapFlow(
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

    suspend fun ensureInitialWallet(userId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (uid != userId) return
        val wRef = walletDoc(uid)
        try {
            val snap = wRef.get().await()
            if (!snap.exists()) {
                wRef.set(WalletSummaryRecord(userId = uid).toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, wRef.path)
        }
    }

    fun observeWallet(uid: String): Flow<WalletSummary> =
        wrapFlow("users/$uid/wallet/summary", OperationType.GET) {
            walletDoc(uid).snapshots().map { snap ->
                snap.toObject(
                    WalletSummaryRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )?.toDomain() ?: WalletSummary(userId = uid)
            }
        }

    fun observeLedger(uid: String): Flow<List<LedgerEntry>> =
        wrapFlow("users/$uid/ledger") {
            ledgerCollection(uid).snapshots().map { snap ->
                snap.toObjects(
                    LedgerEntryRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    .map { it.toDomain() }
            }
        }

    fun observePlatformPricing(): Flow<PlatformPricingConfig?> =
        wrapFlow("platform_config/pricing", OperationType.GET) {
            db.collection("platform_config").document("pricing").snapshots().map { snap ->
                if (snap.exists()) {
                    snap.toObject(
                        PlatformPricingConfigRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )?.toDomain()
                } else null
            }
        }

    fun observePricingHistoryLast30Days(limitCount: Long = 120L): Flow<List<PlatformPricingConfig>> =
        wrapFlow("platform_config/pricing/history", OperationType.LIST) {
            db.collection("platform_config")
                .document("pricing")
                .collection("history")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limitCount)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PlatformPricingConfigRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).map { it.toDomain() }
                }
        }

    suspend fun fetchPricingHistoryLast30Days(limitCount: Long = 120L): Result<List<PlatformPricingConfig>> {
        val histRef = db.collection("platform_config")
            .document("pricing")
            .collection("history")
        return try {
            val snap = histRef
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(limitCount)
                .get()
                .await()
            val items = snap.toObjects(
                PlatformPricingConfigRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            ).map { it.toDomain() }
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, histRef.path)
            Result.failure(e)
        }
    }

    fun observeUserSellRequests(uid: String): Flow<List<SellGoldRequest>> =
        wrapFlow("sell_requests?customerUid=$uid") {
            db.collection("sell_requests")
                .whereEqualTo("customerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        SellGoldRequestRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeAllSellRequestsAdmin(): Flow<List<SellGoldRequest>> =
        wrapFlow("sell_requests") {
            db.collection("sell_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        SellGoldRequestRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeUserFractionalOrders(uid: String): Flow<List<FractionalGoldOrder>> =
        wrapFlow("fractional_orders?customerUid=$uid") {
            db.collection("fractional_orders")
                .whereEqualTo("customerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FractionalGoldOrderRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeUserWithdrawals(uid: String): Flow<List<WithdrawalRequest>> =
        wrapFlow("withdrawal_requests?customerUid=$uid") {
            db.collection("withdrawal_requests")
                .whereEqualTo("customerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        WithdrawalRequestRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeAllWithdrawalsAdmin(): Flow<List<WithdrawalRequest>> =
        wrapFlow("withdrawal_requests") {
            db.collection("withdrawal_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        WithdrawalRequestRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeUserDeposits(uid: String): Flow<List<DepositRequest>> =
        wrapFlow("deposit_requests?customerUid=$uid") {
            db.collection("deposit_requests")
                .whereEqualTo("customerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DepositRequestRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeAllDepositsAdmin(): Flow<List<DepositRequest>> =
        wrapFlow("deposit_requests") {
            db.collection("deposit_requests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DepositRequestRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    fun observeAuditLogsAdmin(): Flow<List<AuditLogRecord>> =
        wrapFlow("audit_logs") {
            db.collection("audit_logs")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AuditLogFsRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    suspend fun submitSellGoldRequest(
        itemTitle: String,
        category: String,
        karat: Int,
        estimatedWeightMg: Long,
        conditionNotes: String,
        photoThumbnails: List<String>,
    ): Result<String> {
        val uid = requireUserId()
        val email = auth.currentUser?.email.orEmpty()
        val reqId = "sell_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("sell_requests").document(reqId)
        val rec = SellGoldRequestRecord(
            customerUid = uid,
            customerEmail = email,
            itemTitle = itemTitle.trim(),
            category = category.trim(),
            karat = karat.toLong(),
            estimatedWeightMg = estimatedWeightMg,
            conditionNotes = conditionNotes.trim(),
            photoThumbnails = photoThumbnails.take(4),
        )
        return try {
            docRef.set(rec.toCreateMap()).await()
            Result.success(reqId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelSellGoldRequest(requestId: String): Result<Unit> {
        requireUserId()
        val ref = db.collection("sell_requests").document(requestId)
        return try {
            ref.update(
                mapOf(
                    "status" to SellRequestStatus.CANCELLED.name,
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

    suspend fun acceptSellGoldOfferAtomic(request: SellGoldRequest): Result<String> {
        val uid = requireUserId()
        ensureInitialWallet(uid)
        val txId = "settle_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val reqRef = db.collection("sell_requests").document(request.id)
        val wRef = walletDoc(uid)
        val ledRef = ledgerCollection(uid).document(txId)

        return try {
            db.runTransaction { tx ->
                val reqSnap = tx.get(reqRef)
                val reqObj = reqSnap.toObject(SellGoldRequestRecord::class.java)
                    ?: throw IllegalStateException("طلب البيع غير موجود.")
                if (reqObj.status != SellRequestStatus.OFFER_SENT.name) {
                    throw IllegalStateException("لا يمكن قبول العرض في الحالة الحالية.")
                }
                val payout = reqObj.finalOfferPiasters
                if (payout <= 0L) throw IllegalStateException("قيمة العرض النهائي غير صالحة.")

                val wSnap = tx.get(wRef)
                val prev = wSnap.toObject(WalletSummaryRecord::class.java)
                    ?: throw IllegalStateException("تعذّر تحميل بيانات المحفظة.")

                tx.update(
                    reqRef,
                    mapOf(
                        "status" to SellRequestStatus.COMPLETED.name,
                        "settlementTransactionId" to txId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.set(
                    ledRef,
                    LedgerEntryRecord(
                        userId = uid,
                        idempotencyKey = txId,
                        txType = LedgerTxType.GOLD_SALE_PROCEEDS.name,
                        referenceId = request.id,
                        karat = reqObj.confirmedKarat,
                        cashDeltaPiasters = payout,
                        unitPricePerGramPiasters = reqObj.referencePricePerGramPiasters,
                        notes = "صافي بيع (${reqObj.itemTitle}) للمنصة",
                    ).toCreateMap(),
                )
                tx.update(
                    wRef,
                    mapOf(
                        "cashAvailablePiasters" to (prev.cashAvailablePiasters + payout),
                        "cashTotalCreditedPiasters" to (prev.cashTotalCreditedPiasters + payout),
                        "lastSettlementRef" to txId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun buyFractionalGoldAtomic(
        karat: Int,
        weightMg: Long,
        pricePerGramPiasters: Long,
        payWithWallet: Boolean,
    ): Result<String> {
        val uid = requireUserId()
        if (karat !in listOf(18, 21, 24)) return Result.failure(IllegalArgumentException("عيار غير مدعوم"))
        if (weightMg <= 0L) return Result.failure(IllegalArgumentException("الوزن يجب أن يكون أكبر من 0 ملجم"))

        val totalPiasters = MoneyUtils.computeFractionalCostPiasters(weightMg, pricePerGramPiasters)
        if (totalPiasters <= 0L) return Result.failure(IllegalArgumentException("قيمة الشراء غير صالحة"))

        ensureInitialWallet(uid)

        val ordId = "frac_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val ledId = "led_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val ordRef = db.collection("fractional_orders").document(ordId)
        val wRef = walletDoc(uid)
        val ledRef = ledgerCollection(uid).document(ledId)

        return try {
            db.runTransaction { tx ->
                val wSnap = tx.get(wRef)
                val prev = wSnap.toObject(WalletSummaryRecord::class.java)
                    ?: throw IllegalStateException("تعذّر تحميل بيانات المحفظة.")

                if (payWithWallet) {
                    if (prev.cashAvailablePiasters < totalPiasters) {
                        throw IllegalStateException("الرصيد المتاح بالمحفظة النقدية غير كافٍ.")
                    }
                    tx.set(
                        ordRef,
                        FractionalGoldOrderRecord(
                            customerUid = uid,
                            karat = karat.toLong(),
                            weightMg = weightMg,
                            pricePerGramPiasters = pricePerGramPiasters,
                            totalPiasters = totalPiasters,
                            paymentSource = "CASH_WALLET",
                            status = "CONFIRMED",
                            ledgerEntryId = ledId,
                        ).toCreateMap(),
                    )
                    tx.set(
                        ledRef,
                        LedgerEntryRecord(
                            userId = uid,
                            idempotencyKey = ledId,
                            txType = LedgerTxType.FRACTIONAL_GOLD_BUY.name,
                            referenceId = ordId,
                            karat = karat.toLong(),
                            cashDeltaPiasters = -totalPiasters,
                            goldDeltaMg = weightMg,
                            unitPricePerGramPiasters = pricePerGramPiasters,
                            notes = "شراء ${MoneyUtils.mgToGramsString(weightMg)} ذهب عيار $karat",
                        ).toCreateMap(),
                    )
                    val new24 = prev.gold24kMg + if (karat == 24) weightMg else 0L
                    val new21 = prev.gold21kMg + if (karat == 21) weightMg else 0L
                    val new18 = prev.gold18kMg + if (karat == 18) weightMg else 0L
                    tx.update(
                        wRef,
                        mapOf(
                            "cashAvailablePiasters" to (prev.cashAvailablePiasters - totalPiasters),
                            "cashTotalDebitedPiasters" to (prev.cashTotalDebitedPiasters + totalPiasters),
                            "goldConfirmedMg" to (prev.goldConfirmedMg + weightMg),
                            "gold24kMg" to new24,
                            "gold21kMg" to new21,
                            "gold18kMg" to new18,
                            "totalInvestedPiasters" to (prev.totalInvestedPiasters + totalPiasters),
                            "lastSettlementRef" to ledId,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                } else {
                    tx.set(
                        ordRef,
                        FractionalGoldOrderRecord(
                            customerUid = uid,
                            karat = karat.toLong(),
                            weightMg = weightMg,
                            pricePerGramPiasters = pricePerGramPiasters,
                            totalPiasters = totalPiasters,
                            paymentSource = "EXTERNAL_PENDING",
                            status = "PENDING_PAYMENT",
                            ledgerEntryId = ledId,
                        ).toCreateMap(),
                    )
                    tx.set(
                        ledRef,
                        LedgerEntryRecord(
                            userId = uid,
                            idempotencyKey = ledId,
                            txType = LedgerTxType.FRACTIONAL_GOLD_PENDING.name,
                            referenceId = ordId,
                            karat = karat.toLong(),
                            pendingGoldDeltaMg = weightMg,
                            unitPricePerGramPiasters = pricePerGramPiasters,
                            notes = "طلب شراء معلق ${MoneyUtils.mgToGramsString(weightMg)} عيار $karat",
                        ).toCreateMap(),
                    )
                    tx.update(
                        wRef,
                        mapOf(
                            "goldPendingMg" to (prev.goldPendingMg + weightMg),
                            "lastSettlementRef" to ledId,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
            }.await()
            Result.success(ordId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ordRef.path)
            Result.failure(e)
        }
    }

    suspend fun requestPhysicalWithdrawalAtomic(
        karat: Int,
        amountMg: Long,
        deliveryType: WithdrawalDeliveryType,
        deliveryAddress: String,
    ): Result<String> {
        val uid = requireUserId()
        if (amountMg < MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG) {
            return Result.failure(IllegalArgumentException("الحد الأدنى للسحب الفعلي للذهب هو 1.000 جرام (1000 ملجم)."))
        }
        val reqId = "wd_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val ledId = "led_wd_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val wdRefString = "WD-${(System.currentTimeMillis() % 900000L + 100000L)}"
        val reqRef = db.collection("withdrawal_requests").document(reqId)
        val wRef = walletDoc(uid)
        val ledRef = ledgerCollection(uid).document(ledId)

        return try {
            db.runTransaction { tx ->
                val wSnap = tx.get(wRef)
                val prev = wSnap.toObject(WalletSummaryRecord::class.java)
                    ?: throw IllegalStateException("الخزنة الرقمية فارغة.")
                val availableTotal = prev.goldConfirmedMg - prev.goldReservedMg
                val karatHeld = when (karat) {
                    24 -> prev.gold24kMg
                    21 -> prev.gold21kMg
                    18 -> prev.gold18kMg
                    else -> 0L
                }
                if (availableTotal < amountMg || karatHeld < amountMg) {
                    throw IllegalStateException("الرصيد المتاح غير محجوز من عيار $karat غير كافٍ لهذا السحب.")
                }

                tx.set(
                    reqRef,
                    WithdrawalRequestRecord(
                        customerUid = uid,
                        customerEmail = auth.currentUser?.email.orEmpty(),
                        karat = karat.toLong(),
                        amountMg = amountMg,
                        deliveryType = deliveryType.name,
                        deliveryAddress = deliveryAddress.trim(),
                        withdrawalRef = wdRefString,
                        ledgerEntryId = ledId,
                    ).toCreateMap(),
                )
                tx.set(
                    ledRef,
                    LedgerEntryRecord(
                        userId = uid,
                        idempotencyKey = ledId,
                        txType = LedgerTxType.GOLD_RESERVATION.name,
                        referenceId = reqId,
                        karat = karat.toLong(),
                        reservedDeltaMg = amountMg,
                        notes = "حجز سحب فعلي ${MoneyUtils.mgToGramsString(amountMg)} ($wdRefString)",
                    ).toCreateMap(),
                )
                tx.update(
                    wRef,
                    mapOf(
                        "goldReservedMg" to (prev.goldReservedMg + amountMg),
                        "lastSettlementRef" to ledId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(wdRefString)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitDepositRequestAtomic(
        amountPiasters: Long,
        channel: String,
        paymentReference: String,
    ): Result<String> {
        val uid = requireUserId()
        if (amountPiasters < 1000L) {
            return Result.failure(IllegalArgumentException("الحد الأدنى للإيداع 10 ج.م."))
        }
        ensureInitialWallet(uid)
        val depId = "dep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val ledId = "led_dep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val depRef = db.collection("deposit_requests").document(depId)
        val wRef = walletDoc(uid)
        val ledRef = ledgerCollection(uid).document(ledId)

        return try {
            db.runTransaction { tx ->
                val wSnap = tx.get(wRef)
                val prev = wSnap.toObject(WalletSummaryRecord::class.java)
                    ?: throw IllegalStateException("تعذّر تحميل بيانات المحفظة.")

                tx.set(
                    depRef,
                    DepositRequestRecord(
                        customerUid = uid,
                        customerEmail = auth.currentUser?.email.orEmpty(),
                        amountPiasters = amountPiasters,
                        channel = channel.trim(),
                        paymentReference = paymentReference.trim(),
                        ledgerEntryId = ledId,
                    ).toCreateMap(),
                )
                tx.set(
                    ledRef,
                    LedgerEntryRecord(
                        userId = uid,
                        idempotencyKey = ledId,
                        txType = LedgerTxType.CASH_DEPOSIT_PENDING.name,
                        referenceId = depId,
                        karat = 0L,
                        pendingCashDeltaPiasters = amountPiasters,
                        notes = "طلب إيداع ($channel) مرجع: $paymentReference",
                    ).toCreateMap(),
                )
                tx.update(
                    wRef,
                    mapOf(
                        "cashPendingPiasters" to (prev.cashPendingPiasters + amountPiasters),
                        "lastSettlementRef" to ledId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(depId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, depRef.path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // Admin Controls (Atomic Evaluation, Deposits, Withdrawals & Pricing)
    // =========================================================================

    suspend fun adminEvaluateSellGold(
        request: SellGoldRequest,
        verifiedWeightMg: Long,
        confirmedKarat: Int,
        referencePricePerGramPiasters: Long,
        makingChargeDeductionPiasters: Long,
        inspectionNotes: String,
    ): Result<Unit> {
        val adminUid = requireUserId()
        val evaluatedValue = MoneyUtils.computeFractionalCostPiasters(verifiedWeightMg, referencePricePerGramPiasters)
        val finalOffer = (evaluatedValue - makingChargeDeductionPiasters).coerceAtLeast(0L)
        val reqRef = db.collection("sell_requests").document(request.id)
        val aLogId = "alog_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            db.runBatch { batch ->
                batch.update(
                    reqRef,
                    mapOf(
                        "status" to SellRequestStatus.OFFER_SENT.name,
                        "evaluatorAdminUid" to adminUid,
                        "verifiedWeightMg" to verifiedWeightMg,
                        "confirmedKarat" to confirmedKarat.toLong(),
                        "referencePricePerGramPiasters" to referencePricePerGramPiasters,
                        "evaluatedGoldValuePiasters" to evaluatedValue,
                        "makingChargeDeductionPiasters" to makingChargeDeductionPiasters,
                        "finalOfferPiasters" to finalOffer,
                        "inspectionNotes" to inspectionNotes.trim(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                batch.set(
                    db.collection("audit_logs").document(aLogId),
                    mapOf(
                        "actorUid" to adminUid,
                        "actorRole" to "ADMIN",
                        "action" to "SELL_GOLD_OFFER_SENT",
                        "targetPath" to reqRef.path,
                        "details" to "تقييم ${request.itemTitle}: صافي العرض ${MoneyUtils.piastersToEgpString(finalOffer)} ج.م",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminRejectSellGold(request: SellGoldRequest, reason: String): Result<Unit> {
        val adminUid = requireUserId()
        val reqRef = db.collection("sell_requests").document(request.id)
        return try {
            reqRef.update(
                mapOf(
                    "status" to SellRequestStatus.REJECTED.name,
                    "evaluatorAdminUid" to adminUid,
                    "rejectionReason" to reason.trim(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminApproveOrRejectDepositAtomic(
        deposit: DepositRequest,
        approve: Boolean,
        adminNote: String,
    ): Result<Unit> {
        val adminUid = requireUserId()
        val depRef = db.collection("deposit_requests").document(deposit.id)
        val wRef = walletDoc(deposit.customerUid)
        val ledId = "led_adm_dep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val ledRef = ledgerCollection(deposit.customerUid).document(ledId)
        val aLogId = "alog_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            db.runTransaction { tx ->
                val depSnap = tx.get(depRef)
                val currDep = depSnap.toObject(DepositRequestRecord::class.java)
                    ?: throw IllegalStateException("طلب الإيداع غير موجود.")
                if (currDep.status != DepositStatus.PENDING.name) {
                    throw IllegalStateException("تمت معالجة هذا الإيداع مسبقاً.")
                }
                val wSnap = tx.get(wRef)
                val prev = wSnap.toObject(WalletSummaryRecord::class.java)
                    ?: WalletSummaryRecord(userId = deposit.customerUid)

                val newPendingCash = (prev.cashPendingPiasters - deposit.amountPiasters).coerceAtLeast(0L)
                tx.update(
                    depRef,
                    mapOf(
                        "status" to if (approve) DepositStatus.APPROVED.name else DepositStatus.REJECTED.name,
                        "adminUid" to adminUid,
                        "adminNote" to adminNote.trim(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.set(
                    ledRef,
                    LedgerEntryRecord(
                        userId = deposit.customerUid,
                        idempotencyKey = ledId,
                        txType = if (approve) LedgerTxType.CASH_DEPOSIT.name else LedgerTxType.ADMIN_ADJUSTMENT.name,
                        referenceId = deposit.id,
                        karat = 0L,
                        cashDeltaPiasters = if (approve) deposit.amountPiasters else 0L,
                        pendingCashDeltaPiasters = -deposit.amountPiasters,
                        notes = if (approve) "اعتماد إيداع ${deposit.paymentReference}" else "رفض إيداع: $adminNote",
                    ).toCreateMap(),
                )
                tx.update(
                    wRef,
                    mapOf(
                        "cashPendingPiasters" to newPendingCash,
                        "cashAvailablePiasters" to (prev.cashAvailablePiasters + if (approve) deposit.amountPiasters else 0L),
                        "cashTotalCreditedPiasters" to (prev.cashTotalCreditedPiasters + if (approve) deposit.amountPiasters else 0L),
                        "lastSettlementRef" to ledId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.set(
                    db.collection("audit_logs").document(aLogId),
                    mapOf(
                        "actorUid" to adminUid,
                        "actorRole" to "ADMIN",
                        "action" to if (approve) "DEPOSIT_APPROVED" else "DEPOSIT_REJECTED",
                        "targetPath" to depRef.path,
                        "details" to "${MoneyUtils.piastersToEgpString(deposit.amountPiasters)} ج.م — $adminNote",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, depRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminProcessWithdrawalAtomic(
        request: WithdrawalRequest,
        newStatus: WithdrawalStatus,
        proofNote: String,
    ): Result<Unit> {
        val adminUid = requireUserId()
        val reqRef = db.collection("withdrawal_requests").document(request.id)
        val wRef = walletDoc(request.customerUid)
        val ledId = "led_adm_wd_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val ledRef = ledgerCollection(request.customerUid).document(ledId)
        val aLogId = "alog_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            db.runTransaction { tx ->
                val wSnap = tx.get(wRef)
                val prev = wSnap.toObject(WalletSummaryRecord::class.java)
                    ?: throw IllegalStateException("ملف محفظة المستخدم غير موجود.")

                tx.update(
                    reqRef,
                    mapOf(
                        "status" to newStatus.name,
                        "adminUid" to adminUid,
                        "proofNote" to proofNote.trim(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )

                when (newStatus) {
                    WithdrawalStatus.FULFILLED -> {
                        val amt = request.amountMg
                        val nextReserved = (prev.goldReservedMg - amt).coerceAtLeast(0L)
                        val nextConfirmed = (prev.goldConfirmedMg - amt).coerceAtLeast(0L)
                        val next24 = if (request.karat == 24) (prev.gold24kMg - amt).coerceAtLeast(0L) else prev.gold24kMg
                        val next21 = if (request.karat == 21) (prev.gold21kMg - amt).coerceAtLeast(0L) else prev.gold21kMg
                        val next18 = if (request.karat == 18) (prev.gold18kMg - amt).coerceAtLeast(0L) else prev.gold18kMg
                        tx.set(
                            ledRef,
                            LedgerEntryRecord(
                                userId = request.customerUid,
                                idempotencyKey = ledId,
                                txType = LedgerTxType.GOLD_WITHDRAWAL_FULFILLED.name,
                                referenceId = request.id,
                                karat = request.karat.toLong(),
                                goldDeltaMg = -amt,
                                reservedDeltaMg = -amt,
                                notes = "تسليم فعلي لطلب ${request.withdrawalRef}: $proofNote",
                            ).toCreateMap(),
                        )
                        tx.update(
                            wRef,
                            mapOf(
                                "goldConfirmedMg" to nextConfirmed,
                                "goldReservedMg" to nextReserved,
                                "gold24kMg" to next24,
                                "gold21kMg" to next21,
                                "gold18kMg" to next18,
                                "lastSettlementRef" to ledId,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                    WithdrawalStatus.REJECTED -> {
                        val amt = request.amountMg
                        val nextReserved = (prev.goldReservedMg - amt).coerceAtLeast(0L)
                        tx.set(
                            ledRef,
                            LedgerEntryRecord(
                                userId = request.customerUid,
                                idempotencyKey = ledId,
                                txType = LedgerTxType.GOLD_RESERVATION_RELEASED.name,
                                referenceId = request.id,
                                karat = request.karat.toLong(),
                                reservedDeltaMg = -amt,
                                notes = "إلغاء حجز سحب ${request.withdrawalRef}: $proofNote",
                            ).toCreateMap(),
                        )
                        tx.update(
                            wRef,
                            mapOf(
                                "goldReservedMg" to nextReserved,
                                "lastSettlementRef" to ledId,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                    else -> Unit
                }

                tx.set(
                    db.collection("audit_logs").document(aLogId),
                    mapOf(
                        "actorUid" to adminUid,
                        "actorRole" to "ADMIN",
                        "action" to "WITHDRAWAL_${newStatus.name}",
                        "targetPath" to reqRef.path,
                        "details" to "${request.withdrawalRef} (${MoneyUtils.mgToGramsString(request.amountMg)}): $proofNote",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            Result.failure(e)
        }
    }

    fun observeAllPendingFractionalAdmin(): Flow<List<FractionalGoldOrder>> =
        wrapFlow("fractional_orders") {
            db.collection("fractional_orders")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FractionalGoldOrderRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        .map { it.toDomain() }
                }
        }

    suspend fun adminConfirmOrRejectFractionalOrderAtomic(
        order: FractionalGoldOrder,
        confirm: Boolean,
    ): Result<Unit> {
        val adminUid = requireUserId()
        val ordRef = db.collection("fractional_orders").document(order.id)
        val wRef = walletDoc(order.customerUid)
        val ledId = "led_adm_frac_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val ledRef = ledgerCollection(order.customerUid).document(ledId)
        val aLogId = "alog_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        return try {
            db.runTransaction { tx ->
                val ordSnap = tx.get(ordRef)
                val curr = ordSnap.toObject(FractionalGoldOrderRecord::class.java)
                    ?: throw IllegalStateException("الطلب غير موجود.")
                if (curr.status != "PENDING_PAYMENT") {
                    throw IllegalStateException("تمت معالجة طلب الشراء هذا مسبقاً.")
                }
                val wSnap = tx.get(wRef)
                val prev = wSnap.toObject(WalletSummaryRecord::class.java)
                    ?: throw IllegalStateException("محفظة المستخدم غير موجودة.")

                val nextPending = (prev.goldPendingMg - order.weightMg).coerceAtLeast(0L)
                tx.update(
                    ordRef,
                    mapOf(
                        "status" to if (confirm) "CONFIRMED" else "REJECTED",
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.set(
                    ledRef,
                    LedgerEntryRecord(
                        userId = order.customerUid,
                        idempotencyKey = ledId,
                        txType = LedgerTxType.ADMIN_ADJUSTMENT.name,
                        referenceId = order.id,
                        karat = order.karat.toLong(),
                        goldDeltaMg = if (confirm) order.weightMg else 0L,
                        pendingGoldDeltaMg = -order.weightMg,
                        unitPricePerGramPiasters = order.pricePerGramPiasters,
                        notes = if (confirm) "تأكيد شراء ذهب مجزأ ${MoneyUtils.mgToGramsString(order.weightMg)}" else "رفض شراء معلق",
                    ).toCreateMap(),
                )
                val wUpdates = mutableMapOf<String, Any>(
                    "goldPendingMg" to nextPending,
                    "lastSettlementRef" to ledId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (confirm) {
                    wUpdates["goldConfirmedMg"] = prev.goldConfirmedMg + order.weightMg
                    wUpdates["gold24kMg"] = prev.gold24kMg + if (order.karat == 24) order.weightMg else 0L
                    wUpdates["gold21kMg"] = prev.gold21kMg + if (order.karat == 21) order.weightMg else 0L
                    wUpdates["gold18kMg"] = prev.gold18kMg + if (order.karat == 18) order.weightMg else 0L
                    wUpdates["totalInvestedPiasters"] = prev.totalInvestedPiasters + order.totalPiasters
                }
                tx.update(wRef, wUpdates)
                tx.set(
                    db.collection("audit_logs").document(aLogId),
                    mapOf(
                        "actorUid" to adminUid,
                        "actorRole" to "ADMIN",
                        "action" to if (confirm) "FRACTIONAL_CONFIRMED" else "FRACTIONAL_REJECTED",
                        "targetPath" to ordRef.path,
                        "details" to "${MoneyUtils.mgToGramsString(order.weightMg)} عيار ${order.karat}",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ordRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdatePricingConfig(config: PlatformPricingConfig): Result<Unit> {
        val adminUid = requireUserId()
        val cfgRef = db.collection("platform_config").document("pricing")
        val histRef = cfgRef.collection("history").document("price_${System.currentTimeMillis()}")
        val rec = PlatformPricingConfigRecord(
            reference24kPerGramPiasters = config.reference24kPerGramPiasters,
            reference21kPerGramPiasters = config.reference21kPerGramPiasters,
            reference18kPerGramPiasters = config.reference18kPerGramPiasters,
            valuation24kPerGramPiasters = config.valuation24kPerGramPiasters,
            valuation21kPerGramPiasters = config.valuation21kPerGramPiasters,
            valuation18kPerGramPiasters = config.valuation18kPerGramPiasters,
            merchantPolicyNote = config.merchantPolicyNote.trim(),
            updatedByAdminUid = adminUid,
        )
        val todayKey = com.example.data.KingGoldRepository.todayDateKey()
        val dailyRef = db.collection("daily_gold_prices").document(todayKey)
        val validDailyOrder = config.reference18kPerGramPiasters >= 100000L &&
            config.reference21kPerGramPiasters > config.reference18kPerGramPiasters &&
            config.reference24kPerGramPiasters > config.reference21kPerGramPiasters
        return try {
            val dailyExists = if (validDailyOrder) dailyRef.get().await().exists() else false
            db.runBatch { batch ->
                batch.set(cfgRef, rec.toWriteMap())
                batch.set(histRef, rec.toWriteMap())
                if (validDailyOrder) {
                    val dailyRec = DailyGoldPriceRecord(
                        id = todayKey,
                        dateKey = todayKey,
                        dayLabel = "اليوم",
                        price24kPerGramPiasters = config.reference24kPerGramPiasters,
                        price21kPerGramPiasters = config.reference21kPerGramPiasters,
                        price18kPerGramPiasters = config.reference18kPerGramPiasters,
                        source = "ADMIN_PRICING",
                        published = true,
                        updatedByAdminUid = adminUid,
                    )
                    if (dailyExists) {
                        batch.update(dailyRef, dailyRec.toUpdateMap())
                    } else {
                        batch.set(dailyRef, dailyRec.toCreateMap())
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, cfgRef.path)
            Result.failure(e)
        }
    }
}
