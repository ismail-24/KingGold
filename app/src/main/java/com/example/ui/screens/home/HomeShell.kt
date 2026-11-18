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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Sell
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

enum class HomeTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
) {
    DISCOVER("home", "استكشف", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_discover"),
    VAULT("vault", "خزنتي", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "tab_vault"),
    SELL_GOLD("sell_gold", "بيع ذهبك", Icons.Filled.Sell, Icons.Outlined.Sell, "tab_sell_gold"),
    ORDERS("orders", "طلباتي", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "tab_orders"),
    ACCOUNT("account", "حسابي", Icons.Filled.Person, Icons.Outlined.Person, "tab_account"),
    ;

    companion object {
        fun fromRoute(route: String?): HomeTab? = entries.firstOrNull { it.route == route }
    }
}

@Composable
fun HomeNavigationShell(
    selectedTab: HomeTab?,
    onSelectTab: (HomeTab) -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Parchment),
            ) {
                if (selectedTab != null) {
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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = AppText.body.copy(
                                            fontSize = 10.5.sp,
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
                }
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        } else {
            Scaffold(
                containerColor = AppColors.Parchment,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (selectedTab != null) {
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
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = AppText.body.copy(
                                                fontSize = 10.sp,
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
                    }
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    content()
                }
            }
        }
    }
}
