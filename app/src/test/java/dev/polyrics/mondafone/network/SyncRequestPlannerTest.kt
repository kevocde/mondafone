package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SequenceInventoryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRequestPlannerTest {

    private val planner = SyncRequestPlanner()

    @Test
    fun requestsWholeRangeForUnknownReporter() {
        val requests = planner.plan(
            remote = listOf(SequenceInventoryEntry("peer", 5L)),
            local = emptyList(),
            ownPubkey = OWN,
        )

        assertEquals(listOf(ReportRequest("peer", 1L, 5L)), requests)
    }

    @Test
    fun requestsOnlyMissingRangeForKnownReporter() {
        val requests = planner.plan(
            remote = listOf(SequenceInventoryEntry("peer", 8L)),
            local = listOf(SequenceInventoryEntry("peer", 3L)),
            ownPubkey = OWN,
        )

        assertEquals(listOf(ReportRequest("peer", 4L, 8L)), requests)
    }

    @Test
    fun skipsReporterAlreadyUpToDate() {
        val requests = planner.plan(
            remote = listOf(SequenceInventoryEntry("peer", 3L)),
            local = listOf(SequenceInventoryEntry("peer", 3L)),
            ownPubkey = OWN,
        )

        assertEquals(emptyList<ReportRequest>(), requests)
    }

    @Test
    fun skipsReporterAheadOfRemote() {
        val requests = planner.plan(
            remote = listOf(SequenceInventoryEntry("peer", 2L)),
            local = listOf(SequenceInventoryEntry("peer", 7L)),
            ownPubkey = OWN,
        )

        assertEquals(emptyList<ReportRequest>(), requests)
    }

    @Test
    fun skipsOwnPubkey() {
        val requests = planner.plan(
            remote = listOf(SequenceInventoryEntry(OWN, 9L)),
            local = emptyList(),
            ownPubkey = OWN,
        )

        assertEquals(emptyList<ReportRequest>(), requests)
    }

    @Test
    fun plansOnlyReportersWithPendingSequences() {
        val requests = planner.plan(
            remote = listOf(
                SequenceInventoryEntry("peer-a", 2L),
                SequenceInventoryEntry("peer-b", 1L),
                SequenceInventoryEntry("peer-c", 6L),
            ),
            local = listOf(
                SequenceInventoryEntry("peer-a", 2L),
                SequenceInventoryEntry("peer-b", 0L),
            ),
            ownPubkey = OWN,
        )

        assertEquals(
            listOf(
                ReportRequest("peer-b", 1L, 1L),
                ReportRequest("peer-c", 1L, 6L),
            ),
            requests,
        )
    }

    private companion object {
        const val OWN = "my-pubkey"
    }
}
