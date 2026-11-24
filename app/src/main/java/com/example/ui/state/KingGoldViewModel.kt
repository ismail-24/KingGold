package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseMarketNewsGateway
import com.example.data.GlobalGoldFeedResult
import com.example.data.GlobalGoldPriceService
import com.example.data.KingGoldRepository
import com.example.data.MarketNewsGateway
import com.example.data.remote.FirestoreKingGoldRepository
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreVaultRepository
import com.example.data.remote.GoldShopLocationRecord
import com.example.data.remote.OrderItemRecord
import com.example.data.remote.UserCartRecord
import com.example.data.remote.UserPreferencesRecord
import com.example.model.AuditLogRecord
import com.example.model.CartItem
import com.example.model.DailyGoldPrice
import com.example.model.DepositRequest
import com.example.model.DepositStatus
import com.example.model.FractionalGoldOrder
import com.example.model.GoldOrder
import com.example.model.KaratPrice
import com.example.model.KycStatus
import com.example.model.LedgerEntry
import com.example.model.MarketNewsTopic
import com.example.model.MarketNewsUiState
import com.example.model.MerchantAuditLog
import com.example.model.MerchantOrderView
import com.example.model.MerchantPrivateDocs
import com.example.model.MerchantProduct
import com.example.model.MerchantProductStatus
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.model.MoneyUtils
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.PlatformPricingConfig
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.SellGoldRequest
import com.example.model.SellRequestStatus
import com.example.model.Shop
import com.example.model.ShopSortOption
import com.example.model.WalletSummary
import com.example.model.WithdrawalDeliveryType
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.components.GoldFeedStatus
import com.example.ui.components.calculateHaversineDistanceKm
import com.example.ui.components.hasValidMapCoordinates
import com.example.ui.components.isValidMapCoordinatePair
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    val userId: String = "",
    val userName: String = "مستخدم King Gold",
    val userEmail: String = "",
    val isAdmin: Boolean = false,
    val phoneNumber: String = "1012345642",
    val otpCode: String = "",
    val prices: List<KaratPrice> = KingGoldRepository.prices,
    val goldFeedStatus: GoldFeedStatus = GoldFeedStatus.Loading,
    val marketNewsTopic: MarketNewsTopic = MarketNewsTopic.ALL,
    val marketNewsState: MarketNewsUiState = MarketNewsUiState.Idle,
    val platformPricing: PlatformPricingConfig = PlatformPricingConfig(),
    val hasRemotePricingConfig: Boolean = false,
    val staticShops: List<Shop> = KingGoldRepository.shops,
    val firestoreGoldShops: List<Shop> = emptyList(),
    val firestoreDailyGoldPrices: List<DailyGoldPrice> = emptyList(),
    val approvedMerchants: List<MerchantStore> = emptyList(),
    val ownMerchants: List<MerchantStore> = emptyList(),
    val allMerchantsAdmin: List<MerchantStore> = emptyList(),
    val activeMerchantProducts: List<MerchantProduct> = emptyList(),
    val ownMerchantProducts: List<MerchantProduct> = emptyList(),
    val allProductsAdmin: List<MerchantProduct> = emptyList(),
    val merchantOrders: List<MerchantOrderView> = emptyList(),
    val selectedMerchantDocs: MerchantPrivateDocs? = null,
    val selectedMerchantAuditLogs: List<MerchantAuditLog> = emptyList(),
    val searchQuery: String = "",
    val selectedKaratFilter: Int? = null,
    val selectedCityFilter: String? = null,
    val selectedCategoryFilter: String? = null,
    val verifiedOnlyFilter: Boolean = true,
    val sortOption: ShopSortOption = ShopSortOption.NEAREST,
    val searchAnchorLat: Double? = null,
    val searchAnchorLon: Double? = null,
    val searchAnchorLabel: String? = null,
    val nearbyRadiusKm: Double? = null,
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
    val wallet: WalletSummary = WalletSummary(),
    val ledgerEntries: List<LedgerEntry> = emptyList(),
    val sellRequests: List<SellGoldRequest> = emptyList(),
    val allSellRequestsAdmin: List<SellGoldRequest> = emptyList(),
    val fractionalOrders: List<FractionalGoldOrder> = emptyList(),
    val allFractionalOrdersAdmin: List<FractionalGoldOrder> = emptyList(),
    val withdrawalRequests: List<WithdrawalRequest> = emptyList(),
    val allWithdrawalsAdmin: List<WithdrawalRequest> = emptyList(),
    val depositRequests: List<DepositRequest> = emptyList(),
    val allDepositsAdmin: List<DepositRequest> = emptyList(),
    val auditLogsAdmin: List<AuditLogRecord> = emptyList(),
    val actionBannerMessage: String? = null,
    val actionErrorMessage: String? = null,
    val isActionBusy: Boolean = false,
    val firestoreSyncError: String? = null,
) {
    val shops: List<Shop>
        get() = mergeGoldShopCatalog(
            staticShops = staticShops,
            firestoreGoldShops = firestoreGoldShops,
            approvedMerchants = approvedMerchants,
        )

    fun productsForShop(shopId: String): List<Product> {
        val dynamic = activeMerchantProducts
            .filter { it.merchantId == shopId }
            .map { it.toProduct() }
        val isStatic = staticShops.any { it.id == shopId }
        return if (isStatic) {
            KingGoldRepository.productsFor(shopId) + dynamic
        } else {
            dynamic
        }
    }

    val availableCities: List<String>
        get() = shops
            .map { it.city.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    val filteredShops: List<Shop>
        get() {
            val anchorLat = searchAnchorLat
            val anchorLon = searchAnchorLon
            val hasAnchor = anchorLat != null &&
                anchorLon != null &&
                isValidMapCoordinatePair(anchorLat, anchorLon)
            val q = searchQuery.trim().lowercase()
            val selectedCategoryEnum = selectedCategoryFilter?.let { filterLabel ->
                ProductCategory.entries.firstOrNull {
                    it.arabicLabel.equals(filterLabel.trim(), ignoreCase = true)
                }
            }
            val withLiveDistances = if (hasAnchor) {
                shops.map { shop ->
                    if (hasValidMapCoordinates(shop)) {
                        shop.copy(
                            distanceKm = calculateHaversineDistanceKm(
                                lat1 = anchorLat!!,
                                lon1 = anchorLon!!,
                                lat2 = shop.latitude,
                                lon2 = shop.longitude,
                            ),
                        )
                    } else {
                        shop
                    }
                }
            } else {
                shops
            }
            return withLiveDistances.filter { shop ->
                val matchesVerified = !verifiedOnlyFilter || shop.verified
                val matchesFavorite = !favoritesOnlyFilter || favoriteShopIds.contains(shop.id)
                val matchesCity = selectedCityFilter == null ||
                    shop.city.trim().equals(selectedCityFilter.trim(), ignoreCase = true)
                val shopProducts = productsForShop(shop.id)
                val matchesKarat = selectedKaratFilter == null ||
                    shopProducts.any { it.karat == selectedKaratFilter }
                val matchesCategory = selectedCategoryFilter == null ||
                    shopProducts.any { product ->
                        selectedCategoryEnum?.matches(product.category) == true ||
                            product.category.equals(selectedCategoryFilter, ignoreCase = true)
                    }
                val matchesQuery = q.isEmpty() ||
                    shop.name.lowercase().contains(q) ||
                    shop.city.lowercase().contains(q) ||
                    shop.area.lowercase().contains(q) ||
                    shop.address.lowercase().contains(q) ||
                    shop.description.lowercase().contains(q) ||
                    shopProducts.any {
                        it.name.lowercase().contains(q) ||
                            it.category.lowercase().contains(q)
                    }
                val matchesRadius = if (hasAnchor && nearbyRadiusKm != null) {
                    hasValidMapCoordinates(shop) && shop.distanceKm <= nearbyRadiusKm
                } else {
                    true
                }
                matchesVerified &&
                    matchesFavorite &&
                    matchesCity &&
                    matchesCategory &&
                    matchesKarat &&
                    matchesQuery &&
                    matchesRadius
            }.let { list ->
                when (sortOption) {
                    ShopSortOption.NEAREST -> list.sortedBy { it.distanceKm }
                    ShopSortOption.HIGHEST_RATED -> list.sortedWith(
                        compareByDescending<Shop> { if (it.hasVerifiedRating) it.rating else -1.0 }
                            .thenBy { it.distanceKm },
                    )
                    ShopSortOption.LOWEST_MAKING_FEE -> list.sortedBy { it.distanceKm }
                }
            }
        }

    fun gramPrice(karat: Int): Double =
        prices.firstOrNull { it.karat == karat }?.pricePerGram ?: KingGoldRepository.gramPrice(karat)

    fun effectivePurchaseGramPiasters(karat: Int): Long =
        if (hasRemotePricingConfig) {
            platformPricing.referencePriceForKarat(karat)
        } else {
            MoneyUtils.egpToPiasters(gramPrice(karat))
        }

    fun effectiveValuationGramPiasters(karat: Int): Long =
        if (hasRemotePricingConfig) {
            platformPricing.valuationPriceForKarat(karat)
        } else {
            MoneyUtils.egpToPiasters(gramPrice(karat))
        }

    fun productFinalPricePiasters(product: Product): Long =
        if (product.sellingPricePiasters > 0L) {
            product.sellingPricePiasters
        } else {
            MoneyUtils.egpToPiasters(gramPrice(product.karat) * product.weightGrams)
        }

    val cartTotalPiasters: Long
        get() = cart.sumOf { item -> productFinalPricePiasters(item.product) * item.quantity }

    // Feature 3: Customer prices NEVER add making charges or service fees anywhere.
    val cartGoldValue: Double
        get() = MoneyUtils.piastersToEgp(cartTotalPiasters)

    val cartMakingFee: Double
        get() = 0.0

    val cartServiceFee: Double
        get() = 0.0

    val cartTotal: Double
        get() = cartGoldValue

    val cartItemCount: Int
        get() = cart.sumOf { it.quantity }

    val vaultAvailableGoldMg: Long
        get() = wallet.goldAvailableMg

    val vaultCurrentValuationPiasters: Long
        get() = MoneyUtils.computeGoldValuationPiasters(
            gold24kMg = wallet.gold24kMg,
            gold21kMg = wallet.gold21kMg,
            gold18kMg = wallet.gold18kMg,
            valuation24kPerGramPiasters = effectiveValuationGramPiasters(24),
            valuation21kPerGramPiasters = effectiveValuationGramPiasters(21),
            valuation18kPerGramPiasters = effectiveValuationGramPiasters(18),
        )

    val vaultUnrealizedProfitLossPiasters: Long
        get() = if (wallet.goldConfirmedMg > 0L) {
            vaultCurrentValuationPiasters - wallet.totalInvestedPiasters
        } else {
            0L
        }

    val vaultProfitLossPercent: Double
        get() = MoneyUtils.computeProfitLossPercent(
            currentValuationPiasters = vaultCurrentValuationPiasters,
            totalInvestedPiasters = wallet.totalInvestedPiasters,
        )

    val vaultAvgCostPerGramPiasters: Long
        get() = MoneyUtils.computeAverageCostPerGramPiasters(
            totalInvestedPiasters = wallet.totalInvestedPiasters,
            goldConfirmedMg = wallet.goldConfirmedMg,
        )

    val availablePaymentMethods: List<PaymentMethod>
        get() = listOf(
            PaymentMethod(
                id = "cash_wallet",
                title = "المحفظة النقدية",
                subtitle = "المتاح الآن: ${MoneyUtils.piastersToEgpString(wallet.cashAvailablePiasters)} ج.م",
            ),
        ) + KingGoldRepository.paymentMethods

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
        get() = availablePaymentMethods.firstOrNull { it.id == selectedPaymentMethodId }
            ?: availablePaymentMethods.first()

    val isDailyPricesFromFirestore: Boolean
        get() = firestoreDailyGoldPrices.isNotEmpty()

    val dailyGoldPriceTrend: List<DailyGoldPrice>
        get() = if (firestoreDailyGoldPrices.isNotEmpty()) {
            firestoreDailyGoldPrices.sortedBy { it.dateKey }.takeLast(30)
        } else {
            KingGoldRepository.dailyGoldPriceFallbackSeries(
                current24KPrice = gramPrice(24),
                current21KPrice = gramPrice(21),
                current18KPrice = gramPrice(18),
                days = 30,
            )
        }

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

    val isPriceAlertTriggered: Boolean
        get() = GoldPriceNotificationHelper.shouldTriggerPriceThresholdAlert(
            alertEnabled = priceAlertEnabled,
            alertAbove = priceAlertAbove,
            alertKarat = priceAlertKarat,
            thresholdText = priceAlertTarget,
            currentKaratPrice = gramPrice(priceAlertKarat),
        ) != null
}

class KingGoldViewModel(
    private val firestoreRepository: FirestoreKingGoldRepository? = null,
    private val marketplaceRepository: FirestoreMarketplaceRepository? = null,
    private val vaultRepository: FirestoreVaultRepository? = null,
    private val currentUserId: String? = null,
    userDisplayName: String? = null,
    userEmail: String? = null,
    private val on24KPriceDroppedBelowThreshold: ((current24K: Double, threshold: Double) -> Unit)? = null,
    private val onPriceThresholdTriggered: ((karat: Int, currentPrice: Double, threshold: Double, isAbove: Boolean) -> Unit)? = null,
    private val onPricesSynced: ((prices: List<KaratPrice>, feedStatus: GoldFeedStatus) -> Unit)? = null,
    private val marketNewsGateway: MarketNewsGateway = FirebaseMarketNewsGateway(),
    autoFetchPrices: Boolean = firestoreRepository != null,
) : ViewModel() {

    private val isAdminUser = userEmail.equals(ADMIN_EMAIL, ignoreCase = true)

    private val _uiState = MutableStateFlow(
        KingGoldUiState(
            userId = currentUserId.orEmpty(),
            userName = userDisplayName?.takeIf { it.isNotBlank() } ?: "مستخدم King Gold",
            userEmail = userEmail.orEmpty(),
            isAdmin = isAdminUser,
        ),
    )
    val uiState: StateFlow<KingGoldUiState> = _uiState.asStateFlow()

    private var priceLockTimerJob: Job? = null
    private var cartSyncJob: Job? = null
    private var merchantAuditJob: Job? = null
    private var marketNewsJob: Job? = null
    private var hasLocalCartEdits = false
    private var lastNotifiedThresholdSignature: Triple<Int, Double, Double>? = null

    init {
        if (firestoreRepository != null && currentUserId != null) {
            startFirestoreSync(firestoreRepository, currentUserId)
        }
        if (marketplaceRepository != null && currentUserId != null) {
            startMarketplaceSync(marketplaceRepository, currentUserId)
        }
        if (vaultRepository != null && currentUserId != null) {
            startVaultSync(vaultRepository, currentUserId)
        }
        if (autoFetchPrices) {
            refreshGlobalGoldPrices()
            refreshMarketNews()
        }
    }

    private fun <T> collectSafeFlow(
        flow: Flow<T>,
        errorLabel: String,
        onValue: (T) -> Unit,
    ) {
        viewModelScope.launch {
            flow.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, errorLabel, root)
                _uiState.update {
                    it.copy(firestoreSyncError = root.localizedMessage ?: errorLabel)
                }
            }.collect(onValue)
        }
    }

    private fun startFirestoreSync(repo: FirestoreKingGoldRepository, uid: String) {
        viewModelScope.launch {
            repo.ensureInitialUserData(uid)
            if (isAdminUser) {
                repo.seedInitialGoldShopsIfAdmin()
                val snapshot = _uiState.value
                repo.seedInitialDailyGoldPricesIfAdmin(
                    current24KPrice = snapshot.gramPrice(24),
                    current21KPrice = snapshot.gramPrice(21),
                    current18KPrice = snapshot.gramPrice(18),
                )
            }
        }
        collectSafeFlow(repo.observeActiveGoldShops(), "تعذّر مزامنة مواقع محلات الذهب") { remoteShops ->
            _uiState.update { it.copy(firestoreGoldShops = remoteShops, firestoreSyncError = null) }
        }
        collectSafeFlow(repo.observeDailyGoldPrices(), "تعذّر مزامنة السجل اليومي لأسعار الذهب") { dailyPrices ->
            _uiState.update { it.copy(firestoreDailyGoldPrices = dailyPrices, firestoreSyncError = null) }
        }
        collectSafeFlow(repo.observeOrders(uid), "تعذّر مزامنة الطلبات") { records ->
            if (records.isNotEmpty()) {
                val mapped = records.map { it.toGoldOrder(_uiState.value.shops) }
                _uiState.update { it.copy(orders = mapped, firestoreSyncError = null) }
            }
        }
        collectSafeFlow(repo.observePreferences(uid), "تعذّر مزامنة الإعدادات") { prefs ->
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
                evaluateAndNotifyPriceAlert()
            }
        }
        collectSafeFlow(repo.observeCart(uid), "تعذّر مزامنة السلة") { remoteCart ->
            if (remoteCart != null && !hasLocalCartEdits && remoteCart.items.isNotEmpty()) {
                val matchedShop = _uiState.value.shops.firstOrNull { it.id == remoteCart.shopId }
                    ?: Shop(
                        id = remoteCart.shopId,
                        name = remoteCart.shopName.ifBlank { "محل شريك" },
                        area = "القاهرة",
                        address = "فرع معتمد",
                        phone = "",
                        rating = 0.0,
                        reviews = 0,
                        distanceKm = 2.0,
                        verified = true,
                        closesAt = "",
                        workingHours = "",
                        makingFeePerGram = 0.0,
                        ownerUid = remoteCart.merchantOwnerUid.orEmpty(),
                        isDynamicMerchant = !remoteCart.merchantOwnerUid.isNullOrBlank(),
                        hasVerifiedRating = false,
                        hasVerifiedPhone = false,
                        hasVerifiedHours = false,
                    )
                val remainingSec = ((remoteCart.priceLockExpiresAtMs - System.currentTimeMillis()) / 1000L)
                    .toInt()
                    .coerceIn(0, 600)
                _uiState.update { state ->
                    if (state.cart.isEmpty()) {
                        state.copy(
                            cart = remoteCart.items.map { it.toCartItem() },
                            cartShop = matchedShop,
                            selectedPickupSlotIndex = remoteCart.pickupSlotIndex.toInt(),
                            priceLockSecondsRemaining = if (remainingSec > 0) remainingSec else 582,
                        )
                    } else {
                        state
                    }
                }
                if (_uiState.value.cart.isNotEmpty()) startPriceLockTimerIfNeeded()
            }
        }
    }

    private fun startMarketplaceSync(repo: FirestoreMarketplaceRepository, uid: String) {
        collectSafeFlow(repo.observeApprovedMerchants(), "تعذّر مزامنة المحلات") { merchants ->
            _uiState.update { it.copy(approvedMerchants = merchants) }
        }
        collectSafeFlow(repo.observeOwnMerchants(uid), "تعذّر مزامنة حساب التاجر") { own ->
            _uiState.update { it.copy(ownMerchants = own) }
        }
        collectSafeFlow(repo.observeActiveProducts(), "تعذّر مزامنة المنتجات") { products ->
            _uiState.update { it.copy(activeMerchantProducts = products) }
        }
        collectSafeFlow(repo.observeOwnMerchantProducts(uid), "تعذّر مزامنة منتجات التاجر") { products ->
            _uiState.update { it.copy(ownMerchantProducts = products) }
        }
        collectSafeFlow(repo.observeMerchantOrders(uid), "تعذّر مزامنة طلبات التاجر") { orders ->
            _uiState.update { it.copy(merchantOrders = orders) }
        }
        if (isAdminUser) {
            collectSafeFlow(repo.observeAllMerchantsAdmin(), "تعذّر مزامنة قائمة التجار للإدارة") { list ->
                _uiState.update { it.copy(allMerchantsAdmin = list) }
            }
            collectSafeFlow(repo.observeAllProductsAdmin(), "تعذّر مزامنة قائمة المنتجات للإدارة") { list ->
                _uiState.update { it.copy(allProductsAdmin = list) }
            }
        }
    }

    private fun startVaultSync(repo: FirestoreVaultRepository, uid: String) {
        viewModelScope.launch {
            repo.ensureInitialWallet(uid)
        }
        collectSafeFlow(repo.observeWallet(uid), "تعذّر مزامنة المحفظة") { wallet ->
            _uiState.update { it.copy(wallet = wallet) }
        }
        collectSafeFlow(repo.observeLedger(uid), "تعذّر مزامنة سجل المعاملات") { entries ->
            _uiState.update { it.copy(ledgerEntries = entries) }
        }
        collectSafeFlow(repo.observePlatformPricing(), "تعذّر مزامنة تسعير المنصة") { cfg ->
            if (cfg != null) {
                val p24 = MoneyUtils.piastersToEgp(cfg.reference24kPerGramPiasters)
                val p21 = MoneyUtils.piastersToEgp(cfg.reference21kPerGramPiasters)
                val p18 = MoneyUtils.piastersToEgp(cfg.reference18kPerGramPiasters)
                val prev24 = _uiState.value.gramPrice(24)
                val pct = if (prev24 > 0.0) ((p24 - prev24) / prev24) * 100.0 else 0.0
                val updatedPrices = listOf(
                    KaratPrice(24, "عيار 24", p24, pct),
                    KaratPrice(21, "عيار 21", p21, pct),
                    KaratPrice(18, "عيار 18", p18, pct),
                    KaratPrice(0, "الجنيه", p21 * 8.0, pct),
                )
                _uiState.update {
                    it.copy(
                        platformPricing = cfg,
                        hasRemotePricingConfig = true,
                        prices = updatedPrices,
                    )
                }
                onPricesSynced?.invoke(updatedPrices, _uiState.value.goldFeedStatus)
                evaluateAndNotifyPriceAlert()
            }
        }
        collectSafeFlow(repo.observeUserSellRequests(uid), "تعذّر مزامنة طلبات البيع") { list ->
            _uiState.update { it.copy(sellRequests = list) }
        }
        collectSafeFlow(repo.observeUserFractionalOrders(uid), "تعذّر مزامنة مشتريات الذهب المجزأ") { list ->
            _uiState.update { it.copy(fractionalOrders = list) }
        }
        collectSafeFlow(repo.observeUserWithdrawals(uid), "تعذّر مزامنة طلبات السحب") { list ->
            _uiState.update { it.copy(withdrawalRequests = list) }
        }
        collectSafeFlow(repo.observeUserDeposits(uid), "تعذّر مزامنة طلبات الإيداع") { list ->
            _uiState.update { it.copy(depositRequests = list) }
        }
        if (isAdminUser) {
            collectSafeFlow(repo.observeAllSellRequestsAdmin(), "تعذّر مزامنة طلبات البيع للإدارة") { list ->
                _uiState.update { it.copy(allSellRequestsAdmin = list) }
            }
            collectSafeFlow(repo.observeAllWithdrawalsAdmin(), "تعذّر مزامنة السحوبات للإدارة") { list ->
                _uiState.update { it.copy(allWithdrawalsAdmin = list) }
            }
            collectSafeFlow(repo.observeAllDepositsAdmin(), "تعذّر مزامنة الإيداعات للإدارة") { list ->
                _uiState.update { it.copy(allDepositsAdmin = list) }
            }
            collectSafeFlow(repo.observeAllPendingFractionalAdmin(), "تعذّر مزامنة المشتريات المجزأة المعلقة") { list ->
                _uiState.update { it.copy(allFractionalOrdersAdmin = list) }
            }
            collectSafeFlow(repo.observeAuditLogsAdmin(), "تعذّر مزامنة سجلات التدقيق") { list ->
                _uiState.update { it.copy(auditLogsAdmin = list) }
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

    private fun scheduleCartSyncToCloud() {
        val repo = firestoreRepository ?: return
        val uid = currentUserId ?: return
        cartSyncJob?.cancel()
        cartSyncJob = viewModelScope.launch {
            delay(180L)
            val state = _uiState.value
            val shop = state.cartShop
            if (state.cart.isEmpty() || shop == null) {
                repo.clearCart()
            } else {
                val expiresAt = System.currentTimeMillis() + (state.priceLockSecondsRemaining.coerceAtLeast(60) * 1000L)
                repo.saveCart(
                    UserCartRecord(
                        userId = uid,
                        shopId = shop.id,
                        shopName = shop.name,
                        merchantOwnerUid = shop.ownerUid.takeIf { it.isNotBlank() },
                        items = state.cart.map { OrderItemRecord.fromCartItem(it) },
                        pickupSlotIndex = state.selectedPickupSlotIndex.toLong(),
                        priceLockExpiresAtMs = expiresAt,
                    ),
                )
            }
        }
    }

    fun clearSyncError() {
        _uiState.update { it.copy(firestoreSyncError = null) }
    }

    fun clearActionMessages() {
        _uiState.update { it.copy(actionBannerMessage = null, actionErrorMessage = null) }
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

    fun updateCityFilter(city: String?) {
        _uiState.update { it.copy(selectedCityFilter = city?.takeIf { c -> c.isNotBlank() }) }
    }

    fun updateCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedCategoryFilter = category?.takeIf { c -> c.isNotBlank() }) }
    }

    fun updateSearchAnchor(
        latitude: Double,
        longitude: Double,
        label: String,
    ) {
        if (!isValidMapCoordinatePair(latitude, longitude)) return
        _uiState.update {
            it.copy(
                searchAnchorLat = latitude,
                searchAnchorLon = longitude,
                searchAnchorLabel = label,
                sortOption = ShopSortOption.NEAREST,
            )
        }
    }

    fun updateNearbyRadiusKm(radiusKm: Double?) {
        _uiState.update { it.copy(nearbyRadiusKm = radiusKm) }
    }

    fun clearSearchAnchor() {
        _uiState.update {
            it.copy(
                searchAnchorLat = null,
                searchAnchorLon = null,
                searchAnchorLabel = null,
                nearbyRadiusKm = null,
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
        hasLocalCartEdits = true
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
                actionErrorMessage = null,
            )
        }
        startPriceLockTimerIfNeeded()
        scheduleCartSyncToCloud()
    }

    fun incrementCartItem(productId: String) {
        hasLocalCartEdits = true
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
        scheduleCartSyncToCloud()
    }

    fun decrementCartItem(productId: String) {
        hasLocalCartEdits = true
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
        scheduleCartSyncToCloud()
    }

    fun removeFromCart(productId: String) {
        hasLocalCartEdits = true
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
        scheduleCartSyncToCloud()
    }

    fun selectPickupSlot(index: Int) {
        hasLocalCartEdits = true
        _uiState.update { it.copy(selectedPickupSlotIndex = index) }
        scheduleCartSyncToCloud()
    }

    fun refreshPriceLock() {
        _uiState.update { it.copy(priceLockSecondsRemaining = 582) }
        startPriceLockTimerIfNeeded()
        scheduleCartSyncToCloud()
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
        _uiState.update { it.copy(selectedPaymentMethodId = methodId, actionErrorMessage = null) }
        syncPreferencesToFirestore(_uiState.value)
    }

    fun placeOrder(): GoldOrder? {
        val state = _uiState.value
        val shop = state.cartShop ?: return null
        if (state.cart.isEmpty()) return null

        val payWithWallet = state.selectedPaymentMethodId == "cash_wallet"
        val totalPiasters = state.cartTotalPiasters
        if (payWithWallet && state.wallet.cashAvailablePiasters < totalPiasters) {
            _uiState.update {
                it.copy(actionErrorMessage = "الرصيد المتاح بالمحفظة النقدية غير كافٍ لإتمام الطلب.")
            }
            return null
        }

        val suffix = (System.currentTimeMillis() % 100_000L).toInt()
        val newOrder = GoldOrder(
            id = "KG-${20480 + state.orders.size + 1}-$suffix",
            shop = shop,
            items = state.cart.toList(),
            goldValue = state.cartGoldValue,
            makingFee = 0.0,
            serviceFee = 0.0,
            pickupWindow = state.selectedPickupWindow,
            status = OrderStatus.READY_FOR_PICKUP,
            totalPiasters = totalPiasters,
            paymentSource = if (payWithWallet) "CASH_WALLET" else "ESCROW_METHOD",
        )
        priceLockTimerJob?.cancel()
        hasLocalCartEdits = true
        val dynamicItems = state.cart.toList()
        _uiState.update { current ->
            current.copy(
                orders = listOf(newOrder) + current.orders,
                cart = emptyList(),
                cartShop = null,
                actionErrorMessage = null,
            )
        }
        viewModelScope.launch {
            firestoreRepository?.clearCart()
            val mRepo = marketplaceRepository
            if (mRepo != null) {
                mRepo.placeMarketplaceOrderAtomic(
                    order = newOrder,
                    payWithWallet = payWithWallet,
                    dynamicCartProductIds = dynamicItems,
                ).onFailure { err ->
                    _uiState.update {
                        it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر حفظ الطلب في السحابة")
                    }
                }
            } else {
                firestoreRepository?.createOrder(newOrder)?.onFailure { err ->
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
        viewModelScope.launch {
            firestoreRepository?.completePickup(orderId)?.onFailure { err ->
                _uiState.update {
                    it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر تحديث حالة الاستلام")
                }
            }
            marketplaceRepository?.syncBuyerOrderStatusToMerchantOrder(orderId, OrderStatus.COMPLETED)
        }
    }

    fun submitRating(orderId: String, stars: Int) {
        _uiState.update { state ->
            val targetOrder = state.orders.firstOrNull { it.id == orderId }
            val updatedOrders = state.orders.map { order ->
                if (order.id == orderId) order.copy(ratingGiven = stars) else order
            }
            val updatedStaticShops = if (targetOrder != null) {
                state.staticShops.map { shop ->
                    if (shop.id == targetOrder.shop.id) {
                        val totalReviews = shop.reviews + 1
                        val newRating = ((shop.rating * shop.reviews) + stars) / totalReviews
                        shop.copy(
                            reviews = totalReviews,
                            rating = (Math.round(newRating * 10.0) / 10.0),
                            hasVerifiedRating = true,
                        )
                    } else {
                        shop
                    }
                }
            } else {
                state.staticShops
            }
            state.copy(orders = updatedOrders, staticShops = updatedStaticShops)
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
        viewModelScope.launch {
            firestoreRepository?.submitIssueReport(orderId, reason)?.onFailure { err ->
                _uiState.update {
                    it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر إرسال البلاغ")
                }
            }
            marketplaceRepository?.syncBuyerOrderStatusToMerchantOrder(orderId, OrderStatus.DISPUTED)
        }
    }

    // =========================================================================
    // Feature 5 & 6: Digital Gold Vault & Cash Wallet Actions
    // =========================================================================

    fun buyFractionalGold(
        karat: Int,
        weightMg: Long,
        payWithWallet: Boolean,
        onSuccess: (String) -> Unit = {},
    ) {
        val state = _uiState.value
        if (karat !in listOf(18, 21, 24)) {
            _uiState.update { it.copy(actionErrorMessage = "العيار غير مدعوم.") }
            return
        }
        if (weightMg <= 0L) {
            _uiState.update { it.copy(actionErrorMessage = "الرجاء إدخال وزن صحيح أكبر من 0 ملجم.") }
            return
        }
        val unitPricePiasters = state.effectivePurchaseGramPiasters(karat)
        val totalPiasters = MoneyUtils.computeFractionalCostPiasters(weightMg, unitPricePiasters)
        if (totalPiasters <= 0L) {
            _uiState.update { it.copy(actionErrorMessage = "قيمة الشراء غير صالحة.") }
            return
        }
        if (payWithWallet && state.wallet.cashAvailablePiasters < totalPiasters) {
            _uiState.update {
                it.copy(actionErrorMessage = "رصيد المحفظة النقدية غير كافٍ (${MoneyUtils.piastersToEgpString(totalPiasters)} ج.م مطلوب).")
            }
            return
        }

        val repo = vaultRepository
        if (repo == null) {
            // Pure local unit test path without Firestore
            _uiState.update { s ->
                val prev = s.wallet
                val nextWallet = if (payWithWallet) {
                    prev.copy(
                        cashAvailablePiasters = prev.cashAvailablePiasters - totalPiasters,
                        cashTotalDebitedPiasters = prev.cashTotalDebitedPiasters + totalPiasters,
                        goldConfirmedMg = prev.goldConfirmedMg + weightMg,
                        gold24kMg = prev.gold24kMg + if (karat == 24) weightMg else 0L,
                        gold21kMg = prev.gold21kMg + if (karat == 21) weightMg else 0L,
                        gold18kMg = prev.gold18kMg + if (karat == 18) weightMg else 0L,
                        totalInvestedPiasters = prev.totalInvestedPiasters + totalPiasters,
                    )
                } else {
                    prev.copy(goldPendingMg = prev.goldPendingMg + weightMg)
                }
                s.copy(
                    wallet = nextWallet,
                    actionBannerMessage = "تم تسجيل شراء ${MoneyUtils.mgToGramsString(weightMg)} عيار $karat بنجاح",
                    actionErrorMessage = null,
                )
            }
            onSuccess("local_frac")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.buyFractionalGoldAtomic(
                karat = karat,
                weightMg = weightMg,
                pricePerGramPiasters = unitPricePiasters,
                payWithWallet = payWithWallet,
            ).onSuccess { ordId ->
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionBannerMessage = if (payWithWallet) {
                            "تمت إضافة ${MoneyUtils.mgToGramsString(weightMg)} عيار $karat لخزنتك فوراً"
                        } else {
                            "تم إنشاء طلب شراء معلق لحين تأكيد السداد"
                        },
                    )
                }
                onSuccess(ordId)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionErrorMessage = err.localizedMessage ?: "تعذّر إتمام شراء الذهب المجزأ",
                    )
                }
            }
        }
    }

    fun requestPhysicalWithdrawal(
        karat: Int,
        amountMg: Long,
        deliveryType: WithdrawalDeliveryType,
        deliveryAddress: String,
        onSuccess: (String) -> Unit = {},
    ) {
        if (amountMg < MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG) {
            _uiState.update {
                it.copy(actionErrorMessage = "الحد الأدنى لاستلام الذهب الفعلي هو 1.000 جرام (1000 ملجم).")
            }
            return
        }
        val state = _uiState.value
        val karatHeld = state.wallet.confirmedMgForKarat(karat)
        if (state.wallet.goldAvailableMg < amountMg || karatHeld < amountMg) {
            _uiState.update {
                it.copy(actionErrorMessage = "رصيد عيار $karat المتاح غير محجوز لا يكفي لسحب ${MoneyUtils.mgToGramsString(amountMg)}.")
            }
            return
        }
        if (deliveryAddress.trim().length < 4) {
            _uiState.update {
                it.copy(actionErrorMessage = "يرجى إدخال عنوان التوصيل أو اسم فرع الاستلام بوضوح.")
            }
            return
        }

        val repo = vaultRepository
        if (repo == null) {
            val wdRef = "WD-TEST-${amountMg}"
            _uiState.update { s ->
                val prev = s.wallet
                s.copy(
                    wallet = prev.copy(goldReservedMg = prev.goldReservedMg + amountMg),
                    actionBannerMessage = "تم حجز ${MoneyUtils.mgToGramsString(amountMg)} للسحب الفعلي ($wdRef)",
                    actionErrorMessage = null,
                )
            }
            onSuccess(wdRef)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.requestPhysicalWithdrawalAtomic(
                karat = karat,
                amountMg = amountMg,
                deliveryType = deliveryType,
                deliveryAddress = deliveryAddress,
            ).onSuccess { wdRef ->
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionBannerMessage = "تم إنشاء طلب السحب الفعلي $wdRef وحجز الرصيد بأمان",
                    )
                }
                onSuccess(wdRef)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionErrorMessage = err.localizedMessage ?: "تعذّر إنشاء طلب السحب الفعلي",
                    )
                }
            }
        }
    }

    fun submitDepositRequest(
        amountPiasters: Long,
        channel: String,
        paymentReference: String,
        onSuccess: () -> Unit = {},
    ) {
        if (amountPiasters < 1000L) {
            _uiState.update { it.copy(actionErrorMessage = "الحد الأدنى لطلب الإيداع هو 10.00 ج.م.") }
            return
        }
        if (paymentReference.trim().length < 3) {
            _uiState.update { it.copy(actionErrorMessage = "يرجى إدخال مرجع التحويل المالي.") }
            return
        }
        val repo = vaultRepository
        if (repo == null) {
            val localId = "DEP-LOCAL-${System.currentTimeMillis()}"
            _uiState.update { s ->
                val newReq = DepositRequest(
                    id = localId,
                    customerUid = s.userId,
                    customerEmail = s.userEmail,
                    amountPiasters = amountPiasters,
                    channel = channel,
                    paymentReference = paymentReference.trim(),
                    status = DepositStatus.PENDING,
                    createdAtMs = System.currentTimeMillis(),
                )
                s.copy(
                    depositRequests = listOf(newReq) + s.depositRequests,
                    wallet = s.wallet.copy(cashPendingPiasters = s.wallet.cashPendingPiasters + amountPiasters),
                    actionBannerMessage = "تم تسجيل طلب إيداع قيد المراجعة بمبلغ ${MoneyUtils.piastersToEgpString(amountPiasters)} ج.م",
                    actionErrorMessage = null,
                )
            }
            onSuccess()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.submitDepositRequestAtomic(amountPiasters, channel, paymentReference)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isActionBusy = false,
                            actionBannerMessage = "تم تسجيل طلب إيداع قيد المراجعة بمبلغ ${MoneyUtils.piastersToEgpString(amountPiasters)} ج.م",
                        )
                    }
                    onSuccess()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isActionBusy = false,
                            actionErrorMessage = err.localizedMessage ?: "تعذّر إرسال طلب الإيداع",
                        )
                    }
                }
        }
    }

    // =========================================================================
    // Feature 2: Customer-to-Platform Sell Your Gold
    // =========================================================================

    fun submitSellGoldRequest(
        itemTitle: String,
        category: String,
        karat: Int,
        estimatedWeightMg: Long,
        conditionNotes: String,
        photoThumbnails: List<String>,
        onSuccess: () -> Unit = {},
    ) {
        if (itemTitle.trim().length < 2) {
            _uiState.update { it.copy(actionErrorMessage = "يرجى كتابة اسم القطعة المراد بيعها.") }
            return
        }
        if (estimatedWeightMg < 10L) {
            _uiState.update { it.copy(actionErrorMessage = "الوزن التقديري يجب أن لا يقل عن 0.01 جم.") }
            return
        }
        val repo = vaultRepository
        if (repo == null) {
            val localReq = SellGoldRequest(
                id = "SELL-LOCAL-${System.currentTimeMillis()}",
                customerUid = _uiState.value.userId,
                customerEmail = _uiState.value.userEmail,
                itemTitle = itemTitle.trim(),
                category = category,
                karat = karat,
                estimatedWeightMg = estimatedWeightMg,
                conditionNotes = conditionNotes.trim(),
                photoThumbnails = photoThumbnails,
                status = SellRequestStatus.SUBMITTED,
                createdAtMs = System.currentTimeMillis(),
            )
            _uiState.update { s ->
                s.copy(
                    sellRequests = listOf(localReq) + s.sellRequests,
                    actionBannerMessage = "تم إرسال طلب بيع الذهب للمنصة وهو قيد الفحص الآن",
                    actionErrorMessage = null,
                )
            }
            onSuccess()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.submitSellGoldRequest(
                itemTitle = itemTitle,
                category = category,
                karat = karat,
                estimatedWeightMg = estimatedWeightMg,
                conditionNotes = conditionNotes,
                photoThumbnails = photoThumbnails,
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionBannerMessage = "تم إرسال طلب بيع الذهب للمنصة وهو قيد الفحص الآن",
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionErrorMessage = err.localizedMessage ?: "تعذّر إرسال طلب بيع الذهب",
                    )
                }
            }
        }
    }

    fun cancelSellGoldRequest(requestId: String) {
        val repo = vaultRepository
        if (repo == null) {
            _uiState.update { s ->
                s.copy(
                    sellRequests = s.sellRequests.map { r ->
                        if (r.id == requestId) r.copy(status = SellRequestStatus.CANCELLED) else r
                    },
                    actionBannerMessage = "تم إلغاء طلب البيع",
                    actionErrorMessage = null,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.cancelSellGoldRequest(requestId).onSuccess {
                _uiState.update {
                    it.copy(isActionBusy = false, actionBannerMessage = "تم إلغاء طلب البيع")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر إلغاء الطلب")
                }
            }
        }
    }

    fun acceptSellGoldOffer(request: SellGoldRequest) {
        val repo = vaultRepository
        if (repo == null) {
            _uiState.update { s ->
                s.copy(
                    sellRequests = s.sellRequests.map { r ->
                        if (r.id == request.id) r.copy(status = SellRequestStatus.COMPLETED) else r
                    },
                    wallet = s.wallet.copy(
                        cashAvailablePiasters = s.wallet.cashAvailablePiasters + request.finalOfferPiasters,
                        cashTotalCreditedPiasters = s.wallet.cashTotalCreditedPiasters + request.finalOfferPiasters,
                    ),
                    actionBannerMessage = "تم قبول العرض وإضافة ${MoneyUtils.piastersToEgpString(request.finalOfferPiasters)} ج.م إلى محفظتك النقدية",
                    actionErrorMessage = null,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.acceptSellGoldOfferAtomic(request).onSuccess {
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionBannerMessage = "تم قبول العرض وإضافة ${MoneyUtils.piastersToEgpString(request.finalOfferPiasters)} ج.م إلى محفظتك النقدية",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر تسوية طلب البيع")
                }
            }
        }
    }

    // =========================================================================
    // Feature 1: Merchant Registration & Store Management
    // =========================================================================

    fun loadMerchantInspectionDetails(merchantId: String) {
        val repo = marketplaceRepository ?: return
        merchantAuditJob?.cancel()
        merchantAuditJob = collectSafeJob(repo.observeMerchantAuditLogs(merchantId)) { logs ->
            _uiState.update { it.copy(selectedMerchantAuditLogs = logs) }
        }
        viewModelScope.launch {
            repo.getMerchantPrivateDocs(merchantId).onSuccess { docs ->
                _uiState.update { it.copy(selectedMerchantDocs = docs) }
            }
        }
    }

    private fun <T> collectSafeJob(flow: Flow<T>, onValue: (T) -> Unit): Job =
        viewModelScope.launch {
            flow.catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Audit log stream error", error)
            }.collect(onValue)
        }

    fun submitMerchantRegistration(
        shopName: String,
        area: String,
        address: String,
        contactPhone: String,
        verifiedEmail: String,
        description: String,
        legalFullName: String,
        nationalIdNumber: String,
        commercialRegNumber: String,
        taxCardNumber: String,
        authorizationNotes: String,
        documentThumbnails: List<String>,
        onSuccess: () -> Unit = {},
    ) {
        if (shopName.trim().length < 2 || address.trim().length < 4 || legalFullName.trim().length < 3 || nationalIdNumber.trim().length < 5) {
            _uiState.update {
                it.copy(actionErrorMessage = "يرجى استكمال اسم المحل والعنوان والاسم القانوني ورقم الهوية/الترخيص.")
            }
            return
        }
        val repo = marketplaceRepository ?: run {
            _uiState.update { it.copy(actionErrorMessage = "خدمة السحابة غير متصلة حالياً — يرجى إعادة تسجيل الدخول.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.submitMerchantRegistration(
                shopName = shopName,
                area = area,
                address = address,
                contactPhone = contactPhone,
                verifiedEmail = verifiedEmail.ifBlank { _uiState.value.userEmail },
                description = description,
                legalFullName = legalFullName,
                nationalIdNumber = nationalIdNumber,
                commercialRegNumber = commercialRegNumber,
                taxCardNumber = taxCardNumber,
                authorizationNotes = authorizationNotes,
                documentThumbnails = documentThumbnails,
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionBannerMessage = "تم تقديم طلب تسجيل التاجر وهو بانتظار اعتماد الإدارة",
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر إرسال طلب التسجيل")
                }
            }
        }
    }

    fun saveMerchantProduct(
        merchant: MerchantStore,
        existingProduct: MerchantProduct?,
        name: String,
        description: String,
        category: String,
        karat: Int,
        weightGrams: Double,
        sellingPricePiasters: Long,
        stockQuantity: Int,
        stamped: Boolean,
        imageThumbnails: List<String>,
        onSuccess: () -> Unit = {},
    ) {
        if (merchant.status != MerchantStatus.APPROVED) {
            _uiState.update { it.copy(actionErrorMessage = "لا يمكن نشر منتجات قبل اعتماد حساب التاجر من الإدارة.") }
            return
        }
        if (name.trim().length < 2 || weightGrams <= 0.0 || sellingPricePiasters <= 0L) {
            _uiState.update { it.copy(actionErrorMessage = "يرجى إدخال اسم المنتج والوزن والسعر النهائي بشكل صحيح.") }
            return
        }
        val repo = marketplaceRepository ?: run {
            _uiState.update { it.copy(actionErrorMessage = "خدمة السحابة غير متصلة حالياً — تعذّر حفظ المنتج.") }
            return
        }
        val resubmit = existingProduct != null &&
            (existingProduct.status == MerchantProductStatus.PAUSED || existingProduct.status == MerchantProductStatus.REJECTED)
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.createOrUpdateMerchantProduct(
                merchant = merchant,
                existingProductId = existingProduct?.id,
                name = name,
                description = description,
                category = category,
                karat = karat,
                weightGrams = weightGrams,
                sellingPricePiasters = sellingPricePiasters,
                stockQuantity = stockQuantity,
                stamped = stamped,
                imageThumbnails = imageThumbnails,
                resubmitForReview = resubmit,
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionBannerMessage = if (existingProduct == null) {
                            "تمت إضافة المنتج وإرساله للمراجعة"
                        } else {
                            "تم تحديث بيانات المنتج"
                        },
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر حفظ المنتج")
                }
            }
        }
    }

    fun toggleMerchantProductPause(product: MerchantProduct) {
        val repo = marketplaceRepository ?: run {
            _uiState.update { it.copy(actionErrorMessage = "خدمة السحابة غير متصلة حالياً.") }
            return
        }
        viewModelScope.launch {
            repo.toggleMerchantProductPause(product).onSuccess {
                _uiState.update { it.copy(actionBannerMessage = "تم تحديث حالة عرض المنتج") }
            }.onFailure { err ->
                _uiState.update { it.copy(actionErrorMessage = err.localizedMessage ?: "تعذّر تعديل حالة المنتج") }
            }
        }
    }

    fun updateMerchantOrderStatus(orderId: String, nextStatus: OrderStatus) {
        val repo = marketplaceRepository ?: run {
            _uiState.update { it.copy(actionErrorMessage = "خدمة السحابة غير متصلة حالياً.") }
            return
        }
        viewModelScope.launch {
            repo.updateMerchantOrderStatus(orderId, nextStatus).onSuccess {
                _uiState.update { it.copy(actionBannerMessage = "تم تحديث حالة الطلب إلى ${nextStatus.arabicLabel}") }
            }.onFailure { err ->
                _uiState.update { it.copy(actionErrorMessage = err.localizedMessage ?: "تعذّر تحديث حالة الطلب") }
            }
        }
    }

    // =========================================================================
    // Feature 7: Admin Control Center Actions
    // =========================================================================

    fun adminReviewMerchant(
        merchant: MerchantStore,
        newStatus: MerchantStatus,
        prepaidApproved: Boolean,
        prepaidBudgetPiasters: Long,
        reasonOrNote: String,
    ) {
        if (!_uiState.value.isAdmin) return
        val repo = marketplaceRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.adminReviewMerchant(merchant, newStatus, prepaidApproved, prepaidBudgetPiasters, reasonOrNote)
                .onSuccess {
                    _uiState.update {
                        it.copy(isActionBusy = false, actionBannerMessage = "تم تحديث حالة التاجر (${merchant.shopName})")
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر تنفيذ إجراء الإدارة")
                    }
                }
        }
    }

    fun adminReviewProduct(
        product: MerchantProduct,
        newStatus: MerchantProductStatus,
        reason: String,
    ) {
        if (!_uiState.value.isAdmin) return
        val repo = marketplaceRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.adminReviewProduct(product, newStatus, reason)
                .onSuccess {
                    _uiState.update {
                        it.copy(isActionBusy = false, actionBannerMessage = "تم تحديث حالة المنتج (${product.name})")
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر مراجعة المنتج")
                    }
                }
        }
    }

    fun adminEvaluateSellGold(
        request: SellGoldRequest,
        verifiedWeightMg: Long,
        confirmedKarat: Int,
        referencePricePerGramPiasters: Long,
        makingChargeDeductionPiasters: Long,
        inspectionNotes: String,
    ) {
        if (!_uiState.value.isAdmin) return
        if (verifiedWeightMg <= 0L || referencePricePerGramPiasters <= 0L) {
            _uiState.update { it.copy(actionErrorMessage = "الرجاء إدخال الوزن الفعلي والسعر المرجعي بشكل صحيح.") }
            return
        }
        val repo = vaultRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.adminEvaluateSellGold(
                request = request,
                verifiedWeightMg = verifiedWeightMg,
                confirmedKarat = confirmedKarat,
                referencePricePerGramPiasters = referencePricePerGramPiasters,
                makingChargeDeductionPiasters = makingChargeDeductionPiasters,
                inspectionNotes = inspectionNotes,
            ).onSuccess {
                _uiState.update {
                    it.copy(isActionBusy = false, actionBannerMessage = "تم إرسال تقييم وعرض السعر النهائي للعميل")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر إرسال التقييم")
                }
            }
        }
    }

    fun adminRejectSellGold(request: SellGoldRequest, reason: String) {
        if (!_uiState.value.isAdmin) return
        val repo = vaultRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.adminRejectSellGold(request, reason).onSuccess {
                _uiState.update {
                    it.copy(isActionBusy = false, actionBannerMessage = "تم رفض طلب بيع الذهب")
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر رفض الطلب")
                }
            }
        }
    }

    fun adminApproveOrRejectDeposit(deposit: DepositRequest, approve: Boolean, adminNote: String) {
        if (!_uiState.value.isAdmin) return
        val repo = vaultRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.adminApproveOrRejectDepositAtomic(deposit, approve, adminNote)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isActionBusy = false,
                            actionBannerMessage = if (approve) "تم اعتماد الإيداع وإضافته لمحفظة العميل" else "تم رفض طلب الإيداع",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر معالجة الإيداع")
                    }
                }
        }
    }

    fun adminProcessWithdrawal(request: WithdrawalRequest, nextStatus: WithdrawalStatus, proofNote: String) {
        if (!_uiState.value.isAdmin) return
        val repo = vaultRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.adminProcessWithdrawalAtomic(request, nextStatus, proofNote)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isActionBusy = false,
                            actionBannerMessage = "تم تحديث السحب ${request.withdrawalRef} إلى ${nextStatus.arabicLabel}",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر معالجة طلب السحب")
                    }
                }
        }
    }

    fun adminConfirmOrRejectFractional(order: FractionalGoldOrder, confirm: Boolean) {
        if (!_uiState.value.isAdmin) return
        val repo = vaultRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.adminConfirmOrRejectFractionalOrderAtomic(order, confirm)
                .onSuccess {
                    _uiState.update {
                        it.copy(isActionBusy = false, actionBannerMessage = if (confirm) "تم تأكيد رصيد الذهب المجزأ للعميل" else "تم رفض الطلب المعلق")
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر معالجة الطلب")
                    }
                }
        }
    }

    fun adminUpdatePricingConfig(config: PlatformPricingConfig) {
        if (!_uiState.value.isAdmin) return
        val repo = vaultRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.adminUpdatePricingConfig(config)
                .onSuccess {
                    _uiState.update {
                        it.copy(isActionBusy = false, actionBannerMessage = "تم تحديث تسعير المنصة وسياسات المصنعية بنجاح")
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isActionBusy = false, actionErrorMessage = err.localizedMessage ?: "تعذّر تحديث التسعير")
                    }
                }
        }
    }

    // =========================================================================
    // KYC, Price Alerts, FCM & Global Price Feed
    // =========================================================================

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
        lastNotifiedThresholdSignature = null
        syncPreferencesToFirestore(_uiState.value)
        evaluateAndNotifyPriceAlert()
    }

    fun updateFcmTokenState(token: String) {
        if (token.isBlank() || token == _uiState.value.fcmToken) return
        _uiState.update { it.copy(fcmToken = token) }
    }

    fun applyIncomingFcmEvent(event: com.example.notifications.FcmIncomingPriceEvent) {
        val current = _uiState.value
        val p24 = event.price24k?.takeIf { it > 0.0 } ?: current.gramPrice(24)
        val p21 = event.price21k?.takeIf { it > 0.0 } ?: current.gramPrice(21)
        val p18 = event.price18k?.takeIf { it > 0.0 } ?: current.gramPrice(18)
        val hasPriceUpdate = listOf(event.price24k, event.price21k, event.price18k).any { (it ?: 0.0) > 0.0 }

        val updatedPrices = if (hasPriceUpdate) {
            val pct = event.changePercent ?: current.prices.firstOrNull { it.karat == 24 }?.changePercent ?: 0.0
            listOf(
                KaratPrice(24, "عيار 24", p24, pct),
                KaratPrice(21, "عيار 21", p21, pct),
                KaratPrice(18, "عيار 18", p18, pct),
                KaratPrice(0, "الجنيه", p21 * 8.0, pct),
            )
        } else {
            current.prices
        }

        val bannerText = if (event.triggeredKarat != null && event.triggeredThreshold != null) {
            val directionLabel = if (event.triggeredAbove == true) "تجاوز" else "انخفض عن"
            event.alertBody?.takeIf { it.isNotBlank() }
                ?: "تنبيه FCM: عيار ${event.triggeredKarat} $directionLabel الحد المحدد (${event.triggeredThreshold.toInt()} ج.م)"
        } else {
            null
        }

        _uiState.update { state ->
            state.copy(
                prices = updatedPrices,
                actionBannerMessage = bannerText ?: state.actionBannerMessage,
            )
        }
        if (hasPriceUpdate) {
            onPricesSynced?.invoke(updatedPrices, _uiState.value.goldFeedStatus)
            evaluateAndNotifyPriceAlert()
        }
    }

    private fun evaluateAndNotifyPriceAlert() {
        val state = _uiState.value
        val karat = state.priceAlertKarat
        val currentPrice = state.gramPrice(karat)
        val threshold = GoldPriceNotificationHelper.shouldTriggerPriceThresholdAlert(
            alertEnabled = state.priceAlertEnabled,
            alertAbove = state.priceAlertAbove,
            alertKarat = karat,
            thresholdText = state.priceAlertTarget,
            currentKaratPrice = currentPrice,
        ) ?: return

        val signature = Triple(karat, currentPrice, threshold)
        if (lastNotifiedThresholdSignature != signature) {
            lastNotifiedThresholdSignature = signature
            onPriceThresholdTriggered?.invoke(karat, currentPrice, threshold, state.priceAlertAbove)
            if (karat == 24 && !state.priceAlertAbove) {
                on24KPriceDroppedBelowThreshold?.invoke(currentPrice, threshold)
            }
        }
    }

    fun findOrder(orderId: String): GoldOrder? =
        _uiState.value.orders.firstOrNull { it.id == orderId }

    fun findShop(shopId: String): Shop? =
        _uiState.value.shops.firstOrNull { it.id == shopId }

    fun saveGoldShopLocationToFirestore(shop: Shop, active: Boolean = true) {
        val repo = firestoreRepository ?: return
        val uid = currentUserId ?: return
        val record = GoldShopLocationRecord.fromShop(
            shop = shop,
            ownerUid = shop.ownerUid.ifBlank { uid },
            merchantId = if (shop.isDynamicMerchant) shop.id else null,
            active = active && _uiState.value.isAdmin,
            includeGeoPoint = true,
        )
        viewModelScope.launch {
            repo.saveGoldShopLocation(record, shopId = shop.id.takeIf { it.isNotBlank() })
                .onSuccess {
                    _uiState.update { it.copy(actionBannerMessage = "تم حفظ إحداثيات محل ${shop.name} في Firestore.") }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(firestoreSyncError = err.localizedMessage ?: "تعذّر حفظ إحداثيات المحل")
                    }
                }
        }
    }

    fun recordTodayDailyGoldPrices() {
        if (!_uiState.value.isAdmin) return
        val repo = firestoreRepository ?: return
        val snapshot = _uiState.value
        val p24 = MoneyUtils.egpToPiasters(snapshot.gramPrice(24))
        val p21 = MoneyUtils.egpToPiasters(snapshot.gramPrice(21))
        val p18 = MoneyUtils.egpToPiasters(snapshot.gramPrice(18))
        viewModelScope.launch {
            _uiState.update { it.copy(isActionBusy = true, actionErrorMessage = null) }
            repo.recordDailyGoldPriceIfAdmin(
                dateKey = KingGoldRepository.todayDateKey(),
                dayLabel = "اليوم",
                price24kPerGramPiasters = p24,
                price21kPerGramPiasters = p21,
                price18kPerGramPiasters = p18,
                source = if (snapshot.goldFeedStatus is GoldFeedStatus.Live) "GLOBAL_FEED" else "ADMIN_PRICING",
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionBannerMessage = "تم تسجيل أسعار اليوم لعيار 24 و21 و18 في Firestore.",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionBusy = false,
                        actionErrorMessage = err.localizedMessage ?: "تعذّر تسجيل أسعار اليوم",
                    )
                }
            }
        }
    }

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
                    val liveStatus = GoldFeedStatus.Live(
                        usdPerOunce = q.usdPerTroyOunce,
                        usdToEgpRate = q.usdToEgpRate,
                        fetchedAtLabel = timeStr,
                        fxRatesPerUsd = q.fxRatesPerUsd,
                    )
                    _uiState.update {
                        it.copy(
                            prices = updatedPrices,
                            goldFeedStatus = liveStatus,
                        )
                    }
                    onPricesSynced?.invoke(updatedPrices, liveStatus)
                    evaluateAndNotifyPriceAlert()
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

    fun selectMarketNewsTopic(topic: MarketNewsTopic) {
        if (_uiState.value.marketNewsTopic == topic && _uiState.value.marketNewsState is MarketNewsUiState.Success) {
            return
        }
        _uiState.update { it.copy(marketNewsTopic = topic) }
        refreshMarketNews(topic)
    }

    fun refreshMarketNews(topic: MarketNewsTopic = _uiState.value.marketNewsTopic) {
        marketNewsJob?.cancel()
        _uiState.update {
            it.copy(
                marketNewsTopic = topic,
                marketNewsState = MarketNewsUiState.Loading,
            )
        }
        marketNewsJob = viewModelScope.launch {
            try {
                val feed = marketNewsGateway.fetchLatestGoldMarketNews(topic)
                _uiState.update {
                    it.copy(
                        marketNewsTopic = topic,
                        marketNewsState = MarketNewsUiState.Success(feed),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        marketNewsState = MarketNewsUiState.Error(
                            e.localizedMessage ?: "تعذّر جلب أخبار السوق عبر Google Search حالياً.",
                        ),
                    )
                }
                runCatching {
                    Log.e(TAG, "Failed to load market news for topic=${topic.name}", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "KingGoldVM"
        const val ADMIN_EMAIL = "esmailtarek228@gmail.com"
    }
}

fun mergeGoldShopCatalog(
    staticShops: List<Shop>,
    firestoreGoldShops: List<Shop>,
    approvedMerchants: List<MerchantStore>,
): List<Shop> {
    val merged = LinkedHashMap<String, Shop>()
    staticShops.forEach { shop -> merged[shop.id] = shop }
    firestoreGoldShops.forEach { shop -> merged[shop.id] = shop }
    approvedMerchants.map { it.toShop() }.forEach { merchantShop ->
        val existing = merged[merchantShop.id]
        val hasMerchantCoords = merchantShop.latitude != 0.0 || merchantShop.longitude != 0.0
        merged[merchantShop.id] = if (existing != null && !hasMerchantCoords) {
            merchantShop.copy(
                latitude = existing.latitude,
                longitude = existing.longitude,
                city = merchantShop.city.ifBlank { existing.city },
            )
        } else {
            merchantShop
        }
    }
    return merged.values.toList()
}
