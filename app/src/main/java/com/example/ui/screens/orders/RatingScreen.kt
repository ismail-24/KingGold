package com.example.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GoldOrder
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

private val ReviewTags = listOf("دقة الوزن", "حسن التعامل", "سرعة التجهيز", "وضوح الفاتورة")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingScreen(
    order: GoldOrder,
    onSubmitRating: (stars: Int) -> Unit,
) {
    var stars by remember { mutableIntStateOf(4) }
    val selectedTags = remember { mutableStateListOf("دقة الوزن", "حسن التعامل") }

    Scaffold(
        containerColor = AppColors.Parchment,
        bottomBar = {
            Surface(
                color = AppColors.Parchment,
                shadowElevation = 4.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = { onSubmitRating(stars) },
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_rating_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brass,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "إرسال التقييم",
                            style = AppText.body.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(AppColors.TrustSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "تم الاستلام",
                                tint = AppColors.Trust,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "تم الاستلام بنجاح",
                            style = AppText.display.copy(fontSize = 24.sp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "مبروك على ذهبك. تم تحويل المبلغ للمحل.",
                            textAlign = TextAlign.Center,
                            style = AppText.body.copy(color = AppColors.Muted),
                        )
                    }
                }

                item {
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(Color.White)
                            .border(1.dp, AppColors.Line, shape)
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "قيّم تجربتك مع ${order.shop.name}",
                            style = AppText.display.copy(fontSize = 17.sp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تقييمك بيساعد باقي العملاء ويحافظ على جودة المحلات",
                            textAlign = TextAlign.Center,
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            for (i in 1..5) {
                                val filled = i <= stars
                                IconButton(
                                    onClick = { stars = i },
                                    modifier = Modifier.testTag("star_$i"),
                                ) {
                                    Icon(
                                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "$i نجوم",
                                        tint = AppColors.Brass,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReviewTags.forEach { tag ->
                                val selected = selectedTags.contains(tag)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) selectedTags.remove(tag) else selectedTags.add(tag)
                                    },
                                    label = {
                                        Text(
                                            text = tag,
                                            style = AppText.body.copy(fontSize = 12.5.sp),
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.Ink,
                                        selectedLabelColor = AppColors.BrassLight,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
