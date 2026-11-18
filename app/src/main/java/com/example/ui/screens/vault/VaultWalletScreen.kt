package com.example.ui.screens.vault

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCard
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LedgerTxType
import com.example.model.MoneyUtils
import com.example.model.WithdrawalDeliveryType
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class LedgerFilter(val label: String) {
    ALL("الكل"),
    CASH("النقدية"),
    GOLD("الذهب"),
    WITHDRAWALS("السحوبات"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultWalletScreen(
    uiState: KingGoldUiState,
    onBuyFractionalGold: (karat: Int, weightMg: Long, payWithWallet: Boolean, onSuccess: () -> Unit) -> Unit,
    onRequestWithdrawal: (karat: Int, amountMg: Long, deliveryType: WithdrawalDeliveryType, address: String, onSuccess: () -> Unit) -> Unit,
    onSubmitDepositRequest: (amountPiasters: Long, channel: String, reference: String, onSuccess: () -> Unit) -> Unit,
    onClearMessages: () -> Unit,
) {
    var showBuySheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    var showDepositSheet by remember { mutableStateOf(false) }
    var ledgerFilter by remember { mutableStateOf(LedgerFilter.ALL) }

    val wallet = uiState.wallet
    val availableForRedemption = wallet.goldAvailableMg
    val canWithdrawPhysical = availableForRedemption >= MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG
    val redemptionProgress = (availableForRedemption.coerceIn(0L, MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG).toFloat() /
        MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG.toFloat())

    val filteredLedger = remember(uiState.ledgerEntries, ledgerFilter) {
        uiState.ledgerEntries.filter { entry ->
            when (ledgerFilter) {
                LedgerFilter.ALL -> true
                LedgerFilter.CASH -> entry.txType in listOf(
                    LedgerTxType.CASH_DEPOSIT,
                    LedgerTxType.CASH_DEPOSIT_PENDING,
                    LedgerTxType.GOLD_SALE_PROCEEDS,
                    LedgerTxType.MARKETPLACE_ORDER_PAID,
                )
                LedgerFilter.GOLD -> entry.txType in listOf(
                    LedgerTxType.FRACTIONAL_GOLD_BUY,
                    LedgerTxType.FRACTIONAL_GOLD_PENDING,
                )
                LedgerFilter.WITHDRAWALS -> entry.txType in listOf(
                    LedgerTxType.GOLD_RESERVATION,
                    LedgerTxType.GOLD_WITHDRAWAL_FULFILLED,
                    LedgerTxType.GOLD_RESERVATION_RELEASED,
                )
            }
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
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "خزنتي ومحفظتي",
                        style = AppText.display.copy(fontSize = 22.sp),
                    )
                    Text(
                        text = "امتلك ذهبك بالملليجرام وتابع أرباحك ورصيدك النقدي لحظياً",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                    )
                }
            }

            if (uiState.actionBannerMessage != null || uiState.actionErrorMessage != null) {
                item {
                    val isErr = uiState.actionErrorMessage != null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isErr) AppColors.AlertSoft else AppColors.TrustSoft)
                            .border(
                                1.dp,
                                if (isErr) AppColors.Alert.copy(alpha = 0.4f) else AppColors.Trust.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.actionErrorMessage ?: uiState.actionBannerMessage.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = AppText.body.copy(
                                color = if (isErr) AppColors.Alert else AppColors.Trust,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            ),
                        )
                        androidx.compose.material3.TextButton(onClick = onClearMessages) {
                            Text("حسناً", style = AppText.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Digital Gold Vault Hero Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = AppColors.Ink,
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Diamond,
                                    contentDescription = "خزنة الذهب",
                                    tint = AppColors.BrassLight,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "خزنة الذهب الرقمية",
                                    style = AppText.body.copy(color = AppColors.BrassLight, fontWeight = FontWeight.Bold),
                                )
                            }
                            Text(
                                text = "${wallet.goldConfirmedMg} ملجم",
                                style = AppText.body.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp),
                            )
                        }

                        Text(
                            text = MoneyUtils.mgToGramsString(wallet.goldConfirmedMg),
                            style = AppText.display.copy(color = Color.White, fontSize = 28.sp),
                            modifier = Modifier.testTag("vault_total_grams_text"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            VaultStatChip(
                                label = "متاح للسحب",
                                value = MoneyUtils.mgToGramsString(availableForRedemption),
                                modifier = Modifier.weight(1f),
                            )
                            VaultStatChip(
                                label = "محجوز سحب",
                                value = MoneyUtils.mgToGramsString(wallet.goldReservedMg),
                                modifier = Modifier.weight(1f),
                            )
                            VaultStatChip(
                                label = "قيد التسوية",
                                value = MoneyUtils.mgToGramsString(wallet.goldPendingMg),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("إجمالي المستثمر", style = AppText.body.copy(color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp))
                                Text(
                                    "${MoneyUtils.piastersToEgpString(wallet.totalInvestedPiasters)} ج.م",
                                    style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("التقييم الحالي", style = AppText.body.copy(color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp))
                                Text(
                                    "${MoneyUtils.piastersToEgpString(uiState.vaultCurrentValuationPiasters)} ج.م",
                                    style = AppText.body.copy(color = AppColors.BrassLight, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val pl = uiState.vaultUnrealizedProfitLossPiasters
                                val pct = uiState.vaultProfitLossPercent
                                val isGain = pl >= 0L
                                val sign = if (pl > 0L) "+" else ""
                                Text("الربح/الخسارة", style = AppText.body.copy(color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp))
                                Text(
                                    "$sign${MoneyUtils.piastersToEgpString(pl)} ج.م (${sign}${String.format(Locale.US, "%.1f", pct)}%)",
                                    style = AppText.body.copy(
                                        color = if (isGain) AppColors.LiveGainDark else AppColors.AlertSoft,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    ),
                                )
                            }
                        }

                        if (wallet.goldConfirmedMg > 0L) {
                            Text(
                                text = "متوسط تكلفة الجرام: ${MoneyUtils.piastersToEgpString(uiState.vaultAvgCostPerGramPiasters)} ج.م",
                                style = AppText.body.copy(color = Color.White.copy(alpha = 0.72f), fontSize = 11.5.sp),
                            )
                        }

                        // Karat distribution visual bar chart
                        KaratBreakdownVisual(
                            mg24 = wallet.gold24kMg,
                            mg21 = wallet.gold21kMg,
                            mg18 = wallet.gold18kMg,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onClearMessages()
                                    showBuySheet = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("buy_fractional_gold_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Brass,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(Icons.Outlined.ShoppingCartCheckout, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("شراء ذهب مجزأ", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
                            }
                            OutlinedButton(
                                onClick = {
                                    onClearMessages()
                                    showWithdrawSheet = true
                                },
                                enabled = canWithdrawPhysical,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("withdraw_physical_gold_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (canWithdrawPhysical) AppColors.BrassLight else Color.White.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AppColors.BrassLight,
                                    disabledContentColor = Color.White.copy(alpha = 0.4f),
                                ),
                            ) {
                                Icon(
                                    if (canWithdrawPhysical) Icons.Outlined.LocalShipping else Icons.Outlined.Lock,
                                    null,
                                    modifier = Modifier.size(17.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سحب فعلي", style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
                            }
                        }

                        if (!canWithdrawPhysical) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { redemptionProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = AppColors.BrassLight,
                                    trackColor = Color.White.copy(alpha = 0.15f),
                                )
                                Text(
                                    text = "الحد الأدنى للاستلام الفعلي: 1.000 جرام (1000 ملجم) — لديك حالياً $availableForRedemption / 1000 ملجم متاح.",
                                    style = AppText.body.copy(color = AppColors.BrassLight, fontSize = 11.sp),
                                )
                            }
                        }
                    }
                }
            }

            // Cash Wallet Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = AppColors.Trust, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("المحفظة النقدية (EGP)", style = AppText.display.copy(fontSize = 17.sp))
                            }
                            OutlinedButton(
                                onClick = {
                                    onClearMessages()
                                    showDepositSheet = true
                                },
                                modifier = Modifier.testTag("open_deposit_sheet_button"),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.Outlined.AddCard, null, tint = AppColors.Brass, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("شحن المحفظة", style = AppText.body.copy(color = AppColors.Brass, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("الرصيد المتاح", style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp))
                                Text(
                                    "${MoneyUtils.piastersToEgpString(wallet.cashAvailablePiasters)} ج.م",
                                    style = AppText.display.copy(color = AppColors.Trust, fontSize = 22.sp),
                                    modifier = Modifier.testTag("wallet_cash_available_text"),
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("إيداعات قيد المراجعة", style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp))
                                Text(
                                    "${MoneyUtils.piastersToEgpString(wallet.cashPendingPiasters)} ج.م",
                                    style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "إجمالي المضاف: ${MoneyUtils.piastersToEgpString(wallet.cashTotalCreditedPiasters)} ج.م",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                            )
                            Text(
                                "إجمالي المصروف: ${MoneyUtils.piastersToEgpString(wallet.cashTotalDebitedPiasters)} ج.م",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                            )
                        }
                    }
                }
            }

            // Withdrawal Requests Section (if any)
            if (uiState.withdrawalRequests.isNotEmpty()) {
                item {
                    Text("طلبات السحب الفعلي", style = AppText.display.copy(fontSize = 16.sp))
                }
                items(uiState.withdrawalRequests, key = { it.id }) { wd ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppColors.Line),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${wd.withdrawalRef} · عيار ${wd.karat} (${MoneyUtils.mgToGramsString(wd.amountMg)})",
                                    style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                                )
                                Text(
                                    wd.status.arabicLabel,
                                    style = AppText.body.copy(color = AppColors.Brass, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                )
                            }
                            Text(
                                "${wd.deliveryType.arabicLabel} — ${wd.deliveryAddress}",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                            )
                            if (!wd.proofNote.isNullOrBlank()) {
                                Text("ملاحظة الإدارة: ${wd.proofNote}", style = AppText.body.copy(color = AppColors.Trust, fontSize = 12.sp))
                            }
                        }
                    }
                }
            }

            // Transaction Ledger Header & Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سجل المعاملات المالي (Ledger)", style = AppText.display.copy(fontSize = 17.sp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LedgerFilter.entries.forEach { f ->
                            FilterChip(
                                selected = ledgerFilter == f,
                                onClick = { ledgerFilter = f },
                                label = { Text(f.label, style = AppText.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Ink,
                                    selectedLabelColor = AppColors.BrassLight,
                                    containerColor = Color.White,
                                ),
                            )
                        }
                    }
                }
            }

            if (filteredLedger.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "لا توجد قيود مالية مسجلة في هذا التصنيف بعد.",
                            style = AppText.body.copy(color = AppColors.Muted),
                        )
                    }
                }
            } else {
                items(filteredLedger, key = { it.id }) { entry ->
                    val timeLabel = remember(entry.createdAtMs) {
                        if (entry.createdAtMs > 0L) {
                            SimpleDateFormat("dd MMM · h:mm a", Locale.forLanguageTag("ar")).format(Date(entry.createdAtMs))
                        } else "الآن"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppColors.Line),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.txType.arabicLabel, style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp))
                                if (entry.notes.isNotBlank()) {
                                    Text(entry.notes, style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp))
                                }
                                Text(timeLabel, style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (entry.cashDeltaPiasters != 0L) {
                                    val sign = if (entry.cashDeltaPiasters > 0) "+" else ""
                                    Text(
                                        "$sign${MoneyUtils.piastersToEgpString(entry.cashDeltaPiasters)} ج.م",
                                        style = AppText.body.copy(
                                            color = if (entry.cashDeltaPiasters > 0) AppColors.Trust else AppColors.Alert,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        ),
                                    )
                                }
                                if (entry.goldDeltaMg != 0L) {
                                    val sign = if (entry.goldDeltaMg > 0) "+" else ""
                                    Text(
                                        "$sign${MoneyUtils.mgToGramsString(entry.goldDeltaMg)}",
                                        style = AppText.body.copy(
                                            color = AppColors.Brass,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                        ),
                                    )
                                }
                                if (entry.pendingCashDeltaPiasters != 0L) {
                                    val sign = if (entry.pendingCashDeltaPiasters > 0) "+" else ""
                                    Text(
                                        "معلق: $sign${MoneyUtils.piastersToEgpString(entry.pendingCashDeltaPiasters)} ج.م",
                                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBuySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedKarat by remember { mutableIntStateOf(24) }
        var selectedMg by remember { mutableLongStateOf(250L) }
        var customMgInput by remember { mutableStateOf("250") }
        var payWithWallet by remember { mutableStateOf(true) }

        val unitPricePiasters = uiState.effectivePurchaseGramPiasters(selectedKarat)
        val estimatedTotalPiasters = MoneyUtils.computeFractionalCostPiasters(selectedMg, unitPricePiasters)

        var sheetError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = {
                sheetError = null
                onClearMessages()
                showBuySheet = false
            },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("شراء ذهب مجزأ (ملليجرام)", style = AppText.display.copy(fontSize = 19.sp))
                Text(
                    "اشترِ حصص ذهب حقيقية بدءاً من 100 ملجم وأضفها فوراً لخزنتك الرقمية.",
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                )

                val visibleBuyError = sheetError ?: uiState.actionErrorMessage
                if (visibleBuyError != null) {
                    Text(
                        text = visibleBuyError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.AlertSoft)
                            .padding(10.dp)
                            .testTag("vault_buy_sheet_error"),
                        style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 21, 18).forEach { k ->
                        FilterChip(
                            selected = selectedKarat == k,
                            onClick = {
                                selectedKarat = k
                                sheetError = null
                            },
                            label = {
                                Text(
                                    "عيار $k (${MoneyUtils.piastersToEgpString(uiState.effectivePurchaseGramPiasters(k))} ج.م/جم)",
                                    style = AppText.body.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Ink,
                                selectedLabelColor = AppColors.BrassLight,
                            ),
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100L to "100 ملجم", 250L to "250 ملجم", 500L to "0.5 جم", 1000L to "1.0 جم").forEach { (mg, lbl) ->
                        FilterChip(
                            selected = selectedMg == mg,
                            onClick = {
                                selectedMg = mg
                                customMgInput = mg.toString()
                                sheetError = null
                            },
                            label = { Text(lbl, style = AppText.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)) },
                        )
                    }
                }

                OutlinedTextField(
                    value = customMgInput,
                    onValueChange = { txt ->
                        customMgInput = txt.filter { it.isDigit() }
                        selectedMg = customMgInput.toLongOrNull() ?: 0L
                        sheetError = null
                    },
                    label = { Text("الوزن المطلوب بالملليجرام (1000 ملجم = 1 جم)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Brass),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = payWithWallet,
                        onClick = {
                            payWithWallet = true
                            sheetError = null
                        },
                        label = {
                            Text(
                                "دفع فوري بالمحفظة (${MoneyUtils.piastersToEgpString(wallet.cashAvailablePiasters)} ج.م)",
                                style = AppText.body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                            )
                        },
                    )
                    FilterChip(
                        selected = !payWithWallet,
                        onClick = {
                            payWithWallet = false
                            sheetError = null
                        },
                        label = { Text("طلب معلق لمراجعة الإدارة", style = AppText.body.copy(fontSize = 11.5.sp)) },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Parchment)
                        .padding(12.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("التكلفة الإجمالية (${MoneyUtils.mgToGramsString(selectedMg)})", style = AppText.body.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "${MoneyUtils.piastersToEgpString(estimatedTotalPiasters)} ج.م",
                            style = AppText.display.copy(color = AppColors.Brass, fontSize = 17.sp),
                        )
                    }
                }

                Button(
                    onClick = {
                        when {
                            selectedMg <= 0L -> sheetError = "الرجاء إدخال وزن صحيح أكبر من 0 ملجم."
                            payWithWallet && wallet.cashAvailablePiasters < estimatedTotalPiasters ->
                                sheetError = "رصيد المحفظة النقدية غير كافٍ (${MoneyUtils.piastersToEgpString(estimatedTotalPiasters)} ج.م مطلوب)."
                            else -> {
                                sheetError = null
                                onBuyFractionalGold(selectedKarat, selectedMg, payWithWallet) {
                                    showBuySheet = false
                                }
                            }
                        }
                    },
                    enabled = selectedMg > 0L && !uiState.isActionBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_buy_fractional_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
                ) {
                    Text("تأكيد شراء الذهب", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showWithdrawSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val initialKarat = when {
            wallet.gold24kMg >= 1000L -> 24
            wallet.gold21kMg >= 1000L -> 21
            else -> 18
        }
        var selectedKarat by remember { mutableIntStateOf(initialKarat) }
        var mgInput by remember { mutableStateOf("1000") }
        var deliveryType by remember { mutableStateOf(WithdrawalDeliveryType.PICKUP_HUB) }
        var addressInput by remember { mutableStateOf("فرع King Gold الرئيسي — المعادي") }
        var sheetError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = {
                sheetError = null
                onClearMessages()
                showWithdrawSheet = false
            },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("طلب سحب ذهب فعلي (بحد أدنى 1000 ملجم)", style = AppText.display.copy(fontSize = 18.sp))

                val visibleWithdrawError = sheetError ?: uiState.actionErrorMessage
                if (visibleWithdrawError != null) {
                    Text(
                        text = visibleWithdrawError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.AlertSoft)
                            .padding(10.dp)
                            .testTag("vault_withdraw_sheet_error"),
                        style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 21, 18).forEach { k ->
                        val held = wallet.confirmedMgForKarat(k)
                        FilterChip(
                            selected = selectedKarat == k,
                            onClick = {
                                selectedKarat = k
                                sheetError = null
                            },
                            enabled = held >= MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG,
                            label = { Text("عيار $k ($held ملجم)") },
                        )
                    }
                }
                OutlinedTextField(
                    value = mgInput,
                    onValueChange = {
                        mgInput = it.filter { ch -> ch.isDigit() }
                        sheetError = null
                    },
                    label = { Text("الوزن بالملليجرام (الحد الأدنى 1000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WithdrawalDeliveryType.entries.forEach { dt ->
                        FilterChip(
                            selected = deliveryType == dt,
                            onClick = { deliveryType = dt },
                            label = { Text(dt.arabicLabel, style = AppText.body.copy(fontSize = 12.sp)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = {
                        addressInput = it
                        sheetError = null
                    },
                    label = { Text("الفرع أو عنوان التوصيل المؤمّن") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        val amt = mgInput.toLongOrNull() ?: 0L
                        val karatHeld = wallet.confirmedMgForKarat(selectedKarat)
                        when {
                            amt < MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG ->
                                sheetError = "الحد الأدنى لاستلام الذهب الفعلي هو 1.000 جرام (1000 ملجم)."
                            wallet.goldAvailableMg < amt || karatHeld < amt ->
                                sheetError = "رصيد عيار $selectedKarat المتاح غير كافٍ لسحب ${MoneyUtils.mgToGramsString(amt)}."
                            addressInput.trim().length < 4 ->
                                sheetError = "يرجى إدخال عنوان التوصيل أو اسم فرع الاستلام بوضوح."
                            else -> {
                                sheetError = null
                                onRequestWithdrawal(selectedKarat, amt, deliveryType, addressInput) {
                                    showWithdrawSheet = false
                                }
                            }
                        }
                    },
                    enabled = (mgInput.toLongOrNull() ?: 0L) >= MoneyUtils.MIN_PHYSICAL_WITHDRAWAL_MG && !uiState.isActionBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_physical_withdrawal_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Ink),
                ) {
                    Text("تأكيد حجز السحب الفعلي", style = AppText.body.copy(color = AppColors.BrassLight, fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showDepositSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var amountEgpText by remember { mutableStateOf("5000") }
        var selectedChannel by remember { mutableStateOf("InstaPay") }
        var refText by remember { mutableStateOf("") }
        var sheetError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = {
                sheetError = null
                onClearMessages()
                showDepositSheet = false
            },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("شحن المحفظة النقدية", style = AppText.display.copy(fontSize = 19.sp))
                Text(
                    "أدخل بيانات التحويل ليتم اعتماد المبلغ من إدارة المنصة وإضافته لمحفظتك.",
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                )

                val visibleDepositError = sheetError ?: uiState.actionErrorMessage
                if (visibleDepositError != null) {
                    Text(
                        text = visibleDepositError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.AlertSoft)
                            .padding(10.dp)
                            .testTag("vault_deposit_sheet_error"),
                        style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("InstaPay", "تحويل بنكي", "Vodafone Cash").forEach { ch ->
                        FilterChip(
                            selected = selectedChannel == ch,
                            onClick = { selectedChannel = ch },
                            label = { Text(ch, style = AppText.body.copy(fontSize = 12.sp)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = amountEgpText,
                    onValueChange = {
                        amountEgpText = it
                        sheetError = null
                    },
                    label = { Text("المبلغ بالجنيه المصري (بحد أدنى 10 ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input"),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = refText,
                    onValueChange = {
                        refText = it
                        sheetError = null
                    },
                    label = { Text("رقم المرجع / العملية") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_reference_input"),
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val piasters = MoneyUtils.egpStringToPiasters(amountEgpText) ?: 0L
                        when {
                            piasters < 1000L ->
                                sheetError = "الحد الأدنى لطلب الإيداع هو 10.00 ج.م."
                            refText.trim().length < 3 ->
                                sheetError = "يرجى إدخال مرجع التحويل المالي."
                            else -> {
                                sheetError = null
                                onSubmitDepositRequest(piasters, selectedChannel, refText) {
                                    showDepositSheet = false
                                }
                            }
                        }
                    },
                    enabled = !uiState.isActionBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_deposit_request_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
                ) {
                    Text("إرسال طلب الشحن للمراجعة", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun VaultStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(label, style = AppText.body.copy(color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp))
        Text(value, style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp))
    }
}

@Composable
private fun KaratBreakdownVisual(mg24: Long, mg21: Long, mg18: Long) {
    val total = (mg24 + mg21 + mg18).coerceAtLeast(1L).toFloat()
    val w24 = mg24 / total
    val w21 = mg21 / total
    val w18 = mg18 / total
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        ) {
            if (mg24 + mg21 + mg18 == 0L) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    cornerRadius = CornerRadius(8f, 8f),
                )
            } else {
                var x = 0f
                val bars = listOf(
                    w24 to AppColors.BrassLight,
                    w21 to AppColors.Brass,
                    w18 to AppColors.LiveGainDark,
                )
                bars.forEach { (ratio, color) ->
                    val segW = size.width * ratio
                    if (segW > 0f) {
                        drawRect(color = color, topLeft = Offset(x, 0f), size = Size(segW, size.height))
                        x += segW
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("24K: ${MoneyUtils.mgToGramsString(mg24)}", style = AppText.body.copy(color = AppColors.BrassLight, fontSize = 10.5.sp))
            Text("21K: ${MoneyUtils.mgToGramsString(mg21)}", style = AppText.body.copy(color = Color.White.copy(alpha = 0.85f), fontSize = 10.5.sp))
            Text("18K: ${MoneyUtils.mgToGramsString(mg18)}", style = AppText.body.copy(color = AppColors.LiveGainDark, fontSize = 10.5.sp))
        }
    }
}
