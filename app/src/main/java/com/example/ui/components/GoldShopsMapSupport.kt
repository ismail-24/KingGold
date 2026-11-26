package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.model.Shop
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

enum class MapLayerMode(val arabicLabel: String) {
    STANDARD("خريطة"),
    SATELLITE("قمر صناعي"),
}

data class MapRadiusOption(
    val radiusKm: Double?,
    val label: String,
    val testTag: String,
)

val MAP_RADIUS_OPTIONS: List<MapRadiusOption> = listOf(
    MapRadiusOption(null, "الكل", "map_radius_chip_all"),
    MapRadiusOption(2.0, "2 كم", "map_radius_chip_2"),
    MapRadiusOption(5.0, "5 كم", "map_radius_chip_5"),
    MapRadiusOption(15.0, "15 كم", "map_radius_chip_15"),
    MapRadiusOption(50.0, "50 كم", "map_radius_chip_50"),
)

data class MapKaratOption(
    val karat: Int?,
    val label: String,
    val testTag: String,
)

val MAP_KARAT_OPTIONS: List<MapKaratOption> = listOf(
    MapKaratOption(null, "كل الأعيرة", "map_karat_chip_all"),
    MapKaratOption(24, "عيار 24", "map_karat_chip_24"),
    MapKaratOption(21, "عيار 21", "map_karat_chip_21"),
    MapKaratOption(18, "عيار 18", "map_karat_chip_18"),
)

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

fun isValidMapCoordinatePair(latitude: Double, longitude: Double): Boolean {
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    if (latitude == 0.0 && longitude == 0.0) return false
    return latitude in -85.0..85.0 && longitude in -180.0..180.0
}

fun hasValidMapCoordinates(shop: Shop): Boolean =
    isValidMapCoordinatePair(shop.latitude, shop.longitude)

fun calculateHaversineDistanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    if (!isValidMapCoordinatePair(lat1, lon1) || !isValidMapCoordinatePair(lat2, lon2)) {
        return Double.MAX_VALUE
    }
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2.0) * sin(dLon / 2.0)
    val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
    val rawKm = earthRadiusKm * c
    return round(rawKm * 10.0) / 10.0
}

private val ARABIC_DIGIT_MAP = mapOf(
    '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
    '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
    '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
    '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
)

private val EXPLICIT_KARAT_PHRASE_REGEX = Regex(
    """(?:(?:ذهب|gold)\s+)?(?:عيار|عيّار|karat|kt|k)\s*[:\-]?\s*(24|21|18|14)\b|\b(24|21|18|14)\s*(?:k|kt|ق|قيراط)(?:\s+(?:ذهب|gold))?\b""",
)
private val STANDALONE_KARAT_REGEX = Regex("""(?:^|\s)(24|21|18|14)(?:\s|$)""")
private val GOLD_PREFIX_WITH_KARAT_REGEX = Regex("""(?:^|\s)(?:ذهب|gold)(?:\s|$)""")

data class ParsedMapSearchQuery(
    val normalizedRaw: String,
    val detectedKarat: Int?,
    val remainingText: String,
)

fun normalizeShopSearchText(input: String): String {
    if (input.isBlank()) return ""
    val sb = StringBuilder(input.length)
    for (ch in input) {
        sb.append(ARABIC_DIGIT_MAP[ch] ?: ch)
    }
    return sb.toString()
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("""\s+"""), " ")
}

