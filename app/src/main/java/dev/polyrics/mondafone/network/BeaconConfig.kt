package dev.polyrics.mondafone.network

import dev.polyrics.mondafone.BuildConfig

data class BeaconConfig(
    val cronExpression: String,
    val windowMs: Long,
    val appSecret: String,
    val gattLifetimeMs: Long,
) {
    companion object {
        fun fromBuild(): BeaconConfig = BeaconConfig(
            cronExpression = BuildConfig.BEACON_CRON,
            windowMs = BuildConfig.BEACON_WINDOW_MS,
            appSecret = BuildConfig.BEACON_APP_SECRET,
            gattLifetimeMs = BuildConfig.GATT_LIFETIME_MS,
        )
    }
}
