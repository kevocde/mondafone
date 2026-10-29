package dev.polyrics.mondafone.spam

import android.content.Context
import android.content.SharedPreferences

class PrefsVerdictImporter(
    private val prefs: SharedPreferences,
    private val store: CallVerdictStore,
) {

    constructor(context: Context, store: CallVerdictStore) : this(
        context.getSharedPreferences(LEGACY_FILE_NAME, Context.MODE_PRIVATE),
        store,
    )

    fun runOnce() {
        if (prefs.getBoolean(KEY_IMPORTED, false)) return
        prefs.all.forEach { (number, value) -> importVerdict(number, value) }
        prefs.edit().clear().putBoolean(KEY_IMPORTED, true).apply()
    }

    private fun importVerdict(number: String, value: Any?) {
        when (value) {
            VerdictContract.VERDICT_WHITELIST, LEGACY_NOT_SPAM -> store.addToWhitelist(number)
            VerdictContract.VERDICT_BLACKLIST, LEGACY_SPAM -> store.addToBlacklist(number)
            VerdictContract.VERDICT_PENDING -> store.addPending(number)
        }
    }

    companion object {
        const val LEGACY_FILE_NAME = "spam_store"
        private const val KEY_IMPORTED = "imported_to_sqlite"
        private const val LEGACY_SPAM = "spam"
        private const val LEGACY_NOT_SPAM = "not_spam"
    }
}
