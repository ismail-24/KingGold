package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun openDirections(context: Context, address: String, onFallback: () -> Unit) {
    try {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
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
