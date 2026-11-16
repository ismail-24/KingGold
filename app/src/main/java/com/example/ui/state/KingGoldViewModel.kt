package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GlobalGoldFeedResult
import com.example.data.GlobalGoldPriceService
import com.example.data.KingGoldRepository
import com.example.data.remote.FirestoreKingGoldRepository
import com.example.data.remote.UserPreferencesRecord
import com.example.model.CartItem
import com.example.model.GoldOrder
import com.example.model.KaratPrice
import com.example.model.KycStatus
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.Shop
import com.example.model.ShopSortOption
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.components.GoldFeedStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class KingGoldUiState(
    val userName: String = "مستخدم King Gold",
    val userEmail: String = "",
    val phoneNumber: String = "1012345642",
    val otpCode: String = "",
    val prices: List<KaratPrice> = KingGoldRepository.prices,
    val goldFeedStatus: GoldFeedStatus = GoldFeedStatus.Loading,
    val shops: List<Shop> = KingGoldRepository.shops,
    val searchQuery: String = "",
    val selectedKaratFilter: Int? = null,
    val verifiedOnlyFilter: Boolean = true,
    val sortOption: ShopSortOption = ShopSortOption.NEAREST,
    val cart: List<CartItem> = emptyList(),
    val cartShop: Shop? = null,
    val selectedPickupSlotIndex: Int = 0,
    val priceLockSecondsRemaining: Int = 582,
    val selectedPaymentMethodId: String = "card",
    val orders: List<GoldOrder> = KingGoldRepository.initialOrders,
    val kycFrontDone: Boolean = true,
    val kycBackDone: Boolean = false,
    val kycSelfieDone: Boolean = false,
    val kycStatus: KycStatus = KycStatus.NOT_STARTED,
    val priceAlertEnabled: Boolean = true,
    val priceAlertAbove: Boolean = false,
    val priceAlertKarat: Int = 24,
    val priceAlertTarget: String = "6000",
    val fcmToken: String? = null,
    val favoriteShopIds: Set<String> = emptySet(),
    val favoritesOnlyFilter: Boolean = false,
    val firestoreSyncError: String? = null,
) {
    val filteredShops: List<Shop>
        get() {
            val q = searchQuery.trim().lowercase()
            return shops.filter { shop ->
                val matchesVerified = !verifiedOnlyFilter || shop.verified
                val matchesFavorite = !favoritesOnlyFilter || favoriteShopIds.contains(shop.id)
                val shopProducts = KingGoldRepository.productsFor(shop.id)
                val matchesKarat = selectedKaratFilter == null ||
                    shopProducts.any { it.karat == selectedKaratFilter }
                val matchesQuery = q.isEmpty() ||
                    shop.name.lowercase().contains(q) ||
                    shop.area.lowercase().contains(q) ||
                    shop.address.lowercase().contains(q) ||
                    shopProducts.any { it.name.lowercase().contains(q) }
                matchesVerified && matchesFavorite && matchesKarat && matchesQuery
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

    val history24K7Days: List<com.example.model.GoldPriceHistoryPoint>
        get() = KingGoldRepository.priceHistory24KForCurrentPrice(gramPrice(24))

    val history21K7Days: List<com.example.model.GoldPriceHistoryPoint>
        get() = KingGoldRepository.priceHistory21KForCurrentPrice(gramPrice(21))

    val is24KAlertTriggered: Boolean
        get() = GoldPriceNotificationHelper.shouldTrigger24KDropAlert(
            alertEnabled = priceAlertEnabled,
            alertAbove = priceAlertAbove,
            alertKarat = priceAlertKarat,
            thresholdText = priceAlertTarget,
            current24KPrice = gramPrice(24),
        ) != null
}

class KingGoldViewModel(
    private val firestoreRepository: FirestoreKingGoldRepository? = null,
    private val currentUserId: String? = null,
    userDisplayName: String? = null,
    userEmail: String? = null,
    private val on24KPriceDroppedBelowThreshold: ((current24K: Double, threshold: Double) -> Unit)? = null,
    autoFetchPrices: Boolean = firestoreRepository != null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        KingGoldUiState(
            userName = userDisplayName?.takeIf { it.isNotBlank() } ?: "مستخدم King Gold",
            userEmail = userEmail.orEmpty(),
        ),
    )
    val uiState: StateFlow<KingGoldUiState> = _uiState.asStateFlow()

    private var priceLockTimerJob: Job? = null
    private var lastNotifiedDropSignature: Pair<Double, Double>? = null

    init {
        if (firestoreRepository != null && currentUserId != null) {
            startFirestoreSync(firestoreRepository, currentUserId)
        }
        if (autoFetchPrices) {
            refreshGlobalGoldPrices()
        }
    }

    private fun startFirestoreSync(repo: FirestoreKingGoldRepository, uid: String) {
        viewModelScope.launch {
            repo.ensureInitialUserData(uid)
        }
        viewModelScope.launch {
            repo.observeOrders(uid)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val root = firestoreError ?: error
                    Log.w(TAG, "Error observing orders", root)
                    _uiState.update {
                        it.copy(firestoreSyncError = root.localizedMessage ?: "تعذّر مزامنة الطلبات")
                    }
                }
                .collect { records ->
                    if (records.isNotEmpty()) {
                        val mapped = records.map { it.toGoldOrder(_uiState.value.shops) }
                        _uiState.update { it.copy(orders = mapped, firestoreSyncError = null) }
                    }
                }
        }
        viewModelScope.launch {
            repo.observePreferences(uid)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val root = firestoreError ?: error
                    Log.w(TAG, "Error observing preferences", root)
                    _uiState.update {
                        it.copy(firestoreSyncError = root.localizedMessage ?: "تعذّر مزامنة الإعدادات")
                    }
                }
                .collect { prefs ->
                    if (prefs != null) {
                        _uiState.update { state ->
                            state.copy(
                                kycFrontDone = prefs.kycFrontDone,
                                kycBackDone = prefs.kycBackDone,
                                kycSelfieDone = prefs.kycSelfieDone,
                                kycStatus = prefs.parsedKycStatus(),
                                selectedPaymentMethodId = prefs.selectedPaymentMethodId,
                                priceAlertEnabled = prefs.priceAlertEnabled,
                                priceAlertAbove = prefs.priceAlertAbove,
                                priceAlertKarat = prefs.priceAlertKarat.toInt().let {
                                    if (it in listOf(18, 21, 24)) it else 24
                                },
                                priceAlertTarget = prefs.priceAlertTarget,
                                fcmToken = prefs.fcmToken,
                                favoriteShopIds = prefs.favoriteShopIds.toSet(),
                                firestoreSyncError = null,
                            )
                        }
                        evaluateAndNotify24KPriceDrop()
                    }
                }
        }
    }

    private fun syncPreferencesToFirestore(state: KingGoldUiState) {
        val repo = firestoreRepository ?: return
        val uid = currentUserId ?: return
        viewModelScope.launch {
            repo.savePreferences(
                UserPreferencesRecord(
                    userId = uid,
                    kycFrontDone = state.kycFrontDone,
                    kycBackDone = state.kycBackDone,
                    kycSelfieDone = state.kycSelfieDone,
                    kycStatus = state.kycStatus.name,
                    selectedPaymentMethodId = state.selectedPaymentMethodId,
                    priceAlertEnabled = state.priceAlertEnabled,
                    priceAlertAbove = state.priceAlertAbove,
                    priceAlertKarat = state.priceAlertKarat.toLong(),
                    priceAlertTarget = state.priceAlertTarget,
                    fcmToken = state.fcmToken,
                    favoriteShopIds = state.favoriteShopIds.toList(),
                ),
            ).onFailure { err ->
                _uiState.update {
                    it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر حفظ الإعدادات")
                }
            }
        }
    }

    fun clearSyncError() {
        _uiState.update { it.copy(firestoreSyncError = null) }
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

    fun toggleFavoritesOnlyFilter(favoritesOnly: Boolean) {
        _uiState.update { it.copy(favoritesOnlyFilter = favoritesOnly) }
    }

    fun toggleFavoriteShop(shopId: String) {
        _uiState.update { state ->
            val updatedFavorites = if (state.favoriteShopIds.contains(shopId)) {
                state.favoriteShopIds - shopId
            } else {
                state.favoriteShopIds + shopId
            }
            state.copy(favoriteShopIds = updatedFavorites)
        }
        syncPreferencesToFirestore(_uiState.value)
    }

    fun addToCart(shop: Shop, product: Product) {
        _uiState.update { state ->
            val currentCart = if (state.cartShop != null && state.cartShop.id != shop.id) {
                emptyList()
            } else {
                state.cart
            }
            val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }
            val maxPieces = product.availablePieces.coerceAtLeast(1)
            val updatedCart = if (existingIndex >= 0) {
                currentCart.mapIndexed { idx, item ->
                    if (idx == existingIndex) {
                        item.copy(quantity = (item.quantity + 1).coerceAtMost(maxPieces))
                    } else {
                        item
                    }
                }
            } else {
                currentCart + CartItem(product = product, quantity = 1)
            }
            val shouldResetTimer = currentCart.isEmpty() || state.priceLockSecondsRemaining <= 0
            state.copy(
                cart = updatedCart,
                cartShop = shop,
                priceLockSecondsRemaining = if (shouldResetTimer) 582 else state.priceLockSecondsRemaining,
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
        syncPreferencesToFirestore(_uiState.value)
    }

    fun placeOrder(): GoldOrder? {
        val state = _uiState.value
        val shop = state.cartShop ?: return null
        if (state.cart.isEmpty()) return null

        val suffix = (System.currentTimeMillis() % 100_000L).toInt()
        val newOrder = GoldOrder(
            id = "KG-${20480 + state.orders.size + 1}-$suffix",
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
        firestoreRepository?.let { repo ->
            viewModelScope.launch {
                repo.createOrder(newOrder).onFailure { err ->
                    _uiState.update {
                        it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر حفظ الطلب في السحابة")
                    }
                }
            }
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
        firestoreRepository?.let { repo ->
            viewModelScope.launch {
                repo.completePickup(orderId).onFailure { err ->
                    _uiState.update {
                        it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر تحديث حالة الاستلام")
                    }
                }
            }
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
        firestoreRepository?.let { repo ->
            viewModelScope.launch {
                repo.submitRating(orderId, stars).onFailure { err ->
                    _uiState.update {
                        it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر حفظ التقييم")
                    }
                }
            }
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
        firestoreRepository?.let { repo ->
            viewModelScope.launch {
                repo.submitIssueReport(orderId, reason).onFailure { err ->
                    _uiState.update {
                        it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر إرسال البلاغ")
                    }
                }
            }
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
        syncPreferencesToFirestore(_uiState.value)
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
        syncPreferencesToFirestore(_uiState.value)
    }

    fun updatePriceAlert(
        enabled: Boolean,
        above: Boolean,
        karat: Int,
        target: String,
    ) {
        _uiState.update {
            it.copy(
                priceAlertEnabled = enabled,
                priceAlertAbove = above,
                priceAlertKarat = karat,
                priceAlertTarget = target,
            )
        }
        lastNotifiedDropSignature = null
        syncPreferencesToFirestore(_uiState.value)
        evaluateAndNotify24KPriceDrop()
    }

    fun updateFcmTokenState(token: String) {
        if (token.isBlank() || token == _uiState.value.fcmToken) return
        _uiState.update { it.copy(fcmToken = token) }
    }

    private fun evaluateAndNotify24KPriceDrop() {
        val state = _uiState.value
        val current24K = state.gramPrice(24)
        val threshold = GoldPriceNotificationHelper.shouldTrigger24KDropAlert(
            alertEnabled = state.priceAlertEnabled,
            alertAbove = state.priceAlertAbove,
            alertKarat = state.priceAlertKarat,
            thresholdText = state.priceAlertTarget,
            current24KPrice = current24K,
        ) ?: return

        val signature = current24K to threshold
        if (lastNotifiedDropSignature != signature) {
            lastNotifiedDropSignature = signature
            on24KPriceDroppedBelowThreshold?.invoke(current24K, threshold)
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
                    val prev24K = _uiState.value.gramPrice(24)
                    val pctChange = if (_uiState.value.goldFeedStatus is GoldFeedStatus.Live && prev24K > 0.0) {
                        ((q.price24KPerGramEgp - prev24K) / prev24K) * 100.0
                    } else {
                        0.0
                    }
                    val updatedPrices = listOf(
                        KaratPrice(24, "عيار 24", q.price24KPerGramEgp, pctChange),
                        KaratPrice(21, "عيار 21", q.price21KPerGramEgp, pctChange),
                        KaratPrice(18, "عيار 18", q.price18KPerGramEgp, pctChange),
                        KaratPrice(0, "الجنيه", q.price21KPerGramEgp * 8.0, pctChange),
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
                    evaluateAndNotify24KPriceDrop()
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

    private companion object {
        const val TAG = "KingGoldVM"
    }
}
