package com.shelflife.ai.data.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Adversarial and Stress Test Suite for [DateParser].
 * Authored by challenger_m3_1_gen2 to empirically stress-test edge cases,
 * boundaries, OCR fuzzing strings, and epoch timestamp precision.
 */
class DateParserAdversarialTest {

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
    // 1. LEAP YEAR BOUNDARIES
    // =========================================================================

    @Test
    fun leapYear_feb29OnValidLeapYears_isAccepted() {
        // 2024 is a leap year
        val res2024 = DateParser.parseExpiryDate("29/02/2024", fixedNow)
        assertEquals(expectedEndOfDay(2024, 2, 29), res2024)

        // 2028 is a leap year
        val res2028 = DateParser.parseExpiryDate("29/02/2028", fixedNow)
        assertEquals(expectedEndOfDay(2028, 2, 29), res2028)

        // 2024 in ISO format
        val resIso2024 = DateParser.parseExpiryDate("2024-02-29", fixedNow)
        assertEquals(expectedEndOfDay(2024, 2, 29), resIso2024)

        // 2028 in textual format
        val resText2028 = DateParser.parseExpiryDate("29 FEB 2028", fixedNow)
        assertEquals(expectedEndOfDay(2028, 2, 29), resText2028)
    }

    @Test
    fun leapYear_feb29OnNonLeapYears_isStrictlyRejected() {
        // 2025 non-leap
        assertNull("Feb 29 on 2025 must be rejected", DateParser.parseExpiryDate("29/02/2025", fixedNow))
        assertNull("Feb 29 on 2025 ISO must be rejected", DateParser.parseExpiryDate("2025-02-29", fixedNow))
        assertNull("Feb 29 on 2025 Text must be rejected", DateParser.parseExpiryDate("29 FEB 2025", fixedNow))

        // 2026 non-leap
        assertNull("Feb 29 on 2026 must be rejected", DateParser.parseExpiryDate("29/02/2026", fixedNow))
        assertNull("Feb 29 on 2026 ISO must be rejected", DateParser.parseExpiryDate("2026-02-29", fixedNow))
        assertNull("Feb 29 on 2026 Text must be rejected", DateParser.parseExpiryDate("29 FEB 2026", fixedNow))

        // 2027 non-leap
        assertNull("Feb 29 on 2027 must be rejected", DateParser.parseExpiryDate("29/02/2027", fixedNow))
        assertNull("Feb 29 on 2027 ISO must be rejected", DateParser.parseExpiryDate("2027-02-29", fixedNow))
        assertNull("Feb 29 on 2027 Text must be rejected", DateParser.parseExpiryDate("29 FEB 2027", fixedNow))
    }

    @Test
    fun leapYear_monthYearNormalization_resolvesCorrectFebruaryLastDay() {
        // Feb 2024 -> Feb 29 (leap)
        val res2024 = DateParser.parseExpiryDate("BB 02/24", fixedNow)
        assertEquals(expectedEndOfDay(2024, 2, 29), res2024)

        // Feb 2028 -> Feb 29 (leap)
        val res2028 = DateParser.parseExpiryDate("BB 02/2028", fixedNow)
        assertEquals(expectedEndOfDay(2028, 2, 29), res2028)

        // Feb 2025 -> Feb 28 (non-leap)
        val res2025 = DateParser.parseExpiryDate("BB 02/25", fixedNow)
        assertEquals(expectedEndOfDay(2025, 2, 28), res2025)

        // Feb 2026 -> Feb 28 (non-leap)
        val res2026 = DateParser.parseExpiryDate("BB 02/26", fixedNow)
        assertEquals(expectedEndOfDay(2026, 2, 28), res2026)

        // Feb 2027 -> Feb 28 (non-leap)
        val res2027 = DateParser.parseExpiryDate("BB 02/2027", fixedNow)
        assertEquals(expectedEndOfDay(2027, 2, 28), res2027)
    }

    // =========================================================================
    // 2. 30-DAY MONTH BOUNDARIES
    // =========================================================================

    @Test
    fun thirtyDayMonths_day31_isStrictlyRejected() {
        // April has 30 days
        assertNull("31/04/2026 must be rejected", DateParser.parseExpiryDate("31/04/2026", fixedNow))
        assertNull("2026-04-31 must be rejected", DateParser.parseExpiryDate("2026-04-31", fixedNow))
        assertNull("31 APR 2026 must be rejected", DateParser.parseExpiryDate("31 APR 2026", fixedNow))

        // June has 30 days
        assertNull("31/06/2026 must be rejected", DateParser.parseExpiryDate("31/06/2026", fixedNow))
        assertNull("2026-06-31 must be rejected", DateParser.parseExpiryDate("2026-06-31", fixedNow))
        assertNull("31 JUN 2026 must be rejected", DateParser.parseExpiryDate("31 JUN 2026", fixedNow))

        // September has 30 days
        assertNull("31/09/2026 must be rejected", DateParser.parseExpiryDate("31/09/2026", fixedNow))
        assertNull("2026-09-31 must be rejected", DateParser.parseExpiryDate("2026-09-31", fixedNow))
        assertNull("31 SEP 2026 must be rejected", DateParser.parseExpiryDate("31 SEP 2026", fixedNow))

        // November has 30 days
        assertNull("31/11/2026 must be rejected", DateParser.parseExpiryDate("31/11/2026", fixedNow))
        assertNull("2026-11-31 must be rejected", DateParser.parseExpiryDate("2026-11-31", fixedNow))
        assertNull("31 NOV 2026 must be rejected", DateParser.parseExpiryDate("31 NOV 2026", fixedNow))
    }

