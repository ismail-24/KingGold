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

    @Test
    fun mapLayerToggle_switchesTileSourceAndAttributionCleanly() {
        val standardSource = com.example.ui.components.tileSourceForLayer(
            com.example.ui.components.MapLayerMode.STANDARD,
        )
        val satelliteSource = com.example.ui.components.tileSourceForLayer(
            com.example.ui.components.MapLayerMode.SATELLITE,
        )

        assertEquals("Mapnik", standardSource.name())
        assertEquals("EsriWorldImagery", satelliteSource.name())

        val sampleTile = org.osmdroid.util.MapTileIndex.getTileIndex(12, 2400, 1680)
        val tileUrl = (satelliteSource as org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase)
            .getTileURLString(sampleTile)
        assertTrue(tileUrl.endsWith("/12/1680/2400"))

        assertTrue(
            com.example.ui.components.attributionForLayer(com.example.ui.components.MapLayerMode.STANDARD)
                .contains("OpenStreetMap"),
        )
        assertTrue(
            com.example.ui.components.attributionForLayer(com.example.ui.components.MapLayerMode.SATELLITE)
                .contains("Esri"),
        )
    }

    @Test
    fun goldShopLocationRecord_mapsCoordinatesAndGeoPoint_andMergesCatalogCleanly() {
        val geoShopRecord = com.example.data.remote.GoldShopLocationRecord(
            id = "fs_luxor_1",
            name = "مجوهرات الكرنك",
            city = "الأقصر",
            area = "شارع المحطة",
            address = "شارع المحطة، الأقصر",
            phone = "+201099887766",
            latitude = 0.0,
            longitude = 0.0,
            coordinates = com.google.firebase.firestore.GeoPoint(25.6872, 32.6396),
            rating = 4.9,
            reviews = 42L,
            distanceKm = 3.2,
            verified = true,
            closesAt = "10:00 م",
            makingFeePerGram = 85.0,
            ownerUid = "owner-luxor",
            active = true,
        )
        assertEquals(25.6872, geoShopRecord.resolvedLatitude(), 0.0001)
        assertEquals(32.6396, geoShopRecord.resolvedLongitude(), 0.0001)

        val domainShop = geoShopRecord.toShop()
        assertEquals("fs_luxor_1", domainShop.id)
        assertEquals("الأقصر", domainShop.city)
        assertEquals(25.6872, domainShop.latitude, 0.0001)
        assertEquals(32.6396, domainShop.longitude, 0.0001)

        val serializedRecord = com.example.data.remote.GoldShopLocationRecord.fromShop(
            shop = domainShop,
            ownerUid = "owner-luxor",
            merchantId = "m_luxor_1",
            active = true,
        )
        val createMap = serializedRecord.toCreateMap()
        assertFalse(createMap.containsKey("id"))
        assertEquals(25.6872, createMap["latitude"] as Double, 0.0001)
        assertEquals(32.6396, createMap["longitude"] as Double, 0.0001)
        assertTrue(createMap["coordinates"] is com.google.firebase.firestore.GeoPoint)
        assertEquals("owner-luxor", createMap["ownerUid"])
        assertEquals(true, createMap["active"])
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))

        // mergeGoldShopCatalog overrides matching IDs from Firestore, appends new Firestore shops,
        // and preserves coordinates when approved merchant stores do not yet store coordinates.
        val staticList = KingGoldRepository.shops
        val overriddenS1 = staticList.first().copy(
            name = "النور للذهب - محدث",
            latitude = 30.0601,
            longitude = 31.2501,
        )
        val merchantWithoutCoords = com.example.model.MerchantStore(
            id = "s2",
            ownerUid = "merchant-owner-2",
            shopName = "سبائك النيل المعتمد",
            area = "المعادي",
            address = "شارع 9، المعادي",
            contactPhone = "+201022334455",
            status = com.example.model.MerchantStatus.APPROVED,
            city = "",
            latitude = 0.0,
            longitude = 0.0,
        )
        val merged = com.example.ui.state.mergeGoldShopCatalog(
            staticShops = staticList,
            firestoreGoldShops = listOf(overriddenS1, domainShop),
            approvedMerchants = listOf(merchantWithoutCoords),
        )

        val s1Merged = merged.first { it.id == "s1" }
        assertEquals("النور للذهب - محدث", s1Merged.name)
        assertEquals(30.0601, s1Merged.latitude, 0.0001)

        val luxorMerged = merged.firstOrNull { it.id == "fs_luxor_1" }
        assertNotNull(luxorMerged)
        assertEquals(25.6872, luxorMerged!!.latitude, 0.0001)

        val s2Merged = merged.first { it.id == "s2" }
        assertEquals("سبائك النيل المعتمد", s2Merged.name)
        assertEquals(staticList.first { it.id == "s2" }.latitude, s2Merged.latitude, 0.0001)
        assertEquals(staticList.first { it.id == "s2" }.longitude, s2Merged.longitude, 0.0001)
    }

    @Test
    fun dailyGoldPriceRecord_serializesPiastersAndPowersTrendSeriesCleanly() {
        val sample = com.example.model.DailyGoldPrice(
            dateKey = "2026-11-20",
            dayLabel = "اليوم",
            price24kEgp = 6350.0,
            price21kEgp = 5556.25,
            price18kEgp = 4762.5,
            source = "ADMIN_PRICING",
        )
        val record = com.example.data.remote.DailyGoldPriceRecord.fromDailyGoldPrice(
            daily = sample,
            updatedByAdminUid = "admin-uid-1",
            published = true,
        )

        assertEquals(635000L, record.price24kPerGramPiasters)
        assertEquals(555625L, record.price21kPerGramPiasters)
        assertEquals(476250L, record.price18kPerGramPiasters)
        assertEquals("2026-11-20", record.dateKey)

        val createMap = record.toCreateMap()
        assertEquals("2026-11-20", createMap["dateKey"])
        assertEquals(635000L, createMap["price24kPerGramPiasters"])
        assertEquals(true, createMap["published"])
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))

        val updateMap = record.toUpdateMap()
        assertFalse(updateMap.containsKey("dateKey"))
        assertFalse(updateMap.containsKey("createdAt"))
        assertTrue(updateMap.containsKey("updatedAt"))

        val roundTrip = record.toDailyGoldPrice()
        assertEquals(6350.0, roundTrip.price24kEgp, 0.01)
        assertEquals(5556.25, roundTrip.price21kEgp, 0.01)
        assertEquals(4762.5, roundTrip.price18kEgp, 0.01)
        assertEquals(5556.25, roundTrip.priceForKarat(21), 0.01)

        // Ordering guard clamps 21K below 24K and 18K below 21K
        val inverted = com.example.data.remote.DailyGoldPriceRecord.fromDailyGoldPrice(
            daily = sample.copy(price24kEgp = 6000.0, price21kEgp = 6200.0, price18kEgp = 6500.0),
            updatedByAdminUid = "admin-uid-1",
        )
        assertTrue(inverted.price24kPerGramPiasters > inverted.price21kPerGramPiasters)
        assertTrue(inverted.price21kPerGramPiasters > inverted.price18kPerGramPiasters)

        // UiState switches between fallback 7-day trend and Firestore-synced records sorted ascending
        val fallbackState = com.example.ui.state.KingGoldUiState()
        assertFalse(fallbackState.isDailyPricesFromFirestore)
        assertEquals(7, fallbackState.dailyGoldPriceTrend.size)

        val olderDay = sample.copy(dateKey = "2026-11-18", dayLabel = "18/11", price24kEgp = 6250.0)
        val syncedState = fallbackState.copy(
            firestoreDailyGoldPrices = listOf(sample, olderDay),
        )
        assertTrue(syncedState.isDailyPricesFromFirestore)
        assertEquals(listOf("2026-11-18", "2026-11-20"), syncedState.dailyGoldPriceTrend.map { it.dateKey })
    }
}
