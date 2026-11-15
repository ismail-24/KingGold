package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoldOrder
import com.example.model.Shop
import com.example.model.ShopSortOption
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

enum class HomeTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
) {
    DISCOVER("استكشف", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_discover"),
    ORDERS("طلباتي", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_orders"),
    SUPPORT("الدعم", Icons.Filled.SupportAgent, Icons.Outlined.SupportAgent, "tab_support"),
    ACCOUNT("حسابي", Icons.Filled.Person, Icons.Outlined.Person, "tab_account"),
}

@Composable
fun HomeShell(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    uiState: KingGoldUiState,
    onSearchChange: (String) -> Unit,
    onApplyFilters: (karat: Int?, verifiedOnly: Boolean, sort: ShopSortOption) -> Unit,
    onOpenPriceInsight: () -> Unit,
    onOpenAiAdvisor: (String?) -> Unit,
    onRefreshGlobalGoldPrices: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenShop: (Shop) -> Unit,
    onToggleFavoriteShop: (String) -> Unit,
    onToggleFavoritesOnlyFilter: (Boolean) -> Unit,
    onOpenOrder: (GoldOrder) -> Unit,
    onOpenKyc: () -> Unit,
    onSelectPaymentMethod: (String) -> Unit,
    onDismissSyncError: () -> Unit,
    onLogout: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Parchment),
            ) {
                NavigationRail(
                    containerColor = Color.White,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    HomeTab.entries.forEach { tab ->
                        val selected = tab == selectedTab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onSelectTab(tab) },
                            modifier = Modifier.testTag(tab.tag),
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = AppText.body.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = AppColors.Brass,
                                selectedTextColor = AppColors.Brass,
                                unselectedIconColor = AppColors.Muted,
                                unselectedTextColor = AppColors.Muted,
                                indicatorColor = AppColors.Brass.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    HomeTabContent(
                        selectedTab = selectedTab,
                        uiState = uiState,
                        onSearchChange = onSearchChange,
                        onApplyFilters = onApplyFilters,
                        onOpenPriceInsight = onOpenPriceInsight,
                        onOpenAiAdvisor = onOpenAiAdvisor,
                        onRefreshGlobalGoldPrices = onRefreshGlobalGoldPrices,
                        onOpenCart = onOpenCart,
                        onOpenShop = onOpenShop,
                        onToggleFavoriteShop = onToggleFavoriteShop,
                        onToggleFavoritesOnlyFilter = onToggleFavoritesOnlyFilter,
                        onOpenOrder = onOpenOrder,
                        onOpenKyc = onOpenKyc,
                        onSelectPaymentMethod = onSelectPaymentMethod,
                        onDismissSyncError = onDismissSyncError,
                        onLogout = onLogout,
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = AppColors.Parchment,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        HomeTab.entries.forEach { tab ->
                            val selected = tab == selectedTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                modifier = Modifier.testTag(tab.tag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = AppText.body.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.Brass,
                                    selectedTextColor = AppColors.Brass,
                                    unselectedIconColor = AppColors.Muted,
                                    unselectedTextColor = AppColors.Muted,
                                    indicatorColor = AppColors.Brass.copy(alpha = 0.15f),
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    HomeTabContent(
                        selectedTab = selectedTab,
                        uiState = uiState,
                        onSearchChange = onSearchChange,
                        onApplyFilters = onApplyFilters,
                        onOpenPriceInsight = onOpenPriceInsight,
                        onOpenAiAdvisor = onOpenAiAdvisor,
                        onRefreshGlobalGoldPrices = onRefreshGlobalGoldPrices,
                        onOpenCart = onOpenCart,
                        onOpenShop = onOpenShop,
                        onToggleFavoriteShop = onToggleFavoriteShop,
                        onToggleFavoritesOnlyFilter = onToggleFavoritesOnlyFilter,
                        onOpenOrder = onOpenOrder,
                        onOpenKyc = onOpenKyc,
                        onSelectPaymentMethod = onSelectPaymentMethod,
                        onDismissSyncError = onDismissSyncError,
                        onLogout = onLogout,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    selectedTab: HomeTab,
    uiState: KingGoldUiState,
    onSearchChange: (String) -> Unit,
    onApplyFilters: (karat: Int?, verifiedOnly: Boolean, sort: ShopSortOption) -> Unit,
    onOpenPriceInsight: () -> Unit,
    onOpenAiAdvisor: (String?) -> Unit,
    onRefreshGlobalGoldPrices: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenShop: (Shop) -> Unit,
    onToggleFavoriteShop: (String) -> Unit,
    onToggleFavoritesOnlyFilter: (Boolean) -> Unit,
    onOpenOrder: (GoldOrder) -> Unit,
    onOpenKyc: () -> Unit,
    onSelectPaymentMethod: (String) -> Unit,
    onDismissSyncError: () -> Unit,
    onLogout: () -> Unit,
) {
    when (selectedTab) {
        HomeTab.DISCOVER -> DiscoverScreen(
            uiState = uiState,
            onSearchChange = onSearchChange,
            onApplyFilters = onApplyFilters,
            onOpenPriceInsight = onOpenPriceInsight,
            onOpenAiAdvisor = onOpenAiAdvisor,
            onRefreshGlobalGoldPrices = onRefreshGlobalGoldPrices,
            onOpenCart = onOpenCart,
            onOpenShop = onOpenShop,
            onToggleFavoriteShop = onToggleFavoriteShop,
            onToggleFavoritesOnlyFilter = onToggleFavoritesOnlyFilter,
            onDismissSyncError = onDismissSyncError,
        )
        HomeTab.ORDERS -> OrdersScreen(
            uiState = uiState,
            onOrderClick = onOpenOrder,
        )
        HomeTab.SUPPORT -> SupportScreen(
            onOpenAiAdvisor = { onOpenAiAdvisor(null) },
        )
        HomeTab.ACCOUNT -> AccountScreen(
            uiState = uiState,
            onOpenKyc = onOpenKyc,
            onOpenPriceAlerts = onOpenPriceInsight,
            onSelectPaymentMethod = onSelectPaymentMethod,
            onLogout = onLogout,
        )
    }
}
