package dev.polyrics.mondafone.spam

import android.app.Application
import android.content.ContentValues
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class CanonicalVerdictMigratorTest {

    private val app: Application = RuntimeEnvironment.getApplication()
    private val identity = TestIdentity()
    private val hasher = SpamReportHasher(SECRET)
    private val normalizer = PhoneNumberNormalizer(FixedRegion("CO"))
    private val store = SqliteCallVerdictStore(app)
    private val reportStore = SqliteSpamReportStore(app, identity, hasher, DeviceReportSigner(identity))
    private val legacyBlacklist = SqliteLegacyBlacklistReader(app)
    private val prefs = FakeSharedPreferences()
    private val migrator = CanonicalVerdictMigrator(prefs, store, reportStore, legacyBlacklist, normalizer)

    @Before
    fun setUp() {
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @After
    fun tearDown() {
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun normalizesWhitelistNumbers() {
        store.addToWhitelist(NATIONAL)

        migrator.runOnce()

        assertTrue(store.isWhitelisted(CANONICAL))
        assertFalse(store.isWhitelisted(NATIONAL))
    }

    @Test
    fun keepsAlreadyCanonicalWhitelistUntouched() {
        store.addToWhitelist(CANONICAL)

        migrator.runOnce()

        assertEquals(listOf(CANONICAL), store.whitelistNumbers())
    }

    @Test
    fun normalizesPendingNumbers() {
        store.addPending(NATIONAL)

        migrator.runOnce()

        assertEquals(listOf(CANONICAL), store.pendingNumbers())
    }

    @Test
    fun convertsLegacyBlacklistIntoOwnReport() {
        insertLegacyBlacklist(NATIONAL)

        migrator.runOnce()

        assertTrue(reportStore.isReportedByMe(CANONICAL))
        assertEquals(listOf(NATIONAL), reportStore.ownReports().map { it.displayNumber })
        assertTrue(legacyBlacklist.blacklistNumbers().isEmpty())
        assertFalse(store.isClassified(NATIONAL))
    }

    @Test
    fun runsOnlyOnce() {
        migrator.runOnce()
        store.addToWhitelist(NATIONAL)

        migrator.runOnce()

        assertTrue(store.isWhitelisted(NATIONAL))
    }

    private fun insertLegacyBlacklist(number: String) {
        val values = ContentValues().apply {
            put(VerdictContract.COLUMN_NUMBER, number)
            put(VerdictContract.COLUMN_VERDICT, VerdictContract.VERDICT_BLACKLIST)
        }
        VerdictDbHelper(app).writableDatabase.insert(VerdictContract.TABLE_VERDICTS, null, values)
    }

    private class FixedRegion(private val iso: String) : RegionProvider {
        override fun regionIso(): String = iso
    }

    private companion object {
        const val SECRET = "test-secret"
        const val NATIONAL = "3001234567"
        const val CANONICAL = "00573001234567"
    }
}
