package dev.polyrics.mondafono.ui

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RetroProtectionBannerTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun loadsLauncherForegroundAsEmblem() {
        val banner = RetroProtectionBanner(context, active = true)

        val emblem = banner.emblemBitmap

        assertNotNull(emblem)
        assertTrue(emblem!!.width > 0)
        assertTrue(emblem.height > 0)
    }
}
