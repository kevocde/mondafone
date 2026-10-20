package dev.polyrics.mondafone.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import dev.polyrics.mondafone.R
import dev.polyrics.mondafone.contacts.PhoneContactsLookup
import dev.polyrics.mondafone.spam.PrefsSpamStore
import dev.polyrics.mondafone.spam.SpamReportDecider
import dev.polyrics.mondafone.ui.SpamReportOverlay

class CallMonitorService : Service() {

    private val store by lazy { PrefsSpamStore(this) }
    private val decider by lazy { SpamReportDecider(store, PhoneContactsLookup(contentResolver)) }
    private val reader by lazy { CallLogReader(contentResolver) }
    private val overlay by lazy { SpamReportOverlay(this) }
    private val handler = Handler(Looper.getMainLooper())

    private var lastSeenCallId = -1L

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = checkLatestCall()
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        lastSeenCallId = reader.latestCall()?.id ?: -1L
        contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(observer)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkLatestCall() {
        val call = reader.latestCall() ?: return
        if (call.id <= lastSeenCallId) return
        lastSeenCallId = call.id
        if (decider.shouldAsk(call.info)) {
            overlay.show(call.info.number) { isSpam ->
                store.mark(call.info.number, isSpam)
            }
        }
    }

    private fun startForegroundWithNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(getString(R.string.monitor_notification_text))
            .build()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
    }

    private companion object {
        const val CHANNEL_ID = "call_monitor"
        const val NOTIFICATION_ID = 1
    }
}
