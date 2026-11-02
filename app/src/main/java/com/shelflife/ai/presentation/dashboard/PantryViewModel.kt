package com.shelflife.ai.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelflife.ai.di.IoDispatcher
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Lead ViewModel governing pantry inventory lifecycle with explicit coroutine dispatching
 * and reactive UDF StateFlow observation.
 */
@HiltViewModel
class PantryViewModel @Inject constructor(
    private val repository: PantryRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _selectedUrgency = MutableStateFlow<ExpiryUrgency?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _itemJustAdded = MutableStateFlow<Long?>(null)
    val itemJustAdded: StateFlow<Long?> = _itemJustAdded

    val uiState: StateFlow<PantryUiState> = combine(
        repository.getAllItems(),
        _selectedCategory,
        _selectedUrgency,
        _searchQuery,
        _userMessage
    ) { allItems, category, urgency, query, message ->
        val filtered = allItems.filter { item ->
            val matchesCategory = category == null || item.category == category
            val matchesUrgency = urgency == null || item.urgency == urgency
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    (item.barcode?.contains(query, ignoreCase = true) == true)
            matchesCategory && matchesUrgency && matchesQuery
        }

        PantryUiState(
            items = filtered,
            isLoading = false,
            selectedCategory = category,
            selectedUrgency = urgency,
            searchQuery = query,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PantryUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: Category?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setUrgencyFilter(urgency: ExpiryUrgency?) {
        _selectedUrgency.value = if (_selectedUrgency.value == urgency) null else urgency
    }

    /**
     * Persists a new item into the local Room database using explicit Dispatchers.IO.
     * The reactive Room Flow triggers instant recomposition on the Dashboard screen.
     */
    fun addPantryItem(
        name: String,
        category: String,
        expiryTimestamp: Long,
        barcode: String? = null,
        quantity: Int = 1,
        onSuccess: (() -> Unit)? = null
    ) {
        val safeName = name.trim().ifBlank { "Pantry Item" }
        val categoryEnum = Category.fromString(category)

        viewModelScope.launch(ioDispatcher) {
            val newItem = PantryItem(
                name = safeName,
                category = categoryEnum,
                expiryTimestamp = expiryTimestamp,
                barcode = barcode?.takeIf { it.isNotBlank() },
                quantity = quantity.coerceAtLeast(1)
            )
            val generatedId = repository.insertItem(newItem)

            withContext(Dispatchers.Main) {
                _itemJustAdded.value = generatedId
                _userMessage.value = "Added \"$safeName\" to pantry"
                onSuccess?.invoke()
            }
        }
    }

    fun updateQuantity(item: PantryItem, delta: Int) {
        val newQuantity = (item.quantity + delta).coerceAtLeast(0)
        viewModelScope.launch(ioDispatcher) {
            if (newQuantity == 0) {
                repository.deleteItem(item)
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Removed ${item.name}"
                }
            } else {
                repository.updateQuantity(item.id, newQuantity)
            }
        }
    }

    fun deleteItem(item: PantryItem) {
        viewModelScope.launch(ioDispatcher) {
            repository.deleteItem(item)
            withContext(Dispatchers.Main) {
                _userMessage.value = "Deleted ${item.name}"
            }
        }
    }

    fun consumeMessage() {
        _userMessage.value = null
    }

    fun clearJustAdded() {
        _itemJustAdded.value = null
    }
}
