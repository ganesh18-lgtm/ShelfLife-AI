package com.shelflife.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Main Room Database for ShelfLife AI.
 */
@Database(
    entities = [PantryItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao

    companion object {
        const val DATABASE_NAME = "shelflife_pantry.db"
    }
}
