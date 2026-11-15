package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.remote.FirestoreKingGoldRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await

object FcmRegistrationManager {

    const val TOPIC_GOLD_24K_ALERTS = "gold_24k_price_alerts"
    private const val TAG = "FcmRegistration"

    suspend fun syncFcmAlertSubscription(
        context: Context,
        enabled: Boolean,
        alertKarat: Int,
        alertAbove: Boolean,
        onTokenSynced: ((String) -> Unit)? = null,
    ) {
        val token = runCatching { Firebase.messaging.token.await() }
            .onFailure { Log.w(TAG, "Unable to retrieve FCM registration token", it) }
            .getOrNull()

        if (!token.isNullOrBlank()) {
            onTokenSynced?.invoke(token)
            val uid = Firebase.auth.currentUser?.uid
            if (uid != null) {
                runCatching {
                    val dbId = context.applicationContext.getString(R.string.firestore_database_id)
                    val repo = FirestoreKingGoldRepository(FirebaseFirestore.getInstance(dbId), Firebase.auth)
                    repo.updateFcmToken(token)
                }.onFailure { Log.w(TAG, "Unable to persist FCM token to Firestore", it) }
            }
        }

        val subscribe24KDrop = enabled && alertKarat == 24 && !alertAbove
        runCatching {
            if (subscribe24KDrop) {
                Firebase.messaging.subscribeToTopic(TOPIC_GOLD_24K_ALERTS).await()
            } else {
                Firebase.messaging.unsubscribeFromTopic(TOPIC_GOLD_24K_ALERTS).await()
            }
        }.onFailure {
            Log.w(TAG, "Topic subscription update skipped: ${it.localizedMessage}")
        }
    }
}
