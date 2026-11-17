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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.MerchantPrivateDocs
import com.example.model.MerchantStore
import com.example.ui.state.AdminApprovalUiState
import com.example.ui.state.AdminApprovalViewModel
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.Base64ThumbnailImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalScreen(
    viewModel: AdminApprovalViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdminApprovalContent(
        uiState = uiState,
        onBack = onBack,
        onLoadKycDocs = viewModel::loadMerchantKycDocs,
        onApproveMerchant = { merchant -> viewModel.approveMerchant(merchant) },
        onRejectMerchant = viewModel::rejectMerchant,
        onClearMessages = viewModel::clearMessages,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApprovalContent(
    uiState: AdminApprovalUiState,
    onBack: () -> Unit,
    onLoadKycDocs: (String) -> Unit,
    onApproveMerchant: (MerchantStore) -> Unit,
    onRejectMerchant: (MerchantStore, String) -> Unit,
    onClearMessages: () -> Unit,
) {
    var rejectingMerchant by remember { mutableStateOf<MerchantStore?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.admin_approval_title),
                        style = AppText.display.copy(fontSize = 20.sp),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_approval_back_button"),
                    ) {
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
            if (!uiState.isAdmin) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppColors.Line),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AdminPanelSettings,
                                contentDescription = null,
                                tint = AppColors.Alert,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                text = "هذا القسم مخصص لحسابات الإدارة المعتمدة فقط.",
                                style = AppText.body.copy(
                                    color = AppColors.Alert,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.Ink,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AdminPanelSettings,
                                contentDescription = null,
                                tint = AppColors.BrassLight,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "طلبات اعتماد التجار (PENDING)",
                                    style = AppText.display.copy(color = AppColors.BrassLight, fontSize = 17.sp),
                                )
                                Text(
                                    text = "عدد الطلبات قيد المراجعة في قاعدة merchants: ${uiState.pendingMerchants.size}",
                                    style = AppText.body.copy(color = Color.White, fontSize = 12.sp),
                                    modifier = Modifier.testTag("admin_approval_pending_count"),
                                )
                            }
                        }
                    }
                }

                if (uiState.bannerMessage != null || uiState.errorMessage != null) {
                    item {
                        val isError = uiState.errorMessage != null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isError) AppColors.AlertSoft else AppColors.TrustSoft)
                                .padding(12.dp)
                                .testTag("admin_approval_feedback_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) AppColors.Alert else AppColors.Trust,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: uiState.bannerMessage.orEmpty(),
                                modifier = Modifier.weight(1f),
                                style = AppText.body.copy(
                                    color = if (isError) AppColors.Alert else AppColors.Trust,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                ),
                            )
                            TextButton(onClick = onClearMessages) {
                                Text("حسناً")
                            }
                        }
                    }
                }

                when {
                    uiState.isLoading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = AppColors.Brass)
                            }
                        }
                    }

                    uiState.pendingMerchants.isEmpty() -> {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_approval_empty_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, AppColors.Line),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.VerifiedUser,
                                        contentDescription = null,
                                        tint = AppColors.Trust,
                                        modifier = Modifier.size(34.dp),
                                    )
                                    Text(
                                        text = "لا توجد طلبات تجار معلّقة حالياً",
                                        style = AppText.display.copy(fontSize = 16.sp),
                                    )
                                    Text(
                                        text = "ستظهر هنا أي طلبات جديدة بحالة PENDING فور إرسالها.",
                                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        items(uiState.pendingMerchants, key = { it.id }) { merchant ->
                            val isProcessing = uiState.processingMerchantIds.contains(merchant.id)
                            val docs = uiState.merchantDocsById[merchant.id]
                            val isLoadingDocs = uiState.loadingDocsMerchantIds.contains(merchant.id)

                            PendingMerchantApprovalCard(
                                merchant = merchant,
                                kycDocs = docs,
                                isLoadingDocs = isLoadingDocs,
                                isProcessing = isProcessing,
                                onInspectKyc = { onLoadKycDocs(merchant.id) },
                                onApprove = { onApproveMerchant(merchant) },
                                onOpenRejectDialog = {
                                    rejectingMerchant = merchant
                                    rejectReason = ""
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    val targetReject = rejectingMerchant
    if (targetReject != null) {
        AlertDialog(
            onDismissRequest = { rejectingMerchant = null },
            containerColor = AppColors.Parchment,
            title = {
                Text(
                    text = "رفض طلب «${targetReject.shopName}»",
                    style = AppText.display.copy(fontSize = 18.sp),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "يرجى كتابة سبب الرفض ليظهر للتاجر في سجل المراجعة:",
                        style = AppText.body.copy(fontSize = 13.sp),
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("سبب الرفض *") },
                        placeholder = { Text("مثال: السجل التجاري غير واضح أو بيانات ناقصة") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_reject_reason_input"),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reasonToSend = rejectReason.trim()
                        rejectingMerchant = null
                        onRejectMerchant(targetReject, reasonToSend)
                    },
                    enabled = rejectReason.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Alert),
                    modifier = Modifier.testTag("admin_confirm_reject_button"),
                ) {
                    Text("تأكيد الرفض", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingMerchant = null }) {
                    Text("إلغاء")
                }
            },
        )
    }
}

@Composable
private fun PendingMerchantApprovalCard(
    merchant: MerchantStore,
    kycDocs: MerchantPrivateDocs?,
    isLoadingDocs: Boolean,
    isProcessing: Boolean,
    onInspectKyc: () -> Unit,
    onApprove: () -> Unit,
    onOpenRejectDialog: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_merchant_card_${merchant.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = AppColors.Brass)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = merchant.shopName,
                        style = AppText.display.copy(fontSize = 17.sp),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppColors.ParchmentDeep,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HourglassTop,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = merchant.status.arabicLabel,
                            style = AppText.body.copy(
                                color = AppColors.Brass,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }

            MerchantInfoRow(Icons.Outlined.LocationOn, "${merchant.area} — ${merchant.address}")
            MerchantInfoRow(Icons.Outlined.Phone, merchant.contactPhone)
            MerchantInfoRow(Icons.Outlined.Email, merchant.verifiedEmail)

            if (merchant.description.isNotBlank()) {
                Text(
                    text = merchant.description,
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                )
            }

            if (kycDocs == null) {
                OutlinedButton(
                    onClick = onInspectKyc,
                    enabled = !isLoadingDocs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("inspect_pending_merchant_${merchant.id}"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (isLoadingDocs) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Outlined.Badge, contentDescription = null, tint = AppColors.Brass)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("عرض الاسم القانوني والرقم التجاري (KYC)")
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Parchment,
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "بيانات التحقق القانونية (KYC)",
                            style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp),
                        )
                        Text("الاسم القانوني: ${kycDocs.legalFullName}", style = AppText.body.copy(fontSize = 12.sp))
                        Text("الهوية التجارية / القومي: ${kycDocs.nationalIdNumber}", style = AppText.body.copy(fontSize = 12.sp))
                        if (kycDocs.commercialRegNumber.isNotBlank()) {
                            Text("السجل التجاري: ${kycDocs.commercialRegNumber}", style = AppText.body.copy(fontSize = 12.sp))
                        }
                        if (kycDocs.taxCardNumber.isNotBlank()) {
                            Text("البطاقة الضريبية: ${kycDocs.taxCardNumber}", style = AppText.body.copy(fontSize = 12.sp))
                        }
                        if (kycDocs.authorizationNotes.isNotBlank()) {
                            Text("ملاحظات التفويض: ${kycDocs.authorizationNotes}", style = AppText.body.copy(fontSize = 12.sp))
                        }
                        if (kycDocs.documentThumbnails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(kycDocs.documentThumbnails) { b64 ->
                                    Base64ThumbnailImage(
                                        encoded = b64,
                                        contentDescription = "مستند تاجر",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = AppColors.Line)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onApprove,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("approve_merchant_${merchant.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Trust),
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اعتماد", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onOpenRejectDialog,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("reject_merchant_${merchant.id}"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppColors.Alert),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = AppColors.Alert, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("رفض", color = AppColors.Alert, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MerchantInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AppColors.Muted, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = AppText.body.copy(fontSize = 12.5.sp))
    }
}
