package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.test.core.app.ApplicationProvider
import com.example.model.MerchantProduct
import com.example.model.MerchantProductStatus
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.model.Shop
import com.example.model.ShopSortOption
import com.example.ui.components.GoldShopsMapView
import com.example.ui.components.calculateHaversineDistanceKm
import com.example.ui.components.filterAndSortShopsForMap
import com.example.ui.components.parseShopSearchQuery
import com.example.ui.components.resolveBestKnownLocation
import com.example.ui.navigation.KingGoldNavHost
import com.example.ui.screens.home.NearbyShopsMapScreen
import com.example.ui.state.KingGoldUiState
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OsmMapNearbySearchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleShops = listOf(
        Shop(
            id = "cairo_verified_near",
            name = "صاغة التحرير الموثّقة",
            area = "وسط البلد",
            address = "ميدان التحرير، القاهرة",
            phone = "+201001112233",
            rating = 4.9,
            reviews = 180,
            distanceKm = 9.9,
            verified = true,
            closesAt = "10:00 م",
            makingFeePerGram = 85.0,
            city = "القاهرة",
            latitude = 30.0480,
            longitude = 31.2336,
        ),
        Shop(
            id = "cairo_unverified_closest",
            name = "محل غير موثّق قريب",
            area = "عابدين",
            address = "شارع الجمهورية، القاهرة",
            phone = "+201004445566",
            rating = 4.2,
            reviews = 22,
            distanceKm = 0.5,
            verified = false,
            closesAt = "9:00 م",
            makingFeePerGram = 120.0,
            city = "القاهرة",
            latitude = 30.0448,
            longitude = 31.2410,
        ),
        Shop(
            id = "maadi_verified_mid",
            name = "ذهب المعادي الأصلي",
            area = "المعادي",
            address = "شارع 9، المعادي، القاهرة",
            phone = "+201007778899",
            rating = 4.8,
            reviews = 140,
            distanceKm = 1.0,
            verified = true,
            closesAt = "9:30 م",
            makingFeePerGram = 90.0,
            city = "القاهرة",
            latitude = 29.9602,
            longitude = 31.2569,
        ),
        Shop(
            id = "alex_verified_far",
            name = "مجوهرات الإسكندرية",
            area = "سموحة",
            address = "شارع فوزي معاذ، الإسكندرية",
            phone = "+201009990011",
            rating = 4.9,
            reviews = 95,
            distanceKm = 2.0,
            verified = true,
            closesAt = "10:30 م",
            makingFeePerGram = 80.0,
            city = "الإسكندرية",
            latitude = 31.2156,
            longitude = 29.9467,
        ),
        Shop(
            id = "no_coords_verified",
            name = "متجر بدون إحداثيات",
            area = "الجيزة",
            address = "الدقي، الجيزة",
            phone = "+201002223344",
            rating = 4.6,
            reviews = 40,
            distanceKm = 3.0,
            verified = true,
            closesAt = "9:00 م",
            makingFeePerGram = 95.0,
            city = "الجيزة",
            latitude = 0.0,
            longitude = 0.0,
        ),
    )

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
    fun haversineAndProximityFilter_findsVerifiedShopsNearLocationAndSortsByDistance() {
        // Downtown Cairo anchor
        val tahrirLat = 30.0444
        val tahrirLon = 31.2357

        val distToMaadi = calculateHaversineDistanceKm(tahrirLat, tahrirLon, 29.9602, 31.2569)
        assertTrue(distToMaadi in 8.5..10.5)

        // Filter verified shops within 15 km -> keeps cairo_verified_near (~0.5 km) then maadi_verified_mid (~9.6 km)
        val nearbyVerified = filterAndSortShopsForMap(
            shops = sampleShops,
            searchQuery = "",
            verifiedOnly = true,
            anchorLat = tahrirLat,
            anchorLon = tahrirLon,
            nearbyRadiusKm = 15.0,
        )
        assertEquals(listOf("cairo_verified_near", "maadi_verified_mid"), nearbyVerified.map { it.id })
        assertTrue(nearbyVerified[0].distanceKm < nearbyVerified[1].distanceKm)
        assertTrue(nearbyVerified.all { it.verified })

        // Tightening radius to 2 km leaves only cairo_verified_near
        val within2Km = filterAndSortShopsForMap(
            shops = sampleShops,
            searchQuery = "",
            verifiedOnly = true,
            anchorLat = tahrirLat,
            anchorLon = tahrirLon,
            nearbyRadiusKm = 2.0,
        )
        assertEquals(listOf("cairo_verified_near"), within2Km.map { it.id })

        // Turning off verifiedOnly within 2 km also surfaces cairo_unverified_closest
        val within2KmAll = filterAndSortShopsForMap(
            shops = sampleShops,
            searchQuery = "",
            verifiedOnly = false,
            anchorLat = tahrirLat,
            anchorLon = tahrirLon,
            nearbyRadiusKm = 2.0,
        )
        assertEquals(2, within2KmAll.size)
        assertTrue(within2KmAll.any { it.id == "cairo_unverified_closest" })
    }

    @Test
    fun viewModel_searchAnchorAndRadius_filtersAndComputesLiveDistances() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        // El-Sagha coordinate -> s1 (0.0 km), s4/s5/s3 (~0.06 - 0.14 km), s2/s8/s6/s7 farther away
        vm.updateSearchAnchor(latitude = 30.0481, longitude = 31.2614, label = "موقعي الحالي")
        vm.updateNearbyRadiusKm(0.03)

        val immediate = vm.uiState.value.filteredShops
        assertEquals(listOf("s1"), immediate.map { it.id })
        assertTrue(immediate.first().distanceKm <= 0.03)

        vm.updateNearbyRadiusKm(1.0)
        val elSaghaCluster = vm.uiState.value.filteredShops
        assertEquals(setOf("s1", "s3", "s4", "s5"), elSaghaCluster.map { it.id }.toSet())

        // Clearing anchor restores all 8 verified Egyptian gold shops
        vm.clearSearchAnchor()
        assertEquals(8, vm.uiState.value.filteredShops.size)
    }

    @Test
    fun goldShopsMapView_nearbyLocationAndSearchQuery_updateOsmdroidMarkersAndRadiusOverlay() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 30.0444
            longitude = 31.2357
            accuracy = 12f
            time = System.currentTimeMillis()
        }
        Shadows.shadowOf(locationManager).setLastKnownLocation(LocationManager.GPS_PROVIDER, gpsLocation)
        assertNotNull(resolveBestKnownLocation(app))

        composeRule.setContent {
            KingGoldTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    GoldShopsMapView(
                        shops = sampleShops,
                        favoriteShopIds = emptySet(),
                        onOpenShop = {},
                        onToggleFavoriteShop = {},
                        onResetFilters = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val rootView = composeRule.activity.window.decorView.rootView
        val mapView = findOsmMapView(rootView)
        assertNotNull(mapView)

        // Initially verifiedOnly=true with no radius -> 3 verified mappable shops (cairo_verified_near, maadi_verified_mid, alex_verified_far)
        assertEquals(3, mapView!!.overlays.filterIsInstance<Marker>().size)

        // Tap "قريب من موقعي" -> resolves ShadowLocationManager GPS fix (30.0444, 31.2357) and sets 15 km radius
        composeRule.onNodeWithTag("map_nearby_my_location_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("map_location_status_banner").performScrollTo().assertIsDisplayed()
        // Radius circle + anchor spot Polygon overlays added without inflating Marker count
        assertTrue(mapView.overlays.filterIsInstance<Polygon>().size >= 2)
        val nearbyMarkers = mapView.overlays.filterIsInstance<Marker>()
        assertEquals(listOf("cairo_verified_near", "maadi_verified_mid"), nearbyMarkers.map { it.id })

        // Filter by map search query "المعادي" -> narrows OSMDroid markers to maadi_verified_mid
        composeRule.onNodeWithTag("map_search_input").performScrollTo().performTextInput("المعادي")
        composeRule.waitForIdle()
        val queryMarkers = mapView.overlays.filterIsInstance<Marker>()
        assertEquals(listOf("maadi_verified_mid"), queryMarkers.map { it.id })

        // Clear search and switch radius to 2 km -> only cairo_verified_near
        composeRule.onNodeWithTag("map_search_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("map_radius_chip_2").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("cairo_verified_near"), mapView.overlays.filterIsInstance<Marker>().map { it.id })

        // Search a query with zero matches -> shows map_empty_state_card + keeps expand/reset controls reachable
        composeRule.onNodeWithTag("map_search_input").performScrollTo().performTextInput("غير موجود")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_empty_state_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("map_expand_radius_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("map_empty_reset_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // After reset, verified mappable markers return cleanly
        assertEquals(3, mapView.overlays.filterIsInstance<Marker>().size)
    }

    @Test
    fun parseShopSearchQuery_and_filterAndSortShopsForMap_matchShopNameAndKaratRatings() {
        val p24 = parseShopSearchQuery("24")
        assertEquals(24, p24.detectedKarat)
        assertEquals("", p24.remainingText)

        val p21Arabic = parseShopSearchQuery("عيار ٢١")
        assertEquals(21, p21Arabic.detectedKarat)
        assertEquals("", p21Arabic.remainingText)

        val p18English = parseShopSearchQuery("18K")
        assertEquals(18, p18English.detectedKarat)
        assertEquals("", p18English.remainingText)

        val pCombined = parseShopSearchQuery("المعادي عيار 21")
        assertEquals(21, pCombined.detectedKarat)
        assertEquals("المعادي", pCombined.remainingText)

        val pStreet = parseShopSearchQuery("8 زقاق")
        assertEquals(null, pStreet.detectedKarat)
        assertEquals("8 زقاق", pStreet.remainingText)

        val karatsByShop = mapOf(
            "cairo_verified_near" to setOf(24, 21),
            "maadi_verified_mid" to setOf(21, 18),
            "alex_verified_far" to setOf(18),
        )
        val provider: (Shop) -> Set<Int> = { karatsByShop[it.id].orEmpty() }

        val only24 = filterAndSortShopsForMap(
            shops = sampleShops,
            searchQuery = "عيار 24",
            verifiedOnly = true,
            anchorLat = null,
            anchorLon = null,
            nearbyRadiusKm = null,
            shopKaratsProvider = provider,
        )
        assertEquals(listOf("cairo_verified_near"), only24.map { it.id })

        val only18 = filterAndSortShopsForMap(
            shops = sampleShops,
            searchQuery = "18k",
            verifiedOnly = true,
            anchorLat = null,
            anchorLon = null,
            nearbyRadiusKm = null,
            shopKaratsProvider = provider,
        )
        assertEquals(listOf("maadi_verified_mid", "alex_verified_far"), only18.map { it.id })

        val maadi18 = filterAndSortShopsForMap(
            shops = sampleShops,
            searchQuery = "المعادي 18",
            verifiedOnly = true,
            anchorLat = null,
            anchorLon = null,
            nearbyRadiusKm = null,
            shopKaratsProvider = provider,
        )
        assertEquals(listOf("maadi_verified_mid"), maadi18.map { it.id })

        val chipFiltered24 = filterAndSortShopsForMap(
            shops = sampleShops,
            searchQuery = "",
            verifiedOnly = true,
            anchorLat = null,
            anchorLon = null,
            nearbyRadiusKm = null,
            selectedKarat = 24,
            shopKaratsProvider = provider,
        )
        assertEquals(listOf("cairo_verified_near"), chipFiltered24.map { it.id })
    }

    @Test
    fun nearbyShopsMapScreen_topSearchBarAndKaratChips_filterByShopNameAndGoldKarat() {
        val merchant24 = MerchantStore(
            id = "m_24",
            ownerUid = "u1",
            shopName = "دار السبائك الذهبية",
            area = "وسط البلد",
            address = "شارع قصر النيل، القاهرة",
            contactPhone = "+201001110024",
            status = MerchantStatus.APPROVED,
            city = "القاهرة",
            latitude = 30.0460,
            longitude = 31.2380,
        )
        val merchant21 = MerchantStore(
            id = "m_21",
            ownerUid = "u2",
            shopName = "مجوهرات النيل التراثية",
            area = "المعادي",
            address = "شارع 9، المعادي، القاهرة",
            contactPhone = "+201001110021",
            status = MerchantStatus.APPROVED,
            city = "القاهرة",
            latitude = 29.9610,
            longitude = 31.2580,
        )
        val merchant18 = MerchantStore(
            id = "m_18",
            ownerUid = "u3",
            shopName = "أتيليه الماسة الحديث",
            area = "الزمالك",
            address = "شارع 26 يوليو، الزمالك، القاهرة",
            contactPhone = "+201001110018",
            status = MerchantStatus.APPROVED,
            city = "القاهرة",
            latitude = 30.0609,
            longitude = 31.2197,
        )
        val products = listOf(
            MerchantProduct(
                id = "mp_24",
                merchantId = "m_24",
                ownerUid = "u1",
                name = "سبيكة ذهب صافية",
                category = "سبائك ذهبية",
                karat = 24,
                weightGrams = 10.0,
                stockQuantity = 5,
                status = MerchantProductStatus.ACTIVE,
            ),
            MerchantProduct(
                id = "mp_21",
                merchantId = "m_21",
                ownerUid = "u2",
                name = "غويشة كلاسيك",
                category = "أساور وغوايش",
                karat = 21,
                weightGrams = 12.0,
                stockQuantity = 4,
                status = MerchantProductStatus.ACTIVE,
            ),
            MerchantProduct(
                id = "mp_18",
                merchantId = "m_18",
                ownerUid = "u3",
                name = "خاتم سوليتير عصري",
                category = "خواتم",
                karat = 18,
                weightGrams = 4.0,
                stockQuantity = 6,
                status = MerchantProductStatus.ACTIVE,
            ),
        )

        composeRule.setContent {
            KingGoldTheme {
                var state by remember {
                    mutableStateOf(
                        KingGoldUiState(
                            staticShops = emptyList(),
                            approvedMerchants = listOf(merchant24, merchant21, merchant18),
                            activeMerchantProducts = products,
                        ),
                    )
                }
                NearbyShopsMapScreen(
                    uiState = state,
                    onSearchChange = { query -> state = state.copy(searchQuery = query) },
                    onApplyFilters = { karat, verifiedOnly, sort ->
                        state = state.copy(
                            selectedKaratFilter = karat,
                            verifiedOnlyFilter = verifiedOnly,
                            sortOption = sort,
                        )
                    },
                    onSelectCityFilter = { city -> state = state.copy(selectedCityFilter = city) },
                    onToggleFavoritesOnlyFilter = { fav -> state = state.copy(favoritesOnlyFilter = fav) },
                    onOpenCart = {},
                    onOpenShop = {},
                    onToggleFavoriteShop = {},
                    onUpdateSearchAnchor = { lat, lon, label ->
                        state = state.copy(
                            searchAnchorLat = lat,
                            searchAnchorLon = lon,
                            searchAnchorLabel = label,
                            sortOption = ShopSortOption.NEAREST,
                        )
                    },
                    onUpdateNearbyRadius = { r -> state = state.copy(nearbyRadiusKm = r) },
                    onClearSearchAnchor = {
                        state = state.copy(
                            searchAnchorLat = null,
                            searchAnchorLon = null,
                            searchAnchorLabel = null,
                            nearbyRadiusKm = null,
                        )
                    },
                )
            }
        }
        composeRule.waitForIdle()

        // Top search bar & karat filter chips must be visible right at the top without scrolling
        composeRule.onNodeWithTag("map_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("map_karat_filter_row").assertIsDisplayed()
        composeRule.onNodeWithTag("map_karat_chip_24").assertIsDisplayed()
        composeRule.onNodeWithTag("map_karat_chip_21").assertIsDisplayed()
        composeRule.onNodeWithTag("map_karat_chip_18").assertIsDisplayed()

        val rootView = composeRule.activity.window.decorView.rootView
        val mapView = findOsmMapView(rootView)
        assertNotNull(mapView)
        assertEquals(setOf("m_24", "m_21", "m_18"), mapView!!.overlays.filterIsInstance<Marker>().map { it.id }.toSet())

        // Filter by shop name in top search bar
        composeRule.onNodeWithTag("map_search_input").performTextInput("الماسة")
        composeRule.waitForIdle()
        assertEquals(listOf("m_18"), mapView.overlays.filterIsInstance<Marker>().map { it.id })

        // Clear and filter by typing karat rating "عيار 21"
        composeRule.onNodeWithTag("map_search_input").performTextClearance()
        composeRule.onNodeWithTag("map_search_input").performTextInput("عيار 21")
        composeRule.waitForIdle()
        assertEquals(listOf("m_21"), mapView.overlays.filterIsInstance<Marker>().map { it.id })

        // Clear and filter by typing "24k"
        composeRule.onNodeWithTag("map_search_input").performTextClearance()
        composeRule.onNodeWithTag("map_search_input").performTextInput("24k")
        composeRule.waitForIdle()
        assertEquals(listOf("m_24"), mapView.overlays.filterIsInstance<Marker>().map { it.id })

        // Clear search text, then filter via top karat filter chip "عيار 18"
        composeRule.onNodeWithTag("map_search_clear").performClick()
        composeRule.waitForIdle()
        assertEquals(3, mapView.overlays.filterIsInstance<Marker>().size)

        composeRule.onNodeWithTag("map_karat_chip_18").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("m_18"), mapView.overlays.filterIsInstance<Marker>().map { it.id })

        // Tap "كل الأعيرة" -> restores all 3 shops
        composeRule.onNodeWithTag("map_karat_chip_all").performClick()
        composeRule.waitForIdle()
        assertEquals(3, mapView.overlays.filterIsInstance<Marker>().size)
    }

    @Test
    fun kingGoldNavHost_requestsLocationPermissionOnStartup_whenNotGranted() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    requestLocationOnStart = true,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val lastPermRequest = Shadows.shadowOf(composeRule.activity).lastRequestedPermission
        val appStartedIntent = Shadows.shadowOf(app).nextStartedActivity
        val requestedPerms = lastPermRequest?.requestedPermissions?.toSet()
            ?: appStartedIntent
                ?.getStringArrayExtra("androidx.activity.result.contract.extra.PERMISSIONS")
                ?.toSet()
            ?: emptySet()
        assertTrue(requestedPerms.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertTrue(requestedPerms.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @Test
    fun kingGoldNavHost_startupGrantedLocation_automaticallyCentersMapOnUserPosition() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val locationManager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val userLat = 31.2001
        val userLon = 29.9187
        val gpsLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = userLat
            longitude = userLon
            accuracy = 10f
            time = System.currentTimeMillis()
        }
        Shadows.shadowOf(locationManager).setLastKnownLocation(LocationManager.GPS_PROVIDER, gpsLocation)

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

        // User location is resolved automatically at app startup
        assertEquals(userLat, vm.uiState.value.userLocationLat ?: 0.0, 0.0001)
        assertEquals(userLon, vm.uiState.value.userLocationLon ?: 0.0, 0.0001)

        // Navigating to the Map screen automatically centers OSMDroid MapView on user's position
        composeRule.onNodeWithTag("tab_map").performClick()
        composeRule.waitForIdle()

        val rootView = composeRule.activity.window.decorView.rootView
        val mapView = findOsmMapView(rootView)
        assertNotNull(mapView)
        assertEquals(userLat, mapView!!.mapCenter.latitude, 0.01)
        assertEquals(userLon, mapView.mapCenter.longitude, 0.01)
        // All 8 verified shops remain plotted on the map
        assertEquals(8, mapView.overlays.filterIsInstance<Marker>().size)
    }

    @Test
    fun kingGoldNavHost_whenStartupPermissionDenied_showsNonBlockingNoticeAndAllShops() {
        val vm = KingGoldViewModel(autoFetchPrices = false).apply {
            markLocationPermissionDenied()
        }
        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tab_map").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("map_location_denied_banner").performScrollTo().assertIsDisplayed()
        val rootView = composeRule.activity.window.decorView.rootView
        val mapView = findOsmMapView(rootView)
        assertNotNull(mapView)
        assertEquals(8, mapView!!.overlays.filterIsInstance<Marker>().size)
    }

    @Test
    fun nearbyShopsMapScreen_markerDetailsFavoriteHeartAndSavedShopsDrawer_manageSavedShops() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        var openedFullSavedScreen = false
        var openedShopId: String? = null

        composeRule.setContent {
            KingGoldTheme {
                var state by remember {
                    mutableStateOf(
                        KingGoldUiState(
                            staticShops = sampleShops,
                            favoriteShopIds = emptySet(),
                        ),
                    )
                }
                NearbyShopsMapScreen(
                    uiState = state,
                    onSearchChange = { state = state.copy(searchQuery = it) },
                    onApplyFilters = { karat, verifiedOnly, sort ->
                        state = state.copy(
                            selectedKaratFilter = karat,
                            verifiedOnlyFilter = verifiedOnly,
                            sortOption = sort,
                        )
                    },
                    onSelectCityFilter = { state = state.copy(selectedCityFilter = it) },
                    onToggleFavoritesOnlyFilter = { state = state.copy(favoritesOnlyFilter = it) },
                    onOpenCart = {},
                    onOpenShop = { openedShopId = it.id },
                    onToggleFavoriteShop = { id ->
                        val next = if (state.favoriteShopIds.contains(id)) {
                            state.favoriteShopIds - id
                        } else {
                            state.favoriteShopIds + id
                        }
                        state = state.copy(favoriteShopIds = next)
                    },
                    onUpdateSearchAnchor = { _, _, _ -> },
                    onUpdateNearbyRadius = { state = state.copy(nearbyRadiusKm = it) },
                    onClearSearchAnchor = {},
                    onFocusShopOnMap = { vm.focusShopOnMap(it) },
                    onOpenSavedShopsScreen = { openedFullSavedScreen = true },
                )
            }
        }
        composeRule.waitForIdle()

        val rootView = composeRule.activity.window.decorView.rootView
        val mapView = findOsmMapView(rootView)
        assertNotNull(mapView)

        // 1. Open Saved Shops drawer when empty -> shows empty state
        composeRule.onNodeWithTag("map_saved_shops_drawer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_shops_empty_state").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_shops_drawer_close_button").performClick()
        composeRule.waitForIdle()

        // 2. Tap favorite heart button in the selected marker details card -> saves selected shop (maadi_verified_mid)
        composeRule.onNodeWithTag("map_selected_favorite_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Also test osmdroid Marker InfoWindow heart button directly for cairo_verified_near
        val cairoMarker = mapView!!.overlays.filterIsInstance<Marker>().first { it.id == "cairo_verified_near" }
        composeRule.runOnUiThread {
            cairoMarker.showInfoWindow()
        }
        composeRule.waitForIdle()
        val infoHeartView = cairoMarker.infoWindow?.view?.findViewWithTag<View>("marker_info_favorite_cairo_verified_near")
        assertNotNull(infoHeartView)
        composeRule.runOnUiThread {
            infoHeartView!!.performClick()
        }
        composeRule.waitForIdle()

        // 3. Re-open Saved Shops drawer -> both saved shops appear cleanly (sorted by distance: maadi then cairo)
        composeRule.onNodeWithTag("map_saved_shops_drawer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_shops_drawer").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_shops_list").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_shop_card_maadi_verified_mid").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_shops_list")
            .performScrollToNode(hasTestTag("saved_shop_card_cairo_verified_near"))
        composeRule.onNodeWithTag("saved_shop_card_cairo_verified_near").assertIsDisplayed()

        // Remove cairo_verified_near inside the drawer -> updates saved list immediately
        composeRule.onNodeWithTag("saved_shop_favorite_cairo_verified_near").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag("saved_shop_card_cairo_verified_near").assertCountEquals(0)
        composeRule.onNodeWithTag("saved_shop_card_maadi_verified_mid").assertIsDisplayed()

        // Focus remaining saved shop on map -> closes drawer & selects maadi_verified_mid
        composeRule.onNodeWithTag("saved_shop_map_focus_maadi_verified_mid").performClick()
        composeRule.waitForIdle()
        assertEquals("maadi_verified_mid", vm.uiState.value.focusedMapShopId)

        // Open full standalone saved shops view button from drawer
        composeRule.onNodeWithTag("map_saved_shops_drawer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_shops_open_full_screen_button").performClick()
        composeRule.waitForIdle()
        assertTrue(openedFullSavedScreen)

        // Opening shop directly from drawer triggers onOpenShop
        composeRule.onNodeWithTag("map_saved_shops_drawer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_shop_open_maadi_verified_mid").performClick()
        composeRule.waitForIdle()
        assertEquals("maadi_verified_mid", openedShopId)
    }

    @Test
    fun kingGoldNavHost_favoriteFromMap_opensStandaloneSavedShopsViewAndFocusesBackOnMap() {
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

        // Switch to Map tab and save the currently selected shop marker via favorite heart button
        composeRule.onNodeWithTag("tab_map").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_selected_favorite_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(vm.uiState.value.favoriteShopIds.isEmpty())
        val savedShopId = vm.uiState.value.favoriteShopIds.first()

        // Open Saved Shops Drawer and jump to standalone SavedShopsScreen
        composeRule.onNodeWithTag("map_saved_shops_drawer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_shops_open_full_screen_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("saved_shops_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_shop_card_$savedShopId").assertIsDisplayed()

        // Focus saved shop back on Map -> navigates to Map tab with focused shop
        composeRule.onNodeWithTag("saved_shop_map_focus_$savedShopId").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nearby_map_main_screen").assertIsDisplayed()
        assertEquals(savedShopId, vm.uiState.value.focusedMapShopId)
    }
}
