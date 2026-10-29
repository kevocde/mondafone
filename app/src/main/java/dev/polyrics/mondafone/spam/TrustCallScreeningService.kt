package dev.polyrics.mondafone.spam

import android.telecom.Call
import android.telecom.CallScreeningService

class TrustCallScreeningService : CallScreeningService() {

    private val dependencies: SpamDependencies by lazy { spamDependencies() }
    private val store: CallVerdictStore get() = dependencies.verdictStore
    private val blockedNotifier: BlockedCallNotifier get() = dependencies.blockedCallNotifier
    private val promptNotifier: SpamVerdictPromptNotifier get() = dependencies.verdictPromptNotifier

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, allowResponse())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart ?: return
        screenIncomingNumber(number) { response -> respondToCall(callDetails, response) }
    }

    internal fun screenIncomingNumber(
        number: String,
        responder: (CallResponse) -> Unit,
    ) {
        when {
            store.isWhitelisted(number) -> responder(allowResponse())
            store.isBlacklisted(number) -> {
                responder(blockResponse())
                blockedNotifier.notifyBlocked(number)
            }
            !store.isClassified(number) -> {
                responder(allowResponse())
                store.addPending(number)
                promptNotifier.promptVerdict(number)
            }
            else -> responder(allowResponse())
        }
    }

    fun shouldBlockCall(number: String): Boolean {
        if (store.isWhitelisted(number)) return false
        return store.isBlacklisted(number)
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
