package com.shelflife.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryUrgencyTest {

    private val now = 1_000_000_000L

    @Test
    fun expiredWhenTimestampIsInThePast() {
        assertEquals(ExpiryUrgency.EXPIRED, calculateExpiryUrgency(now - 1, now))
        assertEquals(ExpiryUrgency.EXPIRED, calculateExpiryUrgency(now - 100_000, now))
    }

    @Test
    fun criticalWhenDiffIsZeroOrUnder48Hours() {
        assertEquals(ExpiryUrgency.CRITICAL, calculateExpiryUrgency(now, now))
        assertEquals(ExpiryUrgency.CRITICAL, calculateExpiryUrgency(now + 1, now))
        assertEquals(ExpiryUrgency.CRITICAL, calculateExpiryUrgency(now + FORTY_EIGHT_HOURS_MILLIS - 1, now))
    }

    @Test
    fun warningWhenDiffIsExactly48HoursOrUnder5Days() {
        assertEquals(ExpiryUrgency.WARNING, calculateExpiryUrgency(now + FORTY_EIGHT_HOURS_MILLIS, now))
        assertEquals(ExpiryUrgency.WARNING, calculateExpiryUrgency(now + FIVE_DAYS_MILLIS - 1, now))
    }

    @Test
    fun freshWhenDiffIsExactly5DaysOrMore() {
        assertEquals(ExpiryUrgency.FRESH, calculateExpiryUrgency(now + FIVE_DAYS_MILLIS, now))
        assertEquals(ExpiryUrgency.FRESH, calculateExpiryUrgency(now + FIVE_DAYS_MILLIS + 10_000, now))
    }

    @Test
    fun helperProperties() {
        assertTrue(ExpiryUrgency.EXPIRED.isCriticalOrExpired)
        assertTrue(ExpiryUrgency.CRITICAL.isCriticalOrExpired)
        assertFalse(ExpiryUrgency.WARNING.isCriticalOrExpired)
        assertFalse(ExpiryUrgency.FRESH.isCriticalOrExpired)

        assertTrue(ExpiryUrgency.EXPIRED.isUrgent)
        assertTrue(ExpiryUrgency.CRITICAL.isUrgent)
        assertTrue(ExpiryUrgency.WARNING.isUrgent)
        assertFalse(ExpiryUrgency.FRESH.isUrgent)
    }
}
