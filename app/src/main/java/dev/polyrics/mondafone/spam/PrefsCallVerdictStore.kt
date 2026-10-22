package dev.polyrics.mondafone.spam

import android.content.Context
import android.content.SharedPreferences

class PrefsCallVerdictStore(private val prefs: SharedPreferences) : CallVerdictStore {

    private var externalListener: (() -> Unit)? = null
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        externalListener?.invoke()
    }

    constructor(context: Context) : this(
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    ) {
        migrateLegacyVerdicts()
    }

    override fun isWhitelisted(number: String): Boolean =
        prefs.getString(number, null) == VERDICT_WHITELIST

    override fun isBlacklisted(number: String): Boolean =
        prefs.getString(number, null) == VERDICT_BLACKLIST

    override fun isClassified(number: String): Boolean =
        prefs.getString(number, null) in CLASSIFIED_VERDICTS

    override fun addToWhitelist(number: String) {
        prefs.edit().putString(number, VERDICT_WHITELIST).apply()
    }

    override fun addToBlacklist(number: String) {
        prefs.edit().putString(number, VERDICT_BLACKLIST).apply()
    }

    override fun addPending(number: String) {
        if (isClassified(number)) return
        prefs.edit().putString(number, VERDICT_PENDING).apply()
    }

    override fun pendingNumbers(): List<String> =
        prefs.all.entries
            .filter { it.value == VERDICT_PENDING }
            .map { it.key }

    override fun whitelistNumbers(): List<String> =
        prefs.all.entries
            .filter { it.value == VERDICT_WHITELIST }
            .map { it.key }

    override fun blacklistNumbers(): List<String> =
        prefs.all.entries
            .filter { it.value == VERDICT_BLACKLIST }
            .map { it.key }

    override fun remove(number: String) {
        prefs.edit().remove(number).apply()
    }

    override fun stats(): VerdictStats {
        val values = prefs.all.values
        return VerdictStats(
            whitelist = values.count { it == VERDICT_WHITELIST },
            blacklist = values.count { it == VERDICT_BLACKLIST },
        )
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun registerOnChangeListener(listener: () -> Unit) {
        externalListener = listener
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun unregisterOnChangeListener() {
        externalListener = null
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun migrateLegacyVerdicts() {
        val editor = prefs.edit()
        var changed = false
        for ((key, value) in prefs.all) {
            when (value) {
                LEGACY_VERDICT_SPAM -> {
                    editor.putString(key, VERDICT_BLACKLIST)
                    changed = true
                }
                LEGACY_VERDICT_NOT_SPAM -> {
                    editor.putString(key, VERDICT_WHITELIST)
                    changed = true
                }
            }
        }
        if (changed) editor.apply()
    }

    private companion object {
        const val FILE_NAME = "spam_store"
        const val VERDICT_WHITELIST = "whitelist"
        const val VERDICT_BLACKLIST = "blacklist"
        const val VERDICT_PENDING = "pending"
        val CLASSIFIED_VERDICTS = setOf(VERDICT_WHITELIST, VERDICT_BLACKLIST)
        const val LEGACY_VERDICT_SPAM = "spam"
        const val LEGACY_VERDICT_NOT_SPAM = "not_spam"
    }
}
