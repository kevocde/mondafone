package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SpamReport

class DataPipeCodec(
    private val requestCodec: ReportRequestCodec = ReportRequestCodec(),
    private val inventoryCodec: SequenceInventoryCodec = SequenceInventoryCodec(),
    private val packetCodec: ReportPacketCodec = ReportPacketCodec(),
) {

    fun encode(message: DataPipeMessage, maxPayload: Int): List<ByteArray> = when (message) {
        is DataPipeMessage.Requests ->
            split(FRAME_REQUESTS, message.requests, maxPayload, requestCodec::encode)
        is DataPipeMessage.OfferState ->
            split(FRAME_OFFER_STATE, message.entries, maxPayload, inventoryCodec::encode)
        is DataPipeMessage.Reports ->
            if (message.reports.isEmpty()) emptyList() else
                split(FRAME_REPORTS, message.reports, maxPayload, ::encodeReportsBody)
        DataPipeMessage.End -> listOf(byteArrayOf(FRAME_END))
    }

    fun decode(frame: ByteArray?): DataPipeMessage? {
        if (frame == null || frame.isEmpty()) return null
        val body = frame.copyOfRange(TYPE_BYTES, frame.size)
        return when (frame[0]) {
            FRAME_REQUESTS -> requestCodec.decode(body)?.let(DataPipeMessage::Requests)
            FRAME_OFFER_STATE -> inventoryCodec.decode(body)?.let(DataPipeMessage::OfferState)
            FRAME_REPORTS -> decodeReports(body)
            FRAME_END -> if (body.isEmpty()) DataPipeMessage.End else null
            else -> null
        }
    }

    private fun <T> split(
        type: Byte,
        items: List<T>,
        maxPayload: Int,
        encodeBody: (List<T>) -> ByteArray,
    ): List<ByteArray> {
        if (items.isEmpty()) return listOf(frame(type, encodeBody(emptyList())))
        val frames = mutableListOf<ByteArray>()
        var batch = mutableListOf<T>()
        items.forEach { item ->
            if (batch.isNotEmpty() && frame(type, encodeBody(batch + item)).size > maxPayload) {
                frames += frame(type, encodeBody(batch))
                batch = mutableListOf(item)
            } else {
                batch.add(item)
            }
        }
        if (batch.isNotEmpty()) frames += frame(type, encodeBody(batch))
        return frames
    }

    private fun encodeReportsBody(reports: List<SpamReport>): ByteArray =
        reports.fold(ByteArray(0)) { acc, report -> acc + packetCodec.encode(report) }

    private fun decodeReports(body: ByteArray): DataPipeMessage.Reports? {
        val reports = mutableListOf<SpamReport>()
        var offset = 0
        while (offset < body.size) {
            val remaining = body.copyOfRange(offset, body.size)
            val size = packetCodec.frameSize(remaining)
            if (size !is ReportPacketCodec.FrameSize.Complete || size.bytes > remaining.size) return null
            reports += packetCodec.decode(remaining.copyOf(size.bytes)) ?: return null
            offset += size.bytes
        }
        return DataPipeMessage.Reports(reports)
    }

    private fun frame(type: Byte, body: ByteArray): ByteArray = byteArrayOf(type) + body

    private companion object {
        const val TYPE_BYTES = 1
        const val FRAME_REQUESTS: Byte = 0x01
        const val FRAME_OFFER_STATE: Byte = 0x02
        const val FRAME_REPORTS: Byte = 0x03
        const val FRAME_END: Byte = 0x04
    }
}
