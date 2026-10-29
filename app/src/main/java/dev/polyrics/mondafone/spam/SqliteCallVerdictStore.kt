package dev.polyrics.mondafone.spam

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SqliteCallVerdictStore(private val helper: SQLiteOpenHelper) : CallVerdictStore {

    constructor(context: Context) : this(VerdictDbHelper(context))

    @Volatile
    private var listener: (() -> Unit)? = null

    override fun isWhitelisted(number: String): Boolean =
        verdictOf(number) == VerdictContract.VERDICT_WHITELIST

    override fun isBlacklisted(number: String): Boolean =
        verdictOf(number) == VerdictContract.VERDICT_BLACKLIST

    override fun isClassified(number: String): Boolean =
        verdictOf(number) in CLASSIFIED_VERDICTS

    override fun addToWhitelist(number: String) =
        writeVerdict(number, VerdictContract.VERDICT_WHITELIST)

    override fun addToBlacklist(number: String) =
        writeVerdict(number, VerdictContract.VERDICT_BLACKLIST)

    override fun addPending(number: String) {
        if (isClassified(number)) return
        writeVerdict(number, VerdictContract.VERDICT_PENDING)
    }

    override fun pendingNumbers(): List<String> =
        numbersWith(VerdictContract.VERDICT_PENDING)

    override fun whitelistNumbers(): List<String> =
        numbersWith(VerdictContract.VERDICT_WHITELIST)

    override fun blacklistNumbers(): List<String> =
        numbersWith(VerdictContract.VERDICT_BLACKLIST)

    override fun remove(number: String) {
        database().delete(
            VerdictContract.TABLE_VERDICTS,
            "${VerdictContract.COLUMN_NUMBER} = ?",
            arrayOf(number),
        )
        notifyChanged()
    }

    override fun stats(): VerdictStats = VerdictStats(
        whitelist = count(VerdictContract.VERDICT_WHITELIST),
        blacklist = count(VerdictContract.VERDICT_BLACKLIST),
    )

    override fun clear() {
        database().delete(VerdictContract.TABLE_VERDICTS, null, null)
        notifyChanged()
    }

    override fun registerOnChangeListener(listener: () -> Unit) {
        this.listener = listener
    }

    override fun unregisterOnChangeListener() {
        listener = null
    }

    private fun verdictOf(number: String): String? =
        queryColumn(VerdictContract.COLUMN_VERDICT, "${VerdictContract.COLUMN_NUMBER} = ?", number)
            .firstOrNull()

    private fun numbersWith(verdict: String): List<String> =
        queryColumn(VerdictContract.COLUMN_NUMBER, "${VerdictContract.COLUMN_VERDICT} = ?", verdict)

    private fun count(verdict: String): Int =
        database()
            .rawQuery("SELECT COUNT(*) FROM ${VerdictContract.TABLE_VERDICTS} WHERE ${VerdictContract.COLUMN_VERDICT} = ?", arrayOf(verdict))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

    private fun queryColumn(column: String, selection: String, argument: String): List<String> =
        database()
            .query(
                VerdictContract.TABLE_VERDICTS,
                arrayOf(column),
                selection,
                arrayOf(argument),
                null,
                null,
                null,
            )
            .use { cursor -> cursor.readStrings() }

    private fun writeVerdict(number: String, verdict: String) {
        val values = ContentValues().apply {
            put(VerdictContract.COLUMN_NUMBER, number)
            put(VerdictContract.COLUMN_VERDICT, verdict)
        }
        database().insertWithOnConflict(
            VerdictContract.TABLE_VERDICTS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
        notifyChanged()
    }

    private fun database(): SQLiteDatabase = helper.writableDatabase

    private fun notifyChanged() {
        listener?.invoke()
    }

    private fun Cursor.readStrings(): List<String> {
        val values = mutableListOf<String>()
        while (moveToNext()) values.add(getString(0))
        return values
    }

    private companion object {
        val CLASSIFIED_VERDICTS = setOf(
            VerdictContract.VERDICT_WHITELIST,
            VerdictContract.VERDICT_BLACKLIST,
        )
    }
}
