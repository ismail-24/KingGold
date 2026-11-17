package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.model.MerchantPrivateDocs
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun interface MerchantApprovalReviewer {
    suspend fun review(
        merchant: MerchantStore,
        newStatus: MerchantStatus,
        reason: String,
    ): Result<Unit>
}

fun interface MerchantKycLoader {
    suspend fun load(merchantId: String): Result<MerchantPrivateDocs?>
}

data class AdminApprovalUiState(
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val pendingMerchants: List<MerchantStore> = emptyList(),
    val processingMerchantIds: Set<String> = emptySet(),
    val merchantDocsById: Map<String, MerchantPrivateDocs> = emptyMap(),
    val loadingDocsMerchantIds: Set<String> = emptySet(),
    val bannerMessage: String? = null,
    val errorMessage: String? = null,
)

class AdminApprovalViewModel(
    private val isAdmin: Boolean = false,
    pendingMerchantsFlow: Flow<List<MerchantStore>> = emptyFlow(),
    private val reviewer: MerchantApprovalReviewer? = null,
    private val kycLoader: MerchantKycLoader? = null,
) : ViewModel() {

    constructor(
        marketplaceRepository: FirestoreMarketplaceRepository,
        isAdmin: Boolean,
    ) : this(
        isAdmin = isAdmin,
        pendingMerchantsFlow = if (isAdmin) marketplaceRepository.observePendingMerchantsAdmin() else emptyFlow(),
        reviewer = MerchantApprovalReviewer { merchant, newStatus, reason ->
            marketplaceRepository.adminReviewMerchant(
                merchant = merchant,
                newStatus = newStatus,
                prepaidApproved = newStatus == MerchantStatus.APPROVED,
                prepaidBudgetPiasters = 0L,
                reasonOrNote = reason,
            )
        },
        kycLoader = MerchantKycLoader { merchantId ->
            marketplaceRepository.getMerchantPrivateDocs(merchantId)
        },
    )

    private val _uiState = MutableStateFlow(
        AdminApprovalUiState(
            isAdmin = isAdmin,
            isLoading = isAdmin,
        ),
    )
    val uiState: StateFlow<AdminApprovalUiState> = _uiState.asStateFlow()

    init {
        if (isAdmin) {
            viewModelScope.launch {
                pendingMerchantsFlow
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Log.w(TAG, "Failed to observe pending merchants", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.localizedMessage ?: "تعذّر تحميل طلبات التجار قيد المراجعة.",
                            )
                        }
                    }
                    .collect { merchants ->
                        val pendingOnly = merchants.filter { it.status == MerchantStatus.PENDING }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingMerchants = pendingOnly,
                            )
                        }
                    }
            }
        }
    }

    fun loadMerchantKycDocs(merchantId: String) {
        val state = _uiState.value
        if (!state.isAdmin || state.merchantDocsById.containsKey(merchantId) || state.loadingDocsMerchantIds.contains(merchantId)) {
            return
        }
        val loader = kycLoader ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(loadingDocsMerchantIds = it.loadingDocsMerchantIds + merchantId)
            }
            loader.load(merchantId)
                .onSuccess { docs ->
                    _uiState.update { current ->
                        val nextDocs = if (docs != null) {
                            current.merchantDocsById + (merchantId to docs)
                        } else {
                            current.merchantDocsById
                        }
                        current.copy(
                            merchantDocsById = nextDocs,
                            loadingDocsMerchantIds = current.loadingDocsMerchantIds - merchantId,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loadingDocsMerchantIds = it.loadingDocsMerchantIds - merchantId,
                            errorMessage = error.localizedMessage ?: "تعذّر تحميل مستندات التاجر.",
                        )
                    }
                }
        }
    }

    fun approveMerchant(
        merchant: MerchantStore,
        note: String = "تمت مراجعة البيانات واعتماد المتجر على المنصة",
    ) {
        reviewMerchant(
            merchant = merchant,
            targetStatus = MerchantStatus.APPROVED,
            reasonOrNote = note.trim().ifBlank { "معتمد من الإدارة" },
            successMessage = "تم اعتماد التاجر «${merchant.shopName}» وتفعيل متجره.",
        )
    }

    fun rejectMerchant(
        merchant: MerchantStore,
        reason: String,
    ) {
        val trimmedReason = reason.trim()
        if (trimmedReason.length < 3) {
            _uiState.update {
                it.copy(errorMessage = "يرجى إدخال سبب واضح للرفض (3 أحرف على الأقل).")
            }
            return
        }
        reviewMerchant(
            merchant = merchant,
            targetStatus = MerchantStatus.REJECTED,
            reasonOrNote = trimmedReason,
            successMessage = "تم رفض طلب التاجر «${merchant.shopName}».",
        )
    }

    private fun reviewMerchant(
        merchant: MerchantStore,
        targetStatus: MerchantStatus,
        reasonOrNote: String,
        successMessage: String,
    ) {
        if (!_uiState.value.isAdmin) {
            _uiState.update {
                it.copy(errorMessage = "هذا الإجراء متاح لحسابات الإدارة المعتمدة فقط.")
            }
            return
        }
        if (_uiState.value.processingMerchantIds.contains(merchant.id)) return
        val activeReviewer = reviewer ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    processingMerchantIds = it.processingMerchantIds + merchant.id,
                    errorMessage = null,
                    bannerMessage = null,
                )
            }
            activeReviewer.review(merchant, targetStatus, reasonOrNote)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            processingMerchantIds = it.processingMerchantIds - merchant.id,
                            bannerMessage = successMessage,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            processingMerchantIds = it.processingMerchantIds - merchant.id,
                            errorMessage = error.localizedMessage ?: "تعذّر تحديث حالة التاجر في Firestore.",
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(bannerMessage = null, errorMessage = null) }
    }

    companion object {
        private const val TAG = "AdminApprovalVM"
    }
}
