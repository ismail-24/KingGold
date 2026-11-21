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
import com.example.ui.navigation.Routes
import java.util.Locale
import kotlin.math.abs

data class CachedPriceAlertRule(
    val enabled: Boolean = true,
    val above: Boolean = false,
    val karat: Int = 24,
    val thresholdText: String = "6000",
)

object GoldPriceNotificationHelper {

    const val EXTRA_TARGET_ROUTE = "extra_target_route"
    private const val PREFS_NAME = "kinggold_fcm_alert_prefs"
    private const val KEY_ENABLED = "alert_enabled"
    private const val KEY_ABOVE = "alert_above"
    private const val KEY_KARAT = "alert_karat"
    private const val KEY_TARGET = "alert_target"
    private const val NOTIFICATION_ID_24K_DROP = 2401
    private const val NOTIFICATION_ID_FLUCTUATION = 2402
    private const val NOTIFICATION_ID_SHOP_OFFER_BASE = 2500

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun saveAlertRuleCache(
        context: Context,
        enabled: Boolean,
        above: Boolean,
        karat: Int,
        thresholdText: String,
    ) {
        val safeKarat = if (karat in setOf(18, 21, 24)) karat else 24
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putBoolean(KEY_ABOVE, above)
            .putInt(KEY_KARAT, safeKarat)
            .putString(KEY_TARGET, thresholdText.trim())
            .commit()
    }

    fun loadAlertRuleCache(context: Context): CachedPriceAlertRule {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val karat = prefs.getInt(KEY_KARAT, 24).let { if (it in setOf(18, 21, 24)) it else 24 }
        return CachedPriceAlertRule(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            above = prefs.getBoolean(KEY_ABOVE, false),
            karat = karat,
            thresholdText = prefs.getString(KEY_TARGET, "6000") ?: "6000",
        )
    }

    fun ensureNotificationChannel(context: Context) {
        ensureAllNotificationChannels(context)
    }

    fun ensureAllNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val priceChannel = NotificationChannel(
            context.getString(R.string.fcm_price_alert_channel_id),
            context.getString(R.string.fcm_price_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.fcm_price_alert_channel_desc)
            enableVibration(true)
        }

