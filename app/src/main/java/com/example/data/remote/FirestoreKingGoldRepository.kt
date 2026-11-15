package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.KingGoldRepository
import com.example.model.GoldOrder
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

    companion object {
        const val PREFS_DOC_ID = "settings"
    }
}
