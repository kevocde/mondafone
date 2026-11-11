package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SequenceInventoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SequenceInventoryCodecTest {

    private val codec = SequenceInventoryCodec()

    @Test
    fun roundTripPreservesEntries() {
        val entries = listOf(
            SequenceInventoryEntry("pubkey-one", 3L),
            SequenceInventoryEntry("pubkey-two", 42L),
        )

        assertEquals(entries, codec.decode(codec.encode(entries)))
    }

    @Test
    fun roundTripPreservesEmptyInventory() {
        assertEquals(emptyList<SequenceInventoryEntry>(), codec.decode(codec.encode(emptyList())))
    }

    @Test
    fun decodeRejectsMissingData() {
        assertNull(codec.decode(null))
    }

    @Test
    fun decodeRejectsEmptyData() {
        assertNull(codec.decode(ByteArray(0)))
    }

    @Test
    fun decodeRejectsTruncatedPayload() {
        val encoded = codec.encode(listOf(SequenceInventoryEntry("pubkey-one", 3L)))

        assertNull(codec.decode(encoded.copyOf(encoded.size - 1)))
    }

    @Test
    fun decodeRejectsTrailingBytes() {
        val encoded = codec.encode(listOf(SequenceInventoryEntry("pubkey-one", 3L)))

        assertNull(codec.decode(encoded + byteArrayOf(0x00)))
    }

    @Test
    fun decodeRejectsEmptyPubkey() {
        val malformed = byteArrayOf(0x00, 0x01, 0x00, 0, 0, 0, 0, 0, 0, 0, 1)

        assertNull(codec.decode(malformed))
    }
}
