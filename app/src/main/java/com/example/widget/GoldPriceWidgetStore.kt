package com.example.widget

import android.content.Context
import com.example.data.GlobalGoldQuote
import com.example.data.KingGoldRepository
import com.example.model.KaratPrice
import com.example.ui.components.GoldFeedStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class GoldPriceWidgetSnapshot(
    val price24KEgp: Double,
    val price21KEgp: Double,
    val price18KEgp: Double,
    val goldPoundEgp: Double,
    val price24KUsd: Double,
    val price21KUsd: Double,
    val price18KUsd: Double,
    val changePercent: Double,
    val usdPerOunce: Double,
    val usdToEgpRate: Double,
    val isLive: Boolean,
    val statusLabel: String,
    val updatedAtLabel: String,
    val updatedAtEpochMs: Long,
    val showUsdPrimary: Boolean = false,
)

object GoldPriceWidgetStore {

    private const val PREFS_NAME = "king_gold_price_widget_prefs"
    private const val KEY_24K_EGP = "p24_egp"
    private const val KEY_21K_EGP = "p21_egp"
    private const val KEY_18K_EGP = "p18_egp"
    private const val KEY_POUND_EGP = "pound_egp"
    private const val KEY_24K_USD = "p24_usd"
    private const val KEY_21K_USD = "p21_usd"
    private const val KEY_18K_USD = "p18_usd"
    private const val KEY_CHANGE_PCT = "change_pct"
    private const val KEY_USD_OZ = "usd_oz"
    private const val KEY_USD_EGP = "usd_egp"
    private const val KEY_IS_LIVE = "is_live"
    private const val KEY_STATUS_LABEL = "status_label"
    private const val KEY_UPDATED_LABEL = "updated_label"
    private const val KEY_UPDATED_EPOCH = "updated_epoch"
    private const val KEY_SHOW_USD_PRIMARY = "show_usd_primary"
    private const val DEFAULT_USD_TO_EGP = 49.5
    private const val TROY_OUNCE_IN_GRAMS = 31.1034768

    fun defaultSnapshot(): GoldPriceWidgetSnapshot {
        val defaults = KingGoldRepository.prices
        val p24 = defaults.firstOrNull { it.karat == 24 }?.pricePerGram ?: 6200.0
        val p21 = defaults.firstOrNull { it.karat == 21 }?.pricePerGram ?: 5425.0
        val p18 = defaults.firstOrNull { it.karat == 18 }?.pricePerGram ?: 4650.0
        val pound = defaults.firstOrNull { it.karat == 0 }?.pricePerGram ?: (p21 * 8.0)
        val pct = defaults.firstOrNull { it.karat == 21 }?.changePercent ?: 0.0
        val p24Usd = p24 / DEFAULT_USD_TO_EGP
        val p21Usd = p21 / DEFAULT_USD_TO_EGP
        val p18Usd = p18 / DEFAULT_USD_TO_EGP
        return GoldPriceWidgetSnapshot(
            price24KEgp = p24,
            price21KEgp = p21,
            price18KEgp = p18,
            goldPoundEgp = pound,
            price24KUsd = p24Usd,
            price21KUsd = p21Usd,
            price18KUsd = p18Usd,
            changePercent = pct,
            usdPerOunce = p24Usd * TROY_OUNCE_IN_GRAMS,
            usdToEgpRate = DEFAULT_USD_TO_EGP,
            isLive = false,
            statusLabel = "مرجعي",
            updatedAtLabel = currentFormattedTime(),
            updatedAtEpochMs = System.currentTimeMillis(),
            showUsdPrimary = false,
        )
    }

