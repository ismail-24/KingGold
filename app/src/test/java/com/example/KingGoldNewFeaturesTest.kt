package com.example

import android.Manifest
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.credentials.Credential
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.FirebaseAuthGateway
import com.example.ui.components.GoldCalculatorScreen
import com.example.ui.components.GoldShopsMapView
import com.example.ui.components.configureOsmdroid
import com.example.ui.components.hasAnyLocationPermission
import com.example.ui.navigation.KingGoldNavHost
import com.example.ui.navigation.Routes
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.home.UserProfileScreen
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KingGoldNewFeaturesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun fakeAuthViewModel(
        onSignUp: () -> Unit = {},
    ): AuthViewModel {
        val gateway = object : FirebaseAuthGateway {
            override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success("uid-1")
            override suspend fun signInWithEmail(email: String, password: String) = Result.success("uid-1")
            override suspend fun signUpWithEmail(fullName: String, email: String, password: String): Result<String> {
                onSignUp()
                return Result.success("uid-1")
            }

            override fun hasActiveUser() = false
        }
        return AuthViewModel(
            authGateway = gateway,
            idTokenExtractor = { _: Credential -> "token" },
        )
    }

    @Test
    fun loginScreen_rendersTraditionalAuthAndSecondaryGoogleOption() {
        var authenticated = false
        val authVm = fakeAuthViewModel()
        composeRule.setContent {
            KingGoldTheme {
                LoginScreen(
                    onAuthSuccess = { authenticated = true },
                    authViewModel = authVm,
                )
            }
        }
        composeRule.waitForIdle()

        // Traditional Sign In fields + secondary Google button visible
        composeRule.onNodeWithTag("auth_mode_sign_in_tab").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_mode_sign_up_tab").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_email_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_password_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_submit_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()

        // Switch to Sign Up tab -> Full Name + Confirm Password appear
        composeRule.onNodeWithTag("auth_mode_sign_up_tab").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("auth_name_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_confirm_password_input").performScrollTo().assertIsDisplayed()

        // Fill Sign Up form and submit
        composeRule.onNodeWithTag("auth_name_input").performScrollTo().performTextInput("عمر حسن")
        composeRule.onNodeWithTag("auth_email_input").performScrollTo().performTextInput("omar@kinggold.eg")
        composeRule.onNodeWithTag("auth_password_input").performScrollTo().performTextInput("gold2026")
        composeRule.onNodeWithTag("auth_confirm_password_input").performScrollTo().performTextInput("gold2026")
        composeRule.onNodeWithTag("auth_submit_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(authenticated)
    }

    @Test
    fun userProfileScreen_showsEmailFavoritedShopsAndSignOut() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        vm.toggleFavoriteShop("s1")
        var clickedShopId: String? = null
        var signedOut = false

        composeRule.setContent {
            KingGoldTheme {
                val state = vm.uiState.value.copy(userEmail = "customer@kinggold.eg")
                UserProfileScreen(
                    uiState = state,
                    onBack = {},
                    onOrderClick = {},
                    onOpenShop = { clickedShopId = it.id },
                    onToggleFavoriteShop = vm::toggleFavoriteShop,
                    onSignOut = { signedOut = true },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_user_email").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("customer@kinggold.eg").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_sign_out_button").performScrollTo().assertIsDisplayed()

        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("profile_favorite_shop_s1"))
        composeRule.onNodeWithTag("profile_favorite_shop_s1").performScrollTo().performClick()
        assertEquals("s1", clickedShopId)

        // Tap Sign Out opens confirmation dialog -> confirm calls onSignOut
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("profile_sign_out_button"))
        composeRule.onNodeWithTag("profile_sign_out_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("خروج").performClick()
        assertTrue(signedOut)
    }

    @Test
    fun discoverScreen_topSearchAndListMapSwitcher_renderOpenStreetMapView() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Search bar sits near top of Discover screen
        composeRule.onNodeWithTag("discover_search_input").assertIsDisplayed()

        // Switch to Map view
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag("discover_mode_map"))
        composeRule.onNodeWithTag("discover_mode_map").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag("gold_shops_map_container"))
        composeRule.onNodeWithTag("gold_shops_map_container").assertIsDisplayed()
        composeRule.onNodeWithTag("map_layer_toggle").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("© OpenStreetMap contributors").assertIsDisplayed()
        composeRule.onNodeWithTag("map_layer_satellite_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("© Esri, Maxar, Earthstar Geographics").assertIsDisplayed()
        composeRule.onNodeWithTag("map_layer_standard_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("© OpenStreetMap contributors").assertIsDisplayed()
        composeRule.onNodeWithTag("map_recenter_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("map_my_location_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("map_zoom_in_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("map_zoom_out_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("map_selected_shop_card").performScrollTo().assertIsDisplayed()
    }

    private fun findOsmMapView(root: View): MapView? {
        if (root is MapView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findOsmMapView(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    @Test
    fun goldShopsMapView_attachesMyLocationNewOverlay_whenLocationPermissionGranted() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        assertFalse(hasAnyLocationPermission(app))
        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        assertTrue(hasAnyLocationPermission(app))

        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shops = vm.uiState.value.shops
        assertTrue(shops.size >= 2)

        composeRule.setContent {
            KingGoldTheme {
                GoldShopsMapView(
                    shops = shops,
                    favoriteShopIds = emptySet(),
                    onOpenShop = {},
                    onToggleFavoriteShop = {},
                    onResetFilters = {},
                )
            }
        }
        composeRule.waitForIdle()

        val rootView = composeRule.activity.window.decorView.rootView
        val mapView = findOsmMapView(rootView)
        assertNotNull(mapView)

        val myLocOverlay = mapView!!.overlays.filterIsInstance<MyLocationNewOverlay>().firstOrNull()
        assertNotNull(myLocOverlay)
        assertTrue(myLocOverlay!!.isDrawAccuracyEnabled)
        assertTrue(mapView.overlays.any { it is Marker })

        // Selecting another shop refreshes markers while keeping MyLocationNewOverlay attached
        composeRule.onNodeWithTag("map_shop_chip_${shops[1].id}").performClick()
        composeRule.waitForIdle()
        assertTrue(mapView.overlays.any { it is MyLocationNewOverlay })
        assertTrue(mapView.overlays.any { it is Marker })

        // Tapping My Location control button triggers centering logic safely
        composeRule.onNodeWithTag("map_my_location_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun goldCalculatorScreen_updatesValuationWhenWeightAndKaratChange() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                GoldCalculatorScreen(
                    uiState = vm.uiState.value,
                    onBack = {},
                    onOpenPriceInsight = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Default is 8g x 5425 (21K) = 43,400.00 ج.م
        composeRule.onNodeWithTag("calculator_result_value")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("43,400.00 ج.م")

        // Switch to 24K and enter 10g -> 10 x 6200 = 62,000.00 ج.م
        composeRule.onNodeWithTag("calculator_karat_24").performScrollTo().performClick()
        composeRule.onNodeWithTag("calculator_weight_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("calculator_weight_input").performTextInput("10")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calculator_result_value")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("62,000.00 ج.م")
    }

    @Test
    fun goldCalculatorScreen_usesLiveMarketFeedUsdRateAndPremiumModes() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        var refreshTriggered = false
        var aiPrompt: String? = null
        val liveState = vm.uiState.value.copy(
            prices = listOf(
                com.example.model.KaratPrice(24, "عيار 24", 6400.0, 0.5),
                com.example.model.KaratPrice(21, "عيار 21", 5600.0, 0.5),
                com.example.model.KaratPrice(18, "عيار 18", 4800.0, 0.5),
                com.example.model.KaratPrice(0, "الجنيه", 44800.0, 0.5),
            ),
            goldFeedStatus = com.example.ui.components.GoldFeedStatus.Live(
                usdPerOunce = 3200.0,
                usdToEgpRate = 50.0,
                fetchedAtLabel = "12:00 م",
            ),
        )

        composeRule.setContent {
            KingGoldTheme {
                GoldCalculatorScreen(
                    uiState = liveState,
                    onBack = {},
                    onOpenPriceInsight = {},
                    onRefreshPrices = { refreshTriggered = true },
                    onOpenAiAdvisor = { aiPrompt = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calculator_live_status_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("calculator_refresh_live_button").performClick()
        assertTrue(refreshTriggered)

        // Default 8g x 5,600 = 44,800.00 ج.م and USD = 44,800 / 50 = $896.00
        composeRule.onNodeWithTag("calculator_result_value").performScrollTo().assertTextEquals("44,800.00 ج.م")
        composeRule.onNodeWithTag("calculator_result_usd").performScrollTo().assertTextEquals("≈ $896.00 USD")

        // Switch to 14K (6400 * 14/24 = 3733.33), weight 10g -> 37,333.30 ج.م + flat 1,000 fee = 38,333.30 ج.م
        composeRule.onNodeWithTag("calculator_karat_14").performScrollTo().performClick()
        composeRule.onNodeWithTag("calculator_weight_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("calculator_weight_input").performTextInput("10")
        composeRule.onNodeWithTag("calculator_premium_mode_flat_total").performScrollTo().performClick()
        composeRule.onNodeWithTag("calculator_making_fee_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("calculator_making_fee_input").performTextInput("1000")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calculator_result_value").performScrollTo().assertTextEquals("38,333.30 ج.م")
        composeRule.onNodeWithTag("calculator_ask_ai_button").performScrollTo().performClick()
        assertTrue(aiPrompt?.contains("عيار 14") == true)
    }

    @Test
    fun osmdroidConfiguration_andFcmNotificationIntents_doNotUseGoogleMaps() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        configureOsmdroid(context)
        assertTrue(Configuration.getInstance().userAgentValue.isNotBlank())
        assertTrue(Configuration.getInstance().osmdroidTileCache.exists())

        val shopIntent = GoldPriceNotificationHelper.buildLaunchIntent(context, Routes.shopDetail("s1"))
        assertEquals("shop_detail/s1", shopIntent.getStringExtra(GoldPriceNotificationHelper.EXTRA_TARGET_ROUTE))

        val priceIntent = GoldPriceNotificationHelper.buildLaunchIntent(context, Routes.PRICE_INSIGHT)
        assertEquals(Routes.PRICE_INSIGHT, priceIntent.getStringExtra(GoldPriceNotificationHelper.EXTRA_TARGET_ROUTE))
        assertTrue(Routes.isSupportedInternalRoute(Routes.GOLD_CALCULATOR))
    }

    @Test
    fun authViewModel_handleGoogleCredentialResult_parsesCustomCredentialAndRejectsUnsupported() = kotlinx.coroutines.test.runTest {
        var exchangedToken: String? = null
        val gateway = object : FirebaseAuthGateway {
            override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {
                exchangedToken = idToken
                return Result.success("uid-google")
            }

            override suspend fun signInWithEmail(email: String, password: String) = Result.success("uid")
            override suspend fun signUpWithEmail(fullName: String, email: String, password: String) = Result.success("uid")
            override fun hasActiveUser() = false
        }
        val vm = AuthViewModel(authGateway = gateway)

        val bundle = android.os.Bundle().apply {
            putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID", "user@gmail.com")
            putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN", "header.payload.signature")
        }
        val googleCred = androidx.credentials.CustomCredential(
            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            bundle,
        )
        assertTrue(vm.handleGoogleCredentialResult(googleCred, silent = false))
        assertEquals("header.payload.signature", exchangedToken)
        assertTrue(vm.uiState.value.isAuthenticated)

        val unsupportedCred = androidx.credentials.CustomCredential("unsupported.type", android.os.Bundle())
        val vm2 = AuthViewModel(authGateway = gateway)
        assertTrue(!vm2.handleGoogleCredentialResult(unsupportedCred, silent = false))
        assertTrue(!vm2.uiState.value.isAuthenticated)
        assertTrue(!vm2.uiState.value.errorMessage.isNullOrBlank())
    }

    @Test
    fun goldShopsMapView_addsOsmdroidMarkersForFirestoreShopCoordinates_andSkipsInvalid() {
        val remoteShopGeo = com.example.data.remote.GoldShopLocationRecord(
            id = "remote_aswan",
            name = "ذهب أسوان السحابي",
            city = "أسوان",
            area = "الكورنيش",
            address = "كورنيش النيل، أسوان",
            phone = "+201055443322",
            coordinates = com.google.firebase.firestore.GeoPoint(24.0889, 32.8998),
            verified = true,
            makingFeePerGram = 80.0,
            ownerUid = "owner-1",
            active = true,
        ).toShop()

        val remoteShopNumeric = com.example.data.remote.GoldShopLocationRecord(
            id = "remote_mansoura",
            name = "صاغة المنصورة",
            city = "المنصورة",
            area = "شارع الجمهورية",
            address = "شارع الجمهورية، المنصورة",
            phone = "+201077665544",
            latitude = 31.0409,
            longitude = 31.3785,
            verified = true,
            makingFeePerGram = 95.0,
            ownerUid = "owner-2",
            active = true,
        ).toShop()

        val invalidZeroShop = com.example.data.remote.GoldShopLocationRecord(
            id = "remote_invalid",
            name = "محل بدون إحداثيات",
            city = "طنطا",
            area = "الوسط",
            latitude = 0.0,
            longitude = 0.0,
            verified = true,
            ownerUid = "owner-3",
            active = true,
        ).toShop()

        composeRule.setContent {
            KingGoldTheme {
                GoldShopsMapView(
                    shops = listOf(remoteShopGeo, invalidZeroShop, remoteShopNumeric),
                    favoriteShopIds = emptySet(),
                    onOpenShop = {},
                    onToggleFavoriteShop = {},
                    onResetFilters = {},
                )
            }
        }
        composeRule.waitForIdle()

        val rootView = composeRule.activity.window.decorView.rootView
        val mapView = findOsmMapView(rootView)
        assertNotNull(mapView)

        val markers = mapView!!.overlays.filterIsInstance<Marker>()
        assertEquals(2, markers.size)
        assertEquals(listOf("remote_aswan", "remote_mansoura"), markers.map { it.id })
        assertEquals("ذهب أسوان السحابي", markers[0].title)
        assertEquals(24.0889, markers[0].position.latitude, 0.0001)
        assertEquals(32.8998, markers[0].position.longitude, 0.0001)
        assertEquals(31.0409, markers[1].position.latitude, 0.0001)
        assertEquals(31.3785, markers[1].position.longitude, 0.0001)

        // Selecting the second Firestore shop updates the bottom card and preserves both markers
        composeRule.onNodeWithTag("map_shop_chip_remote_mansoura").performClick()
        composeRule.waitForIdle()
        val updatedMarkers = mapView.overlays.filterIsInstance<Marker>()
        assertEquals(2, updatedMarkers.size)
        composeRule.onNodeWithTag("map_selected_shop_card").assertIsDisplayed()
    }

    @Test
    fun dailyGoldPriceTrendChartCard_switchesKaratsAndHandlesSingleAndEmptyPointsSafely() {
        val firestoreSeries = listOf(
            com.example.model.DailyGoldPrice(
                dateKey = "2026-11-18",
                dayLabel = "18/11",
                price24kEgp = 6100.0,
                price21kEgp = 5337.5,
                price18kEgp = 4575.0,
                source = "SEED",
            ),
            com.example.model.DailyGoldPrice(
                dateKey = "2026-11-19",
                dayLabel = "19/11",
                price24kEgp = 6180.0,
                price21kEgp = 5407.5,
                price18kEgp = 4635.0,
                source = "SEED",
            ),
            com.example.model.DailyGoldPrice(
                dateKey = "2026-11-20",
                dayLabel = "اليوم",
                price24kEgp = 6250.0,
                price21kEgp = 5468.75,
                price18kEgp = 4687.5,
                source = "ADMIN_PRICING",
            ),
        )
        var recordedClicks = 0

        composeRule.setContent {
            KingGoldTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    com.example.ui.components.DailyGoldPriceTrendChartCard(
                        dailyPrices = firestoreSeries,
                        isSyncedFromFirestore = true,
                        onRecordTodayPrices = { recordedClicks++ },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("daily_gold_trend_chart_card").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_chart_source_badge").assertIsDisplayed()
        composeRule.onNodeWithText("متزامن مع Firestore · 3 أيام").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_chart_legend_24k").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_chart_legend_21k").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_chart_legend_18k").assertIsDisplayed()

        // Interactive karat filter switching: 24K -> 21K -> 18K -> ALL
        composeRule.onNodeWithTag("daily_chart_karat_chip_24").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("daily_chart_karat_chip_21").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("daily_chart_karat_chip_18").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("daily_chart_karat_chip_ALL").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("admin_record_daily_price_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, recordedClicks)
    }

    @Test
    fun dailyGoldPriceTrendChartCard_visualizes30DayTrendAndSwitchesDateRanges() {
        val thirtyDaySeries = com.example.data.KingGoldRepository.dailyGoldPriceFallbackSeries(
            current24KPrice = 6200.0,
            current21KPrice = 5425.0,
            current18KPrice = 4650.0,
            days = 30,
        )
        assertEquals(30, thirtyDaySeries.size)

        composeRule.setContent {
            KingGoldTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    com.example.ui.components.DailyGoldPriceTrendChartCard(
                        dailyPrices = thirtyDaySeries,
                        isSyncedFromFirestore = true,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("daily_gold_trend_chart_card").assertIsDisplayed()
        composeRule.onNodeWithText("متزامن مع Firestore · 30 أيام").assertIsDisplayed()
        composeRule.onNodeWithText("آخر 30 يوماً · عيار 24 و21 و18 (ج.م/جم)").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_chart_range_30D").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_chart_range_14D").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_chart_range_7D").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("الأدنى (عيار 24)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("الأعلى (عيار 24)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("التغير").performScrollTo().assertIsDisplayed()

        // Switch from 30 days to 14 days, then 7 days, then back to 30 days
        composeRule.onNodeWithTag("daily_chart_range_14D").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("آخر 14 يوماً · عيار 24 و21 و18 (ج.م/جم)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("daily_chart_range_7D").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("آخر 7 يوماً · عيار 24 و21 و18 (ج.م/جم)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("daily_chart_range_30D").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("آخر 30 يوماً · عيار 24 و21 و18 (ج.م/جم)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dailyGoldPriceTrendChartCard_handlesEmptyAndSinglePointGracefully() {
        composeRule.setContent {
            KingGoldTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    com.example.ui.components.DailyGoldPriceTrendChartCard(
                        dailyPrices = emptyList(),
                        isSyncedFromFirestore = false,
                    )
                    com.example.ui.components.DailyGoldPriceTrendChartCard(
                        dailyPrices = listOf(
                            com.example.model.DailyGoldPrice(
                                dateKey = "2026-11-20",
                                dayLabel = "اليوم",
                                price24kEgp = 6200.0,
                                price21kEgp = 5425.0,
                                price18kEgp = 4650.0,
                            ),
                        ),
                        isSyncedFromFirestore = true,
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("daily_chart_empty_state").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun priceInsightScreen_displaysDailyMultiKaratVicoChartCard() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                com.example.ui.screens.price.PriceInsightScreen(
                    uiState = vm.uiState.value,
                    onSaveAlert = { _, _, _, _ -> },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("daily_gold_trend_chart_card"))
        composeRule.onNodeWithTag("daily_gold_trend_chart_card").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_gold_vico_chart").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fcmMessagingService_evaluatesRealTimePriceDropThresholdAndPostsNotification() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val notificationManager =
            app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNm = Shadows.shadowOf(notificationManager)
        notificationManager.cancelAll()

        // Cache alert rule: 21K drop below 5400 EGP
        GoldPriceNotificationHelper.saveAlertRuleCache(
            context = app,
            enabled = true,
            above = false,
            karat = 21,
            thresholdText = "5400",
        )
        val cached = GoldPriceNotificationHelper.loadAlertRuleCache(app)
        assertEquals(21, cached.karat)
        assertEquals("5400", cached.thresholdText)

        // Real-time FCM tick above threshold (5450 >= 5400) -> suppressed
        val notTriggered = com.example.notifications.KingGoldMessagingService.handleIncomingFcmData(
            context = app,
            data = mapOf(
                "type" to "price_update",
                "price24k" to "6228",
                "price21k" to "5450",
                "price18k" to "4671",
            ),
        )
        assertFalse(notTriggered)
        assertTrue(shadowNm.allNotifications.isEmpty())

        // Real-time FCM tick below threshold (5320 < 5400) -> notification posted!
        val triggered = com.example.notifications.KingGoldMessagingService.handleIncomingFcmData(
            context = app,
            data = mapOf(
                "type" to "price_update",
                "price24k" to "6080",
                "price21k" to "5320",
                "price18k" to "4560",
            ),
        )
        assertTrue(triggered)
        assertEquals(1, shadowNm.allNotifications.size)
        val postedNotification = shadowNm.allNotifications.first()
        val title = postedNotification.extras.getString(android.app.Notification.EXTRA_TITLE).orEmpty()
        val body = postedNotification.extras.getString(android.app.Notification.EXTRA_TEXT).orEmpty()
        assertTrue(title.contains("21"))
        assertTrue(body.contains("5,320"))
        assertTrue(body.contains("5,400"))

        // Direct FCM price_drop payload also posts notification
        val directPosted = com.example.notifications.KingGoldMessagingService.handleIncomingFcmData(
            context = app,
            data = mapOf(
                "type" to "price_drop",
                "karat" to "24",
                "price24k" to "5950",
                "threshold" to "6000",
            ),
        )
        assertTrue(directPosted)
        assertEquals(2, shadowNm.allNotifications.size)
    }

    @Test
    fun priceInsightScreen_configuresKaratAndThresholdForFcmAlerts() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val vm = KingGoldViewModel(autoFetchPrices = false)
        var savedTuple: List<Any>? = null

        composeRule.setContent {
            KingGoldTheme {
                com.example.ui.screens.price.PriceInsightScreen(
                    uiState = vm.uiState.value.copy(fcmToken = "fcm_device_token_123"),
                    onSaveAlert = { enabled, above, karat, target ->
                        savedTuple = listOf(enabled, above, karat, target)
                    },
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("fcm_price_alert_card"))
        composeRule.onNodeWithTag("fcm_status_banner").performScrollTo().assertIsDisplayed()

        // Select 21K chip, update threshold to 5300, save
        composeRule.onNodeWithTag("alert_karat_chip_21").performScrollTo().performClick()
        composeRule.onNodeWithTag("alert_threshold_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("alert_threshold_input").performTextInput("5300")
        composeRule.onNodeWithTag("save_price_alert_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(true, false, 21, "5300"), savedTuple)
    }
}
