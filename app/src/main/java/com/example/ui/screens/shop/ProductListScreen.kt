package com.example.ui.screens.shop

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MoneyUtils
import com.example.model.Product
import com.example.model.Shop
import com.example.ui.components.ProductGoldArtwork
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.Base64ThumbnailImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    shop: Shop,
    initialCategory: String?,
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onOpenCart: () -> Unit,
) {
    val products = remember(shop.id, uiState.activeMerchantProducts) {
        uiState.productsForShop(shop.id)
    }
    val categories = remember(products) { listOf("الكل") + products.map { it.category }.distinct() }
    var selectedCategory by remember(initialCategory, categories) {
        mutableStateOf(
            if (initialCategory != null && categories.contains(initialCategory)) initialCategory else "الكل",
        )
    }
    val shown = remember(products, selectedCategory) {
        if (selectedCategory == "الكل") products else products.filter { it.category == selectedCategory }
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
                        text = shop.name,
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
                    if (uiState.cartItemCount > 0) {
                        IconButton(
                            onClick = onOpenCart,
                            modifier = Modifier.testTag("product_list_cart_button"),
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = AppColors.Brass) {
                                        Text(
                                            text = uiState.cartItemCount.toString(),
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
            if (uiState.cart.isNotEmpty()) {
                Surface(
                    color = AppColors.Parchment,
                    shadowElevation = 6.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = onOpenCart,
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("view_cart_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Brass,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = "${uiState.cartItemCount} قطعة · اعرض السلة — ${formatCurrency(uiState.cartTotal, decimals = 0)} ج.م",
                                style = AppText.body.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
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
                    .fillMaxSize(),
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    style = AppText.body.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (shown.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "لا توجد قطع ذهب متاحة في هذا القسم حالياً.",
                                    style = AppText.body.copy(color = AppColors.Muted),
                                )
                            }
                        }
                    } else {
                        items(shown, key = { it.id }) { product ->
                            val finalPriceEgp = MoneyUtils.piastersToEgp(uiState.productFinalPricePiasters(product))
                            val shape = RoundedCornerShape(14.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .background(Color.White)
                                    .border(1.dp, AppColors.Line, shape)
                                    .clickable { onOpenProduct(product) }
                                    .padding(12.dp)
                                    .testTag("product_card_${product.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val thumb = product.imageThumbnails.firstOrNull()
                                if (!thumb.isNullOrBlank()) {
                                    Base64ThumbnailImage(
                                        encoded = thumb,
                                        contentDescription = product.name,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                } else {
                                    ProductGoldArtwork(
                                        category = product.category,
                                        karat = product.karat,
                                        modifier = Modifier.size(56.dp),
                                        cornerRadius = 10.dp,
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                                    )
                                    Text(
                                        text = "عيار ${product.karat} · ${product.weightGrams} جم · ${product.availablePieces} متاح",
                                        style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.sp),
                                    )
                                    Text(
                                        text = "السعر شامل (0 مصنعية إضافية)",
                                        style = AppText.body.copy(
                                            color = AppColors.Trust,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${formatCurrency(finalPriceEgp, decimals = 0)} ج.م",
                                        style = AppText.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = {
                                            if (product.availablePieces > 0) {
                                                onAddToCart(product)
                                                scope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "تمت إضافة ${product.name} للسلة",
                                                        actionLabel = "عرض السلة",
                                                        duration = SnackbarDuration.Short,
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        onOpenCart()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = product.availablePieces > 0,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("quick_add_${product.id}"),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = AppColors.Ink,
                                            contentColor = AppColors.BrassLight,
                                        ),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "أضف ${product.name} للسلة",
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
