package dev.polyrics.mondafone.spam

import android.telecom.Call
import android.telecom.CallScreeningService

class SpamCallScreeningService : CallScreeningService() {

    var store: SpamStore? = null
    var blockedNotifier: SpamBlockedNotifier? = null
    var promptNotifier: SpamVerdictPromptNotifier? = null

    private val defaultStore: SpamStore by lazy { PrefsSpamStore(this) }
    private val defaultBlockedNotifier: SpamBlockedNotifier by lazy { NotificationSpamBlockedNotifier(this) }
    private val defaultPromptNotifier: SpamVerdictPromptNotifier by lazy { NotificationSpamVerdictPromptNotifier(this) }

    private fun activeStore(): SpamStore = store ?: defaultStore
    private fun activeBlockedNotifier(): SpamBlockedNotifier = blockedNotifier ?: defaultBlockedNotifier
    private fun activePromptNotifier(): SpamVerdictPromptNotifier = promptNotifier ?: defaultPromptNotifier

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, allowResponse())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart ?: return
        val currentStore = activeStore()

        when {
            currentStore.isSpam(number) -> {
                respondToCall(callDetails, blockResponse())
                activeBlockedNotifier().notifyBlocked(number)
            }
            !currentStore.isRegistered(number) -> {
                respondToCall(callDetails, allowResponse())
                currentStore.addPending(number)
                activePromptNotifier().promptVerdict(number)
            }
            else -> respondToCall(callDetails, allowResponse())
        }
    }

    fun shouldBlockCall(number: String): Boolean = activeStore().isSpam(number)

    private fun blockResponse(): CallResponse = CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build()

    private fun allowResponse(): CallResponse = CallResponse.Builder()
        .setDisallowCall(false)
        .build()
}
