package dev.polyrics.mondafone.spam

import dev.polyrics.mondafone.contacts.ContactsLookup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamReportDeciderTest {

    private val registeredNumbers = mutableSetOf<String>()
    private val contactNumbers = mutableSetOf<String>()

    private val store = object : SpamStore {
        override fun isRegistered(number: String) = number in registeredNumbers
        override fun mark(number: String, isSpam: Boolean) {
            registeredNumbers += number
        }
    }
    private val contacts = object : ContactsLookup {
        override fun isContact(number: String) = number in contactNumbers
    }
    private val decider = SpamReportDecider(store, contacts)

    private fun incoming(number: String = "600123456", duration: Long = 10) =
        CallInfo(number, CallType.INCOMING, duration)

    @Test
    fun asksForUnknownIncomingCall() {
        assertTrue(decider.shouldAsk(incoming()))
    }

    @Test
    fun ignoresShortCalls() {
        assertFalse(decider.shouldAsk(incoming(duration = 2)))
    }

    @Test
    fun ignoresNonIncomingCalls() {
        assertFalse(decider.shouldAsk(incoming().copy(type = CallType.OUTGOING)))
        assertFalse(decider.shouldAsk(incoming().copy(type = CallType.MISSED)))
        assertFalse(decider.shouldAsk(incoming().copy(type = CallType.REJECTED)))
    }

    @Test
    fun ignoresContacts() {
        contactNumbers += "600123456"
        assertFalse(decider.shouldAsk(incoming()))
    }

    @Test
    fun ignoresAlreadyRegisteredNumbers() {
        registeredNumbers += "600123456"
        assertFalse(decider.shouldAsk(incoming()))
    }
}
