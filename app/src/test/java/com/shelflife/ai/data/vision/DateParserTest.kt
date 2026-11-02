package com.shelflife.ai.data.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Comprehensive automated unit test suite for [DateParser].
 * Runs entirely on the local JVM in milliseconds without Android framework dependencies.
 */
class DateParserTest {

    // Reference time: 2026-10-01T00:00:00Z
    private val fixedNow = LocalDate.of(2026, 10, 1)
        .atStartOfDay()
        .atZone(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    private fun expectedEndOfDay(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atTime(23, 59, 59, 999_000_000)
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }

    private fun expectedEndOfMonth(year: Int, month: Int): Long {
        return YearMonth.of(year, month)
            .atEndOfMonth()
            .atTime(23, 59, 59, 999_000_000)
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }

    // =========================================================================
    // 1. EXP Prefix Variations (DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, 2-digit YY)
    // =========================================================================

    @Test
    fun parseExpiryDate_expSlashDmy_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("EXP 24/10/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_expColonSlashDmy_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("EXP: 24/10/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_expDotDotDmy_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("EXP. 24.10.2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_expHyphenDmy_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("EXP 24-10-2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_expTwoDigitYear_expandsToCentury() {
        val result = DateParser.parseExpiryDate("EXP 24/10/26", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_expiryDateFullLabel_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("EXPIRY DATE: 31/12/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 12, 31), result)
    }

    // =========================================================================
    // 2. Best Before & BB Variations (MM/YY, MM/YYYY)
    // =========================================================================

    @Test
    fun parseExpiryDate_bestBeforeMmYy_returnsEndOfMonth() {
        val result = DateParser.parseExpiryDate("BEST BEFORE 12/26", fixedNow)
        assertEquals(expectedEndOfMonth(2026, 12), result)
    }

    @Test
    fun parseExpiryDate_bestBeforeMmYyyy_returnsEndOfMonth() {
        val result = DateParser.parseExpiryDate("BEST BEFORE 08/2027", fixedNow)
        assertEquals(expectedEndOfMonth(2027, 8), result)
    }

    @Test
    fun parseExpiryDate_bbMmYy_returnsEndOfMonth() {
        val result = DateParser.parseExpiryDate("BB 05/27", fixedNow)
        assertEquals(expectedEndOfMonth(2027, 5), result)
    }

    @Test
    fun parseExpiryDate_dottedBbMmYyyy_returnsEndOfMonth() {
        val result = DateParser.parseExpiryDate("B.B. 08/2026", fixedNow)
        assertEquals(expectedEndOfMonth(2026, 8), result)
    }

    @Test
    fun parseExpiryDate_dottedBbMmYy_returnsEndOfMonth() {
        val result = DateParser.parseExpiryDate("B.B. 12/26", fixedNow)
        assertEquals(expectedEndOfMonth(2026, 12), result)
    }

    @Test
    fun parseExpiryDate_bestBeforeEnd_returnsEndOfMonth() {
        val result = DateParser.parseExpiryDate("BEST BEFORE END: 10/25", fixedNow)
        assertEquals(expectedEndOfMonth(2025, 10), result)
    }

    @Test
    fun parseExpiryDate_useBy_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("USE BY 15/05/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 5, 15), result)
    }

    // =========================================================================
    // 3. ISO 8601 Formats (YYYY-MM-DD, YYYY/MM/DD, YYYY.MM.DD)
    // =========================================================================

    @Test
    fun parseExpiryDate_iso8601Hyphen_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("2026-10-24", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_iso8601WithPrefix_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("EXP: 2026-10-24", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_iso8601Slash_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("2026/10/24", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    // =========================================================================
    // 4. Textual Months (DD MMM YYYY, MMM DD YYYY, MMM YYYY)
    // =========================================================================

    @Test
    fun parseExpiryDate_textMonthDdMmmYyyy_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("24 OCT 2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_textMonthHyphenDdMmmYy_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("05-NOV-25", fixedNow)
        assertEquals(expectedEndOfDay(2025, 11, 5), result)
    }

    @Test
    fun parseExpiryDate_textMonthNoSeparator_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("24OCT2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_textMonthOrdinalSuffix_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("24th October 2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_textMonthMmmDdYyyy_returnsCorrectTimestamp() {
        val result = DateParser.parseExpiryDate("OCT 24, 2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_textMonthOnly_returnsEndOfMonth() {
        val result = DateParser.parseExpiryDate("BEST BEFORE DEC 2026", fixedNow)
        assertEquals(expectedEndOfMonth(2026, 12), result)
    }

    @Test
    fun parseExpiryDate_textMonthShortYear_returnsEndOfMonth() {
        val result = DateParser.parseExpiryDate("BB NOV 25", fixedNow)
        assertEquals(expectedEndOfMonth(2025, 11), result)
    }

    // =========================================================================
    // 5. Case-Insensitivity and Whitespace Tolerance
    // =========================================================================

    @Test
    fun parseExpiryDate_caseInsensitive_returnsCorrectTimestamp() {
        assertEquals(expectedEndOfDay(2026, 10, 24), DateParser.parseExpiryDate("exp 24/10/2026", fixedNow))
        assertEquals(expectedEndOfMonth(2026, 12), DateParser.parseExpiryDate("best before 12/26", fixedNow))
        assertEquals(expectedEndOfDay(2026, 10, 24), DateParser.parseExpiryDate("24 oct 2026", fixedNow))
    }

    @Test
    fun parseExpiryDate_excessiveWhitespaceAndNewlines_returnsCorrectTimestamp() {
        val input = "EXP :   24  /  10  /  2026\n"
        assertEquals(expectedEndOfDay(2026, 10, 24), DateParser.parseExpiryDate(input, fixedNow))

        val multiline = "BEST BEFORE\n12 / 26"
        assertEquals(expectedEndOfMonth(2026, 12), DateParser.parseExpiryDate(multiline, fixedNow))
    }

    // =========================================================================
    // 6. Two-Digit Year Expansions with Apostrophe
    // =========================================================================

    @Test
    fun parseExpiryDate_apostropheTwoDigitYear_expandsToCentury() {
        val result = DateParser.parseExpiryDate("EXP 15/06/'26", fixedNow)
        assertEquals(expectedEndOfDay(2026, 6, 15), result)
    }

    // =========================================================================
    // 7. Leap Year and Calendar Boundary Tests
    // =========================================================================

    @Test
    fun parseExpiryDate_leapYearFeb29Valid_returnsCorrectTimestamp() {
        val result2024 = DateParser.parseExpiryDate("29/02/2024", fixedNow)
        assertEquals(expectedEndOfDay(2024, 2, 29), result2024)

        val result2028 = DateParser.parseExpiryDate("29/02/2028", fixedNow)
        assertEquals(expectedEndOfDay(2028, 2, 29), result2028)
    }

    @Test
    fun parseExpiryDate_leapYearEndOfMonth_returnsFeb29OnLeapYear() {
        val resultLeap = DateParser.parseExpiryDate("BB 02/24", fixedNow)
        assertEquals(expectedEndOfDay(2024, 2, 29), resultLeap)

        val resultNonLeap = DateParser.parseExpiryDate("BB 02/25", fixedNow)
        assertEquals(expectedEndOfDay(2025, 2, 28), resultNonLeap)
    }

    @Test
    fun parseExpiryDate_nonLeapYearFeb29_returnsNull() {
        val result2025 = DateParser.parseExpiryDate("29/02/2025", fixedNow)
        assertNull("Feb 29 on 2025 must be rejected", result2025)

        val result2026 = DateParser.parseExpiryDate("29/02/2026", fixedNow)
        assertNull("Feb 29 on 2026 must be rejected", result2026)
    }

    @Test
    fun parseExpiryDate_invalidDayNumbers_returnsNull() {
        assertNull("Day 32 must be rejected", DateParser.parseExpiryDate("32/01/2026", fixedNow))
        assertNull("Day 00 must be rejected", DateParser.parseExpiryDate("00/01/2026", fixedNow))
        assertNull("April 31 must be rejected", DateParser.parseExpiryDate("31/04/2026", fixedNow))
        assertNull("June 31 must be rejected", DateParser.parseExpiryDate("31/06/2026", fixedNow))
        assertNull("September 31 must be rejected", DateParser.parseExpiryDate("31/09/2026", fixedNow))
        assertNull("November 31 must be rejected", DateParser.parseExpiryDate("31/11/2026", fixedNow))
    }

    @Test
    fun parseExpiryDate_invalidMonthNumbers_returnsNull() {
        assertNull("Month 13 must be rejected", DateParser.parseExpiryDate("15/13/2026", fixedNow))
        assertNull("Month 00 must be rejected", DateParser.parseExpiryDate("15/00/2026", fixedNow))
    }

    @Test
    fun parseExpiryDate_yearOutOfRange_returnsNull() {
        assertNull("Year 1980 must be rejected", DateParser.parseExpiryDate("15/10/1980", fixedNow))
        assertNull("Year 2150 must be rejected", DateParser.parseExpiryDate("15/10/2150", fixedNow))
    }

    // =========================================================================
    // 8. Multi-Date Disambiguation (MFG vs EXP, Future vs Past)
    // =========================================================================

    @Test
    fun parseExpiryDate_mfgAndExp_selectsExpiryDate() {
        val input = "MFG: 01/01/2024 EXP: 24/10/2026"
        val result = DateParser.parseExpiryDate(input, fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_expFirstThenProd_selectsExpiryDate() {
        val input = "EXP 24/10/2026 PROD 01/01/2024"
        val result = DateParser.parseExpiryDate(input, fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun parseExpiryDate_twoDatesWithoutPrefix_selectsFutureOrLaterDate() {
        val input = "01/01/2024 24/10/2026"
        val result = DateParser.parseExpiryDate(input, fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    // =========================================================================
    // 9. OCR Noise Rejection
    // =========================================================================

    @Test
    fun parseExpiryDate_emptyOrBlank_returnsNull() {
        assertNull(DateParser.parseExpiryDate(""))
        assertNull(DateParser.parseExpiryDate("   \t\n  "))
    }

    @Test
    fun parseExpiryDate_textNoiseWithoutDate_returnsNull() {
        assertNull(DateParser.parseExpiryDate("STORE IN A COOL DRY PLACE"))
        assertNull(DateParser.parseExpiryDate("NET WT 500g"))
        assertNull(DateParser.parseExpiryDate("BARCODE 8901030382912"))
        assertNull(DateParser.parseExpiryDate("123456789"))
        assertNull(DateParser.parseExpiryDate("LOT 88472-B"))
        assertNull(DateParser.parseExpiryDate("CUSTOMER CARE: 1-800-555-0199"))
    }

    // =========================================================================
    // 10. formatDisplayDate Helper
    // =========================================================================

    @Test
    fun formatDisplayDate_formatsTimestampCorrectly() {
        val ts = expectedEndOfDay(2026, 10, 24)
        val formatted = DateParser.formatDisplayDate(ts)
        assertEquals("Oct 24, 2026", formatted)
    }
}
