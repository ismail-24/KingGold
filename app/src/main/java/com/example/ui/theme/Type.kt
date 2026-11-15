package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Medium),
    Font(R.font.amiri, FontWeight.SemiBold),
    Font(R.font.amiri, FontWeight.Bold),
)

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold),
)

object AppText {
    val display = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        color = AppColors.Ink,
        lineHeight = 28.sp,
    )

    val body = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Normal,
        color = AppColors.Ink,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    )
}

val KingGoldTypography = Typography(
    displayLarge = AppText.display.copy(fontSize = 30.sp, lineHeight = 38.sp),
    displayMedium = AppText.display.copy(fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge = AppText.display.copy(fontSize = 22.sp, lineHeight = 30.sp),
    headlineMedium = AppText.display.copy(fontSize = 20.sp, lineHeight = 28.sp),
    headlineSmall = AppText.display.copy(fontSize = 17.sp, lineHeight = 24.sp),
    titleLarge = AppText.display.copy(fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = AppText.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleSmall = AppText.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp),
    bodyLarge = AppText.body.copy(fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = AppText.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = AppText.body.copy(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = AppText.body.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp),
    labelMedium = AppText.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp),
    labelSmall = AppText.body.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 14.sp),
)
