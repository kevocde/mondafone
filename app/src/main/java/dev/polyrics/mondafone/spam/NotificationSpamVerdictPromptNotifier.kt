package dev.polyrics.mondafone.spam

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.polyrics.mondafone.MainActivity
import dev.polyrics.mondafone.R

class NotificationSpamVerdictPromptNotifier(
    private val context: Context,
) : SpamVerdictPromptNotifier {

    override fun promptVerdict(number: String) {
        ensureChannel()
        val notificationId = SpamVerdictService.notificationIdFor(number)
        notificationManager.notify(notificationId, buildNotification(number, notificationId))
    }

    private fun buildNotification(number: String, notificationId: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val spamIntent = SpamVerdictService.createPendingIntent(
            context,
            number,
            SpamVerdictAction.MARK_SPAM,
            notificationId,
        )

        val notSpamIntent = SpamVerdictService.createPendingIntent(
            context,
            number,
            SpamVerdictAction.MARK_NOT_SPAM,
            notificationId,
        )

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.spam_prompt_title))
            .setContentText(context.getString(R.string.spam_prompt_text, number))
            .setContentIntent(contentIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.answer_yes),
                    spamIntent,
                ).build(),
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.answer_no),
                    notSpamIntent,
                ).build(),
            )
            .setAutoCancel(true)
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.spam_prompt_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    private companion object {
        const val CHANNEL_ID = "spam_verdict_prompt"
    }
}
