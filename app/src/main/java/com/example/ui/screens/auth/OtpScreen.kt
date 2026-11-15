package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    phoneNumber: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    onFillDemoOtp: () -> Unit,
    onBack: () -> Unit,
    onVerifySuccess: () -> Unit,
) {
    var secondsLeft by remember { mutableIntStateOf(42) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    LaunchedEffect(Unit) {
        if (otpCode.isEmpty()) {
            onFillDemoOtp()
        }
        focusRequester.requestFocus()
    }

    val maskedPhone = if (phoneNumber.length >= 4) {
        "+20 10X XXX XX${phoneNumber.takeLast(2)}"
    } else {
        "+20 10X XXX XX42"
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = AppColors.Ink,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Parchment,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = "أدخل رمز التحقق",
                    style = AppText.display.copy(fontSize = 22.sp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "بعتنا رمز من 4 أرقام على الرقم $maskedPhone",
                    style = AppText.body.copy(color = AppColors.Muted),
                )
                Spacer(modifier = Modifier.height(28.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = otpCode,
                            onValueChange = onOtpChange,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("otp_input"),
                            decorationBox = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    repeat(4) { index ->
                                        val char = otpCode.getOrNull(index)?.toString() ?: ""
                                        val isActive = otpCode.length == index
                                        Box(
                                            modifier = Modifier
                                                .size(width = 64.dp, height = 68.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White)
                                                .border(
                                                    width = if (isActive) 1.8.dp else 1.dp,
                                                    color = if (isActive) AppColors.Brass else AppColors.Line,
                                                    shape = RoundedCornerShape(12.dp),
                                                )
                                                .clickable { focusRequester.requestFocus() },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = char,
                                                style = AppText.display.copy(fontSize = 24.sp),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (secondsLeft > 0) {
                        val secStr = secondsLeft.toString().padStart(2, '0')
                        Text(
                            text = "إعادة إرسال الرمز بعد 00:$secStr",
                            style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        )
                    } else {
                        TextButton(onClick = {
                            secondsLeft = 42
                            onFillDemoOtp()
                        }) {
                            Text(
                                text = "إعادة إرسال الرمز",
                                style = AppText.body.copy(
                                    color = AppColors.Brass,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }

                    TextButton(onClick = onFillDemoOtp) {
                        Text(
                            text = "رمز تجريبي (4821)",
                            style = AppText.body.copy(
                                color = AppColors.Muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "تحذير أمان",
                        tint = AppColors.Muted,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "متشاركش الرمز مع أي حد، حتى لو قال إنه من King Gold",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onVerifySuccess,
                    enabled = otpCode.length == 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("otp_confirm_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Brass,
                        contentColor = Color.White,
                        disabledContainerColor = AppColors.Line,
                        disabledContentColor = AppColors.Muted,
                    ),
                ) {
                    Text(
                        text = "تأكيد",
                        style = AppText.body.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                }
            }
        }
    }
}
