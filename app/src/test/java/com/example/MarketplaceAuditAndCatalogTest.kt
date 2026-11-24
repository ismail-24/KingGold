package com.example

import android.app.Application
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
import androidx.test.core.app.ApplicationProvider
import com.example.data.KingGoldRepository
import com.example.data.MarketplaceCatalogData
import com.example.model.ProductCategory
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

    @Test
    fun discoverAndShopAndProductScreens_displayTruthfulInfoDisclosuresAndPagination() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val firstShop = vm.uiState.value.shops.first()
        val firstProducts = vm.uiState.value.productsForShop(firstShop.id)
        val firstProduct = firstProducts.first()

        // 1) DiscoverScreen displays city filters, category filters, and truthful unavailable labels
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
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("discover_city_filter_row"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("city_filter_الإسكندرية").assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("shop_card_s1"))
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("التقييم غير متاح").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("ساعات العمل غير متوفرة").onFirst().assertIsDisplayed()

        // 2) ProductListScreen paginates 18 products incrementally (shows 8 first + load more button)
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
        composeRule.onNodeWithTag("product_card_p1").assertIsDisplayed()
        composeRule.onNodeWithTag("product_list_lazy_column")
            .performScrollToNode(hasTestTag("product_list_load_more_button"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_list_load_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_list_lazy_column")
            .performScrollToNode(hasTestTag("product_card_p16"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_card_p16").assertIsDisplayed()

        // 3) ShopDetailScreen shows catalog photo disclaimer & website action
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
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("shop_website_button"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_website_button").assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("shop_catalog_photo_disclosure"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_catalog_photo_disclosure").assertIsDisplayed()

        // 4) ProductDetailScreen shows inventory photo attribution disclosure
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
}
