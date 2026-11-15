package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val KingGoldLightColorScheme = lightColorScheme(
    primary = AppColors.Brass,
    onPrimary = Color.White,
    primaryContainer = AppColors.BrassLight,
    onPrimaryContainer = AppColors.Ink,
    secondary = AppColors.Trust,
    onSecondary = Color.White,
    secondaryContainer = AppColors.TrustSoft,
    onSecondaryContainer = AppColors.Trust,
    tertiary = AppColors.Seal,
    onTertiary = AppColors.Parchment,
    background = AppColors.Parchment,
    onBackground = AppColors.Ink,
    surface = AppColors.Parchment,
    onSurface = AppColors.Ink,
    surfaceVariant = AppColors.ParchmentDeep,
    onSurfaceVariant = AppColors.Muted,
    outline = AppColors.Line,
    outlineVariant = AppColors.Line,
    error = AppColors.Alert,
    onError = Color.White,
    errorContainer = AppColors.AlertSoft,
    onErrorContainer = AppColors.Alert,
)

private val KingGoldShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun KingGoldTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = KingGoldLightColorScheme,
            typography = KingGoldTypography,
            shapes = KingGoldShapes,
            content = content,
        )
    }
}
