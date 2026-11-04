package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.spam.SequenceInventoryEntry
import java.nio.ByteBuffer

class SequenceInventoryCodec {

    fun encode(entries: List<SequenceInventoryEntry>): ByteArray {
        val buffer = ByteBuffer.allocate(encodedSize(entries))
        buffer.putShort(entries.size.toShort())
        entries.forEach { entry ->
            val pubkey = entry.createdBy.toByteArray(Charsets.UTF_8)
            buffer.put(pubkey.size.toByte())
            buffer.put(pubkey)
            buffer.putLong(entry.maxSeqNum)
        }
        return buffer.array()
    }

    fun decode(bytes: ByteArray?): List<SequenceInventoryEntry>? {
        if (bytes == null || bytes.size < HEADER_BYTES) return null
        val buffer = ByteBuffer.wrap(bytes)
        val count = buffer.short.toInt()
        if (count < 0) return null
        val entries = ArrayList<SequenceInventoryEntry>(count)
        repeat(count) {
            entries.add(decodeEntry(buffer) ?: return null)
        }
        return if (buffer.hasRemaining()) null else entries
    }

    private fun encodedSize(entries: List<SequenceInventoryEntry>): Int =
        HEADER_BYTES + entries.sumOf { entry ->
            PUBKEY_LENGTH_BYTES + entry.createdBy.toByteArray(Charsets.UTF_8).size + SEQ_BYTES
        }

    private fun decodeEntry(buffer: ByteBuffer): SequenceInventoryEntry? {
        if (buffer.remaining() < PUBKEY_LENGTH_BYTES) return null
        val pubkeyLength = buffer.get().toInt() and BYTE_MASK
        if (pubkeyLength == 0 || buffer.remaining() < pubkeyLength + SEQ_BYTES) return null
        val pubkey = ByteArray(pubkeyLength).also(buffer::get)
        val maxSeqNum = buffer.long
        return SequenceInventoryEntry(pubkey.toString(Charsets.UTF_8), maxSeqNum)
    }

    private companion object {
        const val HEADER_BYTES = 2
        const val PUBKEY_LENGTH_BYTES = 1
        const val SEQ_BYTES = 8
        const val BYTE_MASK = 0xFF
    }
}
