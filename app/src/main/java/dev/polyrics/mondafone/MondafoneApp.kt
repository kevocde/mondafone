package dev.polyrics.mondafone

import android.app.Application
import dev.polyrics.mondafone.identity.DeviceIdentity
import dev.polyrics.mondafone.identity.KeystoreDeviceIdentity
import dev.polyrics.mondafone.network.BeaconAlarmScheduler
import dev.polyrics.mondafone.network.BeaconConfig
import dev.polyrics.mondafone.network.BeaconEventLog
import dev.polyrics.mondafone.network.InMemoryBeaconEventLog
import dev.polyrics.mondafone.spam.BlockedCallNotifier
import dev.polyrics.mondafone.spam.CallVerdictStore
import dev.polyrics.mondafone.spam.CanonicalVerdictMigrator
import dev.polyrics.mondafone.spam.DeviceReportSigner
import dev.polyrics.mondafone.spam.FrameworkRegionProvider
import dev.polyrics.mondafone.spam.NotificationBlockedCallNotifier
import dev.polyrics.mondafone.spam.NotificationSpamVerdictPromptNotifier
import dev.polyrics.mondafone.spam.PeerStatusStore
import dev.polyrics.mondafone.spam.PhoneNumberNormalizer
import dev.polyrics.mondafone.spam.PrefsVerdictImporter
import dev.polyrics.mondafone.spam.SpamDependencies
import dev.polyrics.mondafone.spam.SpamReportHasher
import dev.polyrics.mondafone.spam.SpamReportStore
import dev.polyrics.mondafone.spam.SpamVerdictPromptNotifier
import dev.polyrics.mondafone.spam.SqliteCallVerdictStore
import dev.polyrics.mondafone.spam.SqliteLegacyBlacklistReader
import dev.polyrics.mondafone.spam.SqlitePeerStatusStore
import dev.polyrics.mondafone.spam.SqliteSpamReportStore

open class MondafoneApp : Application(), SpamDependencies {

    val beaconEventLog: BeaconEventLog = InMemoryBeaconEventLog()
    val deviceIdentity: DeviceIdentity = KeystoreDeviceIdentity()
    override val verdictStore: CallVerdictStore by lazy {
        SqliteCallVerdictStore(this).also { store ->
            PrefsVerdictImporter(this, store, spamReportStore, phoneNumberNormalizer).runOnce()
            CanonicalVerdictMigrator(
                this,
                store,
                spamReportStore,
                SqliteLegacyBlacklistReader(this),
                phoneNumberNormalizer,
            ).runOnce()
        }
    }
    override val spamReportStore: SpamReportStore by lazy {
        SqliteSpamReportStore(
            this,
            deviceIdentity,
            SpamReportHasher(BeaconConfig.fromBuild().appSecret),
            DeviceReportSigner(deviceIdentity),
        )
    }
    override val peerStatusStore: PeerStatusStore by lazy { SqlitePeerStatusStore(this) }
    override val phoneNumberNormalizer: PhoneNumberNormalizer by lazy {
        PhoneNumberNormalizer(FrameworkRegionProvider(this))
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
