package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "Auth"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

sealed interface GoogleCredentialFetchResult {
    data class Success(val credential: Credential) : GoogleCredentialFetchResult
    data object Cancelled : GoogleCredentialFetchResult
    data class Error(val message: String) : GoogleCredentialFetchResult
}

object GoogleAuthHelper {

    suspend fun fetchSilentGoogleCredential(context: Context): Credential? {
        val alreadySignedIn = runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)
        if (alreadySignedIn) return null
        val activity = context.findActivity() ?: return null
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return null
        }
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            credentialManager.getCredential(activity, request).credential
        } catch (_: NoCredentialException) {
            null
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent Google Sign-In cancelled: ${e.message}", e)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Silent Google Sign-In unavailable: ${e.message}")
            null
        }
    }

    suspend fun fetchInteractiveGoogleCredential(
        context: Context,
        credentialManager: CredentialManager,
    ): GoogleCredentialFetchResult {
        val activity = context.findActivity()
            ?: return GoogleCredentialFetchResult.Error("تعذّر بدء تسجيل الدخول من هذا السياق")
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            return GoogleCredentialFetchResult.Error("إعدادات تسجيل الدخول بحساب Google غير مكتملة")
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            GoogleCredentialFetchResult.Success(result.credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            GoogleCredentialFetchResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credential available on device: ${e.message}", e)
            GoogleCredentialFetchResult.Error(
                "لا يوجد حساب Google متاح على الجهاز. أضف حساب Google من الإعدادات أو تأكد من تسجيل بصمتي SHA-1 و SHA-256 في Firebase Console.",
            )
        } catch (e: GetCredentialUnsupportedException) {
            Log.e(TAG, "Credential Manager unsupported on device", e)
            GoogleCredentialFetchResult.Error("خدمة تسجيل الدخول عبر Google غير مدعومة أو تحتاج لتحديث خدمات Google Play.")
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(TAG, "Google Sign-In provider configuration error", e)
            GoogleCredentialFetchResult.Error("إعدادات مزوّد تسجيل الدخول غير مكتملة. تأكد من تسجيل بصمتي SHA-1 و SHA-256 لشهادة التوقيع في Firebase Console.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.type}", e)
            GoogleCredentialFetchResult.Error(e.localizedMessage ?: "تعذّر إتمام تسجيل الدخول بحساب Google")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            GoogleCredentialFetchResult.Error(e.localizedMessage ?: "فشل تسجيل الدخول بحساب Google")
        }
    }

    suspend fun attemptAutoSignIn(context: Context): Boolean {
        if (runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)) return true
        val credential = fetchSilentGoogleCredential(context) ?: return false
        return try {
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                if (googleIdToken.isBlank()) return false
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                Firebase.auth.currentUser != null
            } else {
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Silent Firebase credential link failed: ${e.message}")
            false
        }
    }

    fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthCancelled: () -> Unit,
        onAuthError: (String) -> Unit,
    ) {
        scope.launch {
            when (val fetchResult = fetchInteractiveGoogleCredential(context, credentialManager)) {
                GoogleCredentialFetchResult.Cancelled -> onAuthCancelled()
                is GoogleCredentialFetchResult.Error -> onAuthError(fetchResult.message)
                is GoogleCredentialFetchResult.Success -> {
                    try {
                        val credential = fetchResult.credential
                        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                            if (googleIdToken.isBlank()) {
                                onAuthError("تعذّر الحصول على رمز التحقق من Google")
                                return@launch
                            }
                            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                            val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                            if (authResult.user != null) {
                                onAuthSuccess()
                            } else {
                                onAuthError("تعذّر ربط حساب Google مع Firebase")
                            }
                        } else {
                            onAuthError("نوع بيانات اعتماد غير مدعوم")
                        }
                    } catch (e: FirebaseAuthException) {
                        Log.e(TAG, "Firebase credential link failed: ${e.errorCode}", e)
                        onAuthError(e.localizedMessage ?: "فشل التحقق من الحساب في Firebase")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Google credential exchange failed", e)
                        onAuthError(e.localizedMessage ?: "فشل تسجيل الدخول بحساب Google")
                    }
                }
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {},
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}
