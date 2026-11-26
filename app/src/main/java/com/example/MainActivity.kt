package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.data.DataStoreSearchHistoryStore
import com.example.data.remote.FirestoreKingGoldRepository
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreVaultRepository
import com.example.firebase.configureAppCheck
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.auth.authStateFlow
import com.example.ui.navigation.KingGoldNavHost
import com.example.ui.navigation.Routes
import com.example.ui.state.AdminApprovalViewModel
import com.example.ui.state.KingGoldViewModel
import com.example.ui.state.MerchantRegistrationViewModel
import com.example.ui.theme.KingGoldTheme
import com.example.widget.GoldPriceAppWidgetProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private val pendingRouteState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)
        GoldPriceNotificationHelper.ensureNotificationChannel(this)
        pendingRouteState.value = extractTargetRoute(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent {
            KingGoldTheme {
                AppContent(
                    pendingRoute = pendingRouteState.value,
                    onPendingRouteConsumed = { pendingRouteState.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractTargetRoute(intent)?.let { route ->
            pendingRouteState.value = route
        }
    }

    private fun extractTargetRoute(intent: Intent?): String? {
        val raw = intent?.getStringExtra(GoldPriceNotificationHelper.EXTRA_TARGET_ROUTE)?.trim()
        if (raw.isNullOrEmpty()) return null
        return raw.takeIf { Routes.isSupportedInternalRoute(it) }
    }
}

@Immutable
private data class UserSessionViewModels(
    val kingGoldViewModel: KingGoldViewModel,
    val merchantRegistrationViewModel: MerchantRegistrationViewModel,
    val adminApprovalViewModel: AdminApprovalViewModel,
)

@Composable
private fun AppContent(
    pendingRoute: String?,
    onPendingRouteConsumed: () -> Unit,
) {
    // Single centralized NavHostController created at the app's main entry point.
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val sessionViewModels = currentUser?.let { user ->
        rememberUserSessionViewModels(auth = auth, user = user)
    }

    KingGoldNavHost(
        viewModel = sessionViewModels?.kingGoldViewModel,
        merchantRegistrationViewModel = sessionViewModels?.merchantRegistrationViewModel,
        adminApprovalViewModel = sessionViewModels?.adminApprovalViewModel,
        navController = navController,
        startDestination = Routes.SPLASH,
        isAuthenticated = currentUser != null,
        pendingRoute = pendingRoute,
        onPendingRouteConsumed = onPendingRouteConsumed,
        requestLocationOnStart = true,
        onSignOut = {
            GoogleAuthHelper.signOut(
                credentialManager = credentialManager,
                scope = coroutineScope,
            )
        },
    )
}

@Composable
private fun rememberUserSessionViewModels(
    auth: FirebaseAuth,
    user: FirebaseUser,
): UserSessionViewModels {
    return key(user.uid) {
        val kingGoldViewModel: KingGoldViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    KingGoldViewModel(
                        firestoreRepository = FirestoreKingGoldRepository(db, auth),
                        marketplaceRepository = FirestoreMarketplaceRepository(db, auth),
                        vaultRepository = FirestoreVaultRepository(db, auth),
                        currentUserId = user.uid,
                        userDisplayName = user.displayName,
                        userEmail = user.email,
                        onPriceThresholdTriggered = { karat, currentPrice, threshold, isAbove ->
                            GoldPriceNotificationHelper.showGoldPriceThresholdAlert(
                                context = app,
                                karat = karat,
                                currentPriceEgp = currentPrice,
                                thresholdEgp = threshold,
                                isAbove = isAbove,
                            )
                        },
                        onPricesSynced = { syncedPrices, feedStatus ->
                            GoldPriceAppWidgetProvider.syncFromPrices(app, syncedPrices, feedStatus)
                        },
                        searchHistoryStore = DataStoreSearchHistoryStore(app),
                    )
                }
            },
        )
        val merchantRegistrationViewModel: MerchantRegistrationViewModel = viewModel(
            key = "merchant_reg_${user.uid}",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    MerchantRegistrationViewModel(
                        marketplaceRepository = FirestoreMarketplaceRepository(db, auth),
                        currentUserId = user.uid,
                        initialEmail = user.email.orEmpty(),
                    )
                }
            },
        )
        val adminApprovalViewModel: AdminApprovalViewModel = viewModel(
            key = "admin_approval_${user.uid}",
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    AdminApprovalViewModel(
                        marketplaceRepository = FirestoreMarketplaceRepository(db, auth),
                        isAdmin = user.email.equals(KingGoldViewModel.ADMIN_EMAIL, ignoreCase = true),
                    )
                }
            },
        )
        UserSessionViewModels(
            kingGoldViewModel = kingGoldViewModel,
            merchantRegistrationViewModel = merchantRegistrationViewModel,
            adminApprovalViewModel = adminApprovalViewModel,
        )
    }
}
