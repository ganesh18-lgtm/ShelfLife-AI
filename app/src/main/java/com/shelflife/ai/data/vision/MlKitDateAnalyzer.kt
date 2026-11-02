package com.shelflife.ai.data.vision

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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

/**
 * CameraX [ImageAnalysis.Analyzer] that extracts expiration dates from camera frames
 * using Google ML Kit Text Recognition and [DateParser].
 *
 * @param textRecognizer The ML Kit TextRecognizer instance (defaults to on-device Latin model).
 * @param onResult Optional direct callback for recognized results.
 * @param debounceIntervalMs Minimum interval in milliseconds between duplicate date emissions.
 */
class MlKitDateAnalyzer(
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
    private val onResult: ((ScanResult) -> Unit)? = null,
    private val debounceIntervalMs: Long = 1500L
) : ImageAnalysis.Analyzer, Closeable {

    private val _scanResults = MutableSharedFlow<ScanResult>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val scanResults: SharedFlow<ScanResult> = _scanResults.asSharedFlow()

    @Volatile
    private var lastEmittedTimestamp: Long = 0L

    @Volatile
    private var lastParsedDate: Long? = null

    @Volatile
    private var lastRawText: String? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        try {
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    processVisionText(visionText)
                }
                .addOnFailureListener { throwable ->
                    emitResult(ScanResult.ScanError(throwable))
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            emitResult(ScanResult.ScanError(e))
            imageProxy.close()
        }
    }

    private fun processVisionText(visionText: Text) {
        // Strategy 1: Iterate through text blocks (preserves spatial multi-line context)
        for (block in visionText.textBlocks) {
            val date = DateParser.parseExpiryDate(block.text)
            if (date != null) {
                if (shouldEmit(date, block.text)) {
                    emitResult(ScanResult.ExpiryDateDetected(rawText = block.text, parsedTimestamp = date))
                }
                return
            }

            // Strategy 2: Check individual lines within the block
            for (line in block.lines) {
                val lineDate = DateParser.parseExpiryDate(line.text)
                if (lineDate != null) {
                    if (shouldEmit(lineDate, line.text)) {
                        emitResult(ScanResult.ExpiryDateDetected(rawText = line.text, parsedTimestamp = lineDate))
                    }
                    return
                }
            }
        }

        // Strategy 3: Fall back to entire OCR text if blocks were fragmented
        if (visionText.text.isNotBlank()) {
            val fallbackDate = DateParser.parseExpiryDate(visionText.text)
            if (fallbackDate != null && shouldEmit(fallbackDate, visionText.text)) {
                emitResult(ScanResult.ExpiryDateDetected(rawText = visionText.text, parsedTimestamp = fallbackDate))
            }
        }
    }

    private fun shouldEmit(parsedDate: Long, rawText: String, now: Long = System.currentTimeMillis()): Boolean {
        val elapsed = now - lastEmittedTimestamp
        if (parsedDate == lastParsedDate && elapsed < debounceIntervalMs) {
            return false
        }
        lastEmittedTimestamp = now
        lastParsedDate = parsedDate
        lastRawText = rawText
        return true
    }

    private fun emitResult(result: ScanResult) {
        _scanResults.tryEmit(result)
        onResult?.invoke(result)
    }

    /**
     * Resets throttling cache to allow immediate detection of subsequent scans.
     */
    fun resetThrottle() {
        lastEmittedTimestamp = 0L
        lastParsedDate = null
        lastRawText = null
    }

    override fun close() {
        textRecognizer.close()
    }
}
