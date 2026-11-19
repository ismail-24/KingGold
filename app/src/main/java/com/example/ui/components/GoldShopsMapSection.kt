package com.example.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.Shop
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

data class MappedShopsPartition(
    val mappable: List<Shop>,
    val unmappable: List<Shop>,
)

fun configureOsmdroid(context: Context) {
    val appContext = context.applicationContext
    val baseDir = File(appContext.filesDir, "osmdroid").apply { mkdirs() }
    val cacheDir = File(appContext.cacheDir, "osmdroid_tiles").apply { mkdirs() }
    Configuration.getInstance().apply {
        userAgentValue = appContext.packageName.ifBlank { "com.example.kinggold" }
        osmdroidBasePath = baseDir
        osmdroidTileCache = cacheDir
    }
}

fun hasValidMapCoordinates(shop: Shop): Boolean {
    if (!shop.latitude.isFinite() || !shop.longitude.isFinite()) return false
    if (shop.latitude == 0.0 && shop.longitude == 0.0) return false
    return shop.latitude in -85.0..85.0 && shop.longitude in -180.0..180.0
}

fun partitionShopsByCoordinates(shops: List<Shop>): MappedShopsPartition {
    val (mappable, unmappable) = shops.partition(::hasValidMapCoordinates)
    return MappedShopsPartition(mappable = mappable, unmappable = unmappable)
}

fun defaultCenterForShops(shops: List<Shop>): GeoPoint {
    val valid = shops.filter(::hasValidMapCoordinates)
    if (valid.isEmpty()) return GeoPoint(30.0444, 31.2357) // Cairo fallback
    val avgLat = valid.map { it.latitude }.average()
    val avgLon = valid.map { it.longitude }.average()
    return GeoPoint(avgLat, avgLon)
}

private fun isNetworkAvailable(context: Context): Boolean = runCatching {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}.getOrDefault(true)

