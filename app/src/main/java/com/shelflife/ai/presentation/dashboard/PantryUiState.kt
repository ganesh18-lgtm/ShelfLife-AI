package com.shelflife.ai.presentation.dashboard

import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem

/**
 * Immutable UI state model for the pantry dashboard.
 */
data class PantryUiState(
    val items: List<PantryItem> = emptyList(),
    val isLoading: Boolean = true,
    val selectedCategory: Category? = null,
    val selectedUrgency: ExpiryUrgency? = null,
    val searchQuery: String = "",
    val userMessage: String? = null
) {
    val criticalCount: Int
        get() = items.count { it.urgency == ExpiryUrgency.CRITICAL || it.urgency == ExpiryUrgency.EXPIRED }

    val warningCount: Int
        get() = items.count { it.urgency == ExpiryUrgency.WARNING }

    val freshCount: Int
        get() = items.count { it.urgency == ExpiryUrgency.FRESH }

    val totalCount: Int
        get() = items.size
}
