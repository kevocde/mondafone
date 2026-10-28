package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryBeaconEventLogTest {

    private val log = InMemoryBeaconEventLog(maxLines = 3) { "12:00:00" }

    @Test
    fun logsEventWithTimestampPrefix() {
        log.log("Beacon active")

        assertEquals(listOf("[12:00:00] Beacon active"), log.lines())
    }

    @Test
    fun keepsOnlyMostRecentLines() {
        log.log("first")
        log.log("second")
        log.log("third")
        log.log("fourth")

        assertEquals(
            listOf(
                "[12:00:00] second",
                "[12:00:00] third",
                "[12:00:00] fourth",
            ),
            log.lines(),
        )
    }

    @Test
    fun notifiesListenerWithCurrentLines() {
        var received: List<String> = emptyList()
        log.setListener { received = it }

        log.log("Beacon active")

        assertEquals(listOf("[12:00:00] Beacon active"), received)
    }

    @Test
    fun stopsNotifyingAfterListenerRemoved() {
        var notified = false
        log.setListener { notified = true }
        log.setListener(null)
        notified = false

        log.log("Beacon active")

        assertTrue(!notified)
    }
}
