package com.example.ui.screens.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoldProductImageLibrary
import com.example.data.KingGoldRepository
import com.example.model.MoneyUtils
import com.example.model.Product
import com.example.model.Shop
import com.example.ui.components.GoldMarketplaceImage
import com.example.ui.components.GoldProductCarouselCard
import com.example.ui.components.LedgerCard
import com.example.ui.components.LedgerLine
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.GoldImageTier
import com.example.ui.util.KingGoldImageLoader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    shop: Shop,
    product: Product,
    relatedProducts: List<Product> = emptyList(),
    gramPrice: Double = KingGoldRepository.gramPrice(product.karat),
    cartItemCount: Int = 0,
    isFavorite: Boolean = false,
    favoriteProductKeys: Set<String> = emptySet(),
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onToggleRelatedFavorite: (Product) -> Unit = {},
    onOpenRelatedProduct: (Product) -> Unit = {},
    onAddToCart: () -> Unit,
    onOpenCart: () -> Unit = {},
    onAskAiAboutProduct: () -> Unit = {},
) {
    val context = LocalContext.current
    val finalPriceEgp = if (product.sellingPricePiasters > 0L) {
        MoneyUtils.piastersToEgp(product.sellingPricePiasters)
    } else {
        gramPrice * product.weightGrams
    }
    val imagery = remember(product) {
        GoldProductImageLibrary.resolveProductImagery(product, GoldImageTier.DETAIL_HIGH_RES)
    }
    var selectedImageIndex by remember(product.id) { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(product.id, relatedProducts) {
        KingGoldImageLoader.prefetch(
            context = context,
            urls = imagery.galleryUrls,
            tier = GoldImageTier.DETAIL_HIGH_RES,
        )
        KingGoldImageLoader.prefetch(
            context = context,
            urls = GoldProductImageLibrary.buildProductPrefetchUrls(relatedProducts, GoldImageTier.CARD_MEDIUM),
            tier = GoldImageTier.CARD_MEDIUM,
        )
    }

    Scaffold(
        containerColor = AppColors.Parchment,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = product.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = AppText.display.copy(fontSize = 19.sp),
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
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("product_detail_favorite_button"),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                            tint = if (isFavorite) AppColors.Alert else AppColors.Ink,
                        )
                    }
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
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                run {
                    val base64List = product.imageThumbnails
                    val remoteList = imagery.galleryUrls
                    val totalImages = if (base64List.isNotEmpty()) base64List.size else remoteList.size
                    val safeIndex = selectedImageIndex.coerceIn(0, (totalImages - 1).coerceAtLeast(0))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .testTag("product_detail_gallery"),
                        ) {
                            GoldMarketplaceImage(
                                imageUrl = if (base64List.isEmpty()) remoteList.getOrNull(safeIndex) else null,
                                base64Thumbnail = base64List.getOrNull(safeIndex),
                                contentDescription = product.name,
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 18.dp,
                                isThumbnail = false,
                                imageTier = GoldImageTier.DETAIL_HIGH_RES,
                                placeholderRes = imagery.placeholderRes,
                                contentScale = ContentScale.Crop,
                            )

                            if (totalImages > 1) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = AppColors.Ink.copy(alpha = 0.82f),
                                ) {
                                    Text(
                                        text = "${safeIndex + 1} / $totalImages",
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        style = AppText.body.copy(
                                            color = AppColors.BrassLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                            }
                        }

                        if (totalImages > 1) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("product_detail_thumbnail_strip"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (base64List.isNotEmpty()) {
                                    itemsIndexed(base64List) { idx, b64 ->
                                        GalleryThumbTile(
                                            imageUrl = null,
                                            base64Thumbnail = b64,
                                            placeholderRes = imagery.placeholderRes,
                                            selected = idx == safeIndex,
                                            contentDescription = "${product.name} ${idx + 1}",
                                            onClick = { selectedImageIndex = idx },
                                        )
                                    }
                                } else {
                                    itemsIndexed(remoteList, key = { idx, url -> "${idx}_$url" }) { idx, url ->
                                        GalleryThumbTile(
                                            imageUrl = url,
                                            base64Thumbnail = null,
                                            placeholderRes = imagery.placeholderRes,
                                            selected = idx == safeIndex,
                                            contentDescription = "${product.name} ${idx + 1}",
                                            onClick = { selectedImageIndex = idx },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InfoPill("عيار ${product.karat}", AppColors.ParchmentDeep, AppColors.Ink)
                    InfoPill("${product.weightGrams} جم", AppColors.ParchmentDeep, AppColors.Ink)
                    InfoPill(product.category, AppColors.ParchmentDeep, AppColors.Seal)
                    if (product.designStyle.isNotBlank()) {
                        InfoPill(product.designStyle, AppColors.ParchmentDeep, AppColors.Ink)
                    }
                    if (product.stamped) {
                        InfoPill("مدموغ ✓", AppColors.TrustSoft, AppColors.Trust)
                    }
                    InfoPill("متاح ${product.availablePieces} قطع", AppColors.ParchmentDeep, AppColors.Ink)
                }

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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "هتستلم إيه من المحل؟",
                        style = AppText.display.copy(fontSize = 16.sp),
                    )
                    CheckLine("القطعة بعد وزنها قدامك على ميزان المحل")
                    CheckLine("دمغة العيار ظاهرة على القطعة")
                    CheckLine("السعر نهائي بدون أي مصنعية إضافية أو رسوم خفية")
                }

                if (relatedProducts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "قطع ذات صلة من ${shop.name}",
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_detail_related_row"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(relatedProducts, key = { it.id }) { related ->
                                val relatedPrice = if (related.sellingPricePiasters > 0L) {
                                    MoneyUtils.piastersToEgp(related.sellingPricePiasters)
                                } else {
                                    KingGoldRepository.gramPrice(related.karat) * related.weightGrams
                                }
                                GoldProductCarouselCard(
                                    product = related,
                                    shopName = shop.name,
                                    finalPriceEgp = relatedPrice,
                                    isFavorite = favoriteProductKeys.contains("${shop.id}_${related.id}"),
                                    onToggleFavorite = { onToggleRelatedFavorite(related) },
                                    onClick = { onOpenRelatedProduct(related) },
                                )
                            }
                        }
                    }
                }

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

@Composable
private fun GalleryThumbTile(
    imageUrl: String?,
    base64Thumbnail: String?,
    placeholderRes: Int,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) AppColors.Brass else AppColors.Line,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
    ) {
        GoldMarketplaceImage(
            imageUrl = imageUrl,
            base64Thumbnail = base64Thumbnail,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 12.dp,
            isThumbnail = true,
            imageTier = GoldImageTier.THUMBNAIL,
            placeholderRes = placeholderRes,
        )
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
