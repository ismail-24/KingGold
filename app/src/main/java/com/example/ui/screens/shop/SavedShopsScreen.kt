package com.example.ui.screens.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoldProductImageLibrary
import com.example.model.Shop
import com.example.ui.components.GoldMarketplaceImage
import com.example.ui.components.ShopGoldMedallion
import com.example.ui.components.VerifiedStrip
import com.example.ui.components.calculateHaversineDistanceKm
import com.example.ui.components.hasValidMapCoordinates
import com.example.ui.components.isValidMapCoordinatePair
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.GoldImageTier
import com.example.ui.util.openDirections

internal fun savedGoldShopsForState(uiState: KingGoldUiState): List<Shop> {
    val anchorLat = uiState.searchAnchorLat ?: uiState.userLocationLat
    val anchorLon = uiState.searchAnchorLon ?: uiState.userLocationLon
    val hasAnchor = anchorLat != null && anchorLon != null && isValidMapCoordinatePair(anchorLat, anchorLon)
    return uiState.shops
        .filter { uiState.favoriteShopIds.contains(it.id) }
        .map { shop ->
            if (hasAnchor && hasValidMapCoordinates(shop)) {
                shop.copy(
                    distanceKm = calculateHaversineDistanceKm(
                        lat1 = anchorLat!!,
                        lon1 = anchorLon!!,
                        lat2 = shop.latitude,
                        lon2 = shop.longitude,
                    ),
                )
            } else {
                shop
            }
        }
        .sortedBy { it.distanceKm }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedShopsScreen(
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onOpenShop: (Shop) -> Unit,
    onToggleFavoriteShop: (String) -> Unit,
    onFocusShopOnMap: (Shop) -> Unit,
    onBrowseMap: () -> Unit,
) {
    val savedShops = remember(
        uiState.shops,
        uiState.favoriteShopIds,
        uiState.searchAnchorLat,
        uiState.searchAnchorLon,
        uiState.userLocationLat,
        uiState.userLocationLon,
    ) {
        savedGoldShopsForState(uiState)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("saved_shops_screen"),
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Parchment,
                    titleContentColor = AppColors.Ink,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("saved_shops_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = AppColors.Ink,
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "محلات الذهب المحفوظة",
                            style = AppText.display.copy(fontSize = 18.sp),
                        )
                        Text(
                            text = "${savedShops.size} محل محفوظ للوصول السريع",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            SavedShopsListContent(
                savedShops = savedShops,
                uiState = uiState,
                onOpenShop = onOpenShop,
                onToggleFavoriteShop = onToggleFavoriteShop,
                onFocusShopOnMap = onFocusShopOnMap,
                onBrowseMap = onBrowseMap,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
            )
        }
    }
}

@Composable
fun SavedShopsDrawerSheet(
    uiState: KingGoldUiState,
    onCloseDrawer: () -> Unit,
    onOpenShop: (Shop) -> Unit,
    onToggleFavoriteShop: (String) -> Unit,
    onFocusShopOnMap: (Shop) -> Unit,
    onOpenFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedShops = remember(
        uiState.shops,
        uiState.favoriteShopIds,
        uiState.searchAnchorLat,
        uiState.searchAnchorLon,
        uiState.userLocationLat,
        uiState.userLocationLon,
    ) {
        savedGoldShopsForState(uiState)
    }

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 360.dp)
            .testTag("saved_shops_drawer"),
        drawerContainerColor = AppColors.Parchment,
        drawerContentColor = AppColors.Ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppColors.AlertSoft,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = AppColors.Alert,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "المحلات المحفوظة",
                            style = AppText.display.copy(fontSize = 17.sp),
                        )
                        Text(
                            text = "${savedShops.size} محل في مفضلتك",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                        )
                    }
                }
                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("saved_shops_drawer_close_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "إغلاق قائمة المحلات المحفوظة",
                        tint = AppColors.Ink,
                    )
                }
            }

            if (savedShops.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onOpenFullScreen,
                        modifier = Modifier.testTag("saved_shops_open_full_screen_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInFull,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "عرض في صفحة مستقلة",
                            style = AppText.body.copy(
                                color = AppColors.Brass,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
            }

            HorizontalDivider(color = AppColors.Line)

            SavedShopsListContent(
                savedShops = savedShops,
                uiState = uiState,
                onOpenShop = onOpenShop,
                onToggleFavoriteShop = onToggleFavoriteShop,
                onFocusShopOnMap = onFocusShopOnMap,
                onBrowseMap = onCloseDrawer,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                compactMode = true,
            )
        }
    }
}

