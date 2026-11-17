package dev.polyrics.mondafone

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import dev.polyrics.mondafone.spam.DeviceReportSigner
import dev.polyrics.mondafone.spam.SpamReportHasher
import dev.polyrics.mondafone.spam.SpamReportStore
import dev.polyrics.mondafone.spam.SqliteSpamReportStore
import dev.polyrics.mondafone.spam.TestIdentity
import dev.polyrics.mondafone.spam.VerdictContract
import dev.polyrics.mondafone.ui.RetroDebugBox
import dev.polyrics.mondafone.ui.RetroEmptyStateView
import dev.polyrics.mondafone.ui.RetroProtectionBanner
import dev.polyrics.mondafone.ui.RetroReadinessStrip
import dev.polyrics.mondafone.ui.RetroStatsBar
import dev.polyrics.mondafone.ui.RetroTabBar
import org.junit.After
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
import org.robolectric.shadows.ShadowRoleManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = MainActivityOnboardingTest.TestMondafoneApp::class)
class MainActivityOnboardingTest {

    private val app = RuntimeEnvironment.getApplication() as TestMondafoneApp

    @After
    fun tearDown() {
        ShadowRoleManager.reset()
        app.deleteDatabase(VerdictContract.DATABASE_NAME)
    }

    @Test
    fun switchesFromOnboardingToMainContentOncePermissionsAreGranted() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = controller.get()

        assertNull(findViewOfType(activity.window.decorView, RetroTabBar::class.java))
        assertNotNull(findViewOfType(activity.window.decorView, RetroProtectionBanner::class.java))
        assertNotNull(findViewOfType(activity.window.decorView, RetroReadinessStrip::class.java))
        assertTrue(hasViewWithDescription(activity.window.decorView, activity.getString(R.string.grant_access)))

        val roleManager = app.getSystemService(RoleManager::class.java)
        org.robolectric.shadow.api.Shadow.extract<ShadowRoleManager>(roleManager)
            .addHeldRole(RoleManager.ROLE_CALL_SCREENING)
        shadowOf(app).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )

        activity.onRequestPermissionsResult(
            3,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            intArrayOf(PackageManager.PERMISSION_GRANTED),
        )

        assertNotNull(findViewOfType(activity.window.decorView, RetroProtectionBanner::class.java))
        assertNotNull(findViewOfType(activity.window.decorView, RetroReadinessStrip::class.java))
        assertNotNull(findViewOfType(activity.window.decorView, RetroStatsBar::class.java))
        assertNotNull(findViewOfType(activity.window.decorView, RetroTabBar::class.java))
        assertNotNull(findViewOfType(activity.window.decorView, RetroEmptyStateView::class.java))
        assertNotNull(findViewOfType(activity.window.decorView, RetroDebugBox::class.java))
        assertFalse(hasViewWithDescription(activity.window.decorView, activity.getString(R.string.grant_access)))
    }

    @Test
    fun hidesDebugBoxWhenDebugModeIsDisabled() {
        val roleManager = app.getSystemService(RoleManager::class.java)
        org.robolectric.shadow.api.Shadow.extract<ShadowRoleManager>(roleManager)
            .addHeldRole(RoleManager.ROLE_CALL_SCREENING)
        shadowOf(app).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )

        val controller = Robolectric.buildActivity(ReleaseLikeMainActivity::class.java).create().start().resume()
        val activity = controller.get()

        assertNotNull(findViewOfType(activity.window.decorView, RetroStatsBar::class.java))
        assertNotNull(findViewOfType(activity.window.decorView, RetroTabBar::class.java))
        assertNull(findViewOfType(activity.window.decorView, RetroDebugBox::class.java))
    }

    class ReleaseLikeMainActivity : MainActivity() {
        override fun isDebugEnabled(): Boolean = false
    }

    private fun <T : View> findViewOfType(root: View, target: Class<T>): T? {
        if (target.isInstance(root)) return target.cast(root)
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findViewOfType(root.getChildAt(i), target)
                if (found != null) return found
            }
        }
        return null
    }

    private fun hasViewWithDescription(root: View, description: String): Boolean {
        if (root.contentDescription?.toString() == description) return true
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                if (hasViewWithDescription(root.getChildAt(i), description)) return true
            }
        }
        return false
    }

    class TestMondafoneApp : MondafoneApp() {
        override val deviceIdentity = TestIdentity()

        override fun onCreate() = Unit

        override val spamReportStore: SpamReportStore by lazy {
            SqliteSpamReportStore(
                this,
                deviceIdentity,
                SpamReportHasher("test-secret"),
                DeviceReportSigner(deviceIdentity),
            )
        }
    }
}
