package com.example.ui.screens.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SealBadge
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@Composable
fun LoginScreen(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    var showPolicyDialog by remember { mutableStateOf<String?>(null) }
    val digitsOnly = phoneNumber.filter { it.isDigit() }
    val isPhoneValid = digitsOnly.length >= 9

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
                    text = "أهلاً بيك",
                    style = AppText.display.copy(fontSize = 22.sp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اكتب رقم موبايلك وهنبعتلك رمز تحقق.",
                    style = AppText.body.copy(color = AppColors.Muted),
                )
                Spacer(modifier = Modifier.height(18.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = onPhoneChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input"),
                        prefix = {
                            Text(
                                text = "+20  ",
                                style = AppText.body.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Ink,
                                ),
                            )
                        },
                        placeholder = {
                            Text(
                                text = "10X XXXX XXXX",
                                style = AppText.body.copy(color = AppColors.Muted),
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = AppColors.Brass,
                            unfocusedBorderColor = AppColors.Line,
                        ),
                    )
                }

                if (!isPhoneValid && phoneNumber.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "أدخل رقم موبايل مصري صحيح (9 أرقام على الأقل)",
                        style = AppText.body.copy(color = AppColors.Alert, fontSize = 12.sp),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onContinue,
                    enabled = isPhoneValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_continue_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Brass,
                        contentColor = Color.White,
                        disabledContainerColor = AppColors.Line,
                        disabledContentColor = AppColors.Muted,
                    ),
                ) {
                    Text(
                        text = "متابعة",
                        style = AppText.body.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
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
