package dev.polyrics.mondafone.network

import java.nio.ByteBuffer

class ReportRequestCodec {

    fun encode(requests: List<ReportRequest>): ByteArray {
        val buffer = ByteBuffer.allocate(encodedSize(requests))
        buffer.putShort(requests.size.toShort())
        requests.forEach { request ->
            val pubkey = request.createdBy.toByteArray(Charsets.UTF_8)
            buffer.put(pubkey.size.toByte())
            buffer.put(pubkey)
            buffer.putLong(request.fromSeq)
            buffer.putLong(request.toSeq)
        }
        return buffer.array()
    }

    fun decode(bytes: ByteArray?): List<ReportRequest>? {
        if (bytes == null || bytes.size < HEADER_BYTES) return null
        val buffer = ByteBuffer.wrap(bytes)
        val count = buffer.short.toInt()
        if (count < 0) return null
        val requests = ArrayList<ReportRequest>(count)
        repeat(count) {
            requests.add(decodeEntry(buffer) ?: return null)
        }
        return if (buffer.hasRemaining()) null else requests
    }

    private fun encodedSize(requests: List<ReportRequest>): Int =
        HEADER_BYTES + requests.sumOf { request ->
            PUBKEY_LENGTH_BYTES +
                request.createdBy.toByteArray(Charsets.UTF_8).size +
                SEQUENCE_BYTES * 2
        }

    private fun decodeEntry(buffer: ByteBuffer): ReportRequest? {
        if (buffer.remaining() < PUBKEY_LENGTH_BYTES) return null
        val pubkeyLength = buffer.get().toInt() and BYTE_MASK
        if (pubkeyLength == 0 || buffer.remaining() < pubkeyLength + SEQUENCE_BYTES * 2) return null
        val pubkey = ByteArray(pubkeyLength).also(buffer::get)
        val fromSeq = buffer.long
        val toSeq = buffer.long
        return ReportRequest(pubkey.toString(Charsets.UTF_8), fromSeq, toSeq)
    }

    private companion object {
        const val HEADER_BYTES = 2
        const val PUBKEY_LENGTH_BYTES = 1
        const val SEQUENCE_BYTES = 8
        const val BYTE_MASK = 0xFF
    }
}
