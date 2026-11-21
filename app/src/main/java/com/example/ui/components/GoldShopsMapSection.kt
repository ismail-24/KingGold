package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SatelliteAlt
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.Shop
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

enum class MapLayerMode(val arabicLabel: String) {
    STANDARD("خريطة"),
    SATELLITE("قمر صناعي"),
}

internal val SATELLITE_TILE_SOURCE: OnlineTileSourceBase = object : OnlineTileSourceBase(
    "EsriWorldImagery",
    0,
    18,
    256,
    ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex)
    }
}

fun tileSourceForLayer(mode: MapLayerMode): ITileSource = when (mode) {
    MapLayerMode.STANDARD -> TileSourceFactory.MAPNIK
    MapLayerMode.SATELLITE -> SATELLITE_TILE_SOURCE
}

fun attributionForLayer(mode: MapLayerMode): String = when (mode) {
    MapLayerMode.STANDARD -> "© OpenStreetMap contributors"
    MapLayerMode.SATELLITE -> "© Esri, Maxar, Earthstar Geographics"
}

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
        tileDownloadThreads = 4.toShort()
        tileFileSystemCacheMaxBytes = 64L * 1024L * 1024L
        tileFileSystemCacheTrimBytes = 48L * 1024L * 1024L
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

fun hasAnyLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

