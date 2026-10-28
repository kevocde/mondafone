package dev.polyrics.mondafone.identity

interface DeviceIdentity {

    fun ensureCreated()

    fun publicKeyBase64(): String?
}
