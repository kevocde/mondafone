package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTurnCoordinatorTest {

    private val coordinator = SyncTurnCoordinator()

    @Test
    fun grantsTurnImmediatelyWhenIdle() {
        val events = mutableListOf<String>()

        val granted = coordinator.requestTurn(
            id = "peer-1",
            onWait = { events.add("wait-1") },
            onReady = { events.add("ready-1") },
        )

        assertTrue(granted)
        assertEquals(listOf("ready-1"), events)
    }

    @Test
    fun queuesSecondRequesterAndNotifiesWaitThenReadyOnRelease() {
        val events = mutableListOf<String>()

        coordinator.requestTurn("peer-1", onReady = { events.add("ready-1") })
        val secondGranted = coordinator.requestTurn(
            id = "peer-2",
            onWait = { events.add("wait-2") },
            onReady = { events.add("ready-2") },
        )
        coordinator.requestTurn(
            id = "peer-3",
            onWait = { events.add("wait-3") },
            onReady = { events.add("ready-3") },
        )

        assertFalse(secondGranted)
        assertEquals(listOf("ready-1", "wait-2", "wait-3"), events)

        coordinator.releaseTurn("peer-1")
        assertEquals(listOf("ready-1", "wait-2", "wait-3", "ready-2"), events)

        coordinator.releaseTurn("peer-2")
        assertEquals(listOf("ready-1", "wait-2", "wait-3", "ready-2", "ready-3"), events)
    }

    @Test
    fun releasingQueuedRequesterRemovesItBeforeTurnArrives() {
        val events = mutableListOf<String>()

        coordinator.requestTurn("peer-1", onReady = { events.add("ready-1") })
        coordinator.requestTurn("peer-2", onReady = { events.add("ready-2") })
        coordinator.requestTurn("peer-3", onReady = { events.add("ready-3") })

        coordinator.releaseTurn("peer-2")
        coordinator.releaseTurn("peer-1")

        assertEquals(listOf("ready-1", "ready-3"), events)
    }
}
