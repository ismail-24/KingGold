package com.example.ui.screens.orders

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoldOrder
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

private val PickupChecklist = listOf(
    "الوزن مطابق" to "القطع اتوزنت قدامك على ميزان المحل.",
    "الدمغة والعيار" to "دمغة العيار ظاهرة وواضحة على القطع.",
    "القطع مطابقة للطلب" to "النوع والمقاس زي ما اخترت بالظبط.",
    "استلمت الفاتورة" to "فاتورة المحل بنفس الأوزان والأسعار.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyPickupScreen(
    order: GoldOrder,
    onBack: () -> Unit,
    onConfirmRelease: () -> Unit,
) {
    val checkedStates = remember { mutableStateListOf(false, false, false, false) }
    val remaining = checkedStates.count { !it }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تأكيد الاستلام",
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
                    if (remaining > 0) {
                        Text(
                            text = "باقي $remaining بند قبل ما تقدر تأكد",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Button(
                        onClick = onConfirmRelease,
                        enabled = remaining == 0,
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_pickup_release_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brass,
                            contentColor = Color.White,
                            disabledContainerColor = AppColors.Line,
                        ),
                    ) {
                        Text(
                            text = "كل شيء سليم · أؤكد الاستلام",
                            style = AppText.body.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                            ),
                        )
                    }
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
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column {
                        Text(
                            text = "اتأكد من القطع قدام البائع",
                            style = AppText.display.copy(fontSize = 20.sp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "علّم على كل بند بعد ما تتأكد منه بنفسك. التأكيد بيحوّل المبلغ للمحل.",
                            style = AppText.body.copy(color = AppColors.Muted),
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.Ink)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "محفوظ في الضمان",
                            style = AppText.body.copy(color = AppColors.BrassLight),
                        )
                        Text(
                            text = "${formatCurrency(order.total)} ج.م",
                            style = AppText.display.copy(color = Color.White, fontSize = 19.sp),
                        )
                    }
                }

                itemsIndexed(PickupChecklist) { index, (title, subtitle) ->
                    val checked = checkedStates[index]
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color.White)
                            .border(
                                width = if (checked) 1.5.dp else 1.dp,
                                color = if (checked) AppColors.Trust else AppColors.Line,
                                shape = shape,
                            )
                            .clickable { checkedStates[index] = !checked }
                            .padding(12.dp)
                            .testTag("verify_check_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { checkedStates[index] = it },
                            colors = CheckboxDefaults.colors(checkedColor = AppColors.Trust),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = subtitle,
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.AlertSoft)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "تنبيه",
                            tint = AppColors.Alert,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بعد التأكيد بيتحوّل المبلغ للمحل ومينفعش يتسحب. لو في أي ملاحظة، أبلّغ قبل ما تأكد.",
                            style = AppText.body.copy(color = AppColors.Alert, fontSize = 12.sp),
                        )
                    }
                }
            }
        }
    }
}
