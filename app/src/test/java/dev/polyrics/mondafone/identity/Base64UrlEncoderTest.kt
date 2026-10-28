package dev.polyrics.mondafone.identity

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Base64UrlEncoderTest {

    private val encoder = Base64UrlEncoder()

    @Test
    fun encodeUsesUrlSafeAlphabet() {
        val encoded = encoder.encode(byteArrayOf(0xFB.toByte(), 0xFF.toByte(), 0xBF.toByte()))

        assertEquals("-_-_", encoded)
    }

    @Test
    fun encodeOmitsPadding() {
        val encoded = encoder.encode(byteArrayOf(1, 2, 3))

        assertFalse(encoded.contains('='))
    }

    @Test
    fun decodeRestoresOriginalBytes() {
        val bytes = byteArrayOf(0xFB.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0x00)

        assertArrayEquals(bytes, encoder.decode(encoder.encode(bytes)))
    }

    @Test
    fun encodeEmptyArrayIsEmptyString() {
        assertEquals("", encoder.encode(ByteArray(0)))
    }

    @Test
    fun roundTripPreservesArbitraryBytes() {
        val bytes = ByteArray(256) { it.toByte() }

        assertArrayEquals(bytes, encoder.decode(encoder.encode(bytes)))
    }
}
