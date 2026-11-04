package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import dev.polyrics.mondafone.spam.SpamReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportStreamDecoderTest {

    private val encoder = Base64UrlEncoder()
    private val codec = ReportPacketCodec()
    private val decoder = ReportStreamDecoder()

    @Test
    fun decodesSinglePacket() {
        val report = report(1L)

        assertEquals(listOf(report), decoder.feed(codec.encode(report)))
    }

    @Test
    fun decodesSeveralPacketsInOneFeed() {
        val first = report(1L)
        val second = report(2L)
        val payload = codec.encode(first) + codec.encode(second)

        assertEquals(listOf(first, second), decoder.feed(payload))
    }

    @Test
    fun waitsForPacketSplitAcrossFeeds() {
        val report = report(1L)
        val encoded = codec.encode(report)

        assertTrue(decoder.feed(encoded.copyOf(20)).isEmpty())
        assertEquals(listOf(report), decoder.feed(encoded.copyOfRange(20, encoded.size)))
    }

    @Test
    fun decodesPacketSplitAtEveryByte() {
        val report = report(3L)
        val encoded = codec.encode(report)
        val decoded = encoded.flatMap { byte -> decoder.feed(byteArrayOf(byte)) }

        assertEquals(listOf(report), decoded)
    }

    @Test
    fun discardsMalformedFrameAndKeepsDecoding() {
        decoder.feed(byteArrayOf(0x00))

        assertEquals(listOf(report(4L)), decoder.feed(codec.encode(report(4L))))
    }

    private fun report(seqNum: Long): SpamReport = SpamReport(
        phoneHash = encoder.encode(ByteArray(32) { 0x11 }),
        createdBy = encoder.encode(ByteArray(91) { 0x22 }),
        seqNum = seqNum,
        createdAt = 1_700_000_000_000L,
        signature = encoder.encode(ByteArray(72) { 0x33 }),
    )
}
