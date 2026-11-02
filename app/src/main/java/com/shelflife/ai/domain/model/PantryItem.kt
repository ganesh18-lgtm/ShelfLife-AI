package com.shelflife.ai.domain.model

/**
 * Core domain representation of an item tracked in the smart pantry.
 *
 * @property id Unique identifier. Defaults to 0L for unpersisted items.
 * @property name Product or food name (e.g., "Whole Milk", "Avocado").
 * @property barcode Optional UPC/EAN barcode string extracted via ML Kit.
 * @property expiryTimestamp Expiration date in epoch milliseconds.
 * @property category Food classification [Category].
 * @property quantity Item count currently stored in pantry (>= 0).
 * @property addedTimestamp Timestamp in epoch milliseconds when item was recorded.
 * @property imageUri Local photo file URI or gallery content URI of the item.
 * @property mfgDateTimestamp Manufacturing date in epoch milliseconds.
 * @property expiryMode Expiration calculation mode: "BEST_BEFORE", "USE_BY", or "EXACT".
 */
data class PantryItem(
    val id: Long = 0L,
    val name: String,
    val barcode: String? = null,
    val expiryTimestamp: Long,
    val category: Category = Category.OTHER,
    val quantity: Int = 1,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val mfgDateTimestamp: Long? = null,
    val expiryMode: String = "EXACT"
) {
    /**
     * Dynamically calculates urgency relative to an arbitrary reference time.
     */
    fun getUrgency(now: Long = System.currentTimeMillis()): ExpiryUrgency =
        calculateExpiryUrgency(expiryTimestamp, now)

    /**
     * Urgency relative to the current system time.
     */
    val urgency: ExpiryUrgency
        get() = getUrgency()

    /**
     * True if the item is critical or expired.
     */
    val isExpiringSoon: Boolean
        get() = urgency.isCriticalOrExpired

    /**
     * True if the item is in stock (quantity > 0).
     */
    val isInStock: Boolean
        get() = quantity > 0
}
