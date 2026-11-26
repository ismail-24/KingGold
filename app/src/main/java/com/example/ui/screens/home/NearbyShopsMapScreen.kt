package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Shop
import com.example.model.ShopSortOption
import com.example.ui.components.GoldShopsMapView
import com.example.ui.components.MapKaratFilterRow
import com.example.ui.components.MapSearchBarHeader
import com.example.ui.components.SealBadge
import com.example.ui.components.hasValidMapCoordinates
import com.example.ui.screens.shop.SavedShopsDrawerSheet
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.launch

@Composable
fun NearbyShopsMapScreen(
    uiState: KingGoldUiState,
    onSearchChange: (String) -> Unit,
    onApplyFilters: (karat: Int?, verifiedOnly: Boolean, sort: ShopSortOption) -> Unit,
    onSelectCityFilter: (String?) -> Unit,
    onToggleFavoritesOnlyFilter: (Boolean) -> Unit,
    onOpenCart: () -> Unit,
    onOpenShop: (Shop) -> Unit,
    onToggleFavoriteShop: (String) -> Unit,
    onUpdateSearchAnchor: (Double, Double, String) -> Unit,
    onUpdateNearbyRadius: (Double?) -> Unit,
    onClearSearchAnchor: () -> Unit,
    onUserLocationResolved: (Double, Double) -> Unit = { _, _ -> },
    onFocusShopOnMap: (String) -> Unit = {},
    onOpenSavedShopsScreen: () -> Unit = {},
) {
    var findNearMyLocationSignal by remember { mutableIntStateOf(0) }
    var localFocusedShopId by remember { mutableStateOf<String?>(null) }
    var localFocusSignal by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val effectiveFocusedShopId = localFocusedShopId ?: uiState.focusedMapShopId
    val effectiveFocusSignal = uiState.focusMapShopCounter + localFocusSignal

    val mappableCount = remember(uiState.filteredShops) {
        uiState.filteredShops.count(::hasValidMapCoordinates)
    }
    val hasActiveFilters = uiState.searchQuery.isNotBlank() ||
        uiState.selectedCityFilter != null ||
        uiState.selectedCategoryFilter != null ||
        uiState.selectedKaratFilter != null ||
        uiState.favoritesOnlyFilter ||
        !uiState.verifiedOnlyFilter ||
        uiState.searchAnchorLat != null ||
        uiState.nearbyRadiusKm != null

    val resetAllFilters: () -> Unit = {
        onSearchChange("")
        onToggleFavoritesOnlyFilter(false)
        onSelectCityFilter(null)
        onClearSearchAnchor()
        onUpdateNearbyRadius(null)
        onApplyFilters(null, true, ShopSortOption.NEAREST)
    }

    LaunchedEffect(uiState.focusMapShopCounter) {
        if (uiState.focusMapShopCounter > 0 && drawerState.isOpen) {
            drawerState.snapTo(DrawerValue.Closed)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            SavedShopsDrawerSheet(
                uiState = uiState,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onOpenShop = { shop ->
                    scope.launch { drawerState.snapTo(DrawerValue.Closed) }
                    onOpenShop(shop)
                },
                onToggleFavoriteShop = onToggleFavoriteShop,
                onFocusShopOnMap = { shop ->
                    localFocusedShopId = shop.id
                    localFocusSignal++
                    onFocusShopOnMap(shop.id)
                    scope.launch { drawerState.snapTo(DrawerValue.Closed) }
                },
                onOpenFullScreen = {
                    scope.launch {
                        drawerState.snapTo(DrawerValue.Closed)
                        onOpenSavedShopsScreen()
                    }
                },
            )
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Parchment)
                .statusBarsPadding()
                .testTag("nearby_map_main_screen"),
            contentAlignment = Alignment.TopCenter,
        ) {
            val responsiveMapHeight = (maxHeight * 0.48f).coerceIn(210.dp, 460.dp)

            Column(
                modifier = Modifier
                    .widthIn(max = 900.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SealBadge(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "خريطة محلات الذهب القريبة",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = AppText.display.copy(fontSize = 17.5.sp),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AppColors.Ink,
                                ) {
                                    Text(
                                        text = "$mappableCount محل على الخريطة",
                                        modifier = Modifier
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                            .testTag("map_screen_shop_count"),
                                        style = AppText.body.copy(
                                            color = AppColors.BrassLight,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                                val cityLabel = uiState.selectedCityFilter ?: "القاهرة والجيزة والإسكندرية"
                                val subtitleText = uiState.selectedKaratFilter?.let { karat ->
                                    "$cityLabel · عيار $karat"
                                } ?: cityLabel
                                Text(
                                    text = subtitleText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp),
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasActiveFilters) {
                            IconButton(
                                onClick = resetAllFilters,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("map_screen_reset_filters_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RestartAlt,
                                    contentDescription = "إعادة ضبط فلاتر الخريطة",
                                    tint = AppColors.Brass,
                                )
                            }
                        }
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("map_saved_shops_drawer_button"),
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
                                        contentDescription = "عرض قائمة المحلات المحفوظة",
                                        tint = AppColors.Alert,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = "عرض قائمة المحلات المحفوظة",
                                    tint = AppColors.Ink,
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenCart,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("map_screen_cart_button"),
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
                    }
                }

                MapSearchBarHeader(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = onSearchChange,
                    onFindNearMyLocation = { findNearMyLocationSignal++ },
                    isMyLocationActive = uiState.searchAnchorLabel == "موقعي الحالي",
                )

                MapKaratFilterRow(
                    selectedKarat = uiState.selectedKaratFilter,
                    onSelectKarat = { karat ->
                        onApplyFilters(karat, uiState.verifiedOnlyFilter, uiState.sortOption)
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("map_screen_city_filter_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = uiState.selectedCityFilter == null,
                        onClick = { onSelectCityFilter(null) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("map_city_filter_all"),
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
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("map_city_filter_$city"),
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
                    FilterChip(
                        selected = uiState.favoritesOnlyFilter,
                        onClick = { onToggleFavoritesOnlyFilter(!uiState.favoritesOnlyFilter) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("map_favorites_only_chip"),
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.favoritesOnlyFilter) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (uiState.favoritesOnlyFilter) AppColors.BrassLight else AppColors.Brass,
                                modifier = Modifier.size(15.dp),
                            )
                        },
                        label = {
                            Text(
                                text = "المفضلة (${uiState.favoriteShopIds.size})",
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    GoldShopsMapView(
                        shops = uiState.filteredShops,
                        favoriteShopIds = uiState.favoriteShopIds,
                        onOpenShop = onOpenShop,
                        onToggleFavoriteShop = onToggleFavoriteShop,
                        onResetFilters = resetAllFilters,
                        modifier = Modifier.fillMaxWidth(),
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
                        mapHeight = responsiveMapHeight,
                        selectedKarat = uiState.selectedKaratFilter,
                        shopKaratsProvider = { shop ->
                            uiState.productsForShop(shop.id).map { it.karat }.toSet()
                        },
                        showSearchHeader = false,
                        findNearMyLocationSignal = findNearMyLocationSignal,
                        userLocationLat = uiState.userLocationLat,
                        userLocationLon = uiState.userLocationLon,
                        onUserLocationResolved = onUserLocationResolved,
                        autoCenterOnUserLocation = true,
                        initialLocationDenied = uiState.locationPermissionDeniedOnStartup,
                        focusedShopId = effectiveFocusedShopId,
                        focusShopSignal = effectiveFocusSignal,
                    )
                }
            }
        }
    }
}
