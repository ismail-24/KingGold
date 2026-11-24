package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoldPremiumMode
import com.example.model.GoldPriceCalculator
import com.example.model.KaratPrice
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import java.util.Locale

private val CALCULATOR_KARATS = listOf(24, 22, 21, 18, 14)
private const val DEFAULT_USD_TO_EGP_RATE = 49.5

data class GoldValuationResult(
    val weightGrams: Double,
    val karat: Int,
    val gramPriceEgp: Double,
    val baseGoldValueEgp: Double,
    val totalMakingFeeEgp: Double,
    val totalMarketValueEgp: Double,
    val pure24kEquivalentGrams: Double,
    val totalMarketValueUsd: Double = totalMarketValueEgp / DEFAULT_USD_TO_EGP_RATE,
)

fun calculateGoldMarketValue(
    weightGrams: Double,
    karat: Int,
    gramPriceEgp: Double,
    makingFeePerGram: Double = 0.0,
    premiumMode: GoldPremiumMode = GoldPremiumMode.PER_GRAM,
    usdToEgpRate: Double = DEFAULT_USD_TO_EGP_RATE,
): GoldValuationResult {
    val safeWeight = if (weightGrams.isFinite() && weightGrams > 0.0) weightGrams else 0.0
    val safePrice = if (gramPriceEgp.isFinite() && gramPriceEgp > 0.0) gramPriceEgp else 0.0
    val safeFee = if (makingFeePerGram.isFinite() && makingFeePerGram > 0.0) makingFeePerGram else 0.0
    val safeFx = if (usdToEgpRate.isFinite() && usdToEgpRate > 1.0) usdToEgpRate else DEFAULT_USD_TO_EGP_RATE
    val clampedKarat = karat.coerceIn(1, 24)

    val calculated = GoldPriceCalculator.calculate(
        weightGrams = safeWeight,
        karat = clampedKarat,
        marketPricePerGram = safePrice,
        premiumMode = premiumMode,
        premiumInputValue = safeFee,
    )
    val pureEquivalent = safeWeight * (clampedKarat / 24.0)
    return GoldValuationResult(
        weightGrams = calculated.weightGrams,
        karat = clampedKarat,
        gramPriceEgp = calculated.marketPricePerGram,
        baseGoldValueEgp = calculated.rawGoldTotal,
        totalMakingFeeEgp = calculated.premiumTotal,
        totalMarketValueEgp = calculated.finalTotalPrice,
        pure24kEquivalentGrams = pureEquivalent,
        totalMarketValueUsd = calculated.finalTotalPrice / safeFx,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalculatorScreen(
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onOpenPriceInsight: () -> Unit,
    onRefreshPrices: () -> Unit = {},
    onOpenAiAdvisor: ((String) -> Unit)? = null,
) {
    val usdToEgp = (uiState.goldFeedStatus as? GoldFeedStatus.Live)?.usdToEgpRate
        ?: DEFAULT_USD_TO_EGP_RATE

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "حاسبة ومحول الذهب المباشر",
                        style = AppText.display.copy(fontSize = 19.sp),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calculator_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = AppColors.Ink,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefreshPrices,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("calculator_refresh_live_button"),
                    ) {
                        if (uiState.goldFeedStatus is GoldFeedStatus.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Brass,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "تحديث أسعار الذهب المباشرة",
                                tint = AppColors.Brass,
                            )
                        }
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
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GoldCalculatorCard(
                    prices = uiState.prices,
                    feedStatus = uiState.goldFeedStatus,
                    onRefreshPrices = onRefreshPrices,
                    onOpenPriceInsight = onOpenPriceInsight,
                    onOpenAiAdvisor = onOpenAiAdvisor,
                )

                GoldCurrencyConverterCard(
                    prices = uiState.prices,
                    feedStatus = uiState.goldFeedStatus,
                    onRefreshPrices = onRefreshPrices,
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_live_reference_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "مرجع الأسعار المباشرة المستخدمة",
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        CALCULATOR_KARATS.forEach { karat ->
                            val priceEgp = GoldPriceCalculator.marketPriceForKarat(uiState.prices, karat)
                            val priceUsd = priceEgp / usdToEgp.coerceAtLeast(1.0)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "سعر جرام عيار $karat",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.sp),
                                )
                                Text(
                                    text = "${formatCurrency(priceEgp)} ج.م · $${formatCurrency(priceUsd, 2)}",
                                    style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
                                )
                            }
                        }
                        HorizontalDivider(color = AppColors.ParchmentDeep)
                        Text(
                            text = "الجنيه الذهب يزن 8 جرام عيار 21، والأونصة العالمية 31.1 جرام عيار 24.",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoldCalculatorCard(
    prices: List<KaratPrice>,
    modifier: Modifier = Modifier,
    feedStatus: GoldFeedStatus = GoldFeedStatus.OfflineFallback("أسعار مرجعية محلية"),
    onRefreshPrices: (() -> Unit)? = null,
    onOpenPriceInsight: (() -> Unit)? = null,
    onOpenAiAdvisor: ((String) -> Unit)? = null,
) {
    var weightText by rememberSaveable { mutableStateOf("8") }
    var makingFeeText by rememberSaveable { mutableStateOf("0") }
    var selectedKarat by rememberSaveable { mutableIntStateOf(21) }
    var premiumMode by rememberSaveable { mutableStateOf(GoldPremiumMode.PER_GRAM) }

    val gramPrice = remember(prices, selectedKarat) {
        GoldPriceCalculator.marketPriceForKarat(prices, selectedKarat)
    }
    val usdToEgpRate = (feedStatus as? GoldFeedStatus.Live)?.usdToEgpRate ?: DEFAULT_USD_TO_EGP_RATE
    val parsedWeight = normalizeNumericInput(weightText).toDoubleOrNull() ?: 0.0
    val parsedMakingFee = normalizeNumericInput(makingFeeText).toDoubleOrNull() ?: 0.0

    val result = remember(parsedWeight, selectedKarat, gramPrice, parsedMakingFee, premiumMode, usdToEgpRate) {
        calculateGoldMarketValue(
            weightGrams = parsedWeight,
            karat = selectedKarat,
            gramPriceEgp = gramPrice,
            makingFeePerGram = parsedMakingFee,
            premiumMode = premiumMode,
            usdToEgpRate = usdToEgpRate,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gold_calculator_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.Ink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Calculate,
                            contentDescription = "حاسبة الذهب",
                            tint = AppColors.BrassLight,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "حاسبة قيمة الذهب",
                            style = AppText.display.copy(fontSize = 17.sp),
                        )
                        Text(
                            text = "احسب القيمة السوقية الفورية بالوزن والعيار",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_live_status_badge"),
                shape = RoundedCornerShape(10.dp),
                color = AppColors.Parchment,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        when (feedStatus) {
                            is GoldFeedStatus.Loading -> CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Brass,
                            )
                            is GoldFeedStatus.Live -> Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.LiveDot),
                            )
                            is GoldFeedStatus.OfflineFallback -> Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Brass),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (feedStatus) {
                                is GoldFeedStatus.Live ->
                                    "بيانات سوق مباشرة · الأونصة $${formatCurrency(feedStatus.usdPerOunce, 0)} · ${feedStatus.fetchedAtLabel}"
                                is GoldFeedStatus.Loading -> "جاري تحديث أسعار الذهب المباشرة..."
                                is GoldFeedStatus.OfflineFallback -> "تعمل الحاسبة بأحدث أسعار السوق المحفوظة"
                            },
                            style = AppText.body.copy(color = AppColors.Ink, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        )
                    }
                    if (onRefreshPrices != null) {
                        TextButton(onClick = onRefreshPrices) {
                            Text(
                                text = "تحديث",
                                style = AppText.body.copy(
                                    color = AppColors.Brass,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                ),
                            )
                        }
                    }
                }
            }

            Text(
                text = "اختر العيار",
                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CALCULATOR_KARATS.forEach { karat ->
                    FilterChip(
                        selected = selectedKarat == karat,
                        onClick = { selectedKarat = karat },
                        modifier = Modifier.testTag("calculator_karat_$karat"),
                        label = {
                            Text(
                                text = "عيار $karat",
                                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Ink,
                            selectedLabelColor = AppColors.BrassLight,
                            containerColor = AppColors.Parchment,
                            labelColor = AppColors.Ink,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = weightText,
                onValueChange = { input ->
                    if (isValidDecimalInput(input)) {
                        weightText = input.take(10)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_weight_input"),
                label = { Text("الوزن بالجرام (جم)", style = AppText.body.copy(fontSize = 12.5.sp)) },
                placeholder = { Text("مثال: 8", style = AppText.body.copy(color = AppColors.Muted)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Brass,
                    unfocusedBorderColor = AppColors.Line,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    "1" to "1 جم",
                    "5" to "5 جم",
                    "8" to "8 جم (جنيه)",
                    "10" to "10 جم",
                    "20" to "20 جم",
                    "31.1" to "أونصة",
                ).forEach { (grams, label) ->
                    val tagSuffix = grams.replace('.', '_')
                    FilterChip(
                        selected = weightText == grams,
                        onClick = { weightText = grams },
                        modifier = Modifier.testTag("calculator_preset_$tagSuffix"),
                        label = { Text(label, style = AppText.body.copy(fontSize = 11.5.sp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Brass,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.Parchment,
                            labelColor = AppColors.Ink,
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GoldPremiumMode.entries.forEach { mode ->
                    FilterChip(
                        selected = premiumMode == mode,
                        onClick = { premiumMode = mode },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calculator_premium_mode_${mode.name.lowercase()}"),
                        label = {
                            Text(
                                text = mode.label,
                                maxLines = 1,
                                style = AppText.body.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Ink,
                            selectedLabelColor = AppColors.BrassLight,
                            containerColor = AppColors.Parchment,
                            labelColor = AppColors.Ink,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = makingFeeText,
                onValueChange = { input ->
                    if (isValidDecimalInput(input)) {
                        makingFeeText = input.take(10)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_making_fee_input"),
                label = {
                    Text(
                        text = "المصنعية / العلاوة (${premiumMode.label}) — اختياري",
                        style = AppText.body.copy(fontSize = 12.sp),
                    )
                },
                suffix = {
                    Text(
                        text = premiumMode.unitHint,
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                    )
                },
                placeholder = { Text("0 للذهب الخام", style = AppText.body.copy(color = AppColors.Muted)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Brass,
                    unfocusedBorderColor = AppColors.Line,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_result_panel"),
                shape = RoundedCornerShape(14.dp),
                color = AppColors.Ink,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "القيمة السوقية المقدرة الآن",
                        style = AppText.body.copy(color = AppColors.BrassLight, fontSize = 12.sp),
                    )
                    Text(
                        text = "${formatCurrency(result.totalMarketValueEgp)} ج.م",
                        modifier = Modifier.testTag("calculator_result_value"),
                        style = AppText.display.copy(color = Color.White, fontSize = 24.sp),
                    )
                    Text(
                        text = "≈ $${formatCurrency(result.totalMarketValueUsd)} USD",
                        modifier = Modifier.testTag("calculator_result_usd"),
                        style = AppText.body.copy(
                            color = AppColors.BrassLight.copy(alpha = 0.9f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "سعر الجرام (عيار ${result.karat})",
                            style = AppText.body.copy(color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp),
                        )
                        Text(
                            text = "${formatCurrency(result.gramPriceEgp)} ج.م",
                            style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "قيمة الذهب الصافية",
                            style = AppText.body.copy(color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp),
                        )
                        Text(
                            text = "${formatCurrency(result.baseGoldValueEgp)} ج.م",
                            style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                        )
                    }
                    if (result.totalMakingFeeEgp > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "إجمالي المصنعية (${premiumMode.label})",
                                style = AppText.body.copy(color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp),
                            )
                            Text(
                                text = "${formatCurrency(result.totalMakingFeeEgp)} ج.م",
                                style = AppText.body.copy(color = AppColors.BrassLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "المكافئ بالذهب الخالص (24K)",
                            style = AppText.body.copy(color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp),
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", result.pure24kEquivalentGrams)} جم",
                            style = AppText.body.copy(color = AppColors.BrassLight, fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                        )
                    }
                }
            }

            if (onOpenAiAdvisor != null) {
                OutlinedButton(
                    onClick = {
                        val prompt = "قيّم لي ذهب عيار ${result.karat} بوزن ${formatCurrency(result.weightGrams)} جم (القيمة السوقية ${formatCurrency(result.totalMarketValueEgp)} ج.م بسعر ${formatCurrency(result.gramPriceEgp)} ج.م/جم). هل الوقت مناسب للبيع أم الاحتفاظ؟"
                        onOpenAiAdvisor(prompt)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("calculator_ask_ai_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppColors.Ink.copy(alpha = 0.25f)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = AppColors.Brass,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "استشارة مستشار Gemini حول هذه القيمة",
                        style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    )
                }
            }

            if (onOpenPriceInsight != null) {
                OutlinedButton(
                    onClick = onOpenPriceInsight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("calculator_price_insight_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppColors.Brass),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShowChart,
                        contentDescription = null,
                        tint = AppColors.Brass,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "عرض مؤشر الأسعار والتنبيهات المباشرة",
                        style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    )
                }
            }
        }
    }
}

private fun isValidDecimalInput(input: String): Boolean {
    if (input.isEmpty()) return true
    var dots = 0
    for (ch in input) {
        when {
            ch.isDigit() -> Unit
            ch == '.' || ch == ',' || ch == '٫' -> {
                dots++
                if (dots > 1) return false
            }
            else -> return false
        }
    }
    return true
}

private fun normalizeNumericInput(input: String): String = buildString {
    for (ch in input) {
        when (ch) {
            ',' , '٫' -> append('.')
            in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
            else -> append(ch)
        }
    }
}
