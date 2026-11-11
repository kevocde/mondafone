package dev.polyrics.mondafone.spam

interface BlockedCallStore {
    fun record(number: String, at: Long)
    fun count(): Int
}
