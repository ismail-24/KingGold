package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openDirections(
    context: Context,
    address: String,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    label: String = address,
    onFallback: () -> Unit,
) {
    try {
        val hasCoords = latitude in -85.0..85.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)
        val uri = if (hasCoords) {
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        onFallback()
    }
}

fun openExternalWebsite(context: Context, url: String, onFallback: () -> Unit = {}) {
    try {
        val normalized = url.trim()
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            onFallback()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        onFallback()
    }
}

fun dialPhone(context: Context, phone: String, onFallback: () -> Unit) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        onFallback()
    }
}
