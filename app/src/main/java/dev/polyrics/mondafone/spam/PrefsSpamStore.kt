package dev.polyrics.mondafone.spam

import android.content.Context
import android.content.SharedPreferences

class PrefsSpamStore(private val prefs: SharedPreferences) : SpamStore {

    constructor(context: Context) : this(
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun isRegistered(number: String): Boolean = prefs.getString(number, null) in VERDICTS

    override fun isSpam(number: String): Boolean = prefs.getString(number, null) == VERDICT_SPAM

    override fun mark(number: String, isSpam: Boolean) {
        val verdict = if (isSpam) VERDICT_SPAM else VERDICT_NOT_SPAM
        prefs.edit().putString(number, verdict).apply()
    }

    override fun addPending(number: String) {
        if (isRegistered(number)) return
        prefs.edit().putString(number, VERDICT_PENDING).apply()
    }

    override fun pendingNumbers(): List<String> =
        prefs.all.entries
            .filter { it.value == VERDICT_PENDING }
            .map { it.key }

    override fun stats(): SpamStats {
        val values = prefs.all.values
        return SpamStats(
            spam = values.count { it == VERDICT_SPAM },
            notSpam = values.count { it == VERDICT_NOT_SPAM },
        )
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "spam_store"
        const val VERDICT_SPAM = "spam"
        const val VERDICT_NOT_SPAM = "not_spam"
        const val VERDICT_PENDING = "pending"
        val VERDICTS = setOf(VERDICT_SPAM, VERDICT_NOT_SPAM)
    }
}
