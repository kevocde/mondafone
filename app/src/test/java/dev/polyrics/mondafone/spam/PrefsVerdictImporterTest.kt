package dev.polyrics.mondafone.spam

import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class PrefsVerdictImporterTest {

    private val prefs = FakeSharedPreferences()
    private lateinit var store: CallVerdictStore

    @Before
    fun setUp() {
        store = SqliteCallVerdictStore(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        RuntimeEnvironment.getApplication().deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun importsWhitelistBlacklistAndPending() {
        prefs.put("600111222", "whitelist")
        prefs.put("600333444", "blacklist")
        prefs.put("600555666", "pending")

        importer().runOnce()

        assertTrue(store.isWhitelisted("600111222"))
        assertTrue(store.isBlacklisted("600333444"))
        assertEquals(listOf("600555666"), store.pendingNumbers())
    }

    @Test
    fun mapsLegacySpamToBlacklist() {
        prefs.put("600111222", "spam")

        importer().runOnce()

        assertTrue(store.isBlacklisted("600111222"))
    }

    @Test
    fun mapsLegacyNotSpamToWhitelist() {
        prefs.put("600111222", "not_spam")

        importer().runOnce()

        assertTrue(store.isWhitelisted("600111222"))
    }

    @Test
    fun ignoresUnknownValues() {
        prefs.put("600111222", "unexpected")

        importer().runOnce()

        assertFalse(store.isClassified("600111222"))
        assertEquals(emptyList<String>(), store.pendingNumbers())
    }

    @Test
    fun clearsPreferencesAfterImport() {
        prefs.put("600111222", "blacklist")

        importer().runOnce()

        assertNull(prefs.getString("600111222", null))
    }

    @Test
    fun runsOnlyOnce() {
        prefs.put("600111222", "blacklist")
        importer().runOnce()

        prefs.put("600333444", "blacklist")
        importer().runOnce()

        assertTrue(store.isBlacklisted("600111222"))
        assertFalse(store.isBlacklisted("600333444"))
    }

    private fun importer(): PrefsVerdictImporter = PrefsVerdictImporter(prefs, store)
}
