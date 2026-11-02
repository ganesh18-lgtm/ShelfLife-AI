package com.shelflife.ai.data.vision

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

/**
 * Robust, zero-Android-dependency OCR expiration date extraction and normalization engine.
 * Pure Kotlin/JVM design executes in microseconds during automated testing.
 *
 * Supports:
 * - Expiration labels: EXP, EXPIRY, EXP., EXPIRY DATE, BEST BEFORE, BEST BEFORE END, BBE, BB, B.B., USE BY, UB
 * - Standard formats: DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, DD/MM/YY
 * - ISO 8601: YYYY-MM-DD, YYYY/MM/DD, YYYY.MM.DD
 * - Textual months: DD MMM YYYY (e.g., 24 OCT 2026, 05-NOV-25), MMM DD YYYY (e.g., OCT 24, 2026)
 * - Month/Year only: MM/YY, MM/YYYY, MMM YYYY (normalized to the final millisecond of that month)
 * - Two-digit year expansion: '26 / 26 -> 2026
 * - Boundary validations: Leap year (Feb 29 valid in 2024/2028, invalid in 2025/2026), 30-day months
 * - Multi-date disambiguation: Ignores manufacturing/production dates (MFG, PROD) and prioritizes future expiry
 */
object DateParser {

    private val PREFIX_REGEX = Pattern.compile(
        """(?i)(?:\b(?:BEST\s+BEFORE(?:\s+END)?|BEST\s+BY|USE\s+BY|EXP(?:IRY)?(?:\s+DATE)?|CONSUME\s+BEFORE|ENJOY\s+BY|BBE|BBD|UB|BB|EXP)\b|B\.B\.(?:D\.)?|EXP\.)[:\s-]*"""
    )

    private val MFG_PREFIX_REGEX = Pattern.compile(
        """(?i)(?:\b(?:MFG|MFR|PROD|PRD|PACKED|PKG|DOM|BATCH|LOT)\b|M\.F\.G\.)[:\s-]*"""
    )

    // DD MMM YYYY or DD MMM YY (e.g., "24 OCT 2026", "05-NOV-25", "24OCT26", "24th October 2026")
    private val PATTERN_DMY_TEXT = Pattern.compile(
        """(?i)\b([0-3]?[0-9])(?:st|nd|rd|th)?(?:\s*[./\-]\s*|\s*)(JAN(?:UARY)?|FEB(?:RUARY)?|MAR(?:CH)?|APR(?:IL)?|MAY|JUN(?:E)?|JUL(?:Y)?|AUG(?:UST)?|SEP(?:TEMBER)?|SEPT|OCT(?:OBER)?|NOV(?:EMBER)?|DEC(?:EMBER)?)(?:\s*[./\-]\s*|\s*)('?\d{2}|\d{4})\b"""
    )

    // MMM DD YYYY or MMM DD, YYYY (e.g., "OCT 24, 2026", "OCT 24 2026", "October 24 2026")
    private val PATTERN_MDY_TEXT = Pattern.compile(
        """(?i)\b(JAN(?:UARY)?|FEB(?:RUARY)?|MAR(?:CH)?|APR(?:IL)?|MAY|JUN(?:E)?|JUL(?:Y)?|AUG(?:UST)?|SEP(?:TEMBER)?|SEPT|OCT(?:OBER)?|NOV(?:EMBER)?|DEC(?:EMBER)?)[,\s./\-]+([0-3]?[0-9])(?:st|nd|rd|th)?[,\s./\-]+('?\d{2}|\d{4})\b"""
    )

    // MMM YYYY or MMM YY (e.g., "DEC 2026", "OCT '25", "OCTOBER 2026") -> end of month
    private val PATTERN_MY_TEXT = Pattern.compile(
        """(?i)\b(JAN(?:UARY)?|FEB(?:RUARY)?|MAR(?:CH)?|APR(?:IL)?|MAY|JUN(?:E)?|JUL(?:Y)?|AUG(?:UST)?|SEP(?:TEMBER)?|SEPT|OCT(?:OBER)?|NOV(?:EMBER)?|DEC(?:EMBER)?)(?:\s*[./\-]\s*|\s*)('?\d{2}|\d{4})\b"""
    )

