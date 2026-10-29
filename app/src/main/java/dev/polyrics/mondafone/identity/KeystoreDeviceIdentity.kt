package dev.polyrics.mondafone.identity

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.ECGenParameterSpec

class KeystoreDeviceIdentity(
    private val encoder: Base64UrlEncoder = Base64UrlEncoder(),
    private val fingerprint: PublicKeyFingerprint = PublicKeyFingerprint(),
) : DeviceIdentity {

    override fun ensureCreated() {
        if (keyStore().containsAlias(KEY_ALIAS)) return
        generateKeyPair()
    }

    override fun publicKeyBase64(): String? = publicKeyEncoded()?.let(encoder::encode)

    override fun publicKeyFingerprint(): String? = publicKeyEncoded()?.let(fingerprint::of)

    private fun publicKeyEncoded(): ByteArray? =
        keyStore().getCertificate(KEY_ALIAS)?.publicKey?.encoded

    private fun generateKeyPair() {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, PROVIDER)
            .apply { initialize(spec) }
            .generateKeyPair()
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(PROVIDER).apply { load(null) }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "mondafone_identity"
        const val CURVE = "secp256r1"
    }
}
