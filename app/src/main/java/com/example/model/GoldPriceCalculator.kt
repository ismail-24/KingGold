package com.example.model

import kotlin.math.round

enum class GoldPremiumMode(
    val label: String,
    val unitHint: String,
) {
    PER_GRAM("لكل جرام", "ج.م / جم"),
    FLAT_TOTAL("مبلغ مقطوع", "ج.م إجمالي"),
    PERCENTAGE("نسبة مئوية %", "%"),
}

data class GoldPriceCalculationResult(
    val weightGrams: Double,
    val karat: Int,
    val marketPricePerGram: Double,
    val premiumMode: GoldPremiumMode,
    val premiumInputValue: Double,
    val rawGoldTotal: Double,
    val premiumTotal: Double,
    val finalTotalPrice: Double,
    val effectivePricePerGram: Double,
)

object GoldPriceCalculator {

    fun calculate(
        weightGrams: Double,
        karat: Int,
        marketPricePerGram: Double,
        premiumMode: GoldPremiumMode,
        premiumInputValue: Double,
    ): GoldPriceCalculationResult {
        val safeWeight = weightGrams.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val safeMarket = marketPricePerGram.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val safePremium = premiumInputValue.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0

        val rawGoldTotal = safeWeight * safeMarket
        val premiumTotal = when (premiumMode) {
            GoldPremiumMode.PER_GRAM -> safeWeight * safePremium
            GoldPremiumMode.FLAT_TOTAL -> safePremium
            GoldPremiumMode.PERCENTAGE -> rawGoldTotal * (safePremium / 100.0)
        }
        val finalTotalPrice = rawGoldTotal + premiumTotal
        val effectivePricePerGram = if (safeWeight > 0.0) finalTotalPrice / safeWeight else 0.0

        return GoldPriceCalculationResult(
            weightGrams = safeWeight,
            karat = karat,
            marketPricePerGram = safeMarket,
            premiumMode = premiumMode,
            premiumInputValue = safePremium,
            rawGoldTotal = roundCurrency(rawGoldTotal),
            premiumTotal = roundCurrency(premiumTotal),
            finalTotalPrice = roundCurrency(finalTotalPrice),
            effectivePricePerGram = roundCurrency(effectivePricePerGram),
        )
    }

    fun marketPriceForKarat(
        prices: List<KaratPrice>,
        karat: Int,
    ): Double {
        val exact = prices.firstOrNull { it.karat == karat && it.pricePerGram > 0.0 }?.pricePerGram
        if (exact != null) return roundCurrency(exact)

        val price24K = prices.firstOrNull { it.karat == 24 && it.pricePerGram > 0.0 }?.pricePerGram
            ?: prices.firstOrNull { it.karat == 21 && it.pricePerGram > 0.0 }?.pricePerGram?.times(24.0 / 21.0)
            ?: 0.0
        val clampedKarat = karat.coerceIn(1, 24)
        return roundCurrency(price24K * (clampedKarat / 24.0))
    }

    private fun roundCurrency(value: Double): Double = round(value * 100.0) / 100.0
}
