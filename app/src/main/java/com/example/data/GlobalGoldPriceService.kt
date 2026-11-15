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
    private const val GOLD_SPOT_URL = "https://api.gold-api.com/price/XAU"
    private const val USD_EGP_FX_URL = "https://open.er-api.com/v6/latest/USD"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetchLiveGlobalGoldPrices(): GlobalGoldFeedResult = withContext(Dispatchers.IO) {
        try {
            val goldBody = executeGet(GOLD_SPOT_URL)
                ?: return@withContext GlobalGoldFeedResult.Error("تعذر الاتصال بسوق الذهب العالمي")
            val goldJson = JSONObject(goldBody)
            val usdPerOz = goldJson.optDouble("price", Double.NaN)
            val updatedAt = goldJson.optString("updatedAt", "")
            if (usdPerOz.isNaN() || usdPerOz <= 0.0) {
                return@withContext GlobalGoldFeedResult.Error("بيانات السعر العالمي غير مكتملة")
            }

            // Fetch live USD -> EGP conversion rate; fall back cleanly if FX endpoint fails
            val fxBody = executeGet(USD_EGP_FX_URL)
            val usdToEgp = if (fxBody != null) {
                val fxJson = JSONObject(fxBody)
                val egp = fxJson.optJSONObject("rates")?.optDouble("EGP", Double.NaN) ?: Double.NaN
                if (!egp.isNaN() && egp > 0.0) egp else 49.5
            } else {
                49.5
            }

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
                    updatedAtIso = updatedAt,
                ),
            )
        } catch (e: Exception) {
            GlobalGoldFeedResult.Error(e.localizedMessage ?: "تعذر تحديث السعر اللحظي")
        }
    }

    private fun executeGet(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }
}
