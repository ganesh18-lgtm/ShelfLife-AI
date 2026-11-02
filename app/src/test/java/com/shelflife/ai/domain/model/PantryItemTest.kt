package com.shelflife.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryItemTest {

    private val now = 1_000_000_000L

    @Test
    fun evaluatesDynamicUrgencyWithInjectedClock() {
        val criticalItem = PantryItem(
            name = "Milk",
            expiryTimestamp = now + 10_000L,
            category = Category.DAIRY
        )
        assertEquals(ExpiryUrgency.CRITICAL, criticalItem.getUrgency(now))
        assertTrue(criticalItem.getUrgency(now).isCriticalOrExpired)
    }

    @Test
    fun verifiesStockStatus() {
        val inStock = PantryItem(name = "Eggs", expiryTimestamp = now + 100_000L, quantity = 6)
        val outOfStock = PantryItem(name = "Eggs", expiryTimestamp = now + 100_000L, quantity = 0)

        assertTrue(inStock.isInStock)
        assertFalse(outOfStock.isInStock)
    }
}
