package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.KaratPrice
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.delay

sealed interface GoldFeedStatus {
    data object Loading : GoldFeedStatus
    data class Live(
        val usdPerOunce: Double,
        val usdToEgpRate: Double,
        val fetchedAtLabel: String,
        val fxRatesPerUsd: Map<String, Double> = emptyMap(),
    ) : GoldFeedStatus
    data class OfflineFallback(
        val errorMessage: String,
    ) : GoldFeedStatus
}

/**
 * Composable that fetches and displays real-time global gold prices (24K, 21K, 18K)
 * at the top of the home screen, showing both EGP/g and global USD/g rates.
 */
@Composable
fun RealTimeGlobalGoldPriceBanner(
    prices: List<KaratPrice>,
    feedStatus: GoldFeedStatus,
    onRefresh: () -> Unit,
    onBannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedKarat by remember { mutableIntStateOf(21) }

    // Refresh live global gold prices every 60 seconds (initial fetch starts in ViewModel)
    LaunchedEffect(Unit) {
        if (feedStatus !is GoldFeedStatus.Live) {
            onRefresh()
        }
        while (true) {
            delay(60_000L)
            onRefresh()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "global_gold_pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "global_dot_alpha",
    )

    val karatItems = remember(prices) {
        prices.filter { it.karat in listOf(24, 21, 18) }
    }

    val usdPerOz = (feedStatus as? GoldFeedStatus.Live)?.usdPerOunce
    val usdToEgp = (feedStatus as? GoldFeedStatus.Live)?.usdToEgpRate ?: 49.5

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_global_gold_banner")
            .clickable(onClick = onBannerClick),
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
                    when (feedStatus) {
                        is GoldFeedStatus.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.BrassLight,
                            )
                        }
                        is GoldFeedStatus.Live -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(dotAlpha)
                                    .clip(CircleShape)
                                    .background(AppColors.LiveDot),
                            )
                        }
                        is GoldFeedStatus.OfflineFallback -> {
                            Icon(
                                imageVector = Icons.Outlined.WifiOff,
                                contentDescription = "غير متصل",
                                tint = AppColors.Alert,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Public,
                                contentDescription = "السعر العالمي",
                                tint = AppColors.BrassLight,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "سعر الذهب العالمي اللحظي (XAU)",
                                style = AppText.body.copy(
                                    color = AppColors.BrassLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Text(
                            text = when (feedStatus) {
                                is GoldFeedStatus.Loading -> "جاري جلب السعر العالمي المباشر..."
                                is GoldFeedStatus.Live ->
                                    "الأونصة: $${formatCurrency(feedStatus.usdPerOunce, 2)} · الدولار: ${formatCurrency(feedStatus.usdToEgpRate, 2)} ج.م · ${feedStatus.fetchedAtLabel}"
                                is GoldFeedStatus.OfflineFallback ->
                                    "أسعار مرجعية محلية · ${feedStatus.errorMessage}"
                            },
                            style = AppText.body.copy(
                                color = AppColors.Muted,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_global_gold_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "تحديث أسعار الذهب العالمية",
                        tint = AppColors.BrassLight,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                karatItems.forEach { item ->
                    val isHighlighted = item.karat == selectedKarat
                    val cardBg by animateColorAsState(
                        targetValue = if (isHighlighted) AppColors.BrassLight else AppColors.InkSoft,
                        animationSpec = tween(350),
                        label = "global_karat_bg",
                    )
                    val usdPerGram = if (usdPerOz != null) {
                        (usdPerOz / 31.1034768) * (item.karat / 24.0)
                    } else {
                        item.pricePerGram / usdToEgp
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(
                                width = if (isHighlighted) 0.dp else 1.dp,
                                color = if (isHighlighted) Color.Transparent else AppColors.Brass.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                selectedKarat = item.karat
                                onBannerClick()
                            }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                            .testTag("global_karat_tile_${item.karat}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${item.label} (${item.karat}K)",
                            style = AppText.body.copy(
                                color = if (isHighlighted) AppColors.Ink.copy(alpha = 0.78f) else AppColors.Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${formatCurrency(item.pricePerGram, 0)} ج.م",
                            style = AppText.display.copy(
                                fontSize = 16.sp,
                                color = if (isHighlighted) AppColors.Ink else Color.White,
                            ),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$${formatCurrency(usdPerGram, 2)} / جم",
                            style = AppText.body.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isHighlighted) AppColors.Trust else AppColors.LiveGainDark,
                            ),
                        )
                    }
                }
            }

            if (feedStatus is GoldFeedStatus.OfflineFallback) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onRefresh) {
                        Text(
                            text = "إعادة المحاولة",
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
