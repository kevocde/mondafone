package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CentralSyncQueueTest {

    private val queue = CentralSyncQueue<String>()

    @Test
    fun returnsNullWhenEmpty() {
        assertNull(queue.pollNext())
        assertFalse(queue.hasPending())
    }

    @Test
    fun enqueuesAndPollsInFifoOrder() {
        val first = CentralPeer("peer1", "AA:BB:CC:11", "dev1")
        val second = CentralPeer("peer2", "AA:BB:CC:22", "dev2")

        assertTrue(queue.enqueue(first))
        assertTrue(queue.enqueue(second))
        assertTrue(queue.hasPending())

        assertEquals(first, queue.pollNext())
        assertEquals(first, queue.activePeer())
        assertNull(queue.pollNext())

        assertEquals(first, queue.completeCurrent())
        assertNull(queue.activePeer())

        assertEquals(second, queue.pollNext())
        assertEquals(second, queue.activePeer())
        assertEquals(second, queue.completeCurrent())

        assertNull(queue.pollNext())
        assertFalse(queue.hasPending())
    }

    @Test
    fun rejectsDuplicateAddress() {
        val peer = CentralPeer("peer1", "AA:BB:CC:11", "dev1")
        val duplicate = CentralPeer("peer1_alt", "AA:BB:CC:11", "dev1_alt")

        assertTrue(queue.enqueue(peer))
        assertFalse(queue.enqueue(duplicate))
    }

    @Test
    fun rejectsDuplicateAddressEvenAfterCompleted() {
        val peer = CentralPeer("peer1", "AA:BB:CC:11", "dev1")

        assertTrue(queue.enqueue(peer))
        assertEquals(peer, queue.pollNext())
        assertEquals(peer, queue.completeCurrent())

        assertFalse(queue.enqueue(peer))
    }

    @Test
    fun clearsQueueAndActivePeer() {
        val peer = CentralPeer("peer1", "AA:BB:CC:11", "dev1")
        queue.enqueue(peer)
        queue.pollNext()

        queue.clear()

        assertNull(queue.activePeer())
        assertNull(queue.pollNext())
        assertFalse(queue.hasPending())
    }
}
