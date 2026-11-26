package com.example.ui.screens.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoldProductImageLibrary
import com.example.model.GoldOrder
import com.example.model.OrderStatus
import com.example.ui.components.GoldMarketplaceImage
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.GoldImageTier

@Composable
fun OrdersScreen(
    uiState: KingGoldUiState,
    onOrderClick: (GoldOrder) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=all, 1=active, 2=completed

    val filtered = remember(uiState.orders, selectedTab) {
        uiState.orders.filter { order ->
            when (selectedTab) {
                1 -> order.status == OrderStatus.PAID ||
                    order.status == OrderStatus.PREPARING ||
                    order.status == OrderStatus.READY_FOR_PICKUP ||
                    order.status == OrderStatus.DISPUTED
                2 -> order.status == OrderStatus.COMPLETED
                else -> true
            }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "طلباتي",
                        style = AppText.display.copy(fontSize = 22.sp),
                    )
                    Text(
                        text = "تابع حالة طلباتك وأموالك المحفوظة",
                        style = AppText.body.copy(color = AppColors.Muted),
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = AppColors.Ink,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "الضمان",
                                tint = AppColors.BrassLight,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "محفوظ في الضمان",
                                style = AppText.body.copy(color = AppColors.BrassLight, fontSize = 12.sp),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${formatCurrency(uiState.escrowHeld)} ج.م",
                            style = AppText.display.copy(color = Color.White, fontSize = 26.sp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "لا يصل المبلغ لأي محل إلا بعد استلامك للذهب.",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderTabChip("الكل", selectedTab == 0) { selectedTab = 0 }
                    OrderTabChip("النشطة", selectedTab == 1) { selectedTab = 1 }
                    OrderTabChip("المكتملة", selectedTab == 2) { selectedTab = 2 }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = AppColors.Muted,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "مفيش طلبات هنا دلوقتي",
                            style = AppText.body.copy(color = AppColors.Muted),
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { order ->
                    OrderTile(order = order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

@Composable
private fun OrderTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) AppColors.Ink else Color.White)
            .border(1.dp, if (selected) AppColors.Ink else AppColors.Line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppText.body.copy(
                color = if (selected) AppColors.BrassLight else AppColors.Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun OrderTile(
    order: GoldOrder,
    onClick: () -> Unit,
) {
    val statusColor = when (order.status) {
        OrderStatus.COMPLETED -> AppColors.Muted
        OrderStatus.DISPUTED -> AppColors.Alert
        else -> AppColors.Trust
    }
    val leadProduct = order.items.firstOrNull()?.product
    val leadImagery = leadProduct?.let {
        GoldProductImageLibrary.resolveProductImagery(it, GoldImageTier.THUMBNAIL)
    }
    val thumbUrl = leadImagery?.primaryUrl
        ?: GoldProductImageLibrary.shopLogoUrl(order.shop, GoldImageTier.THUMBNAIL)
    val thumbPlaceholder = leadImagery?.placeholderRes
        ?: GoldProductImageLibrary.placeholderDrawableForShopLogo(order.shop)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, AppColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("order_card_${order.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoldMarketplaceImage(
            imageUrl = thumbUrl,
            base64Thumbnail = leadProduct?.imageThumbnails?.firstOrNull(),
            contentDescription = leadProduct?.name ?: order.shop.name,
            modifier = Modifier.size(56.dp),
            cornerRadius = 12.dp,
            isThumbnail = true,
            imageTier = GoldImageTier.THUMBNAIL,
            placeholderRes = thumbPlaceholder,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "طلب ${order.id}",
                    style = AppText.body.copy(fontWeight = FontWeight.ExtraBold),
                )
                Text(
                    text = "${formatCurrency(order.total)} ج.م",
                    style = AppText.body.copy(fontWeight = FontWeight.ExtraBold),
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${order.shop.name} · ${order.pickupWindow}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
            )
            if (leadProduct != null) {
                Text(
                    text = if (order.items.size > 1) {
                        "${leadProduct.name} +${order.items.size - 1}"
                    } else {
                        leadProduct.name
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppText.body.copy(color = AppColors.InkSoft, fontSize = 11.5.sp),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = order.status.arabicLabel,
                    style = AppText.body.copy(
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
