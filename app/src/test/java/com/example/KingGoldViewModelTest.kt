package com.example

import com.example.data.KingGoldRepository
import com.example.model.OrderStatus
import com.example.model.ShopSortOption
import com.example.ui.state.KingGoldViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KingGoldViewModelTest {

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
    fun cartCalculations_and_orderPlacement_updateEscrowProperly() {
        val vm = KingGoldViewModel()
        val shop = KingGoldRepository.shops.first()
        val product = KingGoldRepository.productsFor(shop.id).first() // 8g 21k gold coin

        val initialEscrow = vm.uiState.value.escrowHeld
        vm.addToCart(shop, product)

        val stateWithItem = vm.uiState.value
        assertEquals(1, stateWithItem.cartItemCount)
        val expectedGold = 5425.0 * 8.0
        val expectedMaking = shop.makingFeePerGram * 8.0
        val expectedService = (expectedGold + expectedMaking) * 0.01
        assertEquals(expectedGold, stateWithItem.cartGoldValue, 0.01)
        assertEquals(expectedMaking, stateWithItem.cartMakingFee, 0.01)
        assertEquals(expectedService, stateWithItem.cartServiceFee, 0.01)

        val order = vm.placeOrder()
        assertNotNull(order)
        assertEquals(0, vm.uiState.value.cartItemCount)
        assertEquals(OrderStatus.READY_FOR_PICKUP, order!!.status)
        assertEquals(initialEscrow + order.total, vm.uiState.value.escrowHeld, 0.01)

        vm.completePickup(order.id)
        assertEquals(initialEscrow, vm.uiState.value.escrowHeld, 0.01)
        assertEquals(OrderStatus.COMPLETED, vm.findOrder(order.id)?.status)
    }

    @Test
    fun addingProductFromDifferentShop_replacesExistingCart() {
        val vm = KingGoldViewModel()
        val shop1 = KingGoldRepository.shops[0]
        val shop2 = KingGoldRepository.shops[1]
        val p1 = KingGoldRepository.productsFor(shop1.id)[0]
        val p2 = KingGoldRepository.productsFor(shop2.id)[1]

        vm.addToCart(shop1, p1)
        assertEquals(shop1.id, vm.uiState.value.cartShop?.id)

        vm.addToCart(shop2, p2)
        assertEquals(shop2.id, vm.uiState.value.cartShop?.id)
        assertEquals(1, vm.uiState.value.cart.size)
        assertEquals(p2.id, vm.uiState.value.cart.first().product.id)
    }

    @Test
    fun searchAndSorting_filtersShopsCorrectly() {
        val vm = KingGoldViewModel()
        vm.updateSearchQuery("النور")
        assertEquals(1, vm.uiState.value.filteredShops.size)
        assertEquals("s1", vm.uiState.value.filteredShops.first().id)

        vm.updateSearchQuery("")
        vm.updateFilters(karat = 24, verifiedOnly = true, sortOption = ShopSortOption.HIGHEST_RATED)
        assertTrue(vm.uiState.value.filteredShops.isNotEmpty())
        assertEquals("s1", vm.uiState.value.filteredShops.first().id)
    }

    @Test
    fun favoriteShopToggle_and_favoritesFilter_updateFilteredShops() {
        val vm = KingGoldViewModel()
        assertTrue(vm.uiState.value.favoriteShopIds.isEmpty())

        vm.toggleFavoriteShop("s2")
        assertTrue(vm.uiState.value.favoriteShopIds.contains("s2"))

        vm.toggleFavoritesOnlyFilter(true)
        assertEquals(1, vm.uiState.value.filteredShops.size)
        assertEquals("s2", vm.uiState.value.filteredShops.first().id)

        vm.toggleFavoriteShop("s2")
        assertTrue(vm.uiState.value.favoriteShopIds.isEmpty())
        assertTrue(vm.uiState.value.filteredShops.isEmpty())
    }

    @Test
    fun history24K7Days_returnsSevenPointsAnchoredToCurrent24KPrice() {
        val vm = KingGoldViewModel()
        val history = vm.uiState.value.history24K7Days

        assertEquals(7, history.size)
        assertEquals("اليوم", history.last().dayLabel)
        assertEquals(vm.uiState.value.gramPrice(24), history.last().pricePerGram, 0.5)
        assertTrue(history.all { it.pricePerGram > 0.0 })
    }

    @Test
    fun priceDropAlert_triggersCallbackOnlyWhen24KIsBelowThreshold() {
        val triggeredDrops = mutableListOf<Pair<Double, Double>>()
        val vm = KingGoldViewModel(
            on24KPriceDroppedBelowThreshold = { current24K, threshold ->
                triggeredDrops.add(current24K to threshold)
            },
        )

        // Default 24K is 6200.0; threshold 6000 is above? No, 6200 >= 6000 so should NOT trigger.
        vm.updatePriceAlert(enabled = true, above = false, karat = 24, target = "6000")
        assertTrue(triggeredDrops.isEmpty())

        // Setting threshold to 6500 when 24K is 6200 means 24K is below 6500 -> triggers once.
        vm.updatePriceAlert(enabled = true, above = false, karat = 24, target = "6500")
        assertEquals(1, triggeredDrops.size)
        assertEquals(6200.0, triggeredDrops.first().first, 0.01)
        assertEquals(6500.0, triggeredDrops.first().second, 0.01)

        // Above-mode alert should not trigger a drop-below notification.
        vm.updatePriceAlert(enabled = true, above = true, karat = 24, target = "6500")
        assertEquals(1, triggeredDrops.size)
    }
}
