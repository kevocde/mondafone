package dev.polyrics.mondafono.spam

interface LegacyBlacklistReader {
    fun blacklistNumbers(): List<String>
}
