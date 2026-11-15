package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Shop
import com.example.model.ShopSortOption
import com.example.ui.components.Gold24KHistoryChartCard
import com.example.ui.components.RealTimeGlobalGoldPriceBanner
import com.example.ui.components.SealBadge
import com.example.ui.components.ShopGoldMedallion
import com.example.ui.components.VerifiedStrip
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
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
    onDismissSyncError: () -> Unit,
) {
    var showFilterSheet by remember { mutableStateOf(false) }

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
                .fillMaxSize(),
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
                        if (uiState.cartItemCount > 0) {
                            IconButton(
                                onClick = onOpenCart,
                                modifier = Modifier.testTag("discover_cart_button"),
                            ) {
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
                RealTimeGlobalGoldPriceBanner(
                    prices = uiState.prices,
                    feedStatus = uiState.goldFeedStatus,
                    onRefresh = onRefreshGlobalGoldPrices,
                    onBannerClick = onOpenPriceInsight,
                )
            }

            item {
                Gold24KHistoryChartCard(
                    historyPoints = uiState.history24K7Days,
                    onOpenFullInsight = onOpenPriceInsight,
                )
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
                                text = "اسأل عن أفضل عيار للشراء أو احسب التكلفة بالمصنعية",
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
                                text = "ابحث عن محل أو منطقة أو منتج",
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
                            text = "محلات شريكة قريبة منك · $count محل موثّق",
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.ParchmentDeep)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = uiState.sortOption.label,
                                style = AppText.body.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Seal,
                                ),
                            )
                        }
                    }

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
                }
            }

            if (uiState.filteredShops.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp))
                            .padding(24.dp),
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
                                "جرّب مسح البحث أو تغيير العيار من الفلاتر."
                            },
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                        )
                    }
                }
            } else {
                items(uiState.filteredShops, key = { it.id }) { shop ->
                    ShopTile(
                        shop = shop,
                        isFavorite = uiState.favoriteShopIds.contains(shop.id),
                        onToggleFavorite = { onToggleFavoriteShop(shop.id) },
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
                ShopGoldMedallion(
                    shopId = shop.id,
                    modifier = Modifier.size(54.dp),
                    cornerRadius = 12.dp,
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "التقييم",
                                tint = AppColors.Brass,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${shop.rating} (${shop.reviews})",
                                style = AppText.body.copy(
                                    color = AppColors.Ink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
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
                                text = "مفتوح حتى ${shop.closesAt}",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
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

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.8.dp, color = AppColors.ParchmentDeep)
            Spacer(modifier = Modifier.height(10.dp))

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
                            fontSize = 12.5.sp,
                        ),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
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
            }
        }
    }
}
