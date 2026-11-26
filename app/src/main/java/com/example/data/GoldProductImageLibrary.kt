package com.example.data

import androidx.annotation.DrawableRes
import com.example.R
import com.example.model.Product
import com.example.model.ProductCategory
import com.example.model.Shop
import com.example.ui.util.GoldImageTier
import com.example.ui.util.KingGoldImageLoader

/**
 * Granular visual subcategories covering Egyptian gold jewelry, investment bullion,
 * and jewelry store showcase environments.
 */
enum class GoldVisualSubcategory(
    val arabicLabel: String,
    @param:DrawableRes val placeholderDrawableRes: Int,
) {
    BRACELETS_BANGLES("أساور وغوايش ذهبية", R.drawable.img_gold_bracelets_bangles),
    CHAINS_NECKLACES("سلاسل وقلائد ذهبية", R.drawable.img_gold_rings_pendants),
    RINGS("خواتم ودبل ذهبية", R.drawable.img_gold_rings_pendants),
    EARRINGS("أقراط وحلقان ذهبية", R.drawable.img_gold_rings_pendants),
    PENDANTS("دلايات وتعليقات ذهبية", R.drawable.img_gold_rings_pendants),
    NECKLACE_WITH_PENDANT("عقد بدلاية ذهبية", R.drawable.img_gold_bridal_necklace_set),
    COMPLETE_JEWELRY_SETS("أطقم ذهب كاملة", R.drawable.img_gold_bridal_necklace_set),
    WEDDING_SETS("أطقم شبكة وزفاف", R.drawable.img_gold_bridal_necklace_set),
    CHILDRENS_GOLD("مجوهرات أطفال ذهبية", R.drawable.img_gold_rings_pendants),
    EGYPTIAN_DESIGNS("تصاميم صياغة مصرية", R.drawable.img_gold_bracelets_bangles),
    BULLION_BARS("سبائك ذهب استثمارية", R.drawable.img_gold_bullion_collection),
    SMALL_INGOTS("سبائك ذهب صغيرة مغلفة", R.drawable.img_gold_bullion_collection),
    GOLD_COINS("عملات ذهبية", R.drawable.img_gold_bullion_collection),
    EGYPTIAN_GOLD_POUNDS("جنيهات ذهب مدموغة", R.drawable.img_gold_bullion_collection),
    SHOP_SHOWCASE("معرض صاغة ومجوهرات", R.drawable.img_gold_showcase_hero),
}

data class ProductImagerySpec(
    val subcategory: GoldVisualSubcategory,
    val primaryUrl: String?,
    val galleryUrls: List<String>,
    @param:DrawableRes val placeholderRes: Int,
    val isVerifiedMerchantPhoto: Boolean,
    val attributionLabel: String,
)

data class ShopVisualProfile(
    val shopId: String,
    val specialtyBadge: String,
    val coverWikimediaFile: String,
    val logoWikimediaFile: String,
    @param:DrawableRes val coverPlaceholderRes: Int,
    @param:DrawableRes val logoPlaceholderRes: Int,
    val preferredCategoryOrder: List<ProductCategory>,
    val showcaseLabel: String = "صورة توضيحية لمعرض الذهب",
) {
    fun coverUrl(tier: GoldImageTier = GoldImageTier.CARD_MEDIUM): String =
        KingGoldImageLoader.wikimediaUrlForTier(coverWikimediaFile, tier)

    fun logoUrl(tier: GoldImageTier = GoldImageTier.THUMBNAIL): String =
        KingGoldImageLoader.wikimediaUrlForTier(logoWikimediaFile, tier)
}

/**
 * Data-layer imagery component responsible for:
 * 1. Categorizing gold products and shops into realistic jewelry/bullion visual families.
 * 2. Supplying immediate bundled photorealistic placeholders (@DrawableRes) while high-res images load asynchronously.
 * 3. Selecting resolution-appropriate image URLs (360px thumbnail, 600px card, 960px detail) for Coil caching.
 * 4. Providing distinct visual identities, covers, logos, and category ordering for every gold shop.
 */
object GoldProductImageLibrary {

