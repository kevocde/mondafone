package dev.polyrics.mondafone.spam

import android.telecom.Call
import android.telecom.CallScreeningService
import dev.polyrics.mondafone.MondafoneApp

class TrustCallScreeningService : CallScreeningService() {

    var store: CallVerdictStore? = null
    var blockedNotifier: BlockedCallNotifier? = null
    var promptNotifier: SpamVerdictPromptNotifier? = null

    private val defaultStore: CallVerdictStore by lazy { (application as MondafoneApp).verdictStore }
    private val defaultBlockedNotifier: BlockedCallNotifier by lazy { NotificationBlockedCallNotifier(this) }
    private val defaultPromptNotifier: SpamVerdictPromptNotifier by lazy { NotificationSpamVerdictPromptNotifier(this) }

    private fun activeStore(): CallVerdictStore = store ?: defaultStore
    private fun activeBlockedNotifier(): BlockedCallNotifier = blockedNotifier ?: defaultBlockedNotifier
    private fun activePromptNotifier(): SpamVerdictPromptNotifier = promptNotifier ?: defaultPromptNotifier

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, allowResponse())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart ?: return
        val currentStore = activeStore()

        when {
            currentStore.isWhitelisted(number) -> respondToCall(callDetails, allowResponse())
            currentStore.isBlacklisted(number) -> {
                respondToCall(callDetails, blockResponse())
                activeBlockedNotifier().notifyBlocked(number)
            }
            !currentStore.isClassified(number) -> {
                respondToCall(callDetails, allowResponse())
                currentStore.addPending(number)
                activePromptNotifier().promptVerdict(number)
            }
            else -> respondToCall(callDetails, allowResponse())
        }
    }

    fun shouldBlockCall(number: String): Boolean {
        if (activeStore().isWhitelisted(number)) return false
        return activeStore().isBlacklisted(number)
    }

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
