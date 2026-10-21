package dev.polyrics.mondafone.spam

data class SpamStats(
    val spam: Int = 0,
    val notSpam: Int = 0,
) {
    val total: Int get() = spam + notSpam
}

interface SpamStore {
    fun isRegistered(number: String): Boolean
    fun isSpam(number: String): Boolean
    fun mark(number: String, isSpam: Boolean)
    fun stats(): SpamStats = SpamStats()
    fun clear() {}
}
