package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.GoldCurrencyConverter
import com.example.model.KaratPrice
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import java.util.Locale

private val CONVERTER_KARATS = listOf(24, 22, 21, 18, 14)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoldCurrencyConverterCard(
    prices: List<KaratPrice>,
    feedStatus: GoldFeedStatus,
    onRefreshPrices: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val liveStatus = feedStatus as? GoldFeedStatus.Live
    val usdToEgpRate = liveStatus?.usdToEgpRate ?: GoldCurrencyConverter.DEFAULT_USD_TO_EGP
    val liveFxRates = liveStatus?.fxRatesPerUsd.orEmpty()

    val detectedCurrency = remember(liveFxRates) {
        GoldCurrencyConverter.detectDeviceLocalCurrency(Locale.getDefault(), liveFxRates)
    }
    val currencies = remember(liveFxRates) {
        GoldCurrencyConverter.availableCurrencies(Locale.getDefault(), liveFxRates)
    }

    var weightText by rememberSaveable { mutableStateOf("10") }
    var selectedKarat by rememberSaveable { mutableIntStateOf(21) }
    var selectedCurrencyCode by rememberSaveable { mutableStateOf(detectedCurrency.code) }

    val selectedCurrency = remember(selectedCurrencyCode, currencies) {
        currencies.firstOrNull { it.code == selectedCurrencyCode } ?: detectedCurrency
    }
    val parsedWeight = GoldCurrencyConverter.normalizeNumericInput(weightText).toDoubleOrNull() ?: 0.0

    val conversion = remember(
        parsedWeight,
        selectedKarat,
        prices,
        selectedCurrency,
        usdToEgpRate,
        liveFxRates,
        liveStatus,
    ) {
        GoldCurrencyConverter.convert(
            weightGrams = parsedWeight,
            karat = selectedKarat,
            prices = prices,
            currency = selectedCurrency,
            usdToEgpRate = usdToEgpRate,
            fxRatesPerUsd = liveFxRates,
            usesLiveRates = liveStatus != null,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gold_currency_converter_card"),
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CurrencyExchange,
                        contentDescription = "محول سعر الذهب للعملة المحلية",
                        tint = AppColors.BrassLight,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "محول سعر الذهب للعملة المحلية",
                        style = AppText.display.copy(fontSize = 17.sp),
                    )
                    Text(
                        text = "أدخل الوزن بالجرام لعرض السعر الفوري بعملتك المحلية",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("converter_detected_locale_badge"),
                shape = RoundedCornerShape(10.dp),
                color = AppColors.Parchment,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "العملة المحلية المكتشفة: ${detectedCurrency.arabicName} (${detectedCurrency.code})",
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                    if (selectedCurrency.code != detectedCurrency.code) {
                        TextButton(
                            onClick = { selectedCurrencyCode = detectedCurrency.code },
                            modifier = Modifier.testTag("converter_use_local_currency_button"),
                        ) {
                            Text(
                                text = "عملتي",
                                style = AppText.body.copy(
                                    color = AppColors.Brass,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                ),
                            )
                        }
                    } else if (onRefreshPrices != null) {
                        TextButton(
                            onClick = onRefreshPrices,
                            modifier = Modifier.testTag("converter_refresh_button"),
                        ) {
                            Text(
                                text = "تحديث الصرف",
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

            OutlinedTextField(
                value = weightText,
                onValueChange = { input ->
                    if (GoldCurrencyConverter.isValidDecimalInput(input)) {
                        weightText = input.take(10)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("converter_weight_input"),
                label = { Text("الوزن بالجرام (جم)", style = AppText.body.copy(fontSize = 12.5.sp)) },
                placeholder = { Text("مثال: 10", style = AppText.body.copy(color = AppColors.Muted)) },
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
                    "8" to "8 جم",
                    "10" to "10 جم",
                    "31.1" to "31.1 جم (أونصة)",
                ).forEach { (grams, label) ->
                    val tagSuffix = grams.replace('.', '_')
                    FilterChip(
                        selected = weightText == grams,
                        onClick = { weightText = grams },
                        modifier = Modifier.testTag("converter_preset_$tagSuffix"),
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

            Text(
                text = "اختر العيار",
                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CONVERTER_KARATS.forEach { karat ->
                    FilterChip(
                        selected = selectedKarat == karat,
                        onClick = { selectedKarat = karat },
                        modifier = Modifier.testTag("converter_karat_$karat"),
                        label = {
                            Text(
                                text = "عيار $karat",
                                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
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

            Text(
                text = "اختر العملة المحلية أو العالمية",
                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                currencies.forEach { currency ->
                    FilterChip(
                        selected = selectedCurrency.code == currency.code,
                        onClick = { selectedCurrencyCode = currency.code },
                        modifier = Modifier.testTag("converter_currency_chip_${currency.code}"),
                        label = {
                            Text(
                                text = "${currency.code} · ${currency.symbol}",
                                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
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

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("converter_result_panel"),
                shape = RoundedCornerShape(14.dp),
                color = AppColors.Ink,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "السعر المكافئ (${selectedCurrency.arabicName})",
                            style = AppText.body.copy(color = AppColors.BrassLight, fontSize = 12.sp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (conversion.usesLiveRates) AppColors.LiveDot else AppColors.Brass),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (conversion.usesLiveRates) "صرف مباشر" else "صرف مرجعي",
                                style = AppText.body.copy(color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp),
                            )
                        }
                    }

                    Text(
                        text = "${formatCurrency(conversion.totalValueInCurrency)} ${selectedCurrency.symbol}",
                        modifier = Modifier.testTag("converter_result_value"),
                        style = AppText.display.copy(color = Color.White, fontSize = 24.sp),
                    )

                    Text(
                        text = "سعر الجرام (عيار ${conversion.karat}): ${formatCurrency(conversion.gramPriceInCurrency)} ${selectedCurrency.symbol}",
                        modifier = Modifier.testTag("converter_gram_rate"),
                        style = AppText.body.copy(
                            color = AppColors.BrassLight,
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
                            text = "المكافئ بالدولار والجنيه",
                            style = AppText.body.copy(color = Color.White.copy(alpha = 0.78f), fontSize = 11.5.sp),
                        )
                        Text(
                            text = "$${formatCurrency(conversion.totalValueUsd)} USD · ${formatCurrency(conversion.totalValueEgp)} ج.م",
                            modifier = Modifier.testTag("converter_cross_reference"),
                            style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                        )
                    }

                    Text(
                        text = "سعر الصرف المطبق: 1 USD = ${formatCurrency(conversion.fxRatePerUsd, 2)} ${selectedCurrency.code}",
                        modifier = Modifier.testTag("converter_fx_rate_label"),
                        style = AppText.body.copy(color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp),
                    )
                }
            }
        }
    }
}
