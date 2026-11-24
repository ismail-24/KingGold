package com.example.ui.screens.shop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KingGoldRepository
import com.example.model.MoneyUtils
import com.example.model.Product
import com.example.model.Shop
import com.example.ui.components.GoldMarketplaceImage
import com.example.ui.components.LedgerCard
import com.example.ui.components.LedgerLine
import com.example.ui.components.ProductGoldArtwork
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    shop: Shop,
    product: Product,
    gramPrice: Double = KingGoldRepository.gramPrice(product.karat),
    cartItemCount: Int = 0,
    onBack: () -> Unit,
    onAddToCart: () -> Unit,
    onOpenCart: () -> Unit = {},
    onAskAiAboutProduct: () -> Unit = {},
) {
    val finalPriceEgp = if (product.sellingPricePiasters > 0L) {
        MoneyUtils.piastersToEgp(product.sellingPricePiasters)
    } else {
        gramPrice * product.weightGrams
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppColors.Parchment,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            onAddToCart()
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val res = snackbarHostState.showSnackbar(
                                    message = "تمت إضافة ${product.name} إلى السلة",
                                    actionLabel = "عرض السلة",
                                    duration = SnackbarDuration.Short,
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    onOpenCart()
                                }
                            }
                        },
                        enabled = product.availablePieces > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("add_to_cart_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brass,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = if (product.availablePieces > 0) "أضف للسلة" else "نفد المخزون",
                            style = AppText.body.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                            ),
                        )
                    }
                    if (cartItemCount > 0) {
                        OutlinedButton(
                            onClick = onOpenCart,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("product_detail_open_cart_cta"),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = "السلة ($cartItemCount)",
                                style = AppText.body.copy(
                                    color = AppColors.Ink,
                                    fontWeight = FontWeight.Bold,
                                ),
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
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    val highResUrls = remember(product) {
                        (product.galleryImageUrls + listOfNotNull(product.imageUrl)).distinct()
                    }
                    when {
                        product.imageThumbnails.isNotEmpty() -> {
                            LazyRow(
                                modifier = Modifier.testTag("product_detail_gallery"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(product.imageThumbnails) { b64 ->
                                    GoldMarketplaceImage(
                                        imageUrl = null,
                                        base64Thumbnail = b64,
                                        contentDescription = product.name,
                                        modifier = Modifier
                                            .width(296.dp)
                                            .height(210.dp),
                                        cornerRadius = 18.dp,
                                        isThumbnail = false,
                                        fallback = {
                                            ProductGoldArtwork(
                                                category = product.category,
                                                karat = product.karat,
                                                modifier = Modifier.fillMaxSize(),
                                                cornerRadius = 18.dp,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        highResUrls.size > 1 -> {
                            LazyRow(
                                modifier = Modifier.testTag("product_detail_gallery"),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(highResUrls, key = { it }) { url ->
                                    GoldMarketplaceImage(
                                        imageUrl = url,
                                        base64Thumbnail = null,
                                        contentDescription = product.name,
                                        modifier = Modifier
                                            .width(296.dp)
                                            .height(210.dp),
                                        cornerRadius = 18.dp,
                                        isThumbnail = false,
                                        fallback = {
                                            ProductGoldArtwork(
                                                category = product.category,
                                                karat = product.karat,
                                                modifier = Modifier.fillMaxSize(),
                                                cornerRadius = 18.dp,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        highResUrls.size == 1 -> {
                            GoldMarketplaceImage(
                                imageUrl = highResUrls.first(),
                                base64Thumbnail = null,
                                contentDescription = product.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .testTag("product_detail_gallery"),
                                cornerRadius = 18.dp,
                                isThumbnail = false,
                                fallback = {
                                    ProductGoldArtwork(
                                        category = product.category,
                                        karat = product.karat,
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 18.dp,
                                    )
                                },
                            )
                        }
                        else -> {
                            ProductGoldArtwork(
                                category = product.category,
                                karat = product.karat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp),
                                cornerRadius = 18.dp,
                            )
                        }
                    }
                }

                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InfoPill("عيار ${product.karat}", AppColors.ParchmentDeep, AppColors.Ink)
                        InfoPill(product.category, AppColors.ParchmentDeep, AppColors.Seal)
                        if (product.designStyle.isNotBlank()) {
                            InfoPill(product.designStyle, AppColors.ParchmentDeep, AppColors.Ink)
                        }
                        if (product.stamped) {
                            InfoPill("مدموغ ✓", AppColors.TrustSoft, AppColors.Trust)
                        }
                        InfoPill("متاح ${product.availablePieces} قطع", AppColors.ParchmentDeep, AppColors.Ink)
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (product.isPhotoVerifiedInventory) AppColors.TrustSoft else AppColors.ParchmentDeep)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("product_photo_disclosure"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = if (product.isPhotoVerifiedInventory) AppColors.Trust else AppColors.Seal,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (product.isPhotoVerifiedInventory) {
                                "صورة مرفوعة وموثّقة من التاجر للقطعة المعروضة."
                            } else {
                                val sourceText = product.imageSourceAttribution ?: "مصدر مرجعي موثّق"
                                "صورة توضيحية للفئة والتصميم ($sourceText) — لا تضمن توفر نفس القطعة حرفياً في جرد المتجر قبل تأكيد المحل."
                            },
                            style = AppText.body.copy(
                                color = if (product.isPhotoVerifiedInventory) AppColors.Trust else AppColors.Seal,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                            ),
                        )
                    }
                }

                item {
                    Column {
                        Text(
                            text = product.name,
                            style = AppText.display.copy(fontSize = 24.sp),
                        )
                        val shopMeta = buildList {
                            add(shop.name)
                            if (shop.verified) add("موثّق")
                            if (shop.hasVerifiedRating) add("${shop.rating} ★")
                            add("${shop.distanceKm} كم")
                        }.joinToString(" · ")
                        Text(
                            text = shopMeta,
                            style = AppText.body.copy(color = AppColors.Muted),
                        )
                        if (product.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = product.description,
                                style = AppText.body.copy(fontSize = 13.5.sp, color = AppColors.Ink.copy(alpha = 0.85f)),
                            )
                        }
                    }
                }

                item {
                    Column {
                        Text(
                            text = "السعر النهائي — بدون مصنعية إضافية",
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LedgerCard(
                            lines = listOf(
                                LedgerLine(
                                    label = "سعر القطعة (${product.weightGrams} جم · عيار ${product.karat})",
                                    value = formatCurrency(finalPriceEgp, 0),
                                ),
                                LedgerLine(
                                    label = "المصنعية الإضافية",
                                    value = "0.00 ج.م (مشمولة)",
                                ),
                                LedgerLine(
                                    label = "الإجمالي النهائي",
                                    value = "${formatCurrency(finalPriceEgp, 0)} ج.م",
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
                        CheckLine("السعر نهائي بدون أي مصنعية إضافية أو رسوم خفية")
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
