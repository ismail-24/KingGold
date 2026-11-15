package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

object GoldPriceNotificationHelper {

    private const val NOTIFICATION_ID_24K_DROP = 2401

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channelId = context.getString(R.string.fcm_price_alert_channel_id)
        val channelName = context.getString(R.string.fcm_price_alert_channel_name)
        val channelDesc = context.getString(R.string.fcm_price_alert_channel_desc)

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = channelDesc
            enableVibration(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun shouldTrigger24KDropAlert(
        alertEnabled: Boolean,
        alertAbove: Boolean,
        alertKarat: Int,
        thresholdText: String,
        current24KPrice: Double,
    ): Double? {
        if (!alertEnabled || alertAbove || alertKarat != 24 || current24KPrice <= 0.0) {
            return null
        }
        val threshold = thresholdText.trim().toDoubleOrNull() ?: return null
        if (threshold <= 0.0) return null
        return if (current24KPrice < threshold) threshold else null
    }

    fun show24KPriceDropAlert(
        context: Context,
        currentPriceEgp: Double,
        thresholdEgp: Double,
        customTitle: String? = null,
        customBody: String? = null,
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val channelId = context.getString(R.string.fcm_price_alert_channel_id)
        val formattedPrice = String.format(Locale.US, "%,.0f", currentPriceEgp)
        val formattedThreshold = String.format(Locale.US, "%,.0f", thresholdEgp)

        val title = customTitle?.takeIf { it.isNotBlank() }
            ?: "انخفض سعر الذهب عيار 24 الآن"
        val body = customBody?.takeIf { it.isNotBlank() }
            ?: "وصل عيار 24 إلى $formattedPrice ج.م/جم (أقل من الحد المحدد $formattedThreshold ج.م)."

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_24K_DROP,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_gold)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_24K_DROP, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
