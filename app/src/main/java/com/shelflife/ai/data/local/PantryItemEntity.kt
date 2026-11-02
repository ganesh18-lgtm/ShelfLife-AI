package com.shelflife.ai.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an item stored in the local pantry database.
 *
 * Indexed columns optimize:
 * - [expiryTimestamp] for urgency sorting and notification threshold scans.
 * - [barcode] for instant scanner lookups.
 * - [category] for category-filtered pantry queries.
 */
@Entity(
    tableName = "pantry_items",
    indices = [
        Index(value = ["expiryTimestamp"]),
        Index(value = ["barcode"]),
        Index(value = ["category"])
    ]
)
data class PantryItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "barcode")
    val barcode: String? = null,

    @ColumnInfo(name = "expiryTimestamp")
    val expiryTimestamp: Long,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "quantity")
    val quantity: Int = 1,

    @ColumnInfo(name = "addedTimestamp")
    val addedTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "imageUri")
    val imageUri: String? = null,

    @ColumnInfo(name = "mfgDateTimestamp")
    val mfgDateTimestamp: Long? = null,

    @ColumnInfo(name = "expiryMode")
    val expiryMode: String = "EXACT"
)
