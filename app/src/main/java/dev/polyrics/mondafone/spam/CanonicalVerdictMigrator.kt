package dev.polyrics.mondafone.spam

import android.content.Context
import android.content.SharedPreferences

class CanonicalVerdictMigrator(
    private val prefs: SharedPreferences,
    private val store: CallVerdictStore,
    private val reportStore: SpamReportStore,
    private val legacyBlacklist: LegacyBlacklistReader,
    private val normalizer: PhoneNumberNormalizer,
) {

    constructor(
        context: Context,
        store: CallVerdictStore,
        reportStore: SpamReportStore,
        legacyBlacklist: LegacyBlacklistReader,
        normalizer: PhoneNumberNormalizer,
    ) : this(
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
        store,
        reportStore,
        legacyBlacklist,
        normalizer,
    )

    fun runOnce() {
        if (prefs.getBoolean(KEY_DONE, false)) return
        migrateWhitelist()
        migratePending()
        migrateBlacklist()
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    private fun migrateWhitelist() = store.whitelistNumbers().forEach { number ->
        normalizedOf(number)?.let { canonical ->
            store.remove(number)
            store.addToWhitelist(canonical)
        }
    }

    private fun migratePending() = store.pendingNumbers().forEach { number ->
        normalizedOf(number)?.let { canonical ->
            store.remove(number)
            store.addPending(canonical)
        }
    }

    private fun migrateBlacklist() = legacyBlacklist.blacklistNumbers().forEach { number ->
        normalizedOf(number)?.let { canonical ->
            reportStore.reportOwn(canonical, number)
            store.remove(number)
        }
    }

    private fun normalizedOf(number: String): String? =
        normalizer.normalize(number)?.takeUnless { it == number }

    private companion object {
        const val FILE_NAME = "verdict_migrations"
        const val KEY_DONE = "canonical_numbers_migrated"
    }
}
