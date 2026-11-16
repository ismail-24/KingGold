package com.example.ui.screens.shop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KingGoldRepository
import com.example.model.Product
import com.example.model.Shop
import com.example.ui.components.LedgerCard
import com.example.ui.components.LedgerLine
import com.example.ui.components.ProductGoldArtwork
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    shop: Shop,
    product: Product,
    gramPrice: Double = KingGoldRepository.gramPrice(product.karat),
    cartItemCount: Int = 0,
    onBack: () -> Unit,
    onAddToCartAndGo: () -> Unit,
    onOpenCart: () -> Unit = {},
    onAskAiAboutProduct: () -> Unit = {},
) {
    val goldValue = gramPrice * product.weightGrams
    val makingFee = shop.makingFeePerGram * product.weightGrams

    Scaffold(
        containerColor = AppColors.Parchment,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = product.name,
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
                actions = {
                    if (cartItemCount > 0) {
                        IconButton(
                            onClick = onOpenCart,
                            modifier = Modifier.testTag("product_detail_cart_button"),
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = AppColors.Brass) {
                                        Text(
                                            text = cartItemCount.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ShoppingBag,
                                    contentDescription = "عرض السلة",
                                    tint = AppColors.Ink,
                                )
                            }
                        }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onAddToCartAndGo,
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_to_cart_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brass,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "أضف للسلة",
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
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    ProductGoldArtwork(
                        category = product.category,
                        karat = product.karat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        cornerRadius = 18.dp,
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill("عيار ${product.karat}", AppColors.ParchmentDeep, AppColors.Ink)
                        if (product.stamped) {
                            InfoPill("مدموغ ✓", AppColors.TrustSoft, AppColors.Trust)
                        }
                        InfoPill("متاح ${product.availablePieces} قطع", AppColors.ParchmentDeep, AppColors.Ink)
                    }
                }

                item {
                    Column {
                        Text(
                            text = product.name,
                            style = AppText.display.copy(fontSize = 24.sp),
                        )
                        Text(
                            text = "${shop.name} · موثّق · ${shop.rating} ★ ${shop.distanceKm} كم",
                            style = AppText.body.copy(color = AppColors.Muted),
                        )
                    }
                }

                item {
                    Column {
                        Text(
                            text = "السعر التقديري — مرتبط بالسعر اللحظي",
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LedgerCard(
                            lines = listOf(
                                LedgerLine(
                                    label = "قيمة الذهب (${product.weightGrams} جم × ${formatCurrency(gramPrice, 0)})",
                                    value = formatCurrency(goldValue, 0),
                                ),
                                LedgerLine(
                                    label = "مصنعية المحل (${product.weightGrams} جم × ${formatCurrency(shop.makingFeePerGram, 0)})",
                                    value = formatCurrency(makingFee, 0),
                                ),
                                LedgerLine(
                                    label = "الإجمالي قبل رسوم الخدمة",
                                    value = "${formatCurrency(goldValue + makingFee, 0)} ج.م",
                                    isTotal = true,
                                ),
                            ),
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "هتستلم إيه من المحل؟",
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        CheckLine("القطعة بعد وزنها قدامك على ميزان المحل")
                        CheckLine("دمغة العيار ظاهرة على القطعة")
                        CheckLine("فاتورة باسمك بتفاصيل الوزن والعيار والمصنعية")
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onAskAiAboutProduct,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("product_open_ai_advisor_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = AppColors.Brass,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اسأل مستشار Gemini عن هذه القطعة",
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = AppText.body.copy(color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun CheckLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = AppColors.Trust,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = AppText.body.copy(fontSize = 13.sp),
        )
    }
}
