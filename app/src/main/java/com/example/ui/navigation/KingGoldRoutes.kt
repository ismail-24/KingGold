package com.example.ui.navigation

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.example.ui.screens.home.HomeTab

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val VAULT = "vault"
    const val SELL_GOLD = "sell_gold"
    const val ORDERS = "orders"
    const val ACCOUNT = "account"
    const val PROFILE = "profile"
    const val KYC = "kyc"
    const val SUPPORT = "support"
    const val MERCHANT_PORTAL = "merchant_portal"
    const val MERCHANT_REGISTRATION = "merchant_registration"
    const val ADMIN_CENTER = "admin_center"
    const val ADMIN_APPROVAL = "admin_approval"
    const val PRICE_INSIGHT = "price_insight"
    const val MARKET_NEWS = "market_news"
    const val GOLD_CALCULATOR = "gold_calculator"
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

    val publicRoutes = setOf(
        SPLASH,
        LOGIN,
    )

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

    private val staticInternalRoutes = setOf(
        HOME,
        VAULT,
        SELL_GOLD,
        ORDERS,
        ACCOUNT,
        PROFILE,
        KYC,
        SUPPORT,
        MERCHANT_PORTAL,
        MERCHANT_REGISTRATION,
        ADMIN_CENTER,
        ADMIN_APPROVAL,
        PRICE_INSIGHT,
        MARKET_NEWS,
        GOLD_CALCULATOR,
        CART,
        CHECKOUT,
        "ai_advisor",
    )

    fun isPublicRoute(route: String?): Boolean = route != null && route in publicRoutes

    fun isProtectedRoute(route: String?): Boolean = !route.isNullOrBlank() && !isPublicRoute(route)

    fun isSupportedInternalRoute(route: String): Boolean {
        if (route in staticInternalRoutes) return true
        return route.startsWith("ai_advisor?prompt=") ||
            route.startsWith("shop_detail/") ||
            route.startsWith("product_list/") ||
            route.startsWith("product_detail/") ||
            route.startsWith("success/") ||
            route.startsWith("order_detail/") ||
            route.startsWith("verify_pickup/") ||
            route.startsWith("rating/") ||
            route.startsWith("report_issue/")
    }
}

internal fun NavController.redirectToLogin() {
    if (currentDestination?.route == Routes.LOGIN) return
    navigate(Routes.LOGIN) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

internal fun NavController.navigateSingleTop(
    route: String,
    isAuthenticated: Boolean = true,
) {
    val canNavigate = currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
    if (!canNavigate) return
    if (Routes.isProtectedRoute(route) && !isAuthenticated) {
        redirectToLogin()
        return
    }
    if (currentDestination?.route == route) return
    navigate(route) {
        launchSingleTop = true
    }
}

internal fun NavController.navigateToHomeTab(
    tab: HomeTab,
    isAuthenticated: Boolean = true,
    clearBackToHome: Boolean = false,
) {
    val canNavigate = currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
    if (!canNavigate) return
    if (Routes.isProtectedRoute(tab.route) && !isAuthenticated) {
        redirectToLogin()
        return
    }
    if (!clearBackToHome && currentDestination?.route == tab.route) return
    navigate(tab.route) {
        popUpTo(Routes.HOME) {
            saveState = !clearBackToHome
        }
        launchSingleTop = true
        restoreState = !clearBackToHome
    }
}

internal fun NavController.navigateInternalRoute(
    route: String,
    isAuthenticated: Boolean = true,
) {
    if (Routes.isProtectedRoute(route) && !isAuthenticated) {
        redirectToLogin()
        return
    }
    val tab = HomeTab.fromRoute(route)
    if (tab != null) {
        navigateToHomeTab(tab, isAuthenticated = isAuthenticated)
    } else if (Routes.isSupportedInternalRoute(route)) {
        navigateSingleTop(route, isAuthenticated = isAuthenticated)
    }
}

internal fun NavController.popBackSafely() {
    val canPop = currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
    if (canPop && previousBackStackEntry != null) {
        popBackStack()
    }
}