        val offerChannel = NotificationChannel(
            context.getString(R.string.fcm_shop_offer_channel_id),
            context.getString(R.string.fcm_shop_offer_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.fcm_shop_offer_channel_desc)
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(priceChannel, offerChannel))
    }

    fun shouldTrigger24KDropAlert(
        alertEnabled: Boolean,
        alertAbove: Boolean,
        alertKarat: Int,
        thresholdText: String,
        current24KPrice: Double,
    ): Double? {
        if (!alertEnabled || alertAbove || alertKarat != 24) {
            return null
        }
        return shouldTriggerPriceThresholdAlert(
            alertEnabled = true,
            alertAbove = false,
            alertKarat = 24,
            thresholdText = thresholdText,
            currentKaratPrice = current24KPrice,
        )
    }

    fun shouldTriggerPriceThresholdAlert(
        alertEnabled: Boolean,
        alertAbove: Boolean,
        alertKarat: Int,
        thresholdText: String,
        currentKaratPrice: Double,
    ): Double? {
        if (!alertEnabled || alertKarat !in setOf(18, 21, 24) || currentKaratPrice <= 0.0) {
            return null
        }
        val threshold = thresholdText.trim().toDoubleOrNull() ?: return null
        if (threshold <= 0.0) return null
        val triggered = if (alertAbove) currentKaratPrice > threshold else currentKaratPrice < threshold
        return if (triggered) threshold else null
    }

    fun buildLaunchIntent(context: Context, targetRoute: String): Intent {
        val safeRoute = targetRoute.takeIf { Routes.isSupportedInternalRoute(it) } ?: Routes.HOME
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_ROUTE, safeRoute)
        }
    }

    fun show24KPriceDropAlert(
        context: Context,
        currentPriceEgp: Double,
        thresholdEgp: Double,
        customTitle: String? = null,
        customBody: String? = null,
    ): Boolean = showGoldPriceThresholdAlert(
        context = context,
        karat = 24,
        currentPriceEgp = currentPriceEgp,
        thresholdEgp = thresholdEgp,
        isAbove = false,
        customTitle = customTitle,
        customBody = customBody,
    )

    fun showGoldPriceThresholdAlert(
        context: Context,
        karat: Int,
        currentPriceEgp: Double,
        thresholdEgp: Double,
        isAbove: Boolean = false,
        customTitle: String? = null,
        customBody: String? = null,
    ): Boolean {
        ensureAllNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val safeKarat = if (karat in setOf(18, 21, 24)) karat else 24
        val channelId = context.getString(R.string.fcm_price_alert_channel_id)
        val formattedPrice = String.format(Locale.US, "%,.0f", currentPriceEgp)
        val formattedThreshold = String.format(Locale.US, "%,.0f", thresholdEgp)

        val defaultTitle = if (isAbove) {
            "ارتفع سعر الذهب عيار $safeKarat الآن"
        } else {
            "انخفض سعر الذهب عيار $safeKarat الآن"
        }
        val defaultBody = if (isAbove) {
            "تجاوز عيار $safeKarat الحد المحدد ($formattedThreshold ج.م) ليصل إلى $formattedPrice ج.م/جم."
        } else {
            "وصل عيار $safeKarat إلى $formattedPrice ج.م/جم (أقل من الحد المحدد $formattedThreshold ج.م)."
        }

        val title = customTitle?.takeIf { it.isNotBlank() } ?: defaultTitle
        val body = customBody?.takeIf { it.isNotBlank() } ?: defaultBody
        val notificationId = if (safeKarat == 24 && !isAbove) {
            NOTIFICATION_ID_24K_DROP
        } else {
            NOTIFICATION_ID_24K_DROP + safeKarat + if (isAbove) 100 else 0
        }

        return postNotification(
            context = context,
            channelId = channelId,
            notificationId = notificationId,
            title = title,
            body = body,
            targetRoute = Routes.PRICE_INSIGHT,
        )
    }

    fun showPriceFluctuationAlert(
        context: Context,
        karat: Int,
        currentPriceEgp: Double,
        changePercent: Double,
        customTitle: String? = null,
        customBody: String? = null,
    ): Boolean {
        ensureAllNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val channelId = context.getString(R.string.fcm_price_alert_channel_id)
        val direction = if (changePercent >= 0) "ارتفع" else "انخفض"
        val formattedPrice = String.format(Locale.US, "%,.0f", currentPriceEgp)
        val formattedChange = String.format(Locale.US, "%.2f", abs(changePercent))

        val title = customTitle?.takeIf { it.isNotBlank() }
            ?: "تحرك ملحوظ في سعر الذهب عيار $karat"
        val body = customBody?.takeIf { it.isNotBlank() }
            ?: "$direction سعر عيار $karat بنسبة $formattedChange% ليصل إلى $formattedPrice ج.م/جم."

        return postNotification(
            context = context,
            channelId = channelId,
            notificationId = NOTIFICATION_ID_FLUCTUATION,
            title = title,
            body = body,
            targetRoute = Routes.PRICE_INSIGHT,
        )
    }

    fun showNearbyShopOfferAlert(
        context: Context,
        shopId: String,
        shopName: String,
        offerTitle: String? = null,
        offerBody: String? = null,
    ): Boolean {
        ensureAllNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val channelId = context.getString(R.string.fcm_shop_offer_channel_id)
        val cleanShopName = shopName.ifBlank { "محل ذهب قريب" }
        val title = offerTitle?.takeIf { it.isNotBlank() }
            ?: "عرض خاص من $cleanShopName"
        val body = offerBody?.takeIf { it.isNotBlank() }
            ?: "خصم حصري على المصنعية لفترة محدودة لدى $cleanShopName القريب منك."
        val targetRoute = if (shopId.isNotBlank()) Routes.shopDetail(shopId) else Routes.HOME
        val notificationId = NOTIFICATION_ID_SHOP_OFFER_BASE + (shopId.hashCode() and 0x0FFF)

        return postNotification(
            context = context,
            channelId = channelId,
            notificationId = notificationId,
            title = title,
            body = body,
            targetRoute = targetRoute,
        )
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        targetRoute: String,
    ): Boolean {
        val launchIntent = buildLaunchIntent(context, targetRoute)
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
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
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
