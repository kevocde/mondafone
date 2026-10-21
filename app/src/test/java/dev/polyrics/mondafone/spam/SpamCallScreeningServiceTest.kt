package dev.polyrics.mondafone.spam

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamCallScreeningServiceTest {

    private val spamNumbers = mutableSetOf<String>()

    private val store = object : SpamStore {
        override fun isRegistered(number: String) = number in spamNumbers
        override fun isSpam(number: String) = number in spamNumbers
        override fun mark(number: String, isSpam: Boolean) {
            if (isSpam) spamNumbers += number else spamNumbers -= number
        }
        override fun addPending(number: String) {}
        override fun pendingNumbers(): List<String> = emptyList()
    }

    private val notifier = object : SpamBlockedNotifier {
        val blockedNumbers = mutableListOf<String>()
        override fun notifyBlocked(number: String) {
            blockedNumbers += number
        }
    }

    private val screeningService = SpamCallScreeningService().apply {
        this.store = this@SpamCallScreeningServiceTest.store
        this.notifier = this@SpamCallScreeningServiceTest.notifier
    }

    @Test
    fun shouldBlockSpamNumber() {
        spamNumbers += "600111222"

        assertTrue(screeningService.shouldBlockCall("600111222"))
    }

    @Test
    fun shouldNotBlockUnknownNumber() {
        assertFalse(screeningService.shouldBlockCall("600999888"))
    }

    @Test
    fun shouldNotBlockNotSpamNumber() {
        spamNumbers += "600111222"
        store.mark("600111222", isSpam = false)

        assertFalse(screeningService.shouldBlockCall("600111222"))
    }
}
