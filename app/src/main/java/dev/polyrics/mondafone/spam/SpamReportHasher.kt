package dev.polyrics.mondafone.spam

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SpamReportHasher(
    private val secret: String,
    private val encoder: Base64UrlEncoder = Base64UrlEncoder(),
) {

    fun hash(canonicalNumber: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), ALGORITHM))
        return encoder.encode(mac.doFinal(canonicalNumber.toByteArray(Charsets.UTF_8)))
    }

    private companion object {
        const val ALGORITHM = "HmacSHA256"
    }
}
