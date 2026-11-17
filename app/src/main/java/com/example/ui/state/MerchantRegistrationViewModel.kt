package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MerchantRegistrationField {
    LEGAL_NAME,
    BUSINESS_ID,
    SHOP_NAME,
    AREA,
    SHOP_ADDRESS,
    CONTACT_PHONE,
    CONTACT_EMAIL,
}

sealed interface MerchantRegistrationSubmissionState {
    data object Idle : MerchantRegistrationSubmissionState
    data object Submitting : MerchantRegistrationSubmissionState
    data class Success(
        val merchantId: String,
        val shopName: String,
        val status: MerchantStatus = MerchantStatus.PENDING,
    ) : MerchantRegistrationSubmissionState

    data class Error(val message: String) : MerchantRegistrationSubmissionState
}

fun interface MerchantRegistrationSubmitter {
    suspend fun submit(
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
    ): Result<String>
}

data class MerchantRegistrationUiState(
    val legalName: String = "",
    val businessId: String = "",
    val commercialRegNumber: String = "",
    val taxCardNumber: String = "",
    val shopName: String = "",
    val area: String = "",
    val shopAddress: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val description: String = "",
    val authorizationNotes: String = "",
    val documentThumbnails: List<String> = emptyList(),
    val pendingStatus: MerchantStatus = MerchantStatus.PENDING,
    val existingMerchants: List<MerchantStore> = emptyList(),
    val fieldErrors: Map<MerchantRegistrationField, String> = emptyMap(),
    val submissionState: MerchantRegistrationSubmissionState = MerchantRegistrationSubmissionState.Idle,
) {
    val isSubmitting: Boolean
        get() = submissionState is MerchantRegistrationSubmissionState.Submitting
}

