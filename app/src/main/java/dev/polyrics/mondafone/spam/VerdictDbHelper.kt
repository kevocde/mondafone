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
        VerdictContract.CREATE_ALL_TABLES.forEach(db::execSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) VerdictContract.CREATE_TABLES_ADDED_IN_VERSION_TWO.forEach(db::execSQL)
    }
}
