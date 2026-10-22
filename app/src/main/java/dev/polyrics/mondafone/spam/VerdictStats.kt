package dev.polyrics.mondafone.spam

data class VerdictStats(
    val whitelist: Int = 0,
    val blacklist: Int = 0,
) {
    val total: Int get() = whitelist + blacklist
}