fun parseShopSearchQuery(query: String): ParsedMapSearchQuery {
    val normalized = normalizeShopSearchText(query)
    if (normalized.isEmpty()) {
        return ParsedMapSearchQuery(
            normalizedRaw = "",
            detectedKarat = null,
            remainingText = "",
        )
    }

    val explicitMatch = EXPLICIT_KARAT_PHRASE_REGEX.find(normalized)
    if (explicitMatch != null) {
        val karatStr = explicitMatch.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
        val karat = karatStr?.toIntOrNull()
        val stripped = normalized.removeRange(explicitMatch.range)
            .replace(GOLD_PREFIX_WITH_KARAT_REGEX, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return ParsedMapSearchQuery(
            normalizedRaw = normalized,
            detectedKarat = karat,
            remainingText = stripped,
        )
    }

    val standaloneMatch = STANDALONE_KARAT_REGEX.find(normalized)
    if (standaloneMatch != null) {
        val karat = standaloneMatch.groupValues[1].toIntOrNull()
        val stripped = normalized.removeRange(standaloneMatch.range)
            .replace(GOLD_PREFIX_WITH_KARAT_REGEX, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return ParsedMapSearchQuery(
            normalizedRaw = normalized,
            detectedKarat = karat,
            remainingText = stripped,
        )
    }

    return ParsedMapSearchQuery(
        normalizedRaw = normalized,
        detectedKarat = null,
        remainingText = normalized,
    )
}

private fun matchesShopText(
    shop: Shop,
    query: String,
    extraSearchableText: List<String> = emptyList(),
): Boolean {
    if (query.isEmpty()) return true
    return normalizeShopSearchText(shop.name).contains(query) ||
        normalizeShopSearchText(shop.city).contains(query) ||
        normalizeShopSearchText(shop.area).contains(query) ||
        normalizeShopSearchText(shop.address).contains(query) ||
        normalizeShopSearchText(shop.description).contains(query) ||
        extraSearchableText.any { normalizeShopSearchText(it).contains(query) }
}

fun matchesShopSearchAndKarat(
    shop: Shop,
    shopKarats: Set<Int>,
    parsedQuery: ParsedMapSearchQuery,
    selectedKarat: Int? = null,
    extraSearchableText: List<String> = emptyList(),
    hasKaratsMetadata: Boolean = true,
): Boolean {
    val matchesSelectedKarat = if (hasKaratsMetadata) {
        selectedKarat == null || shopKarats.contains(selectedKarat)
    } else {
        true
    }
    if (!matchesSelectedKarat) return false
    if (parsedQuery.normalizedRaw.isEmpty()) return true

    val matchesRawText = matchesShopText(shop, parsedQuery.normalizedRaw, extraSearchableText)
    val detectedKarat = parsedQuery.detectedKarat
    if (detectedKarat == null || !hasKaratsMetadata) {
        return matchesRawText
    }

    val matchesDetectedKarat = shopKarats.contains(detectedKarat)
    val matchesRemainingText = parsedQuery.remainingText.isEmpty() ||
        matchesShopText(shop, parsedQuery.remainingText, extraSearchableText)
    return matchesRawText || (matchesDetectedKarat && matchesRemainingText)
}

fun filterAndSortShopsForMap(
    shops: List<Shop>,
    searchQuery: String,
    verifiedOnly: Boolean,
    anchorLat: Double?,
    anchorLon: Double?,
    nearbyRadiusKm: Double?,
    sortByNearest: Boolean = true,
    selectedKarat: Int? = null,
    shopKaratsProvider: ((Shop) -> Set<Int>)? = null,
): List<Shop> {
    val hasAnchor = anchorLat != null &&
        anchorLon != null &&
        isValidMapCoordinatePair(anchorLat, anchorLon)
    val parsedQuery = parseShopSearchQuery(searchQuery)

    val shopsWithDistance = if (hasAnchor) {
        shops.map { shop ->
            if (hasValidMapCoordinates(shop)) {
                val computedKm = calculateHaversineDistanceKm(
                    lat1 = anchorLat!!,
                    lon1 = anchorLon!!,
                    lat2 = shop.latitude,
                    lon2 = shop.longitude,
                )
                shop.copy(distanceKm = computedKm)
            } else {
                shop
            }
        }
    } else {
        shops
    }

    val filtered = shopsWithDistance.filter { shop ->
        val matchesVerified = !verifiedOnly || shop.verified
        val shopKarats = shopKaratsProvider?.invoke(shop).orEmpty()
        val matchesQueryAndKarat = matchesShopSearchAndKarat(
            shop = shop,
            shopKarats = shopKarats,
            parsedQuery = parsedQuery,
            selectedKarat = selectedKarat,
            hasKaratsMetadata = shopKaratsProvider != null,
        )
        val matchesRadius = if (hasAnchor && nearbyRadiusKm != null) {
            hasValidMapCoordinates(shop) && shop.distanceKm <= nearbyRadiusKm
        } else {
            true
        }
        matchesVerified && matchesQueryAndKarat && matchesRadius
    }

    return if (sortByNearest && hasAnchor) {
        filtered.sortedBy { it.distanceKm }
    } else {
        filtered
    }
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

@SuppressLint("MissingPermission")
fun resolveBestKnownLocation(context: Context): GeoPoint? {
    if (!hasAnyLocationPermission(context)) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
    var best: Location? = null
    for (provider in providers) {
        val candidate = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
        if (!isValidMapCoordinatePair(candidate.latitude, candidate.longitude)) continue
        if (best == null || candidate.time >= best.time) {
            best = candidate
        }
    }
    return best?.let { GeoPoint(it.latitude, it.longitude) }
}

@SuppressLint("MissingPermission")
fun requestCurrentLocationFix(
    context: Context,
    cancellationSignal: CancellationSignal? = null,
    onResolved: (GeoPoint?) -> Unit,
) {
    if (!hasAnyLocationPermission(context)) {
        onResolved(null)
        return
    }
    val bestKnown = resolveBestKnownLocation(context)
    if (bestKnown != null) {
        onResolved(bestKnown)
        return
    }
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (lm == null) {
        onResolved(null)
        return
    }
    val provider = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
    ).firstOrNull { name ->
        runCatching { lm.isProviderEnabled(name) }.getOrDefault(false)
    } ?: LocationManager.NETWORK_PROVIDER

    val started = runCatching {
        LocationManagerCompat.getCurrentLocation(
            lm,
            provider,
            cancellationSignal,
            ContextCompat.getMainExecutor(context),
        ) { location ->
            val point = location?.takeIf {
                isValidMapCoordinatePair(it.latitude, it.longitude)
            }?.let { GeoPoint(it.latitude, it.longitude) }
            onResolved(point)
        }
        true
    }.getOrDefault(false)
    if (!started) {
        onResolved(null)
    }
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

internal fun isNetworkAvailable(context: Context): Boolean = runCatching {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}.getOrDefault(true)

internal fun createGoldShopMarkerDrawable(
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

internal fun createHeartIconDrawable(
    context: Context,
    isFavorite: Boolean,
): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (24f * density).toInt().coerceAtLeast(24)
    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)
    val w = sizePx.toFloat()
    val h = sizePx.toFloat()

    val heartPath = Path().apply {
        moveTo(w * 0.50f, h * 0.84f)
        cubicTo(w * 0.16f, h * 0.62f, w * 0.08f, h * 0.36f, w * 0.24f, h * 0.22f)
        cubicTo(w * 0.36f, h * 0.12f, w * 0.48f, h * 0.20f, w * 0.50f, h * 0.29f)
        cubicTo(w * 0.52f, h * 0.20f, w * 0.64f, h * 0.12f, w * 0.76f, h * 0.22f)
        cubicTo(w * 0.92f, h * 0.36f, w * 0.84f, h * 0.62f, w * 0.50f, h * 0.84f)
        close()
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        if (isFavorite) {
            color = android.graphics.Color.rgb(180, 45, 45)
            style = Paint.Style.FILL
        } else {
            color = android.graphics.Color.rgb(112, 103, 95)
            style = Paint.Style.STROKE
            strokeWidth = (1.9f * density).coerceAtLeast(1.5f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }
    canvas.drawPath(heartPath, paint)
    return BitmapDrawable(context.resources, bitmap)
}

internal class GoldShopMarkerInfoWindow(
    mapView: MapView,
    private val shop: Shop,
    private val isFavorite: Boolean,
    private val onToggleFavorite: ((String) -> Unit)?,
    private val onOpenShop: ((Shop) -> Unit)? = null,
) : InfoWindow(buildMarkerBubbleView(mapView.context), mapView) {

    override fun onOpen(item: Any?) {
        val root = mView as? LinearLayout ?: return
        val ctx = root.context
        val titleView = root.findViewWithTag<TextView>("marker_info_title")
        val snippetView = root.findViewWithTag<TextView>("marker_info_snippet")
        val textCol = root.findViewWithTag<LinearLayout>("marker_info_text_col")
        val favoriteBtn = root.findViewWithTag<ImageButton>("marker_info_favorite_${shop.id}")
            ?: root.findViewWithTag<ImageButton>("marker_info_favorite_btn")

        titleView?.text = shop.name
        val statusLabel = if (shop.verified) "معتمد" else "قيد التحقق"
        snippetView?.text = "${shop.area}، ${shop.city} · ${shop.distanceKm} كم · $statusLabel"

        textCol?.setOnClickListener {
            onOpenShop?.invoke(shop)
        }

        favoriteBtn?.apply {
            tag = "marker_info_favorite_${shop.id}"
            contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة"
            setImageDrawable(createHeartIconDrawable(ctx, isFavorite))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(
                    if (isFavorite) {
                        android.graphics.Color.rgb(252, 232, 232)
                    } else {
                        android.graphics.Color.rgb(245, 239, 228)
                    },
                )
            }
            setOnClickListener {
                onToggleFavorite?.invoke(shop.id)
            }
        }
    }

    override fun onClose() {
        val root = mView as? LinearLayout ?: return
        root.findViewWithTag<View>("marker_info_text_col")?.setOnClickListener(null)
        root.findViewWithTag<View>("marker_info_favorite_${shop.id}")?.setOnClickListener(null)
    }

    companion object {
        private fun dp(context: Context, value: Int): Int =
            (value * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

        private fun buildMarkerBubbleView(context: Context): View {
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(context, 12), dp(context, 8), dp(context, 10), dp(context, 8))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(context, 14).toFloat()
                    setColor(android.graphics.Color.WHITE)
                    setStroke(dp(context, 1), android.graphics.Color.rgb(181, 138, 46))
                }
                elevation = dp(context, 4).toFloat()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }

            val textColumn = LinearLayout(context).apply {
                tag = "marker_info_text_col"
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = dp(context, 8)
                }
            }

            val titleView = TextView(context).apply {
                tag = "marker_info_title"
                setTextColor(android.graphics.Color.rgb(20, 20, 22))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                maxLines = 1
                maxWidth = dp(context, 190)
                ellipsize = TextUtils.TruncateAt.END
            }

            val snippetView = TextView(context).apply {
                tag = "marker_info_snippet"
                setTextColor(android.graphics.Color.rgb(107, 99, 91))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                maxLines = 1
                maxWidth = dp(context, 190)
                ellipsize = TextUtils.TruncateAt.END
            }

            textColumn.addView(titleView)
            textColumn.addView(snippetView)

            val minTap = dp(context, 40)
            val favoriteButton = ImageButton(context).apply {
                tag = "marker_info_favorite_btn"
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                layoutParams = LinearLayout.LayoutParams(minTap, minTap)
                setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8))
            }

            container.addView(textColumn)
            container.addView(favoriteButton)
            return container
        }
    }
}

