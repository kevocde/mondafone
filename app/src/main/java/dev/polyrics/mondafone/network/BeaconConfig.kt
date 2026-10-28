package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.BuildConfig

data class BeaconConfig(
    val deviceId: String,
    val periodMs: Long,
    val windowMs: Long,
) {
    companion object {
        fun fromBuild(): BeaconConfig = BeaconConfig(
            deviceId = BuildConfig.BEACON_DEVICE_ID,
            periodMs = BuildConfig.BEACON_PERIOD_MS,
            windowMs = BuildConfig.BEACON_WINDOW_MS,
        )
    }
}
