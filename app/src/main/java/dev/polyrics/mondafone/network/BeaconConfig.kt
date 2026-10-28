package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.BuildConfig

data class BeaconConfig(
    val periodMs: Long,
    val windowMs: Long,
) {
    companion object {
        fun fromBuild(): BeaconConfig = BeaconConfig(
            periodMs = BuildConfig.BEACON_PERIOD_MS,
            windowMs = BuildConfig.BEACON_WINDOW_MS,
        )
    }
}
