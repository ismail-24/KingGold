package com.example.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.KingGoldRepository
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.auth.KycScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.OtpScreen
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
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val OTP = "otp"
    const val HOME = "home"
    const val KYC = "kyc"
    const val PRICE_INSIGHT = "price_insight"
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
    viewModel: KingGoldViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedHomeTab by remember { mutableStateOf(HomeTab.DISCOVER) }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                phoneNumber = uiState.phoneNumber,
                onPhoneChange = viewModel::updatePhoneNumber,
                onContinue = { navController.navigate(Routes.OTP) },
            )
        }

        composable(Routes.OTP) {
            OtpScreen(
                phoneNumber = uiState.phoneNumber,
                otpCode = uiState.otpCode,
                onOtpChange = viewModel::updateOtpCode,
                onFillDemoOtp = viewModel::fillDemoOtp,
                onBack = { navController.popBackStack() },
                onVerifySuccess = {
                    selectedHomeTab = HomeTab.DISCOVER
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeShell(
                selectedTab = selectedHomeTab,
                onSelectTab = { selectedHomeTab = it },
                uiState = uiState,
                onSearchChange = viewModel::updateSearchQuery,
                onApplyFilters = viewModel::updateFilters,
                onOpenPriceInsight = { navController.navigate(Routes.PRICE_INSIGHT) },
                onRefreshGlobalGoldPrices = viewModel::refreshGlobalGoldPrices,
                onOpenCart = { navController.navigate(Routes.CART) },
                onOpenShop = { shop -> navController.navigate(Routes.shopDetail(shop.id)) },
                onOpenOrder = { order -> navController.navigate(Routes.orderDetail(order.id)) },
                onOpenKyc = { navController.navigate(Routes.KYC) },
                onSelectPaymentMethod = viewModel::selectPaymentMethod,
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
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
