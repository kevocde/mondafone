package dev.polyrics.mondafone.identity

import java.security.MessageDigest

class PublicKeyFingerprint(
    private val encoder: Base64UrlEncoder = Base64UrlEncoder(),
) {

    fun of(encodedKey: ByteArray): String {
        val hash = MessageDigest.getInstance(HASH_ALGORITHM).digest(encodedKey)
        return encoder.encode(hash.copyOf(FINGERPRINT_BYTES))
    }

    private companion object {
        const val HASH_ALGORITHM = "SHA-256"
        const val FINGERPRINT_BYTES = 7
    }
}
