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

    @Test
    fun goldCurrencyConverter_detectsDeviceLocalCurrencyAndSupportsCustomLiveRates() {
        val egp = com.example.model.GoldCurrencyConverter.detectDeviceLocalCurrency(java.util.Locale.forLanguageTag("ar-EG"))
        assertEquals("EGP", egp.code)

        val sar = com.example.model.GoldCurrencyConverter.detectDeviceLocalCurrency(java.util.Locale.forLanguageTag("ar-SA"))
        assertEquals("SAR", sar.code)

        val aed = com.example.model.GoldCurrencyConverter.detectDeviceLocalCurrency(java.util.Locale.forLanguageTag("ar-AE"))
        assertEquals("AED", aed.code)

        val usd = com.example.model.GoldCurrencyConverter.detectDeviceLocalCurrency(java.util.Locale.US)
        assertEquals("USD", usd.code)

        // Non-built-in currency (e.g. JPY) is detected dynamically when present in live FX map
        val jpy = com.example.model.GoldCurrencyConverter.detectDeviceLocalCurrency(
            locale = java.util.Locale.JAPAN,
            liveFxRates = mapOf("JPY" to 152.0),
        )
        assertEquals("JPY", jpy.code)
        assertEquals(152.0, jpy.defaultPerUsdRate, 0.01)
    }

    @Test
    fun goldCurrencyConverter_convertsGramsToLocalCurrenciesUsingLiveMarketRates() {
        val prices = listOf(
            com.example.model.KaratPrice(24, "عيار 24", 6000.0, 0.0),
            com.example.model.KaratPrice(21, "عيار 21", 5250.0, 0.0),
            com.example.model.KaratPrice(18, "عيار 18", 4500.0, 0.0),
        )
        val fxRates = mapOf(
            "USD" to 1.0,
            "EGP" to 50.0,
            "SAR" to 3.75,
            "AED" to 3.67,
            "EUR" to 0.90,
        )

        // 10g of 24K -> 6000 EGP/g = $120/g -> in SAR (3.75): 450 SAR/g -> 4,500.00 SAR
        val sarCurrency = com.example.model.GoldCurrencyConverter.builtInCurrencies.first { it.code == "SAR" }
        val sarResult = com.example.model.GoldCurrencyConverter.convert(
            weightGrams = 10.0,
            karat = 24,
            prices = prices,
            currency = sarCurrency,
            usdToEgpRate = 50.0,
            fxRatesPerUsd = fxRates,
            usesLiveRates = true,
        )
        assertEquals(120.0, sarResult.gramPriceUsd, 0.01)
        assertEquals(450.0, sarResult.gramPriceInCurrency, 0.01)
        assertEquals(4500.0, sarResult.totalValueInCurrency, 0.01)
        assertEquals(60000.0, sarResult.totalValueEgp, 0.01)
        assertEquals(1200.0, sarResult.totalValueUsd, 0.01)

        // Negative weights safely clamp to zero
        val zeroResult = com.example.model.GoldCurrencyConverter.convert(
            weightGrams = -5.0,
            karat = 21,
            prices = prices,
            currency = sarCurrency,
            usdToEgpRate = 50.0,
            fxRatesPerUsd = fxRates,
            usesLiveRates = true,
        )
        assertEquals(0.0, zeroResult.totalValueInCurrency, 0.01)
    }

    @Test
    fun viewModel_fetchesMarketNewsByTopicAndSurfacesErrorsCleanly() {
        var shouldFail = false
        val requestedTopics = mutableListOf<com.example.model.MarketNewsTopic>()
        val gateway = com.example.data.MarketNewsGateway { topic ->
            requestedTopics.add(topic)
            if (shouldFail) {
                throw IllegalStateException("Google Search attestation error")
            }
            com.example.model.MarketNewsFeed(
                topic = topic,
                macroSummary = "ملخص ${topic.label}",
                articles = listOf(
                    com.example.model.MarketNewsArticle(
                        id = "a1",
                        title = "تحديث ${topic.label}",
                        summary = "بيانات سوق الذهب العالمية المحدثة.",
                        impact = com.example.model.NewsImpactDirection.BULLISH,
                        regionTag = topic.label,
                        publishedHint = "الآن",
                        sourceTitle = "Kitco",
                        sourceUrl = "https://www.kitco.com/news",
                    ),
                ),
                groundingSources = listOf(
                    com.example.model.MarketNewsSource("Kitco", "https://www.kitco.com/news", "kitco.com"),
                ),
                searchQueries = listOf("XAU USD gold news"),
                fetchedAtLabel = "12:00 م",
            )
        }

        val vm = KingGoldViewModel(
            marketNewsGateway = gateway,
            autoFetchPrices = false,
        )
        assertTrue(vm.uiState.value.marketNewsState is com.example.model.MarketNewsUiState.Idle)

        vm.selectMarketNewsTopic(com.example.model.MarketNewsTopic.CENTRAL_BANKS)
        testDispatcher.scheduler.advanceUntilIdle()
        val successState = vm.uiState.value.marketNewsState as com.example.model.MarketNewsUiState.Success
        assertEquals(com.example.model.MarketNewsTopic.CENTRAL_BANKS, successState.feed.topic)
        assertEquals(listOf(com.example.model.MarketNewsTopic.CENTRAL_BANKS), requestedTopics)

        shouldFail = true
        vm.refreshMarketNews(com.example.model.MarketNewsTopic.GLOBAL_SPOT)
        testDispatcher.scheduler.advanceUntilIdle()
        val errorState = vm.uiState.value.marketNewsState as com.example.model.MarketNewsUiState.Error
        assertTrue(errorState.message.contains("Google Search attestation error"))
    }
}
