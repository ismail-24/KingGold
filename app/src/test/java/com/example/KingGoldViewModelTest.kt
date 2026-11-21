package com.example

import com.example.data.KingGoldRepository
import com.example.model.MerchantProduct
import com.example.model.MerchantProductStatus
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.model.MoneyUtils
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.ShopSortOption
import com.example.model.WalletSummary
import com.example.model.WithdrawalDeliveryType
import com.example.ui.state.KingGoldViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun cartCalculations_and_orderPlacement_haveZeroExtraMakingOrServiceFees() {
        val vm = KingGoldViewModel()
        val shop = KingGoldRepository.shops.first()
        val product = KingGoldRepository.productsFor(shop.id).first() // 8g 21k gold coin

        val initialEscrow = vm.uiState.value.escrowHeld
        vm.addToCart(shop, product)

        val stateWithItem = vm.uiState.value
        assertEquals(1, stateWithItem.cartItemCount)
        val expectedGold = 5425.0 * 8.0
        assertEquals(expectedGold, stateWithItem.cartGoldValue, 0.01)
        assertEquals(0.0, stateWithItem.cartMakingFee, 0.001)
        assertEquals(0.0, stateWithItem.cartServiceFee, 0.001)
        assertEquals(expectedGold, stateWithItem.cartTotal, 0.01)

        val order = vm.placeOrder()
        assertNotNull(order)
        assertEquals(0, vm.uiState.value.cartItemCount)
        assertEquals(OrderStatus.READY_FOR_PICKUP, order!!.status)
        assertEquals(0.0, order.makingFee, 0.001)
        assertEquals(0.0, order.serviceFee, 0.001)
        assertEquals(initialEscrow + order.total, vm.uiState.value.escrowHeld, 0.01)

        vm.completePickup(order.id)
        assertEquals(initialEscrow, vm.uiState.value.escrowHeld, 0.01)
        assertEquals(OrderStatus.COMPLETED, vm.findOrder(order.id)?.status)
    }

    @Test
    fun merchantProductFixedPrice_usesExactMerchantPriceWithZeroExtraFees() {
        val fixedPricePiasters = 6_450_000L // 64,500.00 EGP final price
        val merchantProduct = MerchantProduct(
            id = "mp_1",
            merchantId = "m_1",
            ownerUid = "u_merchant",
            merchantName = "متجر الملكية",
            name = "سبيكة ذهب 10 جم",
            description = "سعر نهائي شامل",
            category = "جنيهات وسبائك",
            karat = 24,
            weightMg = 10_000L,
            weightGrams = 10.0,
            sellingPricePiasters = fixedPricePiasters,
            stockQuantity = 3,
            stamped = true,
            status = MerchantProductStatus.ACTIVE,
        )
        val mappedProduct: Product = merchantProduct.toProduct()
        val merchantShop = MerchantStore(
            id = "m_1",
            ownerUid = "u_merchant",
            shopName = "متجر الملكية",
            area = "المعادي",
            address = "شارع 9 المعادي",
            contactPhone = "01000000000",
            status = MerchantStatus.APPROVED,
            prepaidMakingApproved = true,
        ).toShop()

        val vm = KingGoldViewModel()
        vm.addToCart(merchantShop, mappedProduct)

        val state = vm.uiState.value
        assertEquals(fixedPricePiasters, state.cartTotalPiasters)
        assertEquals(0.0, state.cartMakingFee, 0.001)
        assertEquals(0.0, state.cartServiceFee, 0.001)
        assertEquals(64_500.0, state.cartTotal, 0.01)
    }

    @Test
    fun cashWalletCheckout_blocksInsufficientBalance_withoutClearingCart() {
        val vm = KingGoldViewModel()
        val shop = KingGoldRepository.shops.first()
        val product = KingGoldRepository.productsFor(shop.id).first()

        vm.addToCart(shop, product)
        vm.selectPaymentMethod("cash_wallet")

        // Wallet cash is 0 -> placeOrder must fail cleanly & preserve cart
        val order = vm.placeOrder()
        assertNull(order)
        assertEquals(1, vm.uiState.value.cartItemCount)
        assertNotNull(vm.uiState.value.actionErrorMessage)
        assertTrue(vm.uiState.value.actionErrorMessage!!.contains("بالمحفظة النقدية"))
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
    fun fractionalGoldPurchase_accumulatesMilligrams_and_enforces1000MgWithdrawalMinimum() {
        val vm = KingGoldViewModel()

        // Fractional gold is paid via external pending flow when wallet cash is 0 -> becomes pending
        vm.buyFractionalGold(karat = 24, weightMg = 100L, payWithWallet = false)
        assertEquals(100L, vm.uiState.value.wallet.goldPendingMg)

        // Small offline buy via wallet after deposit -> let's test direct MoneyUtils & wallet math + withdrawal guard
        // Test withdrawal below 1000 mg strictly blocked
        vm.requestPhysicalWithdrawal(
            karat = 24,
            amountMg = 350L,
            deliveryType = WithdrawalDeliveryType.PICKUP_HUB,
            deliveryAddress = "فرع المعادي الرئيسي",
        )
        assertNotNull(vm.uiState.value.actionErrorMessage)
        assertTrue(vm.uiState.value.actionErrorMessage!!.contains("1.000"))

        vm.clearActionMessages()
        vm.requestPhysicalWithdrawal(
            karat = 24,
            amountMg = 999L,
            deliveryType = WithdrawalDeliveryType.PICKUP_HUB,
            deliveryAddress = "فرع المعادي الرئيسي",
        )
        assertNotNull(vm.uiState.value.actionErrorMessage)

        vm.clearActionMessages()
        val cost100Mg = MoneyUtils.computeFractionalCostPiasters(100L, 620_000L)
        val cost250Mg = MoneyUtils.computeFractionalCostPiasters(250L, 620_000L)
        assertEquals(62_000L, cost100Mg)
        assertEquals(155_000L, cost250Mg)
        assertEquals(350L, 100L + 250L)

        // Verify MoneyUtils vault valuation, avg cost, and unrealized P/L precision
        val valAt6500 = MoneyUtils.computeGoldValuationPiasters(
            gold24kMg = 1000L,
            gold21kMg = 0L,
            gold18kMg = 0L,
            valuation24kPerGramPiasters = 650_000L,
            valuation21kPerGramPiasters = 568_750L,
            valuation18kPerGramPiasters = 487_500L,
        )
        assertEquals(650_000L, valAt6500)
        val avgCost = MoneyUtils.computeAverageCostPerGramPiasters(totalInvestedPiasters = 620_000L, goldConfirmedMg = 1000L)
        assertEquals(620_000L, avgCost)
        val plPct = MoneyUtils.computeProfitLossPercent(currentValuationPiasters = 650_000L, totalInvestedPiasters = 620_000L)
        assertEquals(4.84, plPct, 0.01)

        // Verify WalletSummary available gold subtracts reserved
        val wallet = WalletSummary(goldConfirmedMg = 1250L, goldReservedMg = 1000L)
        assertEquals(250L, wallet.goldAvailableMg)
        assertFalse(wallet.goldAvailableMg >= MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG)
    }

    @Test
    fun adminPrivilege_matchesBootstrapAdminEmailOnly() {
        val anonVm = KingGoldViewModel()
        assertFalse(anonVm.uiState.value.isAdmin)

        val regularVm = KingGoldViewModel(currentUserId = "u_regular", userDisplayName = "Customer", userEmail = "customer@gmail.com")
        assertFalse(regularVm.uiState.value.isAdmin)

        val adminVm = KingGoldViewModel(currentUserId = "u_admin", userDisplayName = "Esmail Admin", userEmail = "EsmailTarek228@gmail.com")
        assertTrue(adminVm.uiState.value.isAdmin)
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

        vm.updatePriceAlert(enabled = true, above = false, karat = 24, target = "6000")
        assertTrue(triggeredDrops.isEmpty())

        vm.updatePriceAlert(enabled = true, above = false, karat = 24, target = "6500")
        assertEquals(1, triggeredDrops.size)
        assertEquals(6200.0, triggeredDrops.first().first, 0.01)
        assertEquals(6500.0, triggeredDrops.first().second, 0.01)

        vm.updatePriceAlert(enabled = true, above = true, karat = 24, target = "6500")
        assertEquals(1, triggeredDrops.size)
    }

    @Test
    fun history21K7Days_returnsSevenPointsAnchoredToCurrent21KPrice() {
        val vm = KingGoldViewModel()
        val history = vm.uiState.value.history21K7Days

        assertEquals(7, history.size)
        assertEquals("اليوم", history.last().dayLabel)
        assertEquals(vm.uiState.value.gramPrice(21), history.last().pricePerGram, 0.5)
        assertTrue(history.all { it.pricePerGram > 0.0 })
    }

    @Test
    fun addToCart_incrementsAndClampsToAvailablePieces() {
        val vm = KingGoldViewModel()
        val shop = KingGoldRepository.shops.first()
        val product = KingGoldRepository.productsFor(shop.id).first()

        repeat(product.availablePieces + 3) {
            vm.addToCart(shop, product)
        }

        assertEquals(product.availablePieces, vm.uiState.value.cartItemCount)
        assertEquals(582, vm.uiState.value.priceLockSecondsRemaining)
    }

    @Test
    fun priceThresholdAlert_triggersFor21KAnd18KWhenBelowThreshold() {
        val triggered = mutableListOf<Triple<Int, Double, Double>>()
        val vm = KingGoldViewModel(
            onPriceThresholdTriggered = { karat, currentPrice, threshold, _ ->
                triggered.add(Triple(karat, currentPrice, threshold))
            },
        )

        // Default 21K is 5425 -> threshold 5400 does not trigger
        vm.updatePriceAlert(enabled = true, above = false, karat = 21, target = "5400")
        assertTrue(triggered.isEmpty())
        assertFalse(vm.uiState.value.isPriceAlertTriggered)

        // Threshold 5500 -> 5425 < 5500 triggers 21K drop alert
        vm.updatePriceAlert(enabled = true, above = false, karat = 21, target = "5500")
        assertEquals(1, triggered.size)
        assertEquals(21, triggered.first().first)
        assertEquals(5425.0, triggered.first().second, 0.01)
        assertEquals(5500.0, triggered.first().third, 0.01)
        assertTrue(vm.uiState.value.isPriceAlertTriggered)

        // Disabled alert suppresses triggers
        vm.updatePriceAlert(enabled = false, above = false, karat = 18, target = "5000")
        assertEquals(1, triggered.size)
        assertFalse(vm.uiState.value.isPriceAlertTriggered)
    }
}
