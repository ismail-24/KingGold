package com.example.ui.components

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
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.KingGoldRepository
import com.example.model.KaratPrice
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import java.util.Locale

data class GoldValuationResult(
    val weightGrams: Double,
    val karat: Int,
    val gramPriceEgp: Double,
    val baseGoldValueEgp: Double,
    val totalMakingFeeEgp: Double,
    val totalMarketValueEgp: Double,
    val pure24kEquivalentGrams: Double,
)

fun calculateGoldMarketValue(
    weightGrams: Double,
    karat: Int,
    gramPriceEgp: Double,
    makingFeePerGram: Double = 0.0,
): GoldValuationResult {
    val safeWeight = if (weightGrams.isFinite() && weightGrams > 0.0) weightGrams else 0.0
    val safePrice = if (gramPriceEgp.isFinite() && gramPriceEgp > 0.0) gramPriceEgp else 0.0
    val safeFee = if (makingFeePerGram.isFinite() && makingFeePerGram > 0.0) makingFeePerGram else 0.0
    val clampedKarat = karat.coerceIn(1, 24)
    val baseGold = safeWeight * safePrice
    val totalMaking = safeWeight * safeFee
    val pureEquivalent = when (clampedKarat) {
        24 -> safeWeight
        21 -> safeWeight * (21.0 / 24.0)
        18 -> safeWeight * (18.0 / 24.0)
        14 -> safeWeight * (14.0 / 24.0)
        else -> safeWeight * (clampedKarat / 24.0)
    }
    return GoldValuationResult(
        weightGrams = safeWeight,
        karat = clampedKarat,
        gramPriceEgp = safePrice,
        baseGoldValueEgp = baseGold,
        totalMakingFeeEgp = totalMaking,
        totalMarketValueEgp = baseGold + totalMaking,
        pure24kEquivalentGrams = pureEquivalent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldCalculatorScreen(
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onOpenPriceInsight: () -> Unit,
) {
    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "حاسبة الذهب المباشرة",
                        style = AppText.display.copy(fontSize = 20.sp),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Parchment),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    GoldCalculatorCard(
                        prices = uiState.prices,
                        onOpenPriceInsight = onOpenPriceInsight,
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            listOf(24, 21, 18).forEach { karat ->
                                val price = uiState.gramPrice(karat)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "سعر جرام عيار $karat",
                                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.sp),
                                    )
                                    Text(
                                        text = "${formatCurrency(price)} ج.م",
                                        style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp),
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoldCalculatorCard(
    prices: List<KaratPrice>,
    modifier: Modifier = Modifier,
    onOpenPriceInsight: (() -> Unit)? = null,
) {
    var weightText by remember { mutableStateOf("8") }
    var makingFeeText by remember { mutableStateOf("0") }
    var selectedKarat by remember { mutableIntStateOf(21) }

    val gramPrice = prices.firstOrNull { it.karat == selectedKarat }?.pricePerGram
        ?: KingGoldRepository.gramPrice(selectedKarat)
    val parsedWeight = weightText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val parsedMakingFee = makingFeeText.replace(',', '.').toDoubleOrNull() ?: 0.0

    val result = remember(parsedWeight, selectedKarat, gramPrice, parsedMakingFee) {
        calculateGoldMarketValue(
            weightGrams = parsedWeight,
            karat = selectedKarat,
            gramPriceEgp = gramPrice,
            makingFeePerGram = parsedMakingFee,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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

            Text(
                text = "اختر العيار",
                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(24, 21, 18).forEach { karat ->
                    FilterChip(
                        selected = selectedKarat == karat,
                        onClick = { selectedKarat = karat },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calculator_karat_$karat"),
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
                    if (input.isEmpty() || input.all { it.isDigit() || it == '.' || it == ',' }) {
                        weightText = input
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

            OutlinedTextField(
                value = makingFeeText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.all { it.isDigit() || it == '.' || it == ',' }) {
                        makingFeeText = input
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_making_fee_input"),
                label = { Text("المصنعية لكل جرام (اختياري)", style = AppText.body.copy(fontSize = 12.5.sp)) },
                placeholder = { Text("0 ج.م/جم للخام", style = AppText.body.copy(color = AppColors.Muted)) },
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
                modifier = Modifier.fillMaxWidth(),
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
                                text = "إجمالي المصنعية",
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