fun createGoldShopMarker(
    mapView: MapView,
    shop: Shop,
    isSelected: Boolean,
    selectedIcon: Drawable,
    verifiedIcon: Drawable,
    defaultIcon: Drawable,
    isFavorite: Boolean = false,
    onToggleFavorite: ((String) -> Unit)? = null,
    onOpenShop: ((Shop) -> Unit)? = null,
    onSelect: (String) -> Unit,
): Marker = Marker(mapView).apply {
    id = shop.id
    position = GeoPoint(shop.latitude, shop.longitude)
    title = shop.name
    val sourceLabel = if (shop.verified) "معتمد" else "قيد التحقق"
    val savedSuffix = if (isFavorite) " · محفوظ" else ""
    snippet = "${shop.area}، ${shop.city} · ${shop.distanceKm} كم · $sourceLabel$savedSuffix"
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    icon = when {
        isSelected -> selectedIcon
        shop.verified -> verifiedIcon
        else -> defaultIcon
    }
    infoWindow = GoldShopMarkerInfoWindow(
        mapView = mapView,
        shop = shop,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onOpenShop = onOpenShop,
    )
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
    searchAnchorLat: Double? = null,
    searchAnchorLon: Double? = null,
    nearbyRadiusKm: Double? = null,
    favoriteShopIds: Set<String> = emptySet(),
    onToggleFavorite: ((String) -> Unit)? = null,
    onOpenShop: ((Shop) -> Unit)? = null,
    onSelect: (String) -> Unit,
): Int {
    InfoWindow.closeAllInfoWindowsOn(mapView)
    mapView.overlays.removeAll { it is Marker || it is Polygon }

    if (searchAnchorLat != null && searchAnchorLon != null &&
        isValidMapCoordinatePair(searchAnchorLat, searchAnchorLon)
    ) {
        val anchorPoint = GeoPoint(searchAnchorLat, searchAnchorLon)
        if (nearbyRadiusKm != null && nearbyRadiusKm > 0.0) {
            val radiusCircle = Polygon(mapView).apply {
                points = Polygon.pointsAsCircle(anchorPoint, nearbyRadiusKm * 1000.0)
                fillPaint.color = android.graphics.Color.argb(30, 181, 138, 46)
                outlinePaint.color = android.graphics.Color.argb(190, 181, 138, 46)
                outlinePaint.strokeWidth = 3f
            }
            mapView.overlays.add(radiusCircle)
        }
        val anchorSpot = Polygon(mapView).apply {
            points = Polygon.pointsAsCircle(anchorPoint, 75.0)
            fillPaint.color = android.graphics.Color.argb(210, 20, 20, 22)
            outlinePaint.color = android.graphics.Color.rgb(227, 189, 98)
            outlinePaint.strokeWidth = 3.5f
        }
        mapView.overlays.add(anchorSpot)
    }

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
            isFavorite = favoriteShopIds.contains(shop.id),
            onToggleFavorite = onToggleFavorite,
            onOpenShop = onOpenShop,
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

@Composable
internal fun MapSearchBarHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFindNearMyLocation: () -> Unit,
    isMyLocationActive: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_search_controls_card"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .testTag("map_search_input"),
            placeholder = {
                Text(
                    text = "ابحث باسم المحل أو العيار (مثل: 24، 21، 18)...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "بحث باسم المحل أو عيار الذهب",
                    tint = AppColors.Brass,
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("map_search_clear"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "مسح بحث الخريطة",
                            tint = AppColors.Muted,
                        )
                    }
                }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AppColors.Brass,
                unfocusedBorderColor = AppColors.Line,
            ),
        )

        FilterChip(
            selected = isMyLocationActive,
            onClick = onFindNearMyLocation,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("map_nearby_my_location_button"),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = "محلات قريبة من موقعي",
                    tint = if (isMyLocationActive) AppColors.BrassLight else AppColors.Brass,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = {
                Text(
                    text = "قريب من موقعي",
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

@Composable
internal fun MapKaratFilterRow(
    selectedKarat: Int?,
    onSelectKarat: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("map_karat_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MAP_KARAT_OPTIONS.forEach { option ->
            val selected = selectedKarat == option.karat
            FilterChip(
                selected = selected,
                onClick = {
                    val next = if (option.karat != null && selected) null else option.karat
                    onSelectKarat(next)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(option.testTag),
                leadingIcon = if (option.karat != null) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = if (selected) AppColors.BrassLight else AppColors.Brass,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else {
                    null
                },
                label = {
                    Text(
                        text = option.label,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MapProximityFilterStrip(
    verifiedOnly: Boolean,
    onToggleVerifiedOnly: () -> Unit,
    nearbyRadiusKm: Double?,
    onSelectRadius: (Double?) -> Unit,
    onSearchCurrentMapArea: () -> Unit,
    searchAnchorLat: Double?,
    searchAnchorLon: Double?,
    searchAnchorLabel: String?,
    onClearAnchor: () -> Unit,
    matchedCount: Int,
    statusMessage: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isAreaActive = searchAnchorLabel != null && searchAnchorLabel != "موقعي الحالي"
            FilterChip(
                selected = isAreaActive,
                onClick = onSearchCurrentMapArea,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("map_search_area_button"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.LocationSearching,
                        contentDescription = "البحث في هذه المنطقة على الخريطة",
                        tint = if (isAreaActive) AppColors.BrassLight else AppColors.Ink,
                        modifier = Modifier.size(15.dp),
                    )
                },
                label = {
                    Text(
                        text = "البحث في هذه المنطقة",
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

            FilterChip(
                selected = verifiedOnly,
                onClick = onToggleVerifiedOnly,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("map_verified_only_chip"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = "تصفية المحلات الموثّقة فقط",
                        tint = if (verifiedOnly) AppColors.BrassLight else AppColors.Muted,
                        modifier = Modifier.size(15.dp),
                    )
                },
                label = {
                    Text(
                        text = "موثّقة فقط",
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

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("map_radius_chips_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MAP_RADIUS_OPTIONS.forEach { option ->
                val isSelected = nearbyRadiusKm == option.radiusKm
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectRadius(option.radiusKm) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(option.testTag),
                    leadingIcon = if (option.radiusKm != null) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Radar,
                                contentDescription = null,
                                tint = if (isSelected) AppColors.BrassLight else AppColors.Brass,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    } else {
                        null
                    },
                    label = {
                        Text(
                            text = option.label,
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

        if (searchAnchorLat != null && searchAnchorLon != null) {
            val radiusLabel = nearbyRadiusKm?.let { "${it.toInt()} كم" } ?: "كل المسافات"
            val coordText = String.format(Locale.US, "%.3f, %.3f", searchAnchorLat, searchAnchorLon)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_location_status_banner"),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${searchAnchorLabel ?: "الموقع المحدد"} · نطاق $radiusLabel · $matchedCount محل",
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "الإحداثيات: $coordText · اضغط مطولاً على الخريطة لتغيير الموقع",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 10.5.sp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onClearAnchor,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_clear_location_anchor_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "إلغاء التصفية بالموقع",
                            tint = AppColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        } else if (!statusMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_location_hint_banner"),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.dp, AppColors.Line),
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = AppText.body.copy(color = AppColors.Ink, fontSize = 11.5.sp),
                )
            }
        }
    }
}
