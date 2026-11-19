package com.example

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.credentials.Credential
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.FirebaseAuthGateway
import com.example.ui.components.GoldCalculatorScreen
import com.example.ui.components.configureOsmdroid
import com.example.ui.navigation.KingGoldNavHost
import com.example.ui.navigation.Routes
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.home.UserProfileScreen
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.config.Configuration
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KingGoldNewFeaturesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun fakeAuthViewModel(
        onSignUp: () -> Unit = {},
    ): AuthViewModel {
        val gateway = object : FirebaseAuthGateway {
            override suspend fun signInWithGoogleIdToken(idToken: String) = Result.success("uid-1")
            override suspend fun signInWithEmail(email: String, password: String) = Result.success("uid-1")
            override suspend fun signUpWithEmail(fullName: String, email: String, password: String): Result<String> {
                onSignUp()
                return Result.success("uid-1")
            }

            override fun hasActiveUser() = false
        }
        return AuthViewModel(
            authGateway = gateway,
            idTokenExtractor = { _: Credential -> "token" },
        )
    }

    @Test
    fun loginScreen_rendersTraditionalAuthAndSecondaryGoogleOption() {
        var authenticated = false
        val authVm = fakeAuthViewModel()
        composeRule.setContent {
            KingGoldTheme {
                LoginScreen(
                    onAuthSuccess = { authenticated = true },
                    authViewModel = authVm,
                )
            }
        }
        composeRule.waitForIdle()

        // Traditional Sign In fields + secondary Google button visible
        composeRule.onNodeWithTag("auth_mode_sign_in_tab").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_mode_sign_up_tab").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_email_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_password_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_submit_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()

        // Switch to Sign Up tab -> Full Name + Confirm Password appear
        composeRule.onNodeWithTag("auth_mode_sign_up_tab").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("auth_name_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("auth_confirm_password_input").performScrollTo().assertIsDisplayed()

        // Fill Sign Up form and submit
        composeRule.onNodeWithTag("auth_name_input").performScrollTo().performTextInput("عمر حسن")
        composeRule.onNodeWithTag("auth_email_input").performScrollTo().performTextInput("omar@kinggold.eg")
        composeRule.onNodeWithTag("auth_password_input").performScrollTo().performTextInput("gold2026")
        composeRule.onNodeWithTag("auth_confirm_password_input").performScrollTo().performTextInput("gold2026")
        composeRule.onNodeWithTag("auth_submit_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(authenticated)
    }

    @Test
    fun userProfileScreen_showsEmailFavoritedShopsAndSignOut() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        vm.toggleFavoriteShop("s1")
        var clickedShopId: String? = null
        var signedOut = false

        composeRule.setContent {
            KingGoldTheme {
                val state = vm.uiState.value.copy(userEmail = "customer@kinggold.eg")
                UserProfileScreen(
                    uiState = state,
                    onBack = {},
                    onOrderClick = {},
                    onOpenShop = { clickedShopId = it.id },
                    onToggleFavoriteShop = vm::toggleFavoriteShop,
                    onSignOut = { signedOut = true },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_user_email").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("customer@kinggold.eg").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_sign_out_button").performScrollTo().assertIsDisplayed()

        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("profile_favorite_shop_s1"))
        composeRule.onNodeWithTag("profile_favorite_shop_s1").performScrollTo().performClick()
        assertEquals("s1", clickedShopId)

        // Tap Sign Out opens confirmation dialog -> confirm calls onSignOut
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("profile_sign_out_button"))
        composeRule.onNodeWithTag("profile_sign_out_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("خروج").performClick()
        assertTrue(signedOut)
    }

    @Test
    fun discoverScreen_topSearchAndListMapSwitcher_renderOpenStreetMapView() {
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

        // Search bar sits near top of Discover screen
        composeRule.onNodeWithTag("discover_search_input").assertIsDisplayed()

        // Switch to Map view
        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag("discover_mode_map"))
        composeRule.onNodeWithTag("discover_mode_map").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag("gold_shops_map_container"))
        composeRule.onNodeWithTag("gold_shops_map_container").assertIsDisplayed()
        composeRule.onNodeWithTag("map_selected_shop_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun goldCalculatorScreen_updatesValuationWhenWeightAndKaratChange() {
        val vm = KingGoldViewModel(autoFetchPrices = false)
        composeRule.setContent {
            KingGoldTheme {
                GoldCalculatorScreen(
                    uiState = vm.uiState.value,
                    onBack = {},
                    onOpenPriceInsight = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Default is 8g x 5425 (21K) = 43,400.00 ج.م
        composeRule.onNodeWithTag("calculator_result_value")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("43,400.00 ج.م")

        // Switch to 24K and enter 10g -> 10 x 6200 = 62,000.00 ج.م
        composeRule.onNodeWithTag("calculator_karat_24").performScrollTo().performClick()
        composeRule.onNodeWithTag("calculator_weight_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("calculator_weight_input").performTextInput("10")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calculator_result_value")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("62,000.00 ج.م")
    }

    @Test
    fun osmdroidConfiguration_andFcmNotificationIntents_doNotUseGoogleMaps() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        configureOsmdroid(context)
        assertTrue(Configuration.getInstance().userAgentValue.isNotBlank())
        assertTrue(Configuration.getInstance().osmdroidTileCache.exists())

        val shopIntent = GoldPriceNotificationHelper.buildLaunchIntent(context, Routes.shopDetail("s1"))
        assertEquals("shop_detail/s1", shopIntent.getStringExtra(GoldPriceNotificationHelper.EXTRA_TARGET_ROUTE))

        val priceIntent = GoldPriceNotificationHelper.buildLaunchIntent(context, Routes.PRICE_INSIGHT)
        assertEquals(Routes.PRICE_INSIGHT, priceIntent.getStringExtra(GoldPriceNotificationHelper.EXTRA_TARGET_ROUTE))
        assertTrue(Routes.isSupportedInternalRoute(Routes.GOLD_CALCULATOR))
    }

    @Test
    fun authViewModel_handleGoogleCredentialResult_parsesCustomCredentialAndRejectsUnsupported() = kotlinx.coroutines.test.runTest {
        var exchangedToken: String? = null
        val gateway = object : FirebaseAuthGateway {
            override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> {
                exchangedToken = idToken
                return Result.success("uid-google")
            }

            override suspend fun signInWithEmail(email: String, password: String) = Result.success("uid")
            override suspend fun signUpWithEmail(fullName: String, email: String, password: String) = Result.success("uid")
            override fun hasActiveUser() = false
        }
        val vm = AuthViewModel(authGateway = gateway)

        val bundle = android.os.Bundle().apply {
            putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID", "user@gmail.com")
            putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN", "header.payload.signature")
        }
        val googleCred = androidx.credentials.CustomCredential(
            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            bundle,
        )
        assertTrue(vm.handleGoogleCredentialResult(googleCred, silent = false))
        assertEquals("header.payload.signature", exchangedToken)
        assertTrue(vm.uiState.value.isAuthenticated)

        val unsupportedCred = androidx.credentials.CustomCredential("unsupported.type", android.os.Bundle())
        val vm2 = AuthViewModel(authGateway = gateway)
        assertTrue(!vm2.handleGoogleCredentialResult(unsupportedCred, silent = false))
        assertTrue(!vm2.uiState.value.isAuthenticated)
        assertTrue(!vm2.uiState.value.errorMessage.isNullOrBlank())
    }
}
