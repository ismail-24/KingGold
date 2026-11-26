package com.example.ui.components

import android.Manifest
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.Directions
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.Shop
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.openDirections
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun GoldShopsMapView(
    shops: List<Shop>,
    favoriteShopIds: Set<String>,
    onOpenShop: (Shop) -> Unit,
    onToggleFavoriteShop: (String) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    verifiedOnly: Boolean = true,
    onVerifiedOnlyChange: ((Boolean) -> Unit)? = null,
    nearbyRadiusKm: Double? = null,
    onNearbyRadiusChange: ((Double?) -> Unit)? = null,
    searchAnchorLat: Double? = null,
    searchAnchorLon: Double? = null,
    searchAnchorLabel: String? = null,
    onUpdateSearchAnchor: ((Double, Double, String) -> Unit)? = null,
    onClearSearchAnchor: (() -> Unit)? = null,
    mapHeight: Dp = 245.dp,
    selectedKarat: Int? = null,
    shopKaratsProvider: ((Shop) -> Set<Int>)? = null,
    showSearchHeader: Boolean = true,
    findNearMyLocationSignal: Int = 0,
    userLocationLat: Double? = null,
    userLocationLon: Double? = null,
    onUserLocationResolved: ((Double, Double) -> Unit)? = null,
    autoCenterOnUserLocation: Boolean = true,
    initialLocationDenied: Boolean = false,
    focusedShopId: String? = null,
    focusShopSignal: Int = 0,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var localSearchQuery by rememberSaveable { mutableStateOf("") }
    var localVerifiedOnly by rememberSaveable { mutableStateOf(verifiedOnly) }
    var localNearbyRadiusKm by rememberSaveable { mutableStateOf<Double?>(null) }
    var localAnchorLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var localAnchorLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var localAnchorLabel by rememberSaveable { mutableStateOf<String?>(null) }

    val effectiveSearchQuery = if (onSearchQueryChange != null) searchQuery else localSearchQuery
    val effectiveVerifiedOnly = if (onVerifiedOnlyChange != null) verifiedOnly else localVerifiedOnly
    val effectiveNearbyRadiusKm = if (onNearbyRadiusChange != null) nearbyRadiusKm else localNearbyRadiusKm
    val effectiveAnchorLat = if (onUpdateSearchAnchor != null) searchAnchorLat else localAnchorLat
    val effectiveAnchorLon = if (onUpdateSearchAnchor != null) searchAnchorLon else localAnchorLon
    val effectiveAnchorLabel = if (onUpdateSearchAnchor != null) searchAnchorLabel else localAnchorLabel

    val displayedShops = remember(
        shops,
        effectiveSearchQuery,
        effectiveVerifiedOnly,
        effectiveAnchorLat,
        effectiveAnchorLon,
        effectiveNearbyRadiusKm,
        selectedKarat,
        shopKaratsProvider,
    ) {
        filterAndSortShopsForMap(
            shops = shops,
            searchQuery = effectiveSearchQuery,
            verifiedOnly = effectiveVerifiedOnly,
            anchorLat = effectiveAnchorLat,
            anchorLon = effectiveAnchorLon,
            nearbyRadiusKm = effectiveNearbyRadiusKm,
            selectedKarat = selectedKarat,
            shopKaratsProvider = shopKaratsProvider,
        )
    }

    val partition = remember(displayedShops) { partitionShopsByCoordinates(displayedShops) }
    val mappableShops = partition.mappable
    val unmappableCount = partition.unmappable.size

    var selectedShopId by remember(mappableShops) {
        mutableStateOf(
            focusedShopId?.takeIf { targetId -> mappableShops.any { it.id == targetId } }
                ?: mappableShops.firstOrNull()?.id,
        )
    }
    var userSelectedShopCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(focusedShopId, focusShopSignal, mappableShops) {
        if (!focusedShopId.isNullOrBlank() && mappableShops.any { it.id == focusedShopId }) {
            selectedShopId = focusedShopId
            userSelectedShopCounter++
        }
    }
    var recenterSignal by remember { mutableIntStateOf(0) }
    var networkCheckTick by remember { mutableIntStateOf(0) }
    var layerMode by rememberSaveable { mutableStateOf(MapLayerMode.STANDARD) }
    var hasLocationPermission by remember { mutableStateOf(hasAnyLocationPermission(context)) }
    var locationPermissionDenied by rememberSaveable {
        mutableStateOf(initialLocationDenied && !hasAnyLocationPermission(context))
    }
    var localUserLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var localUserLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var hasAutoCenteredOnUser by rememberSaveable { mutableStateOf(false) }
    var pendingCenterOnMyLocation by remember { mutableStateOf(false) }
    var pendingNearbyMyLocationSearch by remember { mutableStateOf(false) }
    var locationHintMessage by remember { mutableStateOf<String?>(null) }
    val isOnline = remember(networkCheckTick) { isNetworkAvailable(context) }

    val effectiveUserLat = userLocationLat ?: localUserLat
    val effectiveUserLon = userLocationLon ?: localUserLon

    val selectedShop = remember(mappableShops, selectedShopId) {
        mappableShops.firstOrNull { it.id == selectedShopId } ?: mappableShops.firstOrNull()
    }

    val updateQuery: (String) -> Unit = { next ->
        if (onSearchQueryChange != null) onSearchQueryChange(next) else localSearchQuery = next
    }
    val updateVerified: (Boolean) -> Unit = { next ->
        if (onVerifiedOnlyChange != null) onVerifiedOnlyChange(next) else localVerifiedOnly = next
    }
    val updateRadius: (Double?) -> Unit = { next ->
        if (onNearbyRadiusChange != null) onNearbyRadiusChange(next) else localNearbyRadiusKm = next
    }
    val updateAnchor: (Double, Double, String) -> Unit = { lat, lon, label ->
        locationHintMessage = null
        if (onUpdateSearchAnchor != null) {
            onUpdateSearchAnchor(lat, lon, label)
        } else {
            localAnchorLat = lat
            localAnchorLon = lon
            localAnchorLabel = label
        }
    }
    val clearAnchor: () -> Unit = {
        locationHintMessage = null
        if (onClearSearchAnchor != null) {
            onClearSearchAnchor()
        } else {
            localAnchorLat = null
            localAnchorLon = null
            localAnchorLabel = null
            localNearbyRadiusKm = null
        }
        if (onNearbyRadiusChange != null) {
            onNearbyRadiusChange(null)
        }
    }

    LaunchedEffect(Unit) {
        configureOsmdroid(context)
    }

    LaunchedEffect(initialLocationDenied) {
        if (initialLocationDenied && !hasAnyLocationPermission(context)) {
            locationPermissionDenied = true
        }
    }

    val tileProvider = remember(context) {
        configureOsmdroid(context)
        MapTileProviderBasic(context.applicationContext, TileSourceFactory.MAPNIK)
    }
    val mapView = remember(context, tileProvider) {
        val startupUserPoint = if (autoCenterOnUserLocation && hasLocationPermission) {
            if (effectiveUserLat != null && effectiveUserLon != null &&
                isValidMapCoordinatePair(effectiveUserLat, effectiveUserLon)
            ) {
                GeoPoint(effectiveUserLat, effectiveUserLon)
            } else {
                resolveBestKnownLocation(context)
            }
        } else {
            null
        }
        MapView(context, tileProvider).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setUseDataConnection(isOnline)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(if (startupUserPoint != null) 14.5 else 13.5)
            controller.setCenter(startupUserPoint ?: defaultCenterForShops(mappableShops))
            setOnTouchListener { view, event ->
                val disallow = event.actionMasked != android.view.MotionEvent.ACTION_UP &&
                    event.actionMasked != android.view.MotionEvent.ACTION_CANCEL
                view.parent?.requestDisallowInterceptTouchEvent(disallow)
                false
            }
        }
    }

    val myLocationOverlay = remember(context, mapView) {
        createConfiguredMyLocationOverlay(context, mapView)
    }

    fun applyAnchorAndDefaultRadius(
        lat: Double,
        lon: Double,
        label: String,
        preferredRadiusKm: Double? = effectiveNearbyRadiusKm ?: 15.0,
    ) {
        if (!isValidMapCoordinatePair(lat, lon)) return
        updateAnchor(lat, lon, label)
        if (effectiveNearbyRadiusKm == null && preferredRadiusKm != null) {
            updateRadius(preferredRadiusKm)
        }
    }

    fun recordResolvedUserPosition(geo: GeoPoint) {
        if (!isValidMapCoordinatePair(geo.latitude, geo.longitude)) return
        localUserLat = geo.latitude
        localUserLon = geo.longitude
        locationPermissionDenied = false
        onUserLocationResolved?.invoke(geo.latitude, geo.longitude)
    }

    fun resolveImmediateUserGeoPoint(): GeoPoint? {
        val stateFix = if (effectiveUserLat != null && effectiveUserLon != null &&
            isValidMapCoordinatePair(effectiveUserLat, effectiveUserLon)
        ) {
            GeoPoint(effectiveUserLat, effectiveUserLon)
        } else {
            null
        }
        val overlayFix = myLocationOverlay.myLocation?.takeIf {
            isValidMapCoordinatePair(it.latitude, it.longitude)
        }
        return overlayFix ?: resolveBestKnownLocation(context) ?: stateFix
    }

    fun centerMapOnUserLocation(applyAsNearbySearchAnchor: Boolean) {
        runCatching { myLocationOverlay.enableMyLocation() }
        val currentFix = resolveImmediateUserGeoPoint()
        if (currentFix != null) {
            recordResolvedUserPosition(currentFix)
            if (mapView.zoomLevelDouble < 14.5) {
                mapView.controller.setZoom(15.0)
            }
            mapView.controller.setCenter(currentFix)
            mapView.controller.animateTo(currentFix)
            hasAutoCenteredOnUser = true
            if (applyAsNearbySearchAnchor) {
                applyAnchorAndDefaultRadius(currentFix.latitude, currentFix.longitude, "موقعي الحالي")
            }
        } else {
            if (applyAsNearbySearchAnchor) {
                locationHintMessage = "جاري انتظار إشارة GPS لموقعك — يمكنك تحريك الخريطة والضغط على «البحث في هذه المنطقة»."
            }
            requestCurrentLocationFix(context) { liveGeo ->
                if (liveGeo != null) {
                    recordResolvedUserPosition(liveGeo)
                    if (mapView.zoomLevelDouble < 14.5) {
                        mapView.controller.setZoom(15.0)
                    }
                    mapView.controller.setCenter(liveGeo)
                    mapView.controller.animateTo(liveGeo)
                    hasAutoCenteredOnUser = true
                    if (applyAsNearbySearchAnchor) {
                        applyAnchorAndDefaultRadius(liveGeo.latitude, liveGeo.longitude, "موقعي الحالي")
                    }
                }
            }
            myLocationOverlay.runOnFirstFix {
                mapView.post {
                    val firstFix = myLocationOverlay.myLocation?.takeIf {
                        isValidMapCoordinatePair(it.latitude, it.longitude)
                    } ?: resolveBestKnownLocation(context)
                    firstFix?.let { geo ->
                        recordResolvedUserPosition(geo)
                        if (mapView.zoomLevelDouble < 14.5) {
                            mapView.controller.setZoom(15.0)
                        }
                        mapView.controller.setCenter(geo)
                        mapView.controller.animateTo(geo)
                        hasAutoCenteredOnUser = true
                        if (applyAsNearbySearchAnchor) {
                            applyAnchorAndDefaultRadius(geo.latitude, geo.longitude, "موقعي الحالي")
                        }
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
        if (granted && (pendingCenterOnMyLocation || pendingNearbyMyLocationSearch)) {
            val triggerNearby = pendingNearbyMyLocationSearch
            pendingCenterOnMyLocation = false
            pendingNearbyMyLocationSearch = false
            centerMapOnUserLocation(applyAsNearbySearchAnchor = triggerNearby)
        } else if (!granted) {
            pendingCenterOnMyLocation = false
            pendingNearbyMyLocationSearch = false
            locationHintMessage = "تم رفض إذن الموقع — حرّك الخريطة أو اضغط مطولاً لتحديد موقع البحث."
        }
    }

    fun triggerFindNearMyLocation() {
        if (hasAnyLocationPermission(context)) {
            hasLocationPermission = true
            locationPermissionDenied = false
            centerMapOnUserLocation(applyAsNearbySearchAnchor = true)
        } else {
            pendingNearbyMyLocationSearch = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    fun triggerSearchCurrentMapArea() {
        val center = mapView.mapCenter
        val lat = center.latitude
        val lon = center.longitude
        if (isValidMapCoordinatePair(lat, lon)) {
            applyAnchorAndDefaultRadius(lat, lon, "مركز الخريطة المحدد")
        } else {
            val fallback = defaultCenterForShops(displayedShops)
            applyAnchorAndDefaultRadius(fallback.latitude, fallback.longitude, "مركز الخريطة المحدد")
        }
    }

    val currentLongPressHandler by rememberUpdatedState<(GeoPoint) -> Unit> { point ->
        if (isValidMapCoordinatePair(point.latitude, point.longitude)) {
            applyAnchorAndDefaultRadius(point.latitude, point.longitude, "نقطة محددة على الخريطة")
            mapView.controller.animateTo(point)
        }
    }

    val mapEventsOverlay = remember(mapView) {
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        currentLongPressHandler(p)
                        return true
                    }
                    return false
                }
            },
        )
    }

    LaunchedEffect(findNearMyLocationSignal) {
        if (findNearMyLocationSignal > 0) {
            triggerFindNearMyLocation()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gold_shops_map_container"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showSearchHeader) {
            MapSearchBarHeader(
                searchQuery = effectiveSearchQuery,
                onSearchQueryChange = updateQuery,
                onFindNearMyLocation = ::triggerFindNearMyLocation,
                isMyLocationActive = effectiveAnchorLabel == "موقعي الحالي",
            )
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
                        onClick = {
                            selectedShopId = shop.id
                            userSelectedShopCounter++
                        },
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
                .height(mapHeight)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, AppColors.Line, RoundedCornerShape(18.dp))
                .background(AppColors.ParchmentDeep),
        ) {
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

            LaunchedEffect(hasLocationPermission, autoCenterOnUserLocation, myLocationOverlay, mapView) {
                if (!mapView.overlays.contains(mapEventsOverlay)) {
                    mapView.overlays.add(0, mapEventsOverlay)
                }
                if (hasLocationPermission) {
                    runCatching { myLocationOverlay.enableMyLocation() }
                    if (!mapView.overlays.contains(myLocationOverlay)) {
                        mapView.overlays.add(myLocationOverlay)
                    }
                    if (pendingCenterOnMyLocation || pendingNearbyMyLocationSearch) {
                        val triggerNearby = pendingNearbyMyLocationSearch
                        pendingCenterOnMyLocation = false
                        pendingNearbyMyLocationSearch = false
                        centerMapOnUserLocation(applyAsNearbySearchAnchor = triggerNearby)
                    } else if (autoCenterOnUserLocation && (effectiveUserLat == null || effectiveUserLon == null)) {
                        requestCurrentLocationFix(context) { liveGeo ->
                            liveGeo?.let(::recordResolvedUserPosition)
                        }
                        myLocationOverlay.runOnFirstFix {
                            mapView.post {
                                val firstFix = myLocationOverlay.myLocation?.takeIf {
                                    isValidMapCoordinatePair(it.latitude, it.longitude)
                                } ?: resolveBestKnownLocation(context)
                                firstFix?.let(::recordResolvedUserPosition)
                            }
                        }
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
                userSelectedShopCounter++
            }
            val currentOnToggleFavorite by rememberUpdatedState(onToggleFavoriteShop)
            val currentOnOpenShop by rememberUpdatedState(onOpenShop)

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
                                if (autoCenterOnUserLocation && (effectiveUserLat == null || effectiveUserLon == null)) {
                                    requestCurrentLocationFix(context) { liveGeo ->
                                        liveGeo?.let(::recordResolvedUserPosition)
                                    }
                                }
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
                    if (!view.overlays.contains(mapEventsOverlay)) {
                        view.overlays.add(0, mapEventsOverlay)
                    }
                    val renderSignature = listOf(
                        mappableShops.map {
                            listOf(
                                it.id,
                                it.name,
                                it.latitude,
                                it.longitude,
                                it.verified,
                                it.distanceKm,
                                it.makingFeePerGram,
                            )
                        },
                        selectedShop?.id,
                        favoriteShopIds.sorted(),
                        hasLocationPermission,
                        effectiveAnchorLat,
                        effectiveAnchorLon,
                        effectiveNearbyRadiusKm,
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
                            searchAnchorLat = effectiveAnchorLat,
                            searchAnchorLon = effectiveAnchorLon,
                            nearbyRadiusKm = effectiveNearbyRadiusKm,
                            favoriteShopIds = favoriteShopIds,
                            onToggleFavorite = { shopId -> currentOnToggleFavorite(shopId) },
                            onOpenShop = { shop -> currentOnOpenShop(shop) },
                            onSelect = currentOnSelect,
                        )
                    }
                },
            )

            // Center on user's current position at startup when available, or fit bounds/anchor on filter/recenter
            LaunchedEffect(
                mappableShops,
                effectiveAnchorLat,
                effectiveAnchorLon,
                effectiveUserLat,
                effectiveUserLon,
                recenterSignal,
                effectiveSearchQuery,
                selectedKarat,
                autoCenterOnUserLocation,
            ) {
                val anchorGeo = if (
                    effectiveAnchorLat != null &&
                    effectiveAnchorLon != null &&
                    isValidMapCoordinatePair(effectiveAnchorLat, effectiveAnchorLon)
                ) {
                    GeoPoint(effectiveAnchorLat, effectiveAnchorLon)
                } else {
                    null
                }
                val userGeo = if (
                    effectiveUserLat != null &&
                    effectiveUserLon != null &&
                    isValidMapCoordinatePair(effectiveUserLat, effectiveUserLon)
                ) {
                    GeoPoint(effectiveUserLat, effectiveUserLon)
                } else {
                    null
                }
                val shouldAutoCenterOnUser = autoCenterOnUserLocation &&
                    userGeo != null &&
                    anchorGeo == null &&
                    recenterSignal == 0 &&
                    effectiveSearchQuery.isBlank() &&
                    selectedKarat == null &&
                    userSelectedShopCounter == 0

                when {
                    shouldAutoCenterOnUser -> {
                        mapView.controller.setZoom(14.5)
                        mapView.controller.setCenter(userGeo)
                        mapView.controller.animateTo(userGeo)
                        hasAutoCenteredOnUser = true
                    }
                    mappableShops.size == 1 && anchorGeo == null -> {
                        val only = mappableShops.first()
                        mapView.controller.setZoom(15.0)
                        mapView.controller.setCenter(GeoPoint(only.latitude, only.longitude))
                        mapView.controller.animateTo(GeoPoint(only.latitude, only.longitude))
                    }
                    anchorGeo != null && mappableShops.isEmpty() -> {
                        mapView.controller.setZoom(14.5)
                        mapView.controller.setCenter(anchorGeo)
                        mapView.controller.animateTo(anchorGeo)
                    }
                    mappableShops.isNotEmpty() -> {
                        val points = buildList {
                            addAll(mappableShops.map { GeoPoint(it.latitude, it.longitude) })
                            if (anchorGeo != null) add(anchorGeo)
                        }
                        val box = BoundingBox.fromGeoPointsSafe(points)
                        mapView.post {
                            runCatching {
                                mapView.zoomToBoundingBox(box, true, 72)
                            }.onFailure {
                                mapView.controller.setZoom(13.5)
                                mapView.controller.setCenter(anchorGeo ?: defaultCenterForShops(mappableShops))
                            }
                        }
                    }
                    userGeo != null && autoCenterOnUserLocation -> {
                        mapView.controller.setZoom(14.5)
                        mapView.controller.setCenter(userGeo)
                        mapView.controller.animateTo(userGeo)
                    }
                    else -> {
                        mapView.controller.setZoom(12.0)
                        mapView.controller.setCenter(defaultCenterForShops(emptyList()))
                    }
                }
            }

            LaunchedEffect(selectedShop?.id, userSelectedShopCounter) {
                if (userSelectedShopCounter > 0) {
                    selectedShop?.let { shop ->
                        if (hasValidMapCoordinates(shop)) {
                            mapView.controller.animateTo(GeoPoint(shop.latitude, shop.longitude))
                        }
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
                            centerMapOnUserLocation(applyAsNearbySearchAnchor = true)
                        } else {
                            pendingCenterOnMyLocation = true
                            pendingNearbyMyLocationSearch = true
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
                                style = AppText.body.copy(
                                    color = AppColors.Alert,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
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
                                text = "فعّل إذن الموقع لعرض المحلات الأقرب إليك",
                                modifier = Modifier.weight(1f),
                                style = AppText.body.copy(
                                    color = AppColors.Ink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
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

        if (displayedShops.isEmpty() || selectedShop == null) {
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = AppColors.Muted,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = if (effectiveNearbyRadiusKm != null) {
                            "لا توجد محلات مطابقة ضمن نطاق ${effectiveNearbyRadiusKm.toInt()} كم"
                        } else {
                            "لا توجد محلات مطابقة لعرضها على الخريطة"
                        },
                        style = AppText.body.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "يمكنك توسيع نطاق المسافة أو إعادة ضبط البحث والفلاتر لعرض جميع محلات الصاغة.",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (effectiveNearbyRadiusKm != null && effectiveNearbyRadiusKm < 50.0) {
                            Button(
                                onClick = { updateRadius(50.0) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("map_expand_radius_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Ink,
                                    contentColor = AppColors.BrassLight,
                                ),
                            ) {
                                Text(
                                    text = "توسيع النطاق إلى 50 كم",
                                    style = AppText.body.copy(
                                        color = AppColors.BrassLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    ),
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                clearAnchor()
                                updateQuery("")
                                onResetFilters()
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("map_empty_reset_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AppColors.Brass),
                        ) {
                            Text(
                                text = "إعادة ضبط الفلاتر",
                                style = AppText.body.copy(
                                    color = AppColors.Ink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                ),
                            )
                        }
                    }
                }
            }
        } else {
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
                        GoldMarketplaceImage(
                            imageUrl = selectedShop.logoUrl ?: selectedShop.coverImageUrl,
                            base64Thumbnail = null,
                            contentDescription = selectedShop.name,
                            modifier = Modifier.size(48.dp),
                            cornerRadius = 10.dp,
                            isThumbnail = true,
                            fallback = {
                                ShopGoldMedallion(
                                    shopId = selectedShop.id,
                                    modifier = Modifier.fillMaxSize(),
                                    cornerRadius = 10.dp,
                                )
                            },
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
                                    tint = if (selectedShop.hasVerifiedRating) AppColors.Brass else AppColors.Muted,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                val ratingLabel = if (selectedShop.hasVerifiedRating) {
                                    "${selectedShop.rating} (${selectedShop.reviews})"
                                } else {
                                    "التقييم غير متاح"
                                }
                                val metaSuffix = buildString {
                                    append(" · ${selectedShop.city} - ${selectedShop.area}")
                                    if (selectedShop.makingFeePerGram > 0.0) {
                                        append(" · مصنعية ${selectedShop.makingFeePerGram.toInt()} ج.م/جم")
                                    }
                                }
                                Text(
                                    text = ratingLabel + metaSuffix,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                                )
                            }
                        }
                        Surface(
                            onClick = { onToggleFavoriteShop(selectedShop.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("map_selected_favorite_button"),
                            shape = CircleShape,
                            color = if (isFavorite) AppColors.AlertSoft else AppColors.ParchmentDeep,
                            border = BorderStroke(
                                1.dp,
                                if (isFavorite) AppColors.Alert.copy(alpha = 0.45f) else AppColors.Line,
                            ),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                                    tint = if (isFavorite) AppColors.Alert else AppColors.Ink,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    val selectedShopKarats = remember(selectedShop.id, shopKaratsProvider) {
                        shopKaratsProvider?.invoke(selectedShop).orEmpty().sortedDescending()
                    }
                    if (selectedShopKarats.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("map_selected_shop_karats"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "الأعيرة:",
                                style = AppText.body.copy(
                                    color = AppColors.Muted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            selectedShopKarats.forEach { karat ->
                                val isMatchedKarat = selectedKarat == karat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isMatchedKarat) AppColors.Ink else AppColors.ParchmentDeep,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isMatchedKarat) AppColors.Brass else AppColors.Brass.copy(alpha = 0.35f),
                                    ),
                                ) {
                                    Text(
                                        text = "عيار $karat",
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        style = AppText.body.copy(
                                            color = if (isMatchedKarat) AppColors.BrassLight else AppColors.Ink,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { onOpenShop(selectedShop) },
                            modifier = Modifier
                                .weight(1f)
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = AppText.body.copy(
                                    color = AppColors.BrassLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                ),
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                openDirections(
                                    context = context,
                                    address = selectedShop.address,
                                    latitude = selectedShop.latitude,
                                    longitude = selectedShop.longitude,
                                    label = selectedShop.name,
                                ) {}
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("map_selected_directions_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AppColors.Brass),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Directions,
                                contentDescription = "الاتجاهات إلى ${selectedShop.name}",
                                tint = AppColors.Brass,
                                modifier = Modifier.size(18.dp),
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

        MapProximityFilterStrip(
            verifiedOnly = effectiveVerifiedOnly,
            onToggleVerifiedOnly = { updateVerified(!effectiveVerifiedOnly) },
            nearbyRadiusKm = effectiveNearbyRadiusKm,
            onSelectRadius = { radius ->
                updateRadius(radius)
                if (radius != null && effectiveAnchorLat == null && effectiveAnchorLon == null) {
                    val userGeo = if (hasAnyLocationPermission(context)) resolveImmediateUserGeoPoint() else null
                    if (userGeo != null) {
                        applyAnchorAndDefaultRadius(userGeo.latitude, userGeo.longitude, "موقعي الحالي", radius)
                    } else {
                        val center = mapView.mapCenter
                        val lat = if (isValidMapCoordinatePair(center.latitude, center.longitude)) {
                            center.latitude
                        } else {
                            defaultCenterForShops(shops).latitude
                        }
                        val lon = if (isValidMapCoordinatePair(center.latitude, center.longitude)) {
                            center.longitude
                        } else {
                            defaultCenterForShops(shops).longitude
                        }
                        applyAnchorAndDefaultRadius(lat, lon, "مركز الخريطة المحدد", radius)
                    }
                }
            },
            onSearchCurrentMapArea = ::triggerSearchCurrentMapArea,
            searchAnchorLat = effectiveAnchorLat,
            searchAnchorLon = effectiveAnchorLon,
            searchAnchorLabel = effectiveAnchorLabel,
            onClearAnchor = clearAnchor,
            matchedCount = mappableShops.size,
            statusMessage = locationHintMessage,
        )
    }
}
