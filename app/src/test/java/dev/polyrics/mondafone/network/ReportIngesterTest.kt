package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SpamReport
import dev.polyrics.mondafone.spam.TestIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportIngesterTest {

    private val identity = TestIdentity()
    private val persisted = mutableListOf<SpamReport>()
    private val ingester = ReportIngester(
        validator = ReportValidator(maxAgeMillis = MAX_AGE_MILLIS),
        persister = { report -> persisted.add(report) },
        peerMaxSeqProvider = { 0L },
        clock = { NOW },
    )

    @Test
    fun acceptsAndPersistsValidReports() {
        ingester.ingest(listOf(report(1L), report(2L)))

        assertEquals(2, persisted.size)
        assertEquals(IngestionSummary(accepted = 2, dropped = 0), ingester.summary())
    }

    @Test
    fun advancesLastSequenceWithinSession() {
        ingester.ingest(report(5L))
        ingester.ingest(report(5L))

        assertEquals(IngestionSummary(accepted = 1, dropped = 1), ingester.summary())
        assertEquals(1, persisted.size)
    }

    @Test
    fun usesPersistedMaxSequence() {
        val strict = ReportIngester(
            validator = ReportValidator(MAX_AGE_MILLIS),
            persister = { true },
            peerMaxSeqProvider = { 3L },
            clock = { NOW },
        )

        strict.ingest(report(3L))
        strict.ingest(report(4L))

        assertEquals(IngestionSummary(accepted = 1, dropped = 1), strict.summary())
    }

    @Test
    fun dropsTamperedSignature() {
        val tampered = report(1L).copy(createdAt = NOW + 1)

        ingester.ingest(tampered)

        assertEquals(IngestionSummary(accepted = 0, dropped = 1), ingester.summary())
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun dropsExpiredReport() {
        ingester.ingest(report(1L, createdAt = NOW - MAX_AGE_MILLIS - 1))

        assertEquals(IngestionSummary(accepted = 0, dropped = 1), ingester.summary())
    }

    @Test
    fun dropsReportFromFuture() {
        val future = NOW + ReportValidator.FUTURE_TOLERANCE_MILLIS + 1

        ingester.ingest(report(1L, createdAt = future))

        assertEquals(IngestionSummary(accepted = 0, dropped = 1), ingester.summary())
    }

    @Test
    fun countsPersistenceRejectionAsDropped() {
        val rejecting = ReportIngester(
            validator = ReportValidator(MAX_AGE_MILLIS),
            persister = { false },
            peerMaxSeqProvider = { 0L },
            clock = { NOW },
        )

        rejecting.ingest(report(1L))

        assertEquals(IngestionSummary(accepted = 0, dropped = 1), rejecting.summary())
    }

    @Test
    fun summaryLineIsNullWithoutActivity() {
        assertNull(ingester.summaryLine(total = 7))
    }

    @Test
    fun summaryLineReportsAcceptedAndDropped() {
        ingester.ingest(report(1L))
        ingester.ingest(report(2L).copy(createdAt = NOW + 1))

        assertEquals("sync: got 1, dropped 1, total 7", ingester.summaryLine(total = 7))
    }

    @Test
    fun summaryLineOmitsDroppedWhenZero() {
        ingester.ingest(report(1L))

        assertEquals("sync: got 1, total 3", ingester.summaryLine(total = 3))
        assertFalse(ingester.summaryLine(total = 3)!!.contains("dropped"))
    }

    private fun report(seqNum: Long, createdAt: Long = NOW): SpamReport =
        identity.signedReport(phoneHash = "phone-hash", seqNum = seqNum, createdAt = createdAt)

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val MAX_AGE_MILLIS = 20L * 24 * 60 * 60 * 1000
    }
}
