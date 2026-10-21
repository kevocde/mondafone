package dev.polyrics.mondafone.spam

interface SpamBlockedNotifier {
    fun notifyBlocked(number: String)
}
