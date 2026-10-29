package dev.polyrics.mondafone.network

import android.app.AlarmManager
import android.app.Application
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class BootReceiverTest {

    private val context = RuntimeEnvironment.getApplication()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @Test
    fun reschedulesBeaconAlarmOnBoot() {
        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertFalse(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun ignoresOtherActions() {
        BootReceiver().onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }
}
