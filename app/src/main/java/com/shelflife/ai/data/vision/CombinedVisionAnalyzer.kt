package com.shelflife.ai.data.vision

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "CombinedVisionAnalyzer"

/**
 * Combined ML Vision Engine integrating Google ML Kit Barcode Scanning,
 * OCR Expiration Date Recognition, and Smart Product Name Identification.
 *
 * Guarantees zero-crash camera analysis through synchronous background task execution
 * and safe lifecycle buffer release.
 */
class CombinedVisionAnalyzer(
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(BarcodeAnalyzer.DEFAULT_OPTIONS),
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
    private val onResult: ((ScanResult) -> Unit)? = null,
    private val debounceIntervalMs: Long = 1500L
) : ImageAnalysis.Analyzer, Closeable {

    private val _scanResults = MutableSharedFlow<ScanResult>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val scanResults: SharedFlow<ScanResult> = _scanResults.asSharedFlow()

    private val frameCounter = AtomicLong(0)

    @Volatile
    private var lastScannedBarcode: String? = null
    @Volatile
    private var lastScannedBarcodeTs: Long = 0L

    @Volatile
    private var lastDetectedExpiry: Long? = null
    @Volatile
    private var lastDetectedExpiryTs: Long = 0L

    @Volatile
    private var lastDetectedProductName: String? = null
    @Volatile
    private var lastDetectedProductTs: Long = 0L

    companion object {
        // Built-in smart lookup dictionary mapping common barcodes or keywords to product names & categories
        val KNOWN_PRODUCTS: Map<String, Pair<String, String>> = mapOf(
            "8901030000001" to Pair("Amul Pure Milk", "Dairy"),
            "8901233024888" to Pair("Britannia Whole Wheat Bread", "Bakery"),
            "012000000133" to Pair("Pepsi Zero Sugar", "Beverages"),
            "041789001221" to Pair("Organic Greek Yogurt", "Dairy"),
            "070042000125" to Pair("Fresh Farm Eggs (12-pack)", "Dairy"),
            "024100106852" to Pair("Kellogg's Corn Flakes", "Pantry Staples"),
            "038000358210" to Pair("Quaker Rolled Oats", "Pantry Staples"),
            "071117182004" to Pair("Barilla Spaghetti", "Pantry Staples"),
            "8901058852331" to Pair("Maggi 2-Minute Noodles", "Pantry Staples"),
            "8901725181220" to Pair("Aashirvaad Whole Wheat Atta", "Pantry Staples"),
            "8901491101837" to Pair("Tata Tea Gold", "Beverages"),
            "8901063012423" to Pair("Parle-G Biscuits", "Bakery")
        )

        // Common food product keywords to extract from packaging OCR text
        val PRODUCT_KEYWORD_RULES = listOf(
            listOf("MILK", "TONED MILK", "WHOLE MILK") to Pair("Fresh Milk", "Dairy"),
            listOf("YOGURT", "GREEK YOGURT", "CURD", "DAHI") to Pair("Greek Yogurt", "Dairy"),
            listOf("BUTTER", "SALTED BUTTER") to Pair("Butter", "Dairy"),
            listOf("CHEESE", "CHEDDAR", "MOZZARELLA", "PANEER") to Pair("Cheese", "Dairy"),
            listOf("BREAD", "SOURDOUGH", "WHOLE WHEAT BREAD") to Pair("Fresh Bread", "Bakery"),
            listOf("EGG", "EGGS", "FREE RANGE EGGS") to Pair("Farm Eggs", "Dairy"),
            listOf("OATS", "ROLLED OATS", "OATMEAL") to Pair("Rolled Oats", "Pantry Staples"),
            listOf("RICE", "BASMATI", "JASMINE RICE") to Pair("Basmati Rice", "Pantry Staples"),
            listOf("PASTA", "SPAGHETTI", "PENNE", "MACARONI") to Pair("Pasta", "Pantry Staples"),
            listOf("COFFEE", "ESPRESSO", "COLD BREW") to Pair("Roast Coffee", "Beverages"),
            listOf("TEA", "GREEN TEA", "CHAI") to Pair("Tea", "Beverages"),
            listOf("CHICKEN", "POULTRY", "CHICKEN BREAST") to Pair("Fresh Chicken", "Meat & Seafood"),
            listOf("SALMON", "TUNA", "FISH") to Pair("Fresh Fish", "Meat & Seafood"),
            listOf("APPLE", "APPLES", "HONEYCRISP") to Pair("Fresh Apples", "Produce"),
            listOf("BANANA", "BANANAS") to Pair("Fresh Bananas", "Produce"),
            listOf("TOMATO", "TOMATOES") to Pair("Fresh Tomatoes", "Produce"),
            listOf("SPINACH", "BABY SPINACH") to Pair("Fresh Spinach", "Produce"),
            listOf("AVOCADO", "AVOCADOS") to Pair("Ripe Avocado", "Produce"),
            listOf("JUICE", "ORANGE JUICE", "APPLE JUICE") to Pair("Fruit Juice", "Beverages")
        )
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentFrame = frameCounter.incrementAndGet()

        try {
            val mediaImage = imageProxy.image ?: return
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            // Step 1: Barcode Scan (fast, executed every frame)
            var barcodeFound = false
            try {
                val barcodeTask = barcodeScanner.process(inputImage)
                val barcodes = Tasks.await(barcodeTask, 400, TimeUnit.MILLISECONDS)
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        barcodeFound = true
                        val now = System.currentTimeMillis()
                        if (rawValue != lastScannedBarcode || (now - lastScannedBarcodeTs) > debounceIntervalMs) {
                            lastScannedBarcode = rawValue
                            lastScannedBarcodeTs = now

                            val known = KNOWN_PRODUCTS[rawValue]
                            if (known != null) {
                                emitResult(
                                    ScanResult.ProductDetected(
                                        productName = known.first,
                                        category = known.second,
                                        barcode = rawValue
                                    )
                                )
                            } else {
                                emitResult(
                                    ScanResult.BarcodeDetected(
                                        barcode = rawValue,
                                        format = barcode.format
                                    )
                                )
                            }
                        }
                        break
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Barcode processing note: ${e.message}")
            }

            // Step 2: OCR Text Recognition (run on alternating frames or if no barcode, to maximize battery & performance)
            if (!barcodeFound || currentFrame % 2L == 0L) {
                try {
                    val textTask = textRecognizer.process(inputImage)
                    val visionText = Tasks.await(textTask, 600, TimeUnit.MILLISECONDS)
                    processVisionText(visionText)
                } catch (e: Throwable) {
                    Log.d(TAG, "Text OCR note: ${e.message}")
                }
            }

        } catch (t: Throwable) {
            Log.w(TAG, "Frame analysis handled safely: ${t.message}")
        } finally {
            // Unconditionally close the current ImageProxy buffer before accepting next frame
            try {
                imageProxy.close()
            } catch (_: Throwable) {}
        }
    }

    private fun processVisionText(visionText: Text) {
        val fullText = visionText.text
        if (fullText.isBlank()) return

        // 1. Expiration date recognition
        val date = DateParser.parseExpiryDate(fullText)
        if (date != null) {
            val now = System.currentTimeMillis()
            if (date != lastDetectedExpiry || (now - lastDetectedExpiryTs) > debounceIntervalMs) {
                lastDetectedExpiry = date
                lastDetectedExpiryTs = now
                emitResult(ScanResult.ExpiryDateDetected(rawText = fullText, parsedTimestamp = date))
            }
        }

        // 2. Product keyword matching for product identification
        val upper = fullText.uppercase()
        for ((keywords, productInfo) in PRODUCT_KEYWORD_RULES) {
            if (keywords.any { upper.contains(it) }) {
                val now = System.currentTimeMillis()
                val detectedName = productInfo.first
                if (detectedName != lastDetectedProductName || (now - lastDetectedProductTs) > debounceIntervalMs) {
                    lastDetectedProductName = detectedName
                    lastDetectedProductTs = now
                    emitResult(
                        ScanResult.ProductDetected(
                            productName = detectedName,
                            category = productInfo.second,
                            expiryTimestamp = lastDetectedExpiry
                        )
                    )
                }
                break
            }
        }
    }

    private fun emitResult(result: ScanResult) {
        _scanResults.tryEmit(result)
        try {
            onResult?.invoke(result)
        } catch (t: Throwable) {
            Log.e(TAG, "Error invoking scan callback", t)
        }
    }

    override fun close() {
        try {
            barcodeScanner.close()
        } catch (_: Throwable) {}
        try {
            textRecognizer.close()
        } catch (_: Throwable) {}
    }
}
