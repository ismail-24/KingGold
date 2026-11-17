package com.example

import com.example.model.MerchantPrivateDocs
import com.example.model.MerchantStatus
import com.example.model.MerchantStore
import com.example.ui.state.AdminApprovalViewModel
import com.example.ui.state.MerchantApprovalReviewer
import com.example.ui.state.MerchantKycLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminApprovalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sampleMerchant(
        id: String,
        shopName: String,
        status: MerchantStatus = MerchantStatus.PENDING,
    ) = MerchantStore(
        id = id,
        ownerUid = "owner_$id",
        shopName = shopName,
        area = "الصاغة",
        address = "12 شارع المعز",
        contactPhone = "+201001234567",
        verifiedEmail = "$id@gold.eg",
        description = "تجارة سبائك",
        status = status,
    )

    @Test
    fun nonAdminUser_seesUnauthorizedState_andCannotReviewMerchants() = runTest {
        var reviewCalled = false
        val pendingMerchant = sampleMerchant("m1", "متجر الذهب")
        val vm = AdminApprovalViewModel(
            isAdmin = false,
            pendingMerchantsFlow = MutableStateFlow(listOf(pendingMerchant)),
            reviewer = MerchantApprovalReviewer { _, _, _ ->
                reviewCalled = true
                Result.success(Unit)
            },
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isAdmin)
        assertTrue(vm.uiState.value.pendingMerchants.isEmpty())

        vm.approveMerchant(pendingMerchant)
        advanceUntilIdle()

        assertFalse(reviewCalled)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun adminObservePendingMerchants_filtersStrictlyToPending_andApproveUpdatesFirestore() = runTest {
        val mPending = sampleMerchant("m1", "مجوهرات النور", MerchantStatus.PENDING)
        val mAlreadyApproved = sampleMerchant("m2", "دار السبائك", MerchantStatus.APPROVED)
        val stream = MutableStateFlow(listOf(mPending, mAlreadyApproved))

        var reviewedId = ""
        var reviewedStatus: MerchantStatus? = null
        val vm = AdminApprovalViewModel(
            isAdmin = true,
            pendingMerchantsFlow = stream,
            reviewer = MerchantApprovalReviewer { merchant, newStatus, _ ->
                reviewedId = merchant.id
                reviewedStatus = newStatus
                stream.value = listOf(merchant.copy(status = newStatus), mAlreadyApproved)
                Result.success(Unit)
            },
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf("m1"), vm.uiState.value.pendingMerchants.map { it.id })

        vm.approveMerchant(mPending)
        advanceUntilIdle()

        assertEquals("m1", reviewedId)
        assertEquals(MerchantStatus.APPROVED, reviewedStatus)
        assertTrue(vm.uiState.value.pendingMerchants.isEmpty())
        assertTrue(vm.uiState.value.processingMerchantIds.isEmpty())
        assertNotNull(vm.uiState.value.bannerMessage)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun rejectMerchant_requiresNonBlankReason_andUpdatesStatusToRejected() = runTest {
        val mPending = sampleMerchant("m3", "ذهب الحسين", MerchantStatus.PENDING)
        val stream = MutableStateFlow(listOf(mPending))
        var capturedStatus: MerchantStatus? = null
        var capturedReason = ""

        val vm = AdminApprovalViewModel(
            isAdmin = true,
            pendingMerchantsFlow = stream,
            reviewer = MerchantApprovalReviewer { merchant, newStatus, reason ->
                capturedStatus = newStatus
                capturedReason = reason
                stream.value = emptyList()
                Result.success(Unit)
            },
        )
        advanceUntilIdle()

        // Too-short reason blocked cleanly
        vm.rejectMerchant(mPending, "  لا ")
        advanceUntilIdle()
        assertNull(capturedStatus)
        assertNotNull(vm.uiState.value.errorMessage)

        // Valid reason proceeds
        vm.rejectMerchant(mPending, "  صورة السجل التجاري غير واضحة  ")
        advanceUntilIdle()
        assertEquals(MerchantStatus.REJECTED, capturedStatus)
        assertEquals("صورة السجل التجاري غير واضحة", capturedReason)
        assertTrue(vm.uiState.value.pendingMerchants.isEmpty())
        assertNotNull(vm.uiState.value.bannerMessage)
    }

    @Test
    fun loadMerchantKycDocs_cachesPrivateDocs_andReviewerFailureResetsProcessingState() = runTest {
        val mPending = sampleMerchant("m4", "بيت الذهب", MerchantStatus.PENDING)
        val vm = AdminApprovalViewModel(
            isAdmin = true,
            pendingMerchantsFlow = MutableStateFlow(listOf(mPending)),
            reviewer = MerchantApprovalReviewer { _, _, _ ->
                Result.failure(IllegalStateException("PERMISSION_DENIED"))
            },
            kycLoader = MerchantKycLoader { merchantId ->
                Result.success(
                    MerchantPrivateDocs(
                        ownerUid = "owner_$merchantId",
                        legalFullName = "خالد عبد الرحمن",
                        nationalIdNumber = "29103031234567",
                    ),
                )
            },
        )
        advanceUntilIdle()

        vm.loadMerchantKycDocs("m4")
        advanceUntilIdle()
        val cached = vm.uiState.value.merchantDocsById["m4"]
        assertNotNull(cached)
        assertEquals("خالد عبد الرحمن", cached!!.legalFullName)

        vm.approveMerchant(mPending)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.processingMerchantIds.isEmpty())
        assertTrue(vm.uiState.value.errorMessage!!.contains("PERMISSION_DENIED"))
    }
}
