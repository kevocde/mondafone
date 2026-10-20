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
import android.util.Log
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
    private var checkPending = false

    private val callLogObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = scheduleCallCheck("call log changed")
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        lastSeenCallId = reader.latestCall()?.id ?: -1L
        contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver)
        isRunning = true
        Log.i(TAG, "monitor started, last call id $lastSeenCallId")
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(callLogObserver)
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleCallCheck(reason: String) {
        if (checkPending) return
        checkPending = true
        Log.d(TAG, "check scheduled: $reason")
        handler.postDelayed({
            checkPending = false
            checkLatestCall()
        }, CALL_LOG_WRITE_DELAY_MS)
    }

    private fun checkLatestCall() {
        val call = reader.latestCall()
        if (call == null) {
            Log.d(TAG, "check: no call in log")
            return
        }
        if (call.id <= lastSeenCallId) {
            Log.d(TAG, "check: call #${call.id} already processed")
            return
        }
        lastSeenCallId = call.id
        if (!decider.shouldAsk(call.info)) {
            Log.i(TAG, "check: skipped #${call.id} ${call.info}")
            return
        }
        Log.i(TAG, "check: asking verdict for #${call.id}")
        try {
            overlay.show(call.info.number) { isSpam -> store.mark(call.info.number, isSpam) }
        } catch (e: RuntimeException) {
            Log.e(TAG, "overlay failed", e)
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

    companion object {
        const val TAG = "CallMonitor"
        var isRunning = false
            private set
        private const val CHANNEL_ID = "call_monitor"
        private const val NOTIFICATION_ID = 1
        private const val CALL_LOG_WRITE_DELAY_MS = 1_000L
    }
}
