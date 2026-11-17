package com.example.ui.screens.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KingGoldRepository
import com.example.model.KycStatus
import com.example.model.MoneyUtils
import com.example.ui.components.SealBadge
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    uiState: KingGoldUiState,
    onOpenProfile: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onOpenSellGold: () -> Unit = {},
    onOpenMerchantPortal: () -> Unit = {},
    onOpenMerchantRegistration: () -> Unit = {},
    onOpenAdminCenter: () -> Unit = {},
    onOpenAdminApproval: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onOpenKyc: () -> Unit,
    onOpenPriceAlerts: () -> Unit,
    onSelectPaymentMethod: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var showPaymentMethodsSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val accountSubtitle = uiState.userEmail.ifBlank {
        if (uiState.phoneNumber.length >= 4) {
            "+20 10X XXX XX${uiState.phoneNumber.takeLast(2)}"
        } else {
            "حساب Google موثّق"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Parchment)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "حسابي",
                    style = AppText.display.copy(fontSize = 22.sp),
                )
            }

            item {
                val cardShape = RoundedCornerShape(16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(Color.White)
                        .border(1.dp, AppColors.Line, cardShape)
                        .clickable(onClick = onOpenProfile)
                        .padding(16.dp)
                        .testTag("account_profile_card"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SealBadge(size = 48.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.userName,
                            style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                        )
                        Text(
                            text = accountSubtitle,
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (uiState.kycStatus == KycStatus.VERIFIED) AppColors.TrustSoft else AppColors.ParchmentDeep,
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = uiState.kycStatus.label,
                            style = AppText.body.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.kycStatus == KycStatus.VERIFIED) AppColors.Trust else AppColors.Muted,
                            ),
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                AccountMenuTile(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "خزنتي ومحفظتي (الذهب والنقد)",
                    subtitle = "${MoneyUtils.mgToGramsString(uiState.wallet.goldConfirmedMg)} جم ذهب · ${MoneyUtils.piastersToEgpString(uiState.wallet.cashAvailablePiasters)} ج.م متاح",
                    onClick = onOpenVault,
                    modifier = Modifier.testTag("account_vault_tile"),
                )
            }

            item {
                AccountMenuTile(
                    icon = Icons.Outlined.Sell,
                    title = "بيع ذهبك للمنصة",
                    subtitle = "اعرض قطعك للتقييم الرسمي وسحب القيمة لمحفظتك (${uiState.sellRequests.size} طلب)",
                    onClick = onOpenSellGold,
                    modifier = Modifier.testTag("account_sell_gold_tile"),
                )
            }

            item {
                val ownMerchant = uiState.ownMerchants.firstOrNull()
                val merchantSub = if (ownMerchant != null) {
                    "${ownMerchant.shopName} · الحالة: ${ownMerchant.status.arabicLabel}"
                } else {
                    "سجّل متجرك واعرض منتجاتك بدون مصنعية إضافية للمشتري"
                }
                AccountMenuTile(
                    icon = Icons.Outlined.Storefront,
                    title = "بوابة التاجر (التجار المعتمدين)",
                    subtitle = merchantSub,
                    onClick = onOpenMerchantPortal,
                    modifier = Modifier.testTag("account_merchant_tile"),
                )
            }

            item {
                AccountMenuTile(
                    icon = Icons.Outlined.Badge,
                    title = "تسجيل متجر جديد (طلب اعتماد تاجر)",
                    subtitle = "نموذج مخصص لتسجيل التاجر وحفظه في merchants بحالة قيد المراجعة",
                    onClick = onOpenMerchantRegistration,
                    modifier = Modifier.testTag("account_merchant_register_tile"),
                )
            }

            if (uiState.isAdmin) {
                item {
                    AccountMenuTile(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = "مركز إدارة المنصة (Admin)",
                        subtitle = "اعتماد التجار، المنتجات، تقييم الذهب، التسويات، والتسعير",
                        onClick = onOpenAdminCenter,
                        modifier = Modifier.testTag("account_admin_tile"),
                    )
                }

                item {
                    val pendingMerchantsCount = uiState.allMerchantsAdmin.count { it.status.name == "PENDING" }
                    AccountMenuTile(
                        icon = Icons.Outlined.Storefront,
                        title = "اعتماد التجار المعلّقين (Pending)",
                        subtitle = "مراجعة طلبات merchants ذات الحالة PENDING ($pendingMerchantsCount طلب)",
                        onClick = onOpenAdminApproval,
                        modifier = Modifier.testTag("account_admin_approval_tile"),
                    )
                }
            }

            item {
                AccountMenuTile(
                    icon = Icons.Outlined.Person,
                    title = "الملف الشخصي وسجل الطلبات",
                    subtitle = "$accountSubtitle · ${uiState.orders.size} طلبات",
                    onClick = onOpenProfile,
                    modifier = Modifier.testTag("account_profile_tile"),
                )
            }

            item {
                AccountMenuTile(
                    icon = Icons.Outlined.SupportAgent,
                    title = "مركز الدعم والمساعدة",
                    subtitle = "الأسئلة الشائعة ومستشار الملك الذكي",
                    onClick = onOpenSupport,
                    modifier = Modifier.testTag("account_support_tile"),
                )
            }

            item {
                AccountMenuTile(
                    icon = Icons.Outlined.Badge,
                    title = "توثيق الهوية (KYC)",
                    subtitle = "خطوة واحدة لتأمين استلامك",
                    onClick = onOpenKyc,
                    modifier = Modifier.testTag("account_kyc_tile"),
                )
            }

            item {
                AccountMenuTile(
                    icon = Icons.Outlined.CreditCard,
                    title = "وسائل الدفع",
                    subtitle = "الافتراضية: ${uiState.selectedPaymentMethod.title}",
                    onClick = { showPaymentMethodsSheet = true },
                    modifier = Modifier.testTag("account_payment_tile"),
                )
            }

            item {
                AccountMenuTile(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "تنبيهات الأسعار (FCM)",
                    subtitle = if (uiState.priceAlertEnabled) {
                        val dir = if (uiState.priceAlertAbove) "أعلى من" else "أقل من"
                        "عيار ${uiState.priceAlertKarat} · $dir ${uiState.priceAlertTarget} ج.م"
                    } else {
                        "نبّهني عند هبوط عيار 24 عن حد معيّن"
                    },
                    onClick = onOpenPriceAlerts,
                    modifier = Modifier.testTag("account_alerts_tile"),
                )
            }

            item {
                AccountMenuTile(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "تسجيل الخروج",
                    subtitle = null,
                    danger = true,
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.testTag("account_logout_tile"),
                )
            }
        }
    }

    if (showPaymentMethodsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentMethodsSheet = false },
            containerColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            ) {
                Text(
                    text = "وسائل الدفع المسجّلة",
                    style = AppText.display.copy(fontSize = 18.sp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                uiState.availablePaymentMethods.forEach { method ->
                    val selected = uiState.selectedPaymentMethodId == method.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectPaymentMethod(method.id)
                                showPaymentMethodsSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                            onClick = {
                                onSelectPaymentMethod(method.id)
                                showPaymentMethodsSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Brass),
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = AppColors.Parchment,
            title = {
                Text("تسجيل الخروج", style = AppText.display.copy(fontSize = 19.sp))
            },
            text = {
                Text(
                    "هل تريد الخروج من حسابك؟ طلباتك ومحفظة الضمان تظل محفوظة.",
                    style = AppText.body.copy(fontSize = 13.5.sp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                ) {
                    Text(
                        "خروج",
                        style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
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
private fun AccountMenuTile(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, AppColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (danger) AppColors.Alert else AppColors.Brass,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppText.body.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (danger) AppColors.Alert else AppColors.Ink,
                ),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = AppColors.Muted,
        )
    }
}
