package dev.polyrics.mondafone.spam

interface BlockedCallNotifier {
    fun notifyBlocked(number: String)
}
