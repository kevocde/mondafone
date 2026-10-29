package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BeaconPayloadCodecTest {

    private val codec = BeaconPayloadCodec()

    @Test
    fun decodeRestoresEncodedId() {
        val encoded = codec.encode("device-01")

        assertEquals("device-01", codec.decode(encoded))
    }

    @Test
    fun decodeReturnsNullForMissingData() {
        assertNull(codec.decode(null))
    }

    @Test
    fun decodeReturnsNullForEmptyData() {
        assertNull(codec.decode(ByteArray(0)))
    }

    @Test
    fun decodeReturnsNullForUnprintableData() {
        assertNull(codec.decode(byteArrayOf(0x00, 0x01, 0x02)))
    }

    @Test
    fun acceptsIdThatFitsPayload() {
        assertTrue(codec.fits("device-01"))
    }

    @Test
    fun rejectsIdLongerThanPayloadLimit() {
        val tooLong = "a".repeat(BeaconPayloadCodec.MAX_ID_BYTES + 1)

        assertEquals(false, codec.fits(tooLong))
    }
}
