package dev.polyrics.mondafone.network

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

class BeaconAlarmScheduler(
    private val context: Context,
    private val schedule: CronExpression?,
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java),
) {

    constructor(context: Context) : this(
        context,
        CronExpression.parse(BeaconConfig.fromBuild().cronExpression),
    )

    fun scheduleNext(now: LocalDateTime = LocalDateTime.now()) {
        val next = schedule?.nextAfter(now) ?: return
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val operation = pendingIntent(triggerAt)
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent(System.currentTimeMillis()))
    }

    private fun pendingIntent(expectedAt: Long): PendingIntent = PendingIntent.getForegroundService(
        context,
        REQUEST_CODE,
        Intent(context, BleBeaconService::class.java)
            .putExtra(EXTRA_EXPECTED_AT, expectedAt),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val EXTRA_EXPECTED_AT = "extra_expected_at"
        private const val REQUEST_CODE = 0
    }
}
