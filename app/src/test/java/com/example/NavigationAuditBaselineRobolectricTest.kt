package com.example

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.model.OrderStatus
import com.example.ui.navigation.KingGoldNavHost
import com.example.ui.state.KingGoldViewModel
import com.example.ui.state.MerchantRegistrationSubmitter
import com.example.ui.state.MerchantRegistrationViewModel
import com.example.ui.theme.KingGoldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationAuditBaselineRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun scrollLazyToTag(tag: String) {
        composeRule.onAllNodes(hasScrollToNodeAction())
            .onFirst()
            .performScrollToNode(hasTestTag(tag))
        composeRule.waitForIdle()
    }

    private fun scrollAndClick(tag: String) {
        scrollLazyToTag(tag)
        composeRule.onNodeWithTag(tag).performScrollTo()
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun scrollAndInput(tag: String, text: String) {
        scrollLazyToTag(tag)
        composeRule.onNodeWithTag(tag).performScrollTo()
        composeRule.onNodeWithTag(tag).performTextInput(text)
        composeRule.waitForIdle()
    }

    @Test
    fun homeTabs_backPressReturnsToDiscoverBeforeExitingApp() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Vault tab -> Back returns to Discover without exiting
        composeRule.onNodeWithTag("tab_vault").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_hero_card").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithTag("discover_cart_button").assertIsDisplayed()

        // Sell Gold tab -> Back returns to Discover without exiting
        composeRule.onNodeWithTag("tab_sell_gold").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("بيع ذهبك للمنصة").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithTag("discover_cart_button").assertIsDisplayed()

        // Orders tab -> Back returns to Discover without exiting
        composeRule.onNodeWithTag("tab_orders").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("محفوظ في الضمان").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithTag("discover_cart_button").assertIsDisplayed()

        // Account tab -> Back returns to Discover without exiting
        composeRule.onNodeWithTag("tab_account").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("account_profile_card").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithTag("discover_cart_button").assertIsDisplayed()

        // Back on root Discover tab exits Activity as expected
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun cart_emptyStateAndFullCheckoutFlow_stayInsideApp() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Top Cart button on Discover is visible even when cart is empty
        composeRule.onNodeWithTag("discover_cart_button").assertIsDisplayed()
        composeRule.onNodeWithTag("discover_cart_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("السلة فاضية").assertIsDisplayed()
        composeRule.onNodeWithTag("cart_browse_shops_button").assertIsDisplayed()
        composeRule.onNodeWithTag("cart_browse_shops_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("discover_cart_button").assertIsDisplayed()

        // Add product from shop s1 and complete checkout -> Success -> Orders tab
        scrollAndClick("shop_card_s1")

        composeRule.onNodeWithTag("browse_shop_gold_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quick_add_p1").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("view_cart_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cart_checkout_button").assertIsDisplayed()

        composeRule.onNodeWithTag("cart_checkout_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pay_now_button").assertIsDisplayed()

        composeRule.onNodeWithTag("pay_now_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("view_all_orders_button").assertIsDisplayed()

        // Back on Success screen navigates directly to Orders tab inside the app
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText("محفوظ في الضمان").assertIsDisplayed()
        assertTrue(vm.uiState.value.orders.isNotEmpty())
    }

    @Test
    fun vaultSheets_keepOpenOnValidationErrorAndCloseOnSuccess() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tab_vault").performClick()
        composeRule.waitForIdle()

        // 1) Buy sheet: wallet has 0 balance -> clicking confirm shows inline error and keeps sheet open
        scrollAndClick("buy_fractional_gold_button")
        composeRule.onNodeWithTag("confirm_buy_fractional_button").performScrollTo()
        composeRule.onNodeWithTag("confirm_buy_fractional_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_buy_sheet_error").performScrollTo()
        composeRule.onNodeWithTag("vault_buy_sheet_error").assertIsDisplayed()

        // Switch to pending admin review mode -> closes sheet and records pending gold
        composeRule.onNodeWithText("طلب معلق لمراجعة الإدارة").performScrollTo()
        composeRule.onNodeWithText("طلب معلق لمراجعة الإدارة").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_buy_fractional_button").performScrollTo()
        composeRule.onNodeWithTag("confirm_buy_fractional_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_buy_sheet_error").assertDoesNotExist()
        assertTrue(vm.uiState.value.wallet.goldPendingMg > 0L)

        // Verify Ledger Filter FlowRow renders cleanly without NoSuchMethodError
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(androidx.compose.ui.test.hasText("سجل المعاملات المالي (Ledger)"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("النقدية").performClick()
        composeRule.waitForIdle()

        // 2) Deposit sheet: blank reference shows inline error and keeps sheet open
        scrollAndClick("open_deposit_sheet_button")
        composeRule.onNodeWithTag("deposit_reference_input").performScrollTo()
        composeRule.onNodeWithTag("deposit_reference_input").performTextClearance()
        composeRule.onNodeWithTag("confirm_deposit_request_button").performScrollTo()
        composeRule.onNodeWithTag("confirm_deposit_request_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_deposit_sheet_error").performScrollTo()
        composeRule.onNodeWithTag("vault_deposit_sheet_error").assertIsDisplayed()

        // Enter valid reference -> closes sheet and adds pending cash
        composeRule.onNodeWithTag("deposit_reference_input").performScrollTo()
        composeRule.onNodeWithTag("deposit_reference_input").performTextInput("REF-9910")
        composeRule.onNodeWithTag("confirm_deposit_request_button").performScrollTo()
        composeRule.onNodeWithTag("confirm_deposit_request_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_deposit_sheet_error").assertDoesNotExist()
        assertTrue(vm.uiState.value.wallet.cashPendingPiasters > 0L)
    }

    @Test
    fun verifyPickupAndRating_flowRowAndSkip_stayInsideApp() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val shop = vm.uiState.value.shops.first()
        val product = vm.uiState.value.productsForShop(shop.id).first()
        vm.addToCart(shop, product)
        val created = vm.placeOrder()!!

        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tab_orders").performClick()
        composeRule.waitForIdle()
        scrollAndClick("order_card_${created.id}")
        scrollAndClick("start_verify_pickup_button")

        scrollAndClick("verify_check_0")
        scrollAndClick("verify_check_1")
        scrollAndClick("verify_check_2")
        scrollAndClick("verify_check_3")
        composeRule.onNodeWithTag("confirm_pickup_release_button").performClick()
        composeRule.waitForIdle()

        // RatingScreen uses FlowRow (verifies no FlowRow NoSuchMethodError) and Skip action
        composeRule.onNodeWithTag("skip_rating_button").assertIsDisplayed()
        composeRule.onNodeWithTag("skip_rating_button").performClick()
        composeRule.waitForIdle()

        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithText("محفوظ في الضمان").assertIsDisplayed()
        assertEquals(OrderStatus.COMPLETED, vm.findOrder(created.id)?.status)
    }

    @Test
    fun reportIssue_preservesUserDetailsInOrderDispute() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        // Seed an order via ViewModel cart flow so OrdersScreen has an active order
        val shop = vm.uiState.value.shops.first()
        val product = vm.uiState.value.productsForShop(shop.id).first()
        vm.addToCart(shop, product)
        val created = vm.placeOrder()!!

        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tab_orders").performClick()
        composeRule.waitForIdle()
        scrollAndClick("order_card_${created.id}")

        scrollAndClick("report_issue_button")

        scrollAndInput("report_details_input", "الوزن الفعلي 7.8 جم")
        composeRule.onNodeWithTag("submit_issue_button").performClick()
        composeRule.waitForIdle()

        val updatedOrder = vm.findOrder(created.id)!!
        assertEquals(OrderStatus.DISPUTED, updatedOrder.status)
        assertTrue(updatedOrder.disputeReason.orEmpty().contains("الوزن الفعلي 7.8 جم"))
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun merchantRegistration_openPortalDoesNotDuplicateBackStack() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        val regVm = MerchantRegistrationViewModel(
            submitter = MerchantRegistrationSubmitter { _, _, _, _, _, _, _, _, _, _, _, _ ->
                Result.success("m_test_100")
            },
        )
        composeRule.setContent {
            KingGoldTheme {
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = {},
                    merchantRegistrationViewModel = regVm,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tab_account").performClick()
        composeRule.waitForIdle()
        scrollAndClick("account_merchant_register_tile")

        // Fill valid registration form
        scrollAndInput("reg_legal_name_input", "أحمد محمد الشريف")
        scrollAndInput("reg_business_id_input", "CR-998877")
        scrollAndInput("reg_shop_name_input", "مجوهرات الشريف")
        scrollAndInput("reg_area_input", "الصاغة، القاهرة")
        scrollAndInput("reg_shop_address_input", "14 شارع المعز، الصاغة")
        scrollAndInput("reg_contact_phone_input", "+201012345678")
        scrollAndInput("reg_contact_email_input", "owner@sharifgold.com")
        scrollAndClick("reg_submit_button")

        composeRule.onNodeWithTag("reg_success_card").assertIsDisplayed()
        scrollAndClick("reg_success_open_portal_button")

        // Now on MerchantPortalScreen; single Back returns directly to Home (Account tab), not back to Registration
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("بوابة التاجر").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("بوابة التاجر").onFirst().assertIsDisplayed()
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        composeRule.onNodeWithTag("account_merchant_register_tile").assertIsDisplayed()
    }

    @Test
    fun centralizedNavHost_splashLoginVaultCartAndSignOut_shareSingleContext() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        var isAuthenticated by mutableStateOf(false)
        var pendingRoute by mutableStateOf<String?>(null)
        lateinit var centralizedController: NavHostController

        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).clearNextStartedActivities()

        composeRule.setContent {
            KingGoldTheme {
                centralizedController = rememberNavController()
                KingGoldNavHost(
                    viewModel = if (isAuthenticated) vm else null,
                    onSignOut = { isAuthenticated = false },
                    navController = centralizedController,
                    startDestination = com.example.ui.navigation.Routes.SPLASH,
                    isAuthenticated = isAuthenticated,
                    pendingRoute = pendingRoute,
                    onPendingRouteConsumed = { pendingRoute = null },
                    splashContent = { onFinished ->
                        Button(
                            onClick = onFinished,
                            modifier = Modifier.testTag("test_finish_splash_button"),
                        ) {
                            Text("Enter")
                        }
                    },
                    loginContent = { onAuthSuccess ->
                        Button(
                            onClick = {
                                isAuthenticated = true
                                onAuthSuccess()
                            },
                            modifier = Modifier.testTag("test_complete_login_button"),
                        ) {
                            Text("Sign In")
                        }
                    },
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(com.example.ui.navigation.Routes.SPLASH, centralizedController.currentDestination?.route)

        // Splash -> Login when unauthenticated
        composeRule.onNodeWithTag("test_finish_splash_button").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.LOGIN, centralizedController.currentDestination?.route)

        // Login -> Home via the same centralized controller
        composeRule.onNodeWithTag("test_complete_login_button").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.HOME, centralizedController.currentDestination?.route)
        composeRule.onNodeWithTag("discover_cart_button").assertIsDisplayed()

        // Clicking Discover Vault card navigates inside centralized NavHost to Routes.VAULT without external Intents
        scrollAndClick("discover_vault_summary_card")
        assertEquals(com.example.ui.navigation.Routes.VAULT, centralizedController.currentDestination?.route)
        composeRule.onNodeWithTag("vault_hero_card").assertIsDisplayed()
        assertEquals(null, Shadows.shadowOf(app).nextStartedActivity)

        // Back pops Vault -> Discover (Home)
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertFalse(composeRule.activity.isFinishing)
        assertEquals(com.example.ui.navigation.Routes.HOME, centralizedController.currentDestination?.route)

        // Clicking Discover Cart button navigates inside centralized NavHost to Routes.CART without external Intents
        scrollAndClick("discover_cart_button")
        assertEquals(com.example.ui.navigation.Routes.CART, centralizedController.currentDestination?.route)
        composeRule.onNodeWithTag("cart_browse_shops_button").assertIsDisplayed()
        assertEquals(null, Shadows.shadowOf(app).nextStartedActivity)

        // Incoming notification route navigates within the same centralized NavHost
        pendingRoute = com.example.ui.navigation.Routes.PRICE_INSIGHT
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.PRICE_INSIGHT, centralizedController.currentDestination?.route)
        assertEquals(null, pendingRoute)
        assertEquals(null, Shadows.shadowOf(app).nextStartedActivity)

        // Sign Out clears authenticated back stack and returns to Routes.LOGIN
        isAuthenticated = false
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.LOGIN, centralizedController.currentDestination?.route)

        // Pressing Back on Login exits instead of popping into an authenticated destination
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun navigationGuard_blocksUnauthorizedVaultAndCart_redirectsToLogin() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        var isAuthenticated by mutableStateOf(false)
        var pendingRoute by mutableStateOf<String?>(null)
        lateinit var controller: NavHostController

        composeRule.setContent {
            KingGoldTheme {
                controller = rememberNavController()
                KingGoldNavHost(
                    viewModel = vm,
                    onSignOut = { isAuthenticated = false },
                    navController = controller,
                    startDestination = com.example.ui.navigation.Routes.LOGIN,
                    isAuthenticated = isAuthenticated,
                    pendingRoute = pendingRoute,
                    onPendingRouteConsumed = { pendingRoute = null },
                    loginContent = { onAuthSuccess ->
                        Button(
                            onClick = {
                                isAuthenticated = true
                                onAuthSuccess()
                            },
                            modifier = Modifier.testTag("guard_test_login_button"),
                        ) {
                            Text("Sign In")
                        }
                    },
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.LOGIN, controller.currentDestination?.route)

        // Direct navigation to GoldVault while unauthenticated is intercepted and redirected to LOGIN
        composeRule.runOnUiThread {
            controller.navigate(com.example.ui.navigation.Routes.VAULT)
        }
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.LOGIN, controller.currentDestination?.route)
        composeRule.onNodeWithTag("vault_hero_card").assertDoesNotExist()
        composeRule.onNodeWithTag("guard_test_login_button").assertIsDisplayed()

        // Direct navigation to Cart while unauthenticated is intercepted and redirected to LOGIN
        composeRule.runOnUiThread {
            controller.navigate(com.example.ui.navigation.Routes.CART)
        }
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.LOGIN, controller.currentDestination?.route)
        composeRule.onNodeWithTag("cart_browse_shops_button").assertDoesNotExist()
        composeRule.onNodeWithTag("guard_test_login_button").assertIsDisplayed()

        // Deep-link pendingRoute to Cart while unauthenticated redirects to LOGIN first, then opens CART after sign-in
        pendingRoute = com.example.ui.navigation.Routes.CART
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.LOGIN, controller.currentDestination?.route)
        assertEquals(null, pendingRoute)
        composeRule.onNodeWithTag("cart_browse_shops_button").assertDoesNotExist()

        // Completing login transitions through HOME and fulfills deferred Cart route safely
        composeRule.onNodeWithTag("guard_test_login_button").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.navigation.Routes.CART, controller.currentDestination?.route)
        composeRule.onNodeWithTag("cart_browse_shops_button").assertIsDisplayed()
    }

    @Test
    fun navigationGuard_protectedStartDestinationWhileUnauthenticated_redirectsToLogin() {
        lateinit var controller: NavHostController

        composeRule.setContent {
            KingGoldTheme {
                controller = rememberNavController()
                KingGoldNavHost(
                    viewModel = null,
                    onSignOut = {},
                    navController = controller,
                    startDestination = com.example.ui.navigation.Routes.VAULT,
                    isAuthenticated = false,
                    loginContent = {
                        Button(
                            onClick = {},
                            modifier = Modifier.testTag("guard_start_login_button"),
                        ) {
                            Text("Sign In Required")
                        }
                    },
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(com.example.ui.navigation.Routes.LOGIN, controller.currentDestination?.route)
        composeRule.onNodeWithTag("vault_hero_card").assertDoesNotExist()
        composeRule.onNodeWithTag("guard_start_login_button").assertIsDisplayed()
    }
}
