package dev.polyrics.mondafone.spam

import dev.polyrics.mondafone.identity.Base64UrlEncoder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature

class ReportSignatureTest {

    private val keyPair: KeyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(256) }
        .generateKeyPair()
    private val encoder = Base64UrlEncoder()
    private val phoneHash = "tp8KirWyY4taMv80FI2CSzrWThpYAw0Eht7uAuUjICU"
    private val createdAt = 1_700_000_000_000L
    private val seqNum = 7L

    @Test
    fun acceptsValidSignature() {
        assertTrue(
            ReportSignature.verify(
                phoneHash,
                createdAt,
                seqNum,
                validSignature(),
                encoder.encode(keyPair.public.encoded),
            ),
        )
    }

    @Test
    fun rejectsTamperedPhoneHash() {
        assertFalse(verifyWith("other-hash", createdAt, seqNum, validSignature()))
    }

    @Test
    fun rejectsTamperedCreatedAt() {
        assertFalse(verifyWith(phoneHash, createdAt + 1, seqNum, validSignature()))
    }

    @Test
    fun rejectsTamperedSequenceNumber() {
        assertFalse(verifyWith(phoneHash, createdAt, seqNum + 1, validSignature()))
    }

    @Test
    fun rejectsSignatureFromAnotherKey() {
        val otherKey = KeyPairGenerator.getInstance("EC")
            .apply { initialize(256) }
            .generateKeyPair()
        assertFalse(
            ReportSignature.verify(
                phoneHash,
                createdAt,
                seqNum,
                sign(otherKey),
                encoder.encode(keyPair.public.encoded),
            ),
        )
    }

    @Test
    fun rejectsMalformedSignatureOrKey() {
        assertFalse(ReportSignature.verify(phoneHash, createdAt, seqNum, "not-base64!", encoder.encode(keyPair.public.encoded)))
        assertFalse(ReportSignature.verify(phoneHash, createdAt, seqNum, validSignature(), "not-base64!"))
    }

    @Test
    fun payloadIsDeterministicAndInputDependent() {
        val payload = ReportSignature.payload(phoneHash, createdAt, seqNum)
        assertTrue(payload.contentEquals(ReportSignature.payload(phoneHash, createdAt, seqNum)))
        assertFalse(payload.contentEquals(ReportSignature.payload(phoneHash, createdAt, seqNum + 1)))
    }

    private fun verifyWith(hash: String, created: Long, seq: Long, signature: String): Boolean =
        ReportSignature.verify(hash, created, seq, signature, encoder.encode(keyPair.public.encoded))

    private fun validSignature(): String = sign(keyPair)

    private fun sign(keyPair: KeyPair): String {
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(ReportSignature.payload(phoneHash, createdAt, seqNum))
            sign()
        }
        return encoder.encode(signature)
    }
}
