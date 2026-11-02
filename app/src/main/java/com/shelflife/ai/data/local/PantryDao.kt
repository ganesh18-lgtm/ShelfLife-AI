package com.shelflife.ai.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {

    /**
     * Observes all pantry items sorted by expiration timestamp in ascending order (earliest first).
     */
    @Query("SELECT * FROM pantry_items ORDER BY expiryTimestamp ASC")
    fun getAllItems(): Flow<List<PantryItemEntity>>

    /**
     * Observes items expiring at or before [thresholdTimestamp] sorted by expiration timestamp ASC.
     * Used for reactive dashboard urgency filter views.
     */
    @Query("SELECT * FROM pantry_items WHERE expiryTimestamp <= :thresholdTimestamp ORDER BY expiryTimestamp ASC")
    fun getItemsByUrgencyThreshold(thresholdTimestamp: Long): Flow<List<PantryItemEntity>>

    /**
     * Synchronous one-shot retrieval for items expiring at or before [thresholdTimestamp].
     * Specifically designed for background jobs like ExpiryNotificationWorker.
     */
    @Query("SELECT * FROM pantry_items WHERE expiryTimestamp <= :thresholdTimestamp ORDER BY expiryTimestamp ASC")
    suspend fun getItemsExpiringBeforeSync(thresholdTimestamp: Long): List<PantryItemEntity>

    /**
     * Observes items filtered by [category], ordered by expiration timestamp ASC.
     */
    @Query("SELECT * FROM pantry_items WHERE category = :category ORDER BY expiryTimestamp ASC")
    fun getItemsByCategory(category: String): Flow<List<PantryItemEntity>>

    /**
     * Observes items whose name or barcode contains [query].
     */
    @Query("SELECT * FROM pantry_items WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY expiryTimestamp ASC")
    fun searchItems(query: String): Flow<List<PantryItemEntity>>

    /**
     * Suspended lookup for a single item by its primary key ID.
     */
    @Query("SELECT * FROM pantry_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): PantryItemEntity?

    /**
     * Observes a single item by its primary key ID.
     */
    @Query("SELECT * FROM pantry_items WHERE id = :id LIMIT 1")
    fun getItemByIdFlow(id: Long): Flow<PantryItemEntity?>

    /**
     * Suspended lookup for a single item by its barcode (UPC/EAN).
     */
    @Query("SELECT * FROM pantry_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): PantryItemEntity?

    /**
     * Inserts an item into the database. If ID conflicts, replaces the existing record.
     * @return Generated primary key ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PantryItemEntity): Long

    /**
     * Inserts multiple items into the database.
     * @return List of generated primary key IDs.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PantryItemEntity>): List<Long>

    /**
     * Updates an existing pantry item.
     */
    @Update
    suspend fun updateItem(item: PantryItemEntity)

    /**
     * Atomically updates quantity for a specific item without rewriting other fields.
     */
    @Query("UPDATE pantry_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Int)

    /**
     * Deletes a specific item by entity instance.
     */
    @Delete
    suspend fun deleteItem(item: PantryItemEntity)

    /**
     * Deletes an item by its primary key ID.
     */
    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    /**
     * Deletes all items whose expiration timestamp is strictly less than [currentTimestamp].
     * @return Number of deleted rows.
     */
    @Query("DELETE FROM pantry_items WHERE expiryTimestamp < :currentTimestamp")
    suspend fun deleteExpiredItems(currentTimestamp: Long): Int

    /**
     * Deletes all items from the pantry database table.
     * @return Number of deleted rows.
     */
    @Query("DELETE FROM pantry_items")
    suspend fun deleteAllItems(): Int
}
