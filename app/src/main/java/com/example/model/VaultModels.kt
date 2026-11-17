package com.example.model

enum class SellRequestStatus(val arabicLabel: String) {
    SUBMITTED("تم الإرسال"),
    UNDER_REVIEW("قيد الفحص"),
    OFFER_SENT("عرض شراء متاح"),
    COMPLETED("مكتمل ومُودع بالمحفظة"),
    REJECTED("مرفوض"),
    CANCELLED("ملغي"),
}

enum class WithdrawalStatus(val arabicLabel: String) {
    PENDING("قيد المراجعة"),
    APPROVED("موافق عليه"),
    FULFILLED("تم التسليم"),
    REJECTED("مرفوض وتم رد الرصيد"),
}

enum class DepositStatus(val arabicLabel: String) {
    PENDING("قيد المراجعة"),
    APPROVED("معتمد ومُضاف للمحفظة"),
    REJECTED("مرفوض"),
}

enum class WithdrawalDeliveryType(val arabicLabel: String) {
    PICKUP_HUB("استلام من مركز التوثيق"),
    INSURED_COURIER("شحن مؤمّن حتى الباب"),
}

enum class LedgerTxType(val arabicLabel: String) {
    GOLD_SALE_PROCEEDS("عائد بيع ذهب للمنصة"),
    CASH_DEPOSIT_PENDING("طلب إيداع قيد المراجعة"),
    CASH_DEPOSIT("إيداع نقدي معتمد"),
    FRACTIONAL_GOLD_BUY("شراء ذهب جزئي"),
    FRACTIONAL_GOLD_PENDING("طلب شراء ذهب جزئي معلق"),
    MARKETPLACE_ORDER_PAID("دفع طلب متجر من المحفظة"),
    GOLD_RESERVATION("حجز ذهب للسحب الفعلي"),
    GOLD_WITHDRAWAL_FULFILLED("تسليم ذهب فعلي"),
    GOLD_RESERVATION_RELEASED("إلغاء حجز سحب"),
    ADMIN_ADJUSTMENT("تسوية إدارية"),
}

data class SellGoldRequest(
    val id: String = "",
    val customerUid: String = "",
    val customerEmail: String = "",
    val itemTitle: String = "",
    val category: String = "مشغولات ذهبية",
    val karat: Int = 21,
    val estimatedWeightMg: Long = 0L,
    val conditionNotes: String = "",
    val photoThumbnails: List<String> = emptyList(),
    val status: SellRequestStatus = SellRequestStatus.SUBMITTED,
    val evaluatorAdminUid: String? = null,
    val verifiedWeightMg: Long = 0L,
    val confirmedKarat: Int = 21,
    val referencePricePerGramPiasters: Long = 0L,
    val evaluatedGoldValuePiasters: Long = 0L,
    val makingChargeDeductionPiasters: Long = 0L,
    val finalOfferPiasters: Long = 0L,
    val inspectionNotes: String? = null,
    val rejectionReason: String? = null,
    val settlementTransactionId: String? = null,
    val createdAtMs: Long = 0L,
)

data class WalletSummary(
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
) {
    val goldAvailableMg: Long get() = (goldConfirmedMg - goldReservedMg).coerceAtLeast(0L)

    fun confirmedMgForKarat(karat: Int): Long = when (karat) {
        24 -> gold24kMg
        21 -> gold21kMg
        18 -> gold18kMg
        else -> 0L
    }
}

data class LedgerEntry(
    val id: String = "",
    val userId: String = "",
    val idempotencyKey: String = "",
    val txType: LedgerTxType = LedgerTxType.ADMIN_ADJUSTMENT,
    val referenceId: String = "",
    val karat: Int = 0,
    val cashDeltaPiasters: Long = 0L,
    val pendingCashDeltaPiasters: Long = 0L,
    val goldDeltaMg: Long = 0L,
    val reservedDeltaMg: Long = 0L,
    val pendingGoldDeltaMg: Long = 0L,
    val unitPricePerGramPiasters: Long = 0L,
    val notes: String = "",
    val createdAtMs: Long = 0L,
)

data class FractionalGoldOrder(
    val id: String = "",
    val customerUid: String = "",
    val karat: Int = 24,
    val weightMg: Long = 0L,
    val pricePerGramPiasters: Long = 0L,
    val totalPiasters: Long = 0L,
    val paymentSource: String = "CASH_WALLET",
    val status: String = "CONFIRMED",
    val ledgerEntryId: String = "",
    val createdAtMs: Long = 0L,
)

data class WithdrawalRequest(
    val id: String = "",
    val customerUid: String = "",
    val customerEmail: String = "",
    val karat: Int = 24,
    val amountMg: Long = 0L,
    val deliveryType: WithdrawalDeliveryType = WithdrawalDeliveryType.PICKUP_HUB,
    val deliveryAddress: String = "",
    val withdrawalRef: String = "",
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val proofNote: String? = null,
    val adminUid: String? = null,
    val ledgerEntryId: String = "",
    val createdAtMs: Long = 0L,
)

data class DepositRequest(
    val id: String = "",
    val customerUid: String = "",
    val customerEmail: String = "",
    val amountPiasters: Long = 0L,
    val channel: String = "INSTAPAY",
    val paymentReference: String = "",
    val status: DepositStatus = DepositStatus.PENDING,
    val adminUid: String? = null,
    val adminNote: String? = null,
    val ledgerEntryId: String = "",
    val createdAtMs: Long = 0L,
)

data class PlatformPricingConfig(
    val reference24kPerGramPiasters: Long = 620000L,
    val reference21kPerGramPiasters: Long = 542500L,
    val reference18kPerGramPiasters: Long = 465000L,
    val valuation24kPerGramPiasters: Long = 615000L,
    val valuation21kPerGramPiasters: Long = 538000L,
    val valuation18kPerGramPiasters: Long = 461000L,
    val merchantPolicyNote: String = "جميع أسعار المنتجات المعروضة نهائية بدون مصنعية إضافية على المشتري",
    val updatedByAdminUid: String = "",
    val updatedAtMs: Long = 0L,
) {
    fun referencePriceForKarat(karat: Int): Long = when (karat) {
        24 -> reference24kPerGramPiasters
        21 -> reference21kPerGramPiasters
        18 -> reference18kPerGramPiasters
        else -> reference21kPerGramPiasters
    }

    fun valuationPriceForKarat(karat: Int): Long = when (karat) {
        24 -> valuation24kPerGramPiasters
        21 -> valuation21kPerGramPiasters
        18 -> valuation18kPerGramPiasters
        else -> valuation21kPerGramPiasters
    }
}

data class AuditLogRecord(
    val id: String = "",
    val actorUid: String = "",
    val actorRole: String = "",
    val action: String = "",
    val targetPath: String = "",
    val details: String = "",
    val createdAtMs: Long = 0L,
)
