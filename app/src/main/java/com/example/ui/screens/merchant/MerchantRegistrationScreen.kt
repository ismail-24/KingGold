package com.example.ui.screens.merchant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.MerchantStatus
import com.example.ui.state.MerchantRegistrationField
import com.example.ui.state.MerchantRegistrationSubmissionState
import com.example.ui.state.MerchantRegistrationUiState
import com.example.ui.state.MerchantRegistrationViewModel
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.Base64ThumbnailImage
import com.example.ui.util.ImageThumbnailCodec
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantRegistrationScreen(
    viewModel: MerchantRegistrationViewModel,
    onBack: () -> Unit,
    onOpenMerchantPortal: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val encoded = uris.take(4).mapNotNull {
                    ImageThumbnailCodec.encodeUriToBase64Thumbnail(context, it)
                }
                viewModel.updateDocumentThumbnails(encoded)
            }
        }
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.merchant_reg_title),
                        style = AppText.display.copy(fontSize = 20.sp),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("merchant_registration_back_button"),
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
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val submissionState = uiState.submissionState
            if (submissionState is MerchantRegistrationSubmissionState.Success) {
                MerchantRegistrationSuccessView(
                    result = submissionState,
                    onOpenMerchantPortal = onOpenMerchantPortal,
                    onRegisterAnother = viewModel::resetForAnotherApplication,
                    onBack = onBack,
                )
            } else {
                MerchantRegistrationFormContent(
                    uiState = uiState,
                    onLegalNameChange = viewModel::updateLegalName,
                    onBusinessIdChange = viewModel::updateBusinessId,
                    onCommercialRegChange = viewModel::updateCommercialRegNumber,
                    onTaxCardChange = viewModel::updateTaxCardNumber,
                    onShopNameChange = viewModel::updateShopName,
                    onAreaChange = viewModel::updateArea,
                    onShopAddressChange = viewModel::updateShopAddress,
                    onDescriptionChange = viewModel::updateDescription,
                    onContactPhoneChange = viewModel::updateContactPhone,
                    onContactEmailChange = viewModel::updateContactEmail,
                    onAuthorizationNotesChange = viewModel::updateAuthorizationNotes,
                    onPickDocuments = {
                        docPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onDismissError = viewModel::clearError,
                    onSubmit = viewModel::submitRegistration,
                    onOpenMerchantPortal = onOpenMerchantPortal,
                )
            }
        }
    }
}

