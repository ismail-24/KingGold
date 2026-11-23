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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-time gold price tracking ticker widget displaying live 24K, 21K, 18K,
 * and Sovereign Gold Pound rates with EGP/USD currency toggle and live feed status.
 */
@Composable
fun LivePriceTicker(
    modifier: Modifier = Modifier,
    prices: List<KaratPrice> = KingGoldRepository.prices,
    initialHighlightKarat: Int = 21,
    feedStatus: GoldFeedStatus = GoldFeedStatus.Loading,
    onRefresh: (() -> Unit)? = null,
    onTickerClick: (() -> Unit)? = null,
) {
    var selectedKarat by remember(initialHighlightKarat) { mutableIntStateOf(initialHighlightKarat) }
    var showUsd by remember { mutableStateOf(false) }

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

    val usdToEgpRate = (feedStatus as? GoldFeedStatus.Live)?.usdToEgpRate ?: 49.5
    val statusLabel = when (feedStatus) {
        is GoldFeedStatus.Live -> "مباشر · ${feedStatus.fetchedAtLabel}"
        is GoldFeedStatus.Loading -> currentFormattedTime()
        is GoldFeedStatus.OfflineFallback -> "مرجعي · ${currentFormattedTime()}"
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    if (feedStatus is GoldFeedStatus.Loading && onRefresh != null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = AppColors.BrassLight,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(if (feedStatus is GoldFeedStatus.OfflineFallback) 1f else dotAlpha)
                                .clip(CircleShape)
                                .background(
                                    if (feedStatus is GoldFeedStatus.OfflineFallback) {
                                        AppColors.Brass
                                    } else {
                                        AppColors.LiveDot
                                    },
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (showUsd) "السعر اللحظي · $/جرام" else "السعر اللحظي · ج.م/جرام",
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Text(
                            text = statusLabel,
                            style = AppText.body.copy(
                                color = AppColors.Muted,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.InkSoft)
                            .border(1.dp, AppColors.Brass.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable { showUsd = !showUsd }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("ticker_currency_toggle"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (showUsd) "USD $" else "EGP ج.م",
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }

                    if (onRefresh != null) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("ticker_refresh_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "تحديث أسعار الذهب اللحظية",
                                tint = AppColors.BrassLight,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            if (feedStatus is GoldFeedStatus.Live) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الأونصة العالمية: $${formatCurrency(feedStatus.usdPerOunce, 2)} · سعر الصرف: ${formatCurrency(feedStatus.usdToEgpRate, 2)} ج.م",
                    style = AppText.body.copy(
                        color = AppColors.Muted,
                        fontSize = 11.sp,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                prices.forEach { price ->
                    val isHighlighted = price.karat == selectedKarat
                    val cardBg by animateColorAsState(
                        targetValue = if (isHighlighted) AppColors.BrassLight else AppColors.InkSoft,
                        animationSpec = tween(400),
                        label = "karat_bg",
                    )
                    val targetPrice = if (showUsd) {
                        price.pricePerGram / usdToEgpRate.coerceAtLeast(1.0)
                    } else {
                        price.pricePerGram
                    }
                    val animatedValue by animateFloatAsState(
                        targetValue = targetPrice.toFloat(),
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
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
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("ticker_karat_tile_${price.karat}"),
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
                            text = if (showUsd) {
                                "$${formatCurrency(animatedValue.toDouble(), decimals = if (price.karat == 0) 0 else 1)}"
                            } else {
                                formatCurrency(animatedValue.toDouble(), decimals = 0)
                            },
                            style = AppText.display.copy(
                                fontSize = 15.5.sp,
                                color = if (isHighlighted) AppColors.Ink else Color.White,
                            ),
                        )
                        val trendArrow = if (price.changePercent < 0) "▼" else "▲"
                        Text(
                            text = "${formatCurrency(kotlin.math.abs(price.changePercent), decimals = 2)}% $trendArrow",
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
    val sdf = SimpleDateFormat("h:mm a", Locale.forLanguageTag("ar"))
    return sdf.format(Date())
}
