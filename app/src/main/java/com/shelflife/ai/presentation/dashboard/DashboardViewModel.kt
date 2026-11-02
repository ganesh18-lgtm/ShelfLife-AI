package com.shelflife.ai.presentation.dashboard

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelflife.ai.data.local.UserProfileManager
import com.shelflife.ai.di.IoDispatcher
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.model.UserProfile
import com.shelflife.ai.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel managing UDF reactive state streams and user operations for the pantry dashboard,
 * including item creation, real-time editing of product details & expiry dates, user profile context, and filtering.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: PantryRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userProfileManager: UserProfileManager? = null
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userProfileManager?.profile
        ?: MutableStateFlow(UserProfile()).asStateFlow()

    val geminiApiKey: StateFlow<String> = userProfileManager?.geminiApiKey
        ?: MutableStateFlow("").asStateFlow()

    fun setGeminiApiKey(key: String) {
        userProfileManager?.setGeminiApiKey(key)
    }

    fun hasGeminiApiKey(): Boolean = userProfileManager?.hasGeminiApiKey() == true

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    private val _selectedUrgency = MutableStateFlow<ExpiryUrgency?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)

    private val _editingItem = MutableStateFlow<PantryItem?>(null)
    val editingItem: StateFlow<PantryItem?> = _editingItem.asStateFlow()

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

    init {
        viewModelScope.launch(ioDispatcher) {
            try {
                val existing = repository.getItemsExpiringBeforeSync(Long.MAX_VALUE)
                val hasIndianItems = existing.any {
                    it.name.contains("Paneer", ignoreCase = true) ||
                    it.name.contains("Atta", ignoreCase = true) ||
                    it.name.contains("Dal", ignoreCase = true)
                }
                if (!hasIndianItems) {
                    repository.insertItems(createIndianHouseholdGroceries())
                }
            } catch (e: Exception) {
                Log.w("DashboardViewModel", "Could not seed initial Indian groceries", e)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: Category?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setUrgencyFilter(urgency: ExpiryUrgency?) {
        _selectedUrgency.value = if (_selectedUrgency.value == urgency) null else urgency
    }

    fun startEditingItem(item: PantryItem) {
        _editingItem.value = item
    }

    fun loadItemToEdit(itemId: Long) {
        if (itemId == -1L) {
            _editingItem.value = null
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val item = repository.getItemById(itemId)
            withContext(Dispatchers.Main) {
                _editingItem.value = item
            }
        }
    }

    fun clearEditingItem() {
        _editingItem.value = null
    }

    fun addPantryItem(
        name: String,
        category: String,
        expiryTimestamp: Long,
        barcode: String? = null,
        quantity: Int = 1,
        imageUri: String? = null,
        mfgDateTimestamp: Long? = null,
        expiryMode: String = "EXACT",
        onSuccess: (() -> Unit)? = null
    ) {
        val safeName = name.trim().ifBlank { "Pantry Item" }
        val categoryEnum = Category.fromString(category)

        viewModelScope.launch(ioDispatcher) {
            try {
                val newItem = PantryItem(
                    name = safeName,
                    category = categoryEnum,
                    expiryTimestamp = expiryTimestamp,
                    barcode = barcode?.takeIf { it.isNotBlank() },
                    quantity = quantity.coerceAtLeast(1),
                    imageUri = imageUri,
                    mfgDateTimestamp = mfgDateTimestamp,
                    expiryMode = expiryMode
                )
                repository.insertItem(newItem)
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Added \"$safeName\" to pantry"
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error adding pantry item", e)
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Failed to add item: ${e.message}"
                }
            }
        }
    }

    fun updatePantryItem(
        item: PantryItem,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.updateItem(item)
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Updated \"${item.name}\""
                    _editingItem.value = null
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error updating pantry item", e)
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Failed to update item: ${e.message}"
                }
            }
        }
    }

    fun updateQuantity(item: PantryItem, delta: Int) {
        val newQuantity = (item.quantity + delta).coerceAtLeast(0)
        viewModelScope.launch(ioDispatcher) {
            try {
                if (newQuantity == 0) {
                    repository.deleteItem(item)
                    withContext(Dispatchers.Main) {
                        _userMessage.value = "Removed ${item.name} from pantry"
                    }
                } else {
                    repository.updateQuantity(item.id, newQuantity)
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error updating quantity", e)
            }
        }
    }

    fun deleteItem(item: PantryItem) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.deleteItem(item)
                withContext(Dispatchers.Main) {
                    _userMessage.value = "Deleted ${item.name}"
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error deleting item", e)
            }
        }
    }

    fun deleteItemSilently(item: PantryItem) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.deleteItem(item)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error deleting item", e)
            }
        }
    }

    fun restoreItem(item: PantryItem) {
        viewModelScope.launch(ioDispatcher) {
            try {
                repository.insertItem(item)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error restoring item", e)
            }
        }
    }

    fun seedSampleData(onSuccess: (() -> Unit)? = null) {
        val sampleItems = createIndianHouseholdGroceries()
        viewModelScope.launch(ioDispatcher) {
            repository.insertItems(sampleItems)
            withContext(Dispatchers.Main) {
                _userMessage.value = "Loaded ${sampleItems.size} Indian grocery items into Room SQLite"
                onSuccess?.invoke()
            }
        }
    }

    companion object {
        fun createIndianHouseholdGroceries(): List<PantryItem> {
            val now = System.currentTimeMillis()
            val hour = 3600 * 1000L
            val day = 24L * hour

            return listOf(
                PantryItem(
                    name = "Amul Malai Paneer",
                    barcode = "8901262040122",
                    expiryTimestamp = now + (26 * hour), // Critical: 26 hours
                    category = Category.DAIRY,
                    quantity = 1,
                    addedTimestamp = now - (2 * day),
                    mfgDateTimestamp = now - (3 * day),
                    expiryMode = "USE_BY"
                ),
                PantryItem(
                    name = "Fresh Dhaniya (Coriander Leaves)",
                    barcode = "8901030865430",
                    expiryTimestamp = now + (36 * hour), // Critical: 36 hours
                    category = Category.PRODUCE,
                    quantity = 2,
                    addedTimestamp = now - (1 * day),
                    mfgDateTimestamp = now - (2 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Amul Taaza Toned Milk",
                    barcode = "8901262010019",
                    expiryTimestamp = now + (2 * day), // Warning: 2 days
                    category = Category.DAIRY,
                    quantity = 2,
                    addedTimestamp = now - (1 * day),
                    mfgDateTimestamp = now - (2 * day),
                    expiryMode = "USE_BY"
                ),
                PantryItem(
                    name = "Kadi Patta (Fresh Curry Leaves)",
                    barcode = "8901030865431",
                    expiryTimestamp = now + (3 * day), // Warning: 3 days
                    category = Category.PRODUCE,
                    quantity = 1,
                    addedTimestamp = now - (1 * day),
                    mfgDateTimestamp = now - (2 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Mother Dairy Classic Dahi / Curd",
                    barcode = "8901648001018",
                    expiryTimestamp = now + (4 * day), // Warning: 4 days
                    category = Category.DAIRY,
                    quantity = 2,
                    addedTimestamp = now - (2 * day),
                    mfgDateTimestamp = now - (5 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Fresh Tamatar (Country Tomatoes)",
                    barcode = "8901030865445",
                    expiryTimestamp = now + (4 * day), // Warning: 4 days
                    category = Category.PRODUCE,
                    quantity = 6,
                    addedTimestamp = now - (2 * day),
                    mfgDateTimestamp = now - (4 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Hari Mirch (Fresh Green Chillies)",
                    barcode = "8901030865432",
                    expiryTimestamp = now + (5 * day), // Warning/Fresh: 5 days
                    category = Category.PRODUCE,
                    quantity = 1,
                    addedTimestamp = now - (1 * day),
                    mfgDateTimestamp = now - (3 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Pyaz (Nashik Red Onions)",
                    barcode = "8901030865446",
                    expiryTimestamp = now + (14 * day), // Fresh: 14 days
                    category = Category.PRODUCE,
                    quantity = 10,
                    addedTimestamp = now - (3 * day),
                    mfgDateTimestamp = now - (7 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Aashirvaad Shudh Chakki Atta",
                    barcode = "8901725181122",
                    expiryTimestamp = now + (90 * day), // Fresh: 90 days
                    category = Category.PANTRY,
                    quantity = 1,
                    addedTimestamp = now - (10 * day),
                    mfgDateTimestamp = now - (20 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "India Gate Classic Basmati Rice",
                    barcode = "8901725134012",
                    expiryTimestamp = now + (180 * day), // Fresh: 180 days
                    category = Category.PANTRY,
                    quantity = 1,
                    addedTimestamp = now - (15 * day),
                    mfgDateTimestamp = now - (30 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Tata Sampann Unpolished Toor Dal",
                    barcode = "8901030865421",
                    expiryTimestamp = now + (120 * day), // Fresh: 120 days
                    category = Category.PANTRY,
                    quantity = 2,
                    addedTimestamp = now - (8 * day),
                    mfgDateTimestamp = now - (25 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Tata Sampann Moong Dal Dhuli",
                    barcode = "8901030865422",
                    expiryTimestamp = now + (120 * day), // Fresh: 120 days
                    category = Category.PANTRY,
                    quantity = 1,
                    addedTimestamp = now - (8 * day),
                    mfgDateTimestamp = now - (25 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Amul Pure Cow Ghee (1L)",
                    barcode = "8901262050015",
                    expiryTimestamp = now + (180 * day), // Fresh: 180 days
                    category = Category.PANTRY,
                    quantity = 1,
                    addedTimestamp = now - (12 * day),
                    mfgDateTimestamp = now - (30 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Amul Pasteurised Salted Butter",
                    barcode = "8901262020018",
                    expiryTimestamp = now + (45 * day), // Fresh: 45 days
                    category = Category.DAIRY,
                    quantity = 1,
                    addedTimestamp = now - (5 * day),
                    mfgDateTimestamp = now - (15 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Adrak-Lehsun (Ginger-Garlic) Paste",
                    barcode = "8901063142289",
                    expiryTimestamp = now + (60 * day), // Fresh: 60 days
                    category = Category.PANTRY,
                    quantity = 1,
                    addedTimestamp = now - (5 * day),
                    mfgDateTimestamp = now - (15 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "MDH Agmark Haldi (Turmeric) Powder",
                    barcode = "8902167000102",
                    expiryTimestamp = now + (240 * day), // Fresh: 240 days
                    category = Category.PANTRY,
                    quantity = 1,
                    addedTimestamp = now - (20 * day),
                    mfgDateTimestamp = now - (45 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Everest Super Garam Masala",
                    barcode = "8901786101015",
                    expiryTimestamp = now + (180 * day), // Fresh: 180 days
                    category = Category.PANTRY,
                    quantity = 1,
                    addedTimestamp = now - (15 * day),
                    mfgDateTimestamp = now - (40 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Tata Salt Vacuum Evaporated (1kg)",
                    barcode = "8901030865440",
                    expiryTimestamp = now + (365 * day), // Fresh: 365 days
                    category = Category.PANTRY,
                    quantity = 1,
                    addedTimestamp = now - (10 * day),
                    mfgDateTimestamp = now - (60 * day),
                    expiryMode = "EXACT"
                ),
                PantryItem(
                    name = "Fortune Sunlite Refined Sunflower Oil",
                    barcode = "8906007281014",
                    expiryTimestamp = now + (150 * day), // Fresh: 150 days
                    category = Category.PANTRY,
                    quantity = 2,
                    addedTimestamp = now - (10 * day),
                    mfgDateTimestamp = now - (30 * day),
                    expiryMode = "BEST_BEFORE"
                ),
                PantryItem(
                    name = "Wagh Bakri Premium CTC Leaf Tea",
                    barcode = "8901768001019",
                    expiryTimestamp = now + (240 * day), // Fresh: 240 days
                    category = Category.BEVERAGES,
                    quantity = 1,
                    addedTimestamp = now - (15 * day),
                    mfgDateTimestamp = now - (45 * day),
                    expiryMode = "BEST_BEFORE"
                )
            )
        }
    }

    fun clearAllPantryItems(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch(ioDispatcher) {
            repository.deleteAllItems()
            withContext(Dispatchers.Main) {
                _userMessage.value = "Cleared all pantry items from Room SQLite"
                onSuccess?.invoke()
            }
        }
    }

    fun consumeMessage() {
        _userMessage.value = null
    }

    fun updateUserProfile(
        name: String,
        email: String,
        phone: String = UserProfileManager.DEFAULT_PHONE,
        bio: String = UserProfileManager.DEFAULT_BIO,
        householdSize: String,
        dietaryPreference: String,
        photoUri: String?
    ) {
        userProfileManager?.updateProfile(
            name = name,
            email = email,
            phone = phone,
            bio = bio,
            householdSize = householdSize,
            dietaryPreference = dietaryPreference,
            photoUri = photoUri
        )
    }

    fun updateUserProfile(
        name: String,
        email: String,
        bio: String,
        householdSize: String,
        dietaryPreference: String,
        photoUri: String?
    ) {
        updateUserProfile(
            name = name,
            email = email,
            phone = userProfile.value.phone,
            bio = bio,
            householdSize = householdSize,
            dietaryPreference = dietaryPreference,
            photoUri = photoUri
        )
    }

    fun saveProfilePhotoFromUri(uri: Uri): String? {
        return userProfileManager?.saveImageFromUri(uri)
    }

    fun saveProfilePhotoFromBitmap(bitmap: Bitmap): String? {
        return userProfileManager?.saveImageFromBitmap(bitmap)
    }

    fun resetUserProfile() {
        userProfileManager?.resetProfile()
    }
}
