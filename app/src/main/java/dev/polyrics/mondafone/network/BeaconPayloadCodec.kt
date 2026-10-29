package dev.polyrics.mondafone.network

class BeaconPayloadCodec {

    fun encode(id: String): ByteArray = id.toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray?): String? {
        if (bytes == null || bytes.isEmpty()) return null
        if (bytes.any { it !in PRINTABLE_RANGE }) return null
        return bytes.toString(Charsets.UTF_8)
    }

    fun fits(id: String): Boolean = encode(id).size <= MAX_ID_BYTES

    companion object {
        const val MAX_ID_BYTES = 10
        private val PRINTABLE_RANGE = 0x20..0x7E
    }
}
