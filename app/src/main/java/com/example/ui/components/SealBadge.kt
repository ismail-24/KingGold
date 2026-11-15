package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Goldsmith hallmark stamp seal drawn via Canvas with 16 perimeter notches
 * and inner brass ring, replacing generic green checkmark icons.
 */
@Composable
fun SealBadge(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    label: String = "KG",
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            drawCircle(
                color = AppColors.Seal,
                radius = radius,
                center = center,
            )

            val notches = 16
            val notchColor = AppColors.Parchment.copy(alpha = 0.9f)
            for (i in 0 until notches) {
                val angle = (2.0 * PI / notches) * i
                val pt = Offset(
                    x = center.x + (radius * 0.91f * cos(angle)).toFloat(),
                    y = center.y + (radius * 0.91f * sin(angle)).toFloat(),
                )
                drawCircle(
                    color = notchColor,
                    radius = this.size.minDimension * 0.024f,
                    center = pt,
                )
            }

            drawCircle(
                color = AppColors.BrassLight,
                radius = radius * 0.78f,
                center = center,
                style = Stroke(width = (this.size.minDimension * 0.038f).coerceAtLeast(1.2f)),
            )
        }
        Text(
            text = label,
            style = AppText.display.copy(
                color = AppColors.Parchment,
                fontSize = (size.value * 0.30f).sp,
                lineHeight = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
fun VerifiedStrip(
    modifier: Modifier = Modifier,
    label: String = "موثّق",
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SealBadge(size = 18.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = AppText.body.copy(
                color = AppColors.Trust,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
