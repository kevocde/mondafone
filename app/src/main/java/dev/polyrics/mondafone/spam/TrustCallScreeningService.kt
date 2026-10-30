package dev.polyrics.mondafone.spam

import android.telecom.Call
import android.telecom.CallScreeningService

class TrustCallScreeningService : CallScreeningService() {

    private val dependencies: SpamDependencies by lazy { spamDependencies() }
    private val store: CallVerdictStore get() = dependencies.verdictStore
    private val reportStore: SpamReportStore get() = dependencies.spamReportStore
    private val normalizer: PhoneNumberNormalizer get() = dependencies.phoneNumberNormalizer
    private val blockedNotifier: BlockedCallNotifier get() = dependencies.blockedCallNotifier
    private val promptNotifier: SpamVerdictPromptNotifier get() = dependencies.verdictPromptNotifier

    internal var globalSpamProbability: GlobalSpamProbability = GlobalSpamProbability()

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
        val canonicalNumber = canonicalOf(number)
        if (store.isWhitelisted(canonicalNumber)) {
            responder(allowResponse())
            return
        }
        if (isBlockedNumber(canonicalNumber)) {
            responder(blockResponse())
            blockedNotifier.notifyBlocked(number)
            return
        }
        if (!store.isClassified(canonicalNumber)) {
            responder(allowResponse())
            store.addPending(canonicalNumber)
            promptNotifier.promptVerdict(number)
            return
        }
        responder(allowResponse())
    }

    fun shouldBlockCall(number: String): Boolean {
        val canonicalNumber = canonicalOf(number)
        if (store.isWhitelisted(canonicalNumber)) return false
        return isBlockedNumber(canonicalNumber)
    }

    private fun isBlockedNumber(canonicalNumber: String): Boolean =
        reportStore.isReportedByMe(canonicalNumber) ||
            globalSpamProbability.isSpamByChance(reportStore.foreignReportCount(canonicalNumber))

    private fun canonicalOf(number: String): String =
        normalizer.normalize(number) ?: number

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
