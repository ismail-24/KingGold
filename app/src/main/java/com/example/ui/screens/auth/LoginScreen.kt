package com.example.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthMode
import com.example.ui.auth.AuthViewModel
import com.example.ui.components.SealBadge
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.attemptSilentGoogleSignIn(context)
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = AppColors.Parchment,
        unfocusedContainerColor = AppColors.Parchment,
        focusedBorderColor = AppColors.Brass,
        unfocusedBorderColor = AppColors.Line,
    )

    Scaffold(
        containerColor = AppColors.Parchment,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Ink)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SealBadge(size = 68.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "King Gold",
                        style = AppText.display.copy(
                            color = AppColors.Parchment,
                            fontSize = 26.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اشترِ الذهب من محلات حقيقية قريبة منك،\nوادفع بأمان لحد ما تستلم.",
                        textAlign = TextAlign.Center,
                        style = AppText.body.copy(
                            color = AppColors.Muted,
                            fontSize = 13.5.sp,
                        ),
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TrustChip(Icons.Outlined.LocationOn, "استلام بنفسك")
                        TrustChip(Icons.Outlined.Lock, "ضمان حتى الاستلام")
                        TrustChip(Icons.Outlined.Verified, "محلات موثّقة")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = "أهلاً بيك في ملك الذهب",
                    style = AppText.display.copy(fontSize = 22.sp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "سجّل الدخول بالبريد الإلكتروني أو أنشئ حساباً جديداً لتأمين خزنتك وطلبات الضمان.",
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.sp),
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppColors.Line),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Sign In / Sign Up Mode Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.ParchmentDeep)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AuthModeTab(
                                label = "تسجيل الدخول",
                                selected = uiState.mode == AuthMode.SIGN_IN,
                                onClick = { authViewModel.switchMode(AuthMode.SIGN_IN) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_mode_sign_in_tab"),
                            )
                            AuthModeTab(
                                label = "إنشاء حساب",
                                selected = uiState.mode == AuthMode.SIGN_UP,
                                onClick = { authViewModel.switchMode(AuthMode.SIGN_UP) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("auth_mode_sign_up_tab"),
                            )
                        }

                        if (uiState.errorMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.Alert.copy(alpha = 0.08f))
                                    .border(1.dp, AppColors.Alert.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                                    .testTag("auth_error_banner"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = "خطأ في تسجيل الدخول",
                                    tint = AppColors.Alert,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.errorMessage.orEmpty(),
                                    style = AppText.body.copy(color = AppColors.Alert, fontSize = 12.5.sp),
                                )
                            }
                        } else if (uiState.infoMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.ParchmentDeep)
                                    .border(1.dp, AppColors.Line, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                                    .testTag("auth_info_banner"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "تنبيه",
                                    tint = AppColors.Brass,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.infoMessage.orEmpty(),
                                    style = AppText.body.copy(color = AppColors.Ink, fontSize = 12.5.sp),
                                )
                            }
                        }

                        if (uiState.mode == AuthMode.SIGN_UP) {
                            OutlinedTextField(
                                value = uiState.fullName,
                                onValueChange = authViewModel::updateFullName,
                                enabled = !uiState.isLoading,
                                label = { Text("الاسم الكامل") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = AppColors.Muted)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input"),
                            )
                        }

                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = authViewModel::updateEmail,
                            enabled = !uiState.isLoading,
                            label = { Text("البريد الإلكتروني") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = AppColors.Muted)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input"),
                        )

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = authViewModel::updatePassword,
                            enabled = !uiState.isLoading,
                            label = { Text("كلمة المرور") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppColors.Muted)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                        tint = AppColors.Muted,
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (uiState.mode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input"),
                        )

                        if (uiState.mode == AuthMode.SIGN_UP) {
                            OutlinedTextField(
                                value = uiState.confirmPassword,
                                onValueChange = authViewModel::updateConfirmPassword,
                                enabled = !uiState.isLoading,
                                label = { Text("تأكيد كلمة المرور") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppColors.Muted)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                            contentDescription = if (confirmPasswordVisible) "إخفاء تأكيد كلمة المرور" else "إظهار تأكيد كلمة المرور",
                                            tint = AppColors.Muted,
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_confirm_password_input"),
                            )
                        }

                        Button(
                            onClick = authViewModel::submitTraditionalAuth,
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_submit_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Brass,
                                contentColor = Color.White,
                                disabledContainerColor = AppColors.Line,
                                disabledContentColor = AppColors.Muted,
                            ),
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = if (uiState.mode == AuthMode.SIGN_IN) "تسجيل الدخول" else "إنشاء حساب جديد",
                                    style = AppText.body.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp,
                                    ),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.Line)
                            Text(
                                text = "أو المتابعة عبر",
                                modifier = Modifier.padding(horizontal = 10.dp),
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.Line)
                        }

                        OutlinedButton(
                            onClick = { authViewModel.startGoogleSignIn(context, credentialManager) },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AppColors.Ink,
                                contentColor = AppColors.BrassLight,
                                disabledContainerColor = AppColors.Line,
                                disabledContentColor = AppColors.Muted,
                            ),
                            border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.55f)),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = AppColors.BrassLight,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "المتابعة بحساب Google",
                                style = AppText.body.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val policyText = buildAnnotatedString {
                    append("بالمتابعة أنت بتوافق على ")
                    withStyle(SpanStyle(color = AppColors.Brass, fontWeight = FontWeight.Bold)) {
                        append("الشروط والأحكام")
                    }
                    append(" و")
                    withStyle(SpanStyle(color = AppColors.Brass, fontWeight = FontWeight.Bold)) {
                        append("سياسة الخصوصية")
                    }
                }
                Text(
                    text = policyText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPolicyDialog = "تخضع جميع عمليات الشراء عبر King Gold لنظام الضمان الموثّق؛ لا يُصرف المبلغ للمحل إلا بعد استلامك القطع ومطابقة الفاتورة."
                        }
                        .padding(vertical = 8.dp),
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                )
            }
        }
    }

    if (showPolicyDialog != null) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = null },
            containerColor = AppColors.Parchment,
            title = {
                Text(
                    text = "الشروط والخصوصية",
                    style = AppText.display.copy(fontSize = 19.sp),
                )
            },
            text = {
                Text(
                    text = showPolicyDialog.orEmpty(),
                    style = AppText.body.copy(fontSize = 13.5.sp),
                )
            },
            confirmButton = {
                TextButton(onClick = { showPolicyDialog = null }) {
                    Text(
                        text = "حسناً",
                        style = AppText.body.copy(
                            color = AppColors.Brass,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            },
        )
    }
}

@Composable
private fun AuthModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AppColors.Ink else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppText.body.copy(
                color = if (selected) AppColors.BrassLight else AppColors.Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
            ),
        )
    }
}

@Composable
private fun TrustChip(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AppColors.BrassLight,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp),
        )
    }
}