@Composable
private fun SavedShopsListContent(
    savedShops: List<Shop>,
    uiState: KingGoldUiState,
    onOpenShop: (Shop) -> Unit,
    onToggleFavoriteShop: (String) -> Unit,
    onFocusShopOnMap: ((Shop) -> Unit)?,
    onBrowseMap: () -> Unit,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false,
) {
    val context = LocalContext.current

    if (savedShops.isEmpty()) {
        Box(
            modifier = modifier.padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_shops_empty_state"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppColors.Line),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppColors.ParchmentDeep,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = AppColors.Brass,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Text(
                        text = "لا توجد محلات ذهب محفوظة بعد",
                        style = AppText.display.copy(fontSize = 16.sp),
                    )
                    Text(
                        text = "اضغط على زر القلب في تفاصيل أي محل على الخريطة أو صفحة المتجر لحفظه هنا والوصول إليه بسرعة.",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onBrowseMap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("saved_shops_browse_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Ink,
                            contentColor = AppColors.BrassLight,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "استكشاف المحلات على الخريطة",
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.testTag("saved_shops_list"),
        contentPadding = PaddingValues(horizontal = if (compactMode) 12.dp else 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(savedShops, key = { it.id }) { shop ->
            val karats = remember(shop.id, uiState.activeMerchantProducts) {
                uiState.productsForShop(shop.id).map { it.karat }.distinct().sortedDescending()
            }
            SavedShopCard(
                shop = shop,
                karats = karats,
                onOpenShop = { onOpenShop(shop) },
                onToggleFavorite = { onToggleFavoriteShop(shop.id) },
                onFocusOnMap = onFocusShopOnMap?.let { focus -> { focus(shop) } },
                onOpenDirections = {
                    openDirections(
                        context = context,
                        address = shop.address,
                        latitude = shop.latitude,
                        longitude = shop.longitude,
                        label = shop.name,
                    ) {}
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedShopCard(
    shop: Shop,
    karats: List<Int>,
    onOpenShop: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFocusOnMap: (() -> Unit)?,
    onOpenDirections: () -> Unit,
) {
    Card(
        onClick = onOpenShop,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_shop_card_${shop.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoldMarketplaceImage(
                    imageUrl = GoldProductImageLibrary.shopLogoUrl(shop, GoldImageTier.THUMBNAIL),
                    base64Thumbnail = null,
                    contentDescription = shop.name,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 10.dp,
                    isThumbnail = true,
                    imageTier = GoldImageTier.THUMBNAIL,
                    placeholderRes = GoldProductImageLibrary.placeholderDrawableForShopLogo(shop),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = shop.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp),
                        )
                        if (shop.verified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            VerifiedStrip()
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (shop.hasVerifiedRating) AppColors.Brass else AppColors.Muted,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        val ratingText = if (shop.hasVerifiedRating) {
                            "${shop.rating} (${shop.reviews})"
                        } else {
                            "التقييم غير متاح"
                        }
                        Text(
                            text = "$ratingText · ${shop.city} - ${shop.area}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                        )
                    }
                }
                Surface(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("saved_shop_favorite_${shop.id}"),
                    shape = CircleShape,
                    color = AppColors.AlertSoft,
                    border = BorderStroke(1.dp, AppColors.Alert.copy(alpha = 0.45f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "إزالة من المحلات المحفوظة",
                            tint = AppColors.Alert,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }

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
                        contentDescription = null,
                        tint = AppColors.Brass,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shop.address,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = AppText.body.copy(fontSize = 11.5.sp, color = AppColors.Ink),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.NearMe,
                        contentDescription = null,
                        tint = AppColors.Brass,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${shop.distanceKm} كم",
                        style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    )
                }
            }

            if (karats.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    karats.forEach { karat ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppColors.ParchmentDeep,
                            border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = "عيار $karat",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = AppText.body.copy(
                                    color = AppColors.Ink,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onOpenShop,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("saved_shop_open_${shop.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Ink,
                        contentColor = AppColors.BrassLight,
                    ),
                ) {
                    Text(
                        text = "فتح المتجر",
                        style = AppText.body.copy(
                            color = AppColors.BrassLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                    )
                }

                if (onFocusOnMap != null && hasValidMapCoordinates(shop)) {
                    OutlinedButton(
                        onClick = onFocusOnMap,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("saved_shop_map_focus_${shop.id}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppColors.Brass),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "على الخريطة",
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenDirections,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("saved_shop_directions_${shop.id}"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Directions,
                        contentDescription = null,
                        tint = AppColors.Brass,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "الاتجاهات",
                        style = AppText.body.copy(
                            color = AppColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }
    }
}
