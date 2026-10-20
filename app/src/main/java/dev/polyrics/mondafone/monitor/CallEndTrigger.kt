package dev.polyrics.mondafone.monitor

import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

class CallEndTrigger(private val onCallEnded: () -> Unit) {

    private var wasOffHook = false

    val callback: TelephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> wasOffHook = true
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (wasOffHook) {
                        wasOffHook = false
                        onCallEnded()
                    }
                }
            }
        }
    }
}
