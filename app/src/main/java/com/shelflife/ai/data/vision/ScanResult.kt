package com.shelflife.ai.data.vision

/**
 * Result events emitted by ML Kit camera analyzers.
 */
sealed interface ScanResult {
    /**
     * Emitted when a product barcode (UPC-A, EAN-13, QR, etc.) is recognized.
     */
    data class BarcodeDetected(
        val barcode: String,
        val format: Int
    ) : ScanResult

    /**
     * Emitted when an expiration date pattern is parsed from OCR text.
     */
    data class ExpiryDateDetected(
        val rawText: String,
        val parsedTimestamp: Long
    ) : ScanResult

    /**
     * Emitted when a product name and category are detected from OCR keywords or barcode dictionary.
     */
    data class ProductDetected(
        val productName: String,
        val category: String,
        val barcode: String? = null,
        val expiryTimestamp: Long? = null
    ) : ScanResult

    /**
     * Emitted when vision analysis encounters an unrecoverable processing error.
     */
    data class ScanError(
        val throwable: Throwable
    ) : ScanResult
}
