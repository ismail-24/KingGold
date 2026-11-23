package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.remote.FirestoreKingGoldRepository
import com.example.widget.GoldPriceAppWidgetProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KingGoldMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token")
        val uid = Firebase.auth.currentUser?.uid ?: return
        val appContext = applicationContext
        serviceScope.launch {
            runCatching {
                val dbId = appContext.getString(R.string.firestore_database_id)
                val repo = FirestoreKingGoldRepository(FirebaseFirestore.getInstance(dbId), Firebase.auth)
                if (Firebase.auth.currentUser?.uid == uid) {
                    repo.updateFcmToken(token)
                }
            }.onFailure {
                Log.w(TAG, "Failed to update refreshed FCM token in Firestore", it)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        handleIncomingFcmData(
            context = applicationContext,
            data = remoteMessage.data,
            notifTitle = remoteMessage.notification?.title,
            notifBody = remoteMessage.notification?.body,
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "KingGoldFCMService"

        internal fun handleIncomingFcmData(
            context: Context,
            data: Map<String, String>,
            notifTitle: String? = null,
            notifBody: String? = null,
        ): Boolean {
            val type = data["type"]?.trim()?.lowercase().orEmpty()
            val shopId = data["shopId"]?.trim().orEmpty()
            val shopName = data["shopName"]?.trim().orEmpty()
            val title = notifTitle ?: data["title"]
            val body = notifBody ?: data["body"]

            if (type == "shop_offer" || type == "nearby_offer" || shopId.isNotBlank()) {
                return GoldPriceNotificationHelper.showNearbyShopOfferAlert(
                    context = context,
                    shopId = shopId,
                    shopName = shopName,
                    offerTitle = title,
                    offerBody = body,
                )
            }

            val changePercent = data["changePercent"]?.toDoubleOrNull()
            val karat = data["karat"]?.toIntOrNull()?.takeIf { it in setOf(18, 21, 24) } ?: 24
            val incomingPriceForKarat = priceForKaratFromPayload(data, karat)

            GoldPriceAppWidgetProvider.syncFromPayloadPrices(
                context = context,
                price24K = data["price24k"]?.toDoubleOrNull(),
                price21K = data["price21k"]?.toDoubleOrNull(),
                price18K = data["price18k"]?.toDoubleOrNull(),
                changePercent = changePercent,
            )

            if (type == "price_fluctuation" || changePercent != null) {
                return GoldPriceNotificationHelper.showPriceFluctuationAlert(
                    context = context,
                    karat = karat,
                    currentPriceEgp = incomingPriceForKarat,
                    changePercent = changePercent ?: 0.0,
                    customTitle = title,
                    customBody = body,
                )
            }

            // Real-time background price tick broadcast: only notify if user's configured threshold is crossed
            if (type == "price_update" || type == "gold_price_tick") {
                val rule = GoldPriceNotificationHelper.loadAlertRuleCache(context)
                val targetPrice = priceForKaratFromPayload(data, rule.karat)
                val breachedThreshold = GoldPriceNotificationHelper.shouldTriggerPriceThresholdAlert(
                    alertEnabled = rule.enabled,
                    alertAbove = rule.above,
                    alertKarat = rule.karat,
                    thresholdText = rule.thresholdText,
                    currentKaratPrice = targetPrice,
                ) ?: return false

                return GoldPriceNotificationHelper.showGoldPriceThresholdAlert(
                    context = context,
                    karat = rule.karat,
                    currentPriceEgp = targetPrice,
                    thresholdEgp = breachedThreshold,
                    isAbove = rule.above,
                    customTitle = title,
                    customBody = body,
                )
            }

            val threshold = data["threshold"]?.toDoubleOrNull() ?: 0.0
            val isAbove = data["direction"]?.trim()?.equals("above", ignoreCase = true) == true
            if (incomingPriceForKarat > 0.0 || !title.isNullOrBlank() || !body.isNullOrBlank()) {
                return GoldPriceNotificationHelper.showGoldPriceThresholdAlert(
                    context = context,
                    karat = karat,
                    currentPriceEgp = if (incomingPriceForKarat > 0.0) incomingPriceForKarat else threshold,
                    thresholdEgp = if (threshold > 0.0) threshold else incomingPriceForKarat,
                    isAbove = isAbove,
                    customTitle = title,
                    customBody = body,
                )
            }
            return false
        }

        private fun priceForKaratFromPayload(data: Map<String, String>, karat: Int): Double {
            val specific = when (karat) {
                24 -> data["price24k"]?.toDoubleOrNull()
                21 -> data["price21k"]?.toDoubleOrNull()
                18 -> data["price18k"]?.toDoubleOrNull()
                else -> null
            }
            return specific
                ?: data["price"]?.takeIf { data["karat"]?.toIntOrNull()?.let { k -> k == karat } ?: true }?.toDoubleOrNull()
                ?: data["price24k"]?.toDoubleOrNull()
                ?: data["price"]?.toDoubleOrNull()
                ?: 0.0
        }
    }
}
