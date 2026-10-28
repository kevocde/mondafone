package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Test

class BeaconCycleCounterTest {

    private val counter = BeaconCycleCounter()

    @Test
    fun startsAtZero() {
        assertEquals(0, counter.count())
    }

    @Test
    fun countsEachRecordedAdvertising() {
        counter.record()
        counter.record()
        counter.record()

        assertEquals(3, counter.count())
    }

    @Test
    fun resetStartsANewWindow() {
        counter.record()

        counter.reset()

        assertEquals(0, counter.count())
    }
}
