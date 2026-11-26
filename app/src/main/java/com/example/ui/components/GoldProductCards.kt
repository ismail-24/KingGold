package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoldProductImageLibrary
import com.example.model.Product
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.GoldImageTier

/**
 * Full-width editorial gold product card used in ProductListScreen and search results.
 * Keeps large imagery as the primary visual focal point while clearly presenting karat,
 * weight, merchant name, final price, favorite toggle, details action, and quick cart CTA.
 */
@Composable
fun GoldProductEditorialCard(
    product: Product,
    shopName: String,
    finalPriceEgp: Double,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imagery = remember(product.id, product.thumbnailUrl, product.cardImageUrl, product.imageUrl, product.category, product.karat) {
        GoldProductImageLibrary.resolveProductImagery(product, GoldImageTier.CARD_MEDIUM)
    }

    Card(
        onClick = onOpenDetails,
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFavorite) AppColors.Brass.copy(alpha = 0.65f) else AppColors.Line,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
            ) {
                GoldMarketplaceImage(
                    imageUrl = imagery.primaryUrl,
                    base64Thumbnail = product.imageThumbnails.firstOrNull(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp,
                    isThumbnail = false,
                    imageTier = GoldImageTier.CARD_MEDIUM,
                    placeholderRes = imagery.placeholderRes,
                    contentScale = ContentScale.Crop,
                )

                // Subtle bottom readability gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, AppColors.Ink.copy(alpha = 0.62f)),
                            ),
                        ),
                )

                // Top-start Karat + Hallmark chips
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Ink.copy(alpha = 0.88f),
                    ) {
                        Text(
                            text = "عيار ${product.karat}",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.92f),
                    ) {
                        Text(
                            text = "${product.weightGrams} جم",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                // Top-end Favorite toggle (48dp touch target)
                Surface(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                        .testTag("favorite_product_${product.id}"),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 2.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "إزالة ${product.name} من المفضلة" else "إضافة ${product.name} للمفضلة",
                            tint = if (isFavorite) AppColors.Alert else AppColors.Ink,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }

                // Bottom-start Photo authenticity badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (product.isPhotoVerifiedInventory) {
                        AppColors.Trust.copy(alpha = 0.92f)
                    } else {
                        AppColors.Ink.copy(alpha = 0.78f)
                    },
                ) {
                    Text(
                        text = if (product.isPhotoVerifiedInventory) {
                            "صورة موثّقة من التاجر"
                        } else {
                            "صورة توضيحية للفئة"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = AppText.body.copy(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = AppText.display.copy(fontSize = 16.sp),
                        )
                        Text(
                            text = "${product.category}${if (product.designStyle.isNotBlank()) " · ${product.designStyle}" else ""}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = AppText.body.copy(
                                color = AppColors.Seal,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${formatCurrency(finalPriceEgp, decimals = 0)} ج.م",
                            style = AppText.display.copy(
                                color = AppColors.Brass,
                                fontSize = 16.sp,
                            ),
                        )
                        Text(
                            text = "بدون مصنعية إضافية",
                            style = AppText.body.copy(
                                color = AppColors.Trust,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = AppColors.Muted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = shopName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = AppText.body.copy(
                                color = AppColors.Muted,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${product.availablePieces} متاح",
                        style = AppText.body.copy(
                            color = AppColors.Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onOpenDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("product_details_button_${product.id}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppColors.Brass.copy(alpha = 0.55f)),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = AppColors.Ink,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "عرض التفاصيل",
                            maxLines = 1,
                            style = AppText.body.copy(
                                color = AppColors.Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            ),
                        )
                    }

                    Button(
                        onClick = onAddToCart,
                        enabled = product.availablePieces > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_add_${product.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Ink,
                            contentColor = AppColors.BrassLight,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "أضف ${product.name} للسلة",
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (product.availablePieces > 0) "أضف للسلة" else "نفد المخزون",
                            maxLines = 1,
                            style = AppText.body.copy(
                                color = AppColors.BrassLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact horizontal-carousel gold product card used in DiscoverScreen featured showcase,
 * ShopDetailScreen collection gallery, and ProductDetailScreen related items.
 */
@Composable
fun GoldProductCarouselCard(
    product: Product,
    shopName: String,
    finalPriceEgp: Double,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imagery = remember(product.id, product.thumbnailUrl, product.cardImageUrl, product.imageUrl, product.category, product.karat) {
        GoldProductImageLibrary.resolveProductImagery(product, GoldImageTier.CARD_MEDIUM)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(196.dp)
            .testTag("gallery_item_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFavorite) AppColors.Brass.copy(alpha = 0.6f) else AppColors.Line,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
            ) {
                GoldMarketplaceImage(
                    imageUrl = imagery.primaryUrl,
                    base64Thumbnail = product.imageThumbnails.firstOrNull(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0.dp,
                    isThumbnail = true,
                    imageTier = GoldImageTier.CARD_MEDIUM,
                    placeholderRes = imagery.placeholderRes,
                    contentScale = ContentScale.Crop,
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Ink.copy(alpha = 0.86f),
                ) {
                    Text(
                        text = "عيار ${product.karat}",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = AppText.body.copy(
                            color = AppColors.BrassLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Surface(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(48.dp)
                        .testTag("carousel_favorite_${product.shopId}_${product.id}"),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                            tint = if (isFavorite) AppColors.Alert else AppColors.Ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
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
                    text = "${product.weightGrams} جم · ${product.designStyle.ifBlank { product.category }}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp),
                )
                Text(
                    text = shopName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppText.body.copy(
                        color = AppColors.Seal,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${formatCurrency(finalPriceEgp, 0)} ج.م",
                        style = AppText.body.copy(
                            color = AppColors.Brass,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                        ),
                    )
                    Text(
                        text = "التفاصيل",
                        style = AppText.body.copy(
                            color = AppColors.Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                    )
                }
                Text(
                    text = if (product.isPhotoVerifiedInventory) "صورة موثّقة من التاجر" else "صورة مرجعية للتصميم",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppText.body.copy(
                        color = if (product.isPhotoVerifiedInventory) AppColors.Trust else AppColors.Seal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                    ),
                )
            }
        }
    }
}
