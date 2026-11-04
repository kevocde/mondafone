package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import dev.polyrics.mondafone.spam.SequenceInventoryEntry
import dev.polyrics.mondafone.spam.SpamReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataPipeCodecTest {

    private val encoder = Base64UrlEncoder()
    private val codec = DataPipeCodec()

    @Test
    fun roundTripPreservesRequests() {
        val message = DataPipeMessage.Requests(listOf(ReportRequest("peer-a", 1L, 5L)))

        assertEquals(message, codec.decode(codec.encode(message, MAX_PAYLOAD).single()))
    }

    @Test
    fun roundTripPreservesOfferState() {
        val message = DataPipeMessage.OfferState(listOf(SequenceInventoryEntry("peer-a", 5L)))

        assertEquals(message, codec.decode(codec.encode(message, MAX_PAYLOAD).single()))
    }

    @Test
    fun roundTripPreservesReports() {
        val message = DataPipeMessage.Reports(listOf(report(1L), report(2L)))

        assertEquals(message, codec.decode(codec.encode(message, MAX_PAYLOAD).single()))
    }

    @Test
    fun roundTripPreservesEnd() {
        assertEquals(DataPipeMessage.End, codec.decode(codec.encode(DataPipeMessage.End, MAX_PAYLOAD).single()))
    }

    @Test
    fun emptyRequestsBecomeOneFrame() {
        val frames = codec.encode(DataPipeMessage.Requests(emptyList()), MAX_PAYLOAD)

        assertEquals(1, frames.size)
        assertEquals(DataPipeMessage.Requests(emptyList()), codec.decode(frames.single()))
    }

    @Test
    fun emptyReportsProduceNoFrames() {
        assertTrue(codec.encode(DataPipeMessage.Reports(emptyList()), MAX_PAYLOAD).isEmpty())
    }

    @Test
    fun splitsReportsToFitPayloadLimit() {
        val reports = listOf(report(1L), report(2L), report(3L))

        val frames = codec.encode(DataPipeMessage.Reports(reports), maxPayload = 300)

        assertEquals(3, frames.size)
        assertTrue(frames.all { it.size <= 300 })
        assertEquals(reports, decodedReports(frames))
    }

    @Test
    fun splitsRequestsToFitPayloadLimit() {
        val requests = (1..20).map { ReportRequest("peer-$it", it.toLong(), it.toLong()) }

        val frames = codec.encode(DataPipeMessage.Requests(requests), maxPayload = 100)

        assertTrue(frames.size > 1)
        assertTrue(frames.all { it.size <= 100 })
        assertEquals(requests, frames.flatMap { (codec.decode(it) as DataPipeMessage.Requests).requests })
    }

    @Test
    fun decodeRejectsMissingData() {
        assertNull(codec.decode(null))
        assertNull(codec.decode(ByteArray(0)))
    }

    @Test
    fun decodeRejectsUnknownFrameType() {
        assertNull(codec.decode(byteArrayOf(0x7F)))
    }

    @Test
    fun decodeRejectsEndWithBody() {
        assertNull(codec.decode(byteArrayOf(0x04, 0x00)))
    }

    @Test
    fun decodeRejectsTruncatedReportsBody() {
        val frame = codec.encode(DataPipeMessage.Reports(listOf(report(1L))), MAX_PAYLOAD).single()

        assertNull(codec.decode(frame.copyOf(frame.size - 1)))
    }

    private fun decodedReports(frames: List<ByteArray>): List<SpamReport> =
        frames.flatMap { (codec.decode(it) as DataPipeMessage.Reports).reports }

    private fun report(seqNum: Long): SpamReport = SpamReport(
        phoneHash = encoder.encode(ByteArray(32) { 0x11 }),
        createdBy = encoder.encode(ByteArray(91) { 0x22 }),
        seqNum = seqNum,
        createdAt = 1_700_000_000_000L,
        signature = encoder.encode(ByteArray(72) { 0x33 }),
    )

    private companion object {
        const val MAX_PAYLOAD = 600
    }
}
