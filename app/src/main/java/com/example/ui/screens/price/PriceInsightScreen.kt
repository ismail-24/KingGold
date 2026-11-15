package com.example.ui.screens.price

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KingGoldRepository
import com.example.ui.components.LivePriceTicker
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceInsightScreen(
    uiState: KingGoldUiState,
    onSaveAlert: (enabled: Boolean, above: Boolean, target: String) -> Unit,
    onOpenAiAdvisor: (String?) -> Unit = {},
    onBack: () -> Unit,
) {
    var alertEnabled by remember(uiState.priceAlertEnabled) { mutableStateOf(uiState.priceAlertEnabled) }
    var alertAbove by remember(uiState.priceAlertAbove) { mutableStateOf(uiState.priceAlertAbove) }
    var targetText by remember(uiState.priceAlertTarget) { mutableStateOf(uiState.priceAlertTarget) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppColors.Parchment,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "أسعار الذهب",
                        style = AppText.display.copy(fontSize = 20.sp),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = AppColors.Ink,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Parchment),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    LivePriceTicker(prices = uiState.prices)
                }

                item {
                    Column {
                        Text(
                            text = "عيار 21 · آخر 7 أيام (ج.م/جرام)",
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val shape = RoundedCornerShape(16.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(185.dp)
                                .clip(shape)
                                .background(Color.White)
                                .border(1.dp, AppColors.Line, shape)
                                .padding(14.dp),
                        ) {
                            GoldPriceTrendChart(
                                data = KingGoldRepository.priceHistory7Days,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            KingGoldRepository.priceHistoryLabels.forEach { day ->
                                Text(
                                    text = day,
                                    style = AppText.body.copy(fontSize = 10.5.sp, color = AppColors.Muted),
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatBox("التغير", "+1.69%", AppColors.Trust, Modifier.weight(1f))
                        StatBox("الأدنى", "5,300", AppColors.Ink, Modifier.weight(1f))
                        StatBox("الأعلى", "5,425", AppColors.Ink, Modifier.weight(1f))
                    }
                }

                item {
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, shape)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "نبّهني عند وصول السعر",
                                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                            )
                            Switch(
                                checked = alertEnabled,
                                onCheckedChange = {
                                    alertEnabled = it
                                    onSaveAlert(it, alertAbove, targetText)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Brass),
                            )
                        }

                        if (alertEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = { alertAbove = !alertAbove },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(
                                        text = if (alertAbove) "أعلى من" else "أقل من",
                                        style = AppText.body.copy(
                                            color = AppColors.Ink,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                                OutlinedTextField(
                                    value = targetText,
                                    onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(6) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = AppText.body.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.Brass,
                                        unfocusedBorderColor = AppColors.Line,
                                    ),
                                )
                            }

                            Button(
                                onClick = {
                                    onSaveAlert(alertEnabled, alertAbove, targetText)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تم حفظ منبّه السعر بنجاح")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_price_alert_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Brass,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text(
                                    text = "حفظ التنبيه",
                                    style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            onOpenAiAdvisor("حلل أسعار الذهب اليوم (عيار 21 و24) وهل الوقت مناسب للشراء؟")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("price_open_ai_advisor_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اسأل مستشار Gemini عن اتجاه السعر الحالي",
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                item {
                    Text(
                        text = "الأسعار مرجعية وبتتحدث لحظياً. سعر طلبك بيتثبت لمدة محدودة بعد ما تضيف القطعة للسلة.",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.ParchmentDeep)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = AppText.body.copy(fontSize = 11.sp, color = AppColors.Muted),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = AppText.display.copy(fontSize = 16.sp, color = valueColor),
        )
    }
}

@Composable
private fun GoldPriceTrendChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val minV = data.min()
        val maxV = data.max()
        val range = (maxV - minV).let { if (it < 1.0) 1.0 else it }
        val dx = size.width / (data.size - 1)

        fun pointAt(i: Int): Offset {
            val norm = ((data[i] - minV) / range).toFloat()
            val x = size.width - i * dx // RTL flow: newest on left/end or chronological right-to-left
            val y = size.height - norm * size.height * 0.82f - size.height * 0.09f
            return Offset(x, y)
        }

        // Subtle horizontal guide lines
        for (step in 0..2) {
            val y = size.height * (0.15f + step * 0.35f)
            drawLine(
                color = AppColors.Line.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val linePath = Path().apply {
            val first = pointAt(0)
            moveTo(first.x, first.y)
            for (i in 1 until data.size) {
                val pt = pointAt(i)
                lineTo(pt.x, pt.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            val last = pointAt(data.lastIndex)
            val first = pointAt(0)
            lineTo(last.x, size.height)
            lineTo(first.x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(AppColors.Brass.copy(alpha = 0.24f), Color.Transparent),
            ),
        )

        drawPath(
            path = linePath,
            color = AppColors.Brass,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
        )

        val latest = pointAt(data.lastIndex)
        drawCircle(
            color = AppColors.Brass.copy(alpha = 0.25f),
            radius = 9.dp.toPx(),
            center = latest,
        )
        drawCircle(
            color = AppColors.Brass,
            radius = 5.dp.toPx(),
            center = latest,
        )
    }
}
