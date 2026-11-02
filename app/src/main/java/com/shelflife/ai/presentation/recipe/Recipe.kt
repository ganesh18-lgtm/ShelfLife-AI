package com.shelflife.ai.presentation.recipe

/**
 * Domain representation of a zero-waste recipe recommended to rescue expiring food items.
 */
data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val rescuedPantryItems: List<String>,
    val additionalIngredients: List<String> = emptyList(),
    val cookingTimeMinutes: Int,
    val difficulty: String = "Easy",
    val instructions: List<String>,
    val imageUrl: String = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=80"
)
