package com.example.ui.screens.sell

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.model.MoneyUtils
import com.example.model.SellGoldRequest
import com.example.model.SellRequestStatus
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.Base64ThumbnailImage
import com.example.ui.util.ImageThumbnailCodec
import kotlinx.coroutines.launch

@Composable
fun SellGoldScreen(
    uiState: KingGoldUiState,
    onSubmitRequest: (
        itemTitle: String,
        category: String,
        karat: Int,
        estimatedWeightMg: Long,
        conditionNotes: String,
        photoThumbnails: List<String>,
    ) -> Unit,
    onCancelRequest: (String) -> Unit,
    onAcceptOffer: (SellGoldRequest) -> Unit,
    onClearMessages: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var itemTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("سبائك وجنيهات") }
    var selectedKarat by remember { mutableIntStateOf(21) }
    var estimatedGramsText by remember { mutableStateOf("") }
    var conditionNotes by remember { mutableStateOf("") }
    var thumbnails by remember { mutableStateOf<List<String>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val encoded = uris.take(4).mapNotNull { uri ->
                    ImageThumbnailCodec.encodeUriToBase64Thumbnail(context, uri)
                }
                thumbnails = encoded
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
                    Text("بيع ذهبك للمنصة", style = AppText.display.copy(fontSize = 22.sp))
                    Text(
                        "تشتري منصة King Gold ذهبك مباشرة بعد الفحص وتضيف الصافي لمحفظتك فوراً",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.Ink,
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Verified, null, tint = AppColors.BrassLight, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "المنصة هي المشتري المباشر لذهبك — يحدد فريق الفحص الوزن الفعلي وقيمة المصنعية المخصومة بشفافية تامة قبل موافقتك.",
                            style = AppText.body.copy(color = Color.White, fontSize = 12.sp),
                        )
                    }
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
                            .border(1.dp, if (isErr) AppColors.Alert else AppColors.Trust, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.actionErrorMessage ?: uiState.actionBannerMessage.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = AppText.body.copy(color = if (isErr) AppColors.Alert else AppColors.Trust, fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                        )
                        TextButton(onClick = onClearMessages) {
                            Text("حسناً")
                        }
                    }
                }
            }

            // Submit form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppColors.Line),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تقديم قطعة ذهب للبيع", style = AppText.display.copy(fontSize = 17.sp))

                        OutlinedTextField(
                            value = itemTitle,
                            onValueChange = { itemTitle = it },
                            label = { Text("وصف القطعة (مثال: خاتم عيار 21 أو سبيكة)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sell_item_title_input"),
                            singleLine = true,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("سبائك وجنيهات", "خواتم", "أساور", "كسر ذهب").forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, style = AppText.body.copy(fontSize = 11.5.sp)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.Ink,
                                        selectedLabelColor = AppColors.BrassLight,
                                    ),
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(24, 21, 18).forEach { k ->
                                FilterChip(
                                    selected = selectedKarat == k,
                                    onClick = { selectedKarat = k },
                                    label = { Text("عيار $k", style = AppText.body.copy(fontWeight = FontWeight.Bold)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.Brass,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = estimatedGramsText,
                            onValueChange = { estimatedGramsText = it },
                            label = { Text("الوزن التقديري بالجرام (مثال: 8.5)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sell_weight_input"),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = conditionNotes,
                            onValueChange = { conditionNotes = it },
                            label = { Text("ملاحظات الحالة / الفاتورة (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedButton(
                            onClick = {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null, tint = AppColors.Brass)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (thumbnails.isEmpty()) "إرفاق صور القطعة (حتى 4 صور)" else "تم اختيار ${thumbnails.size} صور — اضغط للتعديل",
                                style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold),
                            )
                        }

                        if (thumbnails.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(thumbnails) { b64 ->
                                    Base64ThumbnailImage(
                                        encoded = b64,
                                        contentDescription = "صورة قطعة الذهب",
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val weightMg = MoneyUtils.gramsStringToMg(estimatedGramsText) ?: 0L
                                onSubmitRequest(
                                    itemTitle,
                                    selectedCategory,
                                    selectedKarat,
                                    weightMg,
                                    conditionNotes,
                                    thumbnails,
                                )
                                if (itemTitle.trim().length >= 2 && weightMg >= 10L) {
                                    itemTitle = ""
                                    estimatedGramsText = ""
                                    conditionNotes = ""
                                    thumbnails = emptyList()
                                }
                            },
                            enabled = !uiState.isActionBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_sell_gold_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brass),
                        ) {
                            Icon(Icons.Outlined.Sell, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال للمعاينة والتسعير", style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            item {
                Text("طلبات البيع الخاصة بي (${uiState.sellRequests.size})", style = AppText.display.copy(fontSize = 17.sp))
            }

            if (uiState.sellRequests.isEmpty()) {
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
                        Text("لم تقم بإرسال أي طلبات بيع بعد.", style = AppText.body.copy(color = AppColors.Muted))
                    }
                }
            } else {
                items(uiState.sellRequests, key = { it.id }) { req ->
                    SellRequestCard(
                        req = req,
                        isBusy = uiState.isActionBusy,
                        onCancel = { onCancelRequest(req.id) },
                        onAcceptOffer = { onAcceptOffer(req) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SellRequestCard(
    req: SellGoldRequest,
    isBusy: Boolean,
    onCancel: () -> Unit,
    onAcceptOffer: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sell_request_card_${req.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            if (req.status == SellRequestStatus.OFFER_SENT) AppColors.Brass else AppColors.Line,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(req.itemTitle, style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (req.status) {
                                SellRequestStatus.COMPLETED -> AppColors.TrustSoft
                                SellRequestStatus.OFFER_SENT -> AppColors.Brass.copy(alpha = 0.16f)
                                SellRequestStatus.REJECTED, SellRequestStatus.CANCELLED -> AppColors.AlertSoft
                                else -> AppColors.ParchmentDeep
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        req.status.arabicLabel,
                        style = AppText.body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }

            Text(
                "عيار ${req.karat} · الوزن المبدئي ${MoneyUtils.mgToGramsString(req.estimatedWeightMg)} · ${req.category}",
                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
            )

            if (req.photoThumbnails.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(req.photoThumbnails) { b64 ->
                        Base64ThumbnailImage(
                            encoded = b64,
                            contentDescription = req.itemTitle,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }

            if (req.status == SellRequestStatus.OFFER_SENT || req.status == SellRequestStatus.COMPLETED) {
                HorizontalDivider(color = AppColors.ParchmentDeep)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Parchment)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("تفاصيل تقييم الإدارة", style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp))
                    OfferRow("الوزن الفعلي المعتمد", "${MoneyUtils.mgToGramsString(req.verifiedWeightMg)} (عيار ${req.confirmedKarat})")
                    OfferRow("السعر المرجعي للجرام", "${MoneyUtils.piastersToEgpString(req.referencePricePerGramPiasters)} ج.م")
                    OfferRow("إجمالي قيمة الذهب", "${MoneyUtils.piastersToEgpString(req.evaluatedGoldValuePiasters)} ج.م")
                    OfferRow("خصم المصنعية (يدوي)", "- ${MoneyUtils.piastersToEgpString(req.makingChargeDeductionPiasters)} ج.م")
                    OfferRow("العرض النهائي الصافي", "${MoneyUtils.piastersToEgpString(req.finalOfferPiasters)} ج.م", highlight = true)
                    if (!req.inspectionNotes.isNullOrBlank()) {
                        Text("ملاحظة الفحص: ${req.inspectionNotes}", style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp))
                    }
                }
            }

            if (req.status == SellRequestStatus.OFFER_SENT) {
                Button(
                    onClick = onAcceptOffer,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("accept_sell_offer_${req.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Trust),
                ) {
                    Text(
                        "قبول العرض وإضافة ${MoneyUtils.piastersToEgpString(req.finalOfferPiasters)} ج.م لمحفظتي",
                        style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    )
                }
            } else if (req.status == SellRequestStatus.SUBMITTED) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("إلغاء الطلب", style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold))
                }
            } else if (req.status == SellRequestStatus.REJECTED && !req.rejectionReason.isNullOrBlank()) {
                Text("سبب الرفض: ${req.rejectionReason}", style = AppText.body.copy(color = AppColors.Alert, fontSize = 12.sp))
            }
        }
    }
}

@Composable
private fun OfferRow(label: String, value: String, highlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = AppText.body.copy(color = if (highlight) AppColors.Ink else AppColors.Muted, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp))
        Text(value, style = AppText.body.copy(color = if (highlight) AppColors.Trust else AppColors.Ink, fontWeight = FontWeight.Bold, fontSize = if (highlight) 14.sp else 12.sp))
    }
}
