package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MerchantAuditLog
import com.example.model.MerchantPrivateDocs
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.model.MoneyUtils
import com.example.model.SellGoldRequest
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.Base64ThumbnailImage

@Composable
fun MerchantInspectionDialog(
    merchant: MerchantStore,
    docs: MerchantPrivateDocs?,
    auditLogs: List<MerchantAuditLog>,
    onDismiss: () -> Unit,
    onReview: (MerchantStatus, prepaidApproved: Boolean, budgetPiasters: Long, note: String) -> Unit,
) {
    var prepaidApproved by remember(merchant.id) { mutableStateOf(merchant.prepaidMakingApproved) }
    var budgetEgp by remember(merchant.id) { mutableStateOf(MoneyUtils.piastersToEgpString(merchant.prepaidMakingBudgetPiasters)) }
    var note by remember(merchant.id) { mutableStateOf(merchant.pricingPolicyNote.orEmpty().ifBlank { "معتمد — السعر نهائي بدون مصنعية على المشتري" }) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("مراجعة طلب تاجر: ${merchant.shopName}", style = AppText.display.copy(fontSize = 18.sp))
                Text("${merchant.area} — ${merchant.address}", style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp))
                Text("هاتف: ${merchant.contactPhone} · بريد: ${merchant.verifiedEmail}", style = AppText.body.copy(fontSize = 12.sp))

                if (docs != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.Parchment),
                        border = BorderStroke(1.dp, AppColors.Line),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("المستندات السرية (KYC)", style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp))
                            Text("الاسم القانوني: ${docs.legalFullName}", style = AppText.body.copy(fontSize = 12.sp))
                            Text("الرقم القومي/الترخيص: ${docs.nationalIdNumber}", style = AppText.body.copy(fontSize = 12.sp))
                            if (docs.commercialRegNumber.isNotBlank()) Text("سجل تجاري: ${docs.commercialRegNumber}", style = AppText.body.copy(fontSize = 12.sp))
                            if (docs.taxCardNumber.isNotBlank()) Text("بطاقة ضريبية: ${docs.taxCardNumber}", style = AppText.body.copy(fontSize = 12.sp))
                            if (docs.authorizationNotes.isNotBlank()) Text("تفويض: ${docs.authorizationNotes}", style = AppText.body.copy(fontSize = 12.sp))
                            if (docs.documentThumbnails.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(docs.documentThumbnails) { b64 ->
                                        Base64ThumbnailImage(encoded = b64, contentDescription = "مستند تاجر", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("جاري تحميل مستندات التحقق...", style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("اعتماد ميزانية مصنعية مسبقة", style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
                    Switch(checked = prepaidApproved, onCheckedChange = { prepaidApproved = it })
                }
                OutlinedTextField(
                    value = budgetEgp,
                    onValueChange = { budgetEgp = it },
                    label = { Text("ميزانية المصنعية المسبقة (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة القرار / سبب الرفض أو الإيقاف") },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (auditLogs.isNotEmpty()) {
                    Text("سجل التدقيق:", style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                    auditLogs.take(4).forEach { log ->
                        Text("• ${log.action}: ${log.note}", style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp))
                    }
                }

                val budgetPiasters = MoneyUtils.egpStringToPiasters(budgetEgp) ?: 0L
                Button(
                    onClick = { onReview(MerchantStatus.APPROVED, prepaidApproved, budgetPiasters, note) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Trust),
                ) {
                    Text("اعتماد وتفعيل المتجر", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onReview(MerchantStatus.REJECTED, false, 0L, note) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("رفض", color = AppColors.Alert, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onReview(MerchantStatus.SUSPENDED, false, 0L, note) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("إيقاف", color = AppColors.Alert, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("إغلاق") }
            }
        }
    }
}

@Composable
fun SellGoldEvaluationDialog(
    request: SellGoldRequest,
    defaultRefPricePiastersForKarat: (Int) -> Long,
    onDismiss: () -> Unit,
    onSendOffer: (
        verifiedWeightMg: Long,
        confirmedKarat: Int,
        refPricePerGramPiasters: Long,
        deductionPiasters: Long,
        notes: String,
    ) -> Unit,
    onReject: (reason: String) -> Unit,
) {
    var verifiedGramsText by remember(request.id) {
        mutableStateOf(MoneyUtils.mgToGramsDouble(request.estimatedWeightMg).toString())
    }
    var confirmedKarat by remember(request.id) { mutableIntStateOf(request.karat) }
    var refPriceEgpText by remember(request.id) {
        mutableStateOf(MoneyUtils.piastersToEgpString(defaultRefPricePiastersForKarat(request.karat)))
    }
    var customDeductionEgpText by remember(request.id) { mutableStateOf("0") }
    var notesText by remember(request.id) { mutableStateOf("") }

    val verifiedMg = MoneyUtils.gramsStringToMg(verifiedGramsText) ?: 0L
    val refPiasters = MoneyUtils.egpStringToPiasters(refPriceEgpText) ?: 0L
    val dedPiasters = MoneyUtils.egpStringToPiasters(customDeductionEgpText) ?: 0L
    val evalPiasters = MoneyUtils.computeFractionalCostPiasters(verifiedMg, refPiasters)
    val finalOffer = (evalPiasters - dedPiasters).coerceAtLeast(0L)

    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("فحص وتقييم: ${request.itemTitle}", style = AppText.display.copy(fontSize = 18.sp))
                Text(
                    "وزن العميل التقديري: ${MoneyUtils.mgToGramsString(request.estimatedWeightMg)} · عيار ${request.karat}",
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(24, 21, 18).forEach { k ->
                        FilterChip(
                            selected = confirmedKarat == k,
                            onClick = {
                                confirmedKarat = k
                                refPriceEgpText = MoneyUtils.piastersToEgpString(defaultRefPricePiastersForKarat(k))
                            },
                            label = { Text("عيار $k") },
                        )
                    }
                }
                OutlinedTextField(
                    value = verifiedGramsText,
                    onValueChange = { verifiedGramsText = it },
                    label = { Text("الوزن الفعلي المعتمد (جم)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = refPriceEgpText,
                    onValueChange = { refPriceEgpText = it },
                    label = { Text("السعر المرجعي للجرام (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = customDeductionEgpText,
                    onValueChange = { customDeductionEgpText = it },
                    label = { Text("خصم المصنعية / إعادة الصياغة المخصص (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("ملاحظات الفحص أو سبب الرفض") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Parchment)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("قيمة الذهب المقيّمة: ${MoneyUtils.piastersToEgpString(evalPiasters)} ج.م", style = AppText.body.copy(fontSize = 12.5.sp))
                    Text("صافي العرض النهائي للعميل: ${MoneyUtils.piastersToEgpString(finalOffer)} ج.م", style = AppText.body.copy(color = AppColors.Trust, fontWeight = FontWeight.ExtraBold, fontSize = 14.5.sp))
                }

                Button(
                    onClick = { onSendOffer(verifiedMg, confirmedKarat, refPiasters, dedPiasters, notesText) },
                    enabled = verifiedMg > 0L && refPiasters > 0L && finalOffer > 0L,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Trust),
                ) {
                    Text("إرسال العرض النهائي للعميل", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onReject(notesText.ifBlank { "القطعة لم تجتز الفحص" }) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("رفض الطلب", color = AppColors.Alert, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("إغلاق") }
            }
        }
    }
}
