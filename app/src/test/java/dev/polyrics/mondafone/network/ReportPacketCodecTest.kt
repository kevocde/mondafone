package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import dev.polyrics.mondafone.spam.SpamReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportPacketCodecTest {

    private val encoder = Base64UrlEncoder()
    private val codec = ReportPacketCodec()

    @Test
    fun roundTripPreservesReport() {
        val report = report(seqNum = 7L)

        assertEquals(report, codec.decode(codec.encode(report)))
    }

    @Test
    fun roundTripPreservesLargeSequence() {
        val report = report(seqNum = 0xFFFFFFFFL)

        assertEquals(report, codec.decode(codec.encode(report)))
    }

    @Test
    fun decodeRejectsTruncatedPacket() {
        val encoded = codec.encode(report(seqNum = 7L))

        assertNull(codec.decode(encoded.copyOf(encoded.size - 1)))
    }

    @Test
    fun decodeRejectsTrailingBytes() {
        val encoded = codec.encode(report(seqNum = 7L))

        assertNull(codec.decode(encoded + byteArrayOf(0x00)))
    }

    @Test
    fun frameSizeReportsIncompleteWhileBufferIsShort() {
        val encoded = codec.encode(report(seqNum = 7L))

        assertEquals(ReportPacketCodec.FrameSize.Incomplete, codec.frameSize(encoded.copyOf(10)))
    }

    @Test
    fun frameSizeReportsCompleteLength() {
        val encoded = codec.encode(report(seqNum = 7L))

        assertEquals(ReportPacketCodec.FrameSize.Complete(encoded.size), codec.frameSize(encoded))
    }

    @Test
    fun frameSizeRejectsEmptyPubkey() {
        assertEquals(ReportPacketCodec.FrameSize.Invalid, codec.frameSize(byteArrayOf(0x00)))
    }

    private fun report(seqNum: Long): SpamReport = SpamReport(
        phoneHash = encoder.encode(ByteArray(32) { 0x11 }),
        createdBy = encoder.encode(ByteArray(91) { 0x22 }),
        seqNum = seqNum,
        createdAt = 1_700_000_000_000L,
        signature = encoder.encode(ByteArray(72) { 0x33 }),
    )
}