    @Test
    fun thirtyDayMonths_day30_isAccepted() {
        assertEquals(expectedEndOfDay(2026, 4, 30), DateParser.parseExpiryDate("30/04/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 6, 30), DateParser.parseExpiryDate("30/06/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 9, 30), DateParser.parseExpiryDate("30/09/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 11, 30), DateParser.parseExpiryDate("30/11/2026", fixedNow))
    }

    @Test
    fun thirtyOneDayMonths_day31_isAccepted() {
        assertEquals(expectedEndOfDay(2026, 1, 31), DateParser.parseExpiryDate("31/01/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 3, 31), DateParser.parseExpiryDate("31/03/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 5, 31), DateParser.parseExpiryDate("31/05/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 7, 31), DateParser.parseExpiryDate("31/07/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 8, 31), DateParser.parseExpiryDate("31/08/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 10, 31), DateParser.parseExpiryDate("31/10/2026", fixedNow))
        assertEquals(expectedEndOfDay(2026, 12, 31), DateParser.parseExpiryDate("31/12/2026", fixedNow))
    }

    // =========================================================================
    // 3. DOTTED PREFIXES
    // =========================================================================

    @Test
    fun dottedPrefixes_expDotAndBbDot_parsesCorrectly() {
        // "EXP. 24.10.2026"
        val res1 = DateParser.parseExpiryDate("EXP. 24.10.2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), res1)

        // "B.B. 12/26"
        val res2 = DateParser.parseExpiryDate("B.B. 12/26", fixedNow)
        assertEquals(expectedEndOfMonth(2026, 12), res2)

        // "BB: 05-2027"
        val res3 = DateParser.parseExpiryDate("BB: 05-2027", fixedNow)
        assertEquals(expectedEndOfMonth(2027, 5), res3)

        // "B.B.D. 10/2026"
        val res4 = DateParser.parseExpiryDate("B.B.D. 10/2026", fixedNow)
        assertEquals(expectedEndOfMonth(2026, 10), res4)

        // "EXP.24.10.2026" (no space after dot)
        val res5 = DateParser.parseExpiryDate("EXP.24.10.2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), res5)

        // "B.B.12/26" (no space after dot)
        val res6 = DateParser.parseExpiryDate("B.B.12/26", fixedNow)
        assertEquals(expectedEndOfMonth(2026, 12), res6)
    }

    // =========================================================================
    // 4. SUBSTRING COLLISIONS
    // =========================================================================

    @Test
    fun substringCollisions_threePartDate_doesNotTruncateToMonthYear() {
        // "24/10/2026": contains "10/2026", but MUST NOT truncate to 2026-10-31!
        val res1 = DateParser.parseExpiryDate("24/10/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), res1)

        // "EXP: 24/10/2026"
        val res2 = DateParser.parseExpiryDate("EXP: 24/10/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), res2)

        // "05/11/2027"
        val res3 = DateParser.parseExpiryDate("05/11/2027", fixedNow)
        assertEquals(expectedEndOfDay(2027, 11, 5), res3)

        // "24.10.2026"
        val res4 = DateParser.parseExpiryDate("24.10.2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), res4)

        // ISO format "2026-10-24"
        val res5 = DateParser.parseExpiryDate("2026-10-24", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), res5)
    }

    // =========================================================================
    // 5. MANUFACTURING DATE DISAMBIGUATION
    // =========================================================================

    @Test
    fun manufacturingDisambiguation_mfgFollowedByExp_alwaysSelectsExpiry() {
        // Direct mission requirement
        val input = "MFG 01/01/2024 EXP 24/10/2026"
        val result = DateParser.parseExpiryDate(input, fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), result)
    }

    @Test
    fun manufacturingDisambiguation_variousMfgLabels_alwaysSelectsExpiry() {
        // PROD prefix
        val res1 = DateParser.parseExpiryDate("PROD 15/03/2023 BEST BEFORE 20/09/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 9, 20), res1)

        // PACKED prefix
        val res2 = DateParser.parseExpiryDate("PACKED ON 10/05/2024 USE BY 10/05/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 5, 10), res2)

        // DOM prefix (Date of Manufacture)
        val res3 = DateParser.parseExpiryDate("DOM: 01/01/2024 BB 05/2027", fixedNow)
        assertEquals(expectedEndOfMonth(2027, 5), res3)

        // Batch and Mfg with ISO dates
        val res4 = DateParser.parseExpiryDate("BATCH#492 MFG 2024-02-01 EXP 2026-02-01", fixedNow)
        assertEquals(expectedEndOfDay(2026, 2, 1), res4)

        // No explicit EXP prefix, but MFG precedes the first date
        val res5 = DateParser.parseExpiryDate("MFG: 01/01/2024 24/10/2026", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), res5)
    }

    // =========================================================================
    // 6. OCR NOISY STRINGS
    // =========================================================================

    @Test
    fun ocrNoise_embeddedInProductLabels_correctlyExtractsDate() {
        // "LOT#98234 EXP: 15/08/2027 NET WT 500G"
        val res1 = DateParser.parseExpiryDate("LOT#98234 EXP: 15/08/2027 NET WT 500G", fixedNow)
        assertEquals(expectedEndOfDay(2027, 8, 15), res1)

        // "BB 28 FEB 26 (C)"
        val res2 = DateParser.parseExpiryDate("BB 28 FEB 26 (C)", fixedNow)
        assertEquals(expectedEndOfDay(2026, 2, 28), res2)

        // "USE BY: 04/11/2026 A1"
        val res3 = DateParser.parseExpiryDate("USE BY: 04/11/2026 A1", fixedNow)
        assertEquals(expectedEndOfDay(2026, 11, 4), res3)

        // Heavy industrial label noise
        val res4 = DateParser.parseExpiryDate("PRICE $4.99 EXP 18/09/2026 STORE IN COOL DRY PLACE", fixedNow)
        assertEquals(expectedEndOfDay(2026, 9, 18), res4)

        // Timestamp noise after date
        val res5 = DateParser.parseExpiryDate("EXP 24/10/2026 14:32:05 LINE 4", fixedNow)
        assertEquals(expectedEndOfDay(2026, 10, 24), res5)
    }

    // =========================================================================
    // 7. EPOCH TIMESTAMP ACCURACY & BOUNDARY PRECISION
    // =========================================================================

    @Test
    fun epochTimestamp_representsExactEndOfDayUtc() {
        val parsed = DateParser.parseExpiryDate("24/10/2026", fixedNow)
        assertNotNull(parsed)

        // Milliseconds in a full 24h day = 86,400,000
        // The last millisecond of the day UTC must have remainder 86,399,999
        val remainder = parsed!! % 86_400_000L
        assertEquals(86_399_999L, remainder)

        // Adding 1 ms must cleanly roll over to the start of the next day (00:00:00.000)
        val nextDayStart = parsed + 1L
        assertEquals(0L, nextDayStart % 86_400_000L)

        val nextDayInstant = Instant.ofEpochMilli(nextDayStart).atZone(ZoneOffset.UTC)
        assertEquals(2026, nextDayInstant.year)
        assertEquals(10, nextDayInstant.monthValue)
        assertEquals(25, nextDayInstant.dayOfMonth)
        assertEquals(0, nextDayInstant.hour)
        assertEquals(0, nextDayInstant.minute)
        assertEquals(0, nextDayInstant.second)
    }

    @Test
    fun epochTimestamp_endOfMonth_representsExactLastDayLastMillisecond() {
        // BB 02/24 -> Leap year -> Feb 29 23:59:59.999 UTC
        val parsedLeap = DateParser.parseExpiryDate("BB 02/24", fixedNow)
        assertNotNull(parsedLeap)
        val leapInstant = Instant.ofEpochMilli(parsedLeap!!).atZone(ZoneOffset.UTC)
        assertEquals(2024, leapInstant.year)
        assertEquals(2, leapInstant.monthValue)
        assertEquals(29, leapInstant.dayOfMonth)
        assertEquals(23, leapInstant.hour)
        assertEquals(59, leapInstant.minute)
        assertEquals(59, leapInstant.second)

        // BB 02/25 -> Non-leap year -> Feb 28 23:59:59.999 UTC
        val parsedNonLeap = DateParser.parseExpiryDate("BB 02/25", fixedNow)
        assertNotNull(parsedNonLeap)
        val nonLeapInstant = Instant.ofEpochMilli(parsedNonLeap!!).atZone(ZoneOffset.UTC)
        assertEquals(2025, nonLeapInstant.year)
        assertEquals(2, nonLeapInstant.monthValue)
        assertEquals(28, nonLeapInstant.dayOfMonth)
        assertEquals(23, nonLeapInstant.hour)
        assertEquals(59, nonLeapInstant.minute)
        assertEquals(59, nonLeapInstant.second)
    }
}
