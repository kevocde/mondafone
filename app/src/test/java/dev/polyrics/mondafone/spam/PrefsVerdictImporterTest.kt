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

    private val app: Application = RuntimeEnvironment.getApplication()
    private val prefs = FakeSharedPreferences()
    private val identity = TestIdentity()
    private lateinit var store: CallVerdictStore
    private lateinit var reports: SpamReportStore

    @Before
    fun setUp() {
        store = SqliteCallVerdictStore(app)
        reports = SqliteSpamReportStore(
            app,
            identity,
            SpamReportHasher(SECRET),
            DeviceReportSigner(identity),
        )
    }

    @After
    fun tearDown() {
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun importsWhitelistReportAndPendingAsCanonicalNumbers() {
        prefs.put(NATIONAL_WHITELIST, VerdictContract.VERDICT_WHITELIST)
        prefs.put(NATIONAL_SPAM, VerdictContract.VERDICT_BLACKLIST)
        prefs.put(NATIONAL_PENDING, VerdictContract.VERDICT_PENDING)

        importer().runOnce()

        assertTrue(store.isWhitelisted(CANONICAL_WHITELIST))
        assertTrue(reports.isReportedByMe(CANONICAL_SPAM))
        assertEquals(listOf(NATIONAL_SPAM), reports.ownReports().map { it.displayNumber })
        assertEquals(listOf(CANONICAL_PENDING), store.pendingNumbers())
    }

    @Test
    fun mapsLegacySpamToOwnReport() {
        prefs.put(NATIONAL_SPAM, LEGACY_SPAM)

        importer().runOnce()

        assertTrue(reports.isReportedByMe(CANONICAL_SPAM))
    }

    @Test
    fun mapsLegacyNotSpamToCanonicalWhitelist() {
        prefs.put(NATIONAL_WHITELIST, LEGACY_NOT_SPAM)

        importer().runOnce()

        assertTrue(store.isWhitelisted(CANONICAL_WHITELIST))
    }

    @Test
    fun ignoresUnknownValues() {
        prefs.put(NATIONAL_WHITELIST, "unexpected")

        importer().runOnce()

        assertFalse(store.isClassified(CANONICAL_WHITELIST))
        assertEquals(emptyList<String>(), store.pendingNumbers())
        assertTrue(reports.ownReports().isEmpty())
    }

    @Test
    fun clearsPreferencesAfterImport() {
        prefs.put(NATIONAL_SPAM, VerdictContract.VERDICT_BLACKLIST)

        importer().runOnce()

        assertNull(prefs.getString(NATIONAL_SPAM, null))
    }

    @Test
    fun runsOnlyOnce() {
        prefs.put(NATIONAL_SPAM, VerdictContract.VERDICT_BLACKLIST)
        importer().runOnce()

        prefs.put(NATIONAL_PENDING, VerdictContract.VERDICT_BLACKLIST)
        importer().runOnce()

        assertTrue(reports.isReportedByMe(CANONICAL_SPAM))
        assertFalse(reports.isReportedByMe(CANONICAL_PENDING))
    }

    private fun importer(): PrefsVerdictImporter {
        val normalizer = PhoneNumberNormalizer(object : RegionProvider {
            override fun regionIso(): String = "ES"
        })
        return PrefsVerdictImporter(prefs, store, reports, normalizer)
    }

    private companion object {
        const val SECRET = "test-secret"
        const val LEGACY_SPAM = "spam"
        const val LEGACY_NOT_SPAM = "not_spam"
        const val NATIONAL_WHITELIST = "600111222"
        const val NATIONAL_SPAM = "600333444"
        const val NATIONAL_PENDING = "600555666"
        const val CANONICAL_WHITELIST = "0034600111222"
        const val CANONICAL_SPAM = "0034600333444"
        const val CANONICAL_PENDING = "0034600555666"
    }
}
