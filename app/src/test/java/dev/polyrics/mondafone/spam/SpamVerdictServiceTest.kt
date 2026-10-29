package dev.polyrics.mondafone.spam

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import dev.polyrics.mondafone.MondafoneApp
import org.junit.After
import org.junit.Assert.assertEquals
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
@Config(sdk = [31])
class SpamVerdictServiceTest {

    private val app = RuntimeEnvironment.getApplication() as MondafoneApp
    private val store = app.verdictStore
    private val notificationManager = app.getSystemService(NotificationManager::class.java)

    @After
    fun tearDown() {
        store.clear()
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun promptNotificationUsesServicePendingIntentAndBlacklistsSynchronously() {
        val number = "600111222"
        store.addPending(number)
        NotificationSpamVerdictPromptNotifier(app).promptVerdict(number)
        val notificationId = SpamVerdictService.notificationIdFor(number)
        val notification = shadowOf(notificationManager).getNotification(notificationId)
        assertNotNull(notification)

        val spamPendingIntent = shadowOf(notification.actions[0].actionIntent)
        assertTrue(spamPendingIntent.isServiceIntent)

        val service = Robolectric.buildService(SpamVerdictService::class.java).create().get()
        val result = service.onStartCommand(spamPendingIntent.savedIntent, 0, 7)

        assertEquals(Service.START_NOT_STICKY, result)
        assertTrue(store.isBlacklisted(number))
        assertNull(shadowOf(notificationManager).getNotification(notificationId))
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertEquals(7, shadowOf(service).stopSelfId)
    }

    @Test
    fun blockedNotificationUsesServicePendingIntentAndWhitelistsSynchronously() {
        val number = "600333444"
        store.addToBlacklist(number)
        NotificationBlockedCallNotifier(app).notifyBlocked(number)
        val notificationId = SpamVerdictService.notificationIdFor(number)
        val notification = shadowOf(notificationManager).getNotification(notificationId)
        assertNotNull(notification)

        val whitelistPendingIntent = shadowOf(notification.actions[1].actionIntent)
        assertTrue(whitelistPendingIntent.isServiceIntent)

        val service = Robolectric.buildService(SpamVerdictService::class.java).create().get()
        service.onStartCommand(whitelistPendingIntent.savedIntent, 0, 11)

        assertTrue(store.isWhitelisted(number))
        assertNull(shadowOf(notificationManager).getNotification(notificationId))
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertEquals(11, shadowOf(service).stopSelfId)
    }

    @Test
    fun malformedIntentStopsServiceWithoutMutatingStore() {
        val service = Robolectric.buildService(SpamVerdictService::class.java).create().get()

        service.onStartCommand(Intent("dev.polyrics.mondafone.UNKNOWN"), 0, 3)

        assertEquals(0, store.stats().total)
        assertTrue(shadowOf(service).isStoppedBySelf)
        assertEquals(3, shadowOf(service).stopSelfId)
    }
}
