package dev.polyrics.mondafono

import android.app.Application
import dev.polyrics.mondafono.identity.DeviceIdentity
import dev.polyrics.mondafono.identity.KeystoreDeviceIdentity
import dev.polyrics.mondafono.network.BeaconAlarmScheduler
import dev.polyrics.mondafono.network.BeaconConfig
import dev.polyrics.mondafono.spam.BlockedCallNotifier
import dev.polyrics.mondafono.spam.BlockedCallStore
import dev.polyrics.mondafono.spam.CallVerdictStore
import dev.polyrics.mondafono.spam.CanonicalVerdictMigrator
import dev.polyrics.mondafono.spam.DeviceReportSigner
import dev.polyrics.mondafono.spam.FrameworkRegionProvider
import dev.polyrics.mondafono.spam.NotificationBlockedCallNotifier
import dev.polyrics.mondafono.spam.NotificationSpamVerdictPromptNotifier
import dev.polyrics.mondafono.spam.PeerStatusStore
import dev.polyrics.mondafono.spam.PhoneNumberNormalizer
import dev.polyrics.mondafono.spam.PrefsVerdictImporter
import dev.polyrics.mondafono.spam.SpamDependencies
import dev.polyrics.mondafono.spam.SpamReportHasher
import dev.polyrics.mondafono.spam.SpamReportStore
import dev.polyrics.mondafono.spam.SpamVerdictPromptNotifier
import dev.polyrics.mondafono.spam.SqliteBlockedCallStore
import dev.polyrics.mondafono.spam.SqliteCallVerdictStore
import dev.polyrics.mondafono.spam.SqliteLegacyBlacklistReader
import dev.polyrics.mondafono.spam.SqlitePeerStatusStore
import dev.polyrics.mondafono.spam.SqliteSpamReportStore

open class MondafonoApp : Application(), SpamDependencies {

    open val deviceIdentity: DeviceIdentity = KeystoreDeviceIdentity()
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
    override val blockedCallStore: BlockedCallStore by lazy { SqliteBlockedCallStore(this) }
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
