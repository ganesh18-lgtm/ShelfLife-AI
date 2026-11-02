package com.shelflife.ai.presentation.recipe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shelflife.ai.presentation.components.ShelfLifeTopBar
import com.shelflife.ai.presentation.theme.BackgroundCanvas
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.EmeraldSoft
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SlateText
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceSubtle
import com.shelflife.ai.presentation.theme.SurfaceWhite

/**
 * Replicated AI Zero-Waste Chef Screen matching ai_zero_waste_chef/code.html:
 * [Top App Bar] -> [Zero-Waste Impact Banner] -> [Synthesized Recipe Cards with Rescued Ingredients]
 */
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf<String?>(null) } // null = All, "quick", "urgent"

    val displayedRecipes = remember(uiState.recipes, selectedFilter) {
        when (selectedFilter) {
            "quick" -> uiState.recipes.filter { it.cookingTimeMinutes <= 15 }
            "urgent" -> uiState.recipes.filter { it.rescuedPantryItems.isNotEmpty() }
            else -> uiState.recipes
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ShelfLifeTopBar(subtitle = "Ai Chef")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // AI Chef Zero-Waste Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Zero-Waste AI Chef",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = SlateNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (uiState.isGeminiPowered) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldSoft)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Gemini 1.5 Flash",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (uiState.expiringItemsCount > 0) {
                                "${uiState.expiringItemsCount} expiring item(s) prioritized to rescue food"
                            } else {
                                "Creative meal inspirations using in-stock pantry items"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SlateMuted
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshRecipes() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Regenerate",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Pills Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPillTag(
                    label = "All (${uiState.recipes.size})",
                    isSelected = selectedFilter == null,
                    onClick = { selectedFilter = null }
                )
                FilterPillTag(
                    label = "Quick (<15m)",
                    isSelected = selectedFilter == "quick",
                    onClick = { selectedFilter = if (selectedFilter == "quick") null else "quick" }
                )
                FilterPillTag(
                    label = "High Priority",
                    isSelected = selectedFilter == "urgent",
                    onClick = { selectedFilter = if (selectedFilter == "urgent") null else "urgent" }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 3.dp)
                    }
                }
                displayedRecipes.isEmpty() -> {
                    EmptyRecipeState(onScanClicked = onNavigateToScanner)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 96.dp)
                    ) {
                        items(items = displayedRecipes, key = { it.id }) { recipe ->
                            ModelRecipeCard(recipe = recipe)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelRecipeCard(recipe: Recipe) {
    var isExpanded by remember { mutableStateOf(false) }
    var isCookingMode by remember { mutableStateOf(false) }
    var completedSteps by remember { mutableStateOf(setOf<Int>()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Recipe Image Hero if available
            if (!recipe.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSubtle)
                ) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 100% Waste Match Pill Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(EmeraldPrimary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "100% Waste Match",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Title & Expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = SlateNavy,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = SlateMuted
                    )
                }
            }

            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                color = SlateText,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badges: Time & Difficulty
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceSubtle)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = SlateMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${recipe.cookingTimeMinutes} mins",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SlateNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldSoft)
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = recipe.difficulty,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rescued Pantry Items Flow
            Text(
                text = "Rescued Ingredients:",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = EmeraldPrimary
            )

            Spacer(modifier = Modifier.height(5.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recipe.rescuedPantryItems.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldSoft)
                            .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            fontWeight = FontWeight.Medium,
                            color = EmeraldDark
                        )
                    }
                }
            }

            // Expandable Cooking Steps
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (recipe.additionalIngredients.isNotEmpty()) {
                        Text(
                            text = "Pantry Staples Needed:",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                            color = SlateNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        recipe.additionalIngredients.forEach { staple ->
                            Text(
                                text = "• $staple",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = SlateText
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Cooking Mode Progress Header
                    if (isCookingMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldSoft)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE COOKING MODE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = EmeraldDark
                            )
                            Text(
                                text = "${completedSteps.size} of ${recipe.instructions.size} done",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Celebration banner if all steps done
                    if (isCookingMode && completedSteps.size == recipe.instructions.size && recipe.instructions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDCFCE7))
                                .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "🎉 Recipe Completed! 100% of pantry ingredients rescued!",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                                    color = EmeraldDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = if (isCookingMode) "Tap steps to check off:" else "Cooking Steps:",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                        color = SlateNavy
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    recipe.instructions.forEachIndexed { index, step ->
                        val isDone = completedSteps.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isCookingMode) {
                                        completedSteps = if (isDone) completedSteps - index else completedSteps + index
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isDone) EmeraldPrimary else SurfaceSubtle)
                                    .border(1.dp, if (isDone) EmeraldPrimary else SurfaceBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = SlateNavy,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = if (isDone) SlateMuted else SlateText
                                ),
                                modifier = Modifier.weight(1f),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            isCookingMode = !isCookingMode
                            if (isCookingMode) isExpanded = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCookingMode) EmeraldDark else EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isCookingMode) "Exit Cooking Mode" else "Start Cooking Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPillTag(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(if (isSelected) EmeraldPrimary else SurfaceWhite)
            .border(
                width = 1.dp,
                color = if (isSelected) EmeraldPrimary else SurfaceBorder,
                shape = RoundedCornerShape(9999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) Color.White else SlateNavy
        )
    }
}

@Composable
private fun EmptyRecipeState(onScanClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(EmeraldSoft)
                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = EmeraldPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No ingredients to cook with yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = SlateNavy,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add items to your pantry, and our zero-waste AI will generate custom recipes that save your expiring food!",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = SlateMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onScanClicked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Scan Items to Get Recipes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