fun createConfiguredMyLocationOverlay(context: Context, mapView: MapView): MyLocationNewOverlay {
    val provider = GpsMyLocationProvider(context.applicationContext).apply {
        addLocationSource(LocationManager.GPS_PROVIDER)
        addLocationSource(LocationManager.NETWORK_PROVIDER)
    }
    return MyLocationNewOverlay(provider, mapView).apply {
        isDrawAccuracyEnabled = true
    }
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
    var layerMode by rememberSaveable { mutableStateOf(MapLayerMode.STANDARD) }
    var hasLocationPermission by remember { mutableStateOf(hasAnyLocationPermission(context)) }
    var locationPermissionDenied by remember { mutableStateOf(false) }
    var pendingCenterOnMyLocation by remember { mutableStateOf(false) }
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
            val tileProvider = remember(context) {
                configureOsmdroid(context)
                MapTileProviderBasic(context.applicationContext, TileSourceFactory.MAPNIK)
            }
            val mapView = remember(context, tileProvider) {
                MapView(context, tileProvider).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setUseDataConnection(isOnline)
                    setMultiTouchControls(true)
                    isHorizontalMapRepetitionEnabled = false
                    isVerticalMapRepetitionEnabled = false
                    minZoomLevel = 4.0
                    maxZoomLevel = 19.0
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(13.5)
                    controller.setCenter(defaultCenterForShops(mappableShops))
                    setOnTouchListener { view, event ->
                        val disallow = event.actionMasked != MotionEvent.ACTION_UP &&
                            event.actionMasked != MotionEvent.ACTION_CANCEL
                        view.parent?.requestDisallowInterceptTouchEvent(disallow)
                        false
                    }
                }
            }

            val myLocationOverlay = remember(context, mapView) {
                createConfiguredMyLocationOverlay(context, mapView)
            }

            fun centerMapOnUserLocation() {
                runCatching { myLocationOverlay.enableMyLocation() }
                val currentFix = myLocationOverlay.myLocation
                if (currentFix != null) {
                    if (mapView.zoomLevelDouble < 15.0) {
                        mapView.controller.setZoom(15.5)
                    }
                    mapView.controller.animateTo(currentFix)
                } else {
                    myLocationOverlay.runOnFirstFix {
                        mapView.post {
                            myLocationOverlay.myLocation?.let { geo ->
                                if (mapView.zoomLevelDouble < 15.0) {
                                    mapView.controller.setZoom(15.5)
                                }
                                mapView.controller.animateTo(geo)
                            }
                        }
                    }
                }
            }

            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
            ) { results ->
                val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                    hasAnyLocationPermission(context)
                hasLocationPermission = granted
                locationPermissionDenied = !granted
                if (granted && pendingCenterOnMyLocation) {
                    pendingCenterOnMyLocation = false
                    centerMapOnUserLocation()
                } else if (!granted) {
                    pendingCenterOnMyLocation = false
                }
            }

            val selectedPinIcon = remember(context) {
                createGoldShopMarkerDrawable(context = context, selected = true, verified = true)
            }
            val verifiedPinIcon = remember(context) {
                createGoldShopMarkerDrawable(context = context, selected = false, verified = true)
            }
            val defaultPinIcon = remember(context) {
                createGoldShopMarkerDrawable(context = context, selected = false, verified = false)
            }

            LaunchedEffect(isOnline, mapView) {
                mapView.setUseDataConnection(isOnline)
            }

            LaunchedEffect(layerMode, mapView) {
                mapView.setTileSource(tileSourceForLayer(layerMode))
                mapView.invalidate()
            }

            LaunchedEffect(hasLocationPermission, myLocationOverlay, mapView) {
                if (hasLocationPermission) {
                    runCatching { myLocationOverlay.enableMyLocation() }
                    if (!mapView.overlays.contains(myLocationOverlay)) {
                        mapView.overlays.add(myLocationOverlay)
                    }
                    if (pendingCenterOnMyLocation) {
                        pendingCenterOnMyLocation = false
                        centerMapOnUserLocation()
                    }
                } else {
                    runCatching {
                        myLocationOverlay.disableFollowLocation()
                        myLocationOverlay.disableMyLocation()
                    }
                    mapView.overlays.remove(myLocationOverlay)
                }
                mapView.invalidate()
            }

            val currentOnSelect by rememberUpdatedState<(String) -> Unit> { clickedId ->
                selectedShopId = clickedId
            }

            DisposableEffect(lifecycleOwner, mapView, myLocationOverlay) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            mapView.onResume()
                            val granted = hasAnyLocationPermission(context)
                            hasLocationPermission = granted
                            if (granted) {
                                locationPermissionDenied = false
                                runCatching { myLocationOverlay.enableMyLocation() }
                            }
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            runCatching { myLocationOverlay.disableMyLocation() }
                            mapView.onPause()
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    runCatching {
                        myLocationOverlay.disableFollowLocation()
                        myLocationOverlay.disableMyLocation()
                    }
                    mapView.onDetach()
                }
            }

            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("osm_map_view"),
                update = { view ->
                    val renderSignature = Triple(
                        mappableShops.map { listOf(it.id, it.name, it.latitude, it.longitude, it.verified, it.makingFeePerGram) },
                        selectedShop?.id,
                        hasLocationPermission,
                    )
                    if (view.tag != renderSignature) {
                        view.tag = renderSignature
                        syncGoldShopMarkers(
                            mapView = view,
                            shops = mappableShops,
                            selectedShopId = selectedShop?.id,
                            selectedIcon = selectedPinIcon,
                            verifiedIcon = verifiedPinIcon,
                            defaultIcon = defaultPinIcon,
                            myLocationOverlay = myLocationOverlay,
                            hasLocationPermission = hasLocationPermission,
                            onSelect = currentOnSelect,
                        )
                    }
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

            // Re-center, My Location, & Zoom controls (+ / -)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FloatingActionButton(
                    onClick = {
                        recenterSignal++
                        networkCheckTick++
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_recenter_button"),
                    shape = CircleShape,
                    containerColor = AppColors.Ink,
                    contentColor = AppColors.BrassLight,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = "إعادة توسيط الخريطة",
                    )
                }

                FloatingActionButton(
                    onClick = {
                        if (hasAnyLocationPermission(context)) {
                            hasLocationPermission = true
                            locationPermissionDenied = false
                            centerMapOnUserLocation()
                        } else {
                            pendingCenterOnMyLocation = true
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                ),
                            )
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("map_my_location_button"),
                    shape = CircleShape,
                    containerColor = if (hasLocationPermission) AppColors.Ink else Color.White.copy(alpha = 0.96f),
                    contentColor = if (hasLocationPermission) AppColors.BrassLight else AppColors.Ink,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = "تحديد موقعي الحالي",
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { mapView.controller.zoomIn() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("map_zoom_in_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "تكبير الخريطة",
                                tint = AppColors.Ink,
                            )
                        }
                        IconButton(
                            onClick = { mapView.controller.zoomOut() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("map_zoom_out_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Remove,
                                contentDescription = "تصغير الخريطة",
                                tint = AppColors.Ink,
                            )
                        }
                    }
                }
            }

            // TopStart overlay: Layer Switcher (Standard / Satellite) + Offline / Location banners
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.testTag("map_layer_toggle"),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isStandard = layerMode == MapLayerMode.STANDARD
                        Row(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isStandard) AppColors.Ink else Color.Transparent)
                                .clickable { layerMode = MapLayerMode.STANDARD }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("map_layer_standard_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = "عرض الخريطة القياسية",
                                tint = if (isStandard) AppColors.BrassLight else AppColors.Ink,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = MapLayerMode.STANDARD.arabicLabel,
                                style = AppText.body.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStandard) Color.White else AppColors.Ink,
                                ),
                            )
                        }

                        val isSatellite = layerMode == MapLayerMode.SATELLITE
                        Row(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSatellite) AppColors.Ink else Color.Transparent)
                                .clickable { layerMode = MapLayerMode.SATELLITE }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("map_layer_satellite_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SatelliteAlt,
                                contentDescription = "عرض الأقمار الصناعية",
                                tint = if (isSatellite) AppColors.BrassLight else AppColors.Ink,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = MapLayerMode.SATELLITE.arabicLabel,
                                style = AppText.body.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSatellite) Color.White else AppColors.Ink,
                                ),
                            )
                        }
                    }
                }

                if (!isOnline) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
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
                                modifier = Modifier.weight(1f),
                                style = AppText.body.copy(color = AppColors.Alert, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { networkCheckTick++ },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("map_retry_network_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "إعادة فحص الاتصال",
                                    tint = AppColors.Alert,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                if (locationPermissionDenied && !hasLocationPermission) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .testTag("map_location_denied_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.45f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOff,
                                contentDescription = null,
                                tint = AppColors.Brass,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فعّل إذن الموقع لعرض موقعك الحالي",
                                modifier = Modifier.weight(1f),
                                style = AppText.body.copy(color = AppColors.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            )
                            IconButton(
                                onClick = { locationPermissionDenied = false },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "إغلاق التنبيه",
                                    tint = AppColors.Muted,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Map tile provider attribution badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .testTag("map_attribution_badge"),
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.88f),
            ) {
                Text(
                    text = attributionForLayer(layerMode),
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

fun createGoldShopMarker(
    mapView: MapView,
    shop: Shop,
    isSelected: Boolean,
    selectedIcon: Drawable,
    verifiedIcon: Drawable,
    defaultIcon: Drawable,
    onSelect: (String) -> Unit,
): Marker = Marker(mapView).apply {
    id = shop.id
    position = GeoPoint(shop.latitude, shop.longitude)
    title = shop.name
    val sourceLabel = if (shop.verified) "معتمد" else "قيد التحقق"
    snippet = "${shop.area}، ${shop.city} · مصنعية ${shop.makingFeePerGram.toInt()} ج.م/جم · $sourceLabel"
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    icon = when {
        isSelected -> selectedIcon
        shop.verified -> verifiedIcon
        else -> defaultIcon
    }
    setOnMarkerClickListener { marker, _ ->
        onSelect(shop.id)
        marker.showInfoWindow()
        true
    }
}

fun syncGoldShopMarkers(
    mapView: MapView,
    shops: List<Shop>,
    selectedShopId: String?,
    selectedIcon: Drawable,
    verifiedIcon: Drawable,
    defaultIcon: Drawable,
    myLocationOverlay: MyLocationNewOverlay? = null,
    hasLocationPermission: Boolean = false,
    onSelect: (String) -> Unit,
): Int {
    mapView.overlays.removeAll { it is Marker }
    var addedCount = 0
    shops.filter(::hasValidMapCoordinates).forEach { shop ->
        val isSelected = selectedShopId == shop.id
        val marker = createGoldShopMarker(
            mapView = mapView,
            shop = shop,
            isSelected = isSelected,
            selectedIcon = selectedIcon,
            verifiedIcon = verifiedIcon,
            defaultIcon = defaultIcon,
            onSelect = onSelect,
        )
        mapView.overlays.add(marker)
        addedCount++
        if (isSelected) {
            marker.showInfoWindow()
        }
    }
    if (myLocationOverlay != null) {
        mapView.overlays.remove(myLocationOverlay)
        if (hasLocationPermission) {
            mapView.overlays.add(myLocationOverlay)
        }
    }
    mapView.invalidate()
    return addedCount
}

