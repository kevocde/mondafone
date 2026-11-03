package dev.polyrics.mondafone.spam

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import dev.polyrics.mondafone.MondafoneApp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = SpamVerdictServiceTest.TestMondafoneApp::class)
class SpamVerdictServiceTest {

    private val app = RuntimeEnvironment.getApplication() as TestMondafoneApp
    private val store = app.verdictStore
    private val reports = app.spamReportStore
    private val notificationManager = app.getSystemService(NotificationManager::class.java)

    @After
    fun tearDown() {
        store.clear()
        reports.ownReports().forEach { reports.removeOwnReport(it.phoneHash) }
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun promptNotificationUsesServicePendingIntentAndReportsSpamSynchronously() {
        val number = NATIONAL
        store.addPending(CANONICAL)
        NotificationSpamVerdictPromptNotifier(app).promptVerdict(number)
        val notificationId = SpamVerdictService.notificationIdFor(number)
        val notification = shadowOf(notificationManager).getNotification(notificationId)
        assertNotNull(notification)

        val spamPendingIntent = shadowOf(notification.actions[0].actionIntent)
        assertTrue(spamPendingIntent.isService)

        val service = Robolectric.buildService(SpamVerdictService::class.java).create().get()
        val result = service.onStartCommand(spamPendingIntent.savedIntent, 0, 7)

        assertEquals(Service.START_NOT_STICKY, result)
        assertTrue(reports.isReportedByMe(CANONICAL))
        assertEquals(listOf(number), reports.ownReports().map { it.displayNumber })
        assertTrue(store.pendingNumbers().isEmpty())
        assertNull(shadowOf(notificationManager).getNotification(notificationId))
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertEquals(7, shadowOf(service).stopSelfId)
    }

    @Test
    fun blockedNotificationUsesServicePendingIntentAndWhitelistsSynchronously() {
        val number = NATIONAL
        reports.reportOwn(CANONICAL, number)
        NotificationBlockedCallNotifier(app).notifyBlocked(number)
        val notificationId = SpamVerdictService.notificationIdFor(number)
        val notification = shadowOf(notificationManager).getNotification(notificationId)
        assertNotNull(notification)

        val whitelistPendingIntent = shadowOf(notification.actions[1].actionIntent)
        assertTrue(whitelistPendingIntent.isService)

        val service = Robolectric.buildService(SpamVerdictService::class.java).create().get()
        service.onStartCommand(whitelistPendingIntent.savedIntent, 0, 11)

        assertTrue(store.isWhitelisted(CANONICAL))
        assertTrue(reports.isReportedByMe(CANONICAL))
        assertNull(shadowOf(notificationManager).getNotification(notificationId))
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertEquals(11, shadowOf(service).stopSelfId)
    }

    @Test
    fun malformedIntentStopsServiceWithoutMutatingStore() {
        val service = Robolectric.buildService(SpamVerdictService::class.java).create().get()

        service.onStartCommand(Intent("dev.polyrics.mondafone.UNKNOWN"), 0, 3)

        assertTrue(store.pendingNumbers().isEmpty())
        assertTrue(store.whitelistNumbers().isEmpty())
        assertFalse(reports.isReportedByMe(CANONICAL))
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertEquals(3, shadowOf(service).stopSelfId)
    }

    class TestMondafoneApp : MondafoneApp() {

        private val reportIdentity = TestIdentity()

        override fun onCreate() = Unit

        override val spamReportStore: SpamReportStore by lazy {
            SqliteSpamReportStore(
                this,
                reportIdentity,
                SpamReportHasher(REPORT_SECRET),
                DeviceReportSigner(reportIdentity),
            )
        }

        override val phoneNumberNormalizer: PhoneNumberNormalizer by lazy {
            PhoneNumberNormalizer(object : RegionProvider {
                override fun regionIso(): String = "CO"
            })
        }
    }

    private companion object {
        const val REPORT_SECRET = "test-secret"
        const val NATIONAL = "3001234567"
        const val CANONICAL = "00573001234567"
    }
}
