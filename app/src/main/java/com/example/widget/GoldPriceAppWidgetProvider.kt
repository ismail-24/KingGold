package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.data.GlobalGoldFeedResult
import com.example.data.GlobalGoldPriceService
import com.example.model.KaratPrice
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.components.GoldFeedStatus
import com.example.ui.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

sealed interface PinWidgetResult {
    data object RequestLaunched : PinWidgetResult
    data object UnsupportedLauncher : PinWidgetResult
}

/**
 * Android Home Screen AppWidget tracking real-time gold prices across 24K, 21K, 18K,
 * and the Sovereign Gold Pound in both EGP/g and global USD/g.
 */
class GoldPriceAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val appContext = context.applicationContext
        val snapshot = GoldPriceWidgetStore.loadSnapshot(appContext)
        updateAllWidgets(appContext, appWidgetManager, appWidgetIds, snapshot)

        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                fetchAndUpdateWidgets(appContext)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appContext = context.applicationContext
        when (intent.action) {
            ACTION_REFRESH_GOLD_WIDGET -> {
                val manager = AppWidgetManager.getInstance(appContext)
                val ids = getWidgetIds(appContext, manager)
                val loadingSnapshot = GoldPriceWidgetStore.loadSnapshot(appContext)
                    .copy(statusLabel = "جاري التحديث...")
                updateAllWidgets(appContext, manager, ids, loadingSnapshot)

                val pendingResult: PendingResult? = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        fetchAndUpdateWidgets(appContext)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_TOGGLE_GOLD_WIDGET_CURRENCY -> {
                val updated = GoldPriceWidgetStore.toggleCurrencyMode(appContext)
                val manager = AppWidgetManager.getInstance(appContext)
                updateAllWidgets(appContext, manager, getWidgetIds(appContext, manager), updated)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_GOLD_WIDGET = "com.example.ACTION_REFRESH_GOLD_WIDGET"
        const val ACTION_TOGGLE_GOLD_WIDGET_CURRENCY = "com.example.ACTION_TOGGLE_GOLD_WIDGET_CURRENCY"

        @androidx.annotation.VisibleForTesting
        internal var livePriceFetcher: suspend () -> GlobalGoldFeedResult = {
            GlobalGoldPriceService.fetchLiveGlobalGoldPrices()
        }

        fun getWidgetIds(
            context: Context,
            appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
        ): IntArray {
            val component = ComponentName(context, GoldPriceAppWidgetProvider::class.java)
            return appWidgetManager.getAppWidgetIds(component)
        }

        fun syncFromPrices(
            context: Context,
            prices: List<KaratPrice>,
            feedStatus: GoldFeedStatus,
        ) {
            val appContext = context.applicationContext
            val snapshot = GoldPriceWidgetStore.saveFromKaratPrices(appContext, prices, feedStatus)
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = getWidgetIds(appContext, manager)
            if (ids.isNotEmpty()) {
                updateAllWidgets(appContext, manager, ids, snapshot)
            }
        }

        fun syncFromPayloadPrices(
            context: Context,
            price24K: Double?,
            price21K: Double?,
            price18K: Double?,
            changePercent: Double? = null,
        ) {
            val appContext = context.applicationContext
            val snapshot = GoldPriceWidgetStore.saveFromRawPrices(
                context = appContext,
                price24K = price24K,
                price21K = price21K,
                price18K = price18K,
                changePercent = changePercent,
            ) ?: return
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = getWidgetIds(appContext, manager)
            if (ids.isNotEmpty()) {
                updateAllWidgets(appContext, manager, ids, snapshot)
            }
        }

        suspend fun fetchAndUpdateWidgets(context: Context): GoldPriceWidgetSnapshot {
            val appContext = context.applicationContext
            val snapshot = when (val result = livePriceFetcher()) {
                is GlobalGoldFeedResult.Success -> {
                    GoldPriceWidgetStore.saveFromQuote(appContext, result.quote)
                }
                is GlobalGoldFeedResult.Error -> {
                    GoldPriceWidgetStore.loadSnapshot(appContext).copy(
                        isLive = false,
                        statusLabel = "مرجعي",
                    ).also { GoldPriceWidgetStore.saveSnapshot(appContext, it) }
                }
            }
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = getWidgetIds(appContext, manager)
            if (ids.isNotEmpty()) {
                updateAllWidgets(appContext, manager, ids, snapshot)
            }
            return snapshot
        }

        fun requestPinWidget(context: Context): PinWidgetResult {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, GoldPriceAppWidgetProvider::class.java)
            if (!appWidgetManager.isRequestPinAppWidgetSupported) {
                return PinWidgetResult.UnsupportedLauncher
            }
            val successCallback = PendingIntent.getBroadcast(
                context,
                4201,
                Intent(context, GoldPriceAppWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_GOLD_WIDGET
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val launched = appWidgetManager.requestPinAppWidget(provider, null, successCallback)
            return if (launched) PinWidgetResult.RequestLaunched else PinWidgetResult.UnsupportedLauncher
        }

        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            snapshot: GoldPriceWidgetSnapshot = GoldPriceWidgetStore.loadSnapshot(context),
        ) {
            if (appWidgetIds.isEmpty()) return
            appWidgetIds.forEach { widgetId ->
                val views = buildRemoteViews(context, snapshot)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        fun buildRemoteViews(
            context: Context,
            snapshot: GoldPriceWidgetSnapshot,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_gold_price_tracker)

            views.setImageViewResource(
                R.id.widget_status_dot,
                if (snapshot.isLive) R.drawable.bg_widget_live_dot else R.drawable.bg_widget_offline_dot,
            )
            views.setTextViewText(
                R.id.widget_title,
                "King Gold · ${snapshot.statusLabel}",
            )
            views.setTextViewText(
                R.id.widget_currency_toggle,
                if (snapshot.showUsdPrimary) "$ / ج.م" else "ج.م / $",
            )

            if (snapshot.showUsdPrimary) {
                views.setTextViewText(R.id.widget_price_24, GoldPriceWidgetStore.formatUsdCompact(snapshot.price24KUsd))
                views.setTextViewText(R.id.widget_sub_24, "${GoldPriceWidgetStore.formatEgpCompact(snapshot.price24KEgp)} ج.م")

                views.setTextViewText(R.id.widget_price_21, GoldPriceWidgetStore.formatUsdCompact(snapshot.price21KUsd))
                views.setTextViewText(R.id.widget_sub_21, "${GoldPriceWidgetStore.formatEgpCompact(snapshot.price21KEgp)} ج.م")

                views.setTextViewText(R.id.widget_price_18, GoldPriceWidgetStore.formatUsdCompact(snapshot.price18KUsd))
                views.setTextViewText(R.id.widget_sub_18, "${GoldPriceWidgetStore.formatEgpCompact(snapshot.price18KEgp)} ج.م")

                val poundUsd = snapshot.goldPoundEgp / snapshot.usdToEgpRate.coerceAtLeast(1.0)
                views.setTextViewText(R.id.widget_price_pound, GoldPriceWidgetStore.formatUsdCompact(poundUsd, decimals = 0))
                views.setTextViewText(R.id.widget_sub_pound, "${GoldPriceWidgetStore.formatEgpCompact(snapshot.goldPoundEgp)} ج.م")
            } else {
                views.setTextViewText(R.id.widget_price_24, GoldPriceWidgetStore.formatEgpCompact(snapshot.price24KEgp))
                views.setTextViewText(R.id.widget_sub_24, GoldPriceWidgetStore.formatUsdCompact(snapshot.price24KUsd))

                views.setTextViewText(R.id.widget_price_21, GoldPriceWidgetStore.formatEgpCompact(snapshot.price21KEgp))
                views.setTextViewText(R.id.widget_sub_21, GoldPriceWidgetStore.formatUsdCompact(snapshot.price21KUsd))

                views.setTextViewText(R.id.widget_price_18, GoldPriceWidgetStore.formatEgpCompact(snapshot.price18KEgp))
                views.setTextViewText(R.id.widget_sub_18, GoldPriceWidgetStore.formatUsdCompact(snapshot.price18KUsd))

                views.setTextViewText(R.id.widget_price_pound, GoldPriceWidgetStore.formatEgpCompact(snapshot.goldPoundEgp))
                views.setTextViewText(R.id.widget_sub_pound, GoldPriceWidgetStore.formatChangeTag(snapshot.changePercent))
            }

            val unitLabel = if (snapshot.showUsdPrimary) "$/جم" else "ج.م/جم"
            views.setTextViewText(
                R.id.widget_spot_summary,
                "$unitLabel · الأونصة ${GoldPriceWidgetStore.formatUsdCompact(snapshot.usdPerOunce, decimals = 0)} · ${snapshot.updatedAtLabel}",
            )

            // Tap widget body -> open PriceInsightScreen inside King Gold NavHost
            val openPricesIntent = GoldPriceNotificationHelper.buildLaunchIntent(
                context = context,
                targetRoute = Routes.PRICE_INSIGHT,
            )
            val openPricesPendingIntent = PendingIntent.getActivity(
                context,
                4202,
                openPricesIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPricesPendingIntent)

            // Tap calculator badge -> open GoldCalculatorScreen directly
            val openCalculatorIntent = GoldPriceNotificationHelper.buildLaunchIntent(
                context = context,
                targetRoute = Routes.GOLD_CALCULATOR,
            )
            val openCalculatorPendingIntent = PendingIntent.getActivity(
                context,
                4203,
                openCalculatorIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_calculator_shortcut, openCalculatorPendingIntent)

            // Tap refresh icon -> fetch live global XAU & USD/EGP prices in background
            val refreshIntent = Intent(context, GoldPriceAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_GOLD_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                4204,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            // Tap currency toggle -> switch primary unit between EGP and USD
            val toggleCurrencyIntent = Intent(context, GoldPriceAppWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_GOLD_WIDGET_CURRENCY
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                4205,
                toggleCurrencyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_currency_toggle, togglePendingIntent)

            return views
        }
    }
}
