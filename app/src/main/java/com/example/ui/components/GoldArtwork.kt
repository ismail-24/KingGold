package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppColors

/**
 * Custom Canvas geometric emblem for partner gold shops (storefront arch + hallmark crest).
 */
@Composable
fun ShopGoldMedallion(
    shopId: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(AppColors.BrassLight, AppColors.Brass, AppColors.Seal),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Subtle radial sheen
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = size.minDimension * 0.44f,
                center = Offset(cx, cy),
            )

            // Goldsmith arch silhouette
            val archWidth = w * 0.46f
            val archHeight = h * 0.52f
            val left = cx - archWidth / 2f
            val top = cy - archHeight / 2f
            val archPath = Path().apply {
                moveTo(left, top + archHeight)
                lineTo(left, top + archHeight * 0.42f)
                quadraticTo(cx, top - archHeight * 0.08f, left + archWidth, top + archHeight * 0.42f)
                lineTo(left + archWidth, top + archHeight)
                close()
            }
            drawPath(
                path = archPath,
                color = AppColors.Ink.copy(alpha = 0.78f),
                style = Stroke(width = (size.minDimension * 0.045f).coerceAtLeast(2f)),
            )

            // Center diamond hallmark
            val d = size.minDimension * (if (shopId == "s1") 0.12f else if (shopId == "s2") 0.10f else 0.14f)
            val diamond = Path().apply {
                moveTo(cx, cy - d)
                lineTo(cx + d, cy)
                lineTo(cx, cy + d)
                lineTo(cx - d, cy)
                close()
            }
            drawPath(diamond, color = AppColors.Parchment)
        }
    }
}

/**
 * Custom Canvas geometric illustration for gold product categories (bullion, ring, bracelet, chain).
 */
@Composable
fun ProductGoldArtwork(
    category: String,
    karat: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(AppColors.BrassLight, AppColors.Brass),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val stroke = (size.minDimension * 0.055f).coerceAtLeast(2.2f)
            val ink = AppColors.Ink.copy(alpha = 0.85f)

            // Soft highlight circle
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = size.minDimension * 0.36f,
                center = Offset(cx * 0.92f, cy * 0.88f),
            )

            when {
                category.contains("سبيكة") || category.contains("سبائك") || (category.contains("جنيه") && karat == 24) -> {
                    val barW = w * 0.48f
                    val barH = h * 0.32f
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(cx - barW / 2f, cy - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = Stroke(width = stroke),
                    )
                    drawRoundRect(
                        color = AppColors.Parchment.copy(alpha = 0.8f),
                        topLeft = Offset(cx - barW * 0.3f, cy - barH * 0.25f),
                        size = Size(barW * 0.6f, barH * 0.5f),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    )
                }
                category.contains("جنيه") || category.contains("عملات") -> {
                    val r = size.minDimension * 0.28f
                    drawCircle(color = ink, radius = r, center = Offset(cx, cy), style = Stroke(width = stroke))
                    drawCircle(color = AppColors.Parchment.copy(alpha = 0.7f), radius = r * 0.62f, center = Offset(cx, cy), style = Stroke(width = stroke * 0.6f))
                }
                category.contains("خواتم") -> {
                    val r = size.minDimension * 0.22f
                    drawCircle(color = ink, radius = r, center = Offset(cx, cy + r * 0.2f), style = Stroke(width = stroke))
                    val crown = Path().apply {
                        moveTo(cx, cy - r * 1.15f)
                        lineTo(cx + r * 0.42f, cy - r * 0.55f)
                        lineTo(cx, cy - r * 0.1f)
                        lineTo(cx - r * 0.42f, cy - r * 0.55f)
                        close()
                    }
                    drawPath(crown, color = AppColors.Parchment)
                }
                category.contains("أساور") || category.contains("غوايش") -> {
                    val r = size.minDimension * 0.26f
                    drawCircle(color = ink, radius = r, center = Offset(cx - r * 0.22f, cy), style = Stroke(width = stroke))
                    drawCircle(color = AppColors.Parchment.copy(alpha = 0.85f), radius = r * 0.85f, center = Offset(cx + r * 0.25f, cy), style = Stroke(width = stroke * 0.8f))
                }
                category.contains("حلق") || category.contains("أقراط") -> {
                    val r = size.minDimension * 0.17f
                    drawCircle(color = ink, radius = r, center = Offset(cx - r * 1.25f, cy), style = Stroke(width = stroke))
                    drawCircle(color = AppColors.Parchment, radius = r, center = Offset(cx + r * 1.25f, cy), style = Stroke(width = stroke))
                }
                category.contains("أطقم") || category.contains("طقم") || category.contains("شبكة") -> {
                    val outer = size.minDimension * 0.28f
                    drawCircle(color = ink, radius = outer, center = Offset(cx, cy - outer * 0.1f), style = Stroke(width = stroke))
                    drawCircle(color = AppColors.Parchment, radius = outer * 0.35f, center = Offset(cx - outer * 1.1f, cy + outer * 0.55f), style = Stroke(width = stroke * 0.8f))
                    drawCircle(color = AppColors.Parchment, radius = outer * 0.35f, center = Offset(cx + outer * 1.1f, cy + outer * 0.55f), style = Stroke(width = stroke * 0.8f))
                }
                category.contains("تعليقات") || category.contains("تصاميم") || category.contains("دلاية") -> {
                    val r = size.minDimension * 0.16f
                    drawCircle(color = ink, radius = r * 0.45f, center = Offset(cx, cy - r * 1.4f), style = Stroke(width = stroke * 0.8f))
                    val pendant = Path().apply {
                        moveTo(cx, cy - r * 0.9f)
                        lineTo(cx + r * 1.1f, cy + r * 0.2f)
                        lineTo(cx, cy + r * 1.35f)
                        lineTo(cx - r * 1.1f, cy + r * 0.2f)
                        close()
                    }
                    drawPath(pendant, color = AppColors.Parchment.copy(alpha = 0.85f))
                    drawPath(pendant, color = ink, style = Stroke(width = stroke))
                }
                else -> {
                    val linkR = size.minDimension * 0.14f
                    drawCircle(color = ink, radius = linkR, center = Offset(cx - linkR * 1.1f, cy - linkR * 0.3f), style = Stroke(width = stroke))
                    drawCircle(color = AppColors.Parchment, radius = linkR, center = Offset(cx, cy), style = Stroke(width = stroke))
                    drawCircle(color = ink, radius = linkR, center = Offset(cx + linkR * 1.1f, cy + linkR * 0.3f), style = Stroke(width = stroke))
                }
            }
        }
    }
}
