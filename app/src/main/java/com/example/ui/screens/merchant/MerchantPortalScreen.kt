package com.example.ui.screens.merchant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MerchantProduct
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.model.MoneyUtils
import com.example.model.OrderStatus
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.Base64ThumbnailImage
import com.example.ui.util.ImageThumbnailCodec
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantPortalScreen(
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onLoadMerchantDetails: (String) -> Unit,
    onSubmitRegistration: (
        shopName: String,
        area: String,
        address: String,
        contactPhone: String,
        verifiedEmail: String,
        description: String,
        legalFullName: String,
        nationalIdNumber: String,
        commercialRegNumber: String,
        taxCardNumber: String,
        authorizationNotes: String,
        documentThumbnails: List<String>,
    ) -> Unit,
    onSaveProduct: (
        merchant: MerchantStore,
        existingProduct: MerchantProduct?,
        name: String,
        description: String,
        category: String,
        karat: Int,
        weightGrams: Double,
        sellingPricePiasters: Long,
        stockQuantity: Int,
        stamped: Boolean,
        imageThumbnails: List<String>,
        onSuccess: () -> Unit,
    ) -> Unit,
    onToggleProductPause: (MerchantProduct) -> Unit,
    onUpdateOrderStatus: (orderId: String, nextStatus: OrderStatus) -> Unit,
    onClearMessages: () -> Unit,
    onOpenDedicatedRegistration: (() -> Unit)? = null,
) {
    val activeMerchant = remember(uiState.ownMerchants) {
        uiState.ownMerchants.firstOrNull { it.status == MerchantStatus.APPROVED }
            ?: uiState.ownMerchants.firstOrNull()
    }
    var forceNewApplication by remember { mutableStateOf(false) }

    LaunchedEffect(activeMerchant?.id) {
        if (activeMerchant != null) {
            onLoadMerchantDetails(activeMerchant.id)
        }
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text("بوابة التاجر", style = AppText.display.copy(fontSize = 20.sp))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = AppColors.Ink)
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
            when {
                activeMerchant == null || forceNewApplication -> {
                    MerchantRegistrationForm(
                        defaultEmail = uiState.userEmail,
                        uiState = uiState,
                        onSubmit = { shop, area, addr, phone, email, desc, legal, nid, comm, tax, authNotes, docs ->
                            onSubmitRegistration(shop, area, addr, phone, email, desc, legal, nid, comm, tax, authNotes, docs)
                            forceNewApplication = false
                        },
                        onClearMessages = onClearMessages,
                    )
                }
                activeMerchant.status != MerchantStatus.APPROVED -> {
                    MerchantVerificationStatusView(
                        merchant = activeMerchant,
                        uiState = uiState,
                        onReapply = {
                            if (onOpenDedicatedRegistration != null) {
                                onOpenDedicatedRegistration()
                            } else {
                                forceNewApplication = true
                            }
                        },
                    )
                }
                else -> {
                    ApprovedMerchantDashboard(
                        merchant = activeMerchant,
                        uiState = uiState,
                        onSaveProduct = { m, existing, name, desc, cat, karat, w, pricePiasters, stock, stamped, thumbs, onSaved ->
                    onSaveProduct(m, existing, name, desc, cat, karat, w, pricePiasters, stock, stamped, thumbs, onSaved)
                },
                        onToggleProductPause = onToggleProductPause,
                        onUpdateOrderStatus = onUpdateOrderStatus,
                        onClearMessages = onClearMessages,
                    )
                }
            }
        }
    }
}

