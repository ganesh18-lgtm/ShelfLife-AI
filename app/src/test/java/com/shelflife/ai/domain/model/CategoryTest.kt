package com.shelflife.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTest {

    @Test
    fun resolvesStandardEnumNamesCaseInsensitively() {
        assertEquals(Category.DAIRY, Category.fromString("DAIRY"))
        assertEquals(Category.DAIRY, Category.fromString("dairy"))
        assertEquals(Category.PRODUCE, Category.fromString("Produce"))
        assertEquals(Category.MEAT, Category.fromString("MEAT"))
    }

    @Test
    fun resolvesDisplayNames() {
        assertEquals(Category.MEAT, Category.fromString("Meat & Seafood"))
        assertEquals(Category.PANTRY, Category.fromString("Pantry Staples"))
        assertEquals(Category.FROZEN, Category.fromString("Frozen Foods"))
    }

    @Test
    fun fallsBackToOtherForUnknownOrNull() {
        assertEquals(Category.OTHER, Category.fromString(null))
        assertEquals(Category.OTHER, Category.fromString(""))
        assertEquals(Category.OTHER, Category.fromString("   "))
        assertEquals(Category.OTHER, Category.fromString("UNKNOWN_FOOD"))
    }
}
