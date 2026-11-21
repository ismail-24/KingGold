package com.example

import com.example.model.GoldPremiumMode
import com.example.model.GoldPriceCalculator
import com.example.ui.state.KingGoldViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KingGoldFeatureUnitTests {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun goldCalculator_computesPerGramFlatAndPercentagePremiumsAccurately() {
        val perGram = GoldPriceCalculator.calculate(
            weightGrams = 10.0,
            karat = 21,
            marketPricePerGram = 5000.0,
            premiumMode = GoldPremiumMode.PER_GRAM,
            premiumInputValue = 100.0,
        )
        assertEquals(50_000.0, perGram.rawGoldTotal, 0.01)
        assertEquals(1_000.0, perGram.premiumTotal, 0.01)
        assertEquals(51_000.0, perGram.finalTotalPrice, 0.01)
        assertEquals(5100.0, perGram.effectivePricePerGram, 0.01)

        val flat = GoldPriceCalculator.calculate(
            weightGrams = 8.0,
            karat = 24,
            marketPricePerGram = 6000.0,
            premiumMode = GoldPremiumMode.FLAT_TOTAL,
            premiumInputValue = 400.0,
        )
        assertEquals(48_000.0, flat.rawGoldTotal, 0.01)
        assertEquals(400.0, flat.premiumTotal, 0.01)
        assertEquals(48_400.0, flat.finalTotalPrice, 0.01)

        val pct = GoldPriceCalculator.calculate(
            weightGrams = 5.0,
            karat = 18,
            marketPricePerGram = 4000.0,
            premiumMode = GoldPremiumMode.PERCENTAGE,
            premiumInputValue = 5.0,
        )
        assertEquals(20_000.0, pct.rawGoldTotal, 0.01)
        assertEquals(1_000.0, pct.premiumTotal, 0.01)
        assertEquals(21_000.0, pct.finalTotalPrice, 0.01)
    }

    @Test
    fun goldCalculator_clampsNegativeInputsSafely() {
        val result = GoldPriceCalculator.calculate(
            weightGrams = -3.0,
            karat = 21,
            marketPricePerGram = -5000.0,
            premiumMode = GoldPremiumMode.PER_GRAM,
            premiumInputValue = -50.0,
        )
        assertEquals(0.0, result.finalTotalPrice, 0.01)
        assertEquals(0.0, result.effectivePricePerGram, 0.01)
    }

    @Test
    fun goldCalculator_derivesMarketPricesAcrossKaratsAndCalculatesPureValue() {
        val prices = com.example.data.KingGoldRepository.prices
        assertEquals(6200.0, GoldPriceCalculator.marketPriceForKarat(prices, 24), 0.01)
        assertEquals(5425.0, GoldPriceCalculator.marketPriceForKarat(prices, 21), 0.01)
        assertEquals(4650.0, GoldPriceCalculator.marketPriceForKarat(prices, 18), 0.01)
        assertEquals(5683.33, GoldPriceCalculator.marketPriceForKarat(prices, 22), 0.02)
        assertEquals(3616.67, GoldPriceCalculator.marketPriceForKarat(prices, 14), 0.02)

        val marketVal = GoldPriceCalculator.calculate(
            weightGrams = 10.0,
            karat = 22,
            marketPricePerGram = GoldPriceCalculator.marketPriceForKarat(prices, 22),
            premiumMode = GoldPremiumMode.PER_GRAM,
            premiumInputValue = 0.0,
        )
        assertEquals(56_833.3, marketVal.rawGoldTotal, 0.1)
        assertEquals(56_833.3, marketVal.finalTotalPrice, 0.1)
    }

    @Test
    fun viewModel_tracks21KHistoryAnchoredToCurrentPrice() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val hist21 = vm.uiState.value.history21K7Days
        assertEquals(7, hist21.size)
        assertEquals("اليوم", hist21.last().dayLabel)
        assertTrue(hist21.last().pricePerGram > 0.0)
    }
}
