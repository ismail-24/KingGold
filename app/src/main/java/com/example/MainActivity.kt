package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.remote.FirestoreKingGoldRepository
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreVaultRepository
import com.example.firebase.configureAppCheck
import com.example.notifications.GoldPriceNotificationHelper
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.auth.authStateFlow
import com.example.ui.navigation.KingGoldNavHost
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.state.AdminApprovalViewModel
import com.example.ui.state.KingGoldViewModel
import com.example.ui.state.MerchantRegistrationViewModel
import com.example.ui.theme.KingGoldTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)
        GoldPriceNotificationHelper.ensureNotificationChannel(this)

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
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    var showSplash by remember { mutableStateOf(true) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val user = currentUser
    if (user == null) {
        LoginScreen(
            onAuthSuccess = {
                // AuthStateListener updates currentUser reactively.
            },
        )
    } else {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }

        key(user.uid) {
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
                            on24KPriceDroppedBelowThreshold = { current24K, threshold ->
                                GoldPriceNotificationHelper.show24KPriceDropAlert(
                                    context = app,
                                    currentPriceEgp = current24K,
                                    thresholdEgp = threshold,
                                )
                            },
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
            KingGoldNavHost(
                viewModel = kingGoldViewModel,
                merchantRegistrationViewModel = merchantRegistrationViewModel,
                adminApprovalViewModel = adminApprovalViewModel,
                onSignOut = {
                    GoogleAuthHelper.signOut(
                        credentialManager = credentialManager,
                        scope = coroutineScope,
                    )
                },
            )
        }
    }
}
