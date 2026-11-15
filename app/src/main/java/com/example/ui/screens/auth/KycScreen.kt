package com.example.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycScreen(
    frontDone: Boolean,
    backDone: Boolean,
    selfieDone: Boolean,
    onStepCompleted: (Int) -> Unit,
    onSubmitKyc: () -> Unit,
    onBack: () -> Unit,
) {
    var pendingStep by remember { mutableIntStateOf(2) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onStepCompleted(pendingStep)
        }
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "وثّق هويتك",
                        style = AppText.display.copy(fontSize = 20.sp),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = AppColors.Ink,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Parchment),
            )
        },
        bottomBar = {
            Surface(
                color = AppColors.Parchment,
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = {
                            onSubmitKyc()
                            onBack()
                        },
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("kyc_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brass,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "رفع ومتابعة",
                            style = AppText.body.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = AppColors.Muted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "نستخدم بياناتك لإصدار الفاتورة وتأمين الاستلام فقط.",
                            style = AppText.body.copy(fontSize = 11.sp, color = AppColors.Muted),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "خطوة واحدة بتخليك تشتري بثقة وتستلم ذهبك من غير تعطيل.",
                        style = AppText.body.copy(color = AppColors.Muted),
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.TrustSoft)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = "توثيق الهوية",
                            tint = AppColors.Trust,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "لإصدار فاتورة الشراء باسمك، ولتأمين الاستلام بحيث ماحدش غيرك يستلم طلبك.",
                            style = AppText.body.copy(color = AppColors.Trust, fontSize = 12.5.sp),
                        )
                    }
                }

                item {
                    KycUploadTile(
                        title = "صورة البطاقة · الوجه",
                        step = 1,
                        done = frontDone,
                        active = !frontDone,
                        onPickPhoto = {
                            pendingStep = 1
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onQuickConfirm = { onStepCompleted(1) },
                    )
                }

                item {
                    KycUploadTile(
                        title = "صورة البطاقة · الظهر",
                        step = 2,
                        done = backDone,
                        active = frontDone && !backDone,
                        onPickPhoto = {
                            pendingStep = 2
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onQuickConfirm = { onStepCompleted(2) },
                    )
                }

                item {
                    KycUploadTile(
                        title = "صورة شخصية",
                        step = 3,
                        done = selfieDone,
                        active = frontDone && backDone && !selfieDone,
                        onPickPhoto = {
                            pendingStep = 3
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onQuickConfirm = { onStepCompleted(3) },
                    )
                }
            }
        }
    }
}

@Composable
private fun KycUploadTile(
    title: String,
    step: Int,
    done: Boolean,
    active: Boolean,
    onPickPhoto: () -> Unit,
    onQuickConfirm: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (active) 1.6.dp else 1.dp,
                color = if (active) AppColors.Brass else AppColors.Line,
                shape = shape,
            )
            .clickable(onClick = onQuickConfirm)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = AppText.body.copy(fontWeight = FontWeight.Bold),
            )
            if (done) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "مكتمل",
                    tint = AppColors.Trust,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(AppColors.ParchmentDeep),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$step",
                        style = AppText.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }

        if (active) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Parchment)
                    .border(1.dp, AppColors.Line, RoundedCornerShape(10.dp))
                    .clickable(onClick = onPickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "اختيار صورة",
                        tint = AppColors.Brass,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط لاختيار صورة أو التفعيل السريع",
                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                    )
                }
            }
        }
    }
}
