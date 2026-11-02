package com.shelflife.ai.domain.model

/**
 * Standard food classifications for inventory categorization and zero-waste recipe sorting.
 */
enum class Category(val displayName: String) {
    PRODUCE("Produce"),
    DAIRY("Dairy"),
    MEAT("Meat & Seafood"),
    PANTRY("Pantry Staples"),
    BAKERY("Bakery"),
    BEVERAGES("Beverages"),
    FROZEN("Frozen Foods"),
    OTHER("Other");

    companion object {
        /**
         * Resolves a Category enum from a string representation (matching either enum name or displayName),
         * case-insensitively. Falls back to OTHER if null, blank, or unrecognized.
         */
        fun fromString(value: String?): Category {
            if (value.isNullOrBlank()) return OTHER
            val trimmed = value.trim()
            if (trimmed.equals("Vegetables", ignoreCase = true) || trimmed.equals("Veg", ignoreCase = true)) {
                return PRODUCE
            }
            return entries.find { it.name.equals(trimmed, ignoreCase = true) }
                ?: entries.find { it.displayName.equals(trimmed, ignoreCase = true) }
                ?: OTHER
        }
    }
}