    // ISO 8601: YYYY-MM-DD, YYYY/MM/DD, YYYY.MM.DD
    private val PATTERN_YMD = Pattern.compile(
        """\b((?:19|20)\d{2})(?:\s*[./\-]\s*|\s+)([0-1]?[0-9])(?:\s*[./\-]\s*|\s+)([0-3]?[0-9])\b"""
    )

    // Full Numeric: DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, DD/MM/YY, DD-MM-YY, DD.MM.YY
    private val PATTERN_DMY = Pattern.compile(
        """\b([0-3]?[0-9])(?:\s*[./\-]\s*|\s+)([0-1]?[0-9])(?:\s*[./\-]\s*|\s+)('?\d{2}|(?:19|20)\d{2})\b"""
    )

    // Month & Year only: MM/YY, MM/YYYY, MM-YY, MM-YYYY (e.g. BEST BEFORE 12/26 or 08/2027) -> end of month
    // Negative lookbehind and lookahead ensure we don't accidentally match part of a 3-part date (e.g. 10/2026 inside 24/10/2026)
    private val PATTERN_MY = Pattern.compile(
        """(?<!\d[./\-])\b([0-1]?[0-9])(?:\s*[./\-]\s*|\s+)('?\d{2}|(?:19|20)\d{2})\b(?![./\-]\d)"""
    )

    private val MONTH_MAP: Map<String, Int> = mapOf(
        "JAN" to 1, "JANUARY" to 1,
        "FEB" to 2, "FEBRUARY" to 2,
        "MAR" to 3, "MARCH" to 3,
        "APR" to 4, "APRIL" to 4,
        "MAY" to 5,
        "JUN" to 6, "JUNE" to 6,
        "JUL" to 7, "JULY" to 7,
        "AUG" to 8, "AUGUST" to 8,
        "SEP" to 9, "SEPT" to 9, "SEPTEMBER" to 9,
        "OCT" to 10, "OCTOBER" to 10,
        "NOV" to 11, "NOVEMBER" to 11,
        "DEC" to 12, "DECEMBER" to 12
    )

    /**
     * Parses expiration text detected via OCR into Unix epoch milliseconds.
     * Represents the exact expiration boundary: 23:59:59.999 UTC of that date.
     *
     * @param text Raw OCR string.
     * @param now Current timestamp in milliseconds used for past/future disambiguation when multiple dates exist.
     * @param zoneId Timezone boundary semantics (defaults to UTC for 100% deterministic test execution).
     * @return Epoch millisecond timestamp, or null if no valid expiration date pattern was matched.
     */
    fun parseExpiryDate(
        text: String,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneOffset.UTC
    ): Long? {
        if (text.isBlank()) return null

        val sanitized = text.replace("\r", " ").replace("\n", " ").trim()

        // 1. High-confidence path: Check if an explicit expiration prefix exists (e.g. "EXP", "BEST BEFORE", "BB", "USE BY")
        val prefixMatcher = PREFIX_REGEX.matcher(sanitized)
        while (prefixMatcher.find()) {
            val afterPrefix = sanitized.substring(prefixMatcher.end()).trim()
            val parsedFromPrefix = extractFirstValidDate(afterPrefix, zoneId)
            if (parsedFromPrefix != null) {
                return parsedFromPrefix
            }
        }

        // 2. Scan all candidate dates in the input string
        val allDates = extractAllDates(sanitized, zoneId)
        if (allDates.isEmpty()) return null
        if (allDates.size == 1) return allDates.first().timestamp

        // 3. Multi-date disambiguation:
        // Identify manufacturing dates preceded by MFG/PROD/PACKED and discard them if other dates exist
        val nonMfgDates = allDates.filterNot { it.isPrecededByMfg }
        val candidatePool = if (nonMfgDates.isNotEmpty()) nonMfgDates else allDates

        // If multiple candidates exist, prioritize future dates relative to 'now', and take the latest date (expiry > mfg)
        val futureCandidates = candidatePool.filter { it.timestamp >= now }
        return if (futureCandidates.isNotEmpty()) {
            futureCandidates.maxByOrNull { it.timestamp }?.timestamp
        } else {
            candidatePool.maxByOrNull { it.timestamp }?.timestamp
        }
    }