class MerchantRegistrationViewModel(
    private val marketplaceRepository: FirestoreMarketplaceRepository? = null,
    private val currentUserId: String? = null,
    initialEmail: String = "",
    private val submitter: MerchantRegistrationSubmitter? = marketplaceRepository?.let { repo ->
        MerchantRegistrationSubmitter { shopName, area, address, contactPhone, verifiedEmail, description, legalFullName, nationalIdNumber, commercialRegNumber, taxCardNumber, authorizationNotes, documentThumbnails ->
            repo.submitMerchantRegistration(
                shopName = shopName,
                area = area,
                address = address,
                contactPhone = contactPhone,
                verifiedEmail = verifiedEmail,
                description = description,
                legalFullName = legalFullName,
                nationalIdNumber = nationalIdNumber,
                commercialRegNumber = commercialRegNumber,
                taxCardNumber = taxCardNumber,
                authorizationNotes = authorizationNotes,
                documentThumbnails = documentThumbnails,
            )
        }
    },
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MerchantRegistrationUiState(
            contactEmail = initialEmail.trim(),
        ),
    )
    val uiState: StateFlow<MerchantRegistrationUiState> = _uiState.asStateFlow()

    init {
        val repo = marketplaceRepository
        val uid = currentUserId?.takeIf { it.isNotBlank() }
        if (repo != null && uid != null) {
            viewModelScope.launch {
                repo.observeOwnMerchants(uid)
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Log.w(TAG, "Failed to observe own merchant applications", error)
                    }
                    .collect { merchants ->
                        _uiState.update { it.copy(existingMerchants = merchants) }
                    }
            }
        }
    }

    fun updateLegalName(value: String) {
        _uiState.update {
            it.copy(
                legalName = value,
                fieldErrors = it.fieldErrors - MerchantRegistrationField.LEGAL_NAME,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateBusinessId(value: String) {
        _uiState.update {
            it.copy(
                businessId = value,
                fieldErrors = it.fieldErrors - MerchantRegistrationField.BUSINESS_ID,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateCommercialRegNumber(value: String) {
        _uiState.update {
            it.copy(
                commercialRegNumber = value,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateTaxCardNumber(value: String) {
        _uiState.update {
            it.copy(
                taxCardNumber = value,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateShopName(value: String) {
        _uiState.update {
            it.copy(
                shopName = value,
                fieldErrors = it.fieldErrors - MerchantRegistrationField.SHOP_NAME,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateArea(value: String) {
        _uiState.update {
            it.copy(
                area = value,
                fieldErrors = it.fieldErrors - MerchantRegistrationField.AREA,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateShopAddress(value: String) {
        _uiState.update {
            it.copy(
                shopAddress = value,
                fieldErrors = it.fieldErrors - MerchantRegistrationField.SHOP_ADDRESS,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateContactPhone(value: String) {
        _uiState.update {
            it.copy(
                contactPhone = value,
                fieldErrors = it.fieldErrors - MerchantRegistrationField.CONTACT_PHONE,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateContactEmail(value: String) {
        _uiState.update {
            it.copy(
                contactEmail = value,
                fieldErrors = it.fieldErrors - MerchantRegistrationField.CONTACT_EMAIL,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateDescription(value: String) {
        _uiState.update {
            it.copy(
                description = value,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateAuthorizationNotes(value: String) {
        _uiState.update {
            it.copy(
                authorizationNotes = value,
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun updateDocumentThumbnails(thumbnails: List<String>) {
        _uiState.update {
            it.copy(
                documentThumbnails = thumbnails.take(4),
                submissionState = clearErrorState(it.submissionState),
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(submissionState = clearErrorState(it.submissionState))
        }
    }

    fun resetForAnotherApplication() {
        val currentEmail = _uiState.value.contactEmail
        val existing = _uiState.value.existingMerchants
        _uiState.value = MerchantRegistrationUiState(
            contactEmail = currentEmail,
            existingMerchants = existing,
        )
    }

    fun submitRegistration() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting) return

        val errors = validate(snapshot)
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    fieldErrors = errors,
                    submissionState = MerchantRegistrationSubmissionState.Error(
                        "يرجى مراجعة الحقول المطلوبة واستكمال البيانات بشكل صحيح.",
                    ),
                )
            }
            return
        }

        val activeSubmitter = submitter
        if (activeSubmitter == null) {
            _uiState.update {
                it.copy(
                    submissionState = MerchantRegistrationSubmissionState.Error(
                        "خدمة التسجيل غير متاحة حالياً. يرجى إعادة المحاولة بعد تسجيل الدخول.",
                    ),
                )
            }
            return
        }

        val shopNameTrimmed = snapshot.shopName.trim()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    fieldErrors = emptyMap(),
                    submissionState = MerchantRegistrationSubmissionState.Submitting,
                )
            }
            activeSubmitter.submit(
                shopName = shopNameTrimmed,
                area = snapshot.area.trim(),
                address = snapshot.shopAddress.trim(),
                contactPhone = snapshot.contactPhone.trim(),
                verifiedEmail = snapshot.contactEmail.trim(),
                description = snapshot.description.trim(),
                legalFullName = snapshot.legalName.trim(),
                nationalIdNumber = snapshot.businessId.trim(),
                commercialRegNumber = snapshot.commercialRegNumber.trim(),
                taxCardNumber = snapshot.taxCardNumber.trim(),
                authorizationNotes = snapshot.authorizationNotes.trim(),
                documentThumbnails = snapshot.documentThumbnails.take(4),
            ).onSuccess { merchantId ->
                _uiState.update {
                    it.copy(
                        submissionState = MerchantRegistrationSubmissionState.Success(
                            merchantId = merchantId,
                            shopName = shopNameTrimmed,
                            status = MerchantStatus.PENDING,
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        submissionState = MerchantRegistrationSubmissionState.Error(
                            error.localizedMessage ?: "تعذّر إرسال طلب تسجيل التاجر إلى الخادم.",
                        ),
                    )
                }
            }
        }
    }

    internal fun validate(state: MerchantRegistrationUiState): Map<MerchantRegistrationField, String> {
        val errors = mutableMapOf<MerchantRegistrationField, String>()
        val legalName = state.legalName.trim()
        if (legalName.length < 3 || legalName.length > 160) {
            errors[MerchantRegistrationField.LEGAL_NAME] = "يرجى إدخال الاسم القانوني الكامل (3 إلى 160 حرفاً)"
        }

        val businessId = state.businessId.trim()
        if (businessId.length < 5 || businessId.length > 64) {
            errors[MerchantRegistrationField.BUSINESS_ID] = "يرجى إدخال رقم الهوية التجارية أو القومية (5 أحرف على الأقل)"
        }

        val shopName = state.shopName.trim()
        if (shopName.length < 2 || shopName.length > 120) {
            errors[MerchantRegistrationField.SHOP_NAME] = "يرجى إدخال اسم محل المجوهرات (حرفان على الأقل)"
        }

        val area = state.area.trim()
        if (area.length < 2 || area.length > 80) {
            errors[MerchantRegistrationField.AREA] = "يرجى إدخال المنطقة أو المدينة (حرفان على الأقل)"
        }

        val shopAddress = state.shopAddress.trim()
        if (shopAddress.length < 4 || shopAddress.length > 240) {
            errors[MerchantRegistrationField.SHOP_ADDRESS] = "يرجى إدخال عنوان المحل التفصيلي (4 أحرف على الأقل)"
        }

        val phone = state.contactPhone.trim()
        if (phone.length < 6 || phone.length > 32) {
            errors[MerchantRegistrationField.CONTACT_PHONE] = "يرجى إدخال رقم هاتف تواصل صحيح (6 أرقام على الأقل)"
        }

        val email = state.contactEmail.trim()
        if (email.length < 3 || email.length > 160 || !email.contains("@") || !email.contains(".")) {
            errors[MerchantRegistrationField.CONTACT_EMAIL] = "يرجى إدخال بريد إلكتروني صحيح للتواصل"
        }

        return errors
    }

    private fun clearErrorState(current: MerchantRegistrationSubmissionState): MerchantRegistrationSubmissionState =
        if (current is MerchantRegistrationSubmissionState.Error) MerchantRegistrationSubmissionState.Idle else current

    companion object {
        private const val TAG = "MerchantRegVM"
    }
}
