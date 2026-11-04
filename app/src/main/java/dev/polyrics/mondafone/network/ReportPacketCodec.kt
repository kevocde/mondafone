package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import dev.polyrics.mondafone.spam.SpamReport
import java.nio.ByteBuffer

class ReportPacketCodec(
    private val encoder: Base64UrlEncoder = Base64UrlEncoder(),
) {

    fun encode(report: SpamReport): ByteArray {
        val pubkey = encoder.decode(report.createdBy)
        val signature = encoder.decode(report.signature)
        val buffer = ByteBuffer.allocate(packetSize(pubkey.size, signature.size))
        buffer.put(pubkey.size.toByte())
        buffer.put(pubkey)
        buffer.putInt(report.seqNum.toInt())
        buffer.put(encoder.decode(report.phoneHash))
        buffer.putLong(report.createdAt)
        buffer.put(signature.size.toByte())
        buffer.put(signature)
        return buffer.array()
    }

    fun decode(packet: ByteArray): SpamReport? {
        val buffer = ByteBuffer.wrap(packet)
        if (buffer.remaining() < PUBKEY_LENGTH_BYTES) return null
        val pubkeyLength = buffer.get().toInt() and BYTE_MASK
        if (pubkeyLength == 0 || buffer.remaining() < pubkeyLength + FIXED_FIELDS_BYTES) return null
        val pubkey = ByteArray(pubkeyLength).also(buffer::get)
        val seqNum = buffer.int.toLong() and UINT32_MASK
        val phoneHash = ByteArray(HASH_BYTES).also(buffer::get)
        val createdAt = buffer.long
        val signatureLength = buffer.get().toInt() and BYTE_MASK
        if (signatureLength == 0 || buffer.remaining() != signatureLength) return null
        val signature = ByteArray(signatureLength).also(buffer::get)
        return SpamReport(
            phoneHash = encoder.encode(phoneHash),
            createdBy = encoder.encode(pubkey),
            seqNum = seqNum,
            createdAt = createdAt,
            signature = encoder.encode(signature),
        )
    }

    fun frameSize(buffer: ByteArray): FrameSize {
        if (buffer.isEmpty()) return FrameSize.Incomplete
        val pubkeyLength = buffer[0].toInt() and BYTE_MASK
        if (pubkeyLength == 0) return FrameSize.Invalid
        val signatureLengthIndex = signatureLengthIndex(pubkeyLength)
        if (buffer.size <= signatureLengthIndex) return FrameSize.Incomplete
        val signatureLength = buffer[signatureLengthIndex].toInt() and BYTE_MASK
        if (signatureLength == 0) return FrameSize.Invalid
        return FrameSize.Complete(signatureLengthIndex + SIGNATURE_LENGTH_BYTES + signatureLength)
    }

    sealed interface FrameSize {
        data class Complete(val bytes: Int) : FrameSize
        data object Incomplete : FrameSize
        data object Invalid : FrameSize
    }

    private fun packetSize(pubkeyLength: Int, signatureLength: Int): Int =
        PUBKEY_LENGTH_BYTES + pubkeyLength + FIXED_FIELDS_BYTES + signatureLength

    private fun signatureLengthIndex(pubkeyLength: Int): Int =
        PUBKEY_LENGTH_BYTES + pubkeyLength + SEQUENCE_BYTES + HASH_BYTES + TIMESTAMP_BYTES

    private companion object {
        const val PUBKEY_LENGTH_BYTES = 1
        const val SEQUENCE_BYTES = 4
        const val HASH_BYTES = 32
        const val TIMESTAMP_BYTES = 8
        const val SIGNATURE_LENGTH_BYTES = 1
        const val FIXED_FIELDS_BYTES =
            SEQUENCE_BYTES + HASH_BYTES + TIMESTAMP_BYTES + SIGNATURE_LENGTH_BYTES
        const val BYTE_MASK = 0xFF
        const val UINT32_MASK = 0xFFFFFFFFL
    }
}
