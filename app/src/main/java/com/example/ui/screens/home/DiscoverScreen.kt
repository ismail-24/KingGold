package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarketNewsTopic
import com.example.model.MoneyUtils
import com.example.model.ProductCategory
import com.example.model.Shop
import com.example.model.ShopSortOption
import com.example.ui.components.DailyGoldPriceTrendChartCard
import com.example.ui.components.GoldMarketplaceImage
import com.example.ui.components.GoldShopsMapView
import com.example.ui.components.MarketNewsSectionCard
import com.example.ui.components.RealTimeGlobalGoldPriceBanner
import com.example.ui.components.SealBadge
import com.example.ui.components.ShopGoldMedallion
import com.example.ui.components.VerifiedStrip
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.openDirections

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
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
    onSelectCityFilter: (String?) -> Unit = {},
    onSelectCategoryFilter: (String?) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSavedShops: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenSellGold: () -> Unit = {},
    onOpenNearbyMap: () -> Unit = {},
    onOpenGoldCalculator: () -> Unit = {},
    onOpenMarketNews: () -> Unit = {},
    onSelectMarketNewsTopic: (MarketNewsTopic) -> Unit = {},
    onRefreshMarketNews: () -> Unit = {},
    onOpenMerchantPortal: () -> Unit = {},
    onOpenAdminCenter: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onDismissSyncError: () -> Unit,
    onUpdateSearchAnchor: (Double, Double, String) -> Unit = { _, _, _ -> },
    onUpdateNearbyRadius: (Double?) -> Unit = {},
    onClearSearchAnchor: () -> Unit = {},
    onUserLocationResolved: (Double, Double) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }
    var isMapView by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Parchment)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("discover_lazy_column"),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SealBadge(size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "King Gold",
                                style = AppText.display.copy(fontSize = 19.sp),
                            )
                            Text(
                                text = "ملك الذهب",
                                style = AppText.body.copy(fontSize = 11.sp, color = AppColors.Muted),
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenCart,
                            modifier = Modifier.testTag("discover_cart_button"),
                        ) {
                            if (uiState.cartItemCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = AppColors.Brass) {
                                            Text(
                                                text = uiState.cartItemCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingBag,
                                        contentDescription = "عرض السلة",
                                        tint = AppColors.Ink,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.ShoppingBag,
                                    contentDescription = "عرض السلة",
                                    tint = AppColors.Ink,
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenPriceInsight,
                            modifier = Modifier.testTag("open_price_insight_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShowChart,
                                contentDescription = "تفاصيل الأسعار",
                                tint = AppColors.Brass,
                            )
                        }
                        IconButton(
                            onClick = onOpenSavedShops,
                            modifier = Modifier.testTag("discover_saved_shops_button"),
                        ) {
                            if (uiState.favoriteShopIds.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = AppColors.Alert) {
                                            Text(
                                                text = uiState.favoriteShopIds.size.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "محلات الذهب المحفوظة",
                                        tint = AppColors.Alert,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = "محلات الذهب المحفوظة",
                                    tint = AppColors.Ink,
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier.testTag("discover_profile_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "الملف الشخصي",
                                tint = AppColors.Ink,
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "ذهب حقيقي من محلات قريبة منك",
                        style = AppText.display.copy(fontSize = 18.sp),
                    )
                    Text(
                        text = "اختار المحل، ادفع بأمان، واستلم ذهبك بنفسك.",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.5.sp),
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("discover_search_input"),
                        placeholder = {
                            Text(
                                text = "ابحث عن محل ذهب بالاسم أو المدينة...",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.sp),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "بحث",
                                tint = AppColors.Muted,
                            )
                        },
                        trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { onSearchChange("") },
                                    modifier = Modifier.testTag("discover_search_clear"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "مسح البحث",
                                        tint = AppColors.Muted,
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = AppColors.Brass,
                            unfocusedBorderColor = AppColors.Line,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.Ink)
                            .clickable { showFilterSheet = true }
                            .testTag("discover_filter_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "تصفية النتائج",
                            tint = AppColors.BrassLight,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            item {
                RealTimeGlobalGoldPriceBanner(
                    prices = uiState.prices,
                    feedStatus = uiState.goldFeedStatus,
                    onRefresh = onRefreshGlobalGoldPrices,
                    onBannerClick = onOpenPriceInsight,
                )
            }

            item {
                Card(
                    onClick = onOpenVault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discover_vault_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Ink),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "خزنتك الرقمية ومحفظتك",
                                    style = AppText.body.copy(color = AppColors.BrassLight, fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = "${MoneyUtils.mgToGramsString(uiState.wallet.goldConfirmedMg)} جم ذهب · ${MoneyUtils.piastersToEgpString(uiState.wallet.cashAvailablePiasters)} ج.م",
                                    style = AppText.display.copy(color = Color.White, fontSize = 16.sp),
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "خزنتي ومحفظتي",
                                tint = AppColors.BrassLight,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DiscoverShortcutPill(
                                label = "شراء/سحب مجزأ",
                                icon = Icons.Outlined.AccountBalanceWallet,
                                onClick = onOpenVault,
                                modifier = Modifier.weight(1f).testTag("discover_shortcut_vault"),
                            )
                            DiscoverShortcutPill(
                                label = "بيع ذهبك",
                                icon = Icons.Outlined.Sell,
                                onClick = onOpenSellGold,
                                modifier = Modifier.weight(1f).testTag("discover_shortcut_sell"),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DiscoverShortcutPill(
                                label = "حاسبة وتحويل",
                                icon = Icons.Outlined.Calculate,
                                onClick = onOpenGoldCalculator,
                                modifier = Modifier.weight(1f).testTag("discover_shortcut_calculator"),
                            )
                            DiscoverShortcutPill(
                                label = "أخبار السوق",
                                icon = Icons.Outlined.Newspaper,
                                onClick = onOpenMarketNews,
                                modifier = Modifier.weight(1f).testTag("discover_shortcut_market_news"),
                            )
                            DiscoverShortcutPill(
                                label = "بوابة التاجر",
                                icon = Icons.Outlined.Storefront,
                                onClick = onOpenMerchantPortal,
                                modifier = Modifier.weight(1f).testTag("discover_shortcut_merchant"),
                            )
                            DiscoverShortcutPill(
                                label = "الدعم",
                                icon = Icons.Outlined.SupportAgent,
                                onClick = onOpenSupport,
                                modifier = Modifier.weight(1f).testTag("discover_shortcut_support"),
                            )
                            if (uiState.isAdmin) {
                                DiscoverShortcutPill(
                                    label = "الإدارة",
                                    icon = Icons.Outlined.AdminPanelSettings,
                                    onClick = onOpenAdminCenter,
                                    highlight = true,
                                    modifier = Modifier.weight(1f).testTag("discover_shortcut_admin"),
                                )
                            }
                        }
                    }
                }
            }

            item {
                DailyGoldPriceTrendChartCard(
                    dailyPrices = uiState.dailyGoldPriceTrend,
                    isSyncedFromFirestore = uiState.isDailyPricesFromFirestore,
                    onOpenFullInsight = onOpenPriceInsight,
                )
            }

            item {
                val triggered = uiState.isPriceAlertTriggered
                val alertBg = when {
                    triggered -> AppColors.Alert.copy(alpha = 0.09f)
                    uiState.priceAlertEnabled -> Color.White
                    else -> AppColors.ParchmentDeep
                }
                val alertBorder = when {
                    triggered -> AppColors.Alert.copy(alpha = 0.45f)
                    uiState.priceAlertEnabled -> AppColors.Brass.copy(alpha = 0.45f)
                    else -> AppColors.Line
                }
                val alertTitle = when {
                    triggered -> "تنبيه عاجل: تحقق شرط عيار ${uiState.priceAlertKarat} (${if (uiState.priceAlertAbove) "أعلى من" else "أقل من"} ${uiState.priceAlertTarget} ج.م)!"
                    uiState.priceAlertEnabled -> "تنبيه FCM: عند ${if (uiState.priceAlertAbove) "صعود" else "هبوط"} عيار ${uiState.priceAlertKarat} (${uiState.priceAlertTarget} ج.م)"
                    else -> "تفعيل تنبيه هبوط أسعار الذهب عبر FCM"
                }
                val alertSub = when {
                    triggered -> "السعر الحالي ${uiState.gramPrice(uiState.priceAlertKarat).toInt()} ج.م/جم — اضغط للمتابعة"
                    uiState.priceAlertEnabled -> "يصلك إشعار فوري عند تحقق الشرط · اضغط للتعديل"
                    else -> "حدد العيار والحد المطلوب ليصلك إشعار فوري عند الهبوط"
                }
                Card(
                    onClick = onOpenPriceInsight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_24k_fcm_alert_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = alertBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, alertBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "تنبيه سعر عيار 24",
                            tint = if (triggered) AppColors.Alert else AppColors.Brass,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alertTitle,
                                style = AppText.body.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = if (triggered) AppColors.Alert else AppColors.Ink,
                                ),
                            )
                            Text(
                                text = alertSub,
                                style = AppText.body.copy(
                                    color = AppColors.Muted,
                                    fontSize = 11.sp,
                                ),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = if (triggered) AppColors.Alert else AppColors.Brass,
                        )
                    }
                }
            }

            if (uiState.firestoreSyncError != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.Alert.copy(alpha = 0.08f))
                            .border(1.dp, AppColors.Alert.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.firestoreSyncError.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = AppText.body.copy(color = AppColors.Alert, fontSize = 12.sp),
                        )
                        IconButton(onClick = onDismissSyncError) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "إغلاق التنبيه",
                                tint = AppColors.Alert,
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    onClick = { onOpenAiAdvisor(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_ai_advisor_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.45f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.Ink),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = "مستشار الملك الذكي",
                                tint = AppColors.BrassLight,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مستشار الملك الذكي · Gemini",
                                style = AppText.body.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                ),
                            )
                            Text(
                                text = "اسأل عن أفضل عيار للشراء وقيمة السبائك بدون مصنعية إضافية",
                                style = AppText.body.copy(
                                    color = AppColors.Muted,
                                    fontSize = 11.5.sp,
                                ),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = AppColors.Brass,
                        )
                    }
                }
            }

            item {
                MarketNewsSectionCard(
                    newsState = uiState.marketNewsState,
                    selectedTopic = uiState.marketNewsTopic,
                    onSelectTopic = onSelectMarketNewsTopic,
                    onRefresh = onRefreshMarketNews,
                    onOpenFullNews = onOpenMarketNews,
                    maxArticlesInPreview = 2,
                )
            }

            item {
                val count = uiState.filteredShops.size
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verified_shops_section_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (uiState.verifiedOnlyFilter) {
                                "محلات شريكة قريبة منك · $count محل موثّق"
                            } else {
                                "محلات قريبة منك · $count محل"
                            },
                            modifier = Modifier.weight(1f),
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        FilterChip(
                            selected = false,
                            onClick = onOpenNearbyMap,
                            modifier = Modifier.testTag("discover_shortcut_nearby_map"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = "فتح خريطة المحلات الرئيسية",
                                    tint = AppColors.Brass,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = "خريطة المحلات",
                                    style = AppText.body.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = AppColors.Ink,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !uiState.favoritesOnlyFilter,
                                onClick = { onToggleFavoritesOnlyFilter(false) },
                                modifier = Modifier.testTag("all_shops_filter_chip"),
                                label = {
                                    Text(
                                        text = "كل المحلات (${uiState.shops.size})",
                                        style = AppText.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Ink,
                                    selectedLabelColor = AppColors.BrassLight,
                                    containerColor = Color.White,
                                    labelColor = AppColors.Ink,
                                ),
                            )
                            FilterChip(
                                selected = uiState.favoritesOnlyFilter,
                                onClick = { onToggleFavoritesOnlyFilter(!uiState.favoritesOnlyFilter) },
                                modifier = Modifier.testTag("favorites_filter_chip"),
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (uiState.favoritesOnlyFilter) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (uiState.favoritesOnlyFilter) AppColors.BrassLight else AppColors.Brass,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = "المفضلة (${uiState.favoriteShopIds.size})",
                                        style = AppText.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Ink,
                                    selectedLabelColor = AppColors.BrassLight,
                                    containerColor = Color.White,
                                    labelColor = AppColors.Ink,
                                ),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(1.dp, AppColors.Line, RoundedCornerShape(10.dp)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { isMapView = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(if (!isMapView) AppColors.Ink else Color.Transparent)
                                    .testTag("discover_mode_list"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FormatListBulleted,
                                    contentDescription = "عرض القائمة",
                                    tint = if (!isMapView) AppColors.BrassLight else AppColors.Muted,
                                )
                            }
                            IconButton(
                                onClick = { isMapView = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(if (isMapView) AppColors.Ink else Color.Transparent)
                                    .testTag("discover_mode_map"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = "عرض الخريطة",
                                    tint = if (isMapView) AppColors.BrassLight else AppColors.Muted,
                                )
                            }
                        }
                    }

                    if (uiState.availableCities.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .testTag("discover_city_filter_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = uiState.selectedCityFilter == null,
                                onClick = { onSelectCityFilter(null) },
                                modifier = Modifier.testTag("city_filter_all"),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationCity,
                                        contentDescription = null,
                                        tint = if (uiState.selectedCityFilter == null) AppColors.BrassLight else AppColors.Brass,
                                        modifier = Modifier.size(15.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = "كل المدن",
                                        style = AppText.body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Ink,
                                    selectedLabelColor = AppColors.BrassLight,
                                    containerColor = Color.White,
                                    labelColor = AppColors.Ink,
                                ),
                            )
                            uiState.availableCities.forEach { city ->
                                val selected = uiState.selectedCityFilter.equals(city, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectCityFilter(if (selected) null else city) },
                                    modifier = Modifier.testTag("city_filter_$city"),
                                    label = {
                                        Text(
                                            text = city,
                                            style = AppText.body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.Ink,
                                        selectedLabelColor = AppColors.BrassLight,
                                        containerColor = Color.White,
                                        labelColor = AppColors.Ink,
                                    ),
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("discover_category_filter_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == null,
                            onClick = { onSelectCategoryFilter(null) },
                            modifier = Modifier.testTag("category_filter_all"),
                            label = {
                                Text(
                                    text = "كل الأقسام",
                                    style = AppText.body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Brass,
                                selectedLabelColor = Color.White,
                                containerColor = AppColors.ParchmentDeep,
                                labelColor = AppColors.Ink,
                            ),
                        )
                        ProductCategory.allArabicLabels.forEach { category ->
                            val selected = uiState.selectedCategoryFilter == category
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectCategoryFilter(if (selected) null else category) },
                                modifier = Modifier.testTag("category_filter_$category"),
                                label = {
                                    Text(
                                        text = category,
                                        style = AppText.body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Brass,
                                    selectedLabelColor = Color.White,
                                    containerColor = AppColors.ParchmentDeep,
                                    labelColor = AppColors.Ink,
                                ),
                            )
                        }
                    }
                }
            }

            if (isMapView) {
                item {
                    GoldShopsMapView(
                        shops = uiState.filteredShops,
                        favoriteShopIds = uiState.favoriteShopIds,
                        onOpenShop = onOpenShop,
                        onToggleFavoriteShop = onToggleFavoriteShop,
                        onResetFilters = {
                            onSearchChange("")
                            onToggleFavoritesOnlyFilter(false)
                            onSelectCityFilter(null)
                            onSelectCategoryFilter(null)
                            onClearSearchAnchor()
                            onApplyFilters(null, true, ShopSortOption.NEAREST)
                        },
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = onSearchChange,
                        verifiedOnly = uiState.verifiedOnlyFilter,
                        onVerifiedOnlyChange = { verifiedOnly ->
                            onApplyFilters(uiState.selectedKaratFilter, verifiedOnly, uiState.sortOption)
                        },
                        nearbyRadiusKm = uiState.nearbyRadiusKm,
                        onNearbyRadiusChange = onUpdateNearbyRadius,
                        searchAnchorLat = uiState.searchAnchorLat,
                        searchAnchorLon = uiState.searchAnchorLon,
                        searchAnchorLabel = uiState.searchAnchorLabel,
                        onUpdateSearchAnchor = onUpdateSearchAnchor,
                        onClearSearchAnchor = onClearSearchAnchor,
                        selectedKarat = uiState.selectedKaratFilter,
                        shopKaratsProvider = { shop ->
                            uiState.productsForShop(shop.id).map { it.karat }.toSet()
                        },
                        userLocationLat = uiState.userLocationLat,
                        userLocationLon = uiState.userLocationLon,
                        onUserLocationResolved = onUserLocationResolved,
                        autoCenterOnUserLocation = true,
                        initialLocationDenied = uiState.locationPermissionDeniedOnStartup,
                    )
                }
            } else if (uiState.filteredShops.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp))
                            .padding(24.dp)
                            .testTag("discover_empty_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (uiState.favoritesOnlyFilter) {
                                "لا توجد محلات في المفضلة بعد"
                            } else {
                                "لا توجد محلات مطابقة لبحثك"
                            },
                            style = AppText.body.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.favoritesOnlyFilter) {
                                "اضغط على أيقونة القلب في أي محل لحفظه هنا والوصول إليه بسرعة."
                            } else {
                                "جرّب مسح البحث أو تغيير المدينة أو القسم أو العيار من الفلاتر."
                            },
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onSearchChange("")
                                onToggleFavoritesOnlyFilter(false)
                                onSelectCityFilter(null)
                                onSelectCategoryFilter(null)
                                onClearSearchAnchor()
                                onApplyFilters(null, true, ShopSortOption.NEAREST)
                            },
                            modifier = Modifier.testTag("discover_reset_filters_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Ink,
                                contentColor = AppColors.BrassLight,
                            ),
                        ) {
                            Text(
                                text = "إعادة ضبط الفلاتر",
                                style = AppText.body.copy(color = AppColors.BrassLight, fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            } else {
                items(uiState.filteredShops, key = { it.id }) { shop ->
                    ShopTile(
                        shop = shop,
                        isFavorite = uiState.favoriteShopIds.contains(shop.id),
                        onToggleFavorite = { onToggleFavoriteShop(shop.id) },
                        onOpenDirections = {
                            openDirections(
                                context = context,
                                address = shop.address,
                                latitude = shop.latitude,
                                longitude = shop.longitude,
                                label = shop.name,
                            ) {}
                        },
                        onClick = { onOpenShop(shop) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.TrustSoft)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "حماية المشتري",
                        tint = AppColors.Trust,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "كل المحلات موثّقة، ومبلغك محفوظ في الضمان لحد ما تستلم ذهبك.",
                        style = AppText.body.copy(color = AppColors.Trust, fontSize = 12.sp),
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var draftKarat by remember(uiState.selectedKaratFilter) { mutableStateOf(uiState.selectedKaratFilter) }
        var draftVerified by remember(uiState.verifiedOnlyFilter) { mutableStateOf(uiState.verifiedOnlyFilter) }
        var draftSort by remember(uiState.sortOption) { mutableStateOf(uiState.sortOption) }
        var draftCity by remember(uiState.selectedCityFilter) { mutableStateOf(uiState.selectedCityFilter) }

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            ) {
                Text(
                    text = "تصفية النتائج",
                    style = AppText.display.copy(fontSize = 18.sp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "العيار",
                    style = AppText.body.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "الكل", 18 to "عيار 18", 21 to "عيار 21", 24 to "عيار 24").forEach { (karat, label) ->
                        FilterChip(
                            selected = draftKarat == karat,
                            onClick = { draftKarat = karat },
                            label = { Text(label, style = AppText.body.copy(fontSize = 12.5.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Ink,
                                selectedLabelColor = AppColors.BrassLight,
                            ),
                        )
                    }
                }

                if (uiState.availableCities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "المدينة",
                        style = AppText.body.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = draftCity == null,
                            onClick = { draftCity = null },
                            label = { Text("كل المدن", style = AppText.body.copy(fontSize = 12.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Ink,
                                selectedLabelColor = AppColors.BrassLight,
                            ),
                        )
                        uiState.availableCities.forEach { city ->
                            FilterChip(
                                selected = draftCity.equals(city, ignoreCase = true),
                                onClick = { draftCity = city },
                                label = { Text(city, style = AppText.body.copy(fontSize = 12.sp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Ink,
                                    selectedLabelColor = AppColors.BrassLight,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "الترتيب",
                    style = AppText.body.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShopSortOption.entries.forEach { option ->
                        FilterChip(
                            selected = draftSort == option,
                            onClick = { draftSort = option },
                            label = { Text(option.label, style = AppText.body.copy(fontSize = 12.sp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Ink,
                                selectedLabelColor = AppColors.BrassLight,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "محلات موثّقة فقط",
                        style = AppText.body.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Switch(
                        checked = draftVerified,
                        onCheckedChange = { draftVerified = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Brass),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onApplyFilters(draftKarat, draftVerified, draftSort)
                        onSelectCityFilter(draftCity)
                        showFilterSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_filters_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Brass,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "عرض النتائج",
                        style = AppText.body.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopTile(
    shop: Shop,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenDirections: () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_card_${shop.id}"),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isFavorite) AppColors.Brass.copy(alpha = 0.55f) else AppColors.Line,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoldMarketplaceImage(
                    imageUrl = shop.logoUrl ?: shop.coverImageUrl,
                    base64Thumbnail = null,
                    contentDescription = shop.name,
                    modifier = Modifier.size(56.dp),
                    cornerRadius = 12.dp,
                    isThumbnail = true,
                    fallback = {
                        ShopGoldMedallion(
                            shopId = shop.id,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 12.dp,
                        )
                    },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = shop.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = AppText.body.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                            ),
                        )
                        if (shop.verified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            VerifiedStrip()
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${shop.city} · ${shop.area} · ${shop.verificationSourceLabel}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = AppText.body.copy(color = AppColors.Seal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "التقييم",
                                tint = if (shop.hasVerifiedRating) AppColors.Brass else AppColors.Muted,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (shop.hasVerifiedRating) {
                                    "${shop.rating} (${shop.reviews})"
                                } else {
                                    "التقييم غير متاح"
                                },
                                style = AppText.body.copy(
                                    color = if (shop.hasVerifiedRating) AppColors.Ink else AppColors.Muted,
                                    fontWeight = if (shop.hasVerifiedRating) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp,
                                ),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "مواعيد العمل",
                                tint = AppColors.Muted,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (shop.hasVerifiedHours) {
                                    "مفتوح حتى ${shop.closesAt}"
                                } else {
                                    "ساعات العمل غير متوفرة"
                                },
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp),
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("favorite_shop_button_${shop.id}"),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                        tint = if (isFavorite) AppColors.Brass else AppColors.Muted,
                    )
                }
            }

            if (shop.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shop.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp, lineHeight = 17.sp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.8.dp, color = AppColors.ParchmentDeep)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "العنوان الفعلي",
                        tint = AppColors.Brass,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shop.address,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = AppText.body.copy(
                            color = AppColors.Ink.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                        ),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppColors.ParchmentDeep)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NearMe,
                        contentDescription = "المسافة",
                        tint = AppColors.Brass,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${shop.distanceKm} كم",
                        style = AppText.body.copy(
                            color = AppColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                        ),
                    )
                }
                IconButton(
                    onClick = onOpenDirections,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("shop_directions_${shop.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Directions,
                        contentDescription = "الاتجاهات إلى ${shop.name}",
                        tint = AppColors.Brass,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverShortcutPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) AppColors.Brass else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlight) AppColors.Ink else AppColors.BrassLight,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppText.body.copy(
                color = if (highlight) AppColors.Ink else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
