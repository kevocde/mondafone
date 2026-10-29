package dev.polyrics.mondafone.spam

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class VerdictDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    VerdictContract.DATABASE_NAME,
    null,
    VerdictContract.DATABASE_VERSION,
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(VerdictContract.CREATE_VERDICTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}
