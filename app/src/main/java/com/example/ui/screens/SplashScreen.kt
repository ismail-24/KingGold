package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SealBadge
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    val dropOffset = remember { Animatable(-80f) }
    val badgeAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            dropOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            badgeAlpha.animateTo(1f, animationSpec = tween(550))
        }
        delay(450L)
        textAlpha.animateTo(1f, animationSpec = tween(550))
        delay(700L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Ink)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SealBadge(
                size = 90.dp,
                label = "KG",
                modifier = Modifier
                    .offset { IntOffset(0, dropOffset.value.roundToInt()) }
                    .alpha(badgeAlpha.value),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value),
            ) {
                Text(
                    text = "King Gold",
                    style = AppText.display.copy(
                        color = AppColors.Parchment,
                        fontSize = 28.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ذهب حقيقي، بدمغة موثّقة",
                    style = AppText.body.copy(
                        color = AppColors.Muted,
                        fontSize = 14.sp,
                    ),
                )
            }
        }
    }
}
