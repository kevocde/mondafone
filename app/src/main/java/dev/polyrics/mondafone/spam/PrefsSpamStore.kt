package dev.polyrics.mondafone.spam

import android.content.Context

class PrefsSpamStore(context: Context) : SpamStore {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun isRegistered(number: String): Boolean = prefs.contains(number)

    override fun mark(number: String, isSpam: Boolean) {
        val verdict = if (isSpam) VERDICT_SPAM else VERDICT_NOT_SPAM
        prefs.edit().putString(number, verdict).apply()
    }

    private companion object {
        const val FILE_NAME = "spam_store"
        const val VERDICT_SPAM = "spam"
        const val VERDICT_NOT_SPAM = "not_spam"
    }
}
