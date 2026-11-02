package com.shelflife.ai.presentation.chat

import app.cash.turbine.test
import com.shelflife.ai.data.ai.GeminiManager
import com.shelflife.ai.data.local.UserProfileManager
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
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var pantryRepository: PantryRepository
    private lateinit var geminiManager: GeminiManager
    private lateinit var userProfileManager: UserProfileManager

    private val pantryFlow = MutableStateFlow<List<PantryItem>>(emptyList())
    private val keyFlow = MutableStateFlow("test_gemini_api_key")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        pantryRepository = mockk(relaxed = true)
        geminiManager = mockk(relaxed = true)
        userProfileManager = mockk(relaxed = true)

        coEvery { pantryRepository.getAllItems() } returns pantryFlow
        every { userProfileManager.geminiApiKey } returns keyFlow
        every { geminiManager.hasApiKey() } returns true
        coEvery { geminiManager.chatWithPantry(any(), any(), any()) } returns Result.success("Mocked answer")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialState_hasWelcomeMessageAndKeyStatus() = runTest {
        val viewModel = ChatViewModel(pantryRepository, geminiManager, userProfileManager)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.messages.isNotEmpty())
            assertFalse(state.messages.first().isUser)
            assertTrue(state.hasApiKey)
        }
    }

    @Test
    fun sendMessage_callsGeminiAndAppendsAssistantResponse() = runTest {
        val now = System.currentTimeMillis()
        val item = PantryItem(
            id = 1L,
            name = "Organic Whole Milk",
            category = Category.DAIRY,
            expiryTimestamp = now + (24 * 3600 * 1000L),
            quantity = 1
        )
        pantryFlow.value = listOf(item)

        coEvery {
            geminiManager.chatWithPantry(
                userQuery = "What's in my pantry?",
                pantryItems = any(),
                chatHistory = any()
            )
        } returns Result.success("You have Organic Whole Milk expiring in 24 hours!")

        val viewModel = ChatViewModel(pantryRepository, geminiManager, userProfileManager)
        testScheduler.advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.sendMessage("What's in my pantry?")
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(3, state.messages.size) // Welcome + User + Assistant
            assertEquals("What's in my pantry?", state.messages[1].text)
            assertTrue(state.messages[1].isUser)
            assertEquals("You have Organic Whole Milk expiring in 24 hours!", state.messages[2].text)
            assertFalse(state.messages[2].isUser)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun clearChat_resetsMessages() = runTest {
        val viewModel = ChatViewModel(pantryRepository, geminiManager, userProfileManager)
        testScheduler.advanceUntilIdle()

        viewModel.sendMessage("Hello")
        testScheduler.advanceUntilIdle()

        viewModel.clearChat()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.messages.size)
            assertFalse(state.messages[0].isUser)
        }
    }
}
