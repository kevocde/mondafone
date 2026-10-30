package dev.polyrics.mondafone.spam

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper

class SqliteLegacyBlacklistReader(private val helper: SQLiteOpenHelper) : LegacyBlacklistReader {

    constructor(context: Context) : this(VerdictDbHelper(context))

    override fun blacklistNumbers(): List<String> =
        helper.writableDatabase
            .query(
                VerdictContract.TABLE_VERDICTS,
                arrayOf(VerdictContract.COLUMN_NUMBER),
                "${VerdictContract.COLUMN_VERDICT} = ?",
                arrayOf(VerdictContract.VERDICT_BLACKLIST),
                null,
                null,
                null,
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }
}
