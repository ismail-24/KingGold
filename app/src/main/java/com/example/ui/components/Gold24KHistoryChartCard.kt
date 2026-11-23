package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.model.DailyGoldPrice
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

enum class DailyChartKaratMode(
    val karat: Int?,
    val chipLabel: String,
    val tagSuffix: String,
    val seriesColor: Color,
) {
    ALL(null, "الكل", "ALL", AppColors.Brass),
    KARAT_24(24, "عيار 24", "24", AppColors.Brass),
    KARAT_21(21, "عيار 21", "21", AppColors.Trust),
    KARAT_18(18, "عيار 18", "18", AppColors.Alert),
}

enum class DailyChartRangeMode(
    val days: Int,
    val chipLabel: String,
    val tagSuffix: String,
) {
    DAYS_30(30, "30 يوم", "30D"),
    DAYS_14(14, "14 يوم", "14D"),
    DAYS_7(7, "7 أيام", "7D"),
}

/**
 * Interactive multi-karat (24K, 21K, 18K) 30-day historical gold price line chart
 * powered by Vico Compose M3 and synchronized from Firestore `/daily_gold_prices`.
 */
@Composable
fun DailyGoldPriceTrendChartCard(
    dailyPrices: List<DailyGoldPrice>,
    isSyncedFromFirestore: Boolean,
    modifier: Modifier = Modifier,
    onOpenFullInsight: (() -> Unit)? = null,
    onRecordTodayPrices: (() -> Unit)? = null,
    isRecordingBusy: Boolean = false,
) {
    var selectedMode by rememberSaveable { mutableStateOf(DailyChartKaratMode.ALL) }
    var selectedRange by rememberSaveable { mutableStateOf(DailyChartRangeMode.DAYS_30) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_gold_trend_chart_card")
            .semantics {
                contentDescription = "رسم بياني لاتجاه أسعار الذهب اليومية لعيارات 24 و21 و18"
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
                            text = "السجل التاريخي لأسعار الذهب",
                            style = AppText.display.copy(fontSize = 15.sp),
                        )
                        Text(
                            text = "آخر ${selectedRange.days} يوماً · عيار 24 و21 و18 (ج.م/جم)",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                        )
                    }
                }

                if (onOpenFullInsight != null) {
                    Text(
                        text = "المؤشر الكامل ◀",
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable(onClick = onOpenFullInsight)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("daily_chart_open_insight"),
                        style = AppText.body.copy(
                            color = AppColors.Brass,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Firestore sync badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSyncedFromFirestore) AppColors.TrustSoft else AppColors.ParchmentDeep)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("daily_chart_source_badge"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isSyncedFromFirestore) Icons.Outlined.CloudDone else Icons.Outlined.CloudQueue,
                    contentDescription = null,
                    tint = if (isSyncedFromFirestore) AppColors.Trust else AppColors.Muted,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSyncedFromFirestore) {
                        "متزامن مع Firestore · ${dailyPrices.size} أيام"
                    } else {
                        "أسعار مرجعية · بانتظار مزامنة السحابة"
                    },
                    style = AppText.body.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSyncedFromFirestore) AppColors.Trust else AppColors.Muted,
                    ),
                )
            }

            if (dailyPrices.isEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.ParchmentDeep)
                        .padding(16.dp)
                        .testTag("daily_chart_empty_state"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "لا توجد قراءات يومية مسجلة حالياً",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                val visiblePrices = remember(dailyPrices, selectedRange) {
                    dailyPrices.takeLast(selectedRange.days)
                }

                // 30D / 14D / 7D range chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DailyChartRangeMode.entries.forEach { rangeMode ->
                        val selected = selectedRange == rangeMode
                        FilterChip(
                            selected = selected,
                            onClick = { selectedRange = rangeMode },
                            modifier = Modifier.testTag("daily_chart_range_${rangeMode.tagSuffix}"),
                            label = {
                                Text(
                                    text = rangeMode.chipLabel,
                                    style = AppText.body.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Brass,
                                selectedLabelColor = Color.White,
                                containerColor = AppColors.Parchment,
                                labelColor = AppColors.Ink,
                            ),
                        )
                    }
                }

                // Interactive karat selector chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DailyChartKaratMode.entries.forEach { mode ->
                        val selected = selectedMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { selectedMode = mode },
                            modifier = Modifier.testTag("daily_chart_karat_chip_${mode.tagSuffix}"),
                            label = {
                                Text(
                                    text = mode.chipLabel,
                                    style = AppText.body.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Ink,
                                selectedLabelColor = AppColors.BrassLight,
                                containerColor = AppColors.ParchmentDeep,
                                labelColor = AppColors.Ink,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val latestPoint = visiblePrices.last()
                val focusKarat = selectedMode.karat ?: 24
                val plottedPoints = remember(visiblePrices) {
                    if (visiblePrices.size == 1) listOf(visiblePrices.first(), visiblePrices.first()) else visiblePrices
                }

                // Color legend for 24K, 21K, 18K
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    KaratLegendPill(
                        label = "24K",
                        priceText = formatCurrency(latestPoint.price24kEgp, decimals = 0),
                        dotColor = AppColors.Brass,
                        highlighted = selectedMode == DailyChartKaratMode.ALL || selectedMode == DailyChartKaratMode.KARAT_24,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("daily_chart_legend_24k"),
                    )
                    KaratLegendPill(
                        label = "21K",
                        priceText = formatCurrency(latestPoint.price21kEgp, decimals = 0),
                        dotColor = AppColors.Trust,
                        highlighted = selectedMode == DailyChartKaratMode.ALL || selectedMode == DailyChartKaratMode.KARAT_21,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("daily_chart_legend_21k"),
                    )
                    KaratLegendPill(
                        label = "18K",
                        priceText = formatCurrency(latestPoint.price18kEgp, decimals = 0),
                        dotColor = AppColors.Alert,
                        highlighted = selectedMode == DailyChartKaratMode.ALL || selectedMode == DailyChartKaratMode.KARAT_18,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("daily_chart_legend_18k"),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val prices24 = remember(plottedPoints) { plottedPoints.map { it.price24kEgp } }
                val prices21 = remember(plottedPoints) { plottedPoints.map { it.price21kEgp } }
                val prices18 = remember(plottedPoints) { plottedPoints.map { it.price18kEgp } }
                val labels = remember(plottedPoints) { plottedPoints.map { it.dayLabel } }

                val focusPrices = remember(plottedPoints, focusKarat) {
                    plottedPoints.map { it.priceForKarat(focusKarat) }
                }
                val activePrices = remember(prices24, prices21, prices18, selectedMode) {
                    when (selectedMode) {
                        DailyChartKaratMode.ALL -> prices24 + prices21 + prices18
                        DailyChartKaratMode.KARAT_24 -> prices24
                        DailyChartKaratMode.KARAT_21 -> prices21
                        DailyChartKaratMode.KARAT_18 -> prices18
                    }
                }
                val minActivePrice = activePrices.minOrNull() ?: 0.0
                val maxActivePrice = activePrices.maxOrNull() ?: 100.0
                val spread = (maxActivePrice - minActivePrice).coerceAtLeast(40.0)
                val yPadding = (spread * 0.18).coerceAtLeast(30.0)
                val chartMinY = (floor((minActivePrice - yPadding) / 10.0) * 10.0).coerceAtLeast(0.0)
                val rawMaxY = ceil((maxActivePrice + yPadding) / 10.0) * 10.0
                val chartMaxY = maxOf(rawMaxY, chartMinY + 50.0)

                val focusMin = focusPrices.minOrNull() ?: 0.0
                val focusMax = focusPrices.maxOrNull() ?: 0.0
                val focusFirst = focusPrices.firstOrNull() ?: 0.0
                val focusLatest = focusPrices.lastOrNull() ?: 0.0
                val changePct = if (focusFirst > 0.0) ((focusLatest - focusFirst) / focusFirst) * 100.0 else 0.0
                val isPositive = changePct >= 0.0

                val modelProducer = remember { CartesianChartModelProducer() }
                LaunchedEffect(prices24, prices21, prices18, selectedMode) {
                    modelProducer.runTransaction {
                        lineSeries {
                            when (selectedMode) {
                                DailyChartKaratMode.ALL -> {
                                    series(prices24)
                                    series(prices21)
                                    series(prices18)
                                }
                                DailyChartKaratMode.KARAT_24 -> series(prices24)
                                DailyChartKaratMode.KARAT_21 -> series(prices21)
                                DailyChartKaratMode.KARAT_18 -> series(prices18)
                            }
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
                val marker = rememberDefaultCartesianMarker(
                    label = rememberTextComponent(
                        color = Color.White,
                        textSize = 11.sp,
                        background = rememberShapeComponent(
                            fill = fill(AppColors.Ink),
                            shape = CorneredShape.Pill,
                        ),
                    ),
                    guideline = rememberLineComponent(
                        fill = fill(AppColors.Brass.copy(alpha = 0.45f)),
                        thickness = 1.dp,
                    ),
                )

                val multiDotSize = if (plottedPoints.size > 15) 3.5.dp else 6.dp
                val singleDotSize = if (plottedPoints.size > 15) 4.5.dp else 7.dp

                val line24Multi = LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(fill(AppColors.Brass)),
                    pointConnector = LineCartesianLayer.PointConnector.cubic(),
                    pointProvider = LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.point(
                            component = rememberShapeComponent(fill = fill(AppColors.Brass), shape = CorneredShape.Pill),
                            size = multiDotSize,
                        ),
                    ),
                )
                val line21Multi = LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(fill(AppColors.Trust)),
                    pointConnector = LineCartesianLayer.PointConnector.cubic(),
                    pointProvider = LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.point(
                            component = rememberShapeComponent(fill = fill(AppColors.Trust), shape = CorneredShape.Pill),
                            size = multiDotSize,
                        ),
                    ),
                )
                val line18Multi = LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(fill(AppColors.Alert)),
                    pointConnector = LineCartesianLayer.PointConnector.cubic(),
                    pointProvider = LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.point(
                            component = rememberShapeComponent(fill = fill(AppColors.Alert), shape = CorneredShape.Pill),
                            size = multiDotSize,
                        ),
                    ),
                )
                val line24Area = LineCartesianLayer.rememberLine(
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
                            component = rememberShapeComponent(fill = fill(AppColors.Brass), shape = CorneredShape.Pill),
                            size = singleDotSize,
                        ),
                    ),
                )
                val line21Area = LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(fill(AppColors.Trust)),
                    areaFill = LineCartesianLayer.AreaFill.single(
                        fill(
                            ShaderProvider.verticalGradient(
                                AppColors.Trust.copy(alpha = 0.25f).toArgb(),
                                Color.Transparent.toArgb(),
                            ),
                        ),
                    ),
                    pointConnector = LineCartesianLayer.PointConnector.cubic(),
                    pointProvider = LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.point(
                            component = rememberShapeComponent(fill = fill(AppColors.Trust), shape = CorneredShape.Pill),
                            size = singleDotSize,
                        ),
                    ),
                )
                val line18Area = LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(fill(AppColors.Alert)),
                    areaFill = LineCartesianLayer.AreaFill.single(
                        fill(
                            ShaderProvider.verticalGradient(
                                AppColors.Alert.copy(alpha = 0.24f).toArgb(),
                                Color.Transparent.toArgb(),
                            ),
                        ),
                    ),
                    pointConnector = LineCartesianLayer.PointConnector.cubic(),
                    pointProvider = LineCartesianLayer.PointProvider.single(
                        LineCartesianLayer.point(
                            component = rememberShapeComponent(fill = fill(AppColors.Alert), shape = CorneredShape.Pill),
                            size = singleDotSize,
                        ),
                    ),
                )

                val lineProvider = when (selectedMode) {
                    DailyChartKaratMode.ALL -> LineCartesianLayer.LineProvider.series(line24Multi, line21Multi, line18Multi)
                    DailyChartKaratMode.KARAT_24 -> LineCartesianLayer.LineProvider.series(line24Area)
                    DailyChartKaratMode.KARAT_21 -> LineCartesianLayer.LineProvider.series(line21Area)
                    DailyChartKaratMode.KARAT_18 -> LineCartesianLayer.LineProvider.series(line18Area)
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = lineProvider,
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
                            itemPlacer = remember(plottedPoints.size) {
                                HorizontalAxis.ItemPlacer.aligned(
                                    spacing = {
                                        when {
                                            plottedPoints.size > 20 -> 5
                                            plottedPoints.size > 10 -> 2
                                            else -> 1
                                        }
                                    },
                                    addExtremeLabelPadding = true,
                                )
                            },
                        ),
                        marker = marker,
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                        .testTag("daily_gold_vico_chart"),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TrendMiniStat(
                        label = "الأدنى (عيار $focusKarat)",
                        value = formatCurrency(focusMin, decimals = 0),
                        valueColor = AppColors.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    TrendMiniStat(
                        label = "الأعلى (عيار $focusKarat)",
                        value = formatCurrency(focusMax, decimals = 0),
                        valueColor = AppColors.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    TrendMiniStat(
                        label = "التغير",
                        value = String.format(
                            Locale.US,
                            "%s%.2f%%",
                            if (isPositive) "+" else "",
                            changePct,
                        ),
                        valueColor = if (isPositive) AppColors.Trust else AppColors.Alert,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (onRecordTodayPrices != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRecordTodayPrices,
                    enabled = !isRecordingBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_record_daily_price_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = null,
                        tint = AppColors.Brass,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecordingBusy) "جاري حفظ قراءة اليوم..." else "تسجيل أسعار اليوم في Firestore",
                        style = AppText.body.copy(
                            color = AppColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun KaratLegendPill(
    label: String,
    priceText: String,
    dotColor: Color,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) AppColors.ParchmentDeep else AppColors.Parchment.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (highlighted) dotColor else dotColor.copy(alpha = 0.35f)),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$label · $priceText",
            style = AppText.body.copy(
                fontSize = 10.5.sp,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (highlighted) AppColors.Ink else AppColors.Muted,
            ),
            maxLines = 1,
        )
    }
}

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
