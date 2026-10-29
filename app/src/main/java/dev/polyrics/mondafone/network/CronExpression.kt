package dev.polyrics.mondafone.network

import java.time.LocalDateTime

class CronExpression private constructor(
    private val minutes: Set<Int>,
    private val hours: Set<Int>,
    private val daysOfMonth: Set<Int>,
    private val months: Set<Int>,
    private val daysOfWeek: Set<Int>,
    private val dayOfMonthRestricted: Boolean,
    private val dayOfWeekRestricted: Boolean,
) {

    fun nextAfter(time: LocalDateTime): LocalDateTime? {
        var candidate = time.withSecond(0).withNano(0).plusMinutes(1)
        repeat(MAX_SEARCH_MINUTES) {
            if (matches(candidate)) return candidate
            candidate = candidate.plusMinutes(1)
        }
        return null
    }

    private fun matches(time: LocalDateTime): Boolean {
        if (time.minute !in minutes) return false
        if (time.hour !in hours) return false
        if (time.monthValue !in months) return false
        val dayOfMonthMatches = time.dayOfMonth in daysOfMonth
        val dayOfWeekMatches = time.dayOfWeek.value % 7 in daysOfWeek
        return when {
            dayOfMonthRestricted && dayOfWeekRestricted ->
                dayOfMonthMatches || dayOfWeekMatches
            else -> dayOfMonthMatches && dayOfWeekMatches
        }
    }

    companion object {
        private const val MAX_SEARCH_MINUTES = 60 * 24 * 366 * 5

        fun parse(text: String): CronExpression? {
            val fields = text.trim().split(WHITESPACE)
            if (fields.size != FIELD_COUNT) return null
            val minutes = parseField(fields[0], 0, 59) ?: return null
            val hours = parseField(fields[1], 0, 23) ?: return null
            val daysOfMonth = parseField(fields[2], 1, 31) ?: return null
            val months = parseField(fields[3], 1, 12) ?: return null
            val daysOfWeek = parseField(fields[4], 0, 7) ?: return null
            return CronExpression(
                minutes = minutes,
                hours = hours,
                daysOfMonth = daysOfMonth,
                months = months,
                daysOfWeek = daysOfWeek.map { it % 7 }.toSet(),
                dayOfMonthRestricted = fields[2] != "*",
                dayOfWeekRestricted = fields[4] != "*",
            )
        }

        private fun parseField(field: String, min: Int, max: Int): Set<Int>? {
            val values = mutableSetOf<Int>()
            for (part in field.split(",")) {
                values += parsePart(part, min, max) ?: return null
            }
            return values
        }

        private fun parsePart(part: String, min: Int, max: Int): Set<Int>? {
            val segments = part.split("/")
            if (segments.size > 2) return null
            val base = segments[0]
            val step = segments.getOrNull(1)?.toIntOrNull() ?: 1
            if (segments.size == 2 && step <= 0) return null
            if (step <= 0) return null
            return when {
                base == "*" -> steppedRange(min, max, min, step)
                "-" in base -> {
                    val bounds = base.split("-")
                    if (bounds.size != 2) return null
                    val start = bounds[0].toIntOrNull() ?: return null
                    val end = bounds[1].toIntOrNull() ?: return null
                    if (start < min || end > max) return null
                    steppedRange(start, end, start, step)
                }
                else -> {
                    val value = base.toIntOrNull() ?: return null
                    if (value !in min..max) return null
                    if (segments.size == 1) setOf(value) else steppedRange(value, max, value, step)
                }
            }
        }

        private fun steppedRange(start: Int, end: Int, origin: Int, step: Int): Set<Int>? {
            if (start > end) return null
            return (start..end).filter { (it - origin) % step == 0 }.toSet()
        }

        private val WHITESPACE = Regex("\\s+")
        private const val FIELD_COUNT = 5
    }
}
