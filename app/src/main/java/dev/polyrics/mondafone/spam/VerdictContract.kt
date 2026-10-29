package dev.polyrics.mondafone.spam

object VerdictContract {
    const val DATABASE_NAME = "spam_verdicts.db"
    const val DATABASE_VERSION = 1

    const val TABLE_VERDICTS = "verdicts"
    const val COLUMN_NUMBER = "number"
    const val COLUMN_VERDICT = "verdict"

    const val VERDICT_WHITELIST = "whitelist"
    const val VERDICT_BLACKLIST = "blacklist"
    const val VERDICT_PENDING = "pending"

    const val CREATE_VERDICTS_TABLE =
        "CREATE TABLE $TABLE_VERDICTS (" +
            "$COLUMN_NUMBER TEXT PRIMARY KEY, " +
            "$COLUMN_VERDICT TEXT NOT NULL)"
}
