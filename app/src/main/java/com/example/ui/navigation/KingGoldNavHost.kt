package com.example.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notifications.FcmRegistrationManager
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.home.AccountScreen
import com.example.ui.screens.home.DiscoverScreen
import com.example.ui.screens.home.HomeNavigationShell
import com.example.ui.screens.home.HomeTab
import com.example.ui.screens.home.OrdersScreen
import com.example.ui.screens.sell.SellGoldScreen
import com.example.ui.screens.vault.VaultWalletScreen
import com.example.ui.state.AdminApprovalViewModel
import com.example.ui.state.KingGoldViewModel
import com.example.ui.state.MerchantRegistrationViewModel
import com.example.ui.theme.AppColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun KingGoldNavHost(
    viewModel: KingGoldViewModel? = null,
    onSignOut: () -> Unit,
    merchantRegistrationViewModel: MerchantRegistrationViewModel? = null,
    adminApprovalViewModel: AdminApprovalViewModel? = null,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME,
    isAuthenticated: Boolean = viewModel != null,
    pendingRoute: String? = null,
    onPendingRouteConsumed: () -> Unit = {},
    splashContent: @Composable (onFinished: () -> Unit) -> Unit = { onFinished ->
        SplashScreen(onFinished = onFinished)
    },
    loginContent: @Composable (onAuthSuccess: () -> Unit) -> Unit = { onAuthSuccess ->
        LoginScreen(onAuthSuccess = onAuthSuccess)
    },
) {
    val context = LocalContext.current
    var splashCompleted by rememberSaveable(startDestination) {
        mutableStateOf(startDestination != Routes.SPLASH)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentHomeTab = remember(currentRoute) { HomeTab.fromRoute(currentRoute) }

    if (viewModel != null) {
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
    }

    LaunchedEffect(isAuthenticated, viewModel, splashCompleted, currentRoute) {
        if (!splashCompleted) return@LaunchedEffect
        if (!isAuthenticated || viewModel == null) {
            if (currentRoute != null && currentRoute != Routes.LOGIN && currentRoute != Routes.SPLASH) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            if (currentRoute == Routes.LOGIN || currentRoute == Routes.SPLASH) {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(pendingRoute, isAuthenticated, viewModel, splashCompleted, currentRoute) {
        val routeToOpen = pendingRoute
        if (routeToOpen != null &&
            splashCompleted &&
            isAuthenticated &&
            viewModel != null &&
            currentRoute != null &&
            currentRoute != Routes.SPLASH &&
            currentRoute != Routes.LOGIN
        ) {
            navController.navigateInternalRoute(routeToOpen)
            onPendingRouteConsumed()
        }
    }

    HomeNavigationShell(
        selectedTab = if (isAuthenticated && viewModel != null) currentHomeTab else null,
        onSelectTab = { nextTab ->
            viewModel?.clearActionMessages()
            navController.navigateToHomeTab(nextTab)
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Routes.SPLASH) {
                splashContent {
                    splashCompleted = true
                    val target = if (isAuthenticated && viewModel != null) Routes.HOME else Routes.LOGIN
                    navController.navigate(target) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            composable(Routes.LOGIN) {
                loginContent {
                    splashCompleted = true
                    if (isAuthenticated && viewModel != null) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }

            composable(
                route = Routes.HOME,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                AuthenticatedRoute(viewModel = viewModel) { vm ->
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    DiscoverScreen(
                        uiState = uiState,
                        onSearchChange = vm::updateSearchQuery,
                        onApplyFilters = vm::updateFilters,
                        onOpenPriceInsight = { navController.navigateSingleTop(Routes.PRICE_INSIGHT) },
                        onOpenAiAdvisor = { prompt -> navController.navigateSingleTop(Routes.aiAdvisor(prompt)) },
                        onRefreshGlobalGoldPrices = vm::refreshGlobalGoldPrices,
                        onOpenCart = { navController.navigateSingleTop(Routes.CART) },
                        onOpenShop = { shop -> navController.navigateSingleTop(Routes.shopDetail(shop.id)) },
                        onToggleFavoriteShop = vm::toggleFavoriteShop,
                        onToggleFavoritesOnlyFilter = vm::toggleFavoritesOnlyFilter,
                        onOpenProfile = { navController.navigateSingleTop(Routes.PROFILE) },
                        onOpenVault = {
                            vm.clearActionMessages()
                            navController.navigateToHomeTab(HomeTab.VAULT)
                        },
                        onOpenSellGold = {
                            vm.clearActionMessages()
                            navController.navigateToHomeTab(HomeTab.SELL_GOLD)
                        },
                        onOpenMerchantPortal = { navController.navigateSingleTop(Routes.MERCHANT_PORTAL) },
                        onOpenAdminCenter = { navController.navigateSingleTop(Routes.ADMIN_CENTER) },
                        onOpenSupport = { navController.navigateSingleTop(Routes.SUPPORT) },
                        onDismissSyncError = vm::clearSyncError,
                    )
                }
            }

            composable(
                route = Routes.VAULT,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                AuthenticatedRoute(viewModel = viewModel) { vm ->
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    VaultWalletScreen(
                        uiState = uiState,
                        onBuyFractionalGold = { karat, weightMg, payWithWallet, onSuccess ->
                            vm.buyFractionalGold(
                                karat = karat,
                                weightMg = weightMg,
                                payWithWallet = payWithWallet,
                                onSuccess = { onSuccess() },
                            )
                        },
                        onRequestWithdrawal = { karat, amountMg, deliveryType, address, onSuccess ->
                            vm.requestPhysicalWithdrawal(
                                karat = karat,
                                amountMg = amountMg,
                                deliveryType = deliveryType,
                                deliveryAddress = address,
                                onSuccess = { onSuccess() },
                            )
                        },
                        onSubmitDepositRequest = vm::submitDepositRequest,
                        onClearMessages = vm::clearActionMessages,
                    )
                }
            }

            composable(
                route = Routes.SELL_GOLD,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                AuthenticatedRoute(viewModel = viewModel) { vm ->
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    SellGoldScreen(
                        uiState = uiState,
                        onSubmitRequest = vm::submitSellGoldRequest,
                        onCancelRequest = vm::cancelSellGoldRequest,
                        onAcceptOffer = vm::acceptSellGoldOffer,
                        onClearMessages = vm::clearActionMessages,
                    )
                }
            }

            composable(
                route = Routes.ORDERS,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                AuthenticatedRoute(viewModel = viewModel) { vm ->
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    OrdersScreen(
                        uiState = uiState,
                        onOrderClick = { order -> navController.navigateSingleTop(Routes.orderDetail(order.id)) },
                    )
                }
            }

            composable(
                route = Routes.ACCOUNT,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                AuthenticatedRoute(viewModel = viewModel) { vm ->
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    AccountScreen(
                        uiState = uiState,
                        onOpenProfile = { navController.navigateSingleTop(Routes.PROFILE) },
                        onOpenVault = {
                            vm.clearActionMessages()
                            navController.navigateToHomeTab(HomeTab.VAULT)
                        },
                        onOpenSellGold = {
                            vm.clearActionMessages()
                            navController.navigateToHomeTab(HomeTab.SELL_GOLD)
                        },
                        onOpenMerchantPortal = { navController.navigateSingleTop(Routes.MERCHANT_PORTAL) },
                        onOpenMerchantRegistration = { navController.navigateSingleTop(Routes.MERCHANT_REGISTRATION) },
                        onOpenAdminCenter = { navController.navigateSingleTop(Routes.ADMIN_CENTER) },
                        onOpenAdminApproval = { navController.navigateSingleTop(Routes.ADMIN_APPROVAL) },
                        onOpenSupport = { navController.navigateSingleTop(Routes.SUPPORT) },
                        onOpenKyc = { navController.navigateSingleTop(Routes.KYC) },
                        onOpenPriceAlerts = { navController.navigateSingleTop(Routes.PRICE_INSIGHT) },
                        onSelectPaymentMethod = vm::selectPaymentMethod,
                        onLogout = onSignOut,
                    )
                }
            }

            registerSecondaryDestinations(
                navController = navController,
                viewModelProvider = { viewModel },
                merchantRegistrationViewModel = merchantRegistrationViewModel,
                adminApprovalViewModel = adminApprovalViewModel,
                onSignOut = onSignOut,
            )
        }
    }
}

@Composable
internal inline fun AuthenticatedRoute(
    viewModel: KingGoldViewModel?,
    content: @Composable (KingGoldViewModel) -> Unit,
) {
    if (viewModel == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Parchment),
        )
    } else {
        content(viewModel)
    }
}
