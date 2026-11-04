package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SpamReport
import java.io.ByteArrayOutputStream

class ReportStreamDecoder(
    private val codec: ReportPacketCodec = ReportPacketCodec(),
) {

    private val pending = ByteArrayOutputStream()

    fun feed(bytes: ByteArray): List<SpamReport> {
        pending.write(bytes)
        return drain()
    }

    private fun drain(): List<SpamReport> {
        val reports = mutableListOf<SpamReport>()
        while (true) {
            val buffered = pending.toByteArray()
            when (val frame = codec.frameSize(buffered)) {
                is ReportPacketCodec.FrameSize.Complete -> {
                    if (frame.bytes > buffered.size) return reports
                    consume(frame.bytes, buffered)?.let(reports::add)
                }
                ReportPacketCodec.FrameSize.Incomplete -> return reports
                ReportPacketCodec.FrameSize.Invalid -> {
                    pending.reset()
                    return reports
                }
            }
        }
    }

    private fun consume(frameBytes: Int, buffered: ByteArray): SpamReport? {
        pending.reset()
        if (buffered.size > frameBytes) {
            pending.write(buffered, frameBytes, buffered.size - frameBytes)
        }
        return codec.decode(buffered.copyOf(frameBytes))
    }
}
