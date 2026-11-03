package dev.polyrics.mondafone.spam

import android.content.Context

interface SpamDependencies {
    val verdictStore: CallVerdictStore
    val spamReportStore: SpamReportStore
    val peerStatusStore: PeerStatusStore
    val phoneNumberNormalizer: PhoneNumberNormalizer
    val blockedCallNotifier: BlockedCallNotifier
    val verdictPromptNotifier: SpamVerdictPromptNotifier
}

internal fun Context.spamDependencies(): SpamDependencies =
    applicationContext as SpamDependencies
