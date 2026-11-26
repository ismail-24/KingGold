package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.data.DataStoreSearchHistoryStore
import com.example.data.InMemorySearchHistoryStore
import com.example.data.KingGoldRepository
import com.example.data.SearchHistoryHelper
import com.example.model.ProductCategory
import com.example.model.ShopSortOption
import com.example.ui.screens.home.DiscoverScreen
import com.example.ui.screens.home.NearbyShopsMapScreen
import com.example.ui.screens.shop.ProductListScreen
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchHistoryTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun searchHistoryHelper_deduplicatesMruCapsLengthAndEncodesLosslessly() {
        var list = emptyList<String>()
        list = SearchHistoryHelper.addQuery(list, "  ا  ") // Too short (<2 chars)
        assertTrue(list.isEmpty())

        list = SearchHistoryHelper.addQuery(list, "سبيكة   ذهب")
        list = SearchHistoryHelper.addQuery(list, "خاتم عيار 21")
        list = SearchHistoryHelper.addQuery(list, "سبيكة ذهب") // Duplicate collapsed -> promoted to MRU index 0
        assertEquals(listOf("سبيكة ذهب", "خاتم عيار 21"), list)

        // Cap at MAX_HISTORY_ITEMS (10)
        for (i in 1..12) {
            list = SearchHistoryHelper.addQuery(list, "بحث رقم $i")
        }
        assertEquals(SearchHistoryHelper.MAX_HISTORY_ITEMS, list.size)
        assertEquals("بحث رقم 12", list.first())

        val special = SearchHistoryHelper.addQuery(listOf("عيار\\24"), "غويشة")
        val encoded = SearchHistoryHelper.encodeHistory(special)
        val decoded = SearchHistoryHelper.decodeHistory(encoded)
        assertEquals(special, decoded)

        val afterRemove = SearchHistoryHelper.removeQuery(decoded, "غويشة")
        assertEquals(listOf("عيار\\24"), afterRemove)
    }

    @Test
    fun searchHistoryHelper_buildSuggestions_filtersRecentCategoriesAndCatalogHints() {
        val history = listOf("سبيكة سويسرية", "خاتم سوليتير", "جنيه ذهب")
        val hints = listOf("سبيكة BTC 10 جم", "حلق ذهب إيطالي", "محل ذهب لازوردي")

        val blankPayload = SearchHistoryHelper.buildSuggestions(
            query = "",
            history = history,
            catalogHints = hints,
        )
        assertEquals(history, blankPayload.recentSearches)
        assertEquals(ProductCategory.entries, blankPayload.categorySuggestions)
        assertTrue(blankPayload.matchingItems.isEmpty())

        val bullionPayload = SearchHistoryHelper.buildSuggestions(
            query = "سبيك",
            history = history,
            catalogHints = hints,
        )
        assertEquals(listOf("سبيكة سويسرية"), bullionPayload.recentSearches)
        assertTrue(bullionPayload.categorySuggestions.contains(ProductCategory.BARS_BULLION))
        assertTrue(bullionPayload.matchingItems.contains("سبيكة BTC 10 جم"))

        // Legacy alias matching ("شبكة" -> JEWELRY_SETS)
        val aliasPayload = SearchHistoryHelper.buildSuggestions(
            query = "شبكة",
            history = history,
            catalogHints = hints,
        )
        assertTrue(aliasPayload.categorySuggestions.contains(ProductCategory.JEWELRY_SETS))
    }

    @Test
    fun dataStoreSearchHistoryStore_persistsAcrossInstancesAndClear() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storeA = DataStoreSearchHistoryStore(context)
        storeA.clearHistory()

        storeA.recordQuery("سبيكة 5 جم")
        storeA.recordQuery("جنيه ذهب")
        storeA.recordQuery("سبيكة 5 جم")

        val storeB = DataStoreSearchHistoryStore(context)
        assertEquals(listOf("سبيكة 5 جم", "جنيه ذهب"), storeB.readHistory())

        storeB.removeQuery("جنيه ذهب")
        assertEquals(listOf("سبيكة 5 جم"), storeB.readHistory())

        storeB.clearHistory()
        assertTrue(storeB.readHistory().isEmpty())
    }

    @Test
    fun discoverScreen_focusSearchBar_displaysRecentItemsAndCategories_andAppliesSelection() {
        val store = InMemorySearchHistoryStore(listOf("سبيكة 10 جم", "خاتم عيار 21"))
        val vm = KingGoldViewModel(autoFetchPrices = false, searchHistoryStore = store)

        composeRule.setContent {
            KingGoldTheme {
                val state by vm.uiState.collectAsStateWithLifecycle()
                DiscoverScreen(
                    uiState = state,
                    onSearchChange = vm::updateSearchQuery,
                    onApplyFilters = vm::updateFilters,
                    onOpenPriceInsight = {},
                    onOpenAiAdvisor = {},
                    onRefreshGlobalGoldPrices = {},
                    onOpenCart = {},
                    onOpenShop = {},
                    onToggleFavoriteShop = {},
                    onToggleFavoritesOnlyFilter = vm::toggleFavoritesOnlyFilter,
                    onSelectCityFilter = vm::updateCityFilter,
                    onSelectCategoryFilter = vm::updateCategoryFilter,
                    onDismissSyncError = {},
                    onSubmitSearch = vm::recordSearchHistory,
                    onRemoveSearchHistoryItem = vm::removeSearchHistoryItem,
                    onClearSearchHistory = vm::clearSearchHistory,
                )
            }
        }
        composeRule.waitForIdle()

        // Focus search bar -> suggestions card appears with history and gold categories
        composeRule.onNodeWithTag("discover_search_input").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("discover_search_suggestions_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_search_history_item_0").assertIsDisplayed()
        composeRule.onAllNodesWithText("خاتم عيار 21").onFirst().assertIsDisplayed()

        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("discover_search_category_suggestions_row"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_search_category_suggestion_coins").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("discover_search_category_suggestion_bars_bullion").performScrollTo().assertIsDisplayed()

        // Delete top recent item ("خاتم عيار 21")
        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("discover_search_history_item_0"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_search_history_remove_0").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("سبيكة 10 جم"), vm.uiState.value.searchHistory)

        // Tap gold product category suggestion ("سبائك ذهبية")
        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("discover_search_category_suggestions_row"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_search_category_suggestion_bars_bullion").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ProductCategory.BARS_BULLION.arabicLabel, vm.uiState.value.selectedCategoryFilter)
        assertTrue(vm.uiState.value.searchHistory.first() == ProductCategory.BARS_BULLION.arabicLabel)

        // Focus again, type a query and press IME Search -> records query in history
        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("discover_search_input"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_search_input").performClick()
        composeRule.onNodeWithTag("discover_search_input").performTextInput("لازوردي")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_search_input").performImeAction()
        composeRule.waitForIdle()
        assertEquals("لازوردي", vm.uiState.value.searchQuery)
        assertEquals("لازوردي", vm.uiState.value.searchHistory.first())

        // Focus again and clear all history
        composeRule.onNodeWithTag("discover_search_input").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_lazy_column")
            .performScrollToNode(hasTestTag("discover_search_clear_all"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_search_clear_all").performClick()
        composeRule.waitForIdle()
        assertTrue(vm.uiState.value.searchHistory.isEmpty())
    }

    @Test
    fun productListScreen_searchSuggestions_appliesRecentQueryAndCategory() {
        val store = InMemorySearchHistoryStore(listOf("جنيه ذهب"))
        val vm = KingGoldViewModel(autoFetchPrices = false, searchHistoryStore = store)
        val shop = KingGoldRepository.shops.first()

        composeRule.setContent {
            KingGoldTheme {
                val state by vm.uiState.collectAsStateWithLifecycle()
                ProductListScreen(
                    shop = shop,
                    initialCategory = null,
                    uiState = state,
                    onBack = {},
                    onOpenProduct = {},
                    onAddToCart = {},
                    onOpenCart = {},
                    onSubmitSearch = vm::recordSearchHistory,
                    onRemoveSearchHistoryItem = vm::removeSearchHistoryItem,
                    onClearSearchHistory = vm::clearSearchHistory,
                )
            }
        }
        composeRule.waitForIdle()

        // Focus product search bar
        composeRule.onNodeWithTag("product_list_search_input").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("product_search_suggestions_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("product_search_history_item_0").assertIsDisplayed()
        composeRule.onNodeWithTag("product_search_category_suggestion_rings").performScrollTo().assertIsDisplayed()

        // Click recent search item ("جنيه ذهب")
        composeRule.onNodeWithTag("product_search_history_item_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_list_search_input").assertTextContains("جنيه ذهب")

        // Focus again and select gold category "خواتم"
        composeRule.onNodeWithTag("product_list_search_clear").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_list_search_input").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("product_search_category_suggestion_rings").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(vm.uiState.value.searchHistory.contains(ProductCategory.RINGS.arabicLabel))
    }

    @Test
    fun nearbyShopsMapScreen_historyButton_togglesSuggestionsWithoutBreakingMapFilters() {
        val store = InMemorySearchHistoryStore(listOf("الماسة"))
        val vm = KingGoldViewModel(autoFetchPrices = false, searchHistoryStore = store)

        composeRule.setContent {
            KingGoldTheme {
                val state by vm.uiState.collectAsStateWithLifecycle()
                NearbyShopsMapScreen(
                    uiState = state,
                    onSearchChange = vm::updateSearchQuery,
                    onApplyFilters = vm::updateFilters,
                    onSelectCityFilter = vm::updateCityFilter,
                    onToggleFavoritesOnlyFilter = vm::toggleFavoritesOnlyFilter,
                    onOpenCart = {},
                    onOpenShop = {},
                    onToggleFavoriteShop = {},
                    onUpdateSearchAnchor = vm::updateSearchAnchor,
                    onUpdateNearbyRadius = vm::updateNearbyRadiusKm,
                    onClearSearchAnchor = vm::clearSearchAnchor,
                    onSubmitSearch = vm::recordSearchHistory,
                    onRemoveSearchHistoryItem = vm::removeSearchHistoryItem,
                    onClearSearchHistory = vm::clearSearchHistory,
                    onSelectCategoryFilter = vm::updateCategoryFilter,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("map_search_history_button").assertIsDisplayed()
        composeRule.onNodeWithTag("map_search_history_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("map_search_suggestions_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("map_search_history_item_0").performClick()
        composeRule.waitForIdle()

        assertEquals("الماسة", vm.uiState.value.searchQuery)
        assertFalse(vm.uiState.value.searchHistory.isEmpty())
    }
}
