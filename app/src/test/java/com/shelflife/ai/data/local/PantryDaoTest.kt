package com.shelflife.ai.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Automated unit test suite verifying Room DAO operations in an in-memory database.
 *
 * Verifies:
 * - Empty table emissions
 * - Ascending ordering by expiryTimestamp
 * - Urgency threshold query filtering
 * - WorkManager synchronous suspend query
 * - Atomic quantity updates
 * - Entity deletion by reference and by ID
 * - Point lookup by barcode
 * - Replacement on conflict strategy
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PantryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var pantryDao: PantryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pantryDao = database.pantryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun emptyDatabaseReturnsEmptyListFlow() = runTest {
        val items = pantryDao.getAllItems().first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun insertAndObserveAllItemsOrderedByExpiryAscending() = runTest {
        val now = 1_000_000L
        val itemLate = PantryItemEntity(name = "Canned Beans", expiryTimestamp = now + 100_000L, category = "PANTRY")
        val itemEarly = PantryItemEntity(name = "Fresh Milk", expiryTimestamp = now + 10_000L, category = "DAIRY")
        val itemMid = PantryItemEntity(name = "Yogurt", expiryTimestamp = now + 50_000L, category = "DAIRY")

        pantryDao.insertItem(itemLate)
        pantryDao.insertItem(itemEarly)
        pantryDao.insertItem(itemMid)

        pantryDao.getAllItems().test {
            val items = awaitItem()
            assertEquals(3, items.size)
            assertEquals("Fresh Milk", items[0].name)
            assertEquals("Yogurt", items[1].name)
            assertEquals("Canned Beans", items[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getItemsByUrgencyThresholdFiltersCorrectly() = runTest {
        val now = 1_000_000L
        val itemCritical = PantryItemEntity(name = "Ripe Berries", expiryTimestamp = now + 24 * 3600 * 1000L, category = "PRODUCE")
        val itemWarning = PantryItemEntity(name = "Cheddar Cheese", expiryTimestamp = now + 40 * 3600 * 1000L, category = "DAIRY")
        val itemFresh = PantryItemEntity(name = "Frozen Peas", expiryTimestamp = now + 72 * 3600 * 1000L, category = "FROZEN")

        pantryDao.insertItem(itemCritical)
        pantryDao.insertItem(itemWarning)
        pantryDao.insertItem(itemFresh)

        val threshold = now + 48 * 3600 * 1000L // 48 hours threshold

        pantryDao.getItemsByUrgencyThreshold(threshold).test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals("Ripe Berries", items[0].name)
            assertEquals("Cheddar Cheese", items[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getItemsExpiringBeforeSyncReturnsListSynchronouslyForWorker() = runTest {
        val now = 2_000_000L
        val itemExpiring = PantryItemEntity(name = "Bread", expiryTimestamp = now + 10_000L, category = "BAKERY", quantity = 2)
        val itemFar = PantryItemEntity(name = "Rice", expiryTimestamp = now + 500_000L, category = "PANTRY", quantity = 1)

        pantryDao.insertItem(itemExpiring)
        pantryDao.insertItem(itemFar)

        val expiringItems = pantryDao.getItemsExpiringBeforeSync(now + 20_000L)
        assertEquals(1, expiringItems.size)
        assertEquals("Bread", expiringItems[0].name)
        assertEquals(2, expiringItems[0].quantity)
    }

    @Test
    fun updateQuantityUpdatesValueWithoutAlteringOtherFields() = runTest {
        val initialItem = PantryItemEntity(
            name = "Eggs",
            barcode = "123456789012",
            expiryTimestamp = 5_000_000L,
            category = "DAIRY",
            quantity = 6
        )
        val id = pantryDao.insertItem(initialItem)

        pantryDao.updateQuantity(id, 12)

        val updated = pantryDao.getItemById(id)
        assertNotNull(updated)
        assertEquals(12, updated!!.quantity)
        assertEquals("Eggs", updated.name)
        assertEquals("123456789012", updated.barcode)
        assertEquals(5_000_000L, updated.expiryTimestamp)
        assertEquals("DAIRY", updated.category)
    }

    @Test
    fun deleteItemRemovesEntity() = runTest {
        val item = PantryItemEntity(name = "Avocado", expiryTimestamp = 3_000_000L, category = "PRODUCE")
        val id = pantryDao.insertItem(item)

        val inserted = pantryDao.getItemById(id)
        assertNotNull(inserted)

        pantryDao.deleteItem(inserted!!)

        val afterDelete = pantryDao.getItemById(id)
        assertNull(afterDelete)
    }

    @Test
    fun deleteItemByIdRemovesEntity() = runTest {
        val item = PantryItemEntity(name = "Spinach", expiryTimestamp = 4_000_000L, category = "PRODUCE")
        val id = pantryDao.insertItem(item)

        pantryDao.deleteItemById(id)

        val items = pantryDao.getAllItems().first()
        assertTrue(items.none { it.id == id })
    }

    @Test
    fun getItemByBarcodeReturnsCorrectItem() = runTest {
        val barcode = "0987654321"
        val item = PantryItemEntity(name = "Pasta", barcode = barcode, expiryTimestamp = 10_000_000L, category = "PANTRY")
        pantryDao.insertItem(item)

        val found = pantryDao.getItemByBarcode(barcode)
        assertNotNull(found)
        assertEquals("Pasta", found!!.name)
        assertEquals(barcode, found.barcode)
    }

    @Test
    fun insertReplacesOnConflictWhenSameId() = runTest {
        val item1 = PantryItemEntity(id = 100L, name = "Apple", expiryTimestamp = 1_000L, category = "PRODUCE", quantity = 1)
        pantryDao.insertItem(item1)

        val item1Replacement = PantryItemEntity(id = 100L, name = "Honeycrisp Apple", expiryTimestamp = 2_000L, category = "PRODUCE", quantity = 3)
        pantryDao.insertItem(item1Replacement)

        val item = pantryDao.getItemById(100L)
        assertNotNull(item)
        assertEquals("Honeycrisp Apple", item!!.name)
        assertEquals(3, item.quantity)
        assertEquals(2_000L, item.expiryTimestamp)
    }

    @Test
    fun deleteAllItemsClearsAllRows() = runTest {
        pantryDao.insertItem(PantryItemEntity(name = "Item 1", expiryTimestamp = 1000L, category = "PRODUCE"))
        pantryDao.insertItem(PantryItemEntity(name = "Item 2", expiryTimestamp = 2000L, category = "DAIRY"))

        val beforeCount = pantryDao.getAllItems().first().size
        assertEquals(2, beforeCount)

        val deletedCount = pantryDao.deleteAllItems()
        assertEquals(2, deletedCount)

        val afterCount = pantryDao.getAllItems().first().size
        assertEquals(0, afterCount)
    }
}
