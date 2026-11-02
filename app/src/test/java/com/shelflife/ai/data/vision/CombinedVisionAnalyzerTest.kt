package com.shelflife.ai.data.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedVisionAnalyzerTest {

    @Test
    fun knownProducts_containsStandardGroceries() {
        val milk = CombinedVisionAnalyzer.KNOWN_PRODUCTS["8901030000001"]
        assertNotNull(milk)
        assertEquals("Amul Pure Milk", milk?.first)
        assertEquals("Dairy", milk?.second)

        val bread = CombinedVisionAnalyzer.KNOWN_PRODUCTS["8901233024888"]
        assertNotNull(bread)
        assertEquals("Britannia Whole Wheat Bread", bread?.first)
        assertEquals("Bakery", bread?.second)
    }

    @Test
    fun knownProducts_unknownBarcode_returnsNull() {
        assertNull(CombinedVisionAnalyzer.KNOWN_PRODUCTS["9999999999999"])
    }

    @Test
    fun productKeywordRules_identifiesStandardGroceryKeywords() {
        val rules = CombinedVisionAnalyzer.PRODUCT_KEYWORD_RULES

        // Test Milk
        val milkMatch = rules.firstOrNull { (keywords, _) ->
            keywords.any { "PASTEURIZED HOMOGENIZED MILK 1000ML".contains(it) }
        }
        assertNotNull(milkMatch)
        assertEquals("Fresh Milk", milkMatch?.second?.first)
        assertEquals("Dairy", milkMatch?.second?.second)

        // Test Sourdough Bread
        val breadMatch = rules.firstOrNull { (keywords, _) ->
            keywords.any { "ARTISAN SOURDOUGH BREAD".contains(it) }
        }
        assertNotNull(breadMatch)
        assertEquals("Fresh Bread", breadMatch?.second?.first)
        assertEquals("Bakery", breadMatch?.second?.second)

        // Test Eggs
        val eggsMatch = rules.firstOrNull { (keywords, _) ->
            keywords.any { "GRADE A ORGANIC EGGS 12 PACK".contains(it) }
        }
        assertNotNull(eggsMatch)
        assertEquals("Farm Eggs", eggsMatch?.second?.first)

        // Test Chicken
        val chickenMatch = rules.firstOrNull { (keywords, _) ->
            keywords.any { "FRESH CHICKEN BREAST 500G".contains(it) }
        }
        assertNotNull(chickenMatch)
        assertEquals("Fresh Chicken", chickenMatch?.second?.first)
        assertEquals("Meat & Seafood", chickenMatch?.second?.second)
    }

    @Test
    fun textCombinedWithExpiryDateParser_extractsBothProductAndDate() {
        val ocrSample = """
            FRESH WHOLE MILK
            NET WT 1L
            BEST BEFORE 28/11/2026
            KEEP REFRIGERATED
        """.trimIndent()

        // Test date parsing
        val expiry = DateParser.parseExpiryDate(ocrSample)
        assertNotNull(expiry)

        // Test product matching
        val upper = ocrSample.uppercase()
        val match = CombinedVisionAnalyzer.PRODUCT_KEYWORD_RULES.firstOrNull { (keywords, _) ->
            keywords.any { upper.contains(it) }
        }
        assertNotNull(match)
        assertEquals("Fresh Milk", match?.second?.first)
    }
}
