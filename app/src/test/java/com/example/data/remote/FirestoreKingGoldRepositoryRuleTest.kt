package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.KingGoldRepository
import com.example.model.CartItem
import com.example.model.GoldOrder
import com.example.model.KycStatus
import com.example.model.OrderStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreKingGoldRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleOrder(orderId: String): GoldOrder {
        val shop = KingGoldRepository.shops.first()
        val product = KingGoldRepository.productsFor(shop.id).first()
        return GoldOrder(
            id = orderId,
            shop = shop,
            items = listOf(CartItem(product = product, quantity = 1)),
            goldValue = 43400.0,
            makingFee = 720.0,
            serviceFee = 441.2,
            pickupWindow = "اليوم · 4:00–8:00 م",
            status = OrderStatus.READY_FOR_PICKUP,
        )
    }

    @Test
    fun createAndObserveOrder_authenticatedOwner_succeedsAndEmitsUpdate() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreKingGoldRepository(firestore, auth)
        val orderId = "KG-${UUID.randomUUID().toString().take(8)}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrder(sampleOrder(orderId))
        }
        assertTrue(createResult.isSuccess)
        assertEquals(orderId, createResult.getOrThrow())

        val orders = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeOrders(uid).first { list -> list.any { it.id == orderId } }
        }
        val stored = orders.first { it.id == orderId }
        assertEquals(uid, stored.userId)
        assertEquals(OrderStatus.READY_FOR_PICKUP.name, stored.status)
    }

    @Test
    fun completePickupAndRate_authenticatedOwner_transitionsAndEnforcesRules() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreKingGoldRepository(firestore, auth)
        val orderId = "KG-${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrder(sampleOrder(orderId)).getOrThrow()
            repository.completePickup(orderId).getOrThrow()
            repository.submitRating(orderId, 5).getOrThrow()
        }

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrderById(orderId).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals(OrderStatus.COMPLETED.name, fetched?.status)
        assertEquals(5L, fetched?.ratingGiven)
    }

    @Test
    fun submitIssueReport_authenticatedOwner_marksOrderDisputed() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreKingGoldRepository(firestore, auth)
        val orderId = "KG-${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createOrder(sampleOrder(orderId)).getOrThrow()
            repository.submitIssueReport(orderId, "الوزن مختلف عن الفاتورة").getOrThrow()
        }

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrderById(orderId).getOrThrow()
        }
        assertEquals(OrderStatus.DISPUTED.name, fetched?.status)
        assertEquals("الوزن مختلف عن الفاتورة", fetched?.disputeReason)
    }

    @Test
    fun saveAndObservePreferences_authenticatedOwner_persistsSettings() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreKingGoldRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePreferences(
                UserPreferencesRecord(
                    userId = uid,
                    kycFrontDone = true,
                    kycBackDone = true,
                    kycSelfieDone = true,
                    kycStatus = KycStatus.VERIFIED.name,
                    selectedPaymentMethodId = "instapay",
                    priceAlertEnabled = true,
                    priceAlertAbove = false,
                    priceAlertTarget = "5350",
                    favoriteShopIds = listOf("s1", "s3"),
                ),
            )
        }
        assertTrue(saveResult.isSuccess)

        val prefs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePreferences(uid).first { it?.kycStatus == KycStatus.VERIFIED.name }
        }
        assertEquals("instapay", prefs?.selectedPaymentMethodId)
        assertEquals("5350", prefs?.priceAlertTarget)
        assertEquals(listOf("s1", "s3"), prefs?.favoriteShopIds)

        val updateFavsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePreferences(prefs!!.copy(favoriteShopIds = listOf("s2")))
        }
        assertTrue(updateFavsResult.isSuccess)
        val updatedPrefs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePreferences(uid).first { it?.favoriteShopIds == listOf("s2") }
        }
        assertEquals(listOf("s2"), updatedPrefs?.favoriteShopIds)
    }

    @Test
    fun observeOrders_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreKingGoldRepository(firestore, auth)
        val orderId = "KG-${UUID.randomUUID().toString().take(8)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrder(sampleOrder(orderId)).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreKingGoldRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeOrders(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED for cross-user read", caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeOrders_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FirestoreKingGoldRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeOrders("any_user_id").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED for unauthenticated caller", caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_kg@test.com"
        const val BOB_EMAIL = "bob_kg@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
