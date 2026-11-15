package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoldPriceHistoryPoint
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.util.Locale
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Interactive 7-day 24K gold price line chart powered by Vico Compose M3.
 */
@Composable
fun Gold24KHistoryChartCard(
    historyPoints: List<GoldPriceHistoryPoint>,
    onOpenFullInsight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (historyPoints.size < 2) return

    val prices = remember(historyPoints) { historyPoints.map { it.pricePerGram } }
    val labels = remember(historyPoints) { historyPoints.map { it.dayLabel } }
    val minPrice = prices.min()
    val maxPrice = prices.max()
    val firstPrice = prices.first()
    val latestPrice = prices.last()
    val changePct = if (firstPrice > 0.0) ((latestPrice - firstPrice) / firstPrice) * 100.0 else 0.0
    val isPositive = changePct >= 0.0

    val yPadding = ((maxPrice - minPrice) * 0.25).coerceAtLeast(25.0)
    val chartMinY = floor((minPrice - yPadding) / 10.0) * 10.0
    val chartMaxY = ceil((maxPrice + yPadding) / 10.0) * 10.0

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(prices) {
        modelProducer.runTransaction {
            lineSeries {
                series(prices)
            }
        }
    }

    val rangeProvider = remember(chartMinY, chartMaxY) {
        CartesianLayerRangeProvider.fixed(minY = chartMinY, maxY = chartMaxY)
    }

    val yAxisValueFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            String.format(Locale.US, "%,.0f", value)
        }
    }

    val xAxisValueFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            val idx = value.roundToInt().coerceIn(0, labels.lastIndex)
            labels[idx]
        }
    }

    val markerLabelComponent = rememberTextComponent(
        color = Color.White,
        textSize = 11.sp,
        background = rememberShapeComponent(
            fill = fill(AppColors.Ink),
            shape = CorneredShape.Pill,
        ),
    )

    val marker = rememberDefaultCartesianMarker(
        label = markerLabelComponent,
        guideline = rememberLineComponent(
            fill = fill(AppColors.Brass.copy(alpha = 0.45f)),
            thickness = 1.dp,
        ),
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gold_24k_history_chart_card")
            .semantics {
                contentDescription = "رسم بياني لاتجاه سعر الذهب عيار 24 خلال آخر 7 أيام"
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.ParchmentDeep),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "اتجاه عيار 24 · آخر 7 أيام",
                            style = AppText.display.copy(fontSize = 15.sp),
                        )
                        Text(
                            text = "ج.م / جرام · سبيكة صافية 999.9",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isPositive) AppColors.TrustSoft else AppColors.Alert.copy(alpha = 0.10f),
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "%s%.2f%% %s",
                                if (isPositive) "+" else "",
                                changePct,
                                if (isPositive) "▲" else "▼",
                            ),
                            style = AppText.body.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPositive) AppColors.Trust else AppColors.Alert,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "المؤشر الكامل ◀",
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(onClick = onOpenFullInsight)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        style = AppText.body.copy(
                            color = AppColors.Brass,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(AppColors.Brass)),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill(
                                        ShaderProvider.verticalGradient(
                                            AppColors.Brass.copy(alpha = 0.28f).toArgb(),
                                            Color.Transparent.toArgb(),
                                        ),
                                    ),
                                ),
                                pointConnector = LineCartesianLayer.PointConnector.cubic(),
                                pointProvider = LineCartesianLayer.PointProvider.single(
                                    LineCartesianLayer.point(
                                        component = rememberShapeComponent(
                                            fill = fill(AppColors.Brass),
                                            shape = CorneredShape.Pill,
                                        ),
                                        size = 7.dp,
                                    ),
                                ),
                            ),
                        ),
                        rangeProvider = rangeProvider,
                    ),
                    startAxis = VerticalAxis.rememberStart(
                        line = null,
                        tick = null,
                        guideline = rememberLineComponent(
                            fill = fill(AppColors.Line.copy(alpha = 0.65f)),
                            thickness = 0.8.dp,
                        ),
                        label = rememberTextComponent(
                            color = AppColors.Muted,
                            textSize = 10.sp,
                        ),
                        valueFormatter = yAxisValueFormatter,
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        line = rememberLineComponent(
                            fill = fill(AppColors.Line),
                            thickness = 1.dp,
                        ),
                        tick = null,
                        guideline = null,
                        label = rememberTextComponent(
                            color = AppColors.Muted,
                            textSize = 9.5.sp,
                        ),
                        valueFormatter = xAxisValueFormatter,
                    ),
                    marker = marker,
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .testTag("gold_24k_vico_line_chart"),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendMiniStat(
                    label = "الأدنى (7 أيام)",
                    value = formatCurrency(minPrice, decimals = 0),
                    valueColor = AppColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                TrendMiniStat(
                    label = "الأعلى (7 أيام)",
                    value = formatCurrency(maxPrice, decimals = 0),
                    valueColor = AppColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                TrendMiniStat(
                    label = "عيار 24 اليوم",
                    value = formatCurrency(latestPrice, decimals = 0),
                    valueColor = AppColors.Brass,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TrendMiniStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.ParchmentDeep)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = AppText.body.copy(fontSize = 10.sp, color = AppColors.Muted),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = AppText.display.copy(fontSize = 13.5.sp, color = valueColor),
        )
    }
}