    private val shopVisualProfiles: Map<String, ShopVisualProfile> = mapOf(
        "s1" to ShopVisualProfile(
            shopId = "s1",
            specialtyBadge = "سبائك عيار 24 وعملات استثمارية",
            coverWikimediaFile = "Gold_bullion_bars.jpg",
            logoWikimediaFile = "1963_Gold_Sovereign_%E2%80%93_St._George_Reverse.png",
            coverPlaceholderRes = R.drawable.img_gold_bullion_collection,
            logoPlaceholderRes = R.drawable.img_gold_bullion_collection,
            preferredCategoryOrder = listOf(
                ProductCategory.COINS,
                ProductCategory.BARS_BULLION,
                ProductCategory.BRACELETS,
                ProductCategory.RINGS,
                ProductCategory.CHAINS,
                ProductCategory.EARRINGS,
                ProductCategory.JEWELRY_SETS,
                ProductCategory.DESIGNS,
            ),
        ),
        "s2" to ShopVisualProfile(
            shopId = "s2",
            specialtyBadge = "مجوهرات عصرية وأطقم وخواتم",
            coverWikimediaFile = "Gold_Necklace_with_Ornaments_MET_DP369467.jpg",
            logoWikimediaFile = "Diamond_and_gold_ring.JPG",
            coverPlaceholderRes = R.drawable.img_gold_bridal_necklace_set,
            logoPlaceholderRes = R.drawable.img_gold_rings_pendants,
            preferredCategoryOrder = listOf(
                ProductCategory.JEWELRY_SETS,
                ProductCategory.RINGS,
                ProductCategory.CHAINS,
                ProductCategory.DESIGNS,
                ProductCategory.EARRINGS,
                ProductCategory.BRACELETS,
                ProductCategory.COINS,
                ProductCategory.BARS_BULLION,
            ),
        ),
        "s3" to ShopVisualProfile(
            shopId = "s3",
            specialtyBadge = "غوايش مصرية وأطقم شبكة",
            coverWikimediaFile = "Gold_jewellery_at_Grand_Bazaar%2C_Istanbul.jpg",
            logoWikimediaFile = "Pair_of_Bangles.JPG",
            coverPlaceholderRes = R.drawable.img_gold_showcase_hero,
            logoPlaceholderRes = R.drawable.img_gold_bracelets_bangles,
            preferredCategoryOrder = listOf(
                ProductCategory.BRACELETS,
                ProductCategory.JEWELRY_SETS,
                ProductCategory.COINS,
                ProductCategory.CHAINS,
                ProductCategory.EARRINGS,
                ProductCategory.RINGS,
                ProductCategory.DESIGNS,
                ProductCategory.BARS_BULLION,
            ),
        ),
        "s4" to ShopVisualProfile(
            shopId = "s4",
            specialtyBadge = "صياغة مصرية وجنيهات وسبائك",
            coverWikimediaFile = "Poland_Gold_filigree_chain.jpg",
            logoWikimediaFile = "Pendant.jpg",
            coverPlaceholderRes = R.drawable.img_gold_bracelets_bangles,
            logoPlaceholderRes = R.drawable.img_gold_rings_pendants,
            preferredCategoryOrder = listOf(
                ProductCategory.DESIGNS,
                ProductCategory.COINS,
                ProductCategory.BRACELETS,
                ProductCategory.BARS_BULLION,
                ProductCategory.JEWELRY_SETS,
                ProductCategory.CHAINS,
                ProductCategory.RINGS,
                ProductCategory.EARRINGS,
            ),
        ),
        "s5" to ShopVisualProfile(
            shopId = "s5",
            specialtyBadge = "سبائك مغلفة وجنيهات مدموغة",
            coverWikimediaFile = "250g_Goldbarren.JPG",
            logoWikimediaFile = "1963_Gold_Sovereign_%E2%80%93_Elizabeth_II_Obverse.png",
            coverPlaceholderRes = R.drawable.img_gold_bullion_collection,
            logoPlaceholderRes = R.drawable.img_gold_bullion_collection,
            preferredCategoryOrder = listOf(
                ProductCategory.BARS_BULLION,
                ProductCategory.COINS,
                ProductCategory.JEWELRY_SETS,
                ProductCategory.BRACELETS,
                ProductCategory.RINGS,
                ProductCategory.CHAINS,
                ProductCategory.EARRINGS,
                ProductCategory.DESIGNS,
            ),
        ),
        "s6" to ShopVisualProfile(
            shopId = "s6",
            specialtyBadge = "خواتم سوليتير وأطقم زفاف",
            coverWikimediaFile = "Two-gold_wedding_rings.jpg",
            logoWikimediaFile = "White_gold_earrings.jpg",
            coverPlaceholderRes = R.drawable.img_gold_rings_pendants,
            logoPlaceholderRes = R.drawable.img_gold_rings_pendants,
            preferredCategoryOrder = listOf(
                ProductCategory.RINGS,
                ProductCategory.EARRINGS,
                ProductCategory.JEWELRY_SETS,
                ProductCategory.CHAINS,
                ProductCategory.DESIGNS,
                ProductCategory.BRACELETS,
                ProductCategory.COINS,
                ProductCategory.BARS_BULLION,
            ),
        ),
        "s7" to ShopVisualProfile(
            shopId = "s7",
            specialtyBadge = "سلاسل وأساور وتعليقات حديثة",
            coverWikimediaFile = "Venetian_Gold_Chain.jpg",
            logoWikimediaFile = "Gold_bracelet.jpg",
            coverPlaceholderRes = R.drawable.img_gold_rings_pendants,
            logoPlaceholderRes = R.drawable.img_gold_bracelets_bangles,
            preferredCategoryOrder = listOf(
                ProductCategory.CHAINS,
                ProductCategory.BRACELETS,
                ProductCategory.EARRINGS,
                ProductCategory.DESIGNS,
                ProductCategory.RINGS,
                ProductCategory.JEWELRY_SETS,
                ProductCategory.COINS,
                ProductCategory.BARS_BULLION,
            ),
        ),
        "s8" to ShopVisualProfile(
            shopId = "s8",
            specialtyBadge = "منفذ سبائك وهدايا ذهبية معتمدة",
            coverWikimediaFile = "Gold_Ingots_on_white_background.jpg",
            logoWikimediaFile = "Pendant_with_opal_and_gold.jpg",
            coverPlaceholderRes = R.drawable.img_gold_showcase_hero,
            logoPlaceholderRes = R.drawable.img_gold_bullion_collection,
            preferredCategoryOrder = listOf(
                ProductCategory.BARS_BULLION,
                ProductCategory.COINS,
                ProductCategory.DESIGNS,
                ProductCategory.JEWELRY_SETS,
                ProductCategory.RINGS,
                ProductCategory.BRACELETS,
                ProductCategory.CHAINS,
                ProductCategory.EARRINGS,
            ),
        ),
    )

