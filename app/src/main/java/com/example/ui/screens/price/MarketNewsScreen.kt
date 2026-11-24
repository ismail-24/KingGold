package com.example.ui.screens.price

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MarketNewsTopic
import com.example.model.MarketNewsUiState
import com.example.ui.components.MarketNewsSectionCard
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarketNewsScreen(
    uiState: KingGoldUiState,
    onSelectTopic: (MarketNewsTopic) -> Unit,
    onRefreshNews: () -> Unit,
    onOpenAiAdvisor: ((String) -> Unit)? = null,
    onBack: () -> Unit,
    autoFetchIfIdle: Boolean = true,
) {
    LaunchedEffect(autoFetchIfIdle, uiState.marketNewsState) {
        if (autoFetchIfIdle && uiState.marketNewsState is MarketNewsUiState.Idle) {
            onRefreshNews()
        }
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "أخبار سوق الذهب",
                        style = AppText.display.copy(fontSize = 20.sp),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("market_news_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = AppColors.Ink,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefreshNews,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("market_news_top_refresh_button"),
                    ) {
                        if (uiState.marketNewsState is MarketNewsUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Brass,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "تحديث الأخبار",
                                tint = AppColors.Brass,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Parchment),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MarketNewsSectionCard(
                    newsState = uiState.marketNewsState,
                    selectedTopic = uiState.marketNewsTopic,
                    onSelectTopic = onSelectTopic,
                    onRefresh = onRefreshNews,
                )

                val successFeed = (uiState.marketNewsState as? MarketNewsUiState.Success)?.feed
                if (successFeed != null && successFeed.searchQueries.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_news_queries_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, AppColors.Line),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = AppColors.Brass,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "استعلامات Google Search المستخدمة",
                                    style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp),
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                successFeed.searchQueries.forEach { query ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppColors.Parchment,
                                    ) {
                                        Text(
                                            text = query,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (onOpenAiAdvisor != null) {
                    OutlinedButton(
                        onClick = {
                            val summaryHint = successFeed?.macroSummary?.take(180).orEmpty()
                            val prompt = if (summaryHint.isNotBlank()) {
                                "بناءً على آخر أخبار السوق ($summaryHint)، ما توقعاتك لحركة الذهب عيار 21 و24 الآن؟"
                            } else {
                                "حلّل لي أهم أخبار سوق الذهب العالمي اليوم وتأثيرها على أسعار الشراء."
                            }
                            onOpenAiAdvisor(prompt)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("market_news_ask_ai_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppColors.Brass),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ناقش تأثير هذه الأخبار مع مستشار Gemini",
                            style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}
