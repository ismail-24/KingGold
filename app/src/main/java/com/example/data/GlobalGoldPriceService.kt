package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GlobalGoldQuote(
    val usdPerTroyOunce: Double,
    val usdToEgpRate: Double,
    val price24KPerGramEgp: Double,
    val price21KPerGramEgp: Double,
    val price18KPerGramEgp: Double,
    val price24KPerGramUsd: Double,
    val price21KPerGramUsd: Double,
    val price18KPerGramUsd: Double,
    val updatedAtIso: String,
)

sealed interface GlobalGoldFeedResult {
    data class Success(val quote: GlobalGoldQuote) : GlobalGoldFeedResult
    data class Error(val message: String) : GlobalGoldFeedResult
}

object GlobalGoldPriceService {

    private const val TROY_OUNCE_IN_GRAMS = 31.1034768
    private const val GOLD_SPOT_PRIMARY_URL = "https://api.gold-api.com/price/XAU"
    private const val GOLD_XAU_CDN_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/xau.json"
    private const val GOLD_XAU_PAGES_URL = "https://latest.currency-api.pages.dev/v1/currencies/xau.json"
    private const val USD_EGP_PRIMARY_URL = "https://open.er-api.com/v6/latest/USD"
    private const val USD_EGP_CDN_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchLiveGlobalGoldPrices(): GlobalGoldFeedResult = withContext(Dispatchers.IO) {
        try {
            val spotResult = fetchSpotXauUsdAndOptionalEgp()
                ?: return@withContext GlobalGoldFeedResult.Error("تعذر الاتصال بسوق الذهب العالمي")

            val usdPerOz = spotResult.usdPerOz
            val usdToEgp = fetchUsdToEgpRate()
                ?: spotResult.egpPerOz?.let { it / usdPerOz }
                ?: return@withContext GlobalGoldFeedResult.Error("تعذر جلب سعر صرف الدولار مقابل الجنيه")

            val usd24 = usdPerOz / TROY_OUNCE_IN_GRAMS
            val usd21 = usd24 * (21.0 / 24.0)
            val usd18 = usd24 * (18.0 / 24.0)

            val egp24 = usd24 * usdToEgp
            val egp21 = usd21 * usdToEgp
            val egp18 = usd18 * usdToEgp

            GlobalGoldFeedResult.Success(
                GlobalGoldQuote(
                    usdPerTroyOunce = usdPerOz,
                    usdToEgpRate = usdToEgp,
                    price24KPerGramEgp = egp24,
                    price21KPerGramEgp = egp21,
                    price18KPerGramEgp = egp18,
                    price24KPerGramUsd = usd24,
                    price21KPerGramUsd = usd21,
                    price18KPerGramUsd = usd18,
                    updatedAtIso = spotResult.updatedAtIso,
                ),
            )
        } catch (e: Exception) {
            GlobalGoldFeedResult.Error(e.localizedMessage ?: "تعذر تحديث السعر اللحظي")
        }
    }

    private data class SpotFetch(
        val usdPerOz: Double,
        val egpPerOz: Double?,
        val updatedAtIso: String,
    )

    private fun fetchSpotXauUsdAndOptionalEgp(): SpotFetch? {
        executeGet(GOLD_SPOT_PRIMARY_URL)?.let { body ->
            val json = JSONObject(body)
            val price = json.optDouble("price", Double.NaN)
            if (!price.isNaN() && price > 500.0) {
                return SpotFetch(
                    usdPerOz = price,
                    egpPerOz = null,
                    updatedAtIso = json.optString("updatedAt", ""),
                )
            }
        }

        for (url in listOf(GOLD_XAU_CDN_URL, GOLD_XAU_PAGES_URL)) {
            executeGet(url)?.let { body ->
                val json = JSONObject(body)
                val xauObj = json.optJSONObject("xau")
                val usd = xauObj?.optDouble("usd", Double.NaN) ?: Double.NaN
                val egp = xauObj?.optDouble("egp", Double.NaN) ?: Double.NaN
                if (!usd.isNaN() && usd > 500.0) {
                    return SpotFetch(
                        usdPerOz = usd,
                        egpPerOz = if (!egp.isNaN() && egp > 0.0) egp else null,
                        updatedAtIso = json.optString("date", ""),
                    )
                }
            }
        }
        return null
    }

    private fun fetchUsdToEgpRate(): Double? {
        executeGet(USD_EGP_PRIMARY_URL)?.let { body ->
            val fxJson = JSONObject(body)
            val egp = fxJson.optJSONObject("rates")?.optDouble("EGP", Double.NaN) ?: Double.NaN
            if (!egp.isNaN() && egp > 1.0) return egp
        }

        executeGet(USD_EGP_CDN_URL)?.let { body ->
            val fxJson = JSONObject(body)
            val egp = fxJson.optJSONObject("usd")?.optDouble("egp", Double.NaN) ?: Double.NaN
            if (!egp.isNaN() && egp > 1.0) return egp
        }
        return null
    }

    private fun executeGet(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "KingGold-Android/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }
}
