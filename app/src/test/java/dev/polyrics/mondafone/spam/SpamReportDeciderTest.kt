package dev.polyrics.mondafone.spam

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamReportDeciderTest {

    private val registeredNumbers = mutableSetOf<String>()

    private val store = object : SpamStore {
        override fun isRegistered(number: String) = number in registeredNumbers
        override fun isSpam(number: String) = false
        override fun mark(number: String, isSpam: Boolean) {
            registeredNumbers += number
        }
        override fun addPending(number: String) {}
        override fun pendingNumbers(): List<String> = emptyList()
    }
    private val decider = SpamReportDecider(store)

    private fun incoming(number: String = "600123456") =
        CallInfo(number, CallType.INCOMING)

    @Test
    fun asksForUnknownIncomingCall() {
        assertTrue(decider.shouldAsk(incoming()))
    }

    @Test
    fun ignoresNonIncomingCalls() {
        assertFalse(decider.shouldAsk(incoming().copy(type = CallType.OUTGOING)))
        assertFalse(decider.shouldAsk(incoming().copy(type = CallType.MISSED)))
        assertFalse(decider.shouldAsk(incoming().copy(type = CallType.REJECTED)))
    }

    @Test
    fun ignoresAlreadyRegisteredNumbers() {
        registeredNumbers += "600123456"
        assertFalse(decider.shouldAsk(incoming()))
    }
}
