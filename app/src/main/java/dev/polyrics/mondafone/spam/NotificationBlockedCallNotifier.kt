package dev.polyrics.mondafone.spam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dev.polyrics.mondafone.R

class NotificationBlockedCallNotifier(
    private val context: Context,
) : BlockedCallNotifier {

    override fun notifyBlocked(number: String) {
        ensureChannel()
        val id = notificationId(number)
        notificationManager.notify(id, buildNotification(number, id))
    }

    private fun buildNotification(number: String, notificationId: Int): Notification {
        val blacklistIntent = SpamVerdictService.createPendingIntent(
            context,
            number,
            SpamVerdictAction.MARK_SPAM,
            notificationId,
        )

        val whitelistIntent = SpamVerdictService.createPendingIntent(
            context,
            number,
            SpamVerdictAction.MARK_NOT_SPAM,
            notificationId,
        )

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_delete)
            .setContentTitle(context.getString(R.string.blocked_call_title))
            .setContentText(context.getString(R.string.blocked_call_text))
            .setSubText(number)
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

    private fun notificationId(number: String): Int = SpamVerdictService.notificationIdFor(number)

    private companion object {
        const val CHANNEL_ID = "blocked_call"
    }
}
