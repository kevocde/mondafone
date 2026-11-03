package dev.polyrics.mondafone.spam

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class VerdictDbHelperTest {

    private val app: Application = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun createsAllTablesOnFreshDatabase() {
        val db = VerdictDbHelper(app).writableDatabase

        listOf(
            VerdictContract.TABLE_VERDICTS,
            VerdictContract.TABLE_SPAM_REPORTS,
            VerdictContract.TABLE_SPAM_REPORT_ALIASES,
            VerdictContract.TABLE_PEERS_STATUS,
            VerdictContract.TABLE_LOCAL_SYNC_STATE,
        ).forEach { assertTrue("missing table $it", db.tableExists(it)) }
    }

    @Test
    fun preservesVerdictsWhenUpgradingFromVersionOne() {
        createVersionOneDatabase()

        val db = VerdictDbHelper(app).writableDatabase

        assertTrue(db.tableExists(VerdictContract.TABLE_SPAM_REPORTS))
        assertTrue(db.tableExists(VerdictContract.TABLE_PEERS_STATUS))
        assertEquals(1, db.rowCount(VerdictContract.TABLE_VERDICTS))
    }

    private fun createVersionOneDatabase() {
        val path = app.getDatabasePath(VerdictContract.DATABASE_NAME)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL(VerdictContract.CREATE_VERDICTS_TABLE)
            db.execSQL("INSERT INTO ${VerdictContract.TABLE_VERDICTS} VALUES ('600111222', 'whitelist')")
            db.version = 1
        }
    }

    private fun SQLiteDatabase.tableExists(name: String): Boolean =
        rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name))
            .use { it.moveToFirst() }

    private fun SQLiteDatabase.rowCount(table: String): Int =
        rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
