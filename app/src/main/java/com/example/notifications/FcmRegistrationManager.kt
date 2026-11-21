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
    const val TOPIC_GOLD_21K_ALERTS = "gold_21k_price_alerts"
    const val TOPIC_GOLD_18K_ALERTS = "gold_18k_price_alerts"
    const val TOPIC_PRICE_THRESHOLD_ALL = "gold_price_threshold_alerts"
    const val TOPIC_PRICE_FLUCTUATIONS = "gold_price_fluctuations"
    const val TOPIC_NEARBY_SHOP_OFFERS = "nearby_gold_shop_offers"
    private const val TAG = "FcmRegistration"

    fun topicForKarat(karat: Int): String = when (karat) {
        21 -> TOPIC_GOLD_21K_ALERTS
        18 -> TOPIC_GOLD_18K_ALERTS
        else -> TOPIC_GOLD_24K_ALERTS
    }

    suspend fun syncFcmAlertSubscription(
        context: Context,
        enabled: Boolean,
        alertKarat: Int,
        alertAbove: Boolean,
        alertTarget: String = "",
        shopOffersEnabled: Boolean = true,
        onTokenSynced: ((String) -> Unit)? = null,
    ) {
        if (alertTarget.isNotBlank()) {
            GoldPriceNotificationHelper.saveAlertRuleCache(
                context = context,
                enabled = enabled,
                above = alertAbove,
                karat = alertKarat,
                thresholdText = alertTarget,
            )
        }

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

        val subscribe24K = enabled && alertKarat == 24 && !alertAbove
        val subscribe21K = enabled && alertKarat == 21
        val subscribe18K = enabled && alertKarat == 18
        runCatching {
            if (subscribe24K) {
                Firebase.messaging.subscribeToTopic(TOPIC_GOLD_24K_ALERTS).await()
            } else {
                Firebase.messaging.unsubscribeFromTopic(TOPIC_GOLD_24K_ALERTS).await()
            }
            if (subscribe21K) {
                Firebase.messaging.subscribeToTopic(TOPIC_GOLD_21K_ALERTS).await()
            } else {
                Firebase.messaging.unsubscribeFromTopic(TOPIC_GOLD_21K_ALERTS).await()
            }
            if (subscribe18K) {
                Firebase.messaging.subscribeToTopic(TOPIC_GOLD_18K_ALERTS).await()
            } else {
                Firebase.messaging.unsubscribeFromTopic(TOPIC_GOLD_18K_ALERTS).await()
            }
            if (enabled) {
                Firebase.messaging.subscribeToTopic(TOPIC_PRICE_THRESHOLD_ALL).await()
                Firebase.messaging.subscribeToTopic(TOPIC_PRICE_FLUCTUATIONS).await()
            } else {
                Firebase.messaging.unsubscribeFromTopic(TOPIC_PRICE_THRESHOLD_ALL).await()
                Firebase.messaging.unsubscribeFromTopic(TOPIC_PRICE_FLUCTUATIONS).await()
            }
            if (shopOffersEnabled) {
                Firebase.messaging.subscribeToTopic(TOPIC_NEARBY_SHOP_OFFERS).await()
            } else {
                Firebase.messaging.unsubscribeFromTopic(TOPIC_NEARBY_SHOP_OFFERS).await()
            }
        }.onFailure {
            Log.w(TAG, "Topic subscription update skipped: ${it.localizedMessage}")
        }
    }
}
