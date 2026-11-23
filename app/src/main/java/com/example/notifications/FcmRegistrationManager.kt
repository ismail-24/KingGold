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
    const val TOPIC_PRICE_DIRECTION_ABOVE = "gold_price_alerts_above"
    const val TOPIC_PRICE_DIRECTION_BELOW = "gold_price_alerts_below"
    const val TOPIC_PRICE_FLUCTUATIONS = "gold_price_fluctuations"
    const val TOPIC_NEARBY_SHOP_OFFERS = "nearby_gold_shop_offers"
    private const val TAG = "FcmRegistration"

    fun topicForKarat(karat: Int): String = when (karat) {
        21 -> TOPIC_GOLD_21K_ALERTS
        18 -> TOPIC_GOLD_18K_ALERTS
        else -> TOPIC_GOLD_24K_ALERTS
    }

    fun activeTopicsFor(
        enabled: Boolean,
        alertKarat: Int,
        alertAbove: Boolean,
        shopOffersEnabled: Boolean = true,
    ): Set<String> = buildSet {
        if (enabled) {
            add(topicForKarat(alertKarat))
            add(TOPIC_PRICE_THRESHOLD_ALL)
            add(TOPIC_PRICE_FLUCTUATIONS)
            add(if (alertAbove) TOPIC_PRICE_DIRECTION_ABOVE else TOPIC_PRICE_DIRECTION_BELOW)
        }
        if (shopOffersEnabled) {
            add(TOPIC_NEARBY_SHOP_OFFERS)
        }
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
        val existingRule = GoldPriceNotificationHelper.loadAlertRuleCache(context)
        val effectiveTarget = alertTarget.trim().ifBlank { existingRule.thresholdText }
        GoldPriceNotificationHelper.saveAlertRuleCache(
            context = context,
            enabled = enabled,
            above = alertAbove,
            karat = alertKarat,
            thresholdText = effectiveTarget,
        )

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

        val desiredTopics = activeTopicsFor(
            enabled = enabled,
            alertKarat = alertKarat,
            alertAbove = alertAbove,
            shopOffersEnabled = shopOffersEnabled,
        )
        val allManagedTopics = listOf(
            TOPIC_GOLD_24K_ALERTS,
            TOPIC_GOLD_21K_ALERTS,
            TOPIC_GOLD_18K_ALERTS,
            TOPIC_PRICE_THRESHOLD_ALL,
            TOPIC_PRICE_DIRECTION_ABOVE,
            TOPIC_PRICE_DIRECTION_BELOW,
            TOPIC_PRICE_FLUCTUATIONS,
            TOPIC_NEARBY_SHOP_OFFERS,
        )
        runCatching {
            allManagedTopics.forEach { topic ->
                if (topic in desiredTopics) {
                    Firebase.messaging.subscribeToTopic(topic).await()
                } else {
                    Firebase.messaging.unsubscribeFromTopic(topic).await()
                }
            }
        }.onFailure {
            Log.w(TAG, "Topic subscription update skipped: ${it.localizedMessage}")
        }
    }
}
