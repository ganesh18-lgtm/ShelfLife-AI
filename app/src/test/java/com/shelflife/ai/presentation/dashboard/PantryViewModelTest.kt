package com.shelflife.ai.presentation.dashboard

import app.cash.turbine.test
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PantryRepository
    private val itemsFlow = MutableStateFlow<List<PantryItem>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.getAllItems() } returns itemsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyEmitsItemsFromRepository() = runTest {
        val now = System.currentTimeMillis()
        val item1 = PantryItem(id = 1L, name = "Almond Milk", expiryTimestamp = now + 100_000, category = Category.DAIRY)
        val item2 = PantryItem(id = 2L, name = "Spinach", expiryTimestamp = now + 200_000, category = Category.PRODUCE)
        itemsFlow.value = listOf(item1, item2)

        val viewModel = PantryViewModel(repository, testDispatcher)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            testScheduler.advanceUntilIdle()
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.items.size)
            assertEquals("Almond Milk", state.items[0].name)
            assertEquals("Spinach", state.items[1].name)
        }
    }

    @Test
    fun addPantryItem_persistsToRepositoryAndNotifiesSuccess() = runTest {
        val itemSlot = slot<PantryItem>()
        coEvery { repository.insertItem(capture(itemSlot)) } returns 42L

        val viewModel = PantryViewModel(repository, testDispatcher)

        var successCallbackCalled = false
        val now = System.currentTimeMillis()
        val futureExpiry = now + 500_000L

        viewModel.addPantryItem(
            name = "Fresh Strawberries",
            category = "Produce",
            expiryTimestamp = futureExpiry,
            barcode = "012345678905",
            quantity = 2,
            onSuccess = { successCallbackCalled = true }
        )

        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.insertItem(any()) }
        assertTrue(successCallbackCalled)
        assertEquals(42L, viewModel.itemJustAdded.value)
        assertEquals("Fresh Strawberries", itemSlot.captured.name)
        assertEquals(Category.PRODUCE, itemSlot.captured.category)
        assertEquals(futureExpiry, itemSlot.captured.expiryTimestamp)
        assertEquals("012345678905", itemSlot.captured.barcode)
        assertEquals(2, itemSlot.captured.quantity)
    }

    @Test
    fun uiState_filtersBySearchQueryAndCategory() = runTest {
        val now = System.currentTimeMillis()
        val item1 = PantryItem(id = 1L, name = "Greek Yogurt", category = Category.DAIRY, expiryTimestamp = now + 100_000)
        val item2 = PantryItem(id = 2L, name = "Cheddar Cheese", category = Category.DAIRY, expiryTimestamp = now + 200_000)
        val item3 = PantryItem(id = 3L, name = "Carrots", category = Category.PRODUCE, expiryTimestamp = now + 300_000)
        itemsFlow.value = listOf(item1, item2, item3)

        val viewModel = PantryViewModel(repository, testDispatcher)

        viewModel.uiState.test {
            awaitItem() // Loading
            testScheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assertEquals(3, loaded.items.size)

            viewModel.setCategoryFilter(Category.DAIRY)
            testScheduler.advanceUntilIdle()
            val categoryFiltered = awaitItem()
            assertEquals(2, categoryFiltered.items.size)

            viewModel.onSearchQueryChanged("Greek")
            testScheduler.advanceUntilIdle()
            val searchFiltered = awaitItem()
            assertEquals(1, searchFiltered.items.size)
            assertEquals("Greek Yogurt", searchFiltered.items[0].name)
        }
    }

    @Test
    fun updateQuantity_updatesInRepository() = runTest {
        val item = PantryItem(id = 5L, name = "Avocado", quantity = 3, expiryTimestamp = 1_000_000L)
        val viewModel = PantryViewModel(repository, testDispatcher)

        viewModel.updateQuantity(item, delta = 1)
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.updateQuantity(5L, 4) }

        viewModel.updateQuantity(item, delta = -3)
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.deleteItem(item) }
    }

    @Test
    fun deleteItem_callsRepositoryDelete() = runTest {
        val item = PantryItem(id = 9L, name = "Milk", quantity = 1, expiryTimestamp = 1_000_000L)
        val viewModel = PantryViewModel(repository, testDispatcher)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteItem(item) }
    }
}
