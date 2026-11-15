package com.example.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun KingGoldNavHost(
    viewModel: KingGoldViewModel,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedHomeTab by remember { mutableStateOf(HomeTab.DISCOVER) }

    LaunchedEffect(
        uiState.priceAlertEnabled,
        uiState.priceAlertKarat,
        uiState.priceAlertAbove,
    ) {
        FcmRegistrationManager.syncFcmAlertSubscription(
            context = context,
            enabled = uiState.priceAlertEnabled,
            alertKarat = uiState.priceAlertKarat,
            alertAbove = uiState.priceAlertAbove,
            onTokenSynced = viewModel::updateFcmTokenState,
        )
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeShell(
                selectedTab = selectedHomeTab,
                onSelectTab = { selectedHomeTab = it },
                uiState = uiState,
                onSearchChange = viewModel::updateSearchQuery,
                onApplyFilters = viewModel::updateFilters,
                onOpenPriceInsight = { navController.navigate(Routes.PRICE_INSIGHT) },
                onOpenAiAdvisor = { prompt -> navController.navigate(Routes.aiAdvisor(prompt)) },
                onRefreshGlobalGoldPrices = viewModel::refreshGlobalGoldPrices,
                onOpenCart = { navController.navigate(Routes.CART) },
                onOpenShop = { shop -> navController.navigate(Routes.shopDetail(shop.id)) },
                onToggleFavoriteShop = viewModel::toggleFavoriteShop,
                onToggleFavoritesOnlyFilter = viewModel::toggleFavoritesOnlyFilter,
                onOpenOrder = { order -> navController.navigate(Routes.orderDetail(order.id)) },
                onOpenKyc = { navController.navigate(Routes.KYC) },
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
            val initialPrompt = backStackEntry.arguments?.getString("prompt")
            AiAdvisorScreen(
                uiState = uiState,
                initialPrompt = initialPrompt,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.KYC) {
            KycScreen(
                frontDone = uiState.kycFrontDone,
                backDone = uiState.kycBackDone,
                selfieDone = uiState.kycSelfieDone,
                onStepCompleted = viewModel::toggleKycStep,
                onSubmitKyc = viewModel::submitKyc,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PRICE_INSIGHT) {
            PriceInsightScreen(
                uiState = uiState,
                onSaveAlert = viewModel::updatePriceAlert,
                onOpenAiAdvisor = { prompt -> navController.navigate(Routes.aiAdvisor(prompt)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SHOP_DETAIL,
            arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val shop = viewModel.findShop(shopId) ?: uiState.shops.first()
            ShopDetailScreen(
                shop = shop,
                prices = uiState.prices,
                isFavorite = uiState.favoriteShopIds.contains(shop.id),
                onToggleFavorite = { viewModel.toggleFavoriteShop(shop.id) },
                onBack = { navController.popBackStack() },
                onBrowseProducts = { category ->
                    navController.navigate(Routes.productList(shop.id, category))
                },
                onOpenProduct = { product ->
                    navController.navigate(Routes.productDetail(shop.id, product.id))
                },
                onOpenPriceInsight = { navController.navigate(Routes.PRICE_INSIGHT) },
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
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val category = backStackEntry.arguments?.getString("category")
            val shop = viewModel.findShop(shopId) ?: uiState.shops.first()
            ProductListScreen(
                shop = shop,
                initialCategory = category,
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onOpenProduct = { product ->
                    navController.navigate(Routes.productDetail(shop.id, product.id))
                },
                onAddToCart = { product -> viewModel.addToCart(shop, product) },
                onOpenCart = { navController.navigate(Routes.CART) },
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(
                navArgument("shopId") { type = NavType.StringType },
                navArgument("productId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            val shop = viewModel.findShop(shopId) ?: uiState.shops.first()
            val products = KingGoldRepository.productsFor(shop.id)
            val product = products.firstOrNull { it.id == productId } ?: products.first()
            ProductDetailScreen(
                shop = shop,
                product = product,
                gramPrice = uiState.gramPrice(product.karat),
                onBack = { navController.popBackStack() },
                onAddToCartAndGo = {
                    viewModel.addToCart(shop, product)
                    navController.navigate(Routes.CART)
                },
                onAskAiAboutProduct = {
                    val prompt = "هل قطعة ${product.name} (${product.weightGrams} جم عيار ${product.karat}) من محل ${shop.name} بمصنعية ${shop.makingFeePerGram.toInt()} ج.م/جم مناسبة للشراء الآن؟"
                    navController.navigate(Routes.aiAdvisor(prompt))
                },
            )
        }

        composable(Routes.CART) {
            CartScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onIncrement = viewModel::incrementCartItem,
                onDecrement = viewModel::decrementCartItem,
                onRemove = viewModel::removeFromCart,
                onSelectPickupSlot = viewModel::selectPickupSlot,
                onRefreshPriceLock = viewModel::refreshPriceLock,
                onProceedToCheckout = { navController.navigate(Routes.CHECKOUT) },
            )
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onSelectPaymentMethod = viewModel::selectPaymentMethod,
                onConfirmPay = {
                    val created = viewModel.placeOrder()
                    if (created != null) {
                        navController.navigate(Routes.success(created.id)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                },
            )
        }

        composable(
            route = Routes.SUCCESS,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.first()
            SuccessScreen(
                order = order,
                onViewAllOrders = {
                    selectedHomeTab = HomeTab.ORDERS
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.first()
            OrderDetailScreen(
                order = order,
                onBack = { navController.popBackStack() },
                onStartVerifyPickup = { navController.navigate(Routes.verifyPickup(order.id)) },
                onReportIssue = { navController.navigate(Routes.reportIssue(order.id)) },
            )
        }

        composable(
            route = Routes.VERIFY_PICKUP,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.first()
            VerifyPickupScreen(
                order = order,
                onBack = { navController.popBackStack() },
                onConfirmRelease = {
                    viewModel.completePickup(order.id)
                    navController.navigate(Routes.rating(order.id)) {
                        popUpTo(Routes.VERIFY_PICKUP) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.RATING,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.first()
            RatingScreen(
                order = order,
                onSubmitRating = { stars ->
                    viewModel.submitRating(order.id, stars)
                    selectedHomeTab = HomeTab.ORDERS
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.REPORT_ISSUE,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = viewModel.findOrder(orderId) ?: uiState.orders.first()
            ReportIssueScreen(
                order = order,
                onBack = { navController.popBackStack() },
                onSubmitReport = { reason ->
                    viewModel.submitIssueReport(order.id, reason)
                    navController.popBackStack()
                },
            )
        }
    }
}
