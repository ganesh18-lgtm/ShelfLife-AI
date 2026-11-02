package com.shelflife.ai.presentation.dashboard

import app.cash.turbine.test
import com.shelflife.ai.data.local.UserProfileManager
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.model.UserProfile
import com.shelflife.ai.domain.repository.PantryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

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

        val viewModel = DashboardViewModel(repository, testDispatcher)

        viewModel.uiState.test {
            val initial = awaitItem() // initialValue is isLoading = true
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
    fun uiState_filtersBySearchQueryCorrectly() = runTest {
        val now = System.currentTimeMillis()
        val item1 = PantryItem(id = 1L, name = "Greek Yogurt", barcode = "123456", expiryTimestamp = now + 100_000)
        val item2 = PantryItem(id = 2L, name = "Cheddar Cheese", barcode = "987654", expiryTimestamp = now + 200_000)
        itemsFlow.value = listOf(item1, item2)

        val viewModel = DashboardViewModel(repository, testDispatcher)

        viewModel.uiState.test {
            awaitItem() // initial loading state
            testScheduler.advanceUntilIdle()
            val loaded = awaitItem() // initial loaded state
            assertEquals(2, loaded.items.size)

            viewModel.onSearchQueryChanged("Cheddar")
            testScheduler.advanceUntilIdle()
            val state = awaitItem()
            assertEquals(1, state.items.size)
            assertEquals("Cheddar Cheese", state.items[0].name)

            viewModel.onSearchQueryChanged("123456")
            testScheduler.advanceUntilIdle()
            val barcodeMatchState = awaitItem()
            assertEquals(1, barcodeMatchState.items.size)
            assertEquals("Greek Yogurt", barcodeMatchState.items[0].name)
        }
    }

    @Test
    fun addPantryItem_persistsToRepositoryAndTriggersCallback() = runTest {
        val viewModel = DashboardViewModel(repository, testDispatcher)
        var callbackTriggered = false

        viewModel.addPantryItem(
            name = "Organic Butter",
            category = "Dairy",
            expiryTimestamp = 5_000_000L,
            barcode = "12345",
            quantity = 2,
            onSuccess = { callbackTriggered = true }
        )
        testScheduler.advanceUntilIdle()

        coVerify {
            repository.insertItem(match {
                it.name == "Organic Butter" &&
                it.category == Category.DAIRY &&
                it.expiryTimestamp == 5_000_000L &&
                it.barcode == "12345" &&
                it.quantity == 2
            })
        }
        assertTrue(callbackTriggered)
    }

    @Test
    fun updatePantryItem_persistsToRepositoryAndClearsEditingState() = runTest {
        val viewModel = DashboardViewModel(repository, testDispatcher)
        val existingItem = PantryItem(id = 10L, name = "Oat Milk", expiryTimestamp = 1_000_000L, category = Category.DAIRY)
        viewModel.startEditingItem(existingItem)
        assertEquals(existingItem, viewModel.editingItem.value)

        val updatedItem = existingItem.copy(name = "Barista Oat Milk", expiryTimestamp = 2_000_000L)
        var successCalled = false
        viewModel.updatePantryItem(updatedItem, onSuccess = { successCalled = true })
        testScheduler.advanceUntilIdle()

        coVerify { repository.updateItem(updatedItem) }
        assertNull(viewModel.editingItem.value)
        assertTrue(successCalled)
    }

    @Test
    fun updateQuantity_updatesInRepository() = runTest {
        val item = PantryItem(id = 5L, name = "Avocado", quantity = 3, expiryTimestamp = 1_000_000L)
        val viewModel = DashboardViewModel(repository, testDispatcher)

        viewModel.updateQuantity(item, delta = 1)
        testScheduler.advanceUntilIdle()

        coVerify { repository.updateQuantity(5L, 4) }
    }

    @Test
    fun updateQuantity_toZero_deletesItemFromRepository() = runTest {
        val item = PantryItem(id = 5L, name = "Avocado", quantity = 1, expiryTimestamp = 1_000_000L)
        val viewModel = DashboardViewModel(repository, testDispatcher)

        viewModel.updateQuantity(item, delta = -1)
        testScheduler.advanceUntilIdle()

        coVerify { repository.deleteItem(item) }
    }

    @Test
    fun deleteItem_callsRepositoryDelete() = runTest {
        val item = PantryItem(id = 7L, name = "Eggs", expiryTimestamp = 1_000_000L)
        val viewModel = DashboardViewModel(repository, testDispatcher)

        viewModel.deleteItem(item)
        testScheduler.advanceUntilIdle()

        coVerify { repository.deleteItem(item) }
    }

    @Test
    fun userProfile_defaultsToElenaRostovaWhenManagerNotProvided() = runTest {
        val viewModel = DashboardViewModel(repository, testDispatcher)
        val profile = viewModel.userProfile.value

        assertEquals("Elena Rostova", profile.name)
        assertEquals("elena.rostova@shelflife.local", profile.email)
        assertEquals("Zero-waste kitchen enthusiast • Household of 3", profile.bio)
    }

    @Test
    fun userProfile_delegatesUpdatesToUserProfileManager() = runTest {
        val mockManager = mockk<UserProfileManager>(relaxed = true)
        val profileFlow = MutableStateFlow(UserProfile(name = "Original Name"))
        val keyFlow = MutableStateFlow("")
        every { mockManager.profile } returns profileFlow
        every { mockManager.geminiApiKey } returns keyFlow

        val viewModel = DashboardViewModel(repository, testDispatcher, mockManager)
        assertEquals("Original Name", viewModel.userProfile.value.name)

        viewModel.updateUserProfile(
            name = "Chef Marco",
            email = "marco@kitchen.local",
            phone = "+1 555-9876",
            householdSize = "4 members",
            dietaryPreference = "Vegetarian",
            photoUri = "file:///avatar.jpg"
        )

        verify {
            mockManager.updateProfile(
                name = "Chef Marco",
                email = "marco@kitchen.local",
                phone = "+1 555-9876",
                bio = UserProfileManager.DEFAULT_BIO,
                householdSize = "4 members",
                dietaryPreference = "Vegetarian",
                photoUri = "file:///avatar.jpg"
            )
        }
    }
}
