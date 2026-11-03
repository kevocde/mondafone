package dev.polyrics.mondafone.spam

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import dev.polyrics.mondafone.identity.DeviceIdentity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature

class DeviceReportSignerTest {

    private val keyPair: KeyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(256) }
        .generateKeyPair()
    private val encoder = Base64UrlEncoder()
    private val identity = object : DeviceIdentity {
        override fun ensureCreated() = Unit
        override fun publicKeyBase64(): String = encoder.encode(keyPair.public.encoded)
        override fun publicKeyFingerprint(): String = "fingerprint"
        override fun sign(data: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(data)
            sign()
        }
    }

    @Test
    fun producesVerifiableSignature() {
        val phoneHash = "tp8KirWyY4taMv80FI2CSzrWThpYAw0Eht7uAuUjICU"
        val createdAt = 1_700_000_000_000L
        val seqNum = 3L

        val signature = DeviceReportSigner(identity).sign(phoneHash, createdAt, seqNum)

        assertNotNull(signature)
        assertTrue(ReportSignature.verify(phoneHash, createdAt, seqNum, requireNotNull(signature), identity.publicKeyBase64()))
    }

    @Test
    fun returnsNullWhenIdentityCannotSign() {
        val signingFailure = object : DeviceIdentity {
            override fun ensureCreated() = Unit
            override fun publicKeyBase64(): String? = null
            override fun publicKeyFingerprint(): String? = null
            override fun sign(data: ByteArray): ByteArray? = null
        }

        assertNull(DeviceReportSigner(signingFailure).sign("hash", 1L, 1L))
    }
}
