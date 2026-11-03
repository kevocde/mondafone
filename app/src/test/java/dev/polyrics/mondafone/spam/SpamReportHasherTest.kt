package dev.polyrics.mondafone.spam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpamReportHasherTest {

    private val hasher = SpamReportHasher("test-secret")

    @Test
    fun hashesKnownCanonicalNumber() {
        assertEquals(
            "tp8KirWyY4taMv80FI2CSzrWThpYAw0Eht7uAuUjICU",
            hasher.hash("00573001234567"),
        )
    }

    @Test
    fun hashIsStableForSameInput() {
        assertEquals(hasher.hash("00573001234567"), hasher.hash("00573001234567"))
    }

    @Test
    fun differentNumbersProduceDifferentHashes() {
        assertNotEquals(hasher.hash("00573001234567"), hasher.hash("00573001234568"))
    }

    @Test
    fun differentSecretsProduceDifferentHashes() {
        assertNotEquals(hasher.hash("00573001234567"), SpamReportHasher("other-secret").hash("00573001234567"))
    }

    @Test
    fun keepsFullSha256WithoutPadding() {
        val hash = hasher.hash("00573001234567")
        assertEquals(43, hash.length)
        assertEquals(false, hash.contains('='))
        assertEquals(false, hash.contains('+'))
        assertEquals(false, hash.contains('/'))
    }
}
