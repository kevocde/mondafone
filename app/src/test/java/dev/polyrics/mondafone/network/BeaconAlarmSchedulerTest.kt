package dev.polyrics.mondafone.network

import android.app.AlarmManager
import android.app.Application
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class BeaconAlarmSchedulerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val cron = CronExpression.parse("*/5 * * * *")

    @Test
    fun schedulesNextCronTickAsWakeupAlarm() {
        val scheduler = BeaconAlarmScheduler(context, cron, alarmManager)

        scheduler.scheduleNext(LocalDateTime.of(2026, 10, 29, 14, 3, 20))

        val scheduled = shadowOf(alarmManager).scheduledAlarms.single()
        assertEquals(AlarmManager.RTC_WAKEUP, scheduled.getType())
        assertEquals(triggerAt(2026, 10, 29, 14, 5), scheduled.getTriggerAtMs())
        assertTrue(scheduled.isAllowWhileIdle())
    }

    @Test
    fun reschedulingTargetsTheFollowingTick() {
        val scheduler = BeaconAlarmScheduler(context, cron, alarmManager)

        scheduler.scheduleNext(LocalDateTime.of(2026, 10, 29, 14, 3))
        scheduler.scheduleNext(LocalDateTime.of(2026, 10, 29, 14, 6))

        val scheduled = shadowOf(alarmManager).scheduledAlarms.last()
        assertEquals(triggerAt(2026, 10, 29, 14, 10), scheduled.getTriggerAtMs())
    }

    @Test
    fun cancelRemovesScheduledAlarm() {
        val scheduler = BeaconAlarmScheduler(context, cron, alarmManager)
        scheduler.scheduleNext(LocalDateTime.of(2026, 10, 29, 14, 3))

        scheduler.cancel()

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun missingScheduleDoesNothing() {
        val scheduler = BeaconAlarmScheduler(context, null, alarmManager)

        scheduler.scheduleNext(LocalDateTime.of(2026, 10, 29, 14, 3))

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Suppress("DEPRECATION")
    @Test
    fun alarmTargetsBeaconService() {
        val scheduler = BeaconAlarmScheduler(context, cron, alarmManager)

        scheduler.scheduleNext(LocalDateTime.of(2026, 10, 29, 14, 3))

        val operation = shadowOf(alarmManager).scheduledAlarms.single().operation
        assertTrue(shadowOf(operation!!).isForegroundService)
    }

    @Suppress("DEPRECATION")
    @Test
    fun alarmCarriesExpectedTriggerTime() {
        val scheduler = BeaconAlarmScheduler(context, cron, alarmManager)
        val expected = triggerAt(2026, 10, 29, 14, 5)

        scheduler.scheduleNext(LocalDateTime.of(2026, 10, 29, 14, 3, 20))

        val operation = shadowOf(alarmManager).scheduledAlarms.single().operation
        assertEquals(
            expected,
            shadowOf(operation!!).savedIntent.getLongExtra(BeaconAlarmScheduler.EXTRA_EXPECTED_AT, 0L),
        )
    }

    private fun triggerAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}
