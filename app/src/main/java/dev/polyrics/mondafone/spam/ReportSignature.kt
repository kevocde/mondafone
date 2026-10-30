package dev.polyrics.mondafone.spam

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object ReportSignature {

    fun payload(phoneHash: String, createdAt: Long, seqNum: Long): ByteArray =
        "$phoneHash$FIELD_SEPARATOR$createdAt$FIELD_SEPARATOR$seqNum".toByteArray(Charsets.UTF_8)

    fun verify(
        phoneHash: String,
        createdAt: Long,
        seqNum: Long,
        signatureBase64: String,
        publicKeyBase64: String,
    ): Boolean = runCatching {
        val publicKey = KeyFactory.getInstance(KEY_ALGORITHM)
            .generatePublic(X509EncodedKeySpec(encoder.decode(publicKeyBase64)))
        Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(publicKey)
            update(payload(phoneHash, createdAt, seqNum))
            verify(encoder.decode(signatureBase64))
        }
    }.getOrDefault(false)

    private val encoder = Base64UrlEncoder()

    private const val FIELD_SEPARATOR = '|'
    private const val KEY_ALGORITHM = "EC"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
}
