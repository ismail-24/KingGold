package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoldOrder
import com.example.model.KycStatus
import com.example.model.OrderStatus
import com.example.ui.components.SealBadge
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onOrderClick: (GoldOrder) -> Unit,
    onSignOut: () -> Unit,
) {
    var showSignOutDialog by remember { mutableStateOf(false) }

    val displayEmail = uiState.userEmail.ifBlank { "حساب Google موثّق" }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الملف الشخصي",
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_user_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppColors.Line),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SealBadge(size = 54.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.userName,
                                        style = AppText.display.copy(fontSize = 19.sp),
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Email,
                                            contentDescription = "البريد الإلكتروني",
                                            tint = AppColors.Brass,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = displayEmail,
                                            modifier = Modifier.testTag("profile_user_email"),
                                            style = AppText.body.copy(
                                                color = AppColors.Muted,
                                                fontSize = 13.sp,
                                            ),
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (uiState.kycStatus == KycStatus.VERIFIED) AppColors.TrustSoft else AppColors.ParchmentDeep,
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (uiState.kycStatus == KycStatus.VERIFIED) AppColors.Trust else AppColors.Muted,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = uiState.kycStatus.label,
                                        style = AppText.body.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.kycStatus == KycStatus.VERIFIED) AppColors.Trust else AppColors.Muted,
                                        ),
                                    )
                                }

                                Button(
                                    onClick = { showSignOutDialog = true },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("profile_sign_out_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.AlertSoft,
                                        contentColor = AppColors.Alert,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تسجيل الخروج",
                                        style = AppText.body.copy(
                                            color = AppColors.Alert,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.Ink,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = AppColors.BrassLight,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الرصيد المحفوظ في الضمان",
                                        style = AppText.body.copy(color = AppColors.BrassLight, fontSize = 12.sp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${formatCurrency(uiState.escrowHeld)} ج.م",
                                    style = AppText.display.copy(color = Color.White, fontSize = 22.sp),
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "إجمالي الطلبات",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                                )
                                Text(
                                    text = "${uiState.orders.size}",
                                    style = AppText.display.copy(color = AppColors.BrassLight, fontSize = 22.sp),
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "سجل الطلبات",
                        style = AppText.display.copy(fontSize = 18.sp),
                    )
                }

                if (uiState.orders.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, AppColors.Line, RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = AppColors.Muted,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد طلبات مسجلة حتى الآن",
                                style = AppText.body.copy(color = AppColors.Muted),
                            )
                        }
                    }
                } else {
                    items(uiState.orders, key = { it.id }) { order ->
                        ProfileOrderHistoryItem(
                            order = order,
                            onClick = { onOrderClick(order) },
                        )
                    }
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = AppColors.Parchment,
            title = {
                Text("تسجيل الخروج", style = AppText.display.copy(fontSize = 19.sp))
            },
            text = {
                Text(
                    "هل تريد تسجيل الخروج من $displayEmail؟ تظل طلباتك وسجل الضمان محفوظة في السحابة.",
                    style = AppText.body.copy(fontSize = 13.5.sp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                ) {
                    Text(
                        "خروج",
                        style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(
                        "إلغاء",
                        style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold),
                    )
                }
            },
        )
    }
}

@Composable
private fun ProfileOrderHistoryItem(
    order: GoldOrder,
    onClick: () -> Unit,
) {
    val statusColor = when (order.status) {
        OrderStatus.COMPLETED -> AppColors.Muted
        OrderStatus.DISPUTED -> AppColors.Alert
        else -> AppColors.Trust
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, AppColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("profile_order_${order.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "طلب ${order.id}",
                    style = AppText.body.copy(fontWeight = FontWeight.ExtraBold),
                )
                Text(
                    text = "${formatCurrency(order.total)} ج.م",
                    style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, color = AppColors.Ink),
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${order.shop.name} · ${order.pickupWindow}",
                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = order.status.arabicLabel,
                    style = AppText.body.copy(
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "عرض تفاصيل الطلب",
            tint = AppColors.Muted,
        )
    }
}
