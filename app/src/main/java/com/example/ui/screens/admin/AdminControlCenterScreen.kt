package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.DepositRequest
import com.example.model.DepositStatus
import com.example.model.FractionalGoldOrder
import com.example.model.MerchantProduct
import com.example.model.MerchantProductStatus
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.model.MoneyUtils
import com.example.model.PlatformPricingConfig
import com.example.model.SellGoldRequest
import com.example.model.SellRequestStatus
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlCenterScreen(
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onLoadMerchantDetails: (String) -> Unit,
    onReviewMerchant: (MerchantStore, MerchantStatus, Boolean, Long, String) -> Unit,
    onReviewProduct: (MerchantProduct, MerchantProductStatus, String) -> Unit,
    onEvaluateSellGold: (SellGoldRequest, Long, Int, Long, Long, String) -> Unit,
    onRejectSellGold: (SellGoldRequest, String) -> Unit,
    onProcessDeposit: (DepositRequest, Boolean, String) -> Unit,
    onProcessWithdrawal: (WithdrawalRequest, WithdrawalStatus, String) -> Unit,
    onProcessFractional: (FractionalGoldOrder, Boolean) -> Unit,
    onUpdatePricing: (PlatformPricingConfig) -> Unit,
    onClearMessages: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var inspectingMerchant by remember { mutableStateOf<MerchantStore?>(null) }
    var evaluatingSellRequest by remember { mutableStateOf<SellGoldRequest?>(null) }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = { Text("مركز إدارة المنصة", style = AppText.display.copy(fontSize = 20.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = AppColors.Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Parchment),
            )
        },
    ) { padding ->
        if (!uiState.isAdmin) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "عذراً، هذه اللوحة مخصصة لمدير المنصة المعتمد فقط.",
                    style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold),
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pendingMerchants = uiState.allMerchantsAdmin.count { it.status == MerchantStatus.PENDING }
            val pendingProds = uiState.allProductsAdmin.count { it.status == MerchantProductStatus.PENDING_REVIEW }
            val pendingSells = uiState.allSellRequestsAdmin.count { it.status == SellRequestStatus.SUBMITTED || it.status == SellRequestStatus.UNDER_REVIEW }
            val pendingFin = uiState.allDepositsAdmin.count { it.status == DepositStatus.PENDING } +
                uiState.allWithdrawalsAdmin.count { it.status == WithdrawalStatus.PENDING || it.status == WithdrawalStatus.APPROVED } +
                uiState.allFractionalOrdersAdmin.count { it.status == "PENDING_PAYMENT" }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = AppColors.Parchment,
                contentColor = AppColors.Brass,
                edgePadding = 12.dp,
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("التجار ($pendingMerchants)") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("المنتجات ($pendingProds)") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("شراء الذهب ($pendingSells)") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("المالية ($pendingFin)") })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("الأسعار والسجل") })
            }

            if (uiState.actionBannerMessage != null || uiState.actionErrorMessage != null) {
                val isErr = uiState.actionErrorMessage != null
                Row(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isErr) AppColors.AlertSoft else AppColors.TrustSoft)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        uiState.actionErrorMessage ?: uiState.actionBannerMessage.orEmpty(),
                        modifier = Modifier.weight(1f),
                        style = AppText.body.copy(color = if (isErr) AppColors.Alert else AppColors.Trust, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    )
                    TextButton(onClick = onClearMessages) { Text("حسناً") }
                }
            }

            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (selectedTab) {
                    0 -> {
                        val sorted = uiState.allMerchantsAdmin.sortedBy { if (it.status == MerchantStatus.PENDING) 0 else 1 }
                        if (sorted.isEmpty()) {
                            item { EmptyAdminNotice("لا توجد طلبات تجار حالياً.") }
                        } else {
                            items(sorted, key = { it.id }) { m ->
                                AdminRowCard(
                                    title = m.shopName,
                                    badge = m.status.arabicLabel,
                                    subtitle = "${m.area} · ${m.contactPhone} · ${m.verifiedEmail}",
                                ) {
                                    Button(
                                        onClick = {
                                            onLoadMerchantDetails(m.id)
                                            inspectingMerchant = m
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Ink),
                                    ) {
                                        Text("فحص المستندات واتخاذ قرار", color = AppColors.BrassLight, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        val sorted = uiState.allProductsAdmin.sortedBy { if (it.status == MerchantProductStatus.PENDING_REVIEW) 0 else 1 }
                        if (sorted.isEmpty()) {
                            item { EmptyAdminNotice("لا توجد منتجات للتجار حالياً.") }
                        } else {
                            items(sorted, key = { it.id }) { p ->
                                AdminRowCard(
                                    title = "${p.name} (${p.merchantName})",
                                    badge = p.status.arabicLabel,
                                    subtitle = "عيار ${p.karat} · ${p.weightGrams} جم · السعر النهائي: ${MoneyUtils.piastersToEgpString(p.sellingPricePiasters)} ج.م · مخزون ${p.stockQuantity}",
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onReviewProduct(p, MerchantProductStatus.ACTIVE, "موافقة الإدارة") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Trust),
                                        ) {
                                            Text("اعتماد", color = Color.White, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { onReviewProduct(p, MerchantProductStatus.REJECTED, "يرجى مراجعة المواصفات والصور") },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("رفض", color = AppColors.Alert, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        val sorted = uiState.allSellRequestsAdmin.sortedBy { if (it.status == SellRequestStatus.SUBMITTED) 0 else 1 }
                        if (sorted.isEmpty()) {
                            item { EmptyAdminNotice("لا توجد طلبات بيع ذهب من العملاء.") }
                        } else {
                            items(sorted, key = { it.id }) { req ->
                                AdminRowCard(
                                    title = "${req.itemTitle} — عيار ${req.karat}",
                                    badge = req.status.arabicLabel,
                                    subtitle = "العميل: ${req.customerEmail.ifBlank { req.customerUid }} · الوزن المبدئي ${MoneyUtils.mgToGramsString(req.estimatedWeightMg)} · النهائي ${MoneyUtils.piastersToEgpString(req.finalOfferPiasters)} ج.م",
                                ) {
                                    if (req.status == SellRequestStatus.SUBMITTED || req.status == SellRequestStatus.UNDER_REVIEW) {
                                        Button(
                                            onClick = { evaluatingSellRequest = req },
                                            modifier = Modifier.testTag("admin_evaluate_sell_${req.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
                                        ) {
                                            Text("تقييم يدوي وإرسال العرض", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        item { Text("طلبات الإيداع النقدي", style = AppText.display.copy(fontSize = 16.sp)) }
                        items(uiState.allDepositsAdmin, key = { "dep_${it.id}" }) { dep ->
                            AdminRowCard(
                                title = "إيداع ${MoneyUtils.piastersToEgpString(dep.amountPiasters)} ج.م (${dep.channel})",
                                badge = dep.status.arabicLabel,
                                subtitle = "${dep.customerEmail} · مرجع: ${dep.paymentReference}",
                            ) {
                                if (dep.status == DepositStatus.PENDING) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onProcessDeposit(dep, true, "معتمد") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Trust)) {
                                            Text("اعتماد", color = Color.White, fontSize = 12.sp)
                                        }
                                        OutlinedButton(onClick = { onProcessDeposit(dep, false, "مرجع غير مطابق") }, modifier = Modifier.weight(1f)) {
                                            Text("رفض", color = AppColors.Alert, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("طلبات السحب الفعلي للذهب (>= 1000 ملجم)", style = AppText.display.copy(fontSize = 16.sp))
                        }
                        items(uiState.allWithdrawalsAdmin, key = { "wd_${it.id}" }) { wd ->
                            AdminRowCard(
                                title = "${wd.withdrawalRef} — عيار ${wd.karat} (${MoneyUtils.mgToGramsString(wd.amountMg)})",
                                badge = wd.status.arabicLabel,
                                subtitle = "${wd.customerEmail} · ${wd.deliveryType.arabicLabel} (${wd.deliveryAddress})",
                            ) {
                                if (wd.status == WithdrawalStatus.PENDING || wd.status == WithdrawalStatus.APPROVED) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (wd.status == WithdrawalStatus.PENDING) {
                                            Button(onClick = { onProcessWithdrawal(wd, WithdrawalStatus.APPROVED, "تم اعتماد السحب وجاري التجهيز") }, modifier = Modifier.weight(1f)) {
                                                Text("موافقة", fontSize = 11.5.sp)
                                            }
                                        }
                                        Button(
                                            onClick = { onProcessWithdrawal(wd, WithdrawalStatus.FULFILLED, "تم التسليم الفعلي للعميل") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Trust),
                                        ) {
                                            Text("تأكيد التسليم", color = Color.White, fontSize = 11.5.sp)
                                        }
                                        OutlinedButton(onClick = { onProcessWithdrawal(wd, WithdrawalStatus.REJECTED, "إلغاء وفك الحجز") }, modifier = Modifier.weight(1f)) {
                                            Text("رفض", color = AppColors.Alert, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                        }

                        val pendingFractional = uiState.allFractionalOrdersAdmin.filter { it.status == "PENDING_PAYMENT" }
                        if (pendingFractional.isNotEmpty()) {
                            item { Text("مشتريات ذهب مجزأ معلقة", style = AppText.display.copy(fontSize = 16.sp)) }
                            items(pendingFractional, key = { "frac_${it.id}" }) { fr ->
                                AdminRowCard(
                                    title = "${MoneyUtils.mgToGramsString(fr.weightMg)} عيار ${fr.karat}",
                                    badge = fr.status,
                                    subtitle = "المبلغ: ${MoneyUtils.piastersToEgpString(fr.totalPiasters)} ج.م",
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onProcessFractional(fr, true) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Trust)) {
                                            Text("تأكيد السداد", color = Color.White, fontSize = 12.sp)
                                        }
                                        OutlinedButton(onClick = { onProcessFractional(fr, false) }, modifier = Modifier.weight(1f)) {
                                            Text("رفض", color = AppColors.Alert, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        item {
                            PricingEditorCard(
                                uiState = uiState,
                                onSave = onUpdatePricing,
                            )
                        }
                        item { Text("سجل تدقيق المنصة (${uiState.auditLogsAdmin.size})", style = AppText.display.copy(fontSize = 16.sp)) }
                        items(uiState.auditLogsAdmin.take(30), key = { it.id }) { log ->
                            AdminRowCard(title = log.action, badge = log.actorRole, subtitle = log.details)
                        }
                    }
                }
            }
        }
    }

    inspectingMerchant?.let { merchant ->
        MerchantInspectionDialog(
            merchant = merchant,
            docs = uiState.selectedMerchantDocs,
            auditLogs = uiState.selectedMerchantAuditLogs,
            onDismiss = { inspectingMerchant = null },
            onReview = { status, prepaid, budget, note ->
                onReviewMerchant(merchant, status, prepaid, budget, note)
                inspectingMerchant = null
            },
        )
    }

    evaluatingSellRequest?.let { req ->
        SellGoldEvaluationDialog(
            request = req,
            defaultRefPricePiastersForKarat = { k -> uiState.effectiveValuationGramPiasters(k) },
            onDismiss = { evaluatingSellRequest = null },
            onSendOffer = { verifiedMg, karat, refPiasters, dedPiasters, notes ->
                onEvaluateSellGold(req, verifiedMg, karat, refPiasters, dedPiasters, notes)
                evaluatingSellRequest = null
            },
            onReject = { reason ->
                onRejectSellGold(req, reason)
                evaluatingSellRequest = null
            },
        )
    }
}

@Composable
private fun AdminRowCard(
    title: String,
    badge: String,
    subtitle: String,
    content: @Composable () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp), modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(6.dp))
                Text(badge, style = AppText.body.copy(color = AppColors.Brass, fontWeight = FontWeight.Bold, fontSize = 11.5.sp))
            }
            Text(subtitle, style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp))
            content()
        }
    }
}

@Composable
private fun EmptyAdminNotice(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(msg, style = AppText.body.copy(color = AppColors.Muted))
    }
}

@Composable
private fun PricingEditorCard(
    uiState: KingGoldUiState,
    onSave: (PlatformPricingConfig) -> Unit,
) {
    val cfg = uiState.platformPricing
    var r24 by remember(cfg) { mutableStateOf(MoneyUtils.piastersToEgpString(uiState.effectivePurchaseGramPiasters(24))) }
    var r21 by remember(cfg) { mutableStateOf(MoneyUtils.piastersToEgpString(uiState.effectivePurchaseGramPiasters(21))) }
    var r18 by remember(cfg) { mutableStateOf(MoneyUtils.piastersToEgpString(uiState.effectivePurchaseGramPiasters(18))) }
    var policyNote by remember(cfg) { mutableStateOf(cfg.merchantPolicyNote) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("إعدادات تسعير المنصة (ج.م / جم)", style = AppText.display.copy(fontSize = 16.sp))
            OutlinedTextField(value = r24, onValueChange = { r24 = it }, label = { Text("عيار 24") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = r21, onValueChange = { r21 = it }, label = { Text("عيار 21") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = r18, onValueChange = { r18 = it }, label = { Text("عيار 18") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = policyNote, onValueChange = { policyNote = it }, label = { Text("سياسة المصنعية") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val p24 = MoneyUtils.egpStringToPiasters(r24) ?: cfg.reference24kPerGramPiasters
                    val p21 = MoneyUtils.egpStringToPiasters(r21) ?: cfg.reference21kPerGramPiasters
                    val p18 = MoneyUtils.egpStringToPiasters(r18) ?: cfg.reference18kPerGramPiasters
                    onSave(
                        PlatformPricingConfig(
                            reference24kPerGramPiasters = p24,
                            reference21kPerGramPiasters = p21,
                            reference18kPerGramPiasters = p18,
                            valuation24kPerGramPiasters = p24,
                            valuation21kPerGramPiasters = p21,
                            valuation18kPerGramPiasters = p18,
                            merchantPolicyNote = policyNote,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
            ) {
                Text("حفظ التسعير المرجعي", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
