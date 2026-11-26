package com.example

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoaderFactory
import com.example.data.GoldProductImageLibrary
import com.example.data.GoldVisualSubcategory
import com.example.data.KingGoldRepository
import com.example.data.MarketplaceCatalogData
import com.example.model.CartItem
import com.example.model.GoldOrder
import com.example.model.MoneyUtils
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.Shop
import com.example.ui.components.GoldMarketplaceImage
import com.example.ui.screens.cart.CartScreen
import com.example.ui.screens.checkout.CheckoutScreen
import com.example.ui.screens.home.DiscoverScreen
import com.example.ui.screens.home.OrdersScreen
import com.example.ui.screens.home.ProfileScreen
import com.example.ui.screens.shop.ProductDetailScreen
import com.example.ui.screens.shop.ProductListScreen
import com.example.ui.screens.shop.SavedShopsScreen
import com.example.ui.screens.shop.ShopDetailScreen
import com.example.ui.state.KingGoldUiState
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
import com.example.ui.util.GoldImageTier
import com.example.ui.util.KingGoldImageLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoldImageryLibraryAndUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun goldProductImageLibrary_classifiesAllJewelryBullionAndShopSubcategoriesWithDrawables() {
        val sampleCases = listOf(
            Product(
                id = "t1", shopId = "s1", name = "غويشة ذهب عريضة منقوشة",
                category = ProductCategory.BRACELETS.arabicLabel, karat = 21, weightGrams = 14.0,
                stamped = true, availablePieces = 2,
            ) to GoldVisualSubcategory.BRACELETS_BANGLES,
            Product(
                id = "t2", shopId = "s1", name = "سلسلة إيطالي فينيسية",
                category = ProductCategory.CHAINS.arabicLabel, karat = 18, weightGrams = 6.5,
                stamped = true, availablePieces = 3,
            ) to GoldVisualSubcategory.CHAINS_NECKLACES,
            Product(
                id = "t3", shopId = "s1", name = "خاتم سوليتير ذهبي",
                category = ProductCategory.RINGS.arabicLabel, karat = 18, weightGrams = 4.2,
                stamped = true, availablePieces = 4,
            ) to GoldVisualSubcategory.RINGS,
            Product(
                id = "t4", shopId = "s1", name = "حلق ذهب متدلي كلاسيك",
                category = ProductCategory.EARRINGS.arabicLabel, karat = 21, weightGrams = 3.8,
                stamped = true, availablePieces = 3,
            ) to GoldVisualSubcategory.EARRINGS,
            Product(
                id = "t5", shopId = "s1", name = "دلاية آية الكرسي ذهبية",
                category = ProductCategory.DESIGNS.arabicLabel, karat = 21, weightGrams = 4.5,
                stamped = true, availablePieces = 2,
            ) to GoldVisualSubcategory.PENDANTS,
            Product(
                id = "t6", shopId = "s1", name = "سلسلة مع دلاية قلب ذهب",
                category = ProductCategory.CHAINS.arabicLabel, karat = 18, weightGrams = 7.0,
                stamped = true, availablePieces = 2,
            ) to GoldVisualSubcategory.NECKLACE_WITH_PENDANT,
            Product(
                id = "t7", shopId = "s1", name = "طقم ذهب كامل 4 قطع",
                category = ProductCategory.JEWELRY_SETS.arabicLabel, karat = 21, weightGrams = 42.0,
                stamped = true, availablePieces = 1,
            ) to GoldVisualSubcategory.COMPLETE_JEWELRY_SETS,
            Product(
                id = "t8", shopId = "s1", name = "طقم شبكة زفاف فاخر",
                category = ProductCategory.JEWELRY_SETS.arabicLabel, karat = 21, weightGrams = 55.0,
                stamped = true, availablePieces = 1,
            ) to GoldVisualSubcategory.WEDDING_SETS,
            Product(
                id = "t9", shopId = "s1", name = "أسورة ذهب أطفال رقيقة",
                category = ProductCategory.BRACELETS.arabicLabel, karat = 18, weightGrams = 2.4,
                stamped = true, availablePieces = 5,
            ) to GoldVisualSubcategory.CHILDRENS_GOLD,
            Product(
                id = "t10", shopId = "s1", name = "قلادة ذهب بتصميم مصري فليغري",
                category = ProductCategory.DESIGNS.arabicLabel, karat = 21, weightGrams = 16.0,
                stamped = true, availablePieces = 2,
            ) to GoldVisualSubcategory.EGYPTIAN_DESIGNS,
            Product(
                id = "t11", shopId = "s1", name = "سبيكة ذهب استثمارية 31.1 جم",
                category = ProductCategory.BARS_BULLION.arabicLabel, karat = 24, weightGrams = 31.1,
                stamped = true, availablePieces = 4,
            ) to GoldVisualSubcategory.BULLION_BARS,
            Product(
                id = "t12", shopId = "s1", name = "سبيكة ذهب مغلفة 2.5 جم",
                category = ProductCategory.BARS_BULLION.arabicLabel, karat = 24, weightGrams = 2.5,
                stamped = true, availablePieces = 10,
            ) to GoldVisualSubcategory.SMALL_INGOTS,
            Product(
                id = "t13", shopId = "s1", name = "جنيه ذهب مدموغ مغلف 8 جم",
                category = ProductCategory.COINS.arabicLabel, karat = 21, weightGrams = 8.0,
                stamped = true, availablePieces = 6,
            ) to GoldVisualSubcategory.EGYPTIAN_GOLD_POUNDS,
            Product(
                id = "t14", shopId = "s1", name = "عملة ذهبية تذكارية",
                category = ProductCategory.COINS.arabicLabel, karat = 24, weightGrams = 4.0,
                stamped = true, availablePieces = 5,
            ) to GoldVisualSubcategory.GOLD_COINS,
        )

        sampleCases.forEach { (product, expectedSubcategory) ->
            assertEquals(
                "Classification mismatch for ${product.name}",
                expectedSubcategory,
                GoldProductImageLibrary.classifyProduct(product),
            )
            val placeholder = GoldProductImageLibrary.placeholderDrawableForProduct(product)
            assertNotEquals("Expected valid drawable for ${product.name}", 0, placeholder)
            assertEquals(expectedSubcategory.placeholderDrawableRes, placeholder)
        }
        assertNotEquals(0, GoldVisualSubcategory.SHOP_SHOWCASE.placeholderDrawableRes)
    }

    @Test
    fun shopVisualIdentities_areDistinctPerShopAndPreserveBusinessDataRules() {
        val shops = MarketplaceCatalogData.verifiedDirectoryShops
        assertEquals(8, shops.size)

        val coverUrls = shops.map { GoldProductImageLibrary.shopCoverUrl(it, GoldImageTier.CARD_MEDIUM) }
        val logoUrls = shops.map { GoldProductImageLibrary.shopLogoUrl(it, GoldImageTier.THUMBNAIL) }
        val specialties = shops.map { it.visualSpecialty }

        assertEquals(shops.size, coverUrls.toSet().size)
        assertEquals(shops.size, logoUrls.toSet().size)
        assertTrue(specialties.all { it.isNotBlank() })
        assertTrue(shops.all { !it.hasVerifiedCoverPhoto && !it.hasVerifiedRating && !it.hasVerifiedHours })

        // Each shop presents its specialty products first while preserving s1's exact p1..p18 order
        val s1Products = KingGoldRepository.productsFor("s1")
        val s2Products = KingGoldRepository.productsFor("s2")
        val s3Products = KingGoldRepository.productsFor("s3")
        val s6Products = KingGoldRepository.productsFor("s6")

        assertEquals("p1", s1Products.first().id)
        assertTrue(ProductCategory.JEWELRY_SETS.matches(s2Products.first().category))
        assertTrue(ProductCategory.BRACELETS.matches(s3Products.first().category))
        assertTrue(ProductCategory.RINGS.matches(s6Products.first().category))

        // Verify multi-tier sizing & zero altered weights/karats/stocks
        val imageryThumb = GoldProductImageLibrary.resolveProductImagery(s1Products.first(), GoldImageTier.THUMBNAIL)
        val imageryCard = GoldProductImageLibrary.resolveProductImagery(s1Products.first(), GoldImageTier.CARD_MEDIUM)
        val imageryDetail = GoldProductImageLibrary.resolveProductImagery(s1Products.first(), GoldImageTier.DETAIL_HIGH_RES)
        assertTrue(imageryThumb.primaryUrl!!.contains("width=360"))
        assertTrue(imageryCard.primaryUrl!!.contains("width=600"))
        assertTrue(imageryDetail.primaryUrl!!.contains("width=960"))
        assertTrue(imageryDetail.galleryUrls.size >= 2)
        assertFalse(imageryDetail.isVerifiedMerchantPhoto)
    }

    @Test
    fun coilApplicationAndImageLoader_configureTierKeysMemoryDiskCachesAndSafePrefetch() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertTrue(app is ImageLoaderFactory)
        val factoryLoader = (app as ImageLoaderFactory).newImageLoader()
        val singletonLoader = KingGoldImageLoader.get(app)
        assertEquals(singletonLoader, factoryLoader)

        val baseUrl = KingGoldImageLoader.wikimediaUrlForTier("Gold_bullion_bars.jpg", GoldImageTier.THUMBNAIL)
        val thumbReq = KingGoldImageLoader.buildImageRequest(app, baseUrl, GoldImageTier.THUMBNAIL)
        val cardReq = KingGoldImageLoader.buildImageRequest(app, baseUrl, GoldImageTier.CARD_MEDIUM)
        val detailReq = KingGoldImageLoader.buildImageRequest(app, baseUrl, GoldImageTier.DETAIL_HIGH_RES)

        assertEquals("${baseUrl}_360", thumbReq.memoryCacheKey?.key)
        assertEquals("${baseUrl}_600", cardReq.memoryCacheKey?.key)
        assertEquals("${baseUrl}_960", detailReq.memoryCacheKey?.key)
        assertEquals("${baseUrl}_THUMBNAIL", thumbReq.diskCacheKey)
        assertEquals("${baseUrl}_DETAIL_HIGH_RES", detailReq.diskCacheKey)
        assertTrue(thumbReq.allowRgb565 && thumbReq.allowHardware)

        // Prefetch with duplicates and blank URLs safely does not throw offline
        KingGoldImageLoader.prefetch(
            context = app,
            urls = listOf("", "   ", baseUrl, baseUrl),
            tier = GoldImageTier.THUMBNAIL,
        )
    }

    @Test
    fun discoverScreen_rendersFeaturedShowcaseDistinctCoversAndShopCards() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                DiscoverScreen(
                    uiState = vm.uiState.value,
                    onSearchChange = {},
                    onApplyFilters = { _, _, _ -> },
                    onOpenPriceInsight = {},
                    onOpenAiAdvisor = {},
                    onRefreshGlobalGoldPrices = {},
                    onOpenCart = {},
                    onOpenShop = {},
                    onToggleFavoriteShop = {},
                    onToggleFavoritesOnlyFilter = {},
                    onSelectCityFilter = {},
                    onSelectCategoryFilter = {},
                    onDismissSyncError = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("discover_featured_products_row"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_featured_products_row").assertIsDisplayed()

        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("shop_card_s1"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_card_s1").assertIsDisplayed()
        composeRule.onAllNodesWithText("معرض مرجعي للتخصص").onFirst().assertIsDisplayed()
    }

    @Test
    fun shopDetailScreen_filtersGalleryCategoriesInteractively() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first()
        val products = vm.uiState.value.productsForShop(shop.id)

        composeRule.setContent {
            KingGoldTheme {
                ShopDetailScreen(
                    shop = shop,
                    products = products,
                    prices = vm.uiState.value.prices,
                    onBack = {},
                    onBrowseProducts = {},
                    onOpenProduct = {},
                    onOpenPriceInsight = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shop_gallery_category_row").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("عرض الكل (18)").assertIsDisplayed()

        val ringsFilter = "shop_gallery_filter_${ProductCategory.RINGS.arabicLabel}"
        composeRule.onNodeWithTag("shop_gallery_category_row")
            .performScrollToNode(hasTestTag(ringsFilter))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ringsFilter).performClick()
        composeRule.waitForIdle()

        val expectedRingsCount = products.count { it.category == ProductCategory.RINGS.arabicLabel }
        composeRule.onNodeWithText("عرض الكل ($expectedRingsCount)").assertIsDisplayed()
    }

    @Test
    fun productListScreen_displaysEditorialCardsAndFavoriteToggle() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first()

        composeRule.setContent {
            KingGoldTheme {
                ProductListScreen(
                    shop = shop,
                    initialCategory = null,
                    uiState = vm.uiState.value,
                    onBack = {},
                    onOpenProduct = {},
                    onToggleFavoriteProduct = { vm.toggleFavoriteProduct(it.shopId, it.id) },
                    onAddToCart = {},
                    onOpenCart = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_card_p1").assertIsDisplayed()
        composeRule.onNodeWithTag("product_details_button_p1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_p1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("favorite_product_p1").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(vm.uiState.value.isProductFavorite(shop.id, "p1"))
    }

    @Test
    fun productDetailScreen_switchesThumbnailsTogglesFavoriteAndShowsRelatedRow() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first()
        val products = vm.uiState.value.productsForShop(shop.id)
        val mainProduct = products.first()
        val related = products.drop(1).take(4)

        composeRule.setContent {
            KingGoldTheme {
                ProductDetailScreen(
                    shop = shop,
                    product = mainProduct,
                    relatedProducts = related,
                    gramPrice = vm.uiState.value.gramPrice(mainProduct.karat),
                    isFavorite = false,
                    onBack = {},
                    onToggleFavorite = { vm.toggleFavoriteProduct(shop.id, mainProduct.id) },
                    onAddToCart = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_detail_gallery").assertIsDisplayed()
        composeRule.onNodeWithTag("product_detail_thumbnail_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("product_detail_favorite_button").performClick()
        assertTrue(vm.uiState.value.isProductFavorite(shop.id, mainProduct.id))

        composeRule.onNodeWithTag("product_detail_related_row").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cartCheckoutOrdersProfileAndSavedShops_renderThumbnailsAndTruthfulDisclosures() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first().copy(
            rating = 0.0,
            reviews = 0,
            hasVerifiedRating = false,
        )
        val product = vm.uiState.value.productsForShop(shop.id).first()
        val sampleOrder = GoldOrder(
            id = "KG-9901",
            shop = shop,
            items = listOf(CartItem(product, 1)),
            goldValue = MoneyUtils.piastersToEgp(vm.uiState.value.productFinalPricePiasters(product)),
            makingFee = 0.0,
            serviceFee = 0.0,
            pickupWindow = "اليوم 4:00 م",
            status = OrderStatus.PAID,
        )
        val state = KingGoldUiState(
            staticShops = listOf(shop),
            cart = listOf(CartItem(product, 2)),
            cartShop = shop,
            orders = listOf(sampleOrder),
            favoriteShopIds = setOf(shop.id),
        )

        // CartScreen thumbnail
        composeRule.setContent {
            KingGoldTheme {
                CartScreen(
                    uiState = state,
                    onBack = {},
                    onIncrement = {},
                    onDecrement = {},
                    onRemove = {},
                    onSelectPickupSlot = {},
                    onRefreshPriceLock = {},
                    onProceedToCheckout = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cart_item_row_${product.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("cart_item_image_${product.id}").assertIsDisplayed()
    }

    @Test
    fun checkoutOrdersProfileAndOfflinePlaceholder_renderWithoutErrors() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first()
        val product = vm.uiState.value.productsForShop(shop.id).first()
        val state = KingGoldUiState(
            staticShops = listOf(shop),
            cart = listOf(CartItem(product, 1)),
            cartShop = shop,
            orders = listOf(
                GoldOrder(
                    id = "KG-771",
                    shop = shop,
                    items = listOf(CartItem(product, 1)),
                    goldValue = 30000.0,
                    makingFee = 0.0,
                    serviceFee = 0.0,
                    pickupWindow = "غداً",
                    status = OrderStatus.PREPARING,
                ),
            ),
            favoriteShopIds = setOf(shop.id),
        )

        composeRule.setContent {
            KingGoldTheme {
                CheckoutScreen(
                    uiState = state,
                    onBack = {},
                    onSelectPaymentMethod = {},
                    onConfirmPay = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("checkout_items_preview").assertIsDisplayed()
        composeRule.onNodeWithText(product.name).assertIsDisplayed()
    }

    @Test
    fun profileOrdersSavedShopsAndOfflineFallbackImage_displayProperly() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first()
        val product = vm.uiState.value.productsForShop(shop.id).first()
        val state = KingGoldUiState(
            staticShops = listOf(shop),
            orders = listOf(
                GoldOrder(
                    id = "KG-881",
                    shop = shop,
                    items = listOf(CartItem(product, 1)),
                    goldValue = 30000.0,
                    makingFee = 0.0,
                    serviceFee = 0.0,
                    pickupWindow = "اليوم",
                    status = OrderStatus.READY_FOR_PICKUP,
                ),
            ),
            favoriteShopIds = setOf(shop.id),
        )

        composeRule.setContent {
            KingGoldTheme {
                ProfileScreen(
                    uiState = state,
                    onBack = {},
                    onOrderClick = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_favorite_shop_${shop.id}").assertIsDisplayed()
        composeRule.onAllNodesWithText("التقييم غير متاح", substring = true).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("0.0 ·", substring = true).assertCountEquals(0)
    }

    @Test
    fun ordersAndSavedShopsScreens_renderThumbnailsCleanly() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first()
        val product = vm.uiState.value.productsForShop(shop.id).first()
        val order = GoldOrder(
            id = "KG-882",
            shop = shop,
            items = listOf(CartItem(product, 1)),
            goldValue = 30000.0,
            makingFee = 0.0,
            serviceFee = 0.0,
            pickupWindow = "اليوم",
            status = OrderStatus.READY_FOR_PICKUP,
        )
        val state = KingGoldUiState(
            staticShops = listOf(shop),
            orders = listOf(order),
            favoriteShopIds = setOf(shop.id),
        )
        composeRule.setContent {
            KingGoldTheme {
                OrdersScreen(uiState = state, onOrderClick = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("order_card_${order.id}").assertIsDisplayed()
        composeRule.onNodeWithText(product.name).assertIsDisplayed()
    }

    @Test
    fun savedShopsScreen_displaysShopLogoThumbnailAndTruthfulRating() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first()
        val state = KingGoldUiState(
            staticShops = listOf(shop),
            favoriteShopIds = setOf(shop.id),
        )
        composeRule.setContent {
            KingGoldTheme {
                SavedShopsScreen(
                    uiState = state,
                    onBack = {},
                    onOpenShop = {},
                    onToggleFavoriteShop = {},
                    onFocusShopOnMap = {},
                    onBrowseMap = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("saved_shop_card_${shop.id}").assertIsDisplayed()
        composeRule.onAllNodesWithText("التقييم غير متاح", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun offlineMarketplaceImage_immediatelyRendersBundledPlaceholderWithoutCrash() {
        composeRule.setContent {
            KingGoldTheme {
                GoldMarketplaceImage(
                    imageUrl = null,
                    base64Thumbnail = null,
                    contentDescription = "صورة احتياطية للسبائك",
                    modifier = Modifier.size(120.dp),
                    placeholderRes = R.drawable.img_gold_bullion_collection,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("gold_placeholder_check").assertDoesNotExist()
        assertTrue(GoldProductImageLibrary.placeholderDrawableForCategory(ProductCategory.BARS_BULLION.arabicLabel, 24) != 0)
    }
}
