package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeaconCycleCounterTest {

    private val counter = BeaconCycleCounter()

    @Test
    fun startsAtZero() {
        assertEquals(0, counter.count())
    }

    @Test
    fun countsEachDistinctId() {
        counter.record("device-a")
        counter.record("device-b")
        counter.record("device-c")

        assertEquals(3, counter.count())
    }

    @Test
    fun ignoresRepeatedIds() {
        counter.record("device-a")
        counter.record("device-a")

        assertEquals(1, counter.count())
    }

    @Test
    fun recordReturnsTrueOnlyForNewIds() {
        assertTrue(counter.record("device-a"))
        assertFalse(counter.record("device-a"))
        assertTrue(counter.record("device-b"))
    }
}
