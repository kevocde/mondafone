package dev.polyrics.mondafone.spam

import android.telecom.Call
import android.telecom.CallScreeningService

class SpamCallScreeningService(
    private val store: SpamStore,
    private val notifier: SpamBlockedNotifier? = null,
) : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: return

        if (shouldBlockCall(number)) {
            respondToCall(callDetails, blockCallResponse())
            notifier?.notifyBlocked(number)
        } else {
            respondToCall(callDetails, allowCallResponse())
        }
    }

    fun shouldBlockCall(number: String): Boolean = store.isSpam(number)

    private fun blockCallResponse(): CallResponse = CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build()

    private fun allowCallResponse(): CallResponse = CallResponse.Builder()
        .setDisallowCall(false)
        .build()
}
