package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.BuildConfig

data class BeaconConfig(
    val cronExpression: String,
    val windowMs: Long,
) {
    companion object {
        fun fromBuild(): BeaconConfig = BeaconConfig(
            cronExpression = BuildConfig.BEACON_CRON,
            windowMs = BuildConfig.BEACON_WINDOW_MS,
        )
    }
}