    fun loadSnapshot(context: Context): GoldPriceWidgetSnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_24K_EGP)) {
            return defaultSnapshot()
        }
        val fallback = defaultSnapshot()
        return GoldPriceWidgetSnapshot(
            price24KEgp = prefs.getDouble(KEY_24K_EGP, fallback.price24KEgp),
            price21KEgp = prefs.getDouble(KEY_21K_EGP, fallback.price21KEgp),
            price18KEgp = prefs.getDouble(KEY_18K_EGP, fallback.price18KEgp),
            goldPoundEgp = prefs.getDouble(KEY_POUND_EGP, fallback.goldPoundEgp),
            price24KUsd = prefs.getDouble(KEY_24K_USD, fallback.price24KUsd),
            price21KUsd = prefs.getDouble(KEY_21K_USD, fallback.price21KUsd),
            price18KUsd = prefs.getDouble(KEY_18K_USD, fallback.price18KUsd),
            changePercent = prefs.getDouble(KEY_CHANGE_PCT, fallback.changePercent),
            usdPerOunce = prefs.getDouble(KEY_USD_OZ, fallback.usdPerOunce),
            usdToEgpRate = prefs.getDouble(KEY_USD_EGP, fallback.usdToEgpRate),
            isLive = prefs.getBoolean(KEY_IS_LIVE, false),
            statusLabel = prefs.getString(KEY_STATUS_LABEL, null)?.takeIf { it.isNotBlank() } ?: fallback.statusLabel,
            updatedAtLabel = prefs.getString(KEY_UPDATED_LABEL, null)?.takeIf { it.isNotBlank() } ?: fallback.updatedAtLabel,
            updatedAtEpochMs = prefs.getLong(KEY_UPDATED_EPOCH, fallback.updatedAtEpochMs),
            showUsdPrimary = prefs.getBoolean(KEY_SHOW_USD_PRIMARY, false),
        )
    }

    fun saveSnapshot(context: Context, snapshot: GoldPriceWidgetSnapshot): GoldPriceWidgetSnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putDouble(KEY_24K_EGP, snapshot.price24KEgp)
            .putDouble(KEY_21K_EGP, snapshot.price21KEgp)
            .putDouble(KEY_18K_EGP, snapshot.price18KEgp)
            .putDouble(KEY_POUND_EGP, snapshot.goldPoundEgp)
            .putDouble(KEY_24K_USD, snapshot.price24KUsd)
            .putDouble(KEY_21K_USD, snapshot.price21KUsd)
            .putDouble(KEY_18K_USD, snapshot.price18KUsd)
            .putDouble(KEY_CHANGE_PCT, snapshot.changePercent)
            .putDouble(KEY_USD_OZ, snapshot.usdPerOunce)
            .putDouble(KEY_USD_EGP, snapshot.usdToEgpRate)
            .putBoolean(KEY_IS_LIVE, snapshot.isLive)
            .putString(KEY_STATUS_LABEL, snapshot.statusLabel)
            .putString(KEY_UPDATED_LABEL, snapshot.updatedAtLabel)
            .putLong(KEY_UPDATED_EPOCH, snapshot.updatedAtEpochMs)
            .putBoolean(KEY_SHOW_USD_PRIMARY, snapshot.showUsdPrimary)
            .apply()
        return snapshot
    }

    fun saveFromQuote(
        context: Context,
        quote: GlobalGoldQuote,
    ): GoldPriceWidgetSnapshot {
        val existing = loadSnapshot(context)
        val pct = if (existing.isLive && existing.price24KEgp > 0.0) {
            ((quote.price24KPerGramEgp - existing.price24KEgp) / existing.price24KEgp) * 100.0
        } else {
            0.0
        }
        val now = System.currentTimeMillis()
        return saveSnapshot(
            context = context,
            snapshot = GoldPriceWidgetSnapshot(
                price24KEgp = quote.price24KPerGramEgp,
                price21KEgp = quote.price21KPerGramEgp,
                price18KEgp = quote.price18KPerGramEgp,
                goldPoundEgp = quote.price21KPerGramEgp * 8.0,
                price24KUsd = quote.price24KPerGramUsd,
                price21KUsd = quote.price21KPerGramUsd,
                price18KUsd = quote.price18KPerGramUsd,
                changePercent = pct,
                usdPerOunce = quote.usdPerTroyOunce,
                usdToEgpRate = quote.usdToEgpRate,
                isLive = true,
                statusLabel = "مباشر",
                updatedAtLabel = currentFormattedTime(now),
                updatedAtEpochMs = now,
                showUsdPrimary = existing.showUsdPrimary,
            ),
        )
    }

    fun saveFromKaratPrices(
        context: Context,
        prices: List<KaratPrice>,
        feedStatus: GoldFeedStatus,
    ): GoldPriceWidgetSnapshot {
        val existing = loadSnapshot(context)
        val p24 = prices.firstOrNull { it.karat == 24 }?.pricePerGram?.takeIf { it > 0.0 } ?: existing.price24KEgp
        val p21 = prices.firstOrNull { it.karat == 21 }?.pricePerGram?.takeIf { it > 0.0 } ?: (p24 * 21.0 / 24.0)
        val p18 = prices.firstOrNull { it.karat == 18 }?.pricePerGram?.takeIf { it > 0.0 } ?: (p24 * 18.0 / 24.0)
        val pound = prices.firstOrNull { it.karat == 0 }?.pricePerGram?.takeIf { it > 0.0 } ?: (p21 * 8.0)
        val pct = prices.firstOrNull { it.karat == 21 }?.changePercent ?: existing.changePercent

        val usdToEgp = (feedStatus as? GoldFeedStatus.Live)?.usdToEgpRate ?: existing.usdToEgpRate.takeIf { it > 1.0 } ?: DEFAULT_USD_TO_EGP
        val usdPerOz = (feedStatus as? GoldFeedStatus.Live)?.usdPerOunce ?: ((p24 / usdToEgp) * TROY_OUNCE_IN_GRAMS)
        val p24Usd = p24 / usdToEgp
        val p21Usd = p21 / usdToEgp
        val p18Usd = p18 / usdToEgp
        val now = System.currentTimeMillis()
        val timeLabel = (feedStatus as? GoldFeedStatus.Live)?.fetchedAtLabel?.takeIf { it.isNotBlank() }
            ?: currentFormattedTime(now)

        return saveSnapshot(
            context = context,
            snapshot = GoldPriceWidgetSnapshot(
                price24KEgp = p24,
                price21KEgp = p21,
                price18KEgp = p18,
                goldPoundEgp = pound,
                price24KUsd = p24Usd,
                price21KUsd = p21Usd,
                price18KUsd = p18Usd,
                changePercent = pct,
                usdPerOunce = usdPerOz,
                usdToEgpRate = usdToEgp,
                isLive = feedStatus is GoldFeedStatus.Live,
                statusLabel = when (feedStatus) {
                    is GoldFeedStatus.Live -> "مباشر"
                    is GoldFeedStatus.Loading -> "جاري التحديث"
                    is GoldFeedStatus.OfflineFallback -> "مرجعي"
                },
                updatedAtLabel = timeLabel,
                updatedAtEpochMs = now,
                showUsdPrimary = existing.showUsdPrimary,
            ),
        )
    }

    fun saveFromRawPrices(
        context: Context,
        price24K: Double?,
        price21K: Double?,
        price18K: Double?,
        changePercent: Double? = null,
    ): GoldPriceWidgetSnapshot? {
        val existing = loadSnapshot(context)
        val p24 = price24K?.takeIf { it > 0.0 }
            ?: price21K?.takeIf { it > 0.0 }?.let { it * 24.0 / 21.0 }
            ?: price18K?.takeIf { it > 0.0 }?.let { it * 24.0 / 18.0 }
            ?: return null
        val p21 = price21K?.takeIf { it > 0.0 } ?: (p24 * 21.0 / 24.0)
        val p18 = price18K?.takeIf { it > 0.0 } ?: (p24 * 18.0 / 24.0)
        val fx = existing.usdToEgpRate.takeIf { it > 1.0 } ?: DEFAULT_USD_TO_EGP
        val now = System.currentTimeMillis()

        return saveSnapshot(
            context = context,
            snapshot = existing.copy(
                price24KEgp = p24,
                price21KEgp = p21,
                price18KEgp = p18,
                goldPoundEgp = p21 * 8.0,
                price24KUsd = p24 / fx,
                price21KUsd = p21 / fx,
                price18KUsd = p18 / fx,
                changePercent = changePercent ?: existing.changePercent,
                usdPerOunce = (p24 / fx) * TROY_OUNCE_IN_GRAMS,
                isLive = true,
                statusLabel = "تحديث فوري",
                updatedAtLabel = currentFormattedTime(now),
                updatedAtEpochMs = now,
            ),
        )
    }

    fun toggleCurrencyMode(context: Context): GoldPriceWidgetSnapshot {
        val current = loadSnapshot(context)
        return saveSnapshot(context, current.copy(showUsdPrimary = !current.showUsdPrimary))
    }

    fun formatEgpCompact(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        return nf.format(amount)
    }

    fun formatUsdCompact(amount: Double, decimals: Int = 1): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }
        return "$${nf.format(amount)}"
    }

    fun formatChangeTag(changePercent: Double): String {
        val rounded = String.format(Locale.US, "%.2f", abs(changePercent))
        return when {
            changePercent > 0.005 -> "+$rounded% ▲"
            changePercent < -0.005 -> "-$rounded% ▼"
            else -> "8جم 21"
        }
    }

    private fun currentFormattedTime(epochMs: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("h:mm a", Locale.forLanguageTag("ar")).format(Date(epochMs))
    }

    private fun android.content.SharedPreferences.Editor.putDouble(key: String, value: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(value))

    private fun android.content.SharedPreferences.getDouble(key: String, defaultValue: Double): Double {
        if (!contains(key)) return defaultValue
        return java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(defaultValue)))
    }
}
