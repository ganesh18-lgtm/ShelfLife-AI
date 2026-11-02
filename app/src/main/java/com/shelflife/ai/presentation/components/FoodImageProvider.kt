package com.shelflife.ai.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceSubtle

/**
 * Curated online imagery provider and responsive image loader for food items,
 * store logos, and zero-waste recipes. Uses Coil for high-performance online loading.
 */
object FoodImageProvider {

    // Curated high-resolution online images for food categories
    private val CATEGORY_IMAGES = mapOf(
        Category.PRODUCE to "https://images.unsplash.com/photo-1610832958506-aa56368176cf?w=320&auto=format&fit=crop&q=80",
        Category.DAIRY to "https://images.unsplash.com/photo-1528750997573-59b89d56f4f7?w=320&auto=format&fit=crop&q=80",
        Category.MEAT to "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=320&auto=format&fit=crop&q=80",
        Category.PANTRY to "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=320&auto=format&fit=crop&q=80",
        Category.BAKERY to "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=320&auto=format&fit=crop&q=80",
        Category.BEVERAGES to "https://images.unsplash.com/photo-1544145945-f90425340c7e?w=320&auto=format&fit=crop&q=80",
        Category.FROZEN to "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=320&auto=format&fit=crop&q=80",
        Category.OTHER to "https://images.unsplash.com/photo-1542838132-92c53300491e?w=320&auto=format&fit=crop&q=80"
    )

    // Curated specific item images
    private val SPECIFIC_FOOD_IMAGES = mapOf(
        "milk" to "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=240&auto=format&fit=crop&q=80",
        "egg" to "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=240&auto=format&fit=crop&q=80",
        "bread" to "https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?w=240&auto=format&fit=crop&q=80",
        "banana" to "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=240&auto=format&fit=crop&q=80",
        "tomato" to "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=240&auto=format&fit=crop&q=80",
        "spinach" to "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=240&auto=format&fit=crop&q=80",
        "chicken" to "https://images.unsplash.com/photo-1604503468506-a8da13d82791?w=240&auto=format&fit=crop&q=80",
        "yogurt" to "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=240&auto=format&fit=crop&q=80",
        "cheese" to "https://images.unsplash.com/photo-1618164436241-4473940d1f5c?w=240&auto=format&fit=crop&q=80",
        "rice" to "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=240&auto=format&fit=crop&q=80",
        "oat" to "https://images.unsplash.com/photo-1586444248902-2f64eddc13df?w=240&auto=format&fit=crop&q=80",
        "apple" to "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=240&auto=format&fit=crop&q=80",
        "avocado" to "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?w=240&auto=format&fit=crop&q=80",
        "pasta" to "https://images.unsplash.com/photo-1621996346565-e3d5d6281699?w=240&auto=format&fit=crop&q=80",
        "coffee" to "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=240&auto=format&fit=crop&q=80"
    )

    /**
     * Resolves the best matching online photo URL for a food name and category.
     */
    fun getImageUrl(productName: String, category: Category): String {
        val lower = productName.lowercase()
        for ((key, url) in SPECIFIC_FOOD_IMAGES) {
            if (lower.contains(key)) return url
        }
        return CATEGORY_IMAGES[category] ?: CATEGORY_IMAGES[Category.OTHER]!!
    }

    /**
     * Resolves an ImageVector icon for a category.
     */
    fun getCategoryVector(category: Category): ImageVector {
        return when (category) {
            Category.PRODUCE -> Icons.Default.Eco
            Category.DAIRY -> Icons.Default.LocalDrink
            Category.MEAT -> Icons.Default.Restaurant
            Category.PANTRY -> Icons.Default.Kitchen
            Category.BAKERY -> Icons.Default.BakeryDining
            Category.BEVERAGES -> Icons.Default.LocalCafe
            Category.FROZEN -> Icons.Default.AcUnit
            Category.OTHER -> Icons.Default.Inventory2
        }
    }
}

/**
 * Modernized responsive online image component for food, pantry, and grocery delivery items.
 * Renders an online image with crossfade, placeholder shimmer, and fallback category vector icon.
 */
@Composable
fun OnlineFoodImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    category: Category = Category.OTHER,
    size: Dp = 48.dp,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    borderColor: Color = SurfaceBorder
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(SurfaceSubtle)
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceBorder.copy(alpha = shimmerAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = EmeraldPrimary
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FoodImageProvider.getCategoryVector(category),
                        contentDescription = null,
                        tint = SlateMuted,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }
        )
    }
}

/**
 * Overload resolving image URL and category icon from food name, category, and optional custom image URI.
 */
@Composable
fun OnlineFoodImage(
    productName: String,
    category: Category,
    imageUri: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    borderColor: Color = SurfaceBorder
) {
    val resolvedUrl = if (!imageUri.isNullOrBlank()) imageUri else FoodImageProvider.getImageUrl(productName, category)
    OnlineFoodImage(
        imageUrl = resolvedUrl,
        contentDescription = productName,
        category = category,
        modifier = modifier,
        size = size,
        shape = shape,
        borderColor = borderColor
    )
}
