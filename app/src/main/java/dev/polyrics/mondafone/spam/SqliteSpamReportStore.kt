package dev.polyrics.mondafone.spam

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.polyrics.mondafone.identity.DeviceIdentity

class SqliteSpamReportStore(
    private val helper: SQLiteOpenHelper,
    private val identity: DeviceIdentity,
    private val hasher: SpamReportHasher,
    private val signer: ReportSigner,
    private val clock: () -> Long = System::currentTimeMillis,
) : SpamReportStore {

    constructor(
        context: Context,
        identity: DeviceIdentity,
        hasher: SpamReportHasher,
        signer: ReportSigner,
    ) : this(VerdictDbHelper(context), identity, hasher, signer)

    @Volatile
    private var listener: (() -> Unit)? = null

    override fun reportOwn(canonicalNumber: String, displayNumber: String): SpamReport? {
        val createdBy = myPublicKey() ?: return null
        val phoneHash = hasher.hash(canonicalNumber)
        ownReport(phoneHash)?.let { return it }

        val createdAt = clock()
        val database = database()
        database.beginTransaction()
        val report = try {
            val seqNum = nextSeqNum(database)
            signer.sign(phoneHash, createdAt, seqNum)?.let { signature ->
                SpamReport(phoneHash, createdBy, seqNum, createdAt, signature).also { saved ->
                    insertReport(database, saved)
                    saveAlias(database, phoneHash, displayNumber)
                    saveNextSeqNum(database, seqNum + 1)
                    database.setTransactionSuccessful()
                }
            }
        } finally {
            database.endTransaction()
        }
        if (report != null) notifyChanged()
        return report
    }

    override fun acceptRemoteReport(report: SpamReport): Boolean {
        val verified = ReportSignature.verify(
            report.phoneHash,
            report.createdAt,
            report.seqNum,
            report.signature,
            report.createdBy,
        )
        if (!verified) return false
        val inserted = database().insertWithOnConflict(
            VerdictContract.TABLE_SPAM_REPORTS,
            null,
            reportValues(report),
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        val accepted = inserted != INSERT_FAILED
        if (accepted) notifyChanged()
        return accepted
    }

    override fun isReportedByMe(canonicalNumber: String): Boolean {
        val createdBy = myPublicKey() ?: return false
        return database()
            .rawQuery(
                "SELECT 1 FROM ${VerdictContract.TABLE_SPAM_REPORTS} " +
                    "WHERE ${VerdictContract.COLUMN_PHONE_HASH} = ? " +
                    "AND ${VerdictContract.COLUMN_CREATED_BY} = ? LIMIT 1",
                arrayOf(hasher.hash(canonicalNumber), createdBy),
            )
            .use { it.moveToFirst() }
    }

    override fun foreignReportCount(canonicalNumber: String): Int {
        val createdBy = myPublicKey() ?: NO_REPORTER
        return database()
            .rawQuery(
                "SELECT COUNT(*) FROM ${VerdictContract.TABLE_SPAM_REPORTS} " +
                    "WHERE ${VerdictContract.COLUMN_PHONE_HASH} = ? " +
                    "AND ${VerdictContract.COLUMN_CREATED_BY} <> ?",
                arrayOf(hasher.hash(canonicalNumber), createdBy),
            )
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
    }

    override fun ownReports(): List<OwnSpamReport> {
        val createdBy = myPublicKey() ?: return emptyList()
        return database()
            .rawQuery(
                "SELECT r.${VerdictContract.COLUMN_PHONE_HASH}, " +
                    "COALESCE(a.${VerdictContract.COLUMN_NUMBER}, r.${VerdictContract.COLUMN_PHONE_HASH}), " +
                    "r.${VerdictContract.COLUMN_SEQ_NUM}, r.${VerdictContract.COLUMN_CREATED_AT} " +
                    "FROM ${VerdictContract.TABLE_SPAM_REPORTS} r " +
                    "LEFT JOIN ${VerdictContract.TABLE_SPAM_REPORT_ALIASES} a " +
                    "ON a.${VerdictContract.COLUMN_PHONE_HASH} = r.${VerdictContract.COLUMN_PHONE_HASH} " +
                    "WHERE r.${VerdictContract.COLUMN_CREATED_BY} = ? " +
                    "ORDER BY r.${VerdictContract.COLUMN_CREATED_AT} DESC",
                arrayOf(createdBy),
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            OwnSpamReport(
                                phoneHash = cursor.getString(0),
                                displayNumber = cursor.getString(1),
                                seqNum = cursor.getLong(2),
                                createdAt = cursor.getLong(3),
                            ),
                        )
                    }
                }
            }
    }

    override fun removeOwnReport(phoneHash: String) {
        val createdBy = myPublicKey() ?: return
        val database = database()
        database.delete(
            VerdictContract.TABLE_SPAM_REPORTS,
            "${VerdictContract.COLUMN_PHONE_HASH} = ? AND ${VerdictContract.COLUMN_CREATED_BY} = ?",
            arrayOf(phoneHash, createdBy),
        )
        database.delete(
            VerdictContract.TABLE_SPAM_REPORT_ALIASES,
            "${VerdictContract.COLUMN_PHONE_HASH} = ?",
            arrayOf(phoneHash),
        )
        notifyChanged()
    }

    override fun registerOnChangeListener(listener: () -> Unit) {
        this.listener = listener
    }

    override fun unregisterOnChangeListener() {
        listener = null
    }

    private fun ownReport(phoneHash: String): SpamReport? {
        val createdBy = myPublicKey() ?: return null
        return database()
            .query(
                VerdictContract.TABLE_SPAM_REPORTS,
                null,
                "${VerdictContract.COLUMN_PHONE_HASH} = ? AND ${VerdictContract.COLUMN_CREATED_BY} = ?",
                arrayOf(phoneHash, createdBy),
                null,
                null,
                null,
            )
            .use { cursor -> if (cursor.moveToFirst()) cursor.toSpamReport() else null }
    }

    private fun nextSeqNum(database: SQLiteDatabase): Long =
        database
            .rawQuery(
                "SELECT ${VerdictContract.COLUMN_NEXT_SEQ_NUM} FROM ${VerdictContract.TABLE_LOCAL_SYNC_STATE} WHERE id = 1",
                null,
            )
            .use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else FIRST_SEQ_NUM }

    private fun saveNextSeqNum(database: SQLiteDatabase, seqNum: Long) {
        val values = ContentValues().apply {
            put("id", SYNC_STATE_ROW_ID)
            put(VerdictContract.COLUMN_NEXT_SEQ_NUM, seqNum)
        }
        database.insertWithOnConflict(
            VerdictContract.TABLE_LOCAL_SYNC_STATE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun insertReport(database: SQLiteDatabase, report: SpamReport) {
        database.insertWithOnConflict(
            VerdictContract.TABLE_SPAM_REPORTS,
            null,
            reportValues(report),
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun saveAlias(database: SQLiteDatabase, phoneHash: String, displayNumber: String) {
        val values = ContentValues().apply {
            put(VerdictContract.COLUMN_PHONE_HASH, phoneHash)
            put(VerdictContract.COLUMN_NUMBER, displayNumber)
        }
        database.insertWithOnConflict(
            VerdictContract.TABLE_SPAM_REPORT_ALIASES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun reportValues(report: SpamReport): ContentValues = ContentValues().apply {
        put(VerdictContract.COLUMN_PHONE_HASH, report.phoneHash)
        put(VerdictContract.COLUMN_CREATED_BY, report.createdBy)
        put(VerdictContract.COLUMN_SEQ_NUM, report.seqNum)
        put(VerdictContract.COLUMN_CREATED_AT, report.createdAt)
        put(VerdictContract.COLUMN_SIGNATURE, report.signature)
    }

    private fun Cursor.toSpamReport(): SpamReport = SpamReport(
        phoneHash = getString(getColumnIndexOrThrow(VerdictContract.COLUMN_PHONE_HASH)),
        createdBy = getString(getColumnIndexOrThrow(VerdictContract.COLUMN_CREATED_BY)),
        seqNum = getLong(getColumnIndexOrThrow(VerdictContract.COLUMN_SEQ_NUM)),
        createdAt = getLong(getColumnIndexOrThrow(VerdictContract.COLUMN_CREATED_AT)),
        signature = getString(getColumnIndexOrThrow(VerdictContract.COLUMN_SIGNATURE)),
    )

    private fun myPublicKey(): String? = identity.publicKeyBase64()

    private fun database(): SQLiteDatabase = helper.writableDatabase

    private fun notifyChanged() {
        listener?.invoke()
    }

    private companion object {
        const val FIRST_SEQ_NUM = 1L
        const val SYNC_STATE_ROW_ID = 1
        const val INSERT_FAILED = -1L
        const val NO_REPORTER = ""
    }
}
