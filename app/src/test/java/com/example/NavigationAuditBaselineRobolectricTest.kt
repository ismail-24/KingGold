package com.example

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.ui.navigation.KingGoldNavHost
import com.example.ui.state.KingGoldViewModel
import com.example.ui.theme.KingGoldTheme
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

    @Test
    fun baseline_vaultTab_and_backPress_behavior() {
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

        // Click Gold Vault tab
        composeRule.onNodeWithTag("tab_vault").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("vault_hero_card").assertIsDisplayed()

        // Press system Back from Gold Vault tab
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        // Before fix: Activity is finishing (exits app!) instead of returning to Discover tab
        val exitedApp = composeRule.activity.isFinishing
        assertTrue(
            "Expected pre-fix baseline to demonstrate that Back from Gold Vault exits Activity; isFinishing=$exitedApp",
            exitedApp,
        )
    }

    @Test
    fun baseline_addToCart_and_openCart_staysInApp() {
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

        // Scroll Discover LazyColumn to shop card s1 and open it
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasTestTag("shop_card_s1"))
        composeRule.onNodeWithTag("shop_card_s1").performClick()
        composeRule.waitForIdle()

        // Browse shop gold -> ProductListScreen
        composeRule.onNodeWithTag("browse_shop_gold_button").performClick()
        composeRule.waitForIdle()

        // Quick add p1 -> opens bottom "view_cart_button" & top "product_list_cart_button"
        composeRule.onNodeWithTag("quick_add_p1").performClick()
        composeRule.waitForIdle()

        // Click view_cart_button
        composeRule.onNodeWithTag("view_cart_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("cart_checkout_button").assertIsDisplayed()
        assertFalse(composeRule.activity.isFinishing)
    }
}
