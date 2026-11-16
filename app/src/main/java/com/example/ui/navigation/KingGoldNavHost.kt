package com.example.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.KingGoldRepository
import com.example.notifications.FcmRegistrationManager
import com.example.ui.screens.ai.AiAdvisorScreen
import com.example.ui.screens.auth.KycScreen
import com.example.ui.screens.cart.CartScreen
import com.example.ui.screens.checkout.CheckoutScreen
import com.example.ui.screens.checkout.SuccessScreen
import com.example.ui.screens.home.HomeShell
import com.example.ui.screens.home.HomeTab
import com.example.ui.screens.orders.OrderDetailScreen
import com.example.ui.screens.orders.RatingScreen
import com.example.ui.screens.orders.ReportIssueScreen
import com.example.ui.screens.orders.VerifyPickupScreen
import com.example.ui.screens.price.PriceInsightScreen
import com.example.ui.screens.shop.ProductDetailScreen
import com.example.ui.screens.shop.ProductListScreen
import com.example.ui.screens.shop.ShopDetailScreen
import com.example.ui.state.KingGoldViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object Routes {
    const val HOME = "home"
    const val KYC = "kyc"
    const val PRICE_INSIGHT = "price_insight"
    const val AI_ADVISOR = "ai_advisor?prompt={prompt}"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val SHOP_DETAIL = "shop_detail/{shopId}"
    const val PRODUCT_LIST = "product_list/{shopId}?category={category}"
    const val PRODUCT_DETAIL = "product_detail/{shopId}/{productId}"
    const val SUCCESS = "success/{orderId}"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val VERIFY_PICKUP = "verify_pickup/{orderId}"
    const val RATING = "rating/{orderId}"
    const val REPORT_ISSUE = "report_issue/{orderId}"

    fun aiAdvisor(prompt: String? = null): String =
        if (prompt.isNullOrBlank()) "ai_advisor" else "ai_advisor?prompt=${Uri.encode(prompt)}"

    fun shopDetail(shopId: String) = "shop_detail/$shopId"
    fun productList(shopId: String, category: String? = null): String {
        val base = "product_list/$shopId"
        return if (category != null) "$base?category=${Uri.encode(category)}" else base
    }
    fun productDetail(shopId: String, productId: String) = "product_detail/$shopId/$productId"
    fun success(orderId: String) = "success/$orderId"
    fun orderDetail(orderId: String) = "order_detail/$orderId"
    fun verifyPickup(orderId: String) = "verify_pickup/$orderId"
    fun rating(orderId: String) = "rating/$orderId"
    fun reportIssue(orderId: String) = "report_issue/$orderId"
}

private fun NavController.navigateSingleTop(route: String) {
    val canNavigate = currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
    if (!canNavigate) return
    if (currentDestination?.route == route) return
    navigate(route) {
        launchSingleTop = true
    }
}

private fun NavController.popBackSafely() {
    val canPop = currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
    if (canPop && previousBackStackEntry != null) {
        popBackStack()
    }
}

