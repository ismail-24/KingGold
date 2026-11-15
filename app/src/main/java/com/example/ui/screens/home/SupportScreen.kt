package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.KingGoldRepository
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@Composable
fun SupportScreen(
    onOpenAiAdvisor: () -> Unit = {},
) {
    var expandedIndex by remember { mutableStateOf<Int?>(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Parchment)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "الدعم",
                    style = AppText.display.copy(fontSize = 22.sp),
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.Ink,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SupportAgent,
                            contentDescription = "فريق الدعم",
                            tint = AppColors.BrassLight,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "فريق الدعم متاح يومياً من 9 ص لـ 11 م",
                            style = AppText.body.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            item {
                Card(
                    onClick = onOpenAiAdvisor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_open_ai_advisor_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.45f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "مستشار الملك الذكي",
                            tint = AppColors.Brass,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "اسأل مستشار الملك الذكي (Gemini)",
                                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = "إجابات فورية حول الضمان، الدمغة، والأسعار",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = AppColors.Brass,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "أسئلة شائعة",
                    style = AppText.display.copy(fontSize = 16.sp),
                )
            }

            itemsIndexed(KingGoldRepository.supportFaqs) { index, (question, answer) ->
                val isExpanded = expandedIndex == index
                val shape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Color.White)
                        .border(1.dp, if (isExpanded) AppColors.Brass else AppColors.Line, shape)
                        .clickable { expandedIndex = if (isExpanded) null else index }
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = question,
                            modifier = Modifier.weight(1f),
                            style = AppText.body.copy(fontWeight = FontWeight.Bold),
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (isExpanded) "إخفاء الإجابة" else "عرض الإجابة",
                            tint = AppColors.Muted,
                        )
                    }
                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = answer,
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 13.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}
