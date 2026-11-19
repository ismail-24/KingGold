package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isAuthenticated: Boolean = false,
)

interface FirebaseAuthGateway {
    suspend fun signInWithGoogleIdToken(idToken: String): Result<String>
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signUpWithEmail(fullName: String, email: String, password: String): Result<String>
    fun hasActiveUser(): Boolean
}

class DefaultFirebaseAuthGateway(
    private val authProvider: () -> FirebaseAuth = { Firebase.auth },
) : FirebaseAuthGateway {

    override fun hasActiveUser(): Boolean = runCatching { authProvider().currentUser != null }.getOrDefault(false)

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<String> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = authProvider().signInWithCredential(credential).await()
        val uid = result.user?.uid ?: throw IllegalStateException("لم يتم العثور على بيانات المستخدم بعد الربط")
        uid
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<String> = runCatching {
        val result = authProvider().signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("تعذّر تسجيل الدخول")
        uid
    }

    override suspend fun signUpWithEmail(fullName: String, email: String, password: String): Result<String> = runCatching {
        val result = authProvider().createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("تعذّر إنشاء الحساب")
        if (fullName.isNotBlank()) {
            runCatching {
                val updates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName.trim())
                    .build()
                user.updateProfile(updates).await()
            }
        }
        user.uid
    }
}