@Composable
private fun MerchantRegistrationForm(
    defaultEmail: String,
    uiState: KingGoldUiState,
    onSubmit: (
        shopName: String,
        area: String,
        address: String,
        contactPhone: String,
        verifiedEmail: String,
        description: String,
        legalFullName: String,
        nationalIdNumber: String,
        commercialRegNumber: String,
        taxCardNumber: String,
        authorizationNotes: String,
        documentThumbnails: List<String>,
    ) -> Unit,
    onClearMessages: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shopName by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember(defaultEmail) { mutableStateOf(defaultEmail) }
    var description by remember { mutableStateOf("") }
    var legalName by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var commReg by remember { mutableStateOf("") }
    var taxCard by remember { mutableStateOf("") }
    var authNotes by remember { mutableStateOf("") }
    var docs by remember { mutableStateOf<List<String>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                docs = uris.take(4).mapNotNull { ImageThumbnailCodec.encodeUriToBase64Thumbnail(context, it) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.Ink,
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Storefront, null, tint = AppColors.BrassLight)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "افتح متجرك الموثّق على King Gold. يظل حسابك قيد المراجعة حتى اعتماد المستندات من الإدارة، وتُعرض أسعارك نهائية بدون مصنعية إضافية على المشتري.",
                        style = AppText.body.copy(color = Color.White, fontSize = 12.sp),
                    )
                }
            }
        }

        if (uiState.actionErrorMessage != null || uiState.actionBannerMessage != null) {
            item {
                val isErr = uiState.actionErrorMessage != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isErr) AppColors.AlertSoft else AppColors.TrustSoft)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.actionErrorMessage ?: uiState.actionBannerMessage.orEmpty(),
                        modifier = Modifier.weight(1f),
                        style = AppText.body.copy(color = if (isErr) AppColors.Alert else AppColors.Trust, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    )
                    TextButton(onClick = onClearMessages) { Text("حسناً") }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppColors.Line),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("بيانات المتجر والنشاط التجاري", style = AppText.display.copy(fontSize = 16.sp))
                    OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("اسم محل المجوهرات *") }, modifier = Modifier.fillMaxWidth().testTag("merchant_shop_name_input"), singleLine = true)
                    OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("المنطقة / المدينة *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان التفصيلي للمحل *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم هاتف التواصل *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني الموثّق *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("نبذة عن المتجر والتخصص") }, modifier = Modifier.fillMaxWidth())

                    Text("بيانات التحقق القانونية والمستندات", style = AppText.display.copy(fontSize = 16.sp))
                    OutlinedTextField(value = legalName, onValueChange = { legalName = it }, label = { Text("الاسم القانوني الكامل للمالك / المفوّض *") }, modifier = Modifier.fillMaxWidth().testTag("merchant_legal_name_input"), singleLine = true)
                    OutlinedTextField(value = nationalId, onValueChange = { nationalId = it }, label = { Text("الرقم القومي أو الهوية التجارية *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = commReg, onValueChange = { commReg = it }, label = { Text("رقم السجل التجاري") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = taxCard, onValueChange = { taxCard = it }, label = { Text("رقم البطاقة الضريبية") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = authNotes, onValueChange = { authNotes = it }, label = { Text("بيانات التفويض أو إثبات الملكية") }, modifier = Modifier.fillMaxWidth())

                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, null, tint = AppColors.Brass)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (docs.isEmpty()) "إرفاق صور المستندات (حتى 4)" else "تم إرفاق ${docs.size} مستندات")
                    }

                    if (docs.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(docs) { b64 ->
                                Base64ThumbnailImage(encoded = b64, contentDescription = "مستند تاجر", modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
                            }
                        }
                    }

                    Button(
                        onClick = { onSubmit(shopName, area, address, phone, email, description, legalName, nationalId, commReg, taxCard, authNotes, docs) },
                        enabled = !uiState.isActionBusy,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_merchant_registration_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
                    ) {
                        Text("إرسال طلب اعتماد التاجر", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantVerificationStatusView(
    merchant: MerchantStore,
    uiState: KingGoldUiState,
    onReapply: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppColors.Line),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(merchant.shopName, style = AppText.display.copy(fontSize = 20.sp))
                    Text(
                        "الحالة الحالية: ${merchant.status.arabicLabel}",
                        style = AppText.body.copy(
                            color = if (merchant.status == MerchantStatus.PENDING) AppColors.Brass else AppColors.Alert,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.testTag("merchant_status_badge"),
                    )
                    Text("${merchant.area} — ${merchant.address}", style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp))
                    if (!merchant.rejectionReason.isNullOrBlank()) {
                        Text(
                            "سبب القرار: ${merchant.rejectionReason}",
                            style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        )
                    }
                    if (merchant.status == MerchantStatus.REJECTED) {
                        Button(
                            onClick = onReapply,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Ink),
                        ) {
                            Text("تقديم طلب اعتماد جديد", color = AppColors.BrassLight)
                        }
                    }
                }
            }
        }

        if (uiState.selectedMerchantAuditLogs.isNotEmpty()) {
            item { Text("سجل مراجعة الحساب", style = AppText.display.copy(fontSize = 16.sp)) }
            items(uiState.selectedMerchantAuditLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(log.action, style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp))
                        Text(log.note, style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApprovedMerchantDashboard(
    merchant: MerchantStore,
    uiState: KingGoldUiState,
    onSaveProduct: (
        merchant: MerchantStore,
        existingProduct: MerchantProduct?,
        name: String,
        description: String,
        category: String,
        karat: Int,
        weightGrams: Double,
        sellingPricePiasters: Long,
        stockQuantity: Int,
        stamped: Boolean,
        imageThumbnails: List<String>,
        onSuccess: () -> Unit,
    ) -> Unit,
    onToggleProductPause: (MerchantProduct) -> Unit,
    onUpdateOrderStatus: (orderId: String, nextStatus: OrderStatus) -> Unit,
    onClearMessages: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<MerchantProduct?>(null) }
    var showProductSheet by remember { mutableStateOf(false) }

    val completedOrders = uiState.merchantOrders.filter { it.status == OrderStatus.COMPLETED }
    val pendingOrders = uiState.merchantOrders.filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.DISPUTED }
    val totalSalesPiasters = completedOrders.sumOf { it.totalPiasters }

    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize(),
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AppColors.Parchment,
            contentColor = AppColors.Brass,
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("نظرة عامة", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("المنتجات (${uiState.ownMerchantProducts.size})", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("الطلبات (${uiState.merchantOrders.size})", fontWeight = FontWeight.Bold) })
        }

        if (uiState.actionBannerMessage != null || uiState.actionErrorMessage != null) {
            val isErr = uiState.actionErrorMessage != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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

        when (selectedTab) {
            0 -> LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AppColors.Ink) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(merchant.shopName, style = AppText.display.copy(color = Color.White, fontSize = 20.sp))
                            Text("متجر معتمد · ${merchant.area}", style = AppText.body.copy(color = AppColors.BrassLight, fontSize = 12.5.sp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("إجمالي المبيعات المكتملة", style = AppText.body.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp))
                                    Text("${MoneyUtils.piastersToEgpString(totalSalesPiasters)} ج.م", style = AppText.display.copy(color = AppColors.LiveGainDark, fontSize = 20.sp))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("طلبات نشطة / مكتملة", style = AppText.body.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp))
                                    Text("${pendingOrders.size} نشط · ${completedOrders.size} مكتمل", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppColors.Line),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("سياسة تسوية المصنعية المسبقة (بدون مصنعية على العميل)", style = AppText.body.copy(fontWeight = FontWeight.ExtraBold))
                            Text(
                                if (merchant.prepaidMakingApproved) {
                                    "مفعّلة — الميزانية المخصصة: ${MoneyUtils.piastersToEgpString(merchant.prepaidMakingBudgetPiasters)} ج.م"
                                } else {
                                    "سياسة المنصة القياسية: تُدرج أسعار المنتجات شاملة دون أي مصنعية إضافية على المشتري."
                                },
                                style = AppText.body.copy(color = AppColors.Trust, fontSize = 12.5.sp),
                            )
                            val policyNote = merchant.pricingPolicyNote
                            if (!policyNote.isNullOrBlank()) {
                                Text(policyNote, style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp))
                            }
                        }
                    }
                }
            }

            1 -> LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Button(
                        onClick = {
                            editingProduct = null
                            showProductSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("merchant_add_product_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة منتج جديد لمتجرك", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }
                items(uiState.ownMerchantProducts, key = { it.id }) { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppColors.Line),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prod.name, style = AppText.body.copy(fontWeight = FontWeight.ExtraBold))
                                Text(prod.status.arabicLabel, style = AppText.body.copy(color = AppColors.Brass, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                            }
                            Text(
                                "عيار ${prod.karat} · ${prod.weightGrams} جم · المخزون: ${prod.stockQuantity} · السعر النهائي: ${MoneyUtils.piastersToEgpString(prod.sellingPricePiasters)} ج.م",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                            )
                            if (!prod.rejectionReason.isNullOrBlank()) {
                                Text("سبب الرفض: ${prod.rejectionReason}", style = AppText.body.copy(color = AppColors.Alert, fontSize = 11.5.sp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingProduct = prod
                                        showProductSheet = true
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل", style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                }
                                OutlinedButton(
                                    onClick = { onToggleProductPause(prod) },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        if (prod.status.name == "ACTIVE") Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (prod.status.name == "ACTIVE") "إيقاف" else "تفعيل", style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.merchantOrders.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد طلبات واردة للمتجر حتى الآن.", style = AppText.body.copy(color = AppColors.Muted))
                        }
                    }
                } else {
                    items(uiState.merchantOrders, key = { it.orderId }) { ord ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, AppColors.Line),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(ord.orderId, style = AppText.body.copy(fontWeight = FontWeight.ExtraBold))
                                    Text(ord.status.arabicLabel, style = AppText.body.copy(color = AppColors.Brass, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                }
                                Text(
                                    "الإجمالي: ${MoneyUtils.piastersToEgpString(ord.totalPiasters)} ج.م · موعد الاستلام: ${ord.pickupWindow}",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                                )
                                if (ord.status == OrderStatus.PAID) {
                                    Button(onClick = { onUpdateOrderStatus(ord.orderId, OrderStatus.PREPARING) }) {
                                        Text("بدء التجهيز")
                                    }
                                } else if (ord.status == OrderStatus.PREPARING) {
                                    Button(onClick = { onUpdateOrderStatus(ord.orderId, OrderStatus.READY_FOR_PICKUP) }) {
                                        Text("جاهز للاستلام من الفرع")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProductSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var pName by remember(editingProduct) { mutableStateOf(editingProduct?.name.orEmpty()) }
        var pDesc by remember(editingProduct) { mutableStateOf(editingProduct?.description.orEmpty()) }
        var pCategory by remember(editingProduct) { mutableStateOf(editingProduct?.category ?: "خواتم") }
        var pKarat by remember(editingProduct) { mutableIntStateOf(editingProduct?.karat ?: 21) }
        var pWeight by remember(editingProduct) { mutableStateOf(editingProduct?.weightGrams?.toString() ?: "") }
        var pPriceEgp by remember(editingProduct) {
            mutableStateOf(
                if ((editingProduct?.sellingPricePiasters ?: 0L) > 0L) {
                    MoneyUtils.piastersToEgpString(editingProduct!!.sellingPricePiasters)
                } else "",
            )
        }
        var pStock by remember(editingProduct) { mutableStateOf(editingProduct?.stockQuantity?.toString() ?: "5") }
        var pStamped by remember(editingProduct) { mutableStateOf(editingProduct?.stamped ?: true) }
        var pThumbs by remember(editingProduct) { mutableStateOf(editingProduct?.imageThumbnails ?: emptyList()) }
        var sheetError by remember { mutableStateOf<String?>(null) }

        val imgPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(4),
        ) { uris ->
            if (uris.isNotEmpty()) {
                scope.launch {
                    pThumbs = uris.take(4).mapNotNull { ImageThumbnailCodec.encodeUriToBase64Thumbnail(context, it) }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                sheetError = null
                onClearMessages()
                showProductSheet = false
            },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (editingProduct == null) "إضافة منتج ذهبي جديد" else "تعديل المنتج", style = AppText.display.copy(fontSize = 18.sp))
                val visibleError = sheetError ?: uiState.actionErrorMessage
                if (visibleError != null) {
                    Text(
                        text = visibleError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.AlertSoft)
                            .padding(10.dp),
                        style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    )
                }
                OutlinedTextField(value = pName, onValueChange = { pName = it; sheetError = null }, label = { Text("اسم القطعة") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = pDesc, onValueChange = { pDesc = it; sheetError = null }, label = { Text("الوصف والمواصفات") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("خواتم", "أساور", "سلاسل", "جنيهات وسبائك").forEach { c ->
                        FilterChip(selected = pCategory == c, onClick = { pCategory = c }, label = { Text(c, fontSize = 11.sp) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 21, 18).forEach { k ->
                        FilterChip(selected = pKarat == k, onClick = { pKarat = k }, label = { Text("عيار $k") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pWeight, onValueChange = { pWeight = it; sheetError = null }, label = { Text("الوزن (جم)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = pStock, onValueChange = { pStock = it.filter { ch -> ch.isDigit() }; sheetError = null }, label = { Text("الكمية المتاحة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(
                    value = pPriceEgp,
                    onValueChange = { pPriceEgp = it; sheetError = null },
                    label = { Text("السعر النهائي للعميل بالجنيه (بدون مصنعية إضافية)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("قطعة مدموغة رسمياً", style = AppText.body.copy(fontWeight = FontWeight.Bold))
                    Switch(checked = pStamped, onCheckedChange = { pStamped = it })
                }
                OutlinedButton(
                    onClick = { imgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (pThumbs.isEmpty()) "اختيار صور المنتج (حتى 4)" else "تم اختيار ${pThumbs.size} صور")
                }
                Button(
                    onClick = {
                        val w = pWeight.toDoubleOrNull() ?: 0.0
                        val pricePiasters = MoneyUtils.egpStringToPiasters(pPriceEgp) ?: 0L
                        val stock = pStock.toIntOrNull() ?: 0
                        if (pName.trim().length < 2 || w <= 0.0 || pricePiasters <= 0L) {
                            sheetError = "يرجى إدخال اسم المنتج والوزن والسعر النهائي بشكل صحيح."
                        } else {
                            sheetError = null
                            onSaveProduct(merchant, editingProduct, pName, pDesc, pCategory, pKarat, w, pricePiasters, stock, pStamped, pThumbs) {
                                showProductSheet = false
                            }
                        }
                    },
                    enabled = !uiState.isActionBusy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
                ) {
                    Text("حفظ ونشر للمراجعة", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}
