package com.example.ui.screens.checkout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EscrowTimeline
import com.example.ui.components.LedgerCard
import com.example.ui.components.LedgerLine
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onSelectPaymentMethod: (String) -> Unit,
    onConfirmPay: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مراجعة الدفع",
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
        bottomBar = {
            Surface(
                color = AppColors.Parchment,
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = onConfirmPay,
                        enabled = uiState.cart.isNotEmpty(),
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("pay_now_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brass,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ادفع ${formatCurrency(uiState.cartTotal)} ج.م بأمان",
                            style = AppText.body.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "محمي بنظام الضمان. لا يُصرف أي مبلغ للمحل قبل استلامك للذهب.",
                        style = AppText.body.copy(fontSize = 11.sp, color = AppColors.Muted),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (uiState.cart.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "لا توجد منتجات في السلة لإتمام الدفع",
                        style = AppText.display.copy(fontSize = 18.sp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("checkout_empty_back_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Ink,
                            contentColor = AppColors.BrassLight,
                        ),
                    ) {
                        Text(
                            text = "العودة للسلة",
                            style = AppText.body.copy(color = AppColors.BrassLight, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (uiState.actionErrorMessage != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.AlertSoft)
                                    .border(1.dp, AppColors.Alert.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = uiState.actionErrorMessage,
                                    style = AppText.body.copy(color = AppColors.Alert, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }

                    item {
                    LedgerCard(
                        lines = listOf(
                            LedgerLine("إجمالي سعر القطع", formatCurrency(uiState.cartGoldValue)),
                            LedgerLine("مصنعية إضافية", "0.00 ج.م (مشمولة)"),
                            LedgerLine("السعر النهائي", "${formatCurrency(uiState.cartTotal)} ج.م", isTotal = true),
                        ),
                    )
                }

                item {
                    EscrowTimeline(activeStep = 0)
                }

                item {
                    Text(
                        text = "ادفع بواسطة",
                        style = AppText.display.copy(fontSize = 16.sp),
                    )
                }

                items(uiState.availablePaymentMethods, key = { it.id }) { method ->
                    val selected = uiState.selectedPaymentMethodId == method.id
                    val icon = when (method.id) {
                        "card" -> Icons.Outlined.CreditCard
                        "instapay" -> Icons.Outlined.PhoneIphone
                        else -> Icons.Outlined.AccountBalanceWallet
                    }
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color.White)
                            .border(
                                width = if (selected) 1.6.dp else 1.dp,
                                color = if (selected) AppColors.Brass else AppColors.Line,
                                shape = shape,
                            )
                            .clickable { onSelectPaymentMethod(method.id) }
                            .padding(14.dp)
                            .testTag("payment_method_${method.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = method.title,
                            tint = AppColors.Brass,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = method.title,
                                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = method.subtitle,
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                            )
                        }
                        RadioButton(
                            selected = selected,
                            onClick = { onSelectPaymentMethod(method.id) },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Brass),
                        )
                    }
                }
            }
            }
        }
    }
}
