package dev.polyrics.mondafone.network

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ExactAlarmStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        if (!context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) return
        BeaconAlarmScheduler(context).scheduleNext()
    }
}
