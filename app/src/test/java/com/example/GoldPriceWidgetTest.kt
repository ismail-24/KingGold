package com.example

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.GlobalGoldFeedResult
import com.example.data.GlobalGoldPriceService
import com.example.data.GlobalGoldQuote
import com.example.model.KaratPrice
import com.example.ui.components.GoldFeedStatus
import com.example.ui.components.LivePriceTicker
import com.example.ui.screens.price.PriceInsightScreen
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
import com.example.widget.GoldPriceAppWidgetProvider
import com.example.widget.GoldPriceWidgetStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoldPriceWidgetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("king_gold_price_widget_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        GoldPriceAppWidgetProvider.livePriceFetcher = {
            GlobalGoldFeedResult.Success(
                GlobalGoldQuote(
                    usdPerTroyOunce = 3110.35,
                    usdToEgpRate = 50.0,
                    price24KPerGramEgp = 6400.0,
                    price21KPerGramEgp = 5600.0,
                    price18KPerGramEgp = 4800.0,
                    price24KPerGramUsd = 128.0,
                    price21KPerGramUsd = 112.0,
                    price18KPerGramUsd = 96.0,
                    updatedAtIso = "2026-11-23T10:30:00Z",
                ),
            )
        }
    }

    @After
    fun tearDown() {
        GoldPriceAppWidgetProvider.livePriceFetcher = {
            GlobalGoldPriceService.fetchLiveGlobalGoldPrices()
        }
    }

    @Test
    fun widgetStore_persistsLiveQuoteAndTogglesCurrency() {
        val quote = GlobalGoldQuote(
            usdPerTroyOunce = 2950.0,
            usdToEgpRate = 50.0,
            price24KPerGramEgp = 4742.24,
            price21KPerGramEgp = 4149.46,
            price18KPerGramEgp = 3556.68,
            price24KPerGramUsd = 94.84,
            price21KPerGramUsd = 82.99,
            price18KPerGramUsd = 71.13,
            updatedAtIso = "2026-11-23T10:00:00Z",
        )

        val saved = GoldPriceWidgetStore.saveFromQuote(app, quote)
        assertTrue(saved.isLive)
        assertEquals("مباشر", saved.statusLabel)
        assertFalse(saved.showUsdPrimary)

        val reloaded = GoldPriceWidgetStore.loadSnapshot(app)
        assertEquals(4742.24, reloaded.price24KEgp, 0.01)
        assertEquals(4149.46, reloaded.price21KEgp, 0.01)
        assertEquals(3556.68, reloaded.price18KEgp, 0.01)
        assertEquals(4149.46 * 8.0, reloaded.goldPoundEgp, 0.05)

        val toggled = GoldPriceWidgetStore.toggleCurrencyMode(app)
        assertTrue(toggled.showUsdPrimary)
        assertTrue(GoldPriceWidgetStore.loadSnapshot(app).showUsdPrimary)
    }

    @Test
    fun widgetProvider_rendersAllKaratRatesAndUpdatesPinnedWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(app)
        val shadowWidgetManager = Shadows.shadowOf(appWidgetManager)
        val widgetId = shadowWidgetManager.createWidget(
            GoldPriceAppWidgetProvider::class.java,
            R.layout.widget_gold_price_tracker,
        )

        val prices = listOf(
            KaratPrice(24, "عيار 24", 6400.0, 0.8),
            KaratPrice(21, "عيار 21", 5600.0, 0.8),
            KaratPrice(18, "عيار 18", 4800.0, 0.8),
            KaratPrice(0, "الجنيه", 44800.0, 0.8),
        )
        val liveStatus = GoldFeedStatus.Live(
            usdPerOunce = 3110.35,
            usdToEgpRate = 50.0,
            fetchedAtLabel = "10:30 ص",
        )

        GoldPriceAppWidgetProvider.syncFromPrices(app, prices, liveStatus)

        val updatedView = shadowWidgetManager.getViewFor(widgetId)
        assertNotNull(updatedView)
        assertEquals("6,400", updatedView.findViewById<TextView>(R.id.widget_price_24).text.toString())
        assertEquals("5,600", updatedView.findViewById<TextView>(R.id.widget_price_21).text.toString())
        assertEquals("4,800", updatedView.findViewById<TextView>(R.id.widget_price_18).text.toString())
        assertEquals("44,800", updatedView.findViewById<TextView>(R.id.widget_price_pound).text.toString())
        assertTrue(updatedView.findViewById<TextView>(R.id.widget_title).text.toString().contains("مباشر"))
        assertTrue(updatedView.findViewById<TextView>(R.id.widget_spot_summary).text.toString().contains("3,110"))

        // Switch primary display to USD via widget broadcast action
        val provider = GoldPriceAppWidgetProvider()
        provider.onReceive(
            app,
            Intent(app, GoldPriceAppWidgetProvider::class.java).apply {
                action = GoldPriceAppWidgetProvider.ACTION_TOGGLE_GOLD_WIDGET_CURRENCY
            },
        )

        val usdSnapshot = GoldPriceWidgetStore.loadSnapshot(app)
        assertTrue(usdSnapshot.showUsdPrimary)
        val usdRemoteViews = GoldPriceAppWidgetProvider.buildRemoteViews(app, usdSnapshot)
        val inflated = usdRemoteViews.apply(app, FrameLayout(app))
        assertEquals("$128.0", inflated.findViewById<TextView>(R.id.widget_price_24).text.toString())
        assertEquals("$112.0", inflated.findViewById<TextView>(R.id.widget_price_21).text.toString())
        assertEquals("$96.0", inflated.findViewById<TextView>(R.id.widget_price_18).text.toString())
        assertEquals("$ / ج.م", inflated.findViewById<TextView>(R.id.widget_currency_toggle).text.toString())

        // Manifest declares receiver for home screen widget
        val receivers = app.packageManager.queryBroadcastReceivers(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
            android.content.pm.PackageManager.GET_META_DATA,
        )
        assertTrue(
            receivers.any {
                it.activityInfo.name == GoldPriceAppWidgetProvider::class.java.name
            },
        )
    }

    @Test
    fun livePriceTicker_switchesCurrencyAndRefreshesPrices() {
        var refreshCount = 0
        val prices = listOf(
            KaratPrice(24, "عيار 24", 6250.0, 0.5),
            KaratPrice(21, "عيار 21", 5500.0, 0.5),
            KaratPrice(18, "عيار 18", 4700.0, 0.5),
            KaratPrice(0, "الجنيه", 44000.0, 0.5),
        )
        val status = GoldFeedStatus.Live(
            usdPerOunce = 3100.0,
            usdToEgpRate = 50.0,
            fetchedAtLabel = "11:15 ص",
        )

        composeRule.setContent {
            KingGoldTheme {
                LivePriceTicker(
                    prices = prices,
                    feedStatus = status,
                    onRefresh = { refreshCount++ },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("live_price_ticker").assertIsDisplayed()
        composeRule.onNodeWithTag("ticker_currency_toggle").assertTextContains("EGP ج.م")
        composeRule.onNodeWithTag("ticker_karat_tile_21").assertTextContains("5,500")

        composeRule.onNodeWithTag("ticker_currency_toggle").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("ticker_currency_toggle").assertTextContains("USD $")
        composeRule.onNodeWithTag("ticker_karat_tile_21").assertTextContains("$110.0")

        composeRule.onNodeWithTag("ticker_refresh_button").performClick()
        assertEquals(1, refreshCount)
    }

    @Test
    fun priceInsightScreen_syncsAndRequestsPinForHomeScreenWidget() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        var refreshCalled = false

        composeRule.setContent {
            KingGoldTheme {
                PriceInsightScreen(
                    uiState = vm.uiState.value,
                    onSaveAlert = { _, _, _, _ -> },
                    onRefreshPrices = { refreshCalled = true },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_widget_promo_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("sync_gold_widget_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(refreshCalled)
        val synced = GoldPriceWidgetStore.loadSnapshot(app)
        assertEquals(vm.uiState.value.gramPrice(24), synced.price24KEgp, 0.01)
        assertEquals(vm.uiState.value.gramPrice(21), synced.price21KEgp, 0.01)
        assertEquals(vm.uiState.value.gramPrice(18), synced.price18KEgp, 0.01)

        // Pin widget button works gracefully across launchers
        composeRule.onNodeWithTag("pin_gold_widget_button").performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun widgetProvider_fetchAndUpdateWidgets_persistsLiveQuoteAndUpdatesViews() = runTest {
        val appWidgetManager = AppWidgetManager.getInstance(app)
        val shadowWidgetManager = Shadows.shadowOf(appWidgetManager)
        val widgetId = shadowWidgetManager.createWidget(
            GoldPriceAppWidgetProvider::class.java,
            R.layout.widget_gold_price_tracker,
        )

        val snapshot = GoldPriceAppWidgetProvider.fetchAndUpdateWidgets(app)
        assertTrue(snapshot.isLive)
        assertEquals(6400.0, snapshot.price24KEgp, 0.01)
        assertEquals(5600.0, snapshot.price21KEgp, 0.01)
        assertEquals(4800.0, snapshot.price18KEgp, 0.01)

        val updatedView = shadowWidgetManager.getViewFor(widgetId)
        assertNotNull(updatedView)
        assertEquals("6,400", updatedView.findViewById<TextView>(R.id.widget_price_24).text.toString())
        assertEquals("5,600", updatedView.findViewById<TextView>(R.id.widget_price_21).text.toString())
        assertEquals("4,800", updatedView.findViewById<TextView>(R.id.widget_price_18).text.toString())
    }
}
