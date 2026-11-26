package com.example.ui.util

import android.content.Context
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.BitmapFactoryDecoder
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.example.KingGoldApplication
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

enum class GoldImageTier(val widthPx: Int) {
    THUMBNAIL(360),
    CARD_MEDIUM(600),
    DETAIL_HIGH_RES(960),
}

/**
 * Shared access & request builder for the application's Coil [ImageLoader] configured in
 * [KingGoldApplication]:
 * - Bounded memory cache (25% heap with strong references) + 64 MB local disk cache.
 * - Automatic WebP decoding (`ImageDecoderDecoder` on API 28+, `BitmapFactoryDecoder` fallback)
 *   plus WebP/AVIF Accept header negotiation via pooled OkHttp.
 * - Multi-resolution tier requests (360px thumbnails, 600px product/shop cards, 960px detail gallery).
 * - RGB_565 + hardware bitmaps for low memory footprint on opaque photography.
 * - Background prefetch helper for upcoming list/grid items.
 */
object KingGoldImageLoader {
    private const val DISK_CACHE_DIR = KingGoldApplication.DISK_CACHE_DIR
    private const val DISK_CACHE_MAX_BYTES = KingGoldApplication.DISK_CACHE_MAX_BYTES
    private const val MEMORY_CACHE_PERCENT = KingGoldApplication.MEMORY_CACHE_PERCENT
    const val THUMBNAIL_WIDTH_PX = 360
    const val CARD_MEDIUM_WIDTH_PX = 600
    const val DETAIL_WIDTH_PX = 960

    @Volatile
    private var instance: ImageLoader? = null
    private val globalRegistered = AtomicBoolean(false)

    fun get(context: Context): ImageLoader {
        val existing = instance
        if (existing != null) return existing
        val appContext = context.applicationContext ?: context
        if (appContext is KingGoldApplication) {
            return appContext.newImageLoader()
        }
        if (appContext is ImageLoaderFactory) {
            return getOrCreate(appContext) { appContext.newImageLoader() }
        }
        return getOrCreate(appContext) { createFallbackImageLoader(it) }
    }

    internal fun getOrCreate(
        context: Context,
        factory: (Context) -> ImageLoader,
    ): ImageLoader {
        val existing = instance
        if (existing != null) return existing
        return synchronized(this) {
            val rechecked = instance
            if (rechecked != null) {
                rechecked
            } else {
                val appContext = context.applicationContext ?: context
                val created = factory(appContext)
                instance = created
                if (globalRegistered.compareAndSet(false, true)) {
                    runCatching { Coil.setImageLoader(created) }
                }
                created
            }
        }
    }

    private fun createFallbackImageLoader(appContext: Context): ImageLoader {
        val dispatcher = Dispatcher().apply {
            maxRequests = KingGoldApplication.MAX_CONCURRENT_REQUESTS
            maxRequestsPerHost = KingGoldApplication.MAX_REQUESTS_PER_HOST
        }
        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", KingGoldApplication.USER_AGENT)
                    .header("Accept", KingGoldApplication.WEBP_ACCEPT_HEADER)
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(appContext)
            .okHttpClient(okHttpClient)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                }
                add(GifDecoder.Factory())
                add(BitmapFactoryDecoder.Factory(maxParallelism = KingGoldApplication.MAX_DECODE_PARALLELISM))
            }
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(appContext.cacheDir, DISK_CACHE_DIR))
                    .maxSizeBytes(DISK_CACHE_MAX_BYTES)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .allowHardware(true)
            .crossfade(KingGoldApplication.CROSSFADE_DURATION_MS)
            .build()
    }

    fun buildImageRequest(
        context: Context,
        url: String,
        tier: GoldImageTier,
        crossfadeEnabled: Boolean = true,
    ): ImageRequest {
        val targetPx = tier.widthPx
        return ImageRequest.Builder(context)
            .data(url)
            .memoryCacheKey("${url}_$targetPx")
            .diskCacheKey("${url}_${tier.name}")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .size(targetPx, targetPx)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .allowHardware(true)
            .crossfade(if (crossfadeEnabled) KingGoldApplication.CROSSFADE_DURATION_MS else 0)
            .build()
    }

    fun prefetch(
        context: Context,
        urls: Collection<String>,
        tier: GoldImageTier = GoldImageTier.THUMBNAIL,
    ) {
        if (urls.isEmpty()) return
        val loader = get(context)
        urls.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
            .forEach { url ->
                loader.enqueue(
                    buildImageRequest(
                        context = context,
                        url = url,
                        tier = tier,
                        crossfadeEnabled = false,
                    ),
                )
            }
    }

    fun wikimediaUrlForTier(encodedFileName: String, tier: GoldImageTier): String =
        "https://commons.wikimedia.org/wiki/Special:FilePath/$encodedFileName?width=${tier.widthPx}"

    fun wikimediaThumbnailUrl(encodedFileName: String): String =
        wikimediaUrlForTier(encodedFileName, GoldImageTier.THUMBNAIL)

    fun wikimediaCardUrl(encodedFileName: String): String =
        wikimediaUrlForTier(encodedFileName, GoldImageTier.CARD_MEDIUM)

    fun wikimediaDetailUrl(encodedFileName: String): String =
        wikimediaUrlForTier(encodedFileName, GoldImageTier.DETAIL_HIGH_RES)
}
