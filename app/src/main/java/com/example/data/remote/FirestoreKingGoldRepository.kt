package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.KingGoldRepository
import com.example.model.DailyGoldPrice
import com.example.model.GoldOrder
import com.example.model.OrderStatus
import com.example.model.Shop
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

class FirestoreKingGoldRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun ordersCollection(userId: String) =
        db.collection("users").document(userId).collection("orders")

    private fun preferencesDoc(userId: String) =
        db.collection("users").document(userId).collection("preferences").document(PREFS_DOC_ID)

    private fun goldShopsCollection() =
        db.collection("gold_shops")

    fun observeActiveGoldShops(): Flow<List<Shop>> = flow {
        val path = "gold_shops?active=true"
        emitAll(
            goldShopsCollection()
                .whereEqualTo("active", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GoldShopLocationRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).map { it.toShop() }
                        .sortedBy { it.distanceKm }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw (firestoreError ?: error)
                },
        )
    }

    suspend fun seedInitialGoldShopsIfAdmin(): Result<Unit> {
        val uid = requireUserId()
        val path = "gold_shops"
        return try {
            val existing = goldShopsCollection().whereEqualTo("active", true).limit(1).get().await()
            if (existing.isEmpty) {
                val batch = db.batch()
                for (shop in KingGoldRepository.shops) {
                    val ref = goldShopsCollection().document(shop.id)
                    val record = GoldShopLocationRecord.fromShop(
                        shop = shop,
                        ownerUid = uid,
                        active = true,
                        includeGeoPoint = true,
                    )
                    batch.set(ref, record.toCreateMap())
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveGoldShopLocation(
        record: GoldShopLocationRecord,
        shopId: String? = null,
    ): Result<String> {
        val uid = requireUserId()
        val lat = record.resolvedLatitude()
        val lng = record.resolvedLongitude()
        if (!lat.isFinite() || !lng.isFinite() || lat !in -85.0..85.0 || lng !in -180.0..180.0 || (lat == 0.0 && lng == 0.0)) {
            return Result.failure(IllegalArgumentException("إحداثيات موقع المحل غير صالحة."))
        }
        val docRef = if (!shopId.isNullOrBlank()) {
            goldShopsCollection().document(shopId.trim())
        } else {
            goldShopsCollection().document()
        }
        val scopedRecord = record.copy(
            id = docRef.id,
            ownerUid = record.ownerUid.ifBlank { uid },
        )
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(scopedRecord.toUpdateMap()).await()
            } else {
                docRef.set(scopedRecord.toCreateMap()).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun dailyGoldPricesCollection() =
        db.collection("daily_gold_prices")

    fun observeDailyGoldPrices(): Flow<List<DailyGoldPrice>> = flow {
        val path = "daily_gold_prices?published=true"
        emitAll(
            dailyGoldPricesCollection()
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyGoldPriceRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).map { it.toDailyGoldPrice() }
                        .filter { it.dateKey.isNotBlank() }
                        .sortedBy { it.dateKey }
                        .takeLast(30)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw (firestoreError ?: error)
                },
        )
    }

    suspend fun seedInitialDailyGoldPricesIfAdmin(
        current24KPrice: Double,
        current21KPrice: Double,
        current18KPrice: Double,
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "daily_gold_prices"
        return try {
            val existing = dailyGoldPricesCollection()
                .whereEqualTo("published", true)
                .limit(1)
                .get()
                .await()
            if (existing.isEmpty) {
                val seedSeries = KingGoldRepository.dailyGoldPriceFallbackSeries(
                    current24KPrice = current24KPrice,
                    current21KPrice = current21KPrice,
                    current18KPrice = current18KPrice,
                )
                val batch = db.batch()
                for (entry in seedSeries) {
                    val ref = dailyGoldPricesCollection().document(entry.dateKey)
                    val record = DailyGoldPriceRecord.fromDailyGoldPrice(
                        daily = entry,
                        updatedByAdminUid = uid,
                        published = true,
                    )
                    batch.set(ref, record.toCreateMap())
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordDailyGoldPriceIfAdmin(
        dateKey: String = KingGoldRepository.todayDateKey(),
        dayLabel: String = "اليوم",
        price24kPerGramPiasters: Long,
        price21kPerGramPiasters: Long,
        price18kPerGramPiasters: Long,
        source: String = "ADMIN_PRICING",
    ): Result<Unit> {
        val uid = requireUserId()
        if (!dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
            return Result.failure(IllegalArgumentException("صيغة تاريخ السعر غير صالحة."))
        }
        if (price18kPerGramPiasters < 100000L ||
            price21kPerGramPiasters <= price18kPerGramPiasters ||
            price24kPerGramPiasters <= price21kPerGramPiasters
        ) {
            return Result.failure(IllegalArgumentException("يجب أن تكون أسعار الأعيرة موجبة وبترتيب 24 > 21 > 18."))
        }
        val docRef = dailyGoldPricesCollection().document(dateKey)
        val record = DailyGoldPriceRecord(
            id = dateKey,
            dateKey = dateKey,
            dayLabel = dayLabel.trim().ifBlank { dateKey },
            price24kPerGramPiasters = price24kPerGramPiasters,
            price21kPerGramPiasters = price21kPerGramPiasters,
            price18kPerGramPiasters = price18kPerGramPiasters,
            source = source.takeIf { it in setOf("SEED", "ADMIN_PRICING", "GLOBAL_FEED") } ?: "ADMIN_PRICING",
            published = true,
            updatedByAdminUid = uid,
        )
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(record.toUpdateMap()).await()
            } else {
                docRef.set(record.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeOrders(userId: String): Flow<List<GoldOrderRecord>> = flow {
        val path = "users/$userId/orders"
        emitAll(
            ordersCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GoldOrderRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw (firestoreError ?: error)
                },
        )
    }

    fun observePreferences(userId: String): Flow<UserPreferencesRecord?> = flow {
        val path = "users/$userId/preferences/$PREFS_DOC_ID"
        emitAll(
            preferencesDoc(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserPreferencesRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw (firestoreError ?: error)
                },
        )
    }

    suspend fun ensureInitialUserData(userId: String) {
        val uid = requireUserId()
        if (uid != userId) return

        val prefsRef = preferencesDoc(uid)
        try {
            val prefsSnap = prefsRef.get().await()
            if (!prefsSnap.exists()) {
                prefsRef.set(UserPreferencesRecord(userId = uid).toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, prefsRef.path)
        }

        val ordersPath = "users/$uid/orders"
        try {
            val existingOrders = ordersCollection(uid).limit(1).get().await()
            if (existingOrders.isEmpty) {
                for (seed in KingGoldRepository.initialOrders) {
                    val record = GoldOrderRecord.fromGoldOrder(uid, seed)
                    ordersCollection(uid).document(seed.id).set(record.toCreateMap()).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ordersPath)
        }
    }

    suspend fun createOrder(order: GoldOrder): Result<String> {
        val uid = requireUserId()
        val docRef = if (order.id.isNotBlank()) {
            ordersCollection(uid).document(order.id)
        } else {
            ordersCollection(uid).document()
        }
        val record = GoldOrderRecord.fromGoldOrder(uid, order.copy(id = docRef.id))
        return try {
            docRef.set(record.toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getOrders(): Result<List<GoldOrderRecord>> {
        val uid = requireUserId()
        val path = "users/$uid/orders"
        return try {
            val snapshot = ordersCollection(uid).get().await()
            val list = snapshot.toObjects(
                GoldOrderRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getOrderById(orderId: String): Result<GoldOrderRecord?> {
        val uid = requireUserId()
        val docRef = ordersCollection(uid).document(orderId)
        return try {
            val snapshot = docRef.get().await()
            val record = if (snapshot.exists()) {
                snapshot.toObject(
                    GoldOrderRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                )
            } else {
                null
            }
            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun completePickup(orderId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = ordersCollection(uid).document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "status" to OrderStatus.COMPLETED.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitRating(orderId: String, stars: Int): Result<Unit> {
        val uid = requireUserId()
        val docRef = ordersCollection(uid).document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "ratingGiven" to stars.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitIssueReport(orderId: String, reason: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = ordersCollection(uid).document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "status" to OrderStatus.DISPUTED.name,
                    "disputeReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun savePreferences(prefs: UserPreferencesRecord): Result<Unit> {
        val uid = requireUserId()
        val docRef = preferencesDoc(uid)
        val scopedPrefs = prefs.copy(userId = uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(scopedPrefs.toUpdateMap()).await()
            } else {
                docRef.set(scopedPrefs.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun cartDoc(userId: String) =
        db.collection("users").document(userId).collection("cart").document(CART_DOC_ID)

    fun observeCart(userId: String): Flow<UserCartRecord?> = flow {
        val path = "users/$userId/cart/$CART_DOC_ID"
        emitAll(
            cartDoc(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserCartRecord::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw (firestoreError ?: error)
                },
        )
    }

    suspend fun saveCart(cart: UserCartRecord): Result<Unit> {
        val uid = requireUserId()
        val docRef = cartDoc(uid)
        val scoped = cart.copy(userId = uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(scoped.toUpdateMap()).await()
            } else {
                docRef.set(scoped.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearCart(): Result<Unit> {
        val uid = requireUserId()
        val docRef = cartDoc(uid)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return Result.success(Unit)
        val uid = requireUserId()
        val docRef = preferencesDoc(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val current = snap.toObject(UserPreferencesRecord::class.java)
                    ?: UserPreferencesRecord(userId = uid)
                val updated = current.copy(userId = uid, fcmToken = trimmed)
                docRef.update(updated.toUpdateMap()).await()
            } else {
                docRef.set(UserPreferencesRecord(userId = uid, fcmToken = trimmed).toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val PREFS_DOC_ID = "settings"
        const val CART_DOC_ID = "current"
    }
}
