package dev.polyrics.mondafone.spam

import dev.polyrics.mondafone.identity.DeviceIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

abstract class SpamReportStoreContractTest {

    protected lateinit var identity: TestIdentity
    private lateinit var store: SpamReportStore

    protected abstract fun createStore(identity: DeviceIdentity): SpamReportStore

    @Before
    fun setUp() {
        identity = TestIdentity()
        store = createStore(identity)
    }

    @Test
    fun reportOwnStoresReportedNumber() {
        val report = store.reportOwn(CANONICAL_A, DISPLAY_A)

        assertNotNull(report)
        assertEquals(hashOf(CANONICAL_A), report?.phoneHash)
        assertTrue(store.isReportedByMe(CANONICAL_A))
        assertFalse(store.isReportedByMe(CANONICAL_B))
    }

    @Test
    fun ownReportsExposeDisplayNumber() {
        store.reportOwn(CANONICAL_A, DISPLAY_A)

        assertEquals(listOf(DISPLAY_A), store.ownReports().map { it.displayNumber })
    }

    @Test
    fun ownSignatureIsVerifiable() {
        val report = requireNotNull(store.reportOwn(CANONICAL_A, DISPLAY_A))

        assertTrue(
            ReportSignature.verify(
                report.phoneHash,
                report.createdAt,
                report.seqNum,
                report.signature,
                report.createdBy,
            ),
        )
    }

    @Test
    fun ownSequenceNumbersIncreasePerReport() {
        val first = requireNotNull(store.reportOwn(CANONICAL_A, DISPLAY_A))
        val second = requireNotNull(store.reportOwn(CANONICAL_B, DISPLAY_B))

        assertEquals(1L, first.seqNum)
        assertEquals(2L, second.seqNum)
    }

    @Test
    fun reportingSameNumberAgainKeepsSingleReportAndSequence() {
        val first = requireNotNull(store.reportOwn(CANONICAL_A, DISPLAY_A))
        val again = requireNotNull(store.reportOwn(CANONICAL_A, DISPLAY_A))

        assertEquals(first.seqNum, again.seqNum)
        assertEquals(1, store.ownReports().size)

        val next = requireNotNull(store.reportOwn(CANONICAL_B, DISPLAY_B))
        assertEquals(2L, next.seqNum)
    }

    @Test
    fun ownReportsDoNotCountAsForeignReports() {
        store.reportOwn(CANONICAL_A, DISPLAY_A)

        assertEquals(0, store.foreignReportCount(CANONICAL_A))
    }

    @Test
    fun acceptsVerifiedRemoteReport() {
        val remote = TestIdentity().signedReport(hashOf(CANONICAL_A), 1L, NOW)

        assertTrue(store.acceptRemoteReport(remote))
        assertEquals(1, store.foreignReportCount(CANONICAL_A))
    }

    @Test
    fun countsDistinctRemoteReporters() {
        val remoteOne = TestIdentity()
        val remoteTwo = TestIdentity()

        store.acceptRemoteReport(remoteOne.signedReport(hashOf(CANONICAL_A), 1L, NOW))
        store.acceptRemoteReport(remoteTwo.signedReport(hashOf(CANONICAL_A), 1L, NOW))

        assertEquals(2, store.foreignReportCount(CANONICAL_A))
    }

    @Test
    fun rejectsRemoteReportWithInvalidSignature() {
        val remote = TestIdentity().signedReport(hashOf(CANONICAL_A), 1L, NOW)
        val tampered = remote.copy(createdAt = NOW + 1)

        assertFalse(store.acceptRemoteReport(tampered))
        assertEquals(0, store.foreignReportCount(CANONICAL_A))
    }

    @Test
    fun deduplicatesRemoteReportsFromSamePeer() {
        val remote = TestIdentity()
        val report = remote.signedReport(hashOf(CANONICAL_A), 1L, NOW)

        assertTrue(store.acceptRemoteReport(report))
        assertFalse(store.acceptRemoteReport(report))
        assertEquals(1, store.foreignReportCount(CANONICAL_A))
    }

    @Test
    fun remoteReportsAreNotListedAsOwnReports() {
        store.acceptRemoteReport(TestIdentity().signedReport(hashOf(CANONICAL_A), 1L, NOW))

        assertTrue(store.ownReports().isEmpty())
        assertFalse(store.isReportedByMe(CANONICAL_A))
    }

    @Test
    fun removeOwnReportDeletesReportAndDisplay() {
        store.reportOwn(CANONICAL_A, DISPLAY_A)

        store.removeOwnReport(hashOf(CANONICAL_A))

        assertFalse(store.isReportedByMe(CANONICAL_A))
        assertTrue(store.ownReports().isEmpty())
    }

    @Test
    fun removeOwnReportKeepsSequenceMonotonic() {
        store.reportOwn(CANONICAL_A, DISPLAY_A)
        store.removeOwnReport(hashOf(CANONICAL_A))

        val next = requireNotNull(store.reportOwn(CANONICAL_B, DISPLAY_B))

        assertEquals(2L, next.seqNum)
    }

    @Test
    fun notifiesListenerOnMutations() {
        var changes = 0
        store.registerOnChangeListener { changes++ }

        store.reportOwn(CANONICAL_A, DISPLAY_A)
        store.acceptRemoteReport(TestIdentity().signedReport(hashOf(CANONICAL_B), 1L, NOW))
        store.removeOwnReport(hashOf(CANONICAL_A))

        assertEquals(3, changes)
    }

    protected fun hashOf(canonicalNumber: String): String = SpamReportHasher(SECRET).hash(canonicalNumber)

    protected companion object {
        const val SECRET = "test-secret"
        const val CANONICAL_A = "00573001234567"
        const val CANONICAL_B = "00573001234568"
        const val DISPLAY_A = "+57 3001234567"
        const val DISPLAY_B = "+57 3001234568"
        const val NOW = 1_700_000_000_000L
    }
}
