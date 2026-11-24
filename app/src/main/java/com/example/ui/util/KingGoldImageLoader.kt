package com.example.ui.util

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized Coil [ImageLoader] configured for marketplace photography:
 * - Bounded memory cache (25% heap) and local disk cache (64 MB) to reuse images across navigation.
 * - Background OkHttp fetching with sane timeouts and explicit User-Agent so CDN hosts never block.
 * - List thumbnails request compact 360px widths while Product Detail requests 960px high-res widths.
 */
object KingGoldImageLoader {
    private const val DISK_CACHE_DIR = "kinggold_image_cache"
    private const val DISK_CACHE_MAX_BYTES = 64L * 1024L * 1024L
    private const val MEMORY_CACHE_PERCENT = 0.25
    const val THUMBNAIL_WIDTH_PX = 360
    const val DETAIL_WIDTH_PX = 960

    @Volatile
    private var instance: ImageLoader? = null
    private val globalRegistered = AtomicBoolean(false)

    fun get(context: Context): ImageLoader {
        val existing = instance
        if (existing != null) return existing
        return synchronized(this) {
            val rechecked = instance
            if (rechecked != null) {
                rechecked
            } else {
                val appContext = context.applicationContext ?: context
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "KingGoldAndroid/1.0 (https://kinggold.app)")
                            .build()
                        chain.proceed(request)
                    }
                    .build()

                val created = ImageLoader.Builder(appContext)
                    .okHttpClient(okHttpClient)
                    .memoryCache {
                        MemoryCache.Builder(appContext)
                            .maxSizePercent(MEMORY_CACHE_PERCENT)
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
                    .crossfade(200)
                    .build()

                instance = created
                if (globalRegistered.compareAndSet(false, true)) {
                    runCatching { Coil.setImageLoader(created) }
                }
                created
            }
        }
    }

    fun wikimediaThumbnailUrl(encodedFileName: String): String =
        "https://commons.wikimedia.org/wiki/Special:FilePath/$encodedFileName?width=$THUMBNAIL_WIDTH_PX"

    fun wikimediaDetailUrl(encodedFileName: String): String =
        "https://commons.wikimedia.org/wiki/Special:FilePath/$encodedFileName?width=$DETAIL_WIDTH_PX"
}
