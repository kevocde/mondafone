package dev.polyrics.mondafone.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportRequestCodecTest {

    private val codec = ReportRequestCodec()

    @Test
    fun roundTripPreservesRequests() {
        val requests = listOf(
            ReportRequest("peer-a", 1L, 5L),
            ReportRequest("peer-b", 4L, 9L),
        )

        assertEquals(requests, codec.decode(codec.encode(requests)))
    }

    @Test
    fun roundTripPreservesEmptyPipe() {
        assertEquals(emptyList<ReportRequest>(), codec.decode(codec.encode(emptyList())))
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
    fun decodeRejectsUnknownFrameType() {
        val encoded = codec.encode(listOf(ReportRequest("peer-a", 1L, 5L)))
        encoded[0] = 0x7F

        assertNull(codec.decode(encoded))
    }

    @Test
    fun decodeRejectsTruncatedPayload() {
        val encoded = codec.encode(listOf(ReportRequest("peer-a", 1L, 5L)))

        assertNull(codec.decode(encoded.copyOf(encoded.size - 1)))
    }

    @Test
    fun decodeRejectsTrailingBytes() {
        val encoded = codec.encode(listOf(ReportRequest("peer-a", 1L, 5L)))

        assertNull(codec.decode(encoded + byteArrayOf(0x00)))
    }
}
