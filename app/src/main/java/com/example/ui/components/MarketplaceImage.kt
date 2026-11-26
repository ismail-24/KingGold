package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.ui.theme.AppColors
import com.example.ui.util.Base64ThumbnailImage
import com.example.ui.util.GoldImageTier
import com.example.ui.util.KingGoldImageLoader

@Composable
fun GoldMarketplaceImage(
    imageUrl: String?,
    base64Thumbnail: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    isThumbnail: Boolean = true,
    imageTier: GoldImageTier = if (isThumbnail) GoldImageTier.THUMBNAIL else GoldImageTier.DETAIL_HIGH_RES,
    @DrawableRes placeholderRes: Int? = null,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {
        if (placeholderRes != null) {
            Image(
                painter = painterResource(id = placeholderRes),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            MarketplaceImageShimmer(modifier = Modifier.fillMaxSize())
        }
    },
) {
    val clippedModifier = modifier.clip(RoundedCornerShape(cornerRadius))
    if (!base64Thumbnail.isNullOrBlank()) {
        Base64ThumbnailImage(
            encoded = base64Thumbnail,
            contentDescription = contentDescription,
            modifier = clippedModifier,
        )
        return
    }

    val normalizedUrl = imageUrl?.trim().orEmpty()
    if (normalizedUrl.isBlank()) {
        Box(modifier = clippedModifier) {
            if (placeholderRes != null) {
                Image(
                    painter = painterResource(id = placeholderRes),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            } else {
                fallback()
            }
        }
        return
    }

    val context = LocalContext.current
    val imageLoader = remember(context) { KingGoldImageLoader.get(context) }
    val request = remember(normalizedUrl, imageTier, context) {
        KingGoldImageLoader.buildImageRequest(
            context = context,
            url = normalizedUrl,
            tier = imageTier,
            crossfadeEnabled = true,
        )
    }

    SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = clippedModifier,
        contentScale = contentScale,
        loading = {
            if (placeholderRes != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = placeholderRes),
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale,
                    )
                    MarketplaceShimmerVeil(modifier = Modifier.fillMaxSize())
                }
            } else {
                MarketplaceImageShimmer(modifier = Modifier.fillMaxSize())
            }
        },
        error = {
            if (placeholderRes != null) {
                Image(
                    painter = painterResource(id = placeholderRes),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
            } else {
                fallback()
            }
        },
        success = {
            SubcomposeAsyncImageContent()
        },
    )
}

@Composable
private fun MarketplaceImageShimmer(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "marketplace_shimmer")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_shift",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            AppColors.ParchmentDeep,
            AppColors.BrassLight.copy(alpha = 0.35f),
            AppColors.ParchmentDeep,
        ),
        start = Offset(shift - 300f, 0f),
        end = Offset(shift, 300f),
    )
    Box(modifier = modifier.background(brush))
}

@Composable
private fun MarketplaceShimmerVeil(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "marketplace_veil")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "veil_shift",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            AppColors.BrassLight.copy(alpha = 0.16f),
            Color.Transparent,
        ),
        start = Offset(shift - 220f, 0f),
        end = Offset(shift, 220f),
    )
    Box(modifier = modifier.background(brush))
}
