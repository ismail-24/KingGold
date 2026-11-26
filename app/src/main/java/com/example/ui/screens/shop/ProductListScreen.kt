package com.example.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.example.data.GoldProductImageLibrary
import com.example.model.MoneyUtils
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.Shop
import com.example.ui.components.GoldProductEditorialCard
import com.example.ui.components.formatCurrency
import com.example.ui.state.KingGoldUiState
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppText
import com.example.ui.util.GoldImageTier
import com.example.ui.util.KingGoldImageLoader
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val PRODUCT_PAGE_SIZE = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    shop: Shop,
    initialCategory: String?,
    uiState: KingGoldUiState,
    onBack: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onToggleFavoriteProduct: (Product) -> Unit = {},
    onAddToCart: (Product) -> Unit,
    onOpenCart: () -> Unit,
) {
    val context = LocalContext.current
    val products = remember(shop.id, uiState.activeMerchantProducts) {
        uiState.productsForShop(shop.id)
    }
    val categories = remember(products) {
        listOf("الكل") + (ProductCategory.allArabicLabels + products.map { it.category }).distinct()
    }
    var selectedCategory by remember(initialCategory, categories) {
        mutableStateOf(
            if (initialCategory != null && categories.contains(initialCategory)) initialCategory else "الكل",
        )
    }
    var selectedKarat by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val shown = remember(products, selectedCategory, selectedKarat, searchQuery) {
        val catEnum = if (selectedCategory == "الكل") {
            null
        } else {
            ProductCategory.entries.firstOrNull { it.arabicLabel == selectedCategory }
        }
        val q = searchQuery.trim().lowercase()
        products.filter { product ->
            val matchesCategory = selectedCategory == "الكل" ||
                catEnum?.matches(product.category) == true ||
                product.category.equals(selectedCategory, ignoreCase = true)
            val matchesKarat = selectedKarat == null || product.karat == selectedKarat
            val matchesSearch = q.isEmpty() ||
                product.name.lowercase().contains(q) ||
                product.category.lowercase().contains(q) ||
                product.designStyle.lowercase().contains(q)
            matchesCategory && matchesKarat && matchesSearch
        }
    }

    var visibleCount by remember(shop.id, selectedCategory, selectedKarat, searchQuery) {
        mutableIntStateOf(PRODUCT_PAGE_SIZE)
    }
    val pagedProducts = remember(shown, visibleCount) {
        shown.take(visibleCount)
    }
    val hasMore = visibleCount < shown.size
    val listState = rememberLazyListState()

    LaunchedEffect(listState, shown.size, visibleCount) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= (pagedProducts.size - 2).coerceAtLeast(0)
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd && visibleCount < shown.size) {
                    visibleCount = (visibleCount + PRODUCT_PAGE_SIZE).coerceAtMost(shown.size)
                }
            }
    }

    // Intelligently prefetch upcoming product images while scrolling
    LaunchedEffect(listState, pagedProducts) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIdx ->
                val upcoming = pagedProducts.drop(firstIdx).take(5)
                val urls = GoldProductImageLibrary.buildProductPrefetchUrls(
                    products = upcoming,
                    tier = GoldImageTier.CARD_MEDIUM,
                )
                KingGoldImageLoader.prefetch(context, urls, GoldImageTier.CARD_MEDIUM)
            }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppColors.Parchment,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = shop.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = AppText.display.copy(fontSize = 18.sp),
                        )
                        Text(
                            text = "${shown.size} قطعة متاحة · عرض ${pagedProducts.size}",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 11.sp),
                        )
                    }
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("product_list_search_input"),
                    placeholder = {
                        Text(
                            text = "ابحث عن سبيكة، جنيه، خاتم، طقم...",
                            style = AppText.body.copy(color = AppColors.Muted, fontSize = 12.5.sp),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "بحث في المنتجات",
                            tint = AppColors.Muted,
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "مسح البحث",
                                    tint = AppColors.Muted,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AppColors.Brass,
                        unfocusedBorderColor = AppColors.Line,
                    ),
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories, key = { it }) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            modifier = Modifier.testTag("product_category_chip_$category"),
                            label = {
                                Text(
                                    text = category,
                                    style = AppText.body.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
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

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val karats = listOf(null to "كل الأعيرة", 24 to "عيار 24", 21 to "عيار 21", 18 to "عيار 18")
                    items(karats, key = { it.second }) { (karat, label) ->
                        FilterChip(
                            selected = selectedKarat == karat,
                            onClick = { selectedKarat = karat },
                            modifier = Modifier.testTag("product_karat_chip_${karat ?: "all"}"),
                            label = {
                                Text(
                                    text = label,
                                    style = AppText.body.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Brass,
                                selectedLabelColor = Color.White,
                                containerColor = AppColors.ParchmentDeep,
                                labelColor = AppColors.Ink,
                            ),
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("product_list_lazy_column"),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (pagedProducts.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, AppColors.Line, RoundedCornerShape(14.dp))
                                    .padding(22.dp)
                                    .testTag("product_list_empty_state"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "لا توجد قطع ذهب مطابقة لهذه الفلاتر.",
                                    style = AppText.body.copy(color = AppColors.Ink, fontWeight = FontWeight.Bold),
                                )
                                OutlinedButton(
                                    onClick = {
                                        selectedCategory = "الكل"
                                        selectedKarat = null
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.testTag("product_list_reset_filters_button"),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text("إعادة ضبط الفلاتر", style = AppText.body.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    } else {
                        items(pagedProducts, key = { it.id }) { product ->
                            val finalPriceEgp = MoneyUtils.piastersToEgp(uiState.productFinalPricePiasters(product))
                            GoldProductEditorialCard(
                                product = product,
                                shopName = shop.name,
                                finalPriceEgp = finalPriceEgp,
                                isFavorite = uiState.isProductFavorite(shop.id, product.id),
                                onToggleFavorite = { onToggleFavoriteProduct(product) },
                                onOpenDetails = { onOpenProduct(product) },
                                onAddToCart = {
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
                            )
                        }

                        if (hasMore) {
                            item(key = "load_more_products") {
                                val remaining = shown.size - pagedProducts.size
                                OutlinedButton(
                                    onClick = {
                                        visibleCount = (visibleCount + PRODUCT_PAGE_SIZE).coerceAtMost(shown.size)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("product_list_load_more_button"),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(
                                        text = "عرض المزيد ($remaining قطعة إضافية)",
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
        }
    }
}
