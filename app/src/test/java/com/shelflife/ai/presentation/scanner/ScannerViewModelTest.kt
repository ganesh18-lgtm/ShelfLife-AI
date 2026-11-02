package com.shelflife.ai.presentation.scanner

import app.cash.turbine.test
import com.google.mlkit.vision.barcode.common.Barcode
import com.shelflife.ai.data.ai.GeminiManager
import com.shelflife.ai.data.vision.ScanResult
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PantryRepository
    private lateinit var geminiManager: GeminiManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        geminiManager = mockk(relaxed = true)
        every { geminiManager.hasApiKey() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onScanResult_barcodeDetected_opensBottomSheetAndPreFillsExistingItem() = runTest {
        val existingItem = PantryItem(
            id = 42L,
            name = "Organic Whole Milk",
            barcode = "0123456789",
            category = Category.DAIRY,
            expiryTimestamp = 1_000_000L
        )
        coEvery { repository.getItemByBarcode("0123456789") } returns existingItem

        val viewModel = ScannerViewModel(repository, geminiManager)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(false, initial.isBottomSheetVisible)

            viewModel.onScanResultReceived(ScanResult.BarcodeDetected("0123456789", Barcode.FORMAT_EAN_13))
            testScheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertTrue(updated.isBottomSheetVisible)
            assertEquals("0123456789", updated.scannedBarcode)
            assertEquals("Organic Whole Milk", updated.productName)
            assertEquals(Category.DAIRY, updated.selectedCategory)
        }
    }

    @Test
    fun onScanResult_expiryDateDetected_opensBottomSheetAndSetsTimestamp() = runTest {
        val targetTimestamp = 1780000000000L
        val viewModel = ScannerViewModel(repository, geminiManager)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onScanResultReceived(ScanResult.ExpiryDateDetected("EXP 25/12/2026", targetTimestamp))
            testScheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertTrue(updated.isBottomSheetVisible)
            assertEquals(targetTimestamp, updated.scannedExpiryTimestamp)
            assertEquals("EXP 25/12/2026", updated.scannedRawText)
        }
    }

    @Test
    fun saveItem_insertsIntoRepositoryAndTriggersCallback() = runTest {
        coEvery { repository.insertItem(any()) } returns 101L

        val viewModel = ScannerViewModel(repository, geminiManager)
        viewModel.onProductNameChanged("Fresh Strawberries")
        viewModel.onCategorySelected(Category.PRODUCE)
        viewModel.onQuantityChanged(2) // 1 + 2 = 3

        var callbackTriggered = false
        viewModel.saveItem {
            callbackTriggered = true
        }

        testScheduler.advanceUntilIdle()

        assertTrue(callbackTriggered)
        coVerify {
            repository.insertItem(match {
                it.name == "Fresh Strawberries" &&
                        it.category == Category.PRODUCE &&
                        it.quantity == 3
            })
        }
    }

    @Test
    fun saveItem_withExplicitParameters_persistsAllValuesDirectly() = runTest {
        coEvery { repository.insertItem(any()) } returns 202L

        val viewModel = ScannerViewModel(repository, geminiManager)
        var callbackTriggered = false

        viewModel.saveItem(
            name = "Organic Avocados",
            category = Category.PRODUCE,
            expiryTimestamp = 1_800_000_000_000L,
            barcode = "8901234567890",
            quantity = 4,
            onSaved = { callbackTriggered = true }
        )

        testScheduler.advanceUntilIdle()

        assertTrue(callbackTriggered)
        coVerify {
            repository.insertItem(match {
                it.name == "Organic Avocados" &&
                        it.category == Category.PRODUCE &&
                        it.expiryTimestamp == 1_800_000_000_000L &&
                        it.barcode == "8901234567890" &&
                        it.quantity == 4
            })
        }
    }

    @Test
    fun onScanResult_whenBottomSheetVisible_suppressesDuplicateScanSpam() = runTest {
        val viewModel = ScannerViewModel(repository, geminiManager)
        viewModel.onProductNameChanged("Current Active Item")
        viewModel.openManualAdd() // sets isBottomSheetVisible = true

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isBottomSheetVisible)
            assertEquals("Current Active Item", state.productName)

            // Emit a new product detection while user is editing
            viewModel.onScanResultReceived(ScanResult.ProductDetected("New Incoming Item", "Dairy"))
            testScheduler.advanceUntilIdle()

            // State should remain unchanged and NOT overwrite user's active item
            expectNoEvents()
            assertEquals("Current Active Item", viewModel.uiState.value.productName)
        }
    }
}