    private val dynamicShopCovers = listOf(
        "Gold_jewellery_at_Grand_Bazaar%2C_Istanbul.jpg" to R.drawable.img_gold_showcase_hero,
        "Gold_Necklace_with_Ornaments_MET_DP369467.jpg" to R.drawable.img_gold_bridal_necklace_set,
        "Gold_bullion_bars.jpg" to R.drawable.img_gold_bullion_collection,
        "Pair_of_Bangles.JPG" to R.drawable.img_gold_bracelets_bangles,
        "Diamond_and_gold_ring.JPG" to R.drawable.img_gold_rings_pendants,
    )

    fun classifyProduct(product: Product): GoldVisualSubcategory =
        classifyRaw(
            rawCategory = product.category,
            productName = product.name,
            designStyle = product.designStyle,
            karat = product.karat,
            weightGrams = product.weightGrams,
        )

    fun classifyRaw(
        rawCategory: String,
        productName: String = "",
        designStyle: String = "",
        karat: Int = 21,
        weightGrams: Double = 0.0,
    ): GoldVisualSubcategory {
        val text = "$rawCategory $productName $designStyle".lowercase()
        val itemText = "$productName $designStyle".lowercase()
        val hasPendantCue = text.contains("دلاية") || text.contains("تعليقة")
        val hasChainCue = text.contains("سلسلة") || text.contains("سلاسل") || text.contains("عقد") || text.contains("قلائد")
        val isEgyptianGoldPound = itemText.contains("جنيه") ||
            (text.contains("جنيه") && !itemText.contains("عملة") && !itemText.contains("سوفيرين"))
        return when {
            text.contains("أطفال") || text.contains("طفل") -> GoldVisualSubcategory.CHILDRENS_GOLD
            text.contains("شبكة") || text.contains("زفاف") -> GoldVisualSubcategory.WEDDING_SETS
            text.contains("طقم") || text.contains("أطقم") -> GoldVisualSubcategory.COMPLETE_JEWELRY_SETS
            isEgyptianGoldPound -> GoldVisualSubcategory.EGYPTIAN_GOLD_POUNDS
            text.contains("عملات") || text.contains("عملة") -> GoldVisualSubcategory.GOLD_COINS
            text.contains("سبيكة") || text.contains("سبائك") -> {
                if (weightGrams in 0.1..5.0 || text.contains("صغير")) {
                    GoldVisualSubcategory.SMALL_INGOTS
                } else {
                    GoldVisualSubcategory.BULLION_BARS
                }
            }
            text.contains("غوايش") || text.contains("أسورة") || text.contains("أساور") -> GoldVisualSubcategory.BRACELETS_BANGLES
            text.contains("خاتم") || text.contains("خواتم") || text.contains("دبل") -> GoldVisualSubcategory.RINGS
            text.contains("حلق") || text.contains("أقراط") -> GoldVisualSubcategory.EARRINGS
            hasPendantCue && hasChainCue -> GoldVisualSubcategory.NECKLACE_WITH_PENDANT
            hasPendantCue -> GoldVisualSubcategory.PENDANTS
            text.contains("تراثي") || text.contains("مصري") || text.contains("فليغري") -> GoldVisualSubcategory.EGYPTIAN_DESIGNS
            text.contains("تعليقات") -> GoldVisualSubcategory.PENDANTS
            hasChainCue -> GoldVisualSubcategory.CHAINS_NECKLACES
            karat == 24 -> GoldVisualSubcategory.BULLION_BARS
            else -> GoldVisualSubcategory.SHOP_SHOWCASE
        }
    }

