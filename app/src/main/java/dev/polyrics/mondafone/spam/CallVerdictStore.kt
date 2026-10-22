package dev.polyrics.mondafone.spam

interface CallVerdictStore {
    fun isWhitelisted(number: String): Boolean
    fun isBlacklisted(number: String): Boolean
    fun isClassified(number: String): Boolean
    fun addToWhitelist(number: String)
    fun addToBlacklist(number: String)
    fun addPending(number: String)
    fun pendingNumbers(): List<String>
    fun remove(number: String)
    fun stats(): VerdictStats
    fun clear()
    fun registerOnChangeListener(listener: () -> Unit)
    fun unregisterOnChangeListener()
}
