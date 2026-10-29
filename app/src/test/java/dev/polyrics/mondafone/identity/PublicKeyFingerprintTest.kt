package dev.polyrics.mondafone.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PublicKeyFingerprintTest {

    private val fingerprint = PublicKeyFingerprint()

    @Test
    fun derivesStableUrlSafeFingerprint() {
        assertEquals("ungWv48Bzw", fingerprint.of("abc".toByteArray()))
    }

    @Test
    fun sameKeyProducesSameFingerprint() {
        val key = byteArrayOf(1, 2, 3, 4)

        assertEquals(fingerprint.of(key), fingerprint.of(key))
    }

    @Test
    fun differentKeysProduceDifferentFingerprints() {
        assertNotEquals(
            fingerprint.of(byteArrayOf(1, 2, 3)),
            fingerprint.of(byteArrayOf(1, 2, 4)),
        )
    }

    @Test
    fun fingerprintHasFixedLength() {
        assertEquals(10, fingerprint.of(byteArrayOf(9)).length)
    }
}
