package dev.polyrics.mondafone.spam

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SpamVerdictReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val number = intent.getStringExtra(EXTRA_NUMBER) ?: return
        val isSpam = intent.action == ACTION_MARK_SPAM

        PrefsSpamStore(context).mark(number, isSpam)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.cancel(number.hashCode())
    }

    companion object {
        const val ACTION_MARK_SPAM = "dev.polyrics.mondafone.ACTION_MARK_SPAM"
        const val ACTION_MARK_NOT_SPAM = "dev.polyrics.mondafone.ACTION_MARK_NOT_SPAM"
        const val EXTRA_NUMBER = "extra_number"
    }
}
