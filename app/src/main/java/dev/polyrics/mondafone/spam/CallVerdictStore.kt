package dev.polyrics.mondafone.spam

interface CallVerdictStore {
    fun isWhitelisted(number: String): Boolean
    fun isClassified(number: String): Boolean
    fun addToWhitelist(number: String)
    fun addPending(number: String)
    fun pendingNumbers(): List<String>
    fun whitelistNumbers(): List<String>
    fun remove(number: String)
    fun clear()
    fun registerOnChangeListener(listener: () -> Unit)
    fun unregisterOnChangeListener()
}
