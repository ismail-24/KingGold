package com.example

import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.example.data.KingGoldRepository
import com.example.data.MarketplaceCatalogData
import com.example.model.ProductCategory
import com.example.ui.navigation.KingGoldNavHost
import com.example.ui.navigation.Routes
import com.example.ui.screens.home.DiscoverScreen
import com.example.ui.screens.shop.ProductDetailScreen
import com.example.ui.screens.shop.ProductListScreen
import com.example.ui.screens.shop.ShopDetailScreen
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
import com.example.ui.util.KingGoldImageLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MarketplaceAuditAndCatalogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun resolveWorkspaceFile(vararg candidatePaths: String): File =
        candidatePaths.map(::File).firstOrNull { it.exists() }
            ?: error("Could not locate any of: ${candidatePaths.joinToString()}")

    @Test
    fun firebaseConfigAudit_canonicalProjectAndDatabaseIdAreConsistentAcrossRepo() {
        val googleServices = resolveWorkspaceFile(
            "google-services.json",
            "app/google-services.json",
        ).readText()
        val firebaseXml = resolveWorkspaceFile(
            "src/main/res/values/firebase_applet_config.xml",
            "app/src/main/res/values/firebase_applet_config.xml",
        ).readText()
        val firebaseJson = resolveWorkspaceFile(
            "../firebase.json",
            "firebase.json",
        ).readText()
        val emulatorTestBase = resolveWorkspaceFile(
            "src/test/java/com/example/base/FirestoreEmulatorTestBase.kt",
            "app/src/test/java/com/example/base/FirestoreEmulatorTestBase.kt",
        ).readText()

        val canonicalProjectId = "gen-lang-client-0717075354"
        val canonicalProjectNumber = "855620265252"
        val canonicalDbId = "ai-studio-android-kinggold-bc58cad2-94b8-4b97-b49c-a2df279a1209"
        val canonicalAppId = "1:855620265252:android:dfe6335f4b9651c3bcbfb1"
        val canonicalPkg = "com.aistudio.kinggold.k9m4vx"
        val canonicalRecaptchaKey = "6LcjlCIuAAAAAGvDtGC40Q3tqcB4UYCNDjdwinFO"

        assertTrue(googleServices.contains(canonicalProjectId))
        assertTrue(googleServices.contains(canonicalProjectNumber))
        assertTrue(googleServices.contains(canonicalAppId))
        assertTrue(googleServices.contains(canonicalPkg))
        assertTrue(firebaseXml.contains(canonicalDbId))
        assertTrue(firebaseXml.contains(canonicalRecaptchaKey))
        assertTrue(firebaseJson.contains(canonicalDbId))
        assertTrue(emulatorTestBase.contains(canonicalDbId))
        assertFalse(emulatorTestBase.contains("898055f9-af3c-4188-bfda-068632d66744"))
    }

    @Test
    fun catalogAudit_all8CategoriesCoveredWithUniqueVerifiedPhotosAndNoFabricatedRatingsOrHours() {
        val shops = MarketplaceCatalogData.verifiedDirectoryShops
        assertEquals(8, shops.size)
        assertTrue(shops.map { it.city }.toSet().containsAll(setOf("القاهرة", "الجيزة", "الإسكندرية")))
        assertTrue(shops.all { it.name.isNotBlank() && it.address.isNotBlank() && it.verified })
        // Unverified crowd ratings & opening hours must stay marked unavailable rather than invented
        assertTrue(shops.all { !it.hasVerifiedRating && it.rating == 0.0 && it.reviews == 0 })
        assertTrue(shops.all { !it.hasVerifiedHours && it.closesAt.isBlank() && it.workingHours.isBlank() })
        assertTrue(shops.all { it.hasVerifiedPhone && !it.websiteUrl.isNullOrBlank() })

        val products = KingGoldRepository.productsFor("s1")
        assertEquals(18, products.size)

        // All 8 ProductCategory entries must be covered
        val missingCategories = ProductCategory.entries.filter { cat ->
            products.none { product -> cat.matches(product.category) }
        }
        assertTrue("Missing categories: $missingCategories", missingCategories.isEmpty())

        // Every product must have distinct Wikimedia URLs (360px thumb, 960px detail) and honest stock disclosure
        val thumbs = products.mapNotNull { it.thumbnailUrl }
        val details = products.mapNotNull { it.imageUrl }
        assertEquals(products.size, thumbs.size)
        assertEquals(products.size, details.size)
        assertEquals(products.size, thumbs.toSet().size)
        assertEquals(products.size, details.toSet().size)
        assertTrue(thumbs.all { it.startsWith("https://commons.wikimedia.org/") && it.contains("width=360") })
        assertTrue(details.all { it.startsWith("https://commons.wikimedia.org/") && it.contains("width=960") })
        assertTrue(products.all { !it.isPhotoVerifiedInventory })
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    @Test
    fun imageLoader_configuresBoundedMemoryAndLocalDiskCaches() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val loader = KingGoldImageLoader.get(app)
        assertNotNull(loader.memoryCache)
        assertNotNull(loader.diskCache)
        assertTrue(loader.diskCache!!.directory.toString().contains("kinggold_image_cache"))
        assertEquals(loader, KingGoldImageLoader.get(app))
    }

    @Test
    fun viewModel_cityAndCategoryFiltersWorkAccurately() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        assertEquals(listOf("القاهرة", "الجيزة", "الإسكندرية"), vm.uiState.value.availableCities)

        vm.updateCityFilter("الإسكندرية")
        assertEquals(listOf("s7"), vm.uiState.value.filteredShops.map { it.id })

        vm.updateCityFilter("الجيزة")
        assertEquals(listOf("s6"), vm.uiState.value.filteredShops.map { it.id })

        vm.updateCityFilter(null)
        vm.updateCategoryFilter(ProductCategory.JEWELRY_SETS.arabicLabel)
        assertEquals(8, vm.uiState.value.filteredShops.size)

        vm.updateCityFilter("القاهرة")
        assertEquals(6, vm.uiState.value.filteredShops.size)
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
    fun discoverScreen_displaysCityCategoryFiltersAndTruthfulDisclosures() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                DiscoverScreen(
                    uiState = vm.uiState.value,
                    onSearchChange = vm::updateSearchQuery,
                    onApplyFilters = vm::updateFilters,
                    onOpenPriceInsight = {},
                    onOpenAiAdvisor = {},
                    onRefreshGlobalGoldPrices = {},
                    onOpenCart = {},
                    onOpenShop = {},
                    onToggleFavoriteShop = vm::toggleFavoriteShop,
                    onToggleFavoritesOnlyFilter = vm::toggleFavoritesOnlyFilter,
                    onSelectCityFilter = vm::updateCityFilter,
                    onSelectCategoryFilter = vm::updateCategoryFilter,
                    onDismissSyncError = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("discover_city_filter_row"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("city_filter_الإسكندرية").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("shop_card_s1"))
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("التقييم غير متاح").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("ساعات العمل غير متوفرة").onFirst().assertIsDisplayed()
    }

    @Test
    fun productListScreen_paginates18ProductsIncrementally() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val firstShop = vm.uiState.value.shops.first()
        composeRule.setContent {
            KingGoldTheme {
                ProductListScreen(
                    shop = firstShop,
                    initialCategory = null,
                    uiState = vm.uiState.value,
                    onBack = {},
                    onOpenProduct = {},
                    onAddToCart = {},
                    onOpenCart = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("18 قطعة متاحة · عرض 8").assertIsDisplayed()
        composeRule.onNodeWithTag("product_card_p1").assertIsDisplayed()
        composeRule.onNodeWithTag("product_card_p16").assertDoesNotExist()

        composeRule.onNodeWithTag("product_list_lazy_column")
            .performScrollToNode(hasTestTag("product_card_p16"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_card_p16").assertIsDisplayed()
        composeRule.onNodeWithTag("product_list_lazy_column")
            .performScrollToNode(hasTestTag("product_card_p18"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_card_p18").assertIsDisplayed()
        composeRule.onNodeWithText("18 قطعة متاحة · عرض 18").assertIsDisplayed()
    }

    @Test
    fun shopDetailAndProductDetail_displayTruthfulPhotoDisclosures() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val firstShop = vm.uiState.value.shops.first()
        val firstProducts = vm.uiState.value.productsForShop(firstShop.id)
        val firstProduct = firstProducts.first()

        composeRule.setContent {
            KingGoldTheme {
                ShopDetailScreen(
                    shop = firstShop,
                    products = firstProducts,
                    prices = vm.uiState.value.prices,
                    onBack = {},
                    onBrowseProducts = {},
                    onOpenProduct = {},
                    onOpenPriceInsight = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_hero_photo_disclaimer").assertIsDisplayed()
        composeRule.onNodeWithTag("shop_website_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("shop_catalog_photo_disclosure")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("shop_collection_gallery")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(firstProduct.name)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun productDetailScreen_showsInventoryPhotoAttributionDisclosure() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val firstShop = vm.uiState.value.shops.first()
        val firstProduct = vm.uiState.value.productsForShop(firstShop.id).first()

        composeRule.setContent {
            KingGoldTheme {
                ProductDetailScreen(
                    shop = firstShop,
                    product = firstProduct,
                    gramPrice = vm.uiState.value.gramPrice(firstProduct.karat),
                    onBack = {},
                    onAddToCart = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_photo_disclosure").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("أضف للسلة").assertIsDisplayed()
    }

    @Test
    fun nearbyMapMainTab_displaysInteractiveOsmdroidMapCityFilterAndShopNavigation() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        lateinit var controller: NavHostController
        assertTrue(Routes.isSupportedInternalRoute(Routes.NEARBY_MAP))

        composeRule.setContent {
            KingGoldTheme {
                controller = rememberNavController()
                KingGoldNavHost(
                    viewModel = vm,
                    navController = controller,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Navigate via main Map tab
        composeRule.onNodeWithTag("tab_map").performClick()
        composeRule.waitForIdle()
        assertEquals(Routes.NEARBY_MAP, controller.currentDestination?.route)

        composeRule.onNodeWithTag("nearby_map_main_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("map_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("map_karat_filter_row").assertIsDisplayed()
        composeRule.onNodeWithTag("osm_map_view").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("map_selected_shop_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("map_selected_directions_button").performScrollTo().assertIsDisplayed()

        val rootView = composeRule.activity.window.decorView.rootView
        val mapView = findOsmMapView(rootView)
        assertNotNull(mapView)
        assertEquals(8, mapView!!.overlays.filterIsInstance<Marker>().size)

        // Search by shop name in top Map search bar -> narrows markers to L'azurde branches (s2, s6)
        composeRule.onNodeWithTag("map_search_input").performTextInput("لازوردي")
        composeRule.waitForIdle()
        assertEquals(setOf("s2", "s6"), mapView.overlays.filterIsInstance<Marker>().map { it.id }.toSet())

        composeRule.onNodeWithTag("map_search_input").performTextClearance()
        composeRule.waitForIdle()
        assertEquals(8, mapView.overlays.filterIsInstance<Marker>().size)

        // Filter by Alexandria on the main map screen -> narrows markers to s7 only
        composeRule.onNodeWithTag("map_city_filter_الإسكندرية").performScrollTo().performClick()
        composeRule.waitForIdle()
        val alexMarkers = mapView.overlays.filterIsInstance<Marker>()
        assertEquals(listOf("s7"), alexMarkers.map { it.id })

        // Open selected shop directly from the map preview card -> navigates to ShopDetailScreen
        composeRule.onNodeWithTag("map_open_shop_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(controller.currentDestination?.route?.startsWith("shop_detail/") == true)

        // Back from ShopDetail -> returns to Nearby Map main screen; Back again -> returns to Discover without exiting
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertEquals(Routes.NEARBY_MAP, controller.currentDestination?.route)

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        assertEquals(Routes.HOME, controller.currentDestination?.route)
    }
}
