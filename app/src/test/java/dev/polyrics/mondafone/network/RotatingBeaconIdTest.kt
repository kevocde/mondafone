package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotatingBeaconIdTest {

    private val rotator = RotatingBeaconId("test-secret")

    @Test
    fun derivesKnownVector() {
        assertEquals("tsrUYjlXng", rotator.of("fingerprint-x", 490000L))
    }

    @Test
    fun staysStableWithinTheSameHour() {
        val hourStart = 490000L * MILLIS_PER_HOUR

        assertEquals(
            rotator.current("fingerprint-x", hourStart),
            rotator.current("fingerprint-x", hourStart + MILLIS_PER_HOUR - 1),
        )
    }

    @Test
    fun rotatesAcrossHours() {
        assertNotEquals(rotator.of("fingerprint-x", 490000L), rotator.of("fingerprint-x", 490001L))
    }

    @Test
    fun distinctIdsForDistinctFingerprints() {
        assertNotEquals(rotator.of("fingerprint-a", 490000L), rotator.of("fingerprint-b", 490000L))
    }

    @Test
    fun idHasFixedLength() {
        assertEquals(10, rotator.of("fingerprint-x", 490000L).length)
    }

    @Test
    fun candidatesIncludeNeighbourHours() {
        val candidates = rotator.candidates("fingerprint-x", 490000L * MILLIS_PER_HOUR)

        assertEquals(3, candidates.size)
        assertTrue(candidates.contains(rotator.of("fingerprint-x", 489999L)))
        assertTrue(candidates.contains(rotator.of("fingerprint-x", 490000L)))
        assertTrue(candidates.contains(rotator.of("fingerprint-x", 490001L)))
    }

    @Test
    fun candidatesRespectTolerance() {
        val now = 490000L * MILLIS_PER_HOUR

        assertFalse(rotator.candidates("fingerprint-x", now).contains(rotator.of("fingerprint-x", 490002L)))
        assertTrue(
            rotator.candidates("fingerprint-x", now, toleranceHours = 2)
                .contains(rotator.of("fingerprint-x", 490002L)),
        )
    }

    @Test
    fun matchesCurrentId() {
        val now = 490000L * MILLIS_PER_HOUR

        assertTrue(rotator.matches("fingerprint-x", rotator.of("fingerprint-x", 490000L), now))
    }

    @Test
    fun rejectsForeignId() {
        assertFalse(rotator.matches("fingerprint-x", "foreign-id", 490000L * MILLIS_PER_HOUR))
    }

    private companion object {
        const val MILLIS_PER_HOUR = 3_600_000L
    }
}
