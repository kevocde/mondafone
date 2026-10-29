package dev.polyrics.mondafone.spam

import android.app.Application
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class SqliteCallVerdictStoreTest : CallVerdictStoreContractTest() {

    override fun createStore(): CallVerdictStore =
        SqliteCallVerdictStore(RuntimeEnvironment.getApplication())

    @After
    fun tearDown() {
        RuntimeEnvironment.getApplication().deleteDatabase(VerdictContract.DATABASE_NAME)
    }
}
