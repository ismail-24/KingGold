package com.example.ui.screens.shop

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KingGoldRepository
import com.example.model.KaratPrice
import com.example.model.MoneyUtils
import com.example.model.Product
import com.example.model.Shop
import com.example.ui.components.LivePriceTicker
import com.example.ui.components.ProductGoldArtwork
import com.example.ui.components.ShopGoldMedallion
import com.example.ui.components.VerifiedStrip
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.Base64ThumbnailImage
import com.example.ui.util.dialPhone
import com.example.ui.util.openDirections
import kotlinx.coroutines.launch

@Composable
fun ShopDetailScreen(
    shop: Shop,
    products: List<Product> = KingGoldRepository.productsFor(shop.id),
    prices: List<KaratPrice>,
    isFavorite: Boolean = false,
    cartItemCount: Int = 0,
    onToggleFavorite: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onBack: () -> Unit,
    onBrowseProducts: (initialCategory: String?) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onOpenPriceInsight: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val categories = remember(products) { products.map { it.category }.distinct() }

    Scaffold(
        containerColor = AppColors.Parchment,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        onClick = { onBrowseProducts(null) },
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("browse_shop_gold_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brass,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "تصفّح واختر الذهب",
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
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                    ) {
                        ShopGoldMedallion(
                            shopId = shop.id,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 0.dp,
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(12.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AppColors.Parchment.copy(alpha = 0.9f)),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = AppColors.Ink,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (cartItemCount > 0) {
                                IconButton(
                                    onClick = onOpenCart,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Parchment.copy(alpha = 0.9f))
                                        .testTag("shop_detail_cart_button"),
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
                            IconButton(
                                onClick = {
                                    val nextFav = !isFavorite
                                    onToggleFavorite()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (nextFav) "تمت إضافة ${shop.name} إلى المفضلة"
                                            else "تمت إزالة ${shop.name} من المفضلة",
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Parchment.copy(alpha = 0.9f))
                                    .testTag("shop_detail_favorite_button"),
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                                    tint = if (isFavorite) AppColors.Brass else AppColors.Ink,
                                )
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = shop.name,
                                modifier = Modifier.weight(1f),
                                style = AppText.display.copy(fontSize = 23.sp),
                            )
                            if (shop.verified) VerifiedStrip()
                        }

                        Text(
                            text = "${shop.rating} ★ (${shop.reviews} تقييم) · ${shop.distanceKm} كم من موقعك · مفتوح حتى ${shop.closesAt}",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                        )

                        LivePriceTicker(
                            prices = prices,
                            onTickerClick = onOpenPriceInsight,
                        )

                        // Address, Working Hours, and Contact Phone Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, AppColors.Line),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                // Address Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = "العنوان",
                                        tint = AppColors.Brass,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = shop.address,
                                        modifier = Modifier.weight(1f),
                                        style = AppText.body.copy(fontSize = 13.sp),
                                    )
                                    TextButton(
                                        onClick = {
                                            openDirections(context, shop.address) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("العنوان: ${shop.address}")
                                                }
                                            }
                                        },
                                    ) {
                                        Text(
                                            text = "الاتجاهات",
                                            style = AppText.body.copy(
                                                color = AppColors.Brass,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            ),
                                        )
                                    }
                                }

                                HorizontalDivider(thickness = 0.8.dp, color = AppColors.ParchmentDeep)

                                // Working Hours Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                        .testTag("shop_working_hours"),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "ساعات العمل",
                                        tint = AppColors.Brass,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ساعات العمل",
                                            style = AppText.body.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            ),
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = shop.workingHours,
                                            style = AppText.body.copy(
                                                color = AppColors.Muted,
                                                fontSize = 12.5.sp,
                                                lineHeight = 19.sp,
                                            ),
                                        )
                                    }
                                }

                                HorizontalDivider(thickness = 0.8.dp, color = AppColors.ParchmentDeep)

                                // Contact Phone Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .testTag("shop_contact_phone"),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = "رقم الهاتف",
                                        tint = AppColors.Brass,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "رقم التواصل",
                                            style = AppText.body.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                            ),
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = shop.phone,
                                                style = AppText.body.copy(
                                                    color = AppColors.Ink,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.5.sp,
                                                ),
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            dialPhone(context, shop.phone) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("هاتف المحل: ${shop.phone}")
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("shop_call_button"),
                                    ) {
                                        Text(
                                            text = "اتصل الآن",
                                            style = AppText.body.copy(
                                                color = AppColors.Brass,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        // Gold Collection Gallery
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "معرض تشكيلة الذهب",
                                style = AppText.display.copy(fontSize = 18.sp),
                            )
                            TextButton(onClick = { onBrowseProducts(null) }) {
                                Text(
                                    text = "عرض الكل (${products.size})",
                                    style = AppText.body.copy(
                                        color = AppColors.Brass,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                    ),
                                )
                            }
                        }

                        if (products.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp))
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = "لم يقم التاجر بنشر منتجات متاحة حالياً.",
                                    style = AppText.body.copy(color = AppColors.Muted),
                                )
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shop_collection_gallery"),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(products, key = { it.id }) { product ->
                                    val gramPrice = prices.firstOrNull { it.karat == product.karat }?.pricePerGram
                                        ?: KingGoldRepository.gramPrice(product.karat)
                                    val finalPrice = if (product.sellingPricePiasters > 0L) {
                                        MoneyUtils.piastersToEgp(product.sellingPricePiasters)
                                    } else {
                                        gramPrice * product.weightGrams
                                    }
                                    GoldGalleryCard(
                                        product = product,
                                        finalPrice = finalPrice,
                                        onClick = { onOpenProduct(product) },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الأقسام المتاحة بالمحل",
                            style = AppText.display.copy(fontSize = 17.sp),
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categories.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    pair.forEach { category ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White)
                                                .border(1.dp, AppColors.Line, RoundedCornerShape(12.dp))
                                                .clickable { onBrowseProducts(category) }
                                                .padding(horizontal = 14.dp)
                                                .testTag("category_tile_$category"),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            Text(
                                                text = category,
                                                style = AppText.body.copy(fontWeight = FontWeight.Bold),
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.TrustSoft)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = "الضمان",
                                tint = AppColors.Trust,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بتدفع دلوقتي، لكن المحل مبياخدش المبلغ إلا بعد ما تستلم ذهبك بنفسك.",
                                style = AppText.body.copy(color = AppColors.Trust, fontSize = 12.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoldGalleryCard(
    product: Product,
    finalPrice: Double,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(164.dp)
            .testTag("gallery_item_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, AppColors.Line),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                val thumb = product.imageThumbnails.firstOrNull()
                if (!thumb.isNullOrBlank()) {
                    Base64ThumbnailImage(
                        encoded = thumb,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    ProductGoldArtwork(
                        category = product.category,
                        karat = product.karat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                        cornerRadius = 12.dp,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Ink.copy(alpha = 0.85f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "عيار ${product.karat}",
                        style = AppText.body.copy(
                            color = AppColors.BrassLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppText.body.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                ),
            )
            Text(
                text = "${product.weightGrams} جم · ${if (product.stamped) "مدموغ" else product.category}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.5.sp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${formatCurrency(finalPrice, 0)} ج.م",
                style = AppText.body.copy(
                    color = AppColors.Brass,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                ),
            )
            Text(
                text = "بدون مصنعية إضافية",
                style = AppText.body.copy(
                    color = AppColors.Trust,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                ),
            )
        }
    }
}
