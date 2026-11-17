package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notifications.FcmRegistrationManager
import com.example.ui.screens.admin.AdminApprovalScreen
import com.example.ui.screens.admin.AdminControlCenterScreen
import com.example.ui.screens.ai.AiAdvisorScreen
import com.example.ui.screens.auth.KycScreen
import com.example.ui.screens.cart.CartScreen
import com.example.ui.screens.checkout.CheckoutScreen
import com.example.ui.screens.checkout.SuccessScreen
import com.example.ui.screens.home.HomeShell
import com.example.ui.screens.home.HomeTab
import com.example.ui.screens.home.ProfileScreen
import com.example.ui.screens.home.SupportScreen
import com.example.ui.screens.merchant.MerchantPortalScreen
import com.example.ui.screens.merchant.MerchantRegistrationScreen
import com.example.ui.screens.orders.OrderDetailScreen
import com.example.ui.screens.orders.RatingScreen
import com.example.ui.screens.orders.ReportIssueScreen
import com.example.ui.screens.orders.VerifyPickupScreen
import com.example.ui.screens.price.PriceInsightScreen
import com.example.ui.screens.shop.ProductDetailScreen
import com.example.ui.screens.shop.ProductListScreen
import com.example.ui.screens.shop.ShopDetailScreen
import com.example.ui.state.AdminApprovalViewModel
import com.example.ui.state.KingGoldViewModel
import com.example.ui.state.MerchantRegistrationViewModel
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val KYC = "kyc"
    const val SUPPORT = "support"
    const val MERCHANT_PORTAL = "merchant_portal"
    const val MERCHANT_REGISTRATION = "merchant_registration"
    const val ADMIN_CENTER = "admin_center"
    const val ADMIN_APPROVAL = "admin_approval"
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
    merchantRegistrationViewModel: MerchantRegistrationViewModel? = null,
    adminApprovalViewModel: AdminApprovalViewModel? = null,
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
                onOpenProfile = { navController.navigateSingleTop(Routes.PROFILE) },
                onOpenMerchantPortal = { navController.navigateSingleTop(Routes.MERCHANT_PORTAL) },
                onOpenMerchantRegistration = { navController.navigateSingleTop(Routes.MERCHANT_REGISTRATION) },
                onOpenAdminCenter = { navController.navigateSingleTop(Routes.ADMIN_CENTER) },
                onOpenAdminApproval = { navController.navigateSingleTop(Routes.ADMIN_APPROVAL) },
                onOpenSupport = { navController.navigateSingleTop(Routes.SUPPORT) },
                onBuyFractionalGold = viewModel::buyFractionalGold,
                onRequestWithdrawal = viewModel::requestPhysicalWithdrawal,
                onSubmitDepositRequest = viewModel::submitDepositRequest,
                onSubmitSellGoldRequest = viewModel::submitSellGoldRequest,
                onCancelSellGoldRequest = viewModel::cancelSellGoldRequest,
                onAcceptSellGoldOffer = viewModel::acceptSellGoldOffer,
                onClearActionMessages = viewModel::clearActionMessages,
                onOpenKyc = { navController.navigateSingleTop(Routes.KYC) },
                onSelectPaymentMethod = viewModel::selectPaymentMethod,
                onDismissSyncError = viewModel::clearSyncError,
                onLogout = onSignOut,
            )
        }

        composable(Routes.SUPPORT) {
            SupportScreen(
                onOpenAiAdvisor = { navController.navigateSingleTop(Routes.aiAdvisor(null)) },
                onBack = { navController.popBackSafely() },
            )
        }

        composable(Routes.MERCHANT_PORTAL) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MerchantPortalScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onLoadMerchantDetails = viewModel::loadMerchantInspectionDetails,
                onSubmitRegistration = viewModel::submitMerchantRegistration,
                onSaveProduct = viewModel::saveMerchantProduct,
                onToggleProductPause = viewModel::toggleMerchantProductPause,
                onUpdateOrderStatus = viewModel::updateMerchantOrderStatus,
                onClearMessages = viewModel::clearActionMessages,
                onOpenDedicatedRegistration = { navController.navigateSingleTop(Routes.MERCHANT_REGISTRATION) },
            )
        }

        composable(Routes.MERCHANT_REGISTRATION) {
            val regViewModel = merchantRegistrationViewModel ?: remember { MerchantRegistrationViewModel() }
            MerchantRegistrationScreen(
                viewModel = regViewModel,
                onBack = { navController.popBackSafely() },
                onOpenMerchantPortal = {
                    navController.navigate(Routes.MERCHANT_PORTAL) {
                        popUpTo(Routes.MERCHANT_REGISTRATION) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.ADMIN_CENTER) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AdminControlCenterScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onLoadMerchantDetails = viewModel::loadMerchantInspectionDetails,
                onReviewMerchant = viewModel::adminReviewMerchant,
                onReviewProduct = viewModel::adminReviewProduct,
                onEvaluateSellGold = viewModel::adminEvaluateSellGold,
                onRejectSellGold = viewModel::adminRejectSellGold,
                onProcessDeposit = viewModel::adminApproveOrRejectDeposit,
                onProcessWithdrawal = viewModel::adminProcessWithdrawal,
                onProcessFractional = viewModel::adminConfirmOrRejectFractional,
                onUpdatePricing = viewModel::adminUpdatePricingConfig,
                onClearMessages = viewModel::clearActionMessages,
                onOpenPendingApprovalScreen = { navController.navigateSingleTop(Routes.ADMIN_APPROVAL) },
            )
        }

        composable(Routes.ADMIN_APPROVAL) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val approvalVm = adminApprovalViewModel ?: remember(uiState.isAdmin) {
                AdminApprovalViewModel(isAdmin = uiState.isAdmin)
            }
            AdminApprovalScreen(
                viewModel = approvalVm,
                onBack = { navController.popBackSafely() },
            )
        }

        composable(Routes.PROFILE) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ProfileScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onOrderClick = { order -> navController.navigateSingleTop(Routes.orderDetail(order.id)) },
                onSignOut = onSignOut,
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
                products = uiState.productsForShop(shop.id),
                prices = uiState.prices,
                isFavorite = uiState.favoriteShopIds.contains(shop.id),
                cartItemCount = uiState.cartItemCount,
                onToggleFavorite = { viewModel.toggleFavoriteShop(shop.id) },
                onOpenCart = { navController.navigateSingleTop(Routes.CART) },
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
            val shop = viewModel.findShop(shopId)
            val product = shop?.let { s ->
                uiState.productsForShop(s.id).firstOrNull { it.id == productId }
            }
            if (shop == null || product == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.Parchment)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "هذا المنتج غير متاح حالياً أو تم تحديث مخزونه",
                            style = AppText.display.copy(fontSize = 18.sp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { navController.popBackSafely() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Ink,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("رجوع للمتجر", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        }
                    }
                }
                return@composable
            }
            ProductDetailScreen(
                shop = shop,
                product = product,
                gramPrice = uiState.gramPrice(product.karat),
                cartItemCount = uiState.cartItemCount,
                onBack = { navController.popBackSafely() },
                onAddToCart = { viewModel.addToCart(shop, product) },
                onOpenCart = { navController.navigateSingleTop(Routes.CART) },
                onAskAiAboutProduct = {
                    val prompt = "هل قطعة ${product.name} (${product.weightGrams} جم عيار ${product.karat}) من محل ${shop.name} بالسعر الشامل بدون مصنعية إضافية مناسبة للشراء الآن؟"
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
