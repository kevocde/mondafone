package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RotatingBeaconId(
    private val secret: String,
    private val encoder: Base64UrlEncoder = Base64UrlEncoder(),
) {

    fun current(fingerprint: String, epochMillis: Long = System.currentTimeMillis()): String =
        of(fingerprint, epochHourOf(epochMillis))

    fun of(fingerprint: String, epochHour: Long): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), ALGORITHM))
        mac.update(fingerprint.toByteArray(Charsets.UTF_8))
        mac.update(ByteBuffer.allocate(LONG_BYTES).putLong(epochHour).array())
        return encoder.encode(mac.doFinal().copyOf(ID_BYTES))
    }

    fun candidates(
        fingerprint: String,
        epochMillis: Long = System.currentTimeMillis(),
        toleranceHours: Int = DEFAULT_TOLERANCE_HOURS,
    ): Set<String> {
        val hour = epochHourOf(epochMillis)
        return (hour - toleranceHours..hour + toleranceHours)
            .map { of(fingerprint, it) }
            .toSet()
    }

    fun matches(
        fingerprint: String,
        id: String,
        epochMillis: Long = System.currentTimeMillis(),
        toleranceHours: Int = DEFAULT_TOLERANCE_HOURS,
    ): Boolean = id in candidates(fingerprint, epochMillis, toleranceHours)

    private fun epochHourOf(epochMillis: Long): Long =
        Math.floorDiv(epochMillis, MILLIS_PER_HOUR)

    private companion object {
        const val ALGORITHM = "HmacSHA256"
        const val ID_BYTES = 7
        const val LONG_BYTES = 8
        const val MILLIS_PER_HOUR = 3_600_000L
        const val DEFAULT_TOLERANCE_HOURS = 1
    }
}
