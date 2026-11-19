package dev.polyrics.mondafono.spam

interface BlockedCallStore {
    fun record(number: String, at: Long)
    fun count(): Int
}