@Composable
private fun MerchantRegistrationFormContent(
    uiState: MerchantRegistrationUiState,
    onLegalNameChange: (String) -> Unit,
    onBusinessIdChange: (String) -> Unit,
    onCommercialRegChange: (String) -> Unit,
    onTaxCardChange: (String) -> Unit,
    onShopNameChange: (String) -> Unit,
    onAreaChange: (String) -> Unit,
    onShopAddressChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onContactPhoneChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit,
    onAuthorizationNotesChange: (String) -> Unit,
    onPickDocuments: () -> Unit,
    onDismissError: () -> Unit,
    onSubmit: () -> Unit,
    onOpenMerchantPortal: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

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
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = AppColors.BrassLight,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "انضم كتاجر ذهب معتمد",
                            style = AppText.display.copy(color = AppColors.BrassLight, fontSize = 17.sp),
                        )
                        Text(
                            text = "تُسجَّل البيانات في قاعدة التجار بحالة ابتدائية «${uiState.pendingStatus.arabicLabel} (${uiState.pendingStatus.name})» لحين مراجعة الإدارة والموافقة.",
                            style = AppText.body.copy(color = Color.White, fontSize = 12.sp),
                        )
                    }
                }
            }
        }

        val latestMerchant = uiState.existingMerchants.firstOrNull()
        if (latestMerchant != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_existing_merchant_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.ParchmentDeep),
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HourglassTop,
                            contentDescription = null,
                            tint = AppColors.Brass,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "لديك طلب مسجّل: ${latestMerchant.shopName}",
                                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            )
                            Text(
                                text = "الحالة الحالية: ${latestMerchant.status.arabicLabel}",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                            )
                        }
                        TextButton(onClick = onOpenMerchantPortal) {
                            Text("فتح البوابة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        val errState = uiState.submissionState as? MerchantRegistrationSubmissionState.Error
        if (errState != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.AlertSoft)
                        .padding(12.dp)
                        .testTag("reg_error_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = AppColors.Alert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errState.message,
                        modifier = Modifier.weight(1f),
                        style = AppText.body.copy(
                            color = AppColors.Alert,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        ),
                    )
                    TextButton(onClick = onDismissError) {
                        Text("إغلاق")
                    }
                }
            }
        }

        item {
            RegistrationSectionCard(
                icon = Icons.Outlined.Badge,
                title = "البيانات القانونية والهوية التجارية",
            ) {
                val legalError = uiState.fieldErrors[MerchantRegistrationField.LEGAL_NAME]
                OutlinedTextField(
                    value = uiState.legalName,
                    onValueChange = onLegalNameChange,
                    label = { Text("الاسم القانوني الكامل *") },
                    placeholder = { Text("الاسم الثلاثي أو الرباعي كما في البطاقة") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = AppColors.Brass) },
                    isError = legalError != null,
                    supportingText = legalError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_legal_name_input"),
                )

                val businessIdError = uiState.fieldErrors[MerchantRegistrationField.BUSINESS_ID]
                OutlinedTextField(
                    value = uiState.businessId,
                    onValueChange = onBusinessIdChange,
                    label = { Text("رقم الهوية التجارية / الرقم القومي (Business ID) *") },
                    placeholder = { Text("مثال: CR-295010112345") },
                    leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = AppColors.Brass) },
                    isError = businessIdError != null,
                    supportingText = businessIdError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_business_id_input"),
                )

                OutlinedTextField(
                    value = uiState.commercialRegNumber,
                    onValueChange = onCommercialRegChange,
                    label = { Text("رقم السجل التجاري (اختياري)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_commercial_reg_input"),
                )

                OutlinedTextField(
                    value = uiState.taxCardNumber,
                    onValueChange = onTaxCardChange,
                    label = { Text("رقم البطاقة الضريبية (اختياري)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_tax_card_input"),
                )
            }
        }

        item {
            RegistrationSectionCard(
                icon = Icons.Outlined.Storefront,
                title = "بيانات المحل وعنوان النشاط",
            ) {
                val shopNameError = uiState.fieldErrors[MerchantRegistrationField.SHOP_NAME]
                OutlinedTextField(
                    value = uiState.shopName,
                    onValueChange = onShopNameChange,
                    label = { Text("اسم محل المجوهرات *") },
                    placeholder = { Text("مثال: مجوهرات الشريف") },
                    leadingIcon = { Icon(Icons.Outlined.Storefront, contentDescription = null, tint = AppColors.Brass) },
                    isError = shopNameError != null,
                    supportingText = shopNameError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_shop_name_input"),
                )

                val areaError = uiState.fieldErrors[MerchantRegistrationField.AREA]
                OutlinedTextField(
                    value = uiState.area,
                    onValueChange = onAreaChange,
                    label = { Text("المنطقة / المدينة *") },
                    placeholder = { Text("مثال: الصاغة، القاهرة") },
                    leadingIcon = { Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = AppColors.Brass) },
                    isError = areaError != null,
                    supportingText = areaError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_area_input"),
                )

                val addressError = uiState.fieldErrors[MerchantRegistrationField.SHOP_ADDRESS]
                OutlinedTextField(
                    value = uiState.shopAddress,
                    onValueChange = onShopAddressChange,
                    label = { Text("عنوان المحل التفصيلي *") },
                    placeholder = { Text("الشارع، رقم العقار، علامة مميزة") },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = AppColors.Brass) },
                    isError = addressError != null,
                    supportingText = addressError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_shop_address_input"),
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("نبذة عن المتجر والتخصص (اختياري)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_description_input"),
                )
            }
        }

        item {
            RegistrationSectionCard(
                icon = Icons.Outlined.ContactPhone,
                title = "بيانات التواصل والمستندات الداعمة",
            ) {
                val phoneError = uiState.fieldErrors[MerchantRegistrationField.CONTACT_PHONE]
                OutlinedTextField(
                    value = uiState.contactPhone,
                    onValueChange = onContactPhoneChange,
                    label = { Text("رقم هاتف التواصل *") },
                    placeholder = { Text("+2010XXXXXXXX") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = AppColors.Brass) },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_contact_phone_input"),
                )

                val emailError = uiState.fieldErrors[MerchantRegistrationField.CONTACT_EMAIL]
                OutlinedTextField(
                    value = uiState.contactEmail,
                    onValueChange = onContactEmailChange,
                    label = { Text("البريد الإلكتروني للتواصل *") },
                    placeholder = { Text("owner@shop.com") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = AppColors.Brass) },
                    isError = emailError != null,
                    supportingText = emailError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_contact_email_input"),
                )

                OutlinedTextField(
                    value = uiState.authorizationNotes,
                    onValueChange = onAuthorizationNotesChange,
                    label = { Text("ملاحظات التفويض أو إثبات الملكية (اختياري)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_auth_notes_input"),
                )

                OutlinedButton(
                    onClick = onPickDocuments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("reg_pick_docs_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = AppColors.Brass)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (uiState.documentThumbnails.isEmpty()) {
                            "إرفاق صور المستندات (حتى 4 صور)"
                        } else {
                            "تم إرفاق ${uiState.documentThumbnails.size} مستندات"
                        },
                    )
                }

                if (uiState.documentThumbnails.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.documentThumbnails) { b64 ->
                            Base64ThumbnailImage(
                                encoded = b64,
                                contentDescription = "مستند تاجر",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onSubmit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("reg_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(R.string.merchant_reg_submit),
                    style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RegistrationSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = AppColors.Brass, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = AppText.display.copy(fontSize = 16.sp))
            }
            content()
        }
    }
}

@Composable
private fun MerchantRegistrationSuccessView(
    result: MerchantRegistrationSubmissionState.Success,
    onOpenMerchantPortal: () -> Unit,
    onRegisterAnother: () -> Unit,
    onBack: () -> Unit,
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
                    .testTag("reg_success_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AppColors.Line),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "تم إرسال الطلب",
                            tint = AppColors.Trust,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "تم تسجيل طلب التاجر بنجاح",
                            style = AppText.display.copy(fontSize = 19.sp),
                        )
                    }

                    Text(
                        text = "اسم المتجر: ${result.shopName}",
                        style = AppText.body.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "معرّف التاجر: ${result.merchantId}",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AppColors.ParchmentDeep,
                    ) {
                        Text(
                            text = "الحالة في merchants: ${result.status.arabicLabel} (${result.status.name})",
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("reg_success_pending_badge"),
                            style = AppText.body.copy(
                                color = AppColors.Brass,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            ),
                        )
                    }
                    Text(
                        text = "تم حفظ الطلب في مجموعة merchants بحالة pending، وحفظ بيانات التحقق في المستند الخاص الآمن للمراجعة الإدارية.",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                    )

                    Button(
                        onClick = onOpenMerchantPortal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("reg_success_open_portal_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Ink),
                    ) {
                        Text("متابعة الطلب في بوابة التاجر", color = AppColors.BrassLight, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRegisterAnother,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("تسجيل متجر آخر")
                    }

                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("العودة للحساب")
                    }
                }
            }
        }
    }
}
