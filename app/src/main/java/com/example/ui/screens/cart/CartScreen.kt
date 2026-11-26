package com.example.ui.screens.cart

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.data.KingGoldRepository
import com.example.model.MoneyUtils
import com.example.ui.components.GoldMarketplaceImage
import com.example.ui.components.LedgerCard
import com.example.ui.components.LedgerLine
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.GoldImageTier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CartScreen(
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSelectPickupSlot: (Int) -> Unit,
    onRefreshPriceLock: () -> Unit,
    onProceedToCheckout: () -> Unit,
    onBrowseShops: () -> Unit = onBack,
) {
    val mm = (uiState.priceLockSecondsRemaining / 60).toString().padStart(2, '0')
    val ss = (uiState.priceLockSecondsRemaining % 60).toString().padStart(2, '0')

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "سلة الطلب",
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
                        onClick = onProceedToCheckout,
                        enabled = uiState.cart.isNotEmpty(),
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("cart_checkout_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brass,
                            contentColor = Color.White,
                            disabledContainerColor = AppColors.Line,
                        ),
                    ) {
                        Text(
                            text = "متابعة للدفع",
                            style = AppText.body.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "لسه مفيش دفع. المبلغ بيتحجز في الضمان في الخطوة الجاية.",
                        style = AppText.body.copy(fontSize = 11.sp, color = AppColors.Muted),
                    )
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
            if (uiState.cart.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        tint = AppColors.Muted,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "السلة فاضية",
                        style = AppText.display.copy(fontSize = 20.sp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اختار قطعة ذهب من أي محل موثّق عشان تثبّت سعرها.",
                        style = AppText.body.copy(color = AppColors.Muted),
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onBrowseShops,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("cart_browse_shops_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Ink,
                            contentColor = AppColors.BrassLight,
                        ),
                    ) {
                        Text(
                            text = "استكشف المحلات",
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                            ),
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "من ${uiState.cartShop?.name.orEmpty()}",
                            style = AppText.body.copy(color = AppColors.Muted),
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.InkSoft)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "تثبيت السعر",
                                tint = AppColors.BrassLight,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.priceLockSecondsRemaining > 0) {
                                    "سعر الذهب مثبّت لك — $mm:$ss"
                                } else {
                                    "انتهت مدة تثبيت السعر"
                                },
                                modifier = Modifier.weight(1f),
                                style = AppText.body.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                ),
                            )
                            if (uiState.priceLockSecondsRemaining <= 60) {
                                TextButton(onClick = onRefreshPriceLock) {
                                    Text(
                                        text = "تحديث",
                                        style = AppText.body.copy(
                                            color = AppColors.BrassLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.cart, key = { "${it.product.shopId}:${it.product.id}" }) { item ->
                        val imagery = GoldProductImageLibrary.resolveProductImagery(
                            product = item.product,
                            tier = GoldImageTier.THUMBNAIL,
                        )
                        val shape = RoundedCornerShape(14.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(Color.White)
                                .border(1.dp, AppColors.Line, shape)
                                .padding(12.dp)
                                .testTag("cart_item_row_${item.product.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val lineSubtotalEgp = MoneyUtils.piastersToEgp(
                                uiState.productFinalPricePiasters(item.product) * item.quantity,
                            )
                            GoldMarketplaceImage(
                                imageUrl = imagery.primaryUrl,
                                base64Thumbnail = item.product.imageThumbnails.firstOrNull(),
                                contentDescription = item.product.name,
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("cart_item_image_${item.product.id}"),
                                cornerRadius = 10.dp,
                                isThumbnail = true,
                                imageTier = GoldImageTier.THUMBNAIL,
                                placeholderRes = imagery.placeholderRes,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = AppText.body.copy(fontWeight = FontWeight.ExtraBold),
                                )
                                Text(
                                    text = "عيار ${item.product.karat} · ${item.product.weightGrams} جم",
                                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                                )
                                Text(
                                    text = "${formatCurrency(lineSubtotalEgp)} ج.م",
                                    style = AppText.body.copy(
                                        color = AppColors.Brass,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    ),
                                )
                            }
                            IconButton(onClick = { onRemove(item.product.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = AppColors.Alert,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onDecrement(item.product.id) },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Remove,
                                        contentDescription = "إنقاص",
                                        tint = AppColors.Ink,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    text = "${item.quantity}",
                                    style = AppText.body.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                IconButton(
                                    onClick = { onIncrement(item.product.id) },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "زيادة",
                                        tint = AppColors.Ink,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text(
                                text = "موعد الاستلام",
                                style = AppText.display.copy(fontSize = 16.sp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                KingGoldRepository.pickupSlots.forEachIndexed { index, slot ->
                                    val selected = uiState.selectedPickupSlotIndex == index
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectPickupSlot(index) },
                                        label = {
                                            Text(
                                                text = slot,
                                                style = AppText.body.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                ),
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AppColors.Ink,
                                            selectedLabelColor = AppColors.BrassLight,
                                            containerColor = Color.White,
                                            labelColor = AppColors.Ink,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LedgerCard(
                            lines = listOf(
                                LedgerLine("إجمالي سعر القطع", formatCurrency(uiState.cartGoldValue)),
                                LedgerLine("المصنعية الإضافية", "0.00 ج.م (مشمولة)"),
                                LedgerLine("السعر النهائي", "${formatCurrency(uiState.cartTotal)} ج.م", isTotal = true),
                            ),
                        )
                    }
                }
            }
        }
    }
}
