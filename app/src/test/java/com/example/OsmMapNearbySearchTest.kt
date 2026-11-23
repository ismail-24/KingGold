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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.model.Shop
import com.example.ui.components.GoldShopsMapView
import com.example.ui.components.calculateHaversineDistanceKm
import com.example.ui.components.filterAndSortShopsForMap
import com.example.ui.components.resolveBestKnownLocation
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
import org.junit.Assert.assertEquals
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
        // Maadi Street 9 coordinate -> s1 (0.0 km), s2 (~0.5 km), s3 (~0.8 km)
        vm.updateSearchAnchor(latitude = 29.9602, longitude = 31.2569, label = "موقعي الحالي")
        vm.updateNearbyRadiusKm(0.2)

        val immediate = vm.uiState.value.filteredShops
        assertEquals(listOf("s1"), immediate.map { it.id })
        assertTrue(immediate.first().distanceKm <= 0.2)

        vm.updateNearbyRadiusKm(0.6)
        val halfKm = vm.uiState.value.filteredShops
        assertEquals(listOf("s1", "s2"), halfKm.map { it.id })

        // Clearing anchor restores all 3 static Cairo shops
        vm.clearSearchAnchor()
        assertEquals(3, vm.uiState.value.filteredShops.size)
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
}
