package com.shelflife.ai.domain.model

/**
 * Expiration urgency classifications for visual coding and notification scheduling.
 *
 * - [EXPIRED]: Past expiration date (< 0 ms).
 * - [CRITICAL]: Expiring in < 48 hours (Red visual theme).
 * - [WARNING]: Expiring in < 5 days (Yellow visual theme).
 * - [FRESH]: Fresh food with >= 5 days remaining (Green visual theme).
 */
enum class ExpiryUrgency {
    EXPIRED,
    CRITICAL,
    WARNING,
    FRESH;

    /**
     * True if the item is either currently expired or expiring within 48 hours.
     */
    val isCriticalOrExpired: Boolean
        get() = this == CRITICAL || this == EXPIRED

    /**
     * True if the item requires user attention (expired, critical, or warning).
     */
    val isUrgent: Boolean
        get() = this != FRESH

    companion object {
        const val FORTY_EIGHT_HOURS_MILLIS: Long = 48L * 60 * 60 * 1000L // 172,800,000 ms
        const val FIVE_DAYS_MILLIS: Long = 5L * 24 * 60 * 60 * 1000L       // 432,000,000 ms
    }
}

/**
 * Millisecond constants for shelf-life urgency thresholds.
 */
const val FORTY_EIGHT_HOURS_MILLIS: Long = ExpiryUrgency.FORTY_EIGHT_HOURS_MILLIS
const val FIVE_DAYS_MILLIS: Long = ExpiryUrgency.FIVE_DAYS_MILLIS

/**
 * Pure domain function to calculate the shelf-life urgency of an item given its
 * expiration timestamp and an optional reference timestamp (defaults to system time).
 *
 * @param expiryTimestamp Expiration timestamp in epoch milliseconds.
 * @param now Reference timestamp in epoch milliseconds (defaults to System.currentTimeMillis()).
 * @return Evaluated [ExpiryUrgency].
 */
fun calculateExpiryUrgency(
    expiryTimestamp: Long,
    now: Long = System.currentTimeMillis()
): ExpiryUrgency {
    val diffMillis = expiryTimestamp - now

    return when {
        diffMillis < 0L -> ExpiryUrgency.EXPIRED
        diffMillis < FORTY_EIGHT_HOURS_MILLIS -> ExpiryUrgency.CRITICAL
        diffMillis < FIVE_DAYS_MILLIS -> ExpiryUrgency.WARNING
        else -> ExpiryUrgency.FRESH
    }
}
