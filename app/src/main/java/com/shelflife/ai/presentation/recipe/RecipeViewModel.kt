package com.shelflife.ai.presentation.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelflife.ai.data.ai.GeminiManager
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val repository: PantryRepository,
    private val geminiManager: GeminiManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState(isLoading = true))
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    private var currentPantryItems: List<PantryItem> = emptyList()

    init {
        viewModelScope.launch {
            repository.getAllItems().collectLatest { allItems ->
                val inStock = allItems.filter { it.isInStock }
                currentPantryItems = inStock
                generateRecipesForItems(inStock)
            }
        }
    }

    fun refreshRecipes() {
        if (currentPantryItems.isNotEmpty()) {
            viewModelScope.launch {
                generateRecipesForItems(currentPantryItems, forceRefresh = true)
            }
        }
    }

    private suspend fun generateRecipesForItems(items: List<PantryItem>, forceRefresh: Boolean = false) {
        val expiringCount = items.count { it.urgency.isUrgent }

        if (items.isEmpty()) {
            _uiState.value = RecipeUiState(
                isLoading = false,
                recipes = emptyList(),
                expiringItemsCount = 0,
                totalPantryItemsCount = 0,
                isGeminiPowered = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        // Try Gemini 1.5 Flash if API key is provided
        if (geminiManager.hasApiKey()) {
            val geminiResult = geminiManager.generateZeroWasteRecipes(items)
            geminiResult.onSuccess { geminiRecipes ->
                if (geminiRecipes.isNotEmpty()) {
                    _uiState.value = RecipeUiState(
                        isLoading = false,
                        recipes = geminiRecipes,
                        expiringItemsCount = expiringCount,
                        totalPantryItemsCount = items.size,
                        isGeminiPowered = true
                    )
                    return
                }
            }
        }

        // Fallback to local zero-waste recipe generator
        val fallbackRecipes = RecipeGenerator.generateZeroWasteRecipes(items)
        _uiState.value = RecipeUiState(
            isLoading = false,
            recipes = fallbackRecipes,
            expiringItemsCount = expiringCount,
            totalPantryItemsCount = items.size,
            isGeminiPowered = false
        )
    }
}
