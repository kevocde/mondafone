package dev.polyrics.mondafone.spam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.polyrics.mondafone.R

class NotificationBlockedCallNotifier(
    private val context: Context,
) : BlockedCallNotifier {

    override fun notifyBlocked(number: String) {
        ensureChannel()
        val notification = buildNotification(number)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(number: String): Notification {
        val blacklistIntent = PendingIntent.getBroadcast(
            context,
            number.hashCode(),
            Intent(context, SpamVerdictReceiver::class.java).apply {
                action = SpamVerdictReceiver.ACTION_MARK_SPAM
                putExtra(SpamVerdictReceiver.EXTRA_NUMBER, number)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val whitelistIntent = PendingIntent.getBroadcast(
            context,
            number.hashCode() + 1,
            Intent(context, SpamVerdictReceiver::class.java).apply {
                action = SpamVerdictReceiver.ACTION_MARK_NOT_SPAM
                putExtra(SpamVerdictReceiver.EXTRA_NUMBER, number)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_delete)
            .setContentTitle(context.getString(R.string.blocked_call_title))
            .setContentText(context.getString(R.string.blocked_call_text, number))
            .setAutoCancel(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.blocked_call_action_yes),
                    blacklistIntent,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.blocked_call_action_no),
                    whitelistIntent,
                ).build(),
            )
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.blocked_call_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    private companion object {
        const val CHANNEL_ID = "blocked_call"
        const val NOTIFICATION_ID = 2
    }
}
