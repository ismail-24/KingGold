package com.example.model

import java.util.Currency
import java.util.Locale
import kotlin.math.round

data class SupportedCurrency(
    val code: String,
    val symbol: String,
    val arabicName: String,
    val defaultPerUsdRate: Double,
)

data class GoldConversionResult(
    val weightGrams: Double,
    val karat: Int,
    val currency: SupportedCurrency,
    val fxRatePerUsd: Double,
    val gramPriceEgp: Double,
    val gramPriceUsd: Double,
    val gramPriceInCurrency: Double,
    val totalValueInCurrency: Double,
    val totalValueEgp: Double,
    val totalValueUsd: Double,
    val usesLiveRates: Boolean,
)

object GoldCurrencyConverter {

    const val DEFAULT_USD_TO_EGP = 49.5

    val builtInCurrencies: List<SupportedCurrency> = listOf(
        SupportedCurrency("EGP", "ج.م", "جنيه مصري", DEFAULT_USD_TO_EGP),
        SupportedCurrency("SAR", "ر.س", "ريال سعودي", 3.75),
        SupportedCurrency("AED", "د.إ", "درهم إماراتي", 3.6725),
        SupportedCurrency("KWD", "د.ك", "دينار كويتي", 0.307),
        SupportedCurrency("QAR", "ر.ق", "ريال قطري", 3.64),
        SupportedCurrency("USD", "$", "دولار أمريكي", 1.0),
        SupportedCurrency("EUR", "€", "يورو", 0.92),
        SupportedCurrency("GBP", "£", "جنيه إسترليني", 0.79),
    )

    fun detectDeviceLocalCurrency(
        locale: Locale = Locale.getDefault(),
        liveFxRates: Map<String, Double> = emptyMap(),
    ): SupportedCurrency {
        val deviceCurrency = try {
            Currency.getInstance(locale)
        } catch (_: Exception) {
            null
        } ?: return builtInCurrencies.first()

        val code = deviceCurrency.currencyCode.uppercase(Locale.US)
        builtInCurrencies.firstOrNull { it.code == code }?.let { return it }

        val liveRate = liveFxRates[code]?.takeIf { it.isFinite() && it > 0.0 }
        if (liveRate != null) {
            val arabicLocale = Locale.forLanguageTag("ar")
            val displayName = deviceCurrency.getDisplayName(arabicLocale).ifBlank { code }
            val symbol = deviceCurrency.getSymbol(locale).ifBlank { code }
            return SupportedCurrency(
                code = code,
                symbol = symbol,
                arabicName = displayName,
                defaultPerUsdRate = liveRate,
            )
        }

        return builtInCurrencies.first()
    }

    fun availableCurrencies(
        locale: Locale = Locale.getDefault(),
        liveFxRates: Map<String, Double> = emptyMap(),
    ): List<SupportedCurrency> {
        val detected = detectDeviceLocalCurrency(locale, liveFxRates)
        return if (builtInCurrencies.any { it.code == detected.code }) {
            builtInCurrencies
        } else {
            listOf(detected) + builtInCurrencies
        }
    }

    fun convert(
        weightGrams: Double,
        karat: Int,
        prices: List<KaratPrice>,
        currency: SupportedCurrency,
        usdToEgpRate: Double = DEFAULT_USD_TO_EGP,
        fxRatesPerUsd: Map<String, Double> = emptyMap(),
        usesLiveRates: Boolean = false,
    ): GoldConversionResult {
        val safeWeight = weightGrams.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val clampedKarat = karat.coerceIn(1, 24)
        val safeUsdToEgp = usdToEgpRate.takeIf { it.isFinite() && it > 1.0 } ?: DEFAULT_USD_TO_EGP

        val gramPriceEgp = GoldPriceCalculator.marketPriceForKarat(prices, clampedKarat)
        val gramPriceUsd = gramPriceEgp / safeUsdToEgp

        val resolvedRate = when (currency.code) {
            "EGP" -> safeUsdToEgp
            "USD" -> 1.0
            else -> fxRatesPerUsd[currency.code]?.takeIf { it.isFinite() && it > 0.0 }
                ?: currency.defaultPerUsdRate
        }

        val gramPriceInCurrency = gramPriceUsd * resolvedRate
        val totalInCurrency = safeWeight * gramPriceInCurrency
        val totalEgp = safeWeight * gramPriceEgp
        val totalUsd = safeWeight * gramPriceUsd

        return GoldConversionResult(
            weightGrams = safeWeight,
            karat = clampedKarat,
            currency = currency,
            fxRatePerUsd = resolvedRate,
            gramPriceEgp = round2(gramPriceEgp),
            gramPriceUsd = round2(gramPriceUsd),
            gramPriceInCurrency = round2(gramPriceInCurrency),
            totalValueInCurrency = round2(totalInCurrency),
            totalValueEgp = round2(totalEgp),
            totalValueUsd = round2(totalUsd),
            usesLiveRates = usesLiveRates,
        )
    }

    fun isValidDecimalInput(input: String): Boolean {
        if (input.isEmpty()) return true
        var dots = 0
        for (ch in input) {
            when {
                ch.isDigit() -> Unit
                ch == '.' || ch == ',' || ch == '٫' -> {
                    dots++
                    if (dots > 1) return false
                }
                else -> return false
            }
        }
        return true
    }

    fun normalizeNumericInput(input: String): String = buildString {
        for (ch in input) {
            when (ch) {
                ',', '٫' -> append('.')
                in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
                else -> append(ch)
            }
        }
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
