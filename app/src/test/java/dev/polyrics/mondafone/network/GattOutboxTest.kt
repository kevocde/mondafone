package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GattOutboxTest {

    private val outbox = GattOutbox()

    @Test
    fun dispatchesPacketsOneByOne() {
        val sent = mutableListOf<String>()

        outbox.enqueue("peer-1", { sent.add("p1-a"); true })
        outbox.enqueue("peer-1", { sent.add("p1-b"); true })
        outbox.enqueue("peer-2", { sent.add("p2-a"); true })

        assertEquals(listOf("p1-a"), sent)

        outbox.onPacketSent()
        assertEquals(listOf("p1-a", "p1-b"), sent)

        outbox.onPacketSent()
        assertEquals(listOf("p1-a", "p1-b", "p2-a"), sent)
    }

    @Test
    fun dropsRemainingTagPacketsAndContinuesWhenSendFails() {
        val sent = mutableListOf<String>()
        var failed = false

        outbox.enqueue("peer-0", { sent.add("p0"); true })
        outbox.enqueue("peer-1", { false }, onFailure = { failed = true })
        outbox.enqueue("peer-1", { sent.add("p1-b"); true })
        outbox.enqueue("peer-2", { sent.add("p2-a"); true })

        outbox.onPacketSent()

        assertTrue(failed)
        assertEquals(listOf("p0", "p2-a"), sent)
    }

    @Test
    fun cancelRemovesQueuedPacketsAndAdvancesIfActiveTagCancelled() {
        val sent = mutableListOf<String>()

        outbox.enqueue("peer-1", { sent.add("p1-a"); true })
        outbox.enqueue("peer-1", { sent.add("p1-b"); true })
        outbox.enqueue("peer-2", { sent.add("p2-a"); true })

        outbox.cancel("peer-1")

        assertEquals(listOf("p1-a", "p2-a"), sent)
    }
}
