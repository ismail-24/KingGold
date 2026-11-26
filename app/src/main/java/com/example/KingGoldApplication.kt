package com.example

import android.app.Application
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
import com.example.ui.util.KingGoldImageLoader
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

/**
 * Application entry point configuring a custom global Coil [ImageLoader] with:
 * - Bounded strong-reference memory cache (25% of app heap)
 * - Persistent 64 MB local disk cache (`kinggold_image_cache`)
 * - Automatic static & animated WebP decoding (`ImageDecoderDecoder` on API 28+,
 *   `BitmapFactoryDecoder` across all supported APIs, plus `Accept: image/webp` negotiation)
 * - Pooled OkHttp transport, RGB_565 + hardware bitmaps, and smooth crossfades for lag-free scrolling.
 */
class KingGoldApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        val customLoader = newImageLoader()
        Coil.setImageLoader(customLoader)
    }

    override fun newImageLoader(): ImageLoader =
        KingGoldImageLoader.getOrCreate(this) { appContext ->
            buildCustomImageLoader(appContext)
        }

    private fun buildCustomImageLoader(appContext: android.content.Context): ImageLoader {
        val dispatcher = Dispatcher().apply {
            maxRequests = MAX_CONCURRENT_REQUESTS
            maxRequestsPerHost = MAX_REQUESTS_PER_HOST
        }
        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", WEBP_ACCEPT_HEADER)
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
                add(BitmapFactoryDecoder.Factory(maxParallelism = MAX_DECODE_PARALLELISM))
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
            .crossfade(CROSSFADE_DURATION_MS)
            .build()
    }

    companion object {
        const val DISK_CACHE_DIR = "kinggold_image_cache"
        const val DISK_CACHE_MAX_BYTES = 64L * 1024L * 1024L
        const val MEMORY_CACHE_PERCENT = 0.25
        const val MAX_CONCURRENT_REQUESTS = 16
        const val MAX_REQUESTS_PER_HOST = 6
        const val MAX_DECODE_PARALLELISM = 4
        const val CROSSFADE_DURATION_MS = 180
        const val USER_AGENT = "KingGoldAndroid/1.0 (https://kinggold.app)"
        const val WEBP_ACCEPT_HEADER = "image/webp,image/avif,image/jpeg,image/png,*/*;q=0.8"
    }
}