    @DrawableRes
    fun placeholderDrawableForProduct(product: Product): Int =
        classifyProduct(product).placeholderDrawableRes

    @DrawableRes
    fun placeholderDrawableForCategory(rawCategory: String, karat: Int = 21): Int =
        classifyRaw(rawCategory = rawCategory, karat = karat).placeholderDrawableRes

    fun resolveShopVisual(shop: Shop): ShopVisualProfile {
        shopVisualProfiles[shop.id]?.let { return it }
        val idx = (shop.id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % dynamicShopCovers.size
        val (coverFile, placeholderRes) = dynamicShopCovers[idx]
        return ShopVisualProfile(
            shopId = shop.id,
            specialtyBadge = shop.visualSpecialty.ifBlank { "معرض ذهب معتمد" },
            coverWikimediaFile = coverFile,
            logoWikimediaFile = coverFile,
            coverPlaceholderRes = placeholderRes,
            logoPlaceholderRes = placeholderRes,
            preferredCategoryOrder = ProductCategory.entries,
        )
    }

    @DrawableRes
    fun placeholderDrawableForShopCover(shop: Shop): Int =
        resolveShopVisual(shop).coverPlaceholderRes

    @DrawableRes
    fun placeholderDrawableForShopLogo(shop: Shop): Int =
        resolveShopVisual(shop).logoPlaceholderRes

    fun shopCoverUrl(shop: Shop, tier: GoldImageTier = GoldImageTier.CARD_MEDIUM): String =
        shop.coverImageUrl?.takeIf { it.isNotBlank() }?.let { rewriteWikimediaWidth(it, tier) }
            ?: resolveShopVisual(shop).coverUrl(tier)

    fun shopLogoUrl(shop: Shop, tier: GoldImageTier = GoldImageTier.THUMBNAIL): String =
        shop.logoUrl?.takeIf { it.isNotBlank() }?.let { rewriteWikimediaWidth(it, tier) }
            ?: resolveShopVisual(shop).logoUrl(tier)

    fun resolveProductImagery(
        product: Product,
        tier: GoldImageTier,
    ): ProductImagerySpec {
        val subcategory = classifyProduct(product)
        val rawUrl = when (tier) {
            GoldImageTier.THUMBNAIL -> product.thumbnailUrl ?: product.cardImageUrl ?: product.imageUrl
            GoldImageTier.CARD_MEDIUM -> product.cardImageUrl ?: product.imageUrl ?: product.thumbnailUrl
            GoldImageTier.DETAIL_HIGH_RES -> product.imageUrl ?: product.cardImageUrl ?: product.thumbnailUrl
        }
        val sizedPrimary = rawUrl?.let { rewriteWikimediaWidth(it, tier) }
        val gallery = (listOfNotNull(sizedPrimary) + product.galleryImageUrls.map { rewriteWikimediaWidth(it, tier) })
            .distinct()
        return ProductImagerySpec(
            subcategory = subcategory,
            primaryUrl = sizedPrimary,
            galleryUrls = gallery,
            placeholderRes = subcategory.placeholderDrawableRes,
            isVerifiedMerchantPhoto = product.isPhotoVerifiedInventory,
            attributionLabel = if (product.isPhotoVerifiedInventory) {
                "صورة موثّقة من التاجر"
            } else {
                product.imageSourceAttribution ?: "صورة توضيحية للفئة والتصميم"
            },
        )
    }

    fun orderProductsForShop(shopId: String, products: List<Product>): List<Product> {
        if (shopId == "s1") return products
        val profile = shopVisualProfiles[shopId] ?: return products
        val priorityIndex = profile.preferredCategoryOrder
            .mapIndexed { index, category -> category to index }
            .toMap()
        return products.sortedBy { product ->
            val matchedCat = ProductCategory.entries.firstOrNull { it.matches(product.category) }
            priorityIndex[matchedCat] ?: Int.MAX_VALUE
        }
    }

    fun categoryGallerySupplementalFiles(category: ProductCategory): List<String> = when (category) {
        ProductCategory.COINS -> listOf(
            "1963_Gold_Sovereign_%E2%80%93_St._George_Reverse.png",
            "1963_Gold_Sovereign_%E2%80%93_Elizabeth_II_Obverse.png",
            "Gold_Ingots_on_white_background.jpg",
        )
        ProductCategory.BARS_BULLION -> listOf(
            "Gold_bullion_bars.jpg",
            "Gold_Ingots_on_white_background.jpg",
            "250g_Goldbarren.JPG",
        )
        ProductCategory.BRACELETS -> listOf(
            "Gold_bracelet.jpg",
            "Pair_of_Bangles.JPG",
        )
        ProductCategory.RINGS -> listOf(
            "Wedding_gold_ring.jpg",
            "Two-gold_wedding_rings.jpg",
            "Diamond_and_gold_ring.JPG",
        )
        ProductCategory.CHAINS -> listOf(
            "Venetian_Gold_Chain.jpg",
            "Poland_Gold_filigree_chain.jpg",
            "Gold_Necklace_with_Ornaments_MET_DP369467.jpg",
        )
        ProductCategory.EARRINGS -> listOf(
            "Gold_plated_earrings.jpg",
            "White_gold_earrings.jpg",
        )
        ProductCategory.JEWELRY_SETS -> listOf(
            "Gold_jewellery_at_Grand_Bazaar%2C_Istanbul.jpg",
            "Gold_Necklace_with_Ornaments_MET_DP369467.jpg",
        )
        ProductCategory.DESIGNS -> listOf(
            "Pendant_with_opal_and_gold.jpg",
            "Pendant.jpg",
            "Poland_Gold_filigree_chain.jpg",
        )
    }

    fun buildShopBannerPrefetchUrls(
        shops: List<Shop>,
        tier: GoldImageTier = GoldImageTier.CARD_MEDIUM,
    ): List<String> = shops.flatMap { shop ->
        listOf(shopCoverUrl(shop, tier), shopLogoUrl(shop, GoldImageTier.THUMBNAIL))
    }.distinct()

    fun buildProductPrefetchUrls(
        products: List<Product>,
        tier: GoldImageTier = GoldImageTier.CARD_MEDIUM,
    ): List<String> = products.mapNotNull { product ->
        resolveProductImagery(product, tier).primaryUrl
    }.distinct()

    private fun rewriteWikimediaWidth(url: String, tier: GoldImageTier): String {
        if (!url.contains("commons.wikimedia.org") || !url.contains("width=")) return url
        return url.replace(Regex("width=\\d+"), "width=${tier.widthPx}")
    }
}
