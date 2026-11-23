package com.example.ui.screens.price

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Widgets
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notifications.FcmRegistrationManager
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.components.DailyGoldPriceTrendChartCard
import com.example.ui.components.LivePriceTicker
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.widget.GoldPriceAppWidgetProvider
import com.example.widget.PinWidgetResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceInsightScreen(
    uiState: KingGoldUiState,
    onSaveAlert: (enabled: Boolean, above: Boolean, karat: Int, target: String) -> Unit,
    onOpenAiAdvisor: (String?) -> Unit = {},
    onRecordTodayPrices: (() -> Unit)? = null,
    onRefreshPrices: (() -> Unit)? = null,
    onOpenCalculator: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var alertEnabled by remember(uiState.priceAlertEnabled) { mutableStateOf(uiState.priceAlertEnabled) }
    var alertAbove by remember(uiState.priceAlertAbove) { mutableStateOf(uiState.priceAlertAbove) }
    var alertKarat by remember(uiState.priceAlertKarat) { mutableIntStateOf(uiState.priceAlertKarat) }
    var targetText by remember(uiState.priceAlertTarget) { mutableStateOf(uiState.priceAlertTarget) }
    var targetError by remember { mutableStateOf<String?>(null) }
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
                    LivePriceTicker(
                        prices = uiState.prices,
                        feedStatus = uiState.goldFeedStatus,
                        onRefresh = onRefreshPrices,
                    )
                }

                item {
                    val cardShape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, cardShape)
                            .padding(16.dp)
                            .testTag("home_widget_promo_card"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.Ink),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Widgets,
                                    contentDescription = "ويدجت الشاشة الرئيسية",
                                    tint = AppColors.BrassLight,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ويدجت أسعار الذهب للشاشة الرئيسية",
                                    style = AppText.display.copy(fontSize = 15.sp),
                                )
                                Text(
                                    text = "تابع عيار 24 و21 و18 والجنيه مباشرة من شاشة هاتفك مع تحديث لحظي.",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    GoldPriceAppWidgetProvider.syncFromPrices(
                                        context = context,
                                        prices = uiState.prices,
                                        feedStatus = uiState.goldFeedStatus,
                                    )
                                    val result = GoldPriceAppWidgetProvider.requestPinWidget(context)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            when (result) {
                                                PinWidgetResult.RequestLaunched ->
                                                    "تم إرسال طلب إضافة ويدجت أسعار الذهب للشاشة الرئيسية"
                                                PinWidgetResult.UnsupportedLauncher ->
                                                    "اضغط مطولاً على الشاشة الرئيسية واختر Widgets لإضافة King Gold"
                                            },
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pin_gold_widget_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Ink,
                                    contentColor = AppColors.BrassLight,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Widgets,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تثبيت الويدجت",
                                    style = AppText.body.copy(
                                        color = AppColors.BrassLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    ),
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onRefreshPrices?.invoke()
                                    GoldPriceAppWidgetProvider.syncFromPrices(
                                        context = context,
                                        prices = uiState.prices,
                                        feedStatus = uiState.goldFeedStatus,
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تمت مزامنة الويدجت بأحدث أسعار العيارات")
                                    }
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("sync_gold_widget_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "مزامنة الويدجت الآن",
                                    tint = AppColors.Brass,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "مزامنة",
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

                item {
                    DailyGoldPriceTrendChartCard(
                        dailyPrices = uiState.dailyGoldPriceTrend,
                        isSyncedFromFirestore = uiState.isDailyPricesFromFirestore,
                        onRecordTodayPrices = onRecordTodayPrices,
                        isRecordingBusy = uiState.isActionBusy,
                    )
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
                                    text = "إشعارات تغير السعر الفورية (FCM)",
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
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("fcm_status_banner"),
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
                            val topicName = FcmRegistrationManager.topicForKarat(alertKarat)
                            Text(
                                text = when {
                                    !hasNotificationPermission -> "يتطلب السماح بإذن الإشعارات لاستلام تنبيهات FCM"
                                    !alertEnabled -> "التنبيهات متوقفة مؤقتاً — فعّل المفتاح للمتابعة"
                                    uiState.fcmToken != null -> "FCM متصل ($topicName) · التنبيه الفوري مفعّل"
                                    else -> "قناة تنبيهات عيار $alertKarat جاهزة ($topicName)"
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

                        if (alertEnabled && uiState.isPriceAlertTriggered) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppColors.Alert.copy(alpha = 0.10f))
                                    .border(1.dp, AppColors.Alert.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("fcm_threshold_crossed_banner"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = AppColors.Alert,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تم تجاوز الحد المحدد الآن لعيار ${uiState.priceAlertKarat} (${if (uiState.priceAlertAbove) "أعلى من" else "أقل من"} ${uiState.priceAlertTarget} ج.م)",
                                    style = AppText.body.copy(
                                        color = AppColors.Alert,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
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
                                    onValueChange = {
                                        targetText = it.filter { ch -> ch.isDigit() }.take(6)
                                        targetError = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("alert_threshold_input"),
                                    singleLine = true,
                                    isError = targetError != null,
                                    supportingText = targetError?.let { err ->
                                        {
                                            Text(
                                                text = err,
                                                modifier = Modifier.testTag("alert_threshold_error"),
                                                style = AppText.body.copy(color = AppColors.Alert, fontSize = 10.5.sp),
                                            )
                                        }
                                    },
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
                                    val parsedTarget = targetText.toDoubleOrNull()
                                    if (parsedTarget == null || parsedTarget <= 0.0) {
                                        targetError = "أدخل سعراً أكبر من 0"
                                        return@Button
                                    }
                                    targetError = null
                                    requestNotificationPermissionIfNeeded()
                                    GoldPriceNotificationHelper.saveAlertRuleCache(
                                        context = context,
                                        enabled = alertEnabled,
                                        above = alertAbove,
                                        karat = alertKarat,
                                        thresholdText = targetText,
                                    )
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
                                        val posted = GoldPriceNotificationHelper.showGoldPriceThresholdAlert(
                                            context = context,
                                            karat = alertKarat,
                                            currentPriceEgp = currentKaratPrice,
                                            thresholdEgp = threshold,
                                            isAbove = alertAbove,
                                        )
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (posted) {
                                                    "تم إرسال إشعار تجريبي لتنبيه عيار $alertKarat"
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
                                    text = "اختبار إشعار عيار $alertKarat الآن",
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

                if (onOpenCalculator != null) {
                    item {
                        OutlinedButton(
                            onClick = onOpenCalculator,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("price_open_calculator_button"),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Calculate,
                                contentDescription = null,
                                tint = AppColors.Brass,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "افتح حاسبة قيمة الذهب المباشرة",
                                style = AppText.body.copy(
                                    color = AppColors.Ink,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
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
