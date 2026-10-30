package dev.polyrics.mondafone.identity

interface DeviceIdentity {

    fun ensureCreated()

    fun publicKeyBase64(): String?

    fun publicKeyFingerprint(): String?

    fun sign(data: ByteArray): ByteArray?
}
