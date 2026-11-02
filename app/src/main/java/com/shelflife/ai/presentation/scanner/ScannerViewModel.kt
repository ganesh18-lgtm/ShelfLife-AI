package com.shelflife.ai.presentation.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shelflife.ai.data.ai.GeminiManager
import com.shelflife.ai.data.vision.DateParser
import com.shelflife.ai.data.vision.ScanResult
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ScannerUiState(
    val scannedBarcode: String? = null,
    val scannedExpiryTimestamp: Long? = null,
    val scannedRawText: String? = null,
    val productName: String = "",
    val selectedCategory: Category = Category.PANTRY,
    val quantity: Int = 1,
    val isBottomSheetVisible: Boolean = false,
    val isSaving: Boolean = false,
    val isAnalyzingWithGemini: Boolean = false,
    val userFeedbackMessage: String? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: PantryRepository,
    private val geminiManager: GeminiManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _scanEvents = MutableSharedFlow<ScanResult>(extraBufferCapacity = 64)
    val scanEvents: SharedFlow<ScanResult> = _scanEvents.asSharedFlow()

    fun onScanResultReceived(result: ScanResult) {
        viewModelScope.launch {
            _scanEvents.emit(result)
            handleScanResult(result)
        }
    }

    private suspend fun handleScanResult(result: ScanResult) {
        when (result) {
            is ScanResult.ProductDetected -> {
                if (_uiState.value.isBottomSheetVisible) {
                    return
                }
                _uiState.update { current ->
                    current.copy(
                        productName = result.productName,
                        selectedCategory = Category.fromString(result.category),
                        scannedBarcode = result.barcode ?: current.scannedBarcode,
                        scannedExpiryTimestamp = result.expiryTimestamp ?: current.scannedExpiryTimestamp,
                        isBottomSheetVisible = true,
                        userFeedbackMessage = "Identified: ${result.productName}"
                    )
                }
            }

            is ScanResult.BarcodeDetected -> {
                if (_uiState.value.isBottomSheetVisible) {
                    return
                }

                val existing = repository.getItemByBarcode(result.barcode)
                _uiState.update { current ->
                    current.copy(
                        scannedBarcode = result.barcode,
                        productName = existing?.name ?: current.productName.ifBlank { "Item ${result.barcode.takeLast(4)}" },
                        selectedCategory = existing?.category ?: current.selectedCategory,
                        isBottomSheetVisible = true,
                        userFeedbackMessage = "Barcode scanned: ${result.barcode}"
                    )
                }
            }

            is ScanResult.ExpiryDateDetected -> {
                if (_uiState.value.isBottomSheetVisible) {
                    return
                }

                _uiState.update { current ->
                    current.copy(
                        scannedExpiryTimestamp = result.parsedTimestamp,
                        scannedRawText = result.rawText,
                        isBottomSheetVisible = true,
                        userFeedbackMessage = "Expiry date detected: ${DateParser.formatDisplayDate(result.parsedTimestamp)}"
                    )
                }
            }

            is ScanResult.ScanError -> {
                _uiState.update { current ->
                    current.copy(userFeedbackMessage = "Vision scan error: ${result.throwable.localizedMessage}")
                }
            }
        }
    }

    /**
     * Uses Gemini 1.5 Flash Vision to analyze the camera image.
     */
    fun analyzeBitmapWithGemini(bitmap: Bitmap) {
        if (!geminiManager.hasApiKey()) {
            _uiState.update {
                it.copy(userFeedbackMessage = "Gemini API key not configured. Add your free key in Settings or Chat.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingWithGemini = true, userFeedbackMessage = "Analyzing item with Gemini 1.5 Flash...") }
            val result = geminiManager.analyzeFoodImage(bitmap)
            result.onSuccess { analysis ->
                val oneDayMs = 24L * 3600 * 1000L
                val calculatedExpiry = System.currentTimeMillis() + (analysis.estimatedDaysToExpiry.toLong() * oneDayMs)
                _uiState.update { current ->
                    current.copy(
                        isAnalyzingWithGemini = false,
                        productName = analysis.productName,
                        selectedCategory = analysis.category,
                        scannedExpiryTimestamp = calculatedExpiry,
                        isBottomSheetVisible = true,
                        userFeedbackMessage = "Gemini identified: ${analysis.productName} (Expires in ~${analysis.estimatedDaysToExpiry} days)"
                    )
                }
            }.onFailure { err ->
                _uiState.update { current ->
                    current.copy(
                        isAnalyzingWithGemini = false,
                        userFeedbackMessage = "Gemini analysis error: ${err.message}"
                    )
                }
            }
        }
    }

    fun hasGeminiApiKey(): Boolean = geminiManager.hasApiKey()

    fun onProductNameChanged(name: String) {
        _uiState.update { it.copy(productName = name) }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onQuantityChanged(delta: Int) {
        _uiState.update { current ->
            val newQty = (current.quantity + delta).coerceAtLeast(1)
            current.copy(quantity = newQty)
        }
    }

    fun onExpiryDateSet(timestamp: Long) {
        _uiState.update { it.copy(scannedExpiryTimestamp = timestamp) }
    }

    fun openManualAdd() {
        _uiState.update { current ->
            current.copy(
                isBottomSheetVisible = true,
                scannedExpiryTimestamp = current.scannedExpiryTimestamp ?: (System.currentTimeMillis() + (7L * 24 * 3600 * 1000L))
            )
        }
    }

    fun dismissBottomSheet() {
        _uiState.update { it.copy(isBottomSheetVisible = false) }
    }

    fun saveItem(
        name: String? = null,
        category: Category? = null,
        expiryTimestamp: Long? = null,
        barcode: String? = null,
        quantity: Int? = null,
        imageUri: String? = null,
        mfgDateTimestamp: Long? = null,
        expiryMode: String = "EXACT",
        onSaved: () -> Unit
    ) {
        val state = _uiState.value
        val finalName = (name ?: state.productName).trim().ifBlank { "Pantry Item" }
        val finalCategory = category ?: state.selectedCategory
        val finalExpiry = expiryTimestamp ?: state.scannedExpiryTimestamp ?: (System.currentTimeMillis() + (7L * 24 * 3600 * 1000L))
        val finalBarcode = barcode ?: state.scannedBarcode
        val finalQuantity = (quantity ?: state.quantity).coerceAtLeast(1)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val item = PantryItem(
                name = finalName,
                barcode = finalBarcode,
                expiryTimestamp = finalExpiry,
                category = finalCategory,
                quantity = finalQuantity,
                imageUri = imageUri,
                mfgDateTimestamp = mfgDateTimestamp,
                expiryMode = expiryMode
            )
            repository.insertItem(item)

            withContext(Dispatchers.Main) {
                _uiState.update {
                    ScannerUiState(
                        userFeedbackMessage = "Saved $finalName to pantry!"
                    )
                }
                onSaved()
            }
        }
    }

    fun consumeFeedback() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }
}
