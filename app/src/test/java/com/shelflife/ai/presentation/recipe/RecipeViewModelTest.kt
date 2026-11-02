package com.shelflife.ai.presentation.recipe

import app.cash.turbine.test
import com.shelflife.ai.data.ai.GeminiManager
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PantryRepository
    private lateinit var geminiManager: GeminiManager
    private val itemsFlow = MutableStateFlow<List<PantryItem>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        geminiManager = mockk(relaxed = true)
        coEvery { repository.getAllItems() } returns itemsFlow
        every { geminiManager.hasApiKey() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_withEmptyPantry_emitsEmptyRecipeList() = runTest {
        itemsFlow.value = emptyList()
        val viewModel = RecipeViewModel(repository, geminiManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            testScheduler.advanceUntilIdle()
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.recipes.isEmpty())
            assertEquals(0, state.expiringItemsCount)
        }
    }

    @Test
    fun uiState_withExpiringProduceAndDairy_generatesZeroWasteRecipes() = runTest {
        val now = System.currentTimeMillis()
        val item1 = PantryItem(
            id = 1L,
            name = "Fresh Spinach",
            category = Category.PRODUCE,
            expiryTimestamp = now + (24 * 3600 * 1000L), // <48h Critical
            quantity = 1
        )
        val item2 = PantryItem(
            id = 2L,
            name = "Greek Yogurt",
            category = Category.DAIRY,
            expiryTimestamp = now + (36 * 3600 * 1000L), // <48h Critical
            quantity = 2
        )
        itemsFlow.value = listOf(item1, item2)

        val viewModel = RecipeViewModel(repository, geminiManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            testScheduler.advanceUntilIdle()
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.expiringItemsCount)
            assertEquals(2, state.totalPantryItemsCount)
            assertTrue(state.recipes.isNotEmpty())

            val allRescued = state.recipes.flatMap { it.rescuedPantryItems }
            assertTrue(allRescued.contains("Fresh Spinach") || allRescued.contains("Greek Yogurt"))
        }
    }
}
