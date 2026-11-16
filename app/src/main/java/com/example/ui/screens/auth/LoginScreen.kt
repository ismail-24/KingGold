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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.SealBadge
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showPolicyDialog by remember { mutableStateOf<String?>(null) }
    var autoSignInJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        val job = coroutineScope.launch {
            val signedInSilently = GoogleAuthHelper.attemptAutoSignIn(context)
            if (signedInSilently) {
                onAuthSuccess()
            }
        }
        autoSignInJob = job
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Ink)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SealBadge(size = 72.dp)
                    Spacer(modifier = Modifier.height(14.dp))
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
                    Spacer(modifier = Modifier.height(20.dp))
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
                    .weight(1f, fill = false)
                    .padding(24.dp)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = "أهلاً بيك في ملك الذهب",
                    style = AppText.display.copy(fontSize = 22.sp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سجّل الدخول بحساب Google لحفظ طلباتك في السحابة وتأمين محفظة الضمان.",
                    style = AppText.body.copy(color = AppColors.Muted),
                )
                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, AppColors.Line),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "تسجيل الدخول الموثّق",
                            style = AppText.display.copy(fontSize = 17.sp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "دخول سريع وآمن ومرتبط بحسابك السحابي لحماية طلبات الشراء والمفضلة.",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (errorMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.Alert.copy(alpha = 0.08f))
                                    .border(1.dp, AppColors.Alert.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
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
                                    text = errorMessage.orEmpty(),
                                    style = AppText.body.copy(color = AppColors.Alert, fontSize = 12.5.sp),
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        } else if (infoMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.ParchmentDeep)
                                    .border(1.dp, AppColors.Line, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
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
                                    text = infoMessage.orEmpty(),
                                    style = AppText.body.copy(color = AppColors.Ink, fontSize = 12.5.sp),
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Button(
                            onClick = {
                                autoSignInJob?.cancel()
                                autoSignInJob = null
                                errorMessage = null
                                infoMessage = null
                                isLoading = true
                                GoogleAuthHelper.signInWithGoogle(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = coroutineScope,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthCancelled = {
                                        isLoading = false
                                        infoMessage = "تم إلغاء تسجيل الدخول بحساب Google. عند التثبيت على هاتف فعلي، تأكد من تسجيل بصمتي SHA-1 و SHA-256 لشهادة التوقيع في Firebase Console."
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Ink,
                                contentColor = AppColors.BrassLight,
                                disabledContainerColor = AppColors.Line,
                                disabledContentColor = AppColors.Muted,
                            ),
                            border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.5f)),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = AppColors.BrassLight,
                                    strokeWidth = 2.dp,
                                )
                            } else {
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
                                        fontSize = 15.5.sp,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
