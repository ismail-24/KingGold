package com.example.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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
    val isAuthorized = isAuthenticated && viewModel != null
    val initialStartDestination = remember {
        if (Routes.isProtectedRoute(startDestination) && !isAuthorized) {
            Routes.LOGIN
        } else {
            startDestination
        }
    }
    var splashCompleted by rememberSaveable(initialStartDestination) {
        mutableStateOf(initialStartDestination != Routes.SPLASH)
    }
    var deferredProtectedRoute by rememberSaveable { mutableStateOf<String?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentHomeTab = remember(currentRoute) { HomeTab.fromRoute(currentRoute) }
    val currentAuthorized by rememberUpdatedState(isAuthorized)

    // Structural destination-level navigation guard inside NavHost:
    // Blocks direct navController.navigate(...) calls to any protected route when unauthenticated.
    DisposableEffect(navController) {
        val guardListener = NavController.OnDestinationChangedListener { controller, destination, _ ->
            val targetRoute = destination.route
            if (Routes.isProtectedRoute(targetRoute) && !currentAuthorized) {
                splashCompleted = true
                controller.redirectToLogin()
            } else if (targetRoute == Routes.LOGIN) {
                splashCompleted = true
            }
        }
        navController.addOnDestinationChangedListener(guardListener)
        onDispose {
            navController.removeOnDestinationChangedListener(guardListener)
        }
    }

    if (viewModel != null) {
        val alertConfig by remember(viewModel) {
            viewModel.uiState
                .map {
                    listOf(
                        it.priceAlertEnabled.toString(),
                        it.priceAlertKarat.toString(),
                        it.priceAlertAbove.toString(),
                        it.priceAlertTarget,
                    )
                }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = listOf("true", "24", "false", "6000"))

        LaunchedEffect(alertConfig) {
            FcmRegistrationManager.syncFcmAlertSubscription(
                context = context,
                enabled = alertConfig[0].toBoolean(),
                alertKarat = alertConfig[1].toIntOrNull() ?: 24,
                alertAbove = alertConfig[2].toBoolean(),
                alertTarget = alertConfig[3],
                onTokenSynced = viewModel::updateFcmTokenState,
            )
        }

        LaunchedEffect(viewModel) {
            com.example.notifications.KingGoldMessagingService.incomingEvents.collect { event ->
                viewModel.applyIncomingFcmEvent(event)
            }
        }
    }

    // Reactive auth guard across state changes (Sign In / Sign Out).
    LaunchedEffect(isAuthorized, splashCompleted, currentRoute) {
        if (!splashCompleted) return@LaunchedEffect
        if (!isAuthorized) {
            deferredProtectedRoute = null
            if (currentRoute != null && Routes.isProtectedRoute(currentRoute)) {
                navController.redirectToLogin()
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

    // Guarded notification/deep-link dispatcher: redirects unauthorized deep links to LOGIN first,
    // and replays deferred deep links cleanly once authenticated on HOME.
    LaunchedEffect(pendingRoute, deferredProtectedRoute, isAuthorized, splashCompleted, currentRoute) {
        if (!splashCompleted || currentRoute == null) return@LaunchedEffect
        val incomingRoute = pendingRoute
        if (incomingRoute != null) {
            if (Routes.isProtectedRoute(incomingRoute) && !isAuthorized) {
                if (Routes.isSupportedInternalRoute(incomingRoute)) {
                    deferredProtectedRoute = incomingRoute
                }
                onPendingRouteConsumed()
                navController.redirectToLogin()
                return@LaunchedEffect
            }
            if (isAuthorized && !Routes.isPublicRoute(currentRoute)) {
                navController.navigateInternalRoute(incomingRoute, isAuthenticated = isAuthorized)
                onPendingRouteConsumed()
                return@LaunchedEffect
            }
        }
        val deferred = deferredProtectedRoute
        if (deferred != null && isAuthorized && !Routes.isPublicRoute(currentRoute)) {
            deferredProtectedRoute = null
            navController.navigateInternalRoute(deferred, isAuthenticated = isAuthorized)
        }
    }

    HomeNavigationShell(
        selectedTab = if (isAuthorized) currentHomeTab else null,
        onSelectTab = { nextTab ->
            viewModel?.clearActionMessages()
            navController.navigateToHomeTab(nextTab, isAuthenticated = isAuthorized)
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = initialStartDestination,
        ) {
            composable(Routes.SPLASH) {
                splashContent {
                    splashCompleted = true
                    val target = if (isAuthorized) Routes.HOME else Routes.LOGIN
                    navController.navigate(target) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            composable(Routes.LOGIN) {
                loginContent {
                    splashCompleted = true
                    if (isAuthorized) {
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
                AuthenticatedRoute(
                    isAuthenticated = isAuthorized,
                    viewModel = viewModel,
                    onUnauthorized = { navController.redirectToLogin() },
                ) { vm ->
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    DiscoverScreen(
                        uiState = uiState,
                        onSearchChange = vm::updateSearchQuery,
                        onApplyFilters = vm::updateFilters,
                        onOpenPriceInsight = { navController.navigateSingleTop(Routes.PRICE_INSIGHT, isAuthorized) },
                        onOpenAiAdvisor = { prompt -> navController.navigateSingleTop(Routes.aiAdvisor(prompt), isAuthorized) },
                        onRefreshGlobalGoldPrices = vm::refreshGlobalGoldPrices,
                        onOpenCart = { navController.navigateSingleTop(Routes.CART, isAuthorized) },
                        onOpenShop = { shop -> navController.navigateSingleTop(Routes.shopDetail(shop.id), isAuthorized) },
                        onToggleFavoriteShop = vm::toggleFavoriteShop,
                        onToggleFavoritesOnlyFilter = vm::toggleFavoritesOnlyFilter,
                        onSelectCityFilter = vm::updateCityFilter,
                        onSelectCategoryFilter = vm::updateCategoryFilter,
                        onOpenProfile = { navController.navigateSingleTop(Routes.PROFILE, isAuthorized) },
                        onOpenVault = {
                            vm.clearActionMessages()
                            navController.navigateToHomeTab(HomeTab.VAULT, isAuthorized)
                        },
                        onOpenSellGold = {
                            vm.clearActionMessages()
                            navController.navigateToHomeTab(HomeTab.SELL_GOLD, isAuthorized)
                        },
                        onOpenGoldCalculator = { navController.navigateSingleTop(Routes.GOLD_CALCULATOR, isAuthorized) },
                        onOpenMarketNews = { navController.navigateSingleTop(Routes.MARKET_NEWS, isAuthorized) },
                        onSelectMarketNewsTopic = vm::selectMarketNewsTopic,
                        onRefreshMarketNews = { vm.refreshMarketNews() },
                        onOpenMerchantPortal = { navController.navigateSingleTop(Routes.MERCHANT_PORTAL, isAuthorized) },
                        onOpenAdminCenter = { navController.navigateSingleTop(Routes.ADMIN_CENTER, isAuthorized) },
                        onOpenSupport = { navController.navigateSingleTop(Routes.SUPPORT, isAuthorized) },
                        onDismissSyncError = vm::clearSyncError,
                        onUpdateSearchAnchor = vm::updateSearchAnchor,
                        onUpdateNearbyRadius = vm::updateNearbyRadiusKm,
                        onClearSearchAnchor = vm::clearSearchAnchor,
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
                AuthenticatedRoute(
                    isAuthenticated = isAuthorized,
                    viewModel = viewModel,
                    onUnauthorized = { navController.redirectToLogin() },
                ) { vm ->
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
                AuthenticatedRoute(
                    isAuthenticated = isAuthorized,
                    viewModel = viewModel,
                    onUnauthorized = { navController.redirectToLogin() },
                ) { vm ->
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
                AuthenticatedRoute(
                    isAuthenticated = isAuthorized,
                    viewModel = viewModel,
                    onUnauthorized = { navController.redirectToLogin() },
                ) { vm ->
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    OrdersScreen(
                        uiState = uiState,
                        onOrderClick = { order -> navController.navigateSingleTop(Routes.orderDetail(order.id), isAuthorized) },
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
                AuthenticatedRoute(
                    isAuthenticated = isAuthorized,
                    viewModel = viewModel,
                    onUnauthorized = { navController.redirectToLogin() },
                ) { vm ->
                    val uiState by vm.uiState.collectAsStateWithLifecycle()
                    AccountScreen(
                        uiState = uiState,
                        onOpenProfile = { navController.navigateSingleTop(Routes.PROFILE, isAuthorized) },
                        onOpenVault = {
                            vm.clearActionMessages()
                            navController.navigateToHomeTab(HomeTab.VAULT, isAuthorized)
                        },
                        onOpenSellGold = {
                            vm.clearActionMessages()
                            navController.navigateToHomeTab(HomeTab.SELL_GOLD, isAuthorized)
                        },
                        onOpenMerchantPortal = { navController.navigateSingleTop(Routes.MERCHANT_PORTAL, isAuthorized) },
                        onOpenMerchantRegistration = { navController.navigateSingleTop(Routes.MERCHANT_REGISTRATION, isAuthorized) },
                        onOpenAdminCenter = { navController.navigateSingleTop(Routes.ADMIN_CENTER, isAuthorized) },
                        onOpenAdminApproval = { navController.navigateSingleTop(Routes.ADMIN_APPROVAL, isAuthorized) },
                        onOpenSupport = { navController.navigateSingleTop(Routes.SUPPORT, isAuthorized) },
                        onOpenKyc = { navController.navigateSingleTop(Routes.KYC, isAuthorized) },
                        onOpenPriceAlerts = { navController.navigateSingleTop(Routes.PRICE_INSIGHT, isAuthorized) },
                        onSelectPaymentMethod = vm::selectPaymentMethod,
                        onLogout = onSignOut,
                    )
                }
            }

            registerSecondaryDestinations(
                navController = navController,
                isAuthenticatedProvider = { isAuthorized },
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
    isAuthenticated: Boolean,
    viewModel: KingGoldViewModel?,
    noinline onUnauthorized: () -> Unit,
    content: @Composable (KingGoldViewModel) -> Unit,
) {
    if (!isAuthenticated || viewModel == null) {
        LaunchedEffect(isAuthenticated, viewModel) {
            onUnauthorized()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Parchment),
        )
    } else {
        content(viewModel)
    }
}