@Composable
fun KingGoldNavHost(
    viewModel: KingGoldViewModel,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var selectedHomeTab by remember { mutableStateOf(HomeTab.DISCOVER) }

    val alertConfig by remember(viewModel) {
        viewModel.uiState
            .map { Triple(it.priceAlertEnabled, it.priceAlertKarat, it.priceAlertAbove) }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = Triple(true, 24, false))

    LaunchedEffect(alertConfig) {
        FcmRegistrationManager.syncFcmAlertSubscription(
            context = context,
            enabled = alertConfig.first,
            alertKarat = alertConfig.second,
            alertAbove = alertConfig.third,
            onTokenSynced = viewModel::updateFcmTokenState,
        )
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HomeShell(
                selectedTab = selectedHomeTab,
                onSelectTab = { selectedHomeTab = it },
                uiState = uiState,
                onSearchChange = viewModel::updateSearchQuery,
                onApplyFilters = viewModel::updateFilters,
                onOpenPriceInsight = { navController.navigateSingleTop(Routes.PRICE_INSIGHT) },
                onOpenAiAdvisor = { prompt -> navController.navigateSingleTop(Routes.aiAdvisor(prompt)) },
                onRefreshGlobalGoldPrices = viewModel::refreshGlobalGoldPrices,
                onOpenCart = { navController.navigateSingleTop(Routes.CART) },
                onOpenShop = { shop -> navController.navigateSingleTop(Routes.shopDetail(shop.id)) },
                onToggleFavoriteShop = viewModel::toggleFavoriteShop,
                onToggleFavoritesOnlyFilter = viewModel::toggleFavoritesOnlyFilter,
                onOpenOrder = { order -> navController.navigateSingleTop(Routes.orderDetail(order.id)) },
                onOpenKyc = { navController.navigateSingleTop(Routes.KYC) },
                onSelectPaymentMethod = viewModel::selectPaymentMethod,
                onDismissSyncError = viewModel::clearSyncError,
                onLogout = onSignOut,
            )
        }

        composable(
            route = Routes.AI_ADVISOR,
            arguments = listOf(
                navArgument("prompt") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val initialPrompt = backStackEntry.arguments?.getString("prompt")
            AiAdvisorScreen(
                uiState = uiState,
                initialPrompt = initialPrompt,
                onBack = { navController.popBackSafely() },
            )
        }

        composable(Routes.KYC) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            KycScreen(
                frontDone = uiState.kycFrontDone,
                backDone = uiState.kycBackDone,
                selfieDone = uiState.kycSelfieDone,
                onStepCompleted = viewModel::toggleKycStep,
                onSubmitKyc = viewModel::submitKyc,
                onBack = { navController.popBackSafely() },
            )
        }

        composable(Routes.PRICE_INSIGHT) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            PriceInsightScreen(
                uiState = uiState,
                onSaveAlert = viewModel::updatePriceAlert,
                onOpenAiAdvisor = { prompt -> navController.navigateSingleTop(Routes.aiAdvisor(prompt)) },
                onBack = { navController.popBackSafely() },
            )
        }

        composable(
            route = Routes.SHOP_DETAIL,
            arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val shop = viewModel.findShop(shopId) ?: uiState.shops.firstOrNull() ?: return@composable
            ShopDetailScreen(
                shop = shop,
                prices = uiState.prices,
                isFavorite = uiState.favoriteShopIds.contains(shop.id),
                onToggleFavorite = { viewModel.toggleFavoriteShop(shop.id) },
                onBack = { navController.popBackSafely() },
                onBrowseProducts = { category ->
                    navController.navigateSingleTop(Routes.productList(shop.id, category))
                },
                onOpenProduct = { product ->
                    navController.navigateSingleTop(Routes.productDetail(shop.id, product.id))
                },
                onOpenPriceInsight = { navController.navigateSingleTop(Routes.PRICE_INSIGHT) },
            )
        }

        composable(
            route = Routes.PRODUCT_LIST,
            arguments = listOf(
                navArgument("shopId") { type = NavType.StringType },
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val category = backStackEntry.arguments?.getString("category")
            val shop = viewModel.findShop(shopId) ?: uiState.shops.firstOrNull() ?: return@composable
            ProductListScreen(
                shop = shop,
                initialCategory = category,
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onOpenProduct = { product ->
                    navController.navigateSingleTop(Routes.productDetail(shop.id, product.id))
                },
                onAddToCart = { product -> viewModel.addToCart(shop, product) },
                onOpenCart = { navController.navigateSingleTop(Routes.CART) },
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(
                navArgument("shopId") { type = NavType.StringType },
                navArgument("productId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            val shop = viewModel.findShop(shopId) ?: uiState.shops.firstOrNull() ?: return@composable
            val products = KingGoldRepository.productsFor(shop.id)
            val product = products.firstOrNull { it.id == productId } ?: products.first()
            ProductDetailScreen(
                shop = shop,
                product = product,
                gramPrice = uiState.gramPrice(product.karat),
                onBack = { navController.popBackSafely() },
                onAddToCartAndGo = {
                    viewModel.addToCart(shop, product)
                    navController.navigateSingleTop(Routes.CART)
                },
                onAskAiAboutProduct = {
                    val prompt = "هل قطعة ${product.name} (${product.weightGrams} جم عيار ${product.karat}) من محل ${shop.name} بمصنعية ${shop.makingFeePerGram.toInt()} ج.م/جم مناسبة للشراء الآن؟"
                    navController.navigateSingleTop(Routes.aiAdvisor(prompt))
                },
            )
        }

        composable(Routes.CART) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            CartScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onIncrement = viewModel::incrementCartItem,
                onDecrement = viewModel::decrementCartItem,
                onRemove = viewModel::removeFromCart,
                onSelectPickupSlot = viewModel::selectPickupSlot,
                onRefreshPriceLock = viewModel::refreshPriceLock,
                onProceedToCheckout = { navController.navigateSingleTop(Routes.CHECKOUT) },
            )
        }

        composable(Routes.CHECKOUT) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            CheckoutScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onSelectPaymentMethod = viewModel::selectPaymentMethod,
                onConfirmPay = {
                    val created = viewModel.placeOrder()
                    if (created != null) {
                        navController.navigate(Routes.success(created.id)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(
            route = Routes.SUCCESS,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.firstOrNull() ?: return@composable
            SuccessScreen(
                order = order,
                onViewAllOrders = {
                    selectedHomeTab = HomeTab.ORDERS
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.firstOrNull() ?: return@composable
            OrderDetailScreen(
                order = order,
                onBack = { navController.popBackSafely() },
                onStartVerifyPickup = { navController.navigateSingleTop(Routes.verifyPickup(order.id)) },
                onReportIssue = { navController.navigateSingleTop(Routes.reportIssue(order.id)) },
            )
        }

        composable(
            route = Routes.VERIFY_PICKUP,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.firstOrNull() ?: return@composable
            VerifyPickupScreen(
                order = order,
                onBack = { navController.popBackSafely() },
                onConfirmRelease = {
                    viewModel.completePickup(order.id)
                    navController.navigate(Routes.rating(order.id)) {
                        popUpTo(Routes.VERIFY_PICKUP) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.RATING,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.firstOrNull() ?: return@composable
            RatingScreen(
                order = order,
                onSubmitRating = { stars ->
                    viewModel.submitRating(order.id, stars)
                    selectedHomeTab = HomeTab.ORDERS
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.REPORT_ISSUE,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.firstOrNull() ?: return@composable
            ReportIssueScreen(
                order = order,
                onBack = { navController.popBackSafely() },
                onSubmitReport = { reason ->
                    viewModel.submitIssueReport(order.id, reason)
                    navController.popBackSafely()
                },
            )
        }
    }
}
