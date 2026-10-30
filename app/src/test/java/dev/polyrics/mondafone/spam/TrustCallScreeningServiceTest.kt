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
    fun blocksNumberReportedByMe() {
        app.ownReported += CANONICAL

        assertTrue(screeningService.shouldBlockCall(NATIONAL))
    }

    @Test
    fun allowsWhitelistedNumber() {
        app.whitelisted += CANONICAL

        assertFalse(screeningService.shouldBlockCall(NATIONAL))
    }

    @Test
    fun allowsUnknownNumber() {
        assertFalse(screeningService.shouldBlockCall("600999888"))
    }

    @Test
    fun allowsPendingNumber() {
        app.pendingNumbers += CANONICAL

        assertFalse(screeningService.shouldBlockCall(NATIONAL))
    }

    @Test
    fun whitelistOverridesOwnReport() {
        app.whitelisted += CANONICAL
        app.ownReported += CANONICAL

        assertFalse(screeningService.shouldBlockCall(NATIONAL))
    }

    @Test
    fun ownReportedIncomingCallRejectsAndNotifiesBlocked() {
        app.ownReported += CANONICAL

        val response = requireNotNull(screen(NATIONAL))

        assertTrue(response.disallowCall)
        assertTrue(response.rejectCall)
        assertEquals(listOf(NATIONAL), app.blockedNumbers)
    }

    @Test
    fun globalChanceBlocksWhenDrawIsBelowProbability() {
        app.foreignReports[CANONICAL] = 1
        screeningService.globalSpamProbability = GlobalSpamProbability(FixedRandom(0.4))

        val response = requireNotNull(screen(NATIONAL))

        assertTrue(response.disallowCall)
        assertEquals(listOf(NATIONAL), app.blockedNumbers)
        assertTrue(app.promptedNumbers.isEmpty())
    }

    @Test
    fun globalChanceAllowsAndPromptsUnknownWhenDrawIsAboveProbability() {
        app.foreignReports[CANONICAL] = 1
        screeningService.globalSpamProbability = GlobalSpamProbability(FixedRandom(0.6))

        val response = requireNotNull(screen(NATIONAL))

        assertFalse(response.disallowCall)
        assertTrue(app.blockedNumbers.isEmpty())
        assertEquals(listOf(CANONICAL), app.pendingNumbers)
        assertEquals(listOf(NATIONAL), app.promptedNumbers)
    }

    @Test
    fun whitelistOverridesGlobalReports() {
        app.whitelisted += CANONICAL
        app.foreignReports[CANONICAL] = 5
        screeningService.globalSpamProbability = GlobalSpamProbability(FixedRandom(0.0))

        val response = requireNotNull(screen(NATIONAL))

        assertFalse(response.disallowCall)
        assertTrue(app.blockedNumbers.isEmpty())
        assertTrue(app.promptedNumbers.isEmpty())
    }

    @Test
    fun unclassifiedIncomingCallAllowsAddsCanonicalPendingAndPrompts() {
        val response = requireNotNull(screen(NATIONAL))

        assertFalse(response.disallowCall)
        assertEquals(listOf(CANONICAL), app.pendingNumbers)
        assertEquals(listOf(NATIONAL), app.promptedNumbers)
    }

    @Test
    fun whitelistedIncomingCallAllowsWithoutNotifications() {
        app.whitelisted += CANONICAL

        val response = requireNotNull(screen(NATIONAL))

        assertFalse(response.disallowCall)
        assertTrue(app.blockedNumbers.isEmpty())
        assertTrue(app.promptedNumbers.isEmpty())
    }

    private fun screen(number: String): CallResponse? {
        var captured: CallResponse? = null
        screeningService.screenIncomingNumber(number) { captured = it }
        return captured
    }

    class TestMondafoneApp : MondafoneApp() {
        val whitelisted = mutableSetOf<String>()
        val pendingNumbers = mutableListOf<String>()
        val ownReported = mutableSetOf<String>()
        val foreignReports = mutableMapOf<String, Int>()
        val blockedNumbers = mutableListOf<String>()
        val promptedNumbers = mutableListOf<String>()

        override fun onCreate() = Unit

        override val verdictStore: CallVerdictStore = object : CallVerdictStore {
            override fun isWhitelisted(number: String) = number in whitelisted
            override fun isBlacklisted(number: String) = false
            override fun isClassified(number: String) = number in whitelisted
            override fun addToWhitelist(number: String) {
                whitelisted += number
                pendingNumbers -= number
            }
            override fun addToBlacklist(number: String) = Unit
            override fun addPending(number: String) {
                if (!isClassified(number)) pendingNumbers += number
            }
            override fun pendingNumbers(): List<String> = pendingNumbers
            override fun whitelistNumbers(): List<String> = whitelisted.toList()
            override fun blacklistNumbers(): List<String> = emptyList()
            override fun remove(number: String) {
                whitelisted -= number
                pendingNumbers -= number
            }
            override fun stats(): VerdictStats = VerdictStats(whitelisted.size, 0)
            override fun clear() {
                whitelisted.clear()
                pendingNumbers.clear()
            }
            override fun registerOnChangeListener(listener: () -> Unit) {}
            override fun unregisterOnChangeListener() {}
        }

        override val spamReportStore: SpamReportStore = object : SpamReportStore {
            override fun reportOwn(canonicalNumber: String, displayNumber: String): SpamReport? {
                ownReported += canonicalNumber
                return null
            }
            override fun acceptRemoteReport(report: SpamReport): Boolean = false
            override fun isReportedByMe(canonicalNumber: String) = canonicalNumber in ownReported
            override fun foreignReportCount(canonicalNumber: String) = foreignReports[canonicalNumber] ?: 0
            override fun ownReports(): List<OwnSpamReport> = emptyList()
            override fun removeOwnReport(phoneHash: String) = Unit
            override fun registerOnChangeListener(listener: () -> Unit) {}
            override fun unregisterOnChangeListener() {}
        }

        override val peerStatusStore: PeerStatusStore = object : PeerStatusStore {
            private val peers = mutableMapOf<String, PeerStatus>()
            override fun upsert(peer: PeerStatus) {
                peers[peer.idPubkey] = peer
            }
            override fun find(idPubkey: String): PeerStatus? = peers[idPubkey]
            override fun all(): List<PeerStatus> = peers.values.toList()
        }

        override val phoneNumberNormalizer: PhoneNumberNormalizer =
            PhoneNumberNormalizer(object : RegionProvider {
                override fun regionIso(): String = "ES"
            })

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

    private companion object {
        const val NATIONAL = "600111222"
        const val CANONICAL = "0034600111222"
    }
}
