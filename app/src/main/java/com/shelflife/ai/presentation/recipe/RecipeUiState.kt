package com.shelflife.ai.presentation.recipe

data class RecipeUiState(
    val isLoading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val expiringItemsCount: Int = 0,
    val totalPantryItemsCount: Int = 0,
    val isGeminiPowered: Boolean = false,
    val errorMessage: String? = null
)
