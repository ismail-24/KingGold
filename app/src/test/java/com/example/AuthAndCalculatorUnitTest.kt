package com.example

import com.example.data.KingGoldRepository
import com.example.model.Shop
import com.example.ui.auth.AuthMode
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.FirebaseAuthGateway
import com.example.ui.components.calculateGoldMarketValue
import com.example.ui.components.defaultCenterForShops
import com.example.ui.components.partitionShopsByCoordinates
import com.example.ui.state.KingGoldViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthAndCalculatorUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAuthGateway(
        var googleResult: Result<String> = Result.success("uid-google"),
        var emailSignInResult: Result<String> = Result.success("uid-email"),
        var emailSignUpResult: Result<String> = Result.success("uid-signup"),
    ) : FirebaseAuthGateway {
        var lastGoogleToken: String? = null
        var lastEmail: String? = null
        var lastFullName: String? = null

        override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {
            lastGoogleToken = idToken
            return googleResult
        }

        override suspend fun signInWithEmail(email: String, password: String): Result<String> {
            lastEmail = email
            return emailSignInResult
        }

        override suspend fun signUpWithEmail(fullName: String, email: String, password: String): Result<String> {
            lastFullName = fullName
            lastEmail = email
            return emailSignUpResult
        }

        override fun hasActiveUser(): Boolean = false
    }

    @Test
    fun authViewModel_exchangeGoogleIdToken_succeedsAndSetsAuthenticated() = runTest {
        val gateway = FakeAuthGateway()
        val vm = AuthViewModel(authGateway = gateway)

        val success = vm.exchangeGoogleIdTokenWithFirebase("valid.google.id.token", silent = false)

        assertTrue(success)
        assertEquals("valid.google.id.token", gateway.lastGoogleToken)
        assertTrue(vm.uiState.value.isAuthenticated)
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun authViewModel_blankGoogleToken_setsErrorAndDoesNotAuthenticate() = runTest {
        val gateway = FakeAuthGateway()
        val vm = AuthViewModel(authGateway = gateway)

        val success = vm.exchangeGoogleIdTokenWithFirebase("   ", silent = false)

        assertFalse(success)
        assertFalse(vm.uiState.value.isAuthenticated)
        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(gateway.lastGoogleToken)
    }

    @Test
    fun authViewModel_googleGatewayFailure_mapsExceptionWithoutCrash() = runTest {
        val gateway = FakeAuthGateway(
            googleResult = Result.failure(IllegalStateException("SHA mismatch")),
        )
        val vm = AuthViewModel(authGateway = gateway)

        val success = vm.exchangeGoogleIdTokenWithFirebase("token-123", silent = false)

        assertFalse(success)
        assertFalse(vm.uiState.value.isAuthenticated)
        assertEquals("SHA mismatch", vm.uiState.value.errorMessage)
    }

    @Test
    fun authViewModel_traditionalSignInAndSignUp_validatesAndDelegates() = runTest {
        val gateway = FakeAuthGateway()
        val vm = AuthViewModel(authGateway = gateway)

        // Invalid email blocks submit
        vm.updateEmail("not-an-email")
        vm.updatePassword("123456")
        vm.submitTraditionalAuth()
        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(gateway.lastEmail)

        // Valid sign in succeeds
        vm.updateEmail("buyer@kinggold.eg")
        vm.updatePassword("secret123")
        vm.submitTraditionalAuth()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isAuthenticated)
        assertEquals("buyer@kinggold.eg", gateway.lastEmail)

        // Switch to sign up validates mismatched confirm password
        val regGateway = FakeAuthGateway()
        val regVm = AuthViewModel(authGateway = regGateway)
        regVm.switchMode(AuthMode.SIGN_UP)
        regVm.updateFullName("أحمد علي")
        regVm.updateEmail("ahmed@kinggold.eg")
        regVm.updatePassword("pass1234")
        regVm.updateConfirmPassword("different")
        regVm.submitTraditionalAuth()
        assertNotNull(regVm.uiState.value.errorMessage)
        assertNull(regGateway.lastEmail)

        // Matching confirm password delegates to signUpWithEmail
        regVm.updateConfirmPassword("pass1234")
        regVm.submitTraditionalAuth()
        advanceUntilIdle()
        assertTrue(regVm.uiState.value.isAuthenticated)
        assertEquals("أحمد علي", regGateway.lastFullName)
        assertEquals("ahmed@kinggold.eg", regGateway.lastEmail)
    }

    @Test
    fun goldCalculator_computesAccurateMarketValues_andClampsInvalidInputs() {
        val coin21k = calculateGoldMarketValue(
            weightGrams = 8.0,
            karat = 21,
            gramPriceEgp = 5425.0,
            makingFeePerGram = 90.0,
        )
        assertEquals(43400.0, coin21k.baseGoldValueEgp, 0.01)
        assertEquals(720.0, coin21k.totalMakingFeeEgp, 0.01)
        assertEquals(44120.0, coin21k.totalMarketValueEgp, 0.01)
        assertEquals(7.0, coin21k.pure24kEquivalentGrams, 0.001)

        val invalid = calculateGoldMarketValue(
            weightGrams = -5.0,
            karat = 24,
            gramPriceEgp = 6200.0,
            makingFeePerGram = -10.0,
        )
        assertEquals(0.0, invalid.weightGrams, 0.001)
        assertEquals(0.0, invalid.totalMarketValueEgp, 0.001)
    }

    @Test
    fun mapCoordinatePartition_andCitySearchFiltering_workAccurately() {
        val validShop = KingGoldRepository.shops.first()
        val invalidZero = validShop.copy(id = "bad_zero", latitude = 0.0, longitude = 0.0)
        val invalidNan = validShop.copy(id = "bad_nan", latitude = Double.NaN, longitude = 31.2)
        val partition = partitionShopsByCoordinates(listOf(validShop, invalidZero, invalidNan))

        assertEquals(1, partition.mappable.size)
        assertEquals(2, partition.unmappable.size)

        val center = defaultCenterForShops(listOf(validShop))
        assertEquals(validShop.latitude, center.latitude, 0.0001)
        assertEquals(validShop.longitude, center.longitude, 0.0001)

        val fallbackCenter = defaultCenterForShops(emptyList<Shop>())
        assertEquals(30.0444, fallbackCenter.latitude, 0.0001)

        val vm = KingGoldViewModel(autoFetchPrices = false)
        vm.updateSearchQuery("القاهرة")
        assertEquals(3, vm.uiState.value.filteredShops.size)

        vm.updateSearchQuery("مدينة غير موجودة")
        assertTrue(vm.uiState.value.filteredShops.isEmpty())

        vm.updateSearchQuery("النور")
        assertEquals(listOf("s1"), vm.uiState.value.filteredShops.map { it.id })
    }
}
