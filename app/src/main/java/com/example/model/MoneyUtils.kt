package com.example.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {
    const val MIN_PHYSICAL_WITHDRAWAL_MG: Long = 1000L
    const val MG_PER_GRAM: Long = 1000L
    const val PIASTERS_PER_EGP: Long = 100L

    fun egpToPiasters(egp: Double): Long {
        if (egp.isNaN() || egp.isInfinite() || egp <= 0.0) return 0L
        return BigDecimal.valueOf(egp)
            .multiply(BigDecimal(PIASTERS_PER_EGP))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
            .coerceAtLeast(0L)
    }

    fun egpStringToPiasters(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val bd = cleaned.toBigDecimalOrNull() ?: return null
        if (bd < BigDecimal.ZERO) return null
        return bd.multiply(BigDecimal(PIASTERS_PER_EGP))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    fun piastersToEgp(piasters: Long): Double =
        BigDecimal(piasters)
            .divide(BigDecimal(PIASTERS_PER_EGP), 2, RoundingMode.HALF_UP)
            .toDouble()

    fun piastersToEgpString(piasters: Long, showDecimals: Boolean = true): String {
        val egp = BigDecimal(piasters).divide(BigDecimal(PIASTERS_PER_EGP), 2, RoundingMode.HALF_UP)
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (showDecimals) 2 else 0
            maximumFractionDigits = if (showDecimals) 2 else 0
        }
        return formatter.format(egp)
    }

    fun gramsStringToMg(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val bd = cleaned.toBigDecimalOrNull() ?: return null
        if (bd <= BigDecimal.ZERO) return null
        return bd.multiply(BigDecimal(MG_PER_GRAM))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    fun mgStringToMg(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        val v = cleaned.toLongOrNull() ?: return null
        return v.takeIf { it > 0L }
    }

    fun mgToGramsDouble(mg: Long): Double =
        BigDecimal(mg).divide(BigDecimal(MG_PER_GRAM), 3, RoundingMode.HALF_UP).toDouble()

    fun mgToGramsString(mg: Long): String {
        val grams = BigDecimal(mg).divide(BigDecimal(MG_PER_GRAM), 3, RoundingMode.HALF_UP)
        return "${grams.toPlainString()} جم ($mg ملجم)"
    }

    fun computeFractionalCostPiasters(weightMg: Long, pricePerGramPiasters: Long): Long {
        if (weightMg <= 0L || pricePerGramPiasters <= 0L) return 0L
        return BigDecimal(weightMg)
            .multiply(BigDecimal(pricePerGramPiasters))
            .divide(BigDecimal(MG_PER_GRAM), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    fun computeGoldValuationPiasters(
        gold24kMg: Long,
        gold21kMg: Long,
        gold18kMg: Long,
        valuation24kPerGramPiasters: Long,
        valuation21kPerGramPiasters: Long,
        valuation18kPerGramPiasters: Long,
    ): Long {
        val v24 = computeFractionalCostPiasters(gold24kMg, valuation24kPerGramPiasters)
        val v21 = computeFractionalCostPiasters(gold21kMg, valuation21kPerGramPiasters)
        val v18 = computeFractionalCostPiasters(gold18kMg, valuation18kPerGramPiasters)
        return v24 + v21 + v18
    }

    fun computeAverageCostPerGramPiasters(totalInvestedPiasters: Long, goldConfirmedMg: Long): Long {
        if (totalInvestedPiasters <= 0L || goldConfirmedMg <= 0L) return 0L
        return BigDecimal(totalInvestedPiasters)
            .multiply(BigDecimal(MG_PER_GRAM))
            .divide(BigDecimal(goldConfirmedMg), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    fun computeProfitLossPercent(currentValuationPiasters: Long, totalInvestedPiasters: Long): Double {
        if (totalInvestedPiasters <= 0L) return 0.0
        val diff = BigDecimal(currentValuationPiasters - totalInvestedPiasters)
        return diff
            .multiply(BigDecimal(100))
            .divide(BigDecimal(totalInvestedPiasters), 2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
