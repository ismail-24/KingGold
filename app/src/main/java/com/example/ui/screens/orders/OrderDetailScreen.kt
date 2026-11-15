package com.example.ui.screens.orders

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoldOrder
import com.example.model.OrderStatus
import com.example.ui.components.LedgerCard
import com.example.ui.components.LedgerLine
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.dialPhone
import com.example.ui.util.openDirections
import kotlinx.coroutines.launch

private val OrderSteps = listOf("تم الدفع", "قيد التجهيز", "جاهز للاستلام", "تم الاستلام")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: GoldOrder,
    onBack: () -> Unit,
    onStartVerifyPickup: () -> Unit,
    onReportIssue: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val activeIdx = when (order.status) {
        OrderStatus.PAID -> 0
        OrderStatus.PREPARING -> 1
        OrderStatus.READY_FOR_PICKUP -> 2
        OrderStatus.COMPLETED, OrderStatus.DISPUTED -> 3
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفاصيل الطلب ${order.id}",
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
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.Ink,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            OrderSteps.forEachIndexed { i, label ->
                                val done = i <= activeIdx
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (done) AppColors.BrassLight else AppColors.InkSoft),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (done) Icons.Filled.Check else Icons.Outlined.Circle,
                                            contentDescription = label,
                                            tint = if (done) AppColors.Ink else AppColors.Muted,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = label,
                                        textAlign = TextAlign.Center,
                                        style = AppText.body.copy(
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            color = if (done) Color.White else AppColors.Muted,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                if (order.status == OrderStatus.READY_FOR_PICKUP) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.TrustSoft)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode2,
                                contentDescription = "رمز الاستلام",
                                tint = AppColors.Trust,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "طلبك جاهز — روح المحل واعرض رمز الاستلام",
                                style = AppText.body.copy(
                                    color = AppColors.Trust,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                ),
                            )
                        }
                    }
                }

                if (order.status == OrderStatus.DISPUTED) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.AlertSoft)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = "بلاغ قيد المراجعة",
                                tint = AppColors.Alert,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "البلاغ تحت المراجعة (${order.disputeReason ?: "مراجعة الضمان"}) — مبلغك محفوظ بأمان.",
                                style = AppText.body.copy(color = AppColors.Alert, fontSize = 13.sp),
                            )
                        }
                    }
                }

                item {
                    val shape = RoundedCornerShape(14.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, shape)
                            .padding(14.dp),
                    ) {
                        Text(
                            text = order.shop.name,
                            style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                        )
                        Text(
                            text = order.shop.address,
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    dialPhone(context, order.shop.phone) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("هاتف المحل: ${order.shop.phone}")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("اتصل", style = AppText.body.copy(fontWeight = FontWeight.Bold))
                            }
                            OutlinedButton(
                                onClick = {
                                    openDirections(context, order.shop.address) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("العنوان: ${order.shop.address}")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("الاتجاهات", style = AppText.body.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                item {
                    LedgerCard(
                        lines = listOf(
                            LedgerLine("قيمة الذهب", formatCurrency(order.goldValue)),
                            LedgerLine("مصنعية المحل", formatCurrency(order.makingFee)),
                            LedgerLine("رسوم خدمة King Gold", formatCurrency(order.serviceFee)),
                            LedgerLine("الإجمالي المدفوع", "${formatCurrency(order.total)} ج.م", isTotal = true),
                        ),
                    )
                }

                if (order.status == OrderStatus.READY_FOR_PICKUP) {
                    item {
                        Button(
                            onClick = onStartVerifyPickup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_verify_pickup_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Brass,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = "وصلت المحل — ابدأ تأكيد الاستلام",
                                style = AppText.body.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                            )
                        }
                    }
                }

                if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.DISPUTED) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextButton(
                                onClick = onReportIssue,
                                modifier = Modifier.testTag("report_issue_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = "إبلاغ",
                                    tint = AppColors.Alert,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الإبلاغ عن مشكلة",
                                    style = AppText.body.copy(
                                        color = AppColors.Alert,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
