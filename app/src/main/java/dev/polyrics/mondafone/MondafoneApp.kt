package dev.polyrics.mondafone

import android.app.Application
import android.content.Intent
import dev.polyrics.mondafone.identity.DeviceIdentity
import dev.polyrics.mondafone.identity.KeystoreDeviceIdentity
import dev.polyrics.mondafone.network.BeaconEventLog
import dev.polyrics.mondafone.network.BleBeaconService
import dev.polyrics.mondafone.network.InMemoryBeaconEventLog
import dev.polyrics.mondafone.spam.CallVerdictStore
import dev.polyrics.mondafone.spam.PrefsVerdictImporter
import dev.polyrics.mondafone.spam.SqliteCallVerdictStore

class MondafoneApp : Application() {

    val beaconEventLog: BeaconEventLog = InMemoryBeaconEventLog()
    val deviceIdentity: DeviceIdentity = KeystoreDeviceIdentity()
    val verdictStore: CallVerdictStore by lazy {
        SqliteCallVerdictStore(this).also { store ->
            PrefsVerdictImporter(this, store).runOnce()
        }
    }

    override fun onCreate() {
        super.onCreate()
        runCatching { deviceIdentity.ensureCreated() }
    }

    fun startBeaconService() {
        runCatching { startForegroundService(Intent(this, BleBeaconService::class.java)) }
    }
}
