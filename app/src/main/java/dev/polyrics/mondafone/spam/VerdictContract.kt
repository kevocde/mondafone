package dev.polyrics.mondafone.spam

object VerdictContract {
    const val DATABASE_NAME = "spam_verdicts.db"
    const val DATABASE_VERSION = 2

    const val TABLE_VERDICTS = "verdicts"
    const val COLUMN_NUMBER = "number"
    const val COLUMN_VERDICT = "verdict"

    const val VERDICT_WHITELIST = "whitelist"
    const val VERDICT_BLACKLIST = "blacklist"
    const val VERDICT_PENDING = "pending"

    const val TABLE_SPAM_REPORTS = "spam_reports"
    const val COLUMN_PHONE_HASH = "phone_hash"
    const val COLUMN_CREATED_BY = "created_by"
    const val COLUMN_SEQ_NUM = "seq_num"
    const val COLUMN_CREATED_AT = "created_at"
    const val COLUMN_SIGNATURE = "signature"

    const val TABLE_SPAM_REPORT_ALIASES = "spam_report_aliases"

    const val TABLE_PEERS_STATUS = "peers_status"
    const val COLUMN_ID_PUBKEY = "id_pubkey"
    const val COLUMN_MAX_SEQ_NUM = "max_seq_num"
    const val COLUMN_LAST_SYNC_AT = "last_sync_at"

    const val TABLE_LOCAL_SYNC_STATE = "local_sync_state"
    const val COLUMN_NEXT_SEQ_NUM = "next_seq_num"

    const val CREATE_VERDICTS_TABLE =
        "CREATE TABLE $TABLE_VERDICTS (" +
            "$COLUMN_NUMBER TEXT PRIMARY KEY, " +
            "$COLUMN_VERDICT TEXT NOT NULL)"

    const val CREATE_SPAM_REPORTS_TABLE =
        "CREATE TABLE $TABLE_SPAM_REPORTS (" +
            "$COLUMN_PHONE_HASH TEXT NOT NULL, " +
            "$COLUMN_CREATED_BY TEXT NOT NULL, " +
            "$COLUMN_SEQ_NUM INTEGER NOT NULL, " +
            "$COLUMN_CREATED_AT INTEGER NOT NULL, " +
            "$COLUMN_SIGNATURE TEXT NOT NULL, " +
            "PRIMARY KEY ($COLUMN_PHONE_HASH, $COLUMN_CREATED_BY))"

    const val CREATE_SPAM_REPORT_ALIASES_TABLE =
        "CREATE TABLE $TABLE_SPAM_REPORT_ALIASES (" +
            "$COLUMN_PHONE_HASH TEXT PRIMARY KEY, " +
            "$COLUMN_NUMBER TEXT NOT NULL)"

    const val CREATE_PEERS_STATUS_TABLE =
        "CREATE TABLE $TABLE_PEERS_STATUS (" +
            "$COLUMN_ID_PUBKEY TEXT PRIMARY KEY, " +
            "$COLUMN_MAX_SEQ_NUM INTEGER NOT NULL, " +
            "$COLUMN_LAST_SYNC_AT INTEGER NOT NULL)"

    const val CREATE_LOCAL_SYNC_STATE_TABLE =
        "CREATE TABLE $TABLE_LOCAL_SYNC_STATE (" +
            "id INTEGER PRIMARY KEY CHECK (id = 1), " +
            "$COLUMN_NEXT_SEQ_NUM INTEGER NOT NULL)"

    val CREATE_ALL_TABLES = listOf(
        CREATE_VERDICTS_TABLE,
        CREATE_SPAM_REPORTS_TABLE,
        CREATE_SPAM_REPORT_ALIASES_TABLE,
        CREATE_PEERS_STATUS_TABLE,
        CREATE_LOCAL_SYNC_STATE_TABLE,
    )

    val CREATE_TABLES_ADDED_IN_VERSION_TWO = listOf(
        CREATE_SPAM_REPORTS_TABLE,
        CREATE_SPAM_REPORT_ALIASES_TABLE,
        CREATE_PEERS_STATUS_TABLE,
        CREATE_LOCAL_SYNC_STATE_TABLE,
    )
}
