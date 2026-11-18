package com.example.ui.navigation

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.admin.AdminApprovalScreen
import com.example.ui.screens.admin.AdminControlCenterScreen
import com.example.ui.screens.ai.AiAdvisorScreen
import com.example.ui.screens.auth.KycScreen
import com.example.ui.screens.cart.CartScreen
import com.example.ui.screens.checkout.CheckoutScreen
import com.example.ui.screens.checkout.SuccessScreen
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

internal fun NavGraphBuilder.registerSecondaryDestinations(
    navController: NavHostController,
    isAuthenticatedProvider: () -> Boolean,
    viewModelProvider: () -> KingGoldViewModel?,
    merchantRegistrationViewModel: MerchantRegistrationViewModel?,
    adminApprovalViewModel: AdminApprovalViewModel?,
    onSignOut: () -> Unit,
) {
    composable(Routes.SUPPORT) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) {
            SupportScreen(
                onOpenAiAdvisor = { navController.navigateSingleTop(Routes.aiAdvisor(null), isAuthorized) },
                onBack = { navController.popBackSafely() },
            )
        }
    }

    composable(Routes.MERCHANT_PORTAL) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            DisposableEffect(vm) {
                onDispose { vm.clearActionMessages() }
            }
            MerchantPortalScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onLoadMerchantDetails = vm::loadMerchantInspectionDetails,
                onSubmitRegistration = vm::submitMerchantRegistration,
                onSaveProduct = { merchant, existing, name, desc, cat, karat, w, pricePiasters, stock, stamped, thumbs, onSuccess ->
                    vm.saveMerchantProduct(
                        merchant = merchant,
                        existingProduct = existing,
                        name = name,
                        description = desc,
                        category = cat,
                        karat = karat,
                        weightGrams = w,
                        sellingPricePiasters = pricePiasters,
                        stockQuantity = stock,
                        stamped = stamped,
                        imageThumbnails = thumbs,
                        onSuccess = onSuccess,
                    )
                },
                onToggleProductPause = vm::toggleMerchantProductPause,
                onUpdateOrderStatus = vm::updateMerchantOrderStatus,
                onClearMessages = vm::clearActionMessages,
                onOpenDedicatedRegistration = { navController.navigateSingleTop(Routes.MERCHANT_REGISTRATION, isAuthorized) },
            )
        }
    }

    composable(Routes.MERCHANT_REGISTRATION) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) {
            val regViewModel = merchantRegistrationViewModel ?: viewModel {
                MerchantRegistrationViewModel()
            }
            MerchantRegistrationScreen(
                viewModel = regViewModel,
                onBack = { navController.popBackSafely() },
                onOpenMerchantPortal = {
                    if (!isAuthorized) {
                        navController.redirectToLogin()
                    } else if (!navController.popBackStack(Routes.MERCHANT_PORTAL, inclusive = false)) {
                        navController.navigate(Routes.MERCHANT_PORTAL) {
                            popUpTo(Routes.MERCHANT_REGISTRATION) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }

    composable(Routes.ADMIN_CENTER) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            DisposableEffect(vm) {
                onDispose { vm.clearActionMessages() }
            }
            AdminControlCenterScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onLoadMerchantDetails = vm::loadMerchantInspectionDetails,
                onReviewMerchant = vm::adminReviewMerchant,
                onReviewProduct = vm::adminReviewProduct,
                onEvaluateSellGold = vm::adminEvaluateSellGold,
                onRejectSellGold = vm::adminRejectSellGold,
                onProcessDeposit = vm::adminApproveOrRejectDeposit,
                onProcessWithdrawal = vm::adminProcessWithdrawal,
                onProcessFractional = vm::adminConfirmOrRejectFractional,
                onUpdatePricing = vm::adminUpdatePricingConfig,
                onClearMessages = vm::clearActionMessages,
                onOpenPendingApprovalScreen = { navController.navigateSingleTop(Routes.ADMIN_APPROVAL, isAuthorized) },
            )
        }
    }

    composable(Routes.ADMIN_APPROVAL) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val approvalVm = adminApprovalViewModel ?: viewModel(
                key = "admin_approval_${uiState.isAdmin}",
            ) {
                AdminApprovalViewModel(isAdmin = uiState.isAdmin)
            }
            AdminApprovalScreen(
                viewModel = approvalVm,
                onBack = { navController.popBackSafely() },
            )
        }
    }

    composable(Routes.PROFILE) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            ProfileScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onOrderClick = { order -> navController.navigateSingleTop(Routes.orderDetail(order.id), isAuthorized) },
                onSignOut = onSignOut,
            )
        }
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
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val initialPrompt = backStackEntry.arguments?.getString("prompt")
            AiAdvisorScreen(
                uiState = uiState,
                initialPrompt = initialPrompt,
                onBack = { navController.popBackSafely() },
            )
        }
    }

    composable(Routes.KYC) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            KycScreen(
                frontDone = uiState.kycFrontDone,
                backDone = uiState.kycBackDone,
                selfieDone = uiState.kycSelfieDone,
                onStepCompleted = vm::toggleKycStep,
                onSubmitKyc = vm::submitKyc,
                onBack = { navController.popBackSafely() },
            )
        }
    }

    composable(Routes.PRICE_INSIGHT) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            PriceInsightScreen(
                uiState = uiState,
                onSaveAlert = vm::updatePriceAlert,
                onOpenAiAdvisor = { prompt -> navController.navigateSingleTop(Routes.aiAdvisor(prompt), isAuthorized) },
                onBack = { navController.popBackSafely() },
            )
        }
    }

    composable(
        route = Routes.SHOP_DETAIL,
        arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val shop = vm.findShop(shopId)
            if (shop == null) {
                RouteNotFoundFallback(
                    message = "هذا المتجر غير متاح حالياً أو تم تحديث بياناته",
                    onBack = { navController.popBackSafely() },
                )
                return@AuthenticatedRoute
            }
            ShopDetailScreen(
                shop = shop,
                products = uiState.productsForShop(shop.id),
                prices = uiState.prices,
                isFavorite = uiState.favoriteShopIds.contains(shop.id),
                cartItemCount = uiState.cartItemCount,
                onToggleFavorite = { vm.toggleFavoriteShop(shop.id) },
                onOpenCart = { navController.navigateSingleTop(Routes.CART, isAuthorized) },
                onBack = { navController.popBackSafely() },
                onBrowseProducts = { category ->
                    navController.navigateSingleTop(Routes.productList(shop.id, category), isAuthorized)
                },
                onOpenProduct = { product ->
                    navController.navigateSingleTop(Routes.productDetail(shop.id, product.id), isAuthorized)
                },
                onOpenPriceInsight = { navController.navigateSingleTop(Routes.PRICE_INSIGHT, isAuthorized) },
            )
        }
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
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val category = backStackEntry.arguments?.getString("category")
            val shop = vm.findShop(shopId)
            if (shop == null) {
                RouteNotFoundFallback(
                    message = "هذا المتجر غير متاح حالياً أو تم تحديث بياناته",
                    onBack = { navController.popBackSafely() },
                )
                return@AuthenticatedRoute
            }
            ProductListScreen(
                shop = shop,
                initialCategory = category,
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onOpenProduct = { product ->
                    navController.navigateSingleTop(Routes.productDetail(shop.id, product.id), isAuthorized)
                },
                onAddToCart = { product -> vm.addToCart(shop, product) },
                onOpenCart = { navController.navigateSingleTop(Routes.CART, isAuthorized) },
            )
        }
    }

    composable(
        route = Routes.PRODUCT_DETAIL,
        arguments = listOf(
            navArgument("shopId") { type = NavType.StringType },
            navArgument("productId") { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()
            val shop = vm.findShop(shopId)
            val product = shop?.let { s ->
                uiState.productsForShop(s.id).firstOrNull { it.id == productId }
            }
            if (shop == null || product == null) {
                RouteNotFoundFallback(
                    message = "هذا المنتج غير متاح حالياً أو تم تحديث مخزونه",
                    buttonText = "رجوع للمتجر",
                    onBack = { navController.popBackSafely() },
                )
                return@AuthenticatedRoute
            }
            ProductDetailScreen(
                shop = shop,
                product = product,
                gramPrice = uiState.gramPrice(product.karat),
                cartItemCount = uiState.cartItemCount,
                onBack = { navController.popBackSafely() },
                onAddToCart = { vm.addToCart(shop, product) },
                onOpenCart = { navController.navigateSingleTop(Routes.CART, isAuthorized) },
                onAskAiAboutProduct = {
                    val prompt = "هل قطعة ${product.name} (${product.weightGrams} جم عيار ${product.karat}) من محل ${shop.name} بالسعر الشامل بدون مصنعية إضافية مناسبة للشراء الآن؟"
                    navController.navigateSingleTop(Routes.aiAdvisor(prompt), isAuthorized)
                },
            )
        }
    }

    composable(Routes.CART) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            CartScreen(
                uiState = uiState,
                onBack = { navController.popBackSafely() },
                onIncrement = vm::incrementCartItem,
                onDecrement = vm::decrementCartItem,
                onRemove = vm::removeFromCart,
                onSelectPickupSlot = vm::selectPickupSlot,
                onRefreshPriceLock = vm::refreshPriceLock,
                onProceedToCheckout = { navController.navigateSingleTop(Routes.CHECKOUT, isAuthorized) },
                onBrowseShops = {
                    navController.navigateToHomeTab(HomeTab.DISCOVER, isAuthenticated = isAuthorized, clearBackToHome = true)
                },
            )
        }
    }

    composable(Routes.CHECKOUT) {
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            DisposableEffect(vm) {
                onDispose { vm.clearActionMessages() }
            }
            CheckoutScreen(
                uiState = uiState,
                onBack = {
                    vm.clearActionMessages()
                    navController.popBackSafely()
                },
                onSelectPaymentMethod = vm::selectPaymentMethod,
                onConfirmPay = {
                    if (!isAuthorized) {
                        navController.redirectToLogin()
                    } else {
                        val created = vm.placeOrder()
                        if (created != null) {
                            navController.navigate(Routes.success(created.id)) {
                                popUpTo(Routes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                },
            )
        }
    }

    composable(
        route = Routes.SUCCESS,
        arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = vm.findOrder(orderId)
            if (order == null) {
                RouteNotFoundFallback(
                    message = "بيانات هذا الطلب غير متوفرة حالياً",
                    onBack = { navController.popBackSafely() },
                )
                return@AuthenticatedRoute
            }
            SuccessScreen(
                order = order,
                onViewAllOrders = {
                    navController.navigateToHomeTab(HomeTab.ORDERS, isAuthenticated = isAuthorized, clearBackToHome = true)
                },
            )
        }
    }

    composable(
        route = Routes.ORDER_DETAIL,
        arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = vm.findOrder(orderId)
            if (order == null) {
                RouteNotFoundFallback(
                    message = "هذا الطلب غير متاح أو تم تحديث حالته",
                    onBack = { navController.popBackSafely() },
                )
                return@AuthenticatedRoute
            }
            OrderDetailScreen(
                order = order,
                onBack = { navController.popBackSafely() },
                onStartVerifyPickup = { navController.navigateSingleTop(Routes.verifyPickup(order.id), isAuthorized) },
                onReportIssue = { navController.navigateSingleTop(Routes.reportIssue(order.id), isAuthorized) },
            )
        }
    }

    composable(
        route = Routes.VERIFY_PICKUP,
        arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = vm.findOrder(orderId)
            if (order == null) {
                RouteNotFoundFallback(
                    message = "تعذر العثور على تفاصيل هذا الطلب للاستلام",
                    onBack = { navController.popBackSafely() },
                )
                return@AuthenticatedRoute
            }
            VerifyPickupScreen(
                order = order,
                onBack = { navController.popBackSafely() },
                onConfirmRelease = {
                    vm.completePickup(order.id)
                    if (!isAuthorized) {
                        navController.redirectToLogin()
                    } else {
                        navController.navigate(Routes.rating(order.id)) {
                            popUpTo(Routes.VERIFY_PICKUP) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }

    composable(
        route = Routes.RATING,
        arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = vm.findOrder(orderId)
            if (order == null) {
                RouteNotFoundFallback(
                    message = "بيانات الطلب غير متاحة للتقييم",
                    onBack = { navController.popBackSafely() },
                )
                return@AuthenticatedRoute
            }
            RatingScreen(
                order = order,
                onSubmitRating = { stars ->
                    vm.submitRating(order.id, stars)
                    navController.navigateToHomeTab(HomeTab.ORDERS, isAuthenticated = isAuthorized, clearBackToHome = true)
                },
                onSkip = {
                    navController.navigateToHomeTab(HomeTab.ORDERS, isAuthenticated = isAuthorized, clearBackToHome = true)
                },
            )
        }
    }

    composable(
        route = Routes.REPORT_ISSUE,
        arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val isAuthorized = isAuthenticatedProvider()
        AuthenticatedRoute(
            isAuthenticated = isAuthorized,
            viewModel = viewModelProvider(),
            onUnauthorized = { navController.redirectToLogin() },
        ) { vm ->
            val orderId = backStackEntry.arguments?.getString("orderId").orEmpty()
            val order = vm.findOrder(orderId)
            if (order == null) {
                RouteNotFoundFallback(
                    message = "بيانات الطلب غير متاحة للإبلاغ",
                    onBack = { navController.popBackSafely() },
                )
                return@AuthenticatedRoute
            }
            ReportIssueScreen(
                order = order,
                onBack = { navController.popBackSafely() },
                onSubmitReport = { reason ->
                    vm.submitIssueReport(order.id, reason)
                    navController.popBackSafely()
                },
            )
        }
    }
}

@Composable
private fun RouteNotFoundFallback(
    message: String,
    buttonText: String = "رجوع",
    onBack: () -> Unit,
) {
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
                text = message,
                style = AppText.display.copy(fontSize = 18.sp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Ink,
                    contentColor = Color.White,
                ),
            ) {
                Text(buttonText, style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
            }
        }
    }
}
