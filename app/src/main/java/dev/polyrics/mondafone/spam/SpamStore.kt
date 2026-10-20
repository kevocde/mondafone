package dev.polyrics.mondafone.spam

interface SpamStore {
    fun isRegistered(number: String): Boolean
    fun mark(number: String, isSpam: Boolean)
}
