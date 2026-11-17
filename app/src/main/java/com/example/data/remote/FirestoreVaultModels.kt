package com.example.data.remote

import androidx.annotation.Keep
import com.example.model.AuditLogRecord
import com.example.model.DepositRequest
import com.example.model.DepositStatus
import com.example.model.FractionalGoldOrder
import com.example.model.LedgerEntry
import com.example.model.LedgerTxType
import com.example.model.PlatformPricingConfig
import com.example.model.SellGoldRequest
import com.example.model.SellRequestStatus
import com.example.model.WalletSummary
import com.example.model.WithdrawalDeliveryType
import com.example.model.WithdrawalRequest
import com.example.model.WithdrawalStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class SellGoldRequestRecord(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val customerEmail: String = "",
    val itemTitle: String = "",
    val category: String = "مشغولات ذهبية",
    val karat: Long = 21L,
    val estimatedWeightMg: Long = 0L,
    val conditionNotes: String = "",
    val photoThumbnails: List<String> = emptyList(),
    val status: String = SellRequestStatus.SUBMITTED.name,
    val evaluatorAdminUid: String? = null,
    val verifiedWeightMg: Long = 0L,
    val confirmedKarat: Long = 21L,
    val referencePricePerGramPiasters: Long = 0L,
    val evaluatedGoldValuePiasters: Long = 0L,
    val makingChargeDeductionPiasters: Long = 0L,
    val finalOfferPiasters: Long = 0L,
    val inspectionNotes: String? = null,
    val rejectionReason: String? = null,
    val settlementTransactionId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): SellGoldRequest = SellGoldRequest(
        id = id,
        customerUid = customerUid,
        customerEmail = customerEmail,
        itemTitle = itemTitle,
        category = category,
        karat = karat.toInt(),
        estimatedWeightMg = estimatedWeightMg,
        conditionNotes = conditionNotes,
        photoThumbnails = photoThumbnails,
        status = enumValues<SellRequestStatus>().firstOrNull { it.name == status }
            ?: SellRequestStatus.SUBMITTED,
        evaluatorAdminUid = evaluatorAdminUid,
        verifiedWeightMg = verifiedWeightMg,
        confirmedKarat = confirmedKarat.toInt(),
        referencePricePerGramPiasters = referencePricePerGramPiasters,
        evaluatedGoldValuePiasters = evaluatedGoldValuePiasters,
        makingChargeDeductionPiasters = makingChargeDeductionPiasters,
        finalOfferPiasters = finalOfferPiasters,
        inspectionNotes = inspectionNotes,
        rejectionReason = rejectionReason,
        settlementTransactionId = settlementTransactionId,
        createdAtMs = createdAt?.toDate()?.time ?: 0L,
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "customerUid" to customerUid,
        "customerEmail" to customerEmail,
        "itemTitle" to itemTitle,
        "category" to category,
        "karat" to karat,
        "estimatedWeightMg" to estimatedWeightMg,
        "conditionNotes" to conditionNotes,
        "photoThumbnails" to photoThumbnails.take(4),
        "status" to SellRequestStatus.SUBMITTED.name,
        "verifiedWeightMg" to 0L,
        "confirmedKarat" to karat,
        "referencePricePerGramPiasters" to 0L,
        "evaluatedGoldValuePiasters" to 0L,
        "makingChargeDeductionPiasters" to 0L,
        "finalOfferPiasters" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class WalletSummaryRecord(
    val userId: String = "",
    val cashAvailablePiasters: Long = 0L,
    val cashPendingPiasters: Long = 0L,
    val cashTotalCreditedPiasters: Long = 0L,
    val cashTotalDebitedPiasters: Long = 0L,
    val goldConfirmedMg: Long = 0L,
    val goldReservedMg: Long = 0L,
    val goldPendingMg: Long = 0L,
    val gold24kMg: Long = 0L,
    val gold21kMg: Long = 0L,
    val gold18kMg: Long = 0L,
    val totalInvestedPiasters: Long = 0L,
    val lastSettlementRef: String = "init",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): WalletSummary = WalletSummary(
        userId = userId,
        cashAvailablePiasters = cashAvailablePiasters,
        cashPendingPiasters = cashPendingPiasters,
        cashTotalCreditedPiasters = cashTotalCreditedPiasters,
        cashTotalDebitedPiasters = cashTotalDebitedPiasters,
        goldConfirmedMg = goldConfirmedMg,
        goldReservedMg = goldReservedMg,
        goldPendingMg = goldPendingMg,
        gold24kMg = gold24kMg,
        gold21kMg = gold21kMg,
        gold18kMg = gold18kMg,
        totalInvestedPiasters = totalInvestedPiasters,
        lastSettlementRef = lastSettlementRef,
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "cashAvailablePiasters" to 0L,
        "cashPendingPiasters" to 0L,
        "cashTotalCreditedPiasters" to 0L,
        "cashTotalDebitedPiasters" to 0L,
        "goldConfirmedMg" to 0L,
        "goldReservedMg" to 0L,
        "goldPendingMg" to 0L,
        "gold24kMg" to 0L,
        "gold21kMg" to 0L,
        "gold18kMg" to 0L,
        "totalInvestedPiasters" to 0L,
        "lastSettlementRef" to "init",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class LedgerEntryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val idempotencyKey: String = "",
    val txType: String = LedgerTxType.ADMIN_ADJUSTMENT.name,
    val referenceId: String = "",
    val karat: Long = 0L,
    val cashDeltaPiasters: Long = 0L,
    val pendingCashDeltaPiasters: Long = 0L,
    val goldDeltaMg: Long = 0L,
    val reservedDeltaMg: Long = 0L,
    val pendingGoldDeltaMg: Long = 0L,
    val unitPricePerGramPiasters: Long = 0L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toDomain(): LedgerEntry = LedgerEntry(
        id = id,
        userId = userId,
        idempotencyKey = idempotencyKey,
        txType = enumValues<LedgerTxType>().firstOrNull { it.name == txType }
            ?: LedgerTxType.ADMIN_ADJUSTMENT,
        referenceId = referenceId,
        karat = karat.toInt(),
        cashDeltaPiasters = cashDeltaPiasters,
        pendingCashDeltaPiasters = pendingCashDeltaPiasters,
        goldDeltaMg = goldDeltaMg,
        reservedDeltaMg = reservedDeltaMg,
        pendingGoldDeltaMg = pendingGoldDeltaMg,
        unitPricePerGramPiasters = unitPricePerGramPiasters,
        notes = notes,
        createdAtMs = createdAt?.toDate()?.time ?: 0L,
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "idempotencyKey" to idempotencyKey,
        "txType" to txType,
        "referenceId" to referenceId,
        "karat" to karat,
        "cashDeltaPiasters" to cashDeltaPiasters,
        "pendingCashDeltaPiasters" to pendingCashDeltaPiasters,
        "goldDeltaMg" to goldDeltaMg,
        "reservedDeltaMg" to reservedDeltaMg,
        "pendingGoldDeltaMg" to pendingGoldDeltaMg,
        "unitPricePerGramPiasters" to unitPricePerGramPiasters,
        "notes" to notes,
        "createdAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class FractionalGoldOrderRecord(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val karat: Long = 24L,
    val weightMg: Long = 0L,
    val pricePerGramPiasters: Long = 0L,
    val totalPiasters: Long = 0L,
    val paymentSource: String = "CASH_WALLET",
    val status: String = "CONFIRMED",
    val ledgerEntryId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): FractionalGoldOrder = FractionalGoldOrder(
        id = id,
        customerUid = customerUid,
        karat = karat.toInt(),
        weightMg = weightMg,
        pricePerGramPiasters = pricePerGramPiasters,
        totalPiasters = totalPiasters,
        paymentSource = paymentSource,
        status = status,
        ledgerEntryId = ledgerEntryId,
        createdAtMs = createdAt?.toDate()?.time ?: 0L,
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "customerUid" to customerUid,
        "karat" to karat,
        "weightMg" to weightMg,
        "pricePerGramPiasters" to pricePerGramPiasters,
        "totalPiasters" to totalPiasters,
        "paymentSource" to paymentSource,
        "status" to status,
        "ledgerEntryId" to ledgerEntryId,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class WithdrawalRequestRecord(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val customerEmail: String = "",
    val karat: Long = 24L,
    val amountMg: Long = 0L,
    val deliveryType: String = WithdrawalDeliveryType.PICKUP_HUB.name,
    val deliveryAddress: String = "",
    val withdrawalRef: String = "",
    val status: String = WithdrawalStatus.PENDING.name,
    val proofNote: String? = null,
    val adminUid: String? = null,
    val ledgerEntryId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): WithdrawalRequest = WithdrawalRequest(
        id = id,
        customerUid = customerUid,
        customerEmail = customerEmail,
        karat = karat.toInt(),
        amountMg = amountMg,
        deliveryType = enumValues<WithdrawalDeliveryType>().firstOrNull { it.name == deliveryType }
            ?: WithdrawalDeliveryType.PICKUP_HUB,
        deliveryAddress = deliveryAddress,
        withdrawalRef = withdrawalRef,
        status = enumValues<WithdrawalStatus>().firstOrNull { it.name == status }
            ?: WithdrawalStatus.PENDING,
        proofNote = proofNote,
        adminUid = adminUid,
        ledgerEntryId = ledgerEntryId,
        createdAtMs = createdAt?.toDate()?.time ?: 0L,
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "customerUid" to customerUid,
        "customerEmail" to customerEmail,
        "karat" to karat,
        "amountMg" to amountMg,
        "deliveryType" to deliveryType,
        "deliveryAddress" to deliveryAddress,
        "withdrawalRef" to withdrawalRef,
        "status" to WithdrawalStatus.PENDING.name,
        "ledgerEntryId" to ledgerEntryId,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class DepositRequestRecord(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val customerEmail: String = "",
    val amountPiasters: Long = 0L,
    val channel: String = "INSTAPAY",
    val paymentReference: String = "",
    val status: String = DepositStatus.PENDING.name,
    val adminUid: String? = null,
    val adminNote: String? = null,
    val ledgerEntryId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): DepositRequest = DepositRequest(
        id = id,
        customerUid = customerUid,
        customerEmail = customerEmail,
        amountPiasters = amountPiasters,
        channel = channel,
        paymentReference = paymentReference,
        status = enumValues<DepositStatus>().firstOrNull { it.name == status }
            ?: DepositStatus.PENDING,
        adminUid = adminUid,
        adminNote = adminNote,
        ledgerEntryId = ledgerEntryId,
        createdAtMs = createdAt?.toDate()?.time ?: 0L,
    )

    fun toCreateMap(): Map<String, Any> = mapOf(
        "customerUid" to customerUid,
        "customerEmail" to customerEmail,
        "amountPiasters" to amountPiasters,
        "channel" to channel,
        "paymentReference" to paymentReference,
        "status" to DepositStatus.PENDING.name,
        "ledgerEntryId" to ledgerEntryId,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class PlatformPricingConfigRecord(
    val reference24kPerGramPiasters: Long = 620000L,
    val reference21kPerGramPiasters: Long = 542500L,
    val reference18kPerGramPiasters: Long = 465000L,
    val valuation24kPerGramPiasters: Long = 615000L,
    val valuation21kPerGramPiasters: Long = 538000L,
    val valuation18kPerGramPiasters: Long = 461000L,
    val merchantPolicyNote: String = "جميع أسعار المنتجات المعروضة نهائية بدون مصنعية إضافية على المشتري",
    val updatedByAdminUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toDomain(): PlatformPricingConfig = PlatformPricingConfig(
        reference24kPerGramPiasters = reference24kPerGramPiasters,
        reference21kPerGramPiasters = reference21kPerGramPiasters,
        reference18kPerGramPiasters = reference18kPerGramPiasters,
        valuation24kPerGramPiasters = valuation24kPerGramPiasters,
        valuation21kPerGramPiasters = valuation21kPerGramPiasters,
        valuation18kPerGramPiasters = valuation18kPerGramPiasters,
        merchantPolicyNote = merchantPolicyNote,
        updatedByAdminUid = updatedByAdminUid,
        updatedAtMs = updatedAt?.toDate()?.time ?: 0L,
    )

    fun toWriteMap(): Map<String, Any> = mapOf(
        "reference24kPerGramPiasters" to reference24kPerGramPiasters,
        "reference21kPerGramPiasters" to reference21kPerGramPiasters,
        "reference18kPerGramPiasters" to reference18kPerGramPiasters,
        "valuation24kPerGramPiasters" to valuation24kPerGramPiasters,
        "valuation21kPerGramPiasters" to valuation21kPerGramPiasters,
        "valuation18kPerGramPiasters" to valuation18kPerGramPiasters,
        "merchantPolicyNote" to merchantPolicyNote,
        "updatedByAdminUid" to updatedByAdminUid,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class AuditLogFsRecord(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorRole: String = "",
    val action: String = "",
    val targetPath: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun toDomain(): AuditLogRecord = AuditLogRecord(
        id = id,
        actorUid = actorUid,
        actorRole = actorRole,
        action = action,
        targetPath = targetPath,
        details = details,
        createdAtMs = createdAt?.toDate()?.time ?: 0L,
    )
}