    private data class DateCandidate(
        val timestamp: Long,
        val startIndex: Int,
        val endIndex: Int,
        var isPrecededByMfg: Boolean = false
    )

    private fun extractFirstValidDate(segment: String, zoneId: ZoneId): Long? {
        return extractAllDates(segment, zoneId).firstOrNull()?.timestamp
    }

    private fun extractAllDates(text: String, zoneId: ZoneId): List<DateCandidate> {
        val candidates = mutableListOf<DateCandidate>()
        val rejectedSpans = mutableListOf<Pair<Int, Int>>()

        // 1. DD MMM YYYY (e.g. "24 OCT 2026", "05-NOV-25")
        val dmyTextMatcher = PATTERN_DMY_TEXT.matcher(text)
        while (dmyTextMatcher.find()) {
            val day = dmyTextMatcher.group(1)?.toIntOrNull() ?: continue
            val monthStr = dmyTextMatcher.group(2)?.uppercase(Locale.ROOT) ?: continue
            val year = normalizeYear(dmyTextMatcher.group(3)) ?: continue
            val month = MONTH_MAP[monthStr] ?: continue
            if (isValidDate(day, month, year)) {
                buildEndOfDayTimestamp(year, month, day, zoneId)?.let {
                    candidates.add(DateCandidate(it, dmyTextMatcher.start(), dmyTextMatcher.end()))
                }
            } else {
                rejectedSpans.add(dmyTextMatcher.start() to dmyTextMatcher.end())
            }
        }

        // 2. MMM DD YYYY (e.g. "OCT 24, 2026")
        val mdyTextMatcher = PATTERN_MDY_TEXT.matcher(text)
        while (mdyTextMatcher.find()) {
            val monthStr = mdyTextMatcher.group(1)?.uppercase(Locale.ROOT) ?: continue
            val day = mdyTextMatcher.group(2)?.toIntOrNull() ?: continue
            val year = normalizeYear(mdyTextMatcher.group(3)) ?: continue
            val month = MONTH_MAP[monthStr] ?: continue
            if (isValidDate(day, month, year)) {
                buildEndOfDayTimestamp(year, month, day, zoneId)?.let {
                    candidates.add(DateCandidate(it, mdyTextMatcher.start(), mdyTextMatcher.end()))
                }
            } else {
                rejectedSpans.add(mdyTextMatcher.start() to mdyTextMatcher.end())
            }
        }

        // 3. ISO 8601: YYYY-MM-DD
        val ymdMatcher = PATTERN_YMD.matcher(text)
        while (ymdMatcher.find()) {
            val year = ymdMatcher.group(1)?.toIntOrNull() ?: continue
            val month = ymdMatcher.group(2)?.toIntOrNull() ?: continue
            val day = ymdMatcher.group(3)?.toIntOrNull() ?: continue
            if (isValidDate(day, month, year)) {
                buildEndOfDayTimestamp(year, month, day, zoneId)?.let {
                    candidates.add(DateCandidate(it, ymdMatcher.start(), ymdMatcher.end()))
                }
            } else {
                rejectedSpans.add(ymdMatcher.start() to ymdMatcher.end())
            }
        }

        // 4. DD/MM/YYYY or DD/MM/YY (Numeric 3-part)
        val dmyMatcher = PATTERN_DMY.matcher(text)
        while (dmyMatcher.find()) {
            val p1 = dmyMatcher.group(1)?.toIntOrNull() ?: continue
            val p2 = dmyMatcher.group(2)?.toIntOrNull() ?: continue
            val year = normalizeYear(dmyMatcher.group(3)) ?: continue

            // Determine if DD/MM (standard) or MM/DD (US fallback)
            val ts = if (isValidDate(p1, p2, year)) {
                buildEndOfDayTimestamp(year, p2, p1, zoneId)
            } else if (isValidDate(p2, p1, year)) {
                buildEndOfDayTimestamp(year, p1, p2, zoneId)
            } else {
                null
            }

            if (ts != null) {
                candidates.add(DateCandidate(ts, dmyMatcher.start(), dmyMatcher.end()))
            } else {
                rejectedSpans.add(dmyMatcher.start() to dmyMatcher.end())
            }
        }

        // 5. MMM YYYY (e.g. "DEC 2026") -> end of month
        val myTextMatcher = PATTERN_MY_TEXT.matcher(text)
        while (myTextMatcher.find()) {
            val matchStart = myTextMatcher.start()
            val matchEnd = myTextMatcher.end()
            // Avoid double-counting if covered by already-matched or rejected 3-part textual date
            if (candidates.any { matchStart < it.endIndex && matchEnd > it.startIndex } ||
                rejectedSpans.any { matchStart < it.second && matchEnd > it.first }
            ) {
                continue
            }
            val monthStr = myTextMatcher.group(1)?.uppercase(Locale.ROOT) ?: continue
            val year = normalizeYear(myTextMatcher.group(2)) ?: continue
            val month = MONTH_MAP[monthStr] ?: continue
            if (month in 1..12 && year in 2000..2099) {
                buildEndOfMonthTimestamp(year, month, zoneId)?.let {
                    candidates.add(DateCandidate(it, matchStart, matchEnd))
                }
            }
        }

        // 6. MM/YY or MM/YYYY (e.g. "12/26") -> end of month
        val myMatcher = PATTERN_MY.matcher(text)
        while (myMatcher.find()) {
            val matchStart = myMatcher.start()
            val matchEnd = myMatcher.end()
            // Avoid double-counting if covered by already-matched or rejected 3-part numeric date
            if (candidates.any { matchStart < it.endIndex && matchEnd > it.startIndex } ||
                rejectedSpans.any { matchStart < it.second && matchEnd > it.first }
            ) {
                continue
            }
            val month = myMatcher.group(1)?.toIntOrNull() ?: continue
            val year = normalizeYear(myMatcher.group(2)) ?: continue
            if (month in 1..12 && year in 2000..2099) {
                buildEndOfMonthTimestamp(year, month, zoneId)?.let {
                    candidates.add(DateCandidate(it, matchStart, matchEnd))
                }
            }
        }

        // Sort candidates chronologically by appearance in text
        candidates.sortBy { it.startIndex }

        // Tag any candidate that is immediately preceded by a manufacturing prefix (e.g. MFG, PROD)
        val mfgMatcher = MFG_PREFIX_REGEX.matcher(text)
        while (mfgMatcher.find()) {
            val mfgEnd = mfgMatcher.end()
            val nextCandidate = candidates.firstOrNull { it.startIndex >= mfgEnd && it.startIndex - mfgEnd < 15 }
            nextCandidate?.isPrecededByMfg = true
        }

        return candidates
    }

