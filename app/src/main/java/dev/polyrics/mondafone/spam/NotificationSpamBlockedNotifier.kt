package dev.polyrics.mondafone.spam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dev.polyrics.mondafone.R

class NotificationSpamBlockedNotifier(
    private val context: Context,
) : SpamBlockedNotifier {

    override fun notifyBlocked(number: String) {
        ensureChannel()
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_delete)
            .setContentTitle(context.getString(R.string.spam_blocked_title))
            .setContentText(context.getString(R.string.spam_blocked_text, number))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.spam_blocked_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    private companion object {
        const val CHANNEL_ID = "spam_blocked"
        const val NOTIFICATION_ID = 2
    }
}
