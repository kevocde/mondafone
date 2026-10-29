package dev.polyrics.mondafone.spam

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import dev.polyrics.mondafone.MondafoneApp

class SpamVerdictService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            processVerdict(intent)
        } finally {
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun processVerdict(intent: Intent?) {
        val number = intent?.getStringExtra(EXTRA_NUMBER)
            ?.takeIf { it.isNotBlank() } ?: return
        val verdictAction = SpamVerdictAction.fromIntentAction(intent.action) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, notificationIdFor(number))

        val store = (applicationContext as MondafoneApp).verdictStore
        verdictAction.applyTo(store, number)
        getSystemService(NotificationManager::class.java)?.cancel(notificationId)
    }

    companion object {
        const val EXTRA_NUMBER = "extra_number"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun notificationIdFor(number: String): Int = number.hashCode()

        fun createPendingIntent(
            context: Context,
            number: String,
            action: SpamVerdictAction,
            notificationId: Int = notificationIdFor(number),
        ): PendingIntent {
            val intent = Intent(context, SpamVerdictService::class.java).apply {
                this.action = action.intentAction
                putExtra(EXTRA_NUMBER, number)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val requestCode = 31 * notificationId + action.ordinal
            return PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
