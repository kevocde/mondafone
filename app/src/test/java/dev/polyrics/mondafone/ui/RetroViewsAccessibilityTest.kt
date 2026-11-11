package dev.polyrics.mondafone.ui

import android.graphics.Color
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.Button
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RetroViewsAccessibilityTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun retroButtonExposesButtonRoleAndTriggersOnAccessibilityClick() {
        var tapped = false
        val button = RetroButton(context, "Grant access", Color.CYAN) { tapped = true }

        val info = button.createAccessibilityNodeInfo()

        assertTrue(button.isClickable)
        assertTrue(button.isFocusable)
        assertEquals(Button::class.java.name, info.className.toString())
        assertEquals("Grant access", button.contentDescription.toString())
        assertTrue(info.actionList.contains(AccessibilityAction.ACTION_CLICK))
        assertTrue(button.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))
        assertTrue(tapped)
    }

    @Test
    fun retroTabBarExposesSingleSelectionTabsBadgesAndSwitchesOnAccessibilityClick() {
        var selectedTab = 0
        val tabBar = RetroTabBar(context, listOf("Pending", "Whitelist", "Blacklist")) {
            selectedTab = it
        }
        tabBar.setCounts(listOf(3, 0, 120))

        val barInfo = tabBar.createAccessibilityNodeInfo()
        assertEquals(3, tabBar.childCount)
        assertEquals(3, barInfo.collectionInfo.columnCount)

        val firstTab = tabBar.getChildAt(0)
        val firstInfo = firstTab.createAccessibilityNodeInfo()
        assertTrue(firstInfo.isSelected)
        assertEquals("Pending (3)", firstTab.contentDescription.toString())
        assertEquals(0, firstInfo.collectionItemInfo.columnIndex)

        val thirdTab = tabBar.getChildAt(2)
        assertEquals("Blacklist (99+)", thirdTab.contentDescription.toString())
        assertTrue(thirdTab.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))
        assertEquals(2, selectedTab)
        assertTrue(thirdTab.createAccessibilityNodeInfo().isSelected)
    }

    @Test
    fun retroStatsBarExposesEachValueToAccessibility() {
        val statsBar = RetroStatsBar(context, listOf("Blocked calls", "Spam numbers", "Devices"))
        statsBar.setValues(listOf(3, 12, 2))

        assertEquals(3, statsBar.childCount)
        assertEquals("Blocked calls: 3", statsBar.getChildAt(0).contentDescription.toString())
        assertEquals("Spam numbers: 12", statsBar.getChildAt(1).contentDescription.toString())
        assertEquals("Devices: 2", statsBar.getChildAt(2).contentDescription.toString())
    }

    @Test
    fun retroDebugBoxAggregatesLinesAndHidesDynamicDescendantsFromFocus() {
        var cleared = false
        val debugBox = RetroDebugBox(context) { cleared = true }
        debugBox.setLines(listOf("Device ID: abc", "Power: unrestricted"))

        val info = debugBox.createAccessibilityNodeInfo()
        val summary = info.contentDescription.toString()
        val header = debugBox.getChildAt(0) as android.widget.LinearLayout
        val clearButton = header.getChildAt(1)

        assertTrue(debugBox.isScreenReaderFocusable)
        assertTrue(summary.contains("Debug:"))
        assertTrue(summary.contains("Device ID: abc"))
        assertTrue(summary.contains("Power: unrestricted"))
        assertEquals(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
            debugBox.getChildAt(1).importantForAccessibility,
        )
        assertTrue(clearButton.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null))
        assertTrue(cleared)
    }

    @Test
    fun retroTitleExposesAccessibleHeading() {
        val title = RetroTitle(context, "Mondafone")

        val info = title.createAccessibilityNodeInfo()

        assertTrue(info.isHeading)
        assertEquals("Mondafone", title.contentDescription.toString())
    }
}
