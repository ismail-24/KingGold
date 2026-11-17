package com.example

import com.example.model.MerchantStatus
import com.example.ui.state.MerchantRegistrationField
import com.example.ui.state.MerchantRegistrationSubmissionState
import com.example.ui.state.MerchantRegistrationSubmitter
import com.example.ui.state.MerchantRegistrationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MerchantRegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_defaultsToPendingStatus_andPrefillsEmail() {
        val vm = MerchantRegistrationViewModel(initialEmail = "  merchant@kinggold.eg  ")
        val state = vm.uiState.value

        assertEquals(MerchantStatus.PENDING, state.pendingStatus)
        assertEquals("merchant@kinggold.eg", state.contactEmail)
        assertTrue(state.fieldErrors.isEmpty())
        assertEquals(MerchantRegistrationSubmissionState.Idle, state.submissionState)
    }

    @Test
    fun submitRegistration_withMissingFields_setsValidationErrorsAndBlocksSubmitter() = runTest {
        var submitCalled = false
        val vm = MerchantRegistrationViewModel(
            submitter = MerchantRegistrationSubmitter { _, _, _, _, _, _, _, _, _, _, _, _ ->
                submitCalled = true
                Result.success("m_should_not_run")
            },
        )

        vm.updateLegalName("أب")
        vm.updateBusinessId("123")
        vm.updateShopName("م")
        vm.updateArea("ق")
        vm.updateShopAddress("شار")
        vm.updateContactPhone("123")
        vm.updateContactEmail("bad-email")

        vm.submitRegistration()
        advanceUntilIdle()

        assertFalse(submitCalled)
        val state = vm.uiState.value
        assertEquals(7, state.fieldErrors.size)
        assertTrue(state.fieldErrors.containsKey(MerchantRegistrationField.LEGAL_NAME))
        assertTrue(state.fieldErrors.containsKey(MerchantRegistrationField.BUSINESS_ID))
        assertTrue(state.fieldErrors.containsKey(MerchantRegistrationField.SHOP_NAME))
        assertTrue(state.fieldErrors.containsKey(MerchantRegistrationField.AREA))
        assertTrue(state.fieldErrors.containsKey(MerchantRegistrationField.SHOP_ADDRESS))
        assertTrue(state.fieldErrors.containsKey(MerchantRegistrationField.CONTACT_PHONE))
        assertTrue(state.fieldErrors.containsKey(MerchantRegistrationField.CONTACT_EMAIL))
        assertTrue(state.submissionState is MerchantRegistrationSubmissionState.Error)

        // Fixing one field clears only that field's error and clears error banner
        vm.updateLegalName("أحمد محمود عبد الله")
        val updated = vm.uiState.value
        assertNull(updated.fieldErrors[MerchantRegistrationField.LEGAL_NAME])
        assertTrue(updated.fieldErrors.containsKey(MerchantRegistrationField.BUSINESS_ID))
        assertEquals(MerchantRegistrationSubmissionState.Idle, updated.submissionState)
    }

    @Test
    fun submitRegistration_validData_submitsTrimmedFields_andReturnsPendingSuccess() = runTest {
        var capturedShopName = ""
        var capturedAddress = ""
        var capturedPhone = ""
        var capturedEmail = ""
        var capturedLegal = ""
        var capturedBusinessId = ""

        val vm = MerchantRegistrationViewModel(
            initialEmail = "owner@goldshop.eg",
            submitter = MerchantRegistrationSubmitter { shopName, _, address, contactPhone, verifiedEmail, _, legalFullName, nationalIdNumber, _, _, _, _ ->
                capturedShopName = shopName
                capturedAddress = address
                capturedPhone = contactPhone
                capturedEmail = verifiedEmail
                capturedLegal = legalFullName
                capturedBusinessId = nationalIdNumber
                Result.success("m_uid_1700000000")
            },
        )

        vm.updateLegalName("  أحمد محمود الشريف  ")
        vm.updateBusinessId("  CR-295010112345  ")
        vm.updateCommercialRegNumber("REG-9981")
        vm.updateTaxCardNumber("TAX-4412")
        vm.updateShopName("  مجوهرات الشريف  ")
        vm.updateArea("  خان الخليلي  ")
        vm.updateShopAddress("  14 شارع الصاغة، الجمالية، القاهرة  ")
        vm.updateContactPhone("  +201001234567  ")
        vm.updateDescription("سبائك وعملات ذهبية مدموغة")

        vm.submitRegistration()
        advanceUntilIdle()

        assertEquals("مجوهرات الشريف", capturedShopName)
        assertEquals("14 شارع الصاغة، الجمالية، القاهرة", capturedAddress)
        assertEquals("+201001234567", capturedPhone)
        assertEquals("owner@goldshop.eg", capturedEmail)
        assertEquals("أحمد محمود الشريف", capturedLegal)
        assertEquals("CR-295010112345", capturedBusinessId)

        val submission = vm.uiState.value.submissionState
        assertTrue(submission is MerchantRegistrationSubmissionState.Success)
        val success = submission as MerchantRegistrationSubmissionState.Success
        assertEquals("m_uid_1700000000", success.merchantId)
        assertEquals("مجوهرات الشريف", success.shopName)
        assertEquals(MerchantStatus.PENDING, success.status)
    }

    @Test
    fun submitRegistration_whenRepositoryFails_surfacesError_andPreservesFormInput() = runTest {
        val vm = MerchantRegistrationViewModel(
            initialEmail = "owner@goldshop.eg",
            submitter = MerchantRegistrationSubmitter { _, _, _, _, _, _, _, _, _, _, _, _ ->
                Result.failure(IllegalStateException("PERMISSION_DENIED: Firestore rules check"))
            },
        )

        vm.updateLegalName("سارة علي محمد")
        vm.updateBusinessId("29805051234567")
        vm.updateShopName("دار السبائك الذهبية")
        vm.updateArea("المعادي")
        vm.updateShopAddress("25 شارع 9، المعادي، القاهرة")
        vm.updateContactPhone("01012345678")

        vm.submitRegistration()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.submissionState is MerchantRegistrationSubmissionState.Error)
        assertTrue((state.submissionState as MerchantRegistrationSubmissionState.Error).message.contains("PERMISSION_DENIED"))
        assertEquals("سارة علي محمد", state.legalName)
        assertEquals("دار السبائك الذهبية", state.shopName)
    }
}
