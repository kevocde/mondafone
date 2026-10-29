package dev.polyrics.mondafone.spam

import android.telecom.CallScreeningService.CallResponse
import dev.polyrics.mondafone.MondafoneApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = TrustCallScreeningServiceTest.TestMondafoneApp::class)
class TrustCallScreeningServiceTest {

    private val app = RuntimeEnvironment.getApplication() as TestMondafoneApp
    private val screeningService =
        Robolectric.buildService(TrustCallScreeningService::class.java).create().get()

    @Test
    fun blocksBlacklistedNumber() {
        app.blacklisted += "600111222"

        assertTrue(screeningService.shouldBlockCall("600111222"))
    }

    @Test
    fun allowsWhitelistedNumber() {
        app.whitelisted += "600111222"

        assertFalse(screeningService.shouldBlockCall("600111222"))
    }

    @Test
    fun allowsUnknownNumber() {
        assertFalse(screeningService.shouldBlockCall("600999888"))
    }

    @Test
    fun allowsPendingNumber() {
        app.pendingNumbers += "600111222"

        assertFalse(screeningService.shouldBlockCall("600111222"))
    }

    @Test
    fun whitelistOverridesBlacklist() {
        app.whitelisted += "600111222"
        app.blacklisted += "600111222"

        assertFalse(screeningService.shouldBlockCall("600111222"))
    }

    @Test
    fun blacklistedIncomingCallRejectsAndNotifiesBlocked() {
        app.blacklisted += "600111222"
        var captured: CallResponse? = null

        screeningService.screenIncomingNumber("600111222") { captured = it }

        val response = requireNotNull(captured)
        assertTrue(response.disallowCall)
        assertTrue(response.rejectCall)
        assertEquals(listOf("600111222"), app.blockedNumbers)
    }

    @Test
    fun unclassifiedIncomingCallAllowsAddsPendingAndPromptsVerdict() {
        var captured: CallResponse? = null

        screeningService.screenIncomingNumber("600999888") { captured = it }

        assertNotNull(captured)
        assertFalse(requireNotNull(captured).disallowCall)
        assertEquals(listOf("600999888"), app.pendingNumbers)
        assertEquals(listOf("600999888"), app.promptedNumbers)
    }

    @Test
    fun whitelistedIncomingCallAllowsWithoutNotifications() {
        app.whitelisted += "600111222"
        var captured: CallResponse? = null

        screeningService.screenIncomingNumber("600111222") { captured = it }

        assertFalse(requireNotNull(captured).disallowCall)
        assertTrue(app.blockedNumbers.isEmpty())
        assertTrue(app.promptedNumbers.isEmpty())
    }

    class TestMondafoneApp : MondafoneApp() {
        val whitelisted = mutableSetOf<String>()
        val blacklisted = mutableSetOf<String>()
        val pendingNumbers = mutableListOf<String>()
        val blockedNumbers = mutableListOf<String>()
        val promptedNumbers = mutableListOf<String>()

        override val verdictStore: CallVerdictStore = object : CallVerdictStore {
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

        override val blockedCallNotifier: BlockedCallNotifier = object : BlockedCallNotifier {
            override fun notifyBlocked(number: String) {
                blockedNumbers += number
            }
        }

        override val verdictPromptNotifier: SpamVerdictPromptNotifier =
            object : SpamVerdictPromptNotifier {
                override fun promptVerdict(number: String) {
                    promptedNumbers += number
                }
            }
    }
}
