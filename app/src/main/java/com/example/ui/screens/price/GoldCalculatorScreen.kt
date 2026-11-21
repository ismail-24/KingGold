package com.example.ui.screens.price

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.GoldFeedStatus
import com.example.ui.components.LedgerCard
import com.example.ui.components.LedgerLine
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlin.math.roundToInt

private val SUPPORTED_KARATS = listOf(24, 22, 21, 18, 14)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoldCalculatorScreen(
    uiState: KingGoldUiState,
    onRefreshPrices: () -> Unit = {},
    onOpenAiAdvisor: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedKarat by rememberSaveable { mutableIntStateOf(21) }
    var weightText by rememberSaveable { mutableStateOf("8") }
    var useLatestLivePrice by rememberSaveable { mutableStateOf(true) }
    val latestKaratMarketPrice = remember(uiState.prices, selectedKarat) {
        GoldPriceCalculator.marketPriceForKarat(uiState.prices, selectedKarat)
    }
    var marketPriceText by rememberSaveable {
        mutableStateOf(latestKaratMarketPrice.roundToInt().toString())
    }
    var premiumMode by rememberSaveable { mutableStateOf(GoldPremiumMode.PER_GRAM) }
    var premiumText by rememberSaveable { mutableStateOf("0") }

    LaunchedEffect(latestKaratMarketPrice, useLatestLivePrice) {
        if (useLatestLivePrice && latestKaratMarketPrice > 0.0) {
            marketPriceText = latestKaratMarketPrice.roundToInt().toString()
        }
    }

    val weight = weightText.toDoubleOrNull() ?: 0.0
    val marketPrice = marketPriceText.toDoubleOrNull() ?: 0.0
    val premiumValue = premiumText.toDoubleOrNull() ?: 0.0

    val result = remember(weight, selectedKarat, marketPrice, premiumMode, premiumValue) {
        GoldPriceCalculator.calculate(
            weightGrams = weight,
            karat = selectedKarat,
            marketPricePerGram = marketPrice,
            premiumMode = premiumMode,
            premiumInputValue = premiumValue,
        )
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "حاسبة الذهب",
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
                actions = {
                    IconButton(
                        onClick = {
                            useLatestLivePrice = true
                            onRefreshPrices()
                        },
                        modifier = Modifier.testTag("calc_refresh_live_prices"),
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
                                contentDescription = "تحديث أسعار السوق",
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
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Ink),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppColors.Brass.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Calculate,
                                        contentDescription = "حاسبة الذهب",
                                        tint = AppColors.BrassLight,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "القيمة السوقية الحالية (عيار $selectedKarat)",
                                        style = AppText.body.copy(
                                            color = AppColors.BrassLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                    Text(
                                        text = "${formatCurrency(result.rawGoldTotal, decimals = 2)} ج.م",
                                        modifier = Modifier.testTag("calc_market_value_total"),
                                        style = AppText.display.copy(
                                            color = Color.White,
                                            fontSize = 25.sp,
                                        ),
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (useLatestLivePrice) Color(0xFF4ADE80) else AppColors.BrassLight,
                                            ),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val feedMeta = when (val status = uiState.goldFeedStatus) {
                                        is GoldFeedStatus.Live -> "مباشر · ${status.fetchedAtLabel}"
                                        is GoldFeedStatus.Loading -> "جاري تحديث الأسعار..."
                                        is GoldFeedStatus.OfflineFallback -> "أحدث سعر محلي محفوظ"
                                    }
                                    Text(
                                        text = if (useLatestLivePrice) feedMeta else "سعر مخصص يدوياً",
                                        style = AppText.body.copy(
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.5.sp,
                                        ),
                                    )
                                }
                                Text(
                                    text = "${formatCurrency(result.marketPricePerGram, 0)} ج.م / جم",
                                    style = AppText.body.copy(
                                        color = AppColors.BrassLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                    ),
                                )
                            }

                            if (result.premiumTotal > 0.0) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "الإجمالي بعد إضافة المصنعية:",
                                        style = AppText.body.copy(
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 12.5.sp,
                                        ),
                                    )
                                    Text(
                                        text = "${formatCurrency(result.finalTotalPrice, decimals = 2)} ج.م",
                                        modifier = Modifier.testTag("calc_final_total"),
                                        style = AppText.body.copy(
                                            color = AppColors.BrassLight,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                        ),
                                    )
                                }
                            } else {
                                Text(
                                    text = "إجمالي السعر الصافي بدون مصنعية: ${formatCurrency(result.finalTotalPrice, decimals = 2)} ج.م",
                                    modifier = Modifier.testTag("calc_final_total"),
                                    style = AppText.body.copy(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                    ),
                                )
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "1. اختر العيار",
                            style = AppText.display.copy(fontSize = 15.sp),
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SUPPORTED_KARATS.forEach { karat ->
                                FilterChip(
                                    selected = selectedKarat == karat,
                                    onClick = {
                                        selectedKarat = karat
                                        useLatestLivePrice = true
                                        marketPriceText = GoldPriceCalculator
                                            .marketPriceForKarat(uiState.prices, karat)
                                            .roundToInt()
                                            .toString()
                                    },
                                    modifier = Modifier.testTag("calc_karat_$karat"),
                                    label = {
                                        Text(
                                            text = "عيار $karat",
                                            style = AppText.body.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
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

                        Text(
                            text = "2. أدخل الوزن (بالجرام)",
                            style = AppText.display.copy(fontSize = 15.sp),
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { input ->
                                val cleaned = sanitizeDecimalInput(input)
                                if (cleaned != null) weightText = cleaned
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_weight_input"),
                            singleLine = true,
                            placeholder = {
                                Text("أدخل الوزن بالجرام", style = AppText.body.copy(color = AppColors.Muted))
                            },
                            suffix = {
                                Text("جم", style = AppText.body.copy(color = AppColors.Muted))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = calcFieldColors(),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("1", "5", "8", "10", "20", "31.1").forEach { preset ->
                                FilterChip(
                                    selected = weightText == preset,
                                    onClick = { weightText = preset },
                                    label = {
                                        Text(
                                            text = "$preset جم",
                                            style = AppText.body.copy(fontSize = 11.5.sp),
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.Brass,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "3. سعر السوق للجرام (مرتبط بالتحديث)",
                                style = AppText.display.copy(fontSize = 14.5.sp),
                            )
                            TextButton(
                                onClick = {
                                    useLatestLivePrice = true
                                    marketPriceText = latestKaratMarketPrice.roundToInt().toString()
                                    onRefreshPrices()
                                },
                                modifier = Modifier.testTag("calc_fill_live_price"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "تعبئة السعر اللحظي",
                                    tint = AppColors.Brass,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تحديث",
                                    style = AppText.body.copy(
                                        color = AppColors.Brass,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = marketPriceText,
                            onValueChange = { input ->
                                val cleaned = sanitizeDecimalInput(input)
                                if (cleaned != null) {
                                    useLatestLivePrice = false
                                    marketPriceText = cleaned
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_market_price_input"),
                            singleLine = true,
                            suffix = {
                                Text("ج.م / جم", style = AppText.body.copy(color = AppColors.Muted))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = calcFieldColors(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "4. مصنعية / علاوة إضافية (اختياري)",
                                style = AppText.display.copy(fontSize = 14.5.sp),
                            )
                            if (premiumText != "0") {
                                TextButton(onClick = { premiumText = "0" }) {
                                    Text(
                                        text = "تصفير",
                                        style = AppText.body.copy(
                                            color = AppColors.Brass,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                        ),
                                    )
                                }
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
                                        .testTag("calc_premium_mode_${mode.name.lowercase()}"),
                                    label = {
                                        Text(
                                            text = mode.label,
                                            maxLines = 1,
                                            style = AppText.body.copy(
                                                fontSize = 11.5.sp,
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
                        OutlinedTextField(
                            value = premiumText,
                            onValueChange = { input ->
                                val cleaned = sanitizeDecimalInput(input)
                                if (cleaned != null) premiumText = cleaned
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_premium_input"),
                            singleLine = true,
                            suffix = {
                                Text(
                                    text = premiumMode.unitHint,
                                    style = AppText.body.copy(color = AppColors.Muted),
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = calcFieldColors(),
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.testTag("calc_result_card")) {
                        Text(
                            text = "تفاصيل القيمة السوقية",
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LedgerCard(
                            lines = buildList {
                                add(
                                    LedgerLine(
                                        label = "قيمة الذهب الخام (عيار $selectedKarat · ${formatCurrency(result.weightGrams, 2)} جم × ${formatCurrency(result.marketPricePerGram, 2)})",
                                        value = "${formatCurrency(result.rawGoldTotal, 2)} ج.م",
                                        isTotal = result.premiumTotal == 0.0,
                                    ),
                                )
                                if (result.premiumTotal > 0.0) {
                                    add(
                                        LedgerLine(
                                            label = "المصنعية / العلاوة (${premiumMode.label})",
                                            value = "${formatCurrency(result.premiumTotal, 2)} ج.م",
                                        ),
                                    )
                                    add(
                                        LedgerLine(
                                            label = "متوسط التكلفة الفعلية للجرام",
                                            value = "${formatCurrency(result.effectivePricePerGram, 2)} ج.م",
                                        ),
                                    )
                                    add(
                                        LedgerLine(
                                            label = "الإجمالي النهائي",
                                            value = "${formatCurrency(result.finalTotalPrice, 2)} ج.م",
                                            isTotal = true,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            val prompt = "أريد تقييم ذهب عيار $selectedKarat بوزن ${formatCurrency(result.weightGrams, 2)} جم بسعر سوق ${formatCurrency(result.marketPricePerGram, 0)} ج.م (القيمة السوقية ${formatCurrency(result.rawGoldTotal, 0)} ج.م، ومصنعية ${formatCurrency(result.premiumTotal, 0)} ج.م). هل السعر مناسب الآن؟"
                            onOpenAiAdvisor(prompt)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("calc_ask_ai_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppColors.Brass),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تقييم السعر عبر Gemini",
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun calcFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = AppColors.Brass,
    unfocusedBorderColor = AppColors.Line,
)

private fun sanitizeDecimalInput(input: String): String? {
    if (input.isEmpty()) return ""
    var seenDot = false
    val filtered = buildString {
        for (ch in input) {
            when {
                ch.isDigit() -> append(ch)
                ch == '.' && !seenDot -> {
                    seenDot = true
                    append(ch)
                }
            }
        }
    }
    return filtered.take(10)
}