class AuthViewModel @JvmOverloads constructor(
    private val authGateway: FirebaseAuthGateway = DefaultFirebaseAuthGateway(),
    private val idTokenExtractor: (Credential) -> String = ::extractGoogleIdToken,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = authGateway.hasActiveUser()),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var silentSignInJob: Job? = null

    fun switchMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun updateFullName(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null, infoMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, infoMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, infoMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null, infoMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun attemptSilentGoogleSignIn(context: Context) {
        if (_uiState.value.isAuthenticated || _uiState.value.isLoading) return
        silentSignInJob?.cancel()
        silentSignInJob = viewModelScope.launch {
            val credential = GoogleAuthHelper.fetchSilentGoogleCredential(context) ?: return@launch
            handleGoogleCredentialResult(credential, silent = true)
        }
    }

    fun startGoogleSignIn(
        context: Context,
        credentialManager: CredentialManager,
    ) {
        silentSignInJob?.cancel()
        silentSignInJob = null
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            when (val result = GoogleAuthHelper.fetchInteractiveGoogleCredential(context, credentialManager)) {
                GoogleCredentialFetchResult.Cancelled -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "تم إلغاء تسجيل الدخول بحساب Google. عند التثبيت على هاتف فعلي (Release)، تأكد من تسجيل بصمتي SHA-1 و SHA-256 لشهادة التوقيع في Firebase Console.",
                        )
                    }
                }
                is GoogleCredentialFetchResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                        )
                    }
                }
                is GoogleCredentialFetchResult.Success -> {
                    handleGoogleCredentialResult(result.credential, silent = false)
                }
            }
        }
    }

    /**
     * Parses a Google ID Token credential returned from Credential Manager / Google Sign-In
     * and exchanges it with Firebase Authentication.
     */
    suspend fun handleGoogleCredentialResult(
        credential: Credential,
        silent: Boolean = false,
    ): Boolean {
        val idToken = try {
            idTokenExtractor(credential)
        } catch (e: GoogleIdTokenParsingException) {
            safeLogError("Failed to parse Google ID token credential", e)
            if (!silent) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "تعذّر قراءة بيانات حساب Google المختار. حاول مرة أخرى.",
                    )
                }
            }
            return false
        } catch (e: IllegalArgumentException) {
            safeLogError("Unsupported or empty Google credential", e)
            if (!silent) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "نوع بيانات اعتماد Google غير صالح",
                    )
                }
            }
            return false
        }

        return exchangeGoogleIdTokenWithFirebase(idToken = idToken, silent = silent)
    }

    suspend fun exchangeGoogleIdTokenWithFirebase(
        idToken: String,
        silent: Boolean = false,
    ): Boolean {
        if (idToken.isBlank()) {
            if (!silent) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "لم يتم استلام رمز التحقق (ID Token) من Google. تحقق من SHA-1 في Firebase Console.",
                    )
                }
            }
            return false
        }

        if (!silent) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
        }

        return try {
            val exchangeResult = authGateway.signInWithGoogleIdToken(idToken)
            val err = exchangeResult.exceptionOrNull()
            if (exchangeResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        infoMessage = null,
                        isAuthenticated = true,
                    )
                }
                true
            } else {
                if (err is CancellationException) throw err
                safeLogError("Firebase Google credential exchange failed", err)
                if (!silent) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapAuthError(err, defaultMessage = "تعذّر ربط حساب Google مع Firebase"),
                        )
                    }
                }
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            safeLogError("Unexpected Google auth error", e)
            if (!silent) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapAuthError(e, defaultMessage = "فشل تسجيل الدخول بحساب Google"),
                    )
                }
            }
            false
        }
    }

    private fun safeLogError(message: String, throwable: Throwable?) {
        runCatching { Log.e(TAG, message, throwable) }
    }

    fun submitTraditionalAuth() {
        val current = _uiState.value
        val trimmedName = current.fullName.trim()
        val trimmedEmail = current.email.trim()
        val password = current.password

        if (current.mode == AuthMode.SIGN_UP && trimmedName.length < 2) {
            _uiState.update { it.copy(errorMessage = "من فضلك أدخل الاسم الكامل (حرفان على الأقل)") }
            return
        }
        if (trimmedEmail.isBlank() || !EMAIL_REGEX.matches(trimmedEmail)) {
            _uiState.update { it.copy(errorMessage = "من فضلك أدخل بريداً إلكترونياً صحيحاً") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "كلمة المرور يجب أن تكون 6 أحرف على الأقل") }
            return
        }
        if (current.mode == AuthMode.SIGN_UP && password != current.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "تأكيد كلمة المرور غير متطابق") }
            return
        }

        silentSignInJob?.cancel()
        silentSignInJob = null
        _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            try {
                val result = if (current.mode == AuthMode.SIGN_IN) {
                    authGateway.signInWithEmail(trimmedEmail, password)
                } else {
                    authGateway.signUpWithEmail(trimmedName, trimmedEmail, password)
                }
                val err = result.exceptionOrNull()
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            infoMessage = null,
                            isAuthenticated = true,
                        )
                    }
                } else {
                    if (err is CancellationException) throw err
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapAuthError(
                                err,
                                defaultMessage = if (current.mode == AuthMode.SIGN_IN) {
                                    "تعذّر تسجيل الدخول بالبريد الإلكتروني"
                                } else {
                                    "تعذّر إنشاء الحساب الجديد"
                                },
                            ),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = mapAuthError(e, defaultMessage = "حدث خطأ أثناء المصادقة"),
                    )
                }
            }
        }
    }

    private fun mapAuthError(error: Throwable?, defaultMessage: String): String = when (error) {
        is FirebaseAuthInvalidUserException -> "لا يوجد حساب مسجّل بهذا البريد الإلكتروني"
        is FirebaseAuthWeakPasswordException -> "كلمة المرور ضعيفة، استخدم 6 أحرف على الأقل"
        is FirebaseAuthUserCollisionException -> "هذا البريد الإلكتروني مسجّل بالفعل، يمكنك تسجيل الدخول مباشرة"
        is FirebaseAuthInvalidCredentialsException -> "بيانات الدخول غير صحيحة أو منتهية الصلاحية"
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "صيغة البريد الإلكتروني غير صحيحة"
            "ERROR_NETWORK_REQUEST_FAILED" -> "تعذّر الاتصال بالإنترنت، حاول مرة أخرى"
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "الحساب مسجّل بطريقة دخول مختلفة، جرّب البريد الإلكتروني أو Google"
            else -> error.localizedMessage ?: defaultMessage
        }
        else -> error?.localizedMessage?.takeIf { it.isNotBlank() } ?: defaultMessage
    }

    companion object {
        private const val TAG = "AuthViewModel"
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun extractGoogleIdToken(credential: Credential): String {
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val parsed = GoogleIdTokenCredential.createFrom(credential.data)
                if (parsed.idToken.isBlank()) {
                    throw IllegalArgumentException("رمز التحقق (ID Token) من Google فارغ")
                }
                return parsed.idToken
            }
            throw IllegalArgumentException("نوع بيانات اعتماد غير مدعوم لتسجيل الدخول عبر Google")
        }
    }
}
