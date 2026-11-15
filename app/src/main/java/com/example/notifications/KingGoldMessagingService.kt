package com.example.notifications

import android.util.Log
import com.example.R
import com.example.data.remote.FirestoreKingGoldRepository
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
        val data = remoteMessage.data
        val notif = remoteMessage.notification

        val price24k = data["price24k"]?.toDoubleOrNull()
            ?: data["price"]?.toDoubleOrNull()
            ?: 0.0
        val threshold = data["threshold"]?.toDoubleOrNull() ?: 0.0
        val title = notif?.title ?: data["title"]
        val body = notif?.body ?: data["body"]

        if (price24k > 0.0 || !title.isNullOrBlank() || !body.isNullOrBlank()) {
            GoldPriceNotificationHelper.show24KPriceDropAlert(
                context = applicationContext,
                currentPriceEgp = if (price24k > 0.0) price24k else threshold,
                thresholdEgp = if (threshold > 0.0) threshold else price24k,
                customTitle = title,
                customBody = body,
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "KingGoldFCMService"
    }
}
