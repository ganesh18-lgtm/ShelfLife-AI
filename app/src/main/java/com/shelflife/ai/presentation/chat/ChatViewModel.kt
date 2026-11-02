package com.shelflife.ai.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelflife.ai.data.ai.GeminiManager
import com.shelflife.ai.data.local.UserProfileManager
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Hello! I'm your ShelfLife AI Assistant powered by Gemini 1.5 Flash. I have full real-time access to your Room pantry database. Ask me anything about what's in stock, items expiring soon, or zero-waste recipe ideas!",
            isUser = false
        )
    ),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val hasApiKey: Boolean = false,
    val totalPantryItemsCount: Int = 0,
    val expiringItemsCount: Int = 0
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val pantryRepository: PantryRepository,
    private val geminiManager: GeminiManager,
    private val userProfileManager: UserProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(hasApiKey = geminiManager.hasApiKey())
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentPantry: List<PantryItem> = emptyList()

    init {
        // Observe live Room pantry database
        viewModelScope.launch {
            pantryRepository.getAllItems().collectLatest { items ->
                currentPantry = items
                val expiring = items.count { it.urgency.isUrgent }
                _uiState.update {
                    it.copy(
                        totalPantryItemsCount = items.size,
                        expiringItemsCount = expiring
                    )
                }
            }
        }

        // Observe API key changes
        viewModelScope.launch {
            userProfileManager.geminiApiKey.collectLatest { key ->
                _uiState.update { it.copy(hasApiKey = key.isNotBlank()) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun saveApiKey(key: String) {
        userProfileManager.setGeminiApiKey(key)
        _uiState.update { it.copy(hasApiKey = key.isNotBlank()) }
    }

    fun sendMessage(query: String? = null) {
        val messageText = (query ?: _uiState.value.inputText).trim()
        if (messageText.isBlank()) return

        val userMessage = ChatMessage(text = messageText, isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            if (!geminiManager.hasApiKey()) {
                val fallbackAnswer = ChatMessage(
                    text = "Gemini API key is required to analyze your database with AI. Please enter your free key above or in Settings.",
                    isUser = false
                )
                _uiState.update { it.copy(messages = it.messages + fallbackAnswer, isLoading = false) }
                return@launch
            }

            val history = _uiState.value.messages.map { it.text to it.isUser }
            val result = geminiManager.chatWithPantry(
                userQuery = messageText,
                pantryItems = currentPantry,
                chatHistory = history
            )

            result.onSuccess { answer ->
                val assistantMessage = ChatMessage(text = answer, isUser = false)
                _uiState.update {
                    it.copy(messages = it.messages + assistantMessage, isLoading = false)
                }
            }.onFailure { err ->
                val errorMessage = ChatMessage(
                    text = "Sorry, I encountered an issue contacting Gemini: ${err.message}. Please check your internet connection or API key.",
                    isUser = false
                )
                _uiState.update {
                    it.copy(messages = it.messages + errorMessage, isLoading = false)
                }
            }
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        text = "Chat history cleared. How can I help you manage your pantry today?",
                        isUser = false
                    )
                )
            )
        }
    }
}
