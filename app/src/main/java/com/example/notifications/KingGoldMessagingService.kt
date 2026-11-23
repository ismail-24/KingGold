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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class FcmIncomingPriceEvent(
    val price24k: Double? = null,
    val price21k: Double? = null,
    val price18k: Double? = null,
    val changePercent: Double? = null,
    val triggeredKarat: Int? = null,
    val triggeredThreshold: Double? = null,
    val triggeredAbove: Boolean? = null,
    val alertTitle: String? = null,
    val alertBody: String? = null,
)

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

        private val _incomingEvents = MutableSharedFlow<FcmIncomingPriceEvent>(extraBufferCapacity = 16)
        val incomingEvents: SharedFlow<FcmIncomingPriceEvent> = _incomingEvents.asSharedFlow()

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

            val price24k = data["price24k"]?.toDoubleOrNull()
            val price21k = data["price21k"]?.toDoubleOrNull()
            val price18k = data["price18k"]?.toDoubleOrNull()
            val changePercent = data["changePercent"]?.toDoubleOrNull()
            val rule = GoldPriceNotificationHelper.loadAlertRuleCache(context)
            val karat = data["karat"]?.toIntOrNull()?.takeIf { it in setOf(18, 21, 24) } ?: rule.karat
            val incomingPriceForKarat = priceForKaratFromPayload(data, karat)

            GoldPriceAppWidgetProvider.syncFromPayloadPrices(
                context = context,
                price24K = price24k,
                price21K = price21k,
                price18K = price18k,
                changePercent = changePercent,
            )

            if (type == "price_fluctuation" || (type.isBlank() && changePercent != null)) {
                val fluctuationShown = GoldPriceNotificationHelper.showPriceFluctuationAlert(
                    context = context,
                    karat = karat,
                    currentPriceEgp = incomingPriceForKarat,
                    changePercent = changePercent ?: 0.0,
                    customTitle = title,
                    customBody = body,
                )
                val targetPrice = priceForKaratFromPayload(data, rule.karat)
                val crossing = GoldPriceNotificationHelper.evaluateAndRecordThresholdCrossing(
                    context = context,
                    currentKaratPrice = targetPrice,
                )
                if (crossing != null) {
                    GoldPriceNotificationHelper.showGoldPriceThresholdAlert(
                        context = context,
                        karat = crossing.karat,
                        currentPriceEgp = crossing.currentPriceEgp,
                        thresholdEgp = crossing.thresholdEgp,
                        isAbove = crossing.isAbove,
                    )
                }
                _incomingEvents.tryEmit(
                    FcmIncomingPriceEvent(
                        price24k = price24k,
                        price21k = price21k,
                        price18k = price18k,
                        changePercent = changePercent,
                        triggeredKarat = crossing?.karat,
                        triggeredThreshold = crossing?.thresholdEgp,
                        triggeredAbove = crossing?.isAbove,
                        alertTitle = title,
                        alertBody = body,
                    ),
                )
                return fluctuationShown || crossing != null
            }

            // Real-time background price tick broadcast: notify only when user's configured threshold is crossed
            if (type in setOf("price_update", "gold_price_tick", "threshold_check")) {
                val targetPrice = priceForKaratFromPayload(data, rule.karat)
                val crossing = GoldPriceNotificationHelper.evaluateAndRecordThresholdCrossing(
                    context = context,
                    currentKaratPrice = targetPrice,
                )
                if (crossing == null) {
                    _incomingEvents.tryEmit(
                        FcmIncomingPriceEvent(
                            price24k = price24k,
                            price21k = price21k,
                            price18k = price18k,
                            changePercent = changePercent,
                        ),
                    )
                    return false
                }

                val posted = GoldPriceNotificationHelper.showGoldPriceThresholdAlert(
                    context = context,
                    karat = crossing.karat,
                    currentPriceEgp = crossing.currentPriceEgp,
                    thresholdEgp = crossing.thresholdEgp,
                    isAbove = crossing.isAbove,
                    customTitle = title,
                    customBody = body,
                )
                _incomingEvents.tryEmit(
                    FcmIncomingPriceEvent(
                        price24k = price24k,
                        price21k = price21k,
                        price18k = price18k,
                        changePercent = changePercent,
                        triggeredKarat = crossing.karat,
                        triggeredThreshold = crossing.thresholdEgp,
                        triggeredAbove = crossing.isAbove,
                        alertTitle = title,
                        alertBody = body,
                    ),
                )
                return posted
            }

            val explicitDirection = data["direction"]?.trim()?.lowercase()
            val isAbove = when (explicitDirection) {
                "above", "up", "surge" -> true
                "below", "down", "drop" -> false
                else -> type == "price_surge" || (type != "price_drop" && rule.above)
            }
            val threshold = data["threshold"]?.toDoubleOrNull()
                ?: rule.thresholdText.toDoubleOrNull()
                ?: 0.0
            if (incomingPriceForKarat > 0.0 || !title.isNullOrBlank() || !body.isNullOrBlank()) {
                val effectivePrice = if (incomingPriceForKarat > 0.0) incomingPriceForKarat else threshold
                val effectiveThreshold = if (threshold > 0.0) threshold else effectivePrice
                val posted = GoldPriceNotificationHelper.showGoldPriceThresholdAlert(
                    context = context,
                    karat = karat,
                    currentPriceEgp = effectivePrice,
                    thresholdEgp = effectiveThreshold,
                    isAbove = isAbove,
                    customTitle = title,
                    customBody = body,
                )
                _incomingEvents.tryEmit(
                    FcmIncomingPriceEvent(
                        price24k = price24k,
                        price21k = price21k,
                        price18k = price18k,
                        changePercent = changePercent,
                        triggeredKarat = karat,
                        triggeredThreshold = effectiveThreshold,
                        triggeredAbove = isAbove,
                        alertTitle = title,
                        alertBody = body,
                    ),
                )
                return posted
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
