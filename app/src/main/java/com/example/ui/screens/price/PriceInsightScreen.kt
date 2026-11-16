package com.example.ui.screens.price

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KingGoldRepository
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.components.LivePriceTicker
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceInsightScreen(
    uiState: KingGoldUiState,
    onSaveAlert: (enabled: Boolean, above: Boolean, karat: Int, target: String) -> Unit,
    onOpenAiAdvisor: (String?) -> Unit = {},
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var alertEnabled by remember(uiState.priceAlertEnabled) { mutableStateOf(uiState.priceAlertEnabled) }
    var alertAbove by remember(uiState.priceAlertAbove) { mutableStateOf(uiState.priceAlertAbove) }
    var alertKarat by remember(uiState.priceAlertKarat) { mutableIntStateOf(uiState.priceAlertKarat) }
    var targetText by remember(uiState.priceAlertTarget) { mutableStateOf(uiState.priceAlertTarget) }
    var hasNotificationPermission by remember {
        mutableStateOf(GoldPriceNotificationHelper.hasNotificationPermission(context))
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) {
                    "تم تفعيل إذن الإشعارات بنجاح"
                } else {
                    "تم رفض إذن الإشعارات — يمكنك تفعيله من إعدادات التطبيق"
                },
            )
        }
    }

    fun requestNotificationPermissionIfNeeded(): Boolean {
        if (hasNotificationPermission) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        return false
    }

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
                    val history21K = uiState.history21K7Days
                    val chartData = history21K.map { it.pricePerGram }
                    val min21K = chartData.minOrNull() ?: uiState.gramPrice(21)
                    val max21K = chartData.maxOrNull() ?: uiState.gramPrice(21)
                    val first21K = chartData.firstOrNull() ?: min21K
                    val last21K = chartData.lastOrNull() ?: max21K
                    val changePct = if (first21K > 0.0) ((last21K - first21K) / first21K) * 100.0 else 0.0
                    val changeLabel = String.format(java.util.Locale.US, "%+.2f%%", changePct)

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
                                data = chartData,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            history21K.forEach { point ->
                                Text(
                                    text = point.dayLabel,
                                    style = AppText.body.copy(fontSize = 10.sp, color = AppColors.Muted),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatBox(
                                label = "التغير",
                                value = changeLabel,
                                valueColor = if (changePct >= 0.0) AppColors.Trust else AppColors.Alert,
                                modifier = Modifier.weight(1f),
                            )
                            StatBox("الأدنى", formatCurrency(min21K, 0), AppColors.Ink, Modifier.weight(1f))
                            StatBox("الأعلى", formatCurrency(max21K, 0), AppColors.Ink, Modifier.weight(1f))
                        }
                    }
                }

                item {
                    val shape = RoundedCornerShape(16.dp)
                    val currentKaratPrice = uiState.gramPrice(alertKarat)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, shape)
                            .padding(16.dp)
                            .testTag("fcm_price_alert_card"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "إشعارات هبوط السعر (FCM)",
                                    style = AppText.display.copy(fontSize = 15.5.sp),
                                )
                                Text(
                                    text = "السعر الحالي لعيار $alertKarat: ${formatCurrency(currentKaratPrice, decimals = 0)} ج.م/جم",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                                )
                            }
                            Switch(
                                checked = alertEnabled,
                                onCheckedChange = { enabled ->
                                    alertEnabled = enabled
                                    if (enabled) {
                                        requestNotificationPermissionIfNeeded()
                                    }
                                    onSaveAlert(enabled, alertAbove, alertKarat, targetText)
                                },
                                modifier = Modifier.testTag("fcm_alert_enable_switch"),
                                colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Brass),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (hasNotificationPermission) AppColors.TrustSoft else AppColors.Alert.copy(alpha = 0.08f),
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (hasNotificationPermission) {
                                    Icons.Outlined.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (hasNotificationPermission) AppColors.Trust else AppColors.Alert,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !hasNotificationPermission -> "يتطلب السماح بإذن الإشعارات لاستلام تنبيهات FCM"
                                    uiState.fcmToken != null -> "FCM متصل · الإشعارات مفعّلة لهذا الجهاز"
                                    else -> "الإشعارات مفعّلة · قناة تنبيهات عيار 24 جاهزة"
                                },
                                modifier = Modifier.weight(1f),
                                style = AppText.body.copy(
                                    color = if (hasNotificationPermission) AppColors.Trust else AppColors.Alert,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            if (!hasNotificationPermission) {
                                OutlinedButton(
                                    onClick = { requestNotificationPermissionIfNeeded() },
                                    modifier = Modifier.testTag("grant_notification_permission_button"),
                                ) {
                                    Text(
                                        text = "السماح",
                                        style = AppText.body.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.Alert,
                                        ),
                                    )
                                }
                            }
                        }

                        if (alertEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(24, 21, 18).forEach { karatOption ->
                                    FilterChip(
                                        selected = alertKarat == karatOption,
                                        onClick = { alertKarat = karatOption },
                                        modifier = Modifier.testTag("alert_karat_chip_$karatOption"),
                                        label = {
                                            Text(
                                                text = "عيار $karatOption",
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = { alertAbove = !alertAbove },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("alert_condition_toggle_button"),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(
                                        text = if (alertAbove) "أعلى من" else "أقل من (هبوط)",
                                        style = AppText.body.copy(
                                            color = AppColors.Ink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                        ),
                                    )
                                }
                                OutlinedTextField(
                                    value = targetText,
                                    onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(6) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("alert_threshold_input"),
                                    singleLine = true,
                                    suffix = {
                                        Text(
                                            text = "ج.م",
                                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                                        )
                                    },
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
                                    requestNotificationPermissionIfNeeded()
                                    onSaveAlert(alertEnabled, alertAbove, alertKarat, targetText)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "تم حفظ تنبيه عيار $alertKarat (${if (alertAbove) "أعلى من" else "أقل من"} $targetText ج.م) في السحابة",
                                        )
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
                                    text = "حفظ تنبيه السعر",
                                    style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val threshold = targetText.toDoubleOrNull() ?: currentKaratPrice
                                    if (!hasNotificationPermission) {
                                        requestNotificationPermissionIfNeeded()
                                    } else {
                                        val posted = GoldPriceNotificationHelper.show24KPriceDropAlert(
                                            context = context,
                                            currentPriceEgp = uiState.gramPrice(24),
                                            thresholdEgp = threshold,
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (posted) {
                                                    "تم إرسال إشعار تجريبي لهبوط عيار 24"
                                                } else {
                                                    "تعذر عرض الإشعار — تأكد من تفعيل إذن الإشعارات"
                                                },
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("test_fcm_alert_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = AppColors.Brass,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اختبار إشعار هبوط عيار 24 الآن",
                                    style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold),
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