    private fun normalizeYear(yearStr: String?): Int? {
        if (yearStr == null) return null
        val cleaned = yearStr.trim().removePrefix("'")
        val value = cleaned.toIntOrNull() ?: return null
        return when {
            value in 0..99 -> 2000 + value
            value in 2000..2099 -> value
            else -> null // Out of realistic food expiration century range
        }
    }

    private fun isValidDate(day: Int, month: Int, year: Int): Boolean {
        if (month !in 1..12 || year !in 2000..2099 || day !in 1..31) return false
        return try {
            LocalDate.of(year, month, day)
            true
        } catch (e: DateTimeException) {
            false
        }
    }

    private fun buildEndOfDayTimestamp(year: Int, month: Int, day: Int, zoneId: ZoneId): Long? {
        return try {
            val localDate = LocalDate.of(year, month, day)
            localDate.atTime(23, 59, 59, 999_000_000)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        } catch (e: DateTimeException) {
            null
        }
    }

    private fun buildEndOfMonthTimestamp(year: Int, month: Int, zoneId: ZoneId): Long? {
        return try {
            val yearMonth = YearMonth.of(year, month)
            yearMonth.atEndOfMonth()
                .atTime(23, 59, 59, 999_000_000)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        } catch (e: DateTimeException) {
            null
        }
    }

    /**
     * Formats epoch milliseconds into a human-readable display date string (e.g., "Oct 24, 2026").
     */
    fun formatDisplayDate(timestamp: Long, zoneId: ZoneId = ZoneOffset.UTC): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).format(formatter)
    }
}
