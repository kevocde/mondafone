package dev.polyrics.mondafone.spam

import android.app.Application
import dev.polyrics.mondafone.identity.DeviceIdentity
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class SqliteSpamReportStoreTest : SpamReportStoreContractTest() {

    private val app: Application = RuntimeEnvironment.getApplication()

    override fun createStore(identity: DeviceIdentity): SpamReportStore =
        SqliteSpamReportStore(
            app,
            identity,
            SpamReportHasher(SECRET),
            DeviceReportSigner(identity),
        )

    @After
    fun tearDown() {
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }
}
