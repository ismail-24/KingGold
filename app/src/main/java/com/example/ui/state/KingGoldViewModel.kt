package com.example.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GlobalGoldFeedResult
import com.example.data.GlobalGoldPriceService
import com.example.data.KingGoldRepository
import com.example.model.CartItem
import com.example.model.GoldOrder
import com.example.model.KaratPrice
import com.example.model.KycStatus
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.Shop
import com.example.model.ShopSortOption
import com.example.ui.components.GoldFeedStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class KingGoldUiState(
    val userName: String = "إسماعيل",
    val phoneNumber: String = "1012345642",
    val otpCode: String = "",
    val prices: List<KaratPrice> = KingGoldRepository.prices,
    val goldFeedStatus: GoldFeedStatus = GoldFeedStatus.Loading,
    val shops: List<Shop> = KingGoldRepository.shops,
    val searchQuery: String = "",
    val selectedKaratFilter: Int? = null, // null = all
    val verifiedOnlyFilter: Boolean = true,
    val sortOption: ShopSortOption = ShopSortOption.NEAREST,
    val cart: List<CartItem> = emptyList(),
    val cartShop: Shop? = null,
    val selectedPickupSlotIndex: Int = 0,
    val priceLockSecondsRemaining: Int = 582, // 09:42
    val selectedPaymentMethodId: String = "card",
    val orders: List<GoldOrder> = KingGoldRepository.initialOrders,
    val kycFrontDone: Boolean = true,
    val kycBackDone: Boolean = false,
    val kycSelfieDone: Boolean = false,
    val kycStatus: KycStatus = KycStatus.NOT_STARTED,
    val priceAlertEnabled: Boolean = true,
    val priceAlertAbove: Boolean = true,
    val priceAlertTarget: String = "5500",
) {
    val filteredShops: List<Shop>
        get() {
            val q = searchQuery.trim().lowercase()
            return shops.filter { shop ->
                val matchesVerified = !verifiedOnlyFilter || shop.verified
                val shopProducts = KingGoldRepository.productsFor(shop.id)
                val matchesKarat = selectedKaratFilter == null ||
                    shopProducts.any { it.karat == selectedKaratFilter }
                val matchesQuery = q.isEmpty() ||
                    shop.name.lowercase().contains(q) ||
                    shop.area.lowercase().contains(q) ||
                    shop.address.lowercase().contains(q) ||
                    shopProducts.any { it.name.lowercase().contains(q) }
                matchesVerified && matchesKarat && matchesQuery
            }.let { list ->
                when (sortOption) {
                    ShopSortOption.NEAREST -> list.sortedBy { it.distanceKm }
                    ShopSortOption.HIGHEST_RATED -> list.sortedByDescending { it.rating }
                    ShopSortOption.LOWEST_MAKING_FEE -> list.sortedBy { it.makingFeePerGram }
                }
            }
        }

    fun gramPrice(karat: Int): Double =
        prices.firstOrNull { it.karat == karat }?.pricePerGram ?: KingGoldRepository.gramPrice(karat)

    val cartGoldValue: Double
        get() = cart.sumOf { item ->
            gramPrice(item.product.karat) *
                item.product.weightGrams *
                item.quantity
        }

    val cartMakingFee: Double
        get() = cart.sumOf { item ->
            (cartShop?.makingFeePerGram ?: 0.0) *
                item.product.weightGrams *
                item.quantity
        }

    val cartServiceFee: Double
        get() = (cartGoldValue + cartMakingFee) * 0.01

    val cartTotal: Double
        get() = cartGoldValue + cartMakingFee + cartServiceFee

    val cartItemCount: Int
        get() = cart.sumOf { it.quantity }

    val escrowHeld: Double
        get() = orders
            .filter {
                it.status == OrderStatus.PAID ||
                    it.status == OrderStatus.PREPARING ||
                    it.status == OrderStatus.READY_FOR_PICKUP ||
                    it.status == OrderStatus.DISPUTED
            }
            .sumOf { it.total }

    val selectedPickupWindow: String
        get() = KingGoldRepository.pickupSlots.getOrElse(selectedPickupSlotIndex) {
            KingGoldRepository.pickupSlots.first()
        }

    val selectedPaymentMethod: PaymentMethod
        get() = KingGoldRepository.paymentMethods.firstOrNull { it.id == selectedPaymentMethodId }
            ?: KingGoldRepository.paymentMethods.first()
}

class KingGoldViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(KingGoldUiState())
    val uiState: StateFlow<KingGoldUiState> = _uiState.asStateFlow()

    private var priceLockTimerJob: Job? = null

    fun updatePhoneNumber(phone: String) {
        val cleaned = phone.filter { it.isDigit() || it == ' ' }.take(13)
        _uiState.update { it.copy(phoneNumber = cleaned) }
    }

    fun updateOtpCode(code: String) {
        val digits = code.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(otpCode = digits) }
    }

    fun fillDemoOtp() {
        _uiState.update { it.copy(otpCode = "4821") }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateFilters(
        karat: Int?,
        verifiedOnly: Boolean,
        sortOption: ShopSortOption,
    ) {
        _uiState.update {
            it.copy(
                selectedKaratFilter = karat,
                verifiedOnlyFilter = verifiedOnly,
                sortOption = sortOption,
            )
        }
    }

    fun addToCart(shop: Shop, product: Product) {
        _uiState.update { state ->
            val currentCart = if (state.cartShop != null && state.cartShop.id != shop.id) {
                emptyList()
            } else {
                state.cart
            }
            val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
            val updatedCart = if (existingIndex >= 0) {
                currentCart.mapIndexed { idx, item ->
                    if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
                }
            } else {
                currentCart + CartItem(product = product, quantity = 1)
            }
            state.copy(
                cart = updatedCart,
                cartShop = shop,
                priceLockSecondsRemaining = if (currentCart.isEmpty()) 582 else state.priceLockSecondsRemaining,
            )
        }
        startPriceLockTimerIfNeeded()
    }

    fun incrementCartItem(productId: String) {
        _uiState.update { state ->
            state.copy(
                cart = state.cart.map { item ->
                    if (item.product.id == productId && item.quantity < item.product.availablePieces) {
                        item.copy(quantity = item.quantity + 1)
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun decrementCartItem(productId: String) {
        _uiState.update { state ->
            val updated = state.cart.mapNotNull { item ->
                if (item.product.id == productId) {
                    if (item.quantity > 1) item.copy(quantity = item.quantity - 1) else null
                } else {
                    item
                }
            }
            state.copy(
                cart = updated,
                cartShop = if (updated.isEmpty()) null else state.cartShop,
            )
        }
        if (_uiState.value.cart.isEmpty()) {
            priceLockTimerJob?.cancel()
        }
    }

    fun removeFromCart(productId: String) {
        _uiState.update { state ->
            val updated = state.cart.filterNot { it.product.id == productId }
            state.copy(
                cart = updated,
                cartShop = if (updated.isEmpty()) null else state.cartShop,
            )
        }
        if (_uiState.value.cart.isEmpty()) {
            priceLockTimerJob?.cancel()
        }
    }

    fun selectPickupSlot(index: Int) {
        _uiState.update { it.copy(selectedPickupSlotIndex = index) }
    }

    fun refreshPriceLock() {
        _uiState.update { it.copy(priceLockSecondsRemaining = 582) }
        startPriceLockTimerIfNeeded()
    }

    private fun startPriceLockTimerIfNeeded() {
        if (priceLockTimerJob?.isActive == true) return
        priceLockTimerJob = viewModelScope.launch {
            while (_uiState.value.cart.isNotEmpty() && _uiState.value.priceLockSecondsRemaining > 0) {
                delay(1000L)
                _uiState.update { state ->
                    if (state.priceLockSecondsRemaining > 0) {
                        state.copy(priceLockSecondsRemaining = state.priceLockSecondsRemaining - 1)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun selectPaymentMethod(methodId: String) {
        _uiState.update { it.copy(selectedPaymentMethodId = methodId) }
    }

    fun placeOrder(): GoldOrder? {
        val state = _uiState.value
        val shop = state.cartShop ?: return null
        if (state.cart.isEmpty()) return null

        val newOrder = GoldOrder(
            id = "KG-${20480 + state.orders.size + 1}",
            shop = shop,
            items = state.cart.toList(),
            goldValue = state.cartGoldValue,
            makingFee = state.cartMakingFee,
            serviceFee = state.cartServiceFee,
            pickupWindow = state.selectedPickupWindow,
            status = OrderStatus.READY_FOR_PICKUP,
        )
        priceLockTimerJob?.cancel()
        _uiState.update { current ->
            current.copy(
                orders = listOf(newOrder) + current.orders,
                cart = emptyList(),
                cartShop = null,
            )
        }
        return newOrder
    }

    fun completePickup(orderId: String) {
        _uiState.update { state ->
            state.copy(
                orders = state.orders.map { order ->
                    if (order.id == orderId) order.copy(status = OrderStatus.COMPLETED) else order
                },
            )
        }
    }

    fun submitRating(orderId: String, stars: Int) {
        _uiState.update { state ->
            val targetOrder = state.orders.firstOrNull { it.id == orderId }
            val updatedOrders = state.orders.map { order ->
                if (order.id == orderId) order.copy(ratingGiven = stars) else order
            }
            val updatedShops = if (targetOrder != null) {
                state.shops.map { shop ->
                    if (shop.id == targetOrder.shop.id) {
                        val totalReviews = shop.reviews + 1
                        val newRating = ((shop.rating * shop.reviews) + stars) / totalReviews
                        shop.copy(
                            reviews = totalReviews,
                            rating = (Math.round(newRating * 10.0) / 10.0),
                        )
                    } else {
                        shop
                    }
                }
            } else {
                state.shops
            }
            state.copy(orders = updatedOrders, shops = updatedShops)
        }
    }

    fun submitIssueReport(orderId: String, reason: String) {
        _uiState.update { state ->
            state.copy(
                orders = state.orders.map { order ->
                    if (order.id == orderId) {
                        order.copy(status = OrderStatus.DISPUTED, disputeReason = reason)
                    } else {
                        order
                    }
                },
            )
        }
    }

    fun toggleKycStep(step: Int) {
        _uiState.update { state ->
            when (step) {
                1 -> state.copy(kycFrontDone = true)
                2 -> state.copy(kycBackDone = true)
                3 -> state.copy(kycSelfieDone = true)
                else -> state
            }
        }
    }

    fun submitKyc() {
        _uiState.update { state ->
            state.copy(
                kycFrontDone = true,
                kycBackDone = true,
                kycSelfieDone = true,
                kycStatus = KycStatus.VERIFIED,
            )
        }
    }

    fun updatePriceAlert(enabled: Boolean, above: Boolean, target: String) {
        _uiState.update {
            it.copy(
                priceAlertEnabled = enabled,
                priceAlertAbove = above,
                priceAlertTarget = target,
            )
        }
    }

    fun findOrder(orderId: String): GoldOrder? =
        _uiState.value.orders.firstOrNull { it.id == orderId }

    fun findShop(shopId: String): Shop? =
        _uiState.value.shops.firstOrNull { it.id == shopId }

    fun refreshGlobalGoldPrices() {
        _uiState.update { it.copy(goldFeedStatus = GoldFeedStatus.Loading) }
        viewModelScope.launch {
            when (val result = GlobalGoldPriceService.fetchLiveGlobalGoldPrices()) {
                is GlobalGoldFeedResult.Success -> {
                    val q = result.quote
                    val timeStr = SimpleDateFormat("h:mm a", Locale.forLanguageTag("ar")).format(Date())
                    val updatedPrices = listOf(
                        KaratPrice(24, "عيار 24", q.price24KPerGramEgp, 0.4),
                        KaratPrice(21, "عيار 21", q.price21KPerGramEgp, 0.4),
                        KaratPrice(18, "عيار 18", q.price18KPerGramEgp, 0.4),
                        KaratPrice(0, "الجنيه", q.price21KPerGramEgp * 8.0, 0.4),
                    )
                    _uiState.update {
                        it.copy(
                            prices = updatedPrices,
                            goldFeedStatus = GoldFeedStatus.Live(
                                usdPerOunce = q.usdPerTroyOunce,
                                usdToEgpRate = q.usdToEgpRate,
                                fetchedAtLabel = timeStr,
                            ),
                        )
                    }
                }
                is GlobalGoldFeedResult.Error -> {
                    _uiState.update {
                        it.copy(
                            goldFeedStatus = GoldFeedStatus.OfflineFallback(result.message),
                        )
                    }
                }
            }
        }
    }
}