@Composable
fun GoldShopsMapView(
    shops: List<Shop>,
    favoriteShopIds: Set<String>,
    onOpenShop: (Shop) -> Unit,
    onToggleFavoriteShop: (String) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val partition = remember(shops) { partitionShopsByCoordinates(shops) }
    val mappableShops = partition.mappable
    val unmappableCount = partition.unmappable.size

    var selectedShopId by remember(mappableShops) {
        mutableStateOf(mappableShops.firstOrNull()?.id)
    }
    var recenterSignal by remember { mutableIntStateOf(0) }
    var networkCheckTick by remember { mutableIntStateOf(0) }
    val isOnline = remember(networkCheckTick) { isNetworkAvailable(context) }

    val selectedShop = remember(mappableShops, selectedShopId) {
        mappableShops.firstOrNull { it.id == selectedShopId } ?: mappableShops.firstOrNull()
    }

    LaunchedEffect(Unit) {
        configureOsmdroid(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gold_shops_map_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (shops.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_empty_state_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppColors.Line),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = AppColors.Muted,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = "لا توجد محلات مطابقة لعرضها على الخريطة",
                        style = AppText.body.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "أعد ضبط البحث أو الفلاتر لعرض جميع محلات الصاغة القريبة.",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                    )
                    OutlinedButton(
                        onClick = onResetFilters,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("map_empty_reset_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppColors.Brass),
                    ) {
                        Text(
                            text = "إعادة ضبط الفلاتر",
                            style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            return@Column
        }

        if (mappableShops.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                items(mappableShops, key = { it.id }) { shop ->
                    val isSelected = selectedShop?.id == shop.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedShopId = shop.id },
                        modifier = Modifier.testTag("map_shop_chip_${shop.id}"),
                        label = {
                            Text(
                                text = "${shop.name} (${shop.distanceKm} كم)",
                                style = AppText.body.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
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

        if (unmappableCount > 0) {
            Text(
                text = "ملاحظة: هناك $unmappableCount محل بدون إحداثيات موقع دقيقة، يظهر في وضع القائمة فقط.",
                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, AppColors.Line, RoundedCornerShape(18.dp))
                .background(AppColors.ParchmentDeep),
        ) {
            val mapView = remember {
                configureOsmdroid(context)
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(13.5)
                    controller.setCenter(defaultCenterForShops(mappableShops))
                }
            }

            val currentOnSelect by rememberUpdatedState<(String) -> Unit> { clickedId ->
                selectedShopId = clickedId
            }

            DisposableEffect(lifecycleOwner, mapView) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    mapView.onDetach()
                }
            }

            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("osm_map_view"),
                update = { view ->
                    view.overlays.clear()
                    mappableShops.forEach { shop ->
                        val isSelected = selectedShop?.id == shop.id
                        val marker = Marker(view).apply {
                            position = GeoPoint(shop.latitude, shop.longitude)
                            title = shop.name
                            snippet = "${shop.area}، ${shop.city} · مصنعية ${shop.makingFeePerGram.toInt()} ج.م/جم"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = createGoldShopMarkerDrawable(
                                context = context,
                                selected = isSelected,
                                verified = shop.verified,
                            )
                            setOnMarkerClickListener { m, _ ->
                                currentOnSelect(shop.id)
                                m.showInfoWindow()
                                true
                            }
                        }
                        view.overlays.add(marker)
                        if (isSelected) {
                            marker.showInfoWindow()
                        }
                    }
                    view.invalidate()
                },
            )

            // Fit bounds or focus selected shop whenever shop list or recenter changes
            LaunchedEffect(mappableShops, recenterSignal) {
                when {
                    mappableShops.size == 1 -> {
                        val only = mappableShops.first()
                        mapView.controller.setZoom(15.0)
                        mapView.controller.animateTo(GeoPoint(only.latitude, only.longitude))
                    }
                    mappableShops.size > 1 -> {
                        val points = mappableShops.map { GeoPoint(it.latitude, it.longitude) }
                        val box = BoundingBox.fromGeoPointsSafe(points)
                        mapView.post {
                            runCatching {
                                mapView.zoomToBoundingBox(box, true, 72)
                            }.onFailure {
                                mapView.controller.setZoom(13.5)
                                mapView.controller.setCenter(defaultCenterForShops(mappableShops))
                            }
                        }
                    }
                    else -> {
                        mapView.controller.setZoom(12.0)
                        mapView.controller.setCenter(defaultCenterForShops(emptyList()))
                    }
                }
            }

            LaunchedEffect(selectedShop?.id) {
                selectedShop?.let { shop ->
                    if (hasValidMapCoordinates(shop)) {
                        mapView.controller.animateTo(GeoPoint(shop.latitude, shop.longitude))
                    }
                }
            }

            // Re-center FloatingActionButton
            FloatingActionButton(
                onClick = {
                    recenterSignal++
                    networkCheckTick++
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .testTag("map_recenter_button"),
                shape = CircleShape,
                containerColor = AppColors.Ink,
                contentColor = AppColors.BrassLight,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = "إعادة توسيط الخريطة",
                )
            }

            // Offline tile warning banner
            if (!isOnline) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .testTag("map_offline_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.AlertSoft,
                    border = BorderStroke(1.dp, AppColors.Alert.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = AppColors.Alert,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "لا يوجد اتصال — قد لا تتحمل بلاطات الخريطة",
                            style = AppText.body.copy(color = AppColors.Alert, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "إعادة فحص الاتصال",
                            tint = AppColors.Alert,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { networkCheckTick++ }
                                .testTag("map_retry_network_button"),
                        )
                    }
                }
            }

            // OpenStreetMap attribution badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.88f),
            ) {
                Text(
                    text = "© OpenStreetMap contributors",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = AppText.body.copy(color = AppColors.Ink, fontSize = 10.sp),
                )
            }
        }

        // Selected Shop Preview Card below the map
        if (selectedShop != null) {
            val isFavorite = favoriteShopIds.contains(selectedShop.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_selected_shop_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.55f)),
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
                        ShopGoldMedallion(
                            shopId = selectedShop.id,
                            modifier = Modifier.size(48.dp),
                            cornerRadius = 10.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedShop.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                                )
                                if (selectedShop.verified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    VerifiedStrip()
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = AppColors.Brass,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${selectedShop.rating} (${selectedShop.reviews}) · مصنعية ${selectedShop.makingFeePerGram.toInt()} ج.م/جم",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { onToggleFavoriteShop(selectedShop.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("map_selected_favorite_button"),
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                                tint = if (isFavorite) AppColors.Brass else AppColors.Muted,
                            )
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
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedShop.address,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = AppText.body.copy(fontSize = 12.sp, color = AppColors.Ink),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.NearMe,
                                contentDescription = null,
                                tint = AppColors.Brass,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${selectedShop.distanceKm} كم",
                                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                            )
                        }
                    }

                    Button(
                        onClick = { onOpenShop(selectedShop) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("map_open_shop_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Ink,
                            contentColor = AppColors.BrassLight,
                        ),
                    ) {
                        Text(
                            text = "دخول المتجر وتصفح المشغولات",
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun createGoldShopMarkerDrawable(
    context: Context,
    selected: Boolean,
    verified: Boolean,
): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = ((if (selected) 44f else 36f) * density).toInt().coerceAtLeast(24)
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)

    val cx = sizePx / 2f
    val headRadius = sizePx * 0.32f
    val cy = headRadius + sizePx * 0.06f

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when {
            selected -> android.graphics.Color.rgb(20, 20, 22)
            verified -> android.graphics.Color.rgb(181, 138, 46)
            else -> android.graphics.Color.rgb(110, 103, 95)
        }
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) android.graphics.Color.rgb(227, 189, 98) else android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
    }
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) android.graphics.Color.rgb(227, 189, 98) else android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }

    val pinPath = Path().apply {
        moveTo(cx, sizePx * 0.96f)
        lineTo(cx - headRadius * 0.65f, cy + headRadius * 0.45f)
        lineTo(cx + headRadius * 0.65f, cy + headRadius * 0.45f)
        close()
    }
    canvas.drawPath(pinPath, fillPaint)
    canvas.drawCircle(cx, cy, headRadius, fillPaint)
    canvas.drawCircle(cx, cy, headRadius, borderPaint)
    canvas.drawCircle(cx, cy, headRadius * 0.38f, innerPaint)

    return BitmapDrawable(context.resources, bitmap)
}
