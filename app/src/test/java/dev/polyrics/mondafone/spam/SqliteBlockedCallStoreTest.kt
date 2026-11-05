package dev.polyrics.mondafone.spam

import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class SqliteBlockedCallStoreTest {

    private val app: Application = RuntimeEnvironment.getApplication()
    private lateinit var store: BlockedCallStore

    @Before
    fun setUp() {
        store = SqliteBlockedCallStore(app)
    }

    @After
    fun tearDown() {
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun startsAtZero() {
        assertEquals(0, store.count())
    }

    @Test
    fun countsEveryBlockedCall() {
        store.record("00573001234567", 100)
        store.record("00573001234568", 200)

        assertEquals(2, store.count())
    }

    @Test
    fun countsRepeatedBlocksOfTheSameNumber() {
        store.record("00573001234567", 100)
        store.record("00573001234567", 200)

        assertEquals(2, store.count())
    }
}
