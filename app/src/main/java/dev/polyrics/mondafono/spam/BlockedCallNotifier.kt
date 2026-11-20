package dev.polyrics.mondafono.spam

interface BlockedCallNotifier {
    fun notifyBlocked(number: String)
}
