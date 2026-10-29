package dev.polyrics.mondafone.spam

import android.content.Context

interface SpamDependencies {
    val verdictStore: CallVerdictStore
    val blockedCallNotifier: BlockedCallNotifier
    val verdictPromptNotifier: SpamVerdictPromptNotifier
}

internal fun Context.spamDependencies(): SpamDependencies =
    applicationContext as SpamDependencies
