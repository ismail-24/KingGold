package com.example.ui.screens.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.formatCurrency
import com.example.ui.state.AdvisorMessage
import com.example.ui.state.AiAdvisorUiState
import com.example.ui.state.AiGoldAdvisorViewModel
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdvisorScreen(
    uiState: KingGoldUiState,
    initialPrompt: String? = null,
    onBack: () -> Unit,
    advisorViewModel: AiGoldAdvisorViewModel = viewModel(),
) {
    val aiState by advisorViewModel.uiState.collectAsStateWithLifecycle()
    val messages by advisorViewModel.messages.collectAsStateWithLifecycle()
    var inputText by remember(initialPrompt) { mutableStateOf(initialPrompt.orEmpty()) }
    val listState = rememberLazyListState()

    val quickPrompts = remember {
        listOf(
            "أفضل خيار للادخار: عيار 24 ولا 21؟",
            "احسب لي إجمالي جنيه ذهب 8 جم اليوم",
            "قارن مصنعية المحلات المتاحة",
            "كيف أفحص الدمغة والميزان عند الاستلام؟",
        )
    }

    LaunchedEffect(messages.size, aiState) {
        if (messages.isNotEmpty() || aiState is AiAdvisorUiState.Streaming) {
            val target = (messages.size + if (aiState is AiAdvisorUiState.Streaming) 1 else 0)
                .coerceAtLeast(1) - 1
            listState.animateScrollToItem(target)
        }
    }

    val isGenerating = aiState is AiAdvisorUiState.Loading || aiState is AiAdvisorUiState.Streaming

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مستشار الملك الذكي",
                                style = AppText.display.copy(fontSize = 18.sp),
                            )
                            Text(
                                text = "تحليل الأسعار والعيارات عبر Gemini",
                                style = AppText.body.copy(fontSize = 11.sp, color = AppColors.Muted),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_advisor_back_button")) {
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
                color = Color.White,
                shadowElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_advisor_input"),
                            placeholder = {
                                Text(
                                    text = "اسأل عن العيار، المصنعية، أو حساب السعر...",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.sp),
                                )
                            },
                            maxLines = 3,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppColors.Parchment,
                                unfocusedContainerColor = AppColors.Parchment,
                                focusedBorderColor = AppColors.Brass,
                                unfocusedBorderColor = AppColors.Line,
                            ),
                        )
                        FilledIconButton(
                            onClick = {
                                val prompt = inputText.trim()
                                if (prompt.isNotEmpty() && !isGenerating) {
                                    inputText = ""
                                    advisorViewModel.askAdvisor(prompt, uiState.prices, uiState.shops)
                                }
                            },
                            enabled = inputText.isNotBlank() && !isGenerating,
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("ai_advisor_send_button"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = AppColors.Ink,
                                contentColor = AppColors.BrassLight,
                                disabledContainerColor = AppColors.Line,
                                disabledContentColor = AppColors.Muted,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال السؤال",
                            )
                        }
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
                state = listState,
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val p24 = uiState.gramPrice(24)
                    val p21 = uiState.gramPrice(21)
                    val p18 = uiState.gramPrice(18)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.Ink)
                            .padding(14.dp),
                    ) {
                        Text(
                            text = "مرتبط بأسعار اليوم اللحظية",
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "عيار 24: ${formatCurrency(p24, 0)} · عيار 21: ${formatCurrency(p21, 0)} · عيار 18: ${formatCurrency(p18, 0)} ج.م",
                            style = AppText.body.copy(color = Color.White, fontSize = 12.5.sp),
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "أسئلة سريعة",
                            style = AppText.body.copy(
                                color = AppColors.Muted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            quickPrompts.forEach { sample ->
                                AssistChip(
                                    onClick = {
                                        if (!isGenerating) {
                                            advisorViewModel.askAdvisor(sample, uiState.prices, uiState.shops)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = sample,
                                            style = AppText.body.copy(fontSize = 12.sp),
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color.White,
                                        labelColor = AppColors.Ink,
                                    ),
                                    border = BorderStroke(1.dp, AppColors.Line),
                                )
                            }
                        }
                    }
                }

                if (messages.isEmpty() && aiState is AiAdvisorUiState.Idle) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, AppColors.Line),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "أهلاً بك مع مستشار الملك الذكي",
                                    style = AppText.display.copy(fontSize = 17.sp),
                                )
                                Text(
                                    text = "اسألني عن الفرق بين السبائك والجنيهات، حساب التكلفة الإجمالية شاملة المصنعية في المحلات الشريكة، أو خطوات فحص الدمغة عند الاستلام.",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.sp),
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    AdvisorChatBubble(message = msg)
                }

                if (aiState is AiAdvisorUiState.Loading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AppColors.Brass,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "جاري تحليل الأسعار وصياغة الإجابة...",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.sp),
                            )
                        }
                    }
                }

                val streaming = aiState as? AiAdvisorUiState.Streaming
                if (streaming != null && streaming.text.isNotBlank()) {
                    item {
                        AdvisorChatBubble(
                            message = AdvisorMessage(
                                id = "streaming",
                                isUser = false,
                                text = streaming.text,
                            ),
                        )
                    }
                }

                val error = aiState as? AiAdvisorUiState.Error
                if (error != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.Alert.copy(alpha = 0.08f))
                                .border(1.dp, AppColors.Alert.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = "خطأ",
                                    tint = AppColors.Alert,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error.message,
                                    style = AppText.body.copy(color = AppColors.Alert, fontSize = 13.sp),
                                )
                            }
                            Button(
                                onClick = { advisorViewModel.retryLastQuestion() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Ink,
                                    contentColor = AppColors.BrassLight,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة المحاولة", style = AppText.body.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvisorChatBubble(message: AdvisorMessage) {
    val bgColor = if (message.isUser) AppColors.Ink else Color.White
    val textColor = if (message.isUser) AppColors.Parchment else AppColors.Ink
    val borderColor = if (message.isUser) AppColors.Ink else AppColors.Line

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            if (!message.isUser) {
                Text(
                    text = "مستشار الملك الذكي",
                    style = AppText.body.copy(
                        color = AppColors.Brass,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = message.text,
                style = AppText.body.copy(color = textColor, fontSize = 13.5.sp),
            )
        }
    }
}
