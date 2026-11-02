package com.shelflife.ai.data.vision

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.Closeable

/**
 * CameraX [ImageAnalysis.Analyzer] that scans camera frames for product barcodes
 * using Google ML Kit Barcode Scanning.
 *
 * @param barcodeScanner The ML Kit BarcodeScanner instance.
 * @param onResult Optional direct callback for recognized results.
 * @param deduplicationIntervalMs Cooldown window in ms during which duplicate barcodes are ignored.
 */
class BarcodeAnalyzer(
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(DEFAULT_OPTIONS),
    private val onResult: ((ScanResult) -> Unit)? = null,
    private val deduplicationIntervalMs: Long = 2000L
) : ImageAnalysis.Analyzer, Closeable {

    companion object {
        val DEFAULT_OPTIONS: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    }

    private val _scanResults = MutableSharedFlow<ScanResult>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val scanResults: SharedFlow<ScanResult> = _scanResults.asSharedFlow()

    @Volatile
    private var lastScannedBarcode: String? = null

    @Volatile
    private var lastScannedTimestamp: Long = 0L

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
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    processBarcodes(barcodes)
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

    private fun processBarcodes(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            val rawValue = barcode.rawValue
            if (!rawValue.isNullOrBlank()) {
                if (shouldEmit(rawValue)) {
                    emitResult(ScanResult.BarcodeDetected(barcode = rawValue, format = barcode.format))
                    break // Emit first recognized valid barcode in this frame
                }
            }
        }
    }

    private fun shouldEmit(barcode: String, now: Long = System.currentTimeMillis()): Boolean {
        val elapsed = now - lastScannedTimestamp
        if (barcode == lastScannedBarcode && elapsed < deduplicationIntervalMs) {
            return false
        }
        lastScannedBarcode = barcode
        lastScannedTimestamp = now
        return true
    }

    private fun emitResult(result: ScanResult) {
        _scanResults.tryEmit(result)
        onResult?.invoke(result)
    }

    /**
     * Resets deduplication tracking to permit immediate re-scanning of the same barcode.
     */
    fun resetDeduplication() {
        lastScannedBarcode = null
        lastScannedTimestamp = 0L
    }

    override fun close() {
        barcodeScanner.close()
    }
}
