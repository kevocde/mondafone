package dev.polyrics.mondafone.spam

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SqliteBlockedCallStore(private val helper: SQLiteOpenHelper) : BlockedCallStore {

    constructor(context: Context) : this(VerdictDbHelper(context))

    override fun record(number: String, at: Long) {
        val values = ContentValues().apply {
            put(VerdictContract.COLUMN_NUMBER, number)
            put(VerdictContract.COLUMN_BLOCKED_AT, at)
        }
        database().insert(VerdictContract.TABLE_BLOCKED_CALLS, null, values)
    }

    override fun count(): Int =
        database()
            .rawQuery("SELECT COUNT(*) FROM ${VerdictContract.TABLE_BLOCKED_CALLS}", null)
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

    private fun database(): SQLiteDatabase = helper.writableDatabase
}
