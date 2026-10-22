package dev.polyrics.mondafone.spam

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustCallScreeningServiceTest {

    private val whitelisted = mutableSetOf<String>()
    private val blacklisted = mutableSetOf<String>()
    private val pendingNumbers = mutableListOf<String>()

    private val store = object : CallVerdictStore {
        override fun isWhitelisted(number: String) = number in whitelisted
        override fun isBlacklisted(number: String) = number in blacklisted
        override fun isClassified(number: String) = number in whitelisted || number in blacklisted
        override fun addToWhitelist(number: String) {
            whitelisted += number
            pendingNumbers -= number
        }
        override fun addToBlacklist(number: String) {
            blacklisted += number
            pendingNumbers -= number
        }
        override fun addPending(number: String) {
            if (!isClassified(number)) pendingNumbers += number
        }
        override fun pendingNumbers(): List<String> = pendingNumbers
        override fun whitelistNumbers(): List<String> = whitelisted.toList()
        override fun blacklistNumbers(): List<String> = blacklisted.toList()
        override fun remove(number: String) {
            whitelisted -= number
            blacklisted -= number
        }
        override fun stats(): VerdictStats = VerdictStats(whitelisted.size, blacklisted.size)
        override fun clear() {
            whitelisted.clear()
            blacklisted.clear()
            pendingNumbers.clear()
        }
        override fun registerOnChangeListener(listener: () -> Unit) {}
        override fun unregisterOnChangeListener() {}
    }

    private val notifier = object : BlockedCallNotifier {
        val blockedNumbers = mutableListOf<String>()
        override fun notifyBlocked(number: String) {
            blockedNumbers += number
        }
    }

    private val screeningService = TrustCallScreeningService().apply {
        this.store = this@TrustCallScreeningServiceTest.store
        this.blockedNotifier = this@TrustCallScreeningServiceTest.notifier
    }

    @Test
    fun blocksBlacklistedNumber() {
        blacklisted += "600111222"

        assertTrue(screeningService.shouldBlockCall("600111222"))
    }

    @Test
    fun allowsWhitelistedNumber() {
        whitelisted += "600111222"

        assertFalse(screeningService.shouldBlockCall("600111222"))
    }

    @Test
    fun allowsUnknownNumber() {
        assertFalse(screeningService.shouldBlockCall("600999888"))
    }

    @Test
    fun allowsPendingNumber() {
        pendingNumbers += "600111222"

        assertFalse(screeningService.shouldBlockCall("600111222"))
    }

    @Test
    fun whitelistOverridesBlacklist() {
        whitelisted += "600111222"
        blacklisted += "600111222"

        assertFalse(screeningService.shouldBlockCall("600111222"))
    }
}
