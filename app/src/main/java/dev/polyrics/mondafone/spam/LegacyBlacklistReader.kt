package dev.polyrics.mondafone.spam

interface LegacyBlacklistReader {
    fun blacklistNumbers(): List<String>
}
