package com.shelflife.ai.domain.repository

import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract defining all storage, query, and observation operations
 * for smart pantry inventory management.
 */
interface PantryRepository {

    /**
     * Emits a continuous reactive stream of all pantry items ordered by expiry date
     * ascending (earliest expiration first).
     */
    fun getAllItems(): Flow<List<PantryItem>>

    /**
     * Emits a reactive stream of pantry items whose expiryTimestamp is less than or
     * equal to [thresholdTimestamp], ordered by expiry date ascending.
     */
    fun getItemsByUrgencyThreshold(thresholdTimestamp: Long): Flow<List<PantryItem>>

    /**
     * Synchronously/suspendingly retrieves all items whose expiryTimestamp <= [thresholdTimestamp],
     * ordered by expiry date ascending. Specifically designed for background jobs
     * such as WorkManager workers.
     */
    suspend fun getItemsExpiringBeforeSync(thresholdTimestamp: Long): List<PantryItem>

    /**
     * Contract harmony alias for [getItemsExpiringBeforeSync].
     */
    suspend fun getItemsExpiringWithinSync(thresholdTimestamp: Long): List<PantryItem> =
        getItemsExpiringBeforeSync(thresholdTimestamp)

    /**
     * Emits a reactive stream of items belonging to the specified [category],
     * ordered by expiry date ascending.
     */
    fun getItemsByCategory(category: Category): Flow<List<PantryItem>>

    /**
     * Emits a reactive stream of items whose name or barcode contains the search [query].
     */
    fun searchItems(query: String): Flow<List<PantryItem>>

    /**
     * Retrieves a single item by its database [id], or null if not found.
     */
    suspend fun getItemById(id: Long): PantryItem?

    /**
     * Observes a single item by its database [id] as a reactive stream.
     */
    fun observeItemById(id: Long): Flow<PantryItem?>

    /**
     * Retrieves a single item by its [barcode] string, or null if not found.
     */
    suspend fun getItemByBarcode(barcode: String): PantryItem?

    /**
     * Inserts a pantry item into local storage, returning the auto-generated row ID.
     */
    suspend fun insertItem(item: PantryItem): Long

    /**
     * Batch inserts multiple pantry items into local storage, returning their generated IDs.
     */
    suspend fun insertItems(items: List<PantryItem>): List<Long>

    /**
     * Updates an existing pantry item's details.
     */
    suspend fun updateItem(item: PantryItem)

    /**
     * Directly and atomically updates the quantity of the item identified by [id].
     */
    suspend fun updateQuantity(id: Long, quantity: Int)

    /**
     * Deletes a pantry item from local storage.
     */
    suspend fun deleteItem(item: PantryItem)

    /**
     * Deletes a pantry item by its [id].
     */
    suspend fun deleteItemById(id: Long)

    /**
     * Deletes all pantry items from local storage.
     * @return Number of deleted rows.
     */
    suspend fun deleteAllItems(): Int
}
