package com.shelflife.ai.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shelflife.ai.data.local.AppDatabase
import com.shelflife.ai.data.local.PantryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module responsible for provisioning the Room persistence layer.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                val day = 24L * 3600 * 1000L
                val hour = 3600 * 1000L

                // Seed 20 authentic Indian household groceries into Room SQLite
                val indianGroceries = listOf(
                    "('Amul Malai Paneer', '8901262040122', ${now + 26 * hour}, 'DAIRY', 1, ${now - 2 * day}, NULL, ${now - 3 * day}, 'USE_BY')",
                    "('Fresh Dhaniya (Coriander)', '8901030865430', ${now + 36 * hour}, 'PRODUCE', 2, ${now - 1 * day}, NULL, ${now - 2 * day}, 'BEST_BEFORE')",
                    "('Amul Taaza Toned Milk', '8901262010019', ${now + 2 * day}, 'DAIRY', 2, ${now - 1 * day}, NULL, ${now - 2 * day}, 'USE_BY')",
                    "('Kadi Patta (Curry Leaves)', '8901030865431', ${now + 3 * day}, 'PRODUCE', 1, ${now - 1 * day}, NULL, ${now - 2 * day}, 'BEST_BEFORE')",
                    "('Mother Dairy Classic Dahi', '8901648001018', ${now + 4 * day}, 'DAIRY', 2, ${now - 2 * day}, NULL, ${now - 5 * day}, 'BEST_BEFORE')",
                    "('Fresh Tamatar (Tomatoes)', '8901030865445', ${now + 4 * day}, 'PRODUCE', 6, ${now - 2 * day}, NULL, ${now - 4 * day}, 'BEST_BEFORE')",
                    "('Hari Mirch (Green Chillies)', '8901030865432', ${now + 5 * day}, 'PRODUCE', 1, ${now - 1 * day}, NULL, ${now - 3 * day}, 'BEST_BEFORE')",
                    "('Amul Salted Butter', '8901262020018', ${now + 45 * day}, 'DAIRY', 1, ${now - 5 * day}, NULL, ${now - 15 * day}, 'BEST_BEFORE')",
                    "('Pyaz (Red Onions)', '8901030865446', ${now + 14 * day}, 'PRODUCE', 10, ${now - 3 * day}, NULL, ${now - 7 * day}, 'BEST_BEFORE')",
                    "('Aashirvaad Shudh Chakki Atta', '8901725181122', ${now + 90 * day}, 'PANTRY', 1, ${now - 10 * day}, NULL, ${now - 20 * day}, 'BEST_BEFORE')",
                    "('India Gate Basmati Rice', '8901725134012', ${now + 180 * day}, 'PANTRY', 1, ${now - 15 * day}, NULL, ${now - 30 * day}, 'BEST_BEFORE')",
                    "('Tata Sampann Toor Dal', '8901030865421', ${now + 120 * day}, 'PANTRY', 2, ${now - 8 * day}, NULL, ${now - 25 * day}, 'BEST_BEFORE')",
                    "('Tata Sampann Moong Dal', '8901030865422', ${now + 120 * day}, 'PANTRY', 1, ${now - 8 * day}, NULL, ${now - 25 * day}, 'BEST_BEFORE')",
                    "('Amul Pure Cow Ghee', '8901262050015', ${now + 180 * day}, 'PANTRY', 1, ${now - 12 * day}, NULL, ${now - 30 * day}, 'BEST_BEFORE')",
                    "('Adrak-Lehsun (Ginger-Garlic) Paste', '8901063142289', ${now + 60 * day}, 'PANTRY', 1, ${now - 5 * day}, NULL, ${now - 15 * day}, 'BEST_BEFORE')",
                    "('MDH Haldi (Turmeric) Powder', '8902167000102', ${now + 240 * day}, 'PANTRY', 1, ${now - 20 * day}, NULL, ${now - 45 * day}, 'BEST_BEFORE')",
                    "('Everest Garam Masala', '8901786101015', ${now + 180 * day}, 'PANTRY', 1, ${now - 15 * day}, NULL, ${now - 40 * day}, 'BEST_BEFORE')",
                    "('Tata Salt Vacuum Evaporated', '8901030865440', ${now + 365 * day}, 'PANTRY', 1, ${now - 10 * day}, NULL, ${now - 60 * day}, 'EXACT')",
                    "('Fortune Sunlite Sunflower Oil', '8906007281014', ${now + 150 * day}, 'PANTRY', 2, ${now - 10 * day}, NULL, ${now - 30 * day}, 'BEST_BEFORE')",
                    "('Wagh Bakri Premium CTC Tea', '8901768001019', ${now + 240 * day}, 'PANTRY', 1, ${now - 15 * day}, NULL, ${now - 45 * day}, 'BEST_BEFORE')"
                )

                for (itemSql in indianGroceries) {
                    db.execSQL(
                        "INSERT INTO pantry_items (name, barcode, expiryTimestamp, category, quantity, addedTimestamp, imageUri, mfgDateTimestamp, expiryMode) VALUES $itemSql"
                    )
                }
            }
        })
        .build()
    }

    @Provides
    @Singleton
    fun providePantryDao(database: AppDatabase): PantryDao {
        return database.pantryDao()
    }
}
