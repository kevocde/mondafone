package dev.polyrics.mondafone

import android.app.Application
import android.content.Intent
import dev.polyrics.mondafone.identity.DeviceIdentity
import dev.polyrics.mondafone.identity.KeystoreDeviceIdentity
import dev.polyrics.mondafone.network.BeaconEventLog
import dev.polyrics.mondafone.network.BleBeaconService
import dev.polyrics.mondafone.network.InMemoryBeaconEventLog

class MondafoneApp : Application() {

    val beaconEventLog: BeaconEventLog = InMemoryBeaconEventLog()
    val deviceIdentity: DeviceIdentity = KeystoreDeviceIdentity()

    override fun onCreate() {
        super.onCreate()
        runCatching { deviceIdentity.ensureCreated() }
        startBeaconService()
    }

    fun startBeaconService() {
        runCatching { startForegroundService(Intent(this, BleBeaconService::class.java)) }
    }
}
