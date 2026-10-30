package dev.polyrics.mondafone.spam

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import dev.polyrics.mondafone.identity.DeviceIdentity
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature

class TestIdentity(
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(256) }
        .generateKeyPair(),
) : DeviceIdentity {

    private val encoder = Base64UrlEncoder()

    override fun ensureCreated() = Unit

    override fun publicKeyBase64(): String = encoder.encode(keyPair.public.encoded)

    override fun publicKeyFingerprint(): String = "test-fingerprint"

    override fun sign(data: ByteArray): ByteArray =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(data)
            sign()
        }

    fun signBase64(data: ByteArray): String = encoder.encode(sign(data))

    fun signedReport(phoneHash: String, seqNum: Long, createdAt: Long): SpamReport =
        SpamReport(
            phoneHash = phoneHash,
            createdBy = publicKeyBase64(),
            seqNum = seqNum,
            createdAt = createdAt,
            signature = signBase64(ReportSignature.payload(phoneHash, createdAt, seqNum)),
        )
}
