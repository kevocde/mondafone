package dev.polyrics.mondafone.spam

import android.telecom.Call
import android.telecom.CallScreeningService

class SpamCallScreeningService : CallScreeningService() {

    var store: SpamStore? = null
    var notifier: SpamBlockedNotifier? = null

    private val defaultStore: SpamStore by lazy { PrefsSpamStore(this) }
    private val defaultNotifier: SpamBlockedNotifier by lazy { NotificationSpamBlockedNotifier(this) }

    private fun activeStore(): SpamStore = store ?: defaultStore
    private fun activeNotifier(): SpamBlockedNotifier = notifier ?: defaultNotifier

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: return

        if (shouldBlockCall(number)) {
            respondToCall(callDetails, blockCallResponse())
            activeNotifier().notifyBlocked(number)
        } else {
            respondToCall(callDetails, allowCallResponse())
        }
    }

    fun shouldBlockCall(number: String): Boolean = activeStore().isSpam(number)

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
