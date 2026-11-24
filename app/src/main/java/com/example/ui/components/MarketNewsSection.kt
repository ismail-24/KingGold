package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarketNewsParser
import com.example.model.MarketNewsArticle
import com.example.model.MarketNewsSource
import com.example.model.MarketNewsTopic
import com.example.model.MarketNewsUiState
import com.example.model.NewsImpactDirection
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketNewsSectionCard(
    newsState: MarketNewsUiState,
    selectedTopic: MarketNewsTopic,
    onSelectTopic: (MarketNewsTopic) -> Unit,
    onRefresh: () -> Unit,
    onOpenFullNews: (() -> Unit)? = null,
    maxArticlesInPreview: Int = 3,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    fun openSafeUrl(url: String) {
        if (!MarketNewsParser.isSafeHttpUrl(url)) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("market_news_section_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.42f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.Ink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Newspaper,
                            contentDescription = "أخبار سوق الذهب",
                            tint = AppColors.BrassLight,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "أخبار السوق المباشرة",
                            style = AppText.display.copy(fontSize = 17.sp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.LiveDot),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "مدعومة ببحث Google الفوري للذهب العالمي",
                                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("market_news_refresh_button"),
                ) {
                    if (newsState is MarketNewsUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.Brass,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "تحديث أخبار السوق عبر Google",
                            tint = AppColors.Brass,
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MarketNewsTopic.entries.forEach { topic ->
                    FilterChip(
                        selected = selectedTopic == topic,
                        onClick = { onSelectTopic(topic) },
                        modifier = Modifier.testTag("market_news_topic_${topic.name.lowercase()}"),
                        label = {
                            Text(
                                text = topic.label,
                                style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Ink,
                            selectedLabelColor = AppColors.BrassLight,
                            containerColor = AppColors.Parchment,
                            labelColor = AppColors.Ink,
                        ),
                    )
                }
            }

            when (newsState) {
                is MarketNewsUiState.Idle -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_news_idle_state"),
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.Parchment,
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "اجلب أحدث أخبار أسعار الذهب العالمية وقرارات البنوك المركزية مباشرة من بحث Google.",
                                style = AppText.body.copy(color = AppColors.Ink, fontSize = 12.5.sp),
                            )
                            Button(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("market_news_fetch_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Ink,
                                    contentColor = AppColors.BrassLight,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جلب أحدث أخبار السوق عبر Google",
                                    style = AppText.body.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }

                is MarketNewsUiState.Loading -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_news_loading_state"),
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.Parchment,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = AppColors.Brass,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "جاري البحث في Google عن أحدث أخبار الذهب...",
                                    style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                )
                                Text(
                                    text = "يتم جمع المؤشرات العالمية والمصادر الموثّقة الآن",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
                                )
                            }
                        }
                    }
                }

                is MarketNewsUiState.Error -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_news_error_state"),
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.Alert.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, AppColors.Alert.copy(alpha = 0.3f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = AppColors.Alert,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = newsState.message,
                                    style = AppText.body.copy(
                                        color = AppColors.Alert,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                            OutlinedButton(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("market_news_retry_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AppColors.Alert),
                            ) {
                                Text(
                                    text = "إعادة المحاولة",
                                    style = AppText.body.copy(color = AppColors.Alert, fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }
                }

                is MarketNewsUiState.Success -> {
                    val feed = newsState.feed
                    if (feed.macroSummary.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("market_news_macro_summary"),
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.Ink,
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "ملخص اتجاه السوق · Google Search",
                                        style = AppText.body.copy(
                                            color = AppColors.BrassLight,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                    Text(
                                        text = feed.fetchedAtLabel,
                                        style = AppText.body.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp),
                                    )
                                }
                                Text(
                                    text = feed.macroSummary,
                                    style = AppText.body.copy(color = Color.White, fontSize = 12.5.sp),
                                )
                            }
                        }
                    }

                    val displayedArticles = if (onOpenFullNews != null) {
                        feed.articles.take(maxArticlesInPreview)
                    } else {
                        feed.articles
                    }

                    displayedArticles.forEachIndexed { idx, article ->
                        MarketNewsArticleRow(
                            article = article,
                            index = idx,
                            onOpenUrl = ::openSafeUrl,
                        )
                    }

                    if (feed.groundingSources.isNotEmpty()) {
                        HorizontalDivider(color = AppColors.ParchmentDeep)
                        Text(
                            text = "المصادر المرجعية من بحث Google",
                            style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AppColors.Muted),
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            feed.groundingSources.take(5).forEachIndexed { index, source ->
                                GroundingSourceChip(
                                    source = source,
                                    index = index,
                                    onClick = { openSafeUrl(source.url) },
                                )
                            }
                        }
                    }

                    if (onOpenFullNews != null) {
                        OutlinedButton(
                            onClick = onOpenFullNews,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("market_news_open_full_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AppColors.Brass),
                        ) {
                            Text(
                                text = "عرض كل أخبار السوق والمصادر (${feed.articles.size})",
                                style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = AppColors.Brass,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketNewsArticleRow(
    article: MarketNewsArticle,
    index: Int,
    onOpenUrl: (String) -> Unit,
) {
    val impactBg = when (article.impact) {
        NewsImpactDirection.BULLISH -> AppColors.TrustSoft
        NewsImpactDirection.BEARISH -> AppColors.Alert.copy(alpha = 0.10f)
        NewsImpactDirection.NEUTRAL -> AppColors.ParchmentDeep
    }
    val impactTint = when (article.impact) {
        NewsImpactDirection.BULLISH -> AppColors.Trust
        NewsImpactDirection.BEARISH -> AppColors.Alert
        NewsImpactDirection.NEUTRAL -> AppColors.Ink
    }
    val impactIcon = when (article.impact) {
        NewsImpactDirection.BULLISH -> Icons.AutoMirrored.Outlined.TrendingUp
        NewsImpactDirection.BEARISH -> Icons.AutoMirrored.Outlined.TrendingDown
        NewsImpactDirection.NEUTRAL -> Icons.AutoMirrored.Outlined.TrendingFlat
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("market_news_article_$index"),
        shape = RoundedCornerShape(14.dp),
        color = AppColors.Parchment,
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = impactBg,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = impactIcon,
                            contentDescription = article.impact.label,
                            tint = impactTint,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = article.impact.label,
                            style = AppText.body.copy(
                                color = impactTint,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
                Text(
                    text = "${article.regionTag} · ${article.publishedHint}",
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp),
                )
            }

            Text(
                text = article.title,
                style = AppText.display.copy(fontSize = 14.5.sp),
            )
            Text(
                text = article.summary,
                style = AppText.body.copy(color = AppColors.InkSoft, fontSize = 12.sp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "المصدر: ${article.sourceTitle}",
                    style = AppText.body.copy(
                        color = AppColors.Brass,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (MarketNewsParser.isSafeHttpUrl(article.sourceUrl)) {
                    TextButton(
                        onClick = { onOpenUrl(article.sourceUrl) },
                        modifier = Modifier.testTag("market_news_source_link_$index"),
                    ) {
                        Text(
                            text = "اقرأ المصدر",
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                            ),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = "فتح المصدر في المتصفح",
                            tint = AppColors.Brass,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroundingSourceChip(
    source: MarketNewsSource,
    index: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("market_news_grounding_chip_$index"),
        shape = RoundedCornerShape(10.dp),
        color = AppColors.Parchment,
        border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "فتح ${source.title}",
                tint = AppColors.Brass,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = source.domain.ifBlank { source.title },
                style = AppText.body.copy(
                    color = AppColors.Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
