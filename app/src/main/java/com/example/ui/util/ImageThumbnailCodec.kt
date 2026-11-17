package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.ui.theme.AppColors
import java.io.ByteArrayOutputStream

/**
 * Compresses an Android Photo Picker [Uri] into a compact JPEG Base64 string (~15-30 KB max)
 * so multiple attachments can be persisted cleanly inside bounded Firestore arrays without
 * requiring storage permissions or unprovisioned external storage buckets.
 */
object ImageThumbnailCodec {
    fun encodeUriToBase64Thumbnail(context: Context, uri: Uri, maxDimPx: Int = 360): String? {
        return runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > maxDimPx * 2 || (bounds.outHeight / sampleSize) > maxDimPx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val ratio = minOf(
                maxDimPx.toFloat() / decoded.width.toFloat(),
                maxDimPx.toFloat() / decoded.height.toFloat(),
                1f,
            )
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            var quality = 72
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > 35_000 && quality > 35) {
                out.reset()
                quality -= 12
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    fun decodeBase64ToBitmap(encoded: String): Bitmap? {
        if (encoded.isBlank()) return null
        return runCatching {
            val clean = encoded.substringAfter("base64,", encoded)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}

@Composable
fun Base64ThumbnailImage(
    encoded: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(encoded) { ImageThumbnailCodec.decodeBase64ToBitmap(encoded) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(AppColors.ParchmentDeep),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = contentDescription,
                tint = AppColors.Muted,
            )
        }
    }
}
