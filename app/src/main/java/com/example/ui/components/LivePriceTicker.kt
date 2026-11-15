package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KingGoldRepository
import com.example.model.KaratPrice
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Live gold price ticker with subtle scale-settling micro-fluctuations every 4 seconds
 * and pulsing live status indicator.
 */
@Composable
fun LivePriceTicker(
    modifier: Modifier = Modifier,
    prices: List<KaratPrice> = KingGoldRepository.prices,
    initialHighlightKarat: Int = 21,
    onTickerClick: (() -> Unit)? = null,
) {
    var selectedKarat by remember(initialHighlightKarat) { mutableIntStateOf(initialHighlightKarat) }
    var wobbles by remember(prices) { mutableStateOf(List(prices.size) { 0f }) }
    var formattedTime by remember { mutableStateOf(currentFormattedTime()) }

    LaunchedEffect(prices) {
        while (true) {
            delay(4000L)
            wobbles = prices.map { (Random.nextFloat() - 0.5f) * 3f }
            formattedTime = currentFormattedTime()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "live_dot")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_price_ticker")
            .then(
                if (onTickerClick != null) {
                    Modifier.clickable(onClick = onTickerClick)
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(18.dp),
        color = AppColors.Ink,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(AppColors.LiveDot),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "السعر اللحظي · ج.م/جرام",
                        style = AppText.body.copy(
                            color = AppColors.BrassLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                Text(
                    text = formattedTime,
                    style = AppText.body.copy(
                        color = AppColors.Muted,
                        fontSize = 12.sp,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                prices.forEachIndexed { index, price ->
                    val isHighlighted = price.karat == selectedKarat
                    val cardBg by animateColorAsState(
                        targetValue = if (isHighlighted) AppColors.BrassLight else AppColors.InkSoft,
                        animationSpec = tween(400),
                        label = "karat_bg",
                    )
                    val animatedValue by animateFloatAsState(
                        targetValue = (price.pricePerGram + wobbles.getOrElse(index) { 0f }).toFloat(),
                        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                        label = "karat_val",
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .clickable {
                                selectedKarat = price.karat
                                onTickerClick?.invoke()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = price.label,
                            style = AppText.body.copy(
                                color = if (isHighlighted) AppColors.Ink.copy(alpha = 0.72f) else AppColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(animatedValue.toDouble(), decimals = 0),
                            style = AppText.display.copy(
                                fontSize = 16.sp,
                                color = if (isHighlighted) AppColors.Ink else Color.White,
                            ),
                        )
                        Text(
                            text = "${price.changePercent}% ▲",
                            style = AppText.body.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHighlighted) AppColors.Trust else AppColors.LiveGainDark,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun currentFormattedTime(): String {
    val sdf = SimpleDateFormat("h:mm a", Locale("ar"))
    return sdf.format(Date())
}
