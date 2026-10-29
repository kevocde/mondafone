package dev.polyrics.mondafone

import android.app.Application
import dev.polyrics.mondafone.identity.DeviceIdentity
import dev.polyrics.mondafone.identity.KeystoreDeviceIdentity
import dev.polyrics.mondafone.network.BeaconAlarmScheduler
import dev.polyrics.mondafone.network.BeaconEventLog
import dev.polyrics.mondafone.network.InMemoryBeaconEventLog
import dev.polyrics.mondafone.spam.BlockedCallNotifier
import dev.polyrics.mondafone.spam.CallVerdictStore
import dev.polyrics.mondafone.spam.NotificationBlockedCallNotifier
import dev.polyrics.mondafone.spam.NotificationSpamVerdictPromptNotifier
import dev.polyrics.mondafone.spam.PrefsVerdictImporter
import dev.polyrics.mondafone.spam.SpamDependencies
import dev.polyrics.mondafone.spam.SpamVerdictPromptNotifier
import dev.polyrics.mondafone.spam.SqliteCallVerdictStore

open class MondafoneApp : Application(), SpamDependencies {

    val beaconEventLog: BeaconEventLog = InMemoryBeaconEventLog()
    val deviceIdentity: DeviceIdentity = KeystoreDeviceIdentity()
    override val verdictStore: CallVerdictStore by lazy {
        SqliteCallVerdictStore(this).also { store ->
            PrefsVerdictImporter(this, store).runOnce()
        }
    }
    override val blockedCallNotifier: BlockedCallNotifier by lazy {
        NotificationBlockedCallNotifier(this)
    }
    override val verdictPromptNotifier: SpamVerdictPromptNotifier by lazy {
        NotificationSpamVerdictPromptNotifier(this)
    }

    override fun onCreate() {
        super.onCreate()
        runCatching { deviceIdentity.ensureCreated() }
    }

    fun scheduleBeacon() {
        runCatching { BeaconAlarmScheduler(this).scheduleNext() }
    }
}
