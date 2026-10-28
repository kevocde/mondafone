package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class CronExpressionTest {

    @Test
    fun everyFiveMinutesRunsOnNextMultiple() {
        val cron = CronExpression.parse("*/5 * * * *")!!

        assertEquals(
            LocalDateTime.of(2026, 10, 28, 10, 5),
            cron.nextAfter(LocalDateTime.of(2026, 10, 28, 10, 2)),
        )
    }

    @Test
    fun everyFiveMinutesSkipsCurrentMatch() {
        val cron = CronExpression.parse("*/5 * * * *")!!

        assertEquals(
            LocalDateTime.of(2026, 10, 28, 10, 10),
            cron.nextAfter(LocalDateTime.of(2026, 10, 28, 10, 5, 30)),
        )
    }

    @Test
    fun fivePastEveryHourRunsAtNextHour() {
        val cron = CronExpression.parse("5 * * * *")!!

        assertEquals(
            LocalDateTime.of(2026, 10, 28, 11, 5),
            cron.nextAfter(LocalDateTime.of(2026, 10, 28, 10, 6)),
        )
    }

    @Test
    fun fivePastEveryHourRunsSameHourWhenBefore() {
        val cron = CronExpression.parse("5 * * * *")!!

        assertEquals(
            LocalDateTime.of(2026, 10, 28, 10, 5),
            cron.nextAfter(LocalDateTime.of(2026, 10, 28, 10, 4)),
        )
    }

    @Test
    fun supportsLists() {
        val cron = CronExpression.parse("5,35 * * * *")!!

        assertEquals(
            LocalDateTime.of(2026, 10, 28, 10, 35),
            cron.nextAfter(LocalDateTime.of(2026, 10, 28, 10, 6)),
        )
    }

    @Test
    fun supportsRangesWithStep() {
        val cron = CronExpression.parse("0-10/5 * * * *")!!

        assertEquals(
            LocalDateTime.of(2026, 10, 28, 10, 10),
            cron.nextAfter(LocalDateTime.of(2026, 10, 28, 10, 6)),
        )
    }

    @Test
    fun supportsDailySchedule() {
        val cron = CronExpression.parse("30 8 * * *")!!

        assertEquals(
            LocalDateTime.of(2026, 10, 29, 8, 30),
            cron.nextAfter(LocalDateTime.of(2026, 10, 28, 10, 0)),
        )
    }

    @Test
    fun supportsDayOfWeek() {
        val cron = CronExpression.parse("0 9 * * 1")!!

        assertEquals(
            LocalDateTime.of(2026, 11, 2, 9, 0),
            cron.nextAfter(LocalDateTime.of(2026, 10, 28, 10, 0)),
        )
    }

    @Test
    fun rejectsExpressionWithWrongFieldCount() {
        assertNull(CronExpression.parse("*/5 * *"))
    }

    @Test
    fun rejectsOutOfRangeValues() {
        assertNull(CronExpression.parse("60 * * * *"))
    }

    @Test
    fun rejectsNonNumericValues() {
        assertNull(CronExpression.parse("abc * * * *"))
    }
}
