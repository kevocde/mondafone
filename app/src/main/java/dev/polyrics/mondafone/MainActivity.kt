package dev.polyrics.mondafone

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import dev.polyrics.mondafone.spam.PhoneNumberDisplay
import dev.polyrics.mondafone.ui.RetroButton
import dev.polyrics.mondafone.ui.RetroDebugBox
import dev.polyrics.mondafone.ui.RetroEmptyStateView
import dev.polyrics.mondafone.ui.RetroPanel
import dev.polyrics.mondafone.ui.RetroProtectionBanner
import dev.polyrics.mondafone.ui.RetroReadinessStrip
import dev.polyrics.mondafone.ui.RetroStatsBar
import dev.polyrics.mondafone.ui.RetroTabBar
import dev.polyrics.mondafone.ui.VerdictListItemView
import java.util.concurrent.TimeUnit

open class MainActivity : Activity() {

    private val store by lazy { (application as MondafoneApp).verdictStore }
    private val reports by lazy { (application as MondafoneApp).spamReportStore }
    private val identity by lazy { (application as MondafoneApp).deviceIdentity }
    private val blockedCalls by lazy { (application as MondafoneApp).blockedCallStore }
    private val beaconLog by lazy { (application as MondafoneApp).beaconEventLog }
    private lateinit var listContainer: LinearLayout
    private lateinit var tabBar: RetroTabBar
    private lateinit var statsBar: RetroStatsBar
    private lateinit var protectionBanner: RetroProtectionBanner
    private lateinit var readinessStrip: RetroReadinessStrip
    private var debugBox: RetroDebugBox? = null
    private var mainContentMounted = false
    private var currentTab = 0
    private var batteryExemptionPrompted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(if (isReady()) buildContent() else buildOnboardingContent())
    }

    override fun onResume() {
        super.onResume()
        store.registerOnChangeListener { runOnUiThread { renderList() } }
        reports.registerOnChangeListener { runOnUiThread { renderList() } }
        if (isDebugEnabled()) {
            beaconLog.setListener { events -> runOnUiThread { renderDebug(events) } }
        }
        if (isBluetoothGranted()) (application as MondafoneApp).scheduleBeacon()
        promptBatteryExemptionIfNeeded()
        renderList()
    }

    override fun onPause() {
        store.unregisterOnChangeListener()
        reports.unregisterOnChangeListener()
        beaconLog.setListener(null)
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_NOTIFICATIONS_CODE ->
                if (isNotificationGranted()) requestBluetoothPermissions() else refreshView()
            REQUEST_BLUETOOTH_CODE -> {
                if (isBluetoothGranted()) {
                    (application as MondafoneApp).scheduleBeacon()
                    promptBatteryExemptionIfNeeded()
                }
                refreshView()
            }
            else -> refreshView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ROLE_CODE && isCallScreeningRoleHeld()) {
            requestAccess()
        } else {
            refreshView()
        }
    }

    private fun buildContent(): LinearLayout {
        mainContentMounted = true
        val density = resources.displayMetrics.density
        val pad = (20 * density).toInt()
        val gap = (12 * density).toInt()
        val smallGap = (8 * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            applySystemBarPadding(this, pad)
            setBackgroundColor(getColor(R.color.background))
            clipChildren = false
            protectionBanner = RetroProtectionBanner(this@MainActivity, active = true)
            addView(protectionBanner, matchWidthParams(bottom = smallGap))
            readinessStrip = createReadinessStrip()
            addView(readinessStrip, matchWidthParams(bottom = gap))
            statsBar = RetroStatsBar(
                this@MainActivity,
                listOf(
                    getString(R.string.stat_blocked_calls),
                    getString(R.string.stat_spam_numbers),
                    getString(R.string.stat_network_devices),
                ),
            )
            addView(statsBar, matchWidthParams(bottom = (18 * density).toInt()))
            tabBar = RetroTabBar(
                this@MainActivity,
                listOf(
                    getString(R.string.tab_pending),
                    getString(R.string.tab_whitelist),
                    getString(R.string.tab_blacklist),
                ),
            ) { index ->
                currentTab = index
                renderList()
            }
            addView(tabBar, marginParams(bottom = 0))
            listContainer = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(
                ScrollView(this@MainActivity).apply {
                    addView(
                        RetroPanel(this@MainActivity).apply {
                            addView(listContainer)
                        },
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                        ),
                    )
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ),
            )
            if (isDebugEnabled()) {
                debugBox = RetroDebugBox(this@MainActivity) { beaconLog.clear() }
                addView(
                    debugBox,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { topMargin = gap },
                )
                renderDebug(beaconLog.lines())
            } else {
                debugBox = null
            }
        }
    }

    private fun createReadinessStrip(): RetroReadinessStrip =
        RetroReadinessStrip(
            this,
            listOf(
                getString(R.string.stage_call_filter),
                getString(R.string.stage_notifications),
                getString(R.string.stage_ble_mesh),
                getString(R.string.stage_battery),
            ),
        ) {
            requestAccess()
        }

    private fun currentStageStates(): List<Boolean> =
        listOf(
            isCallScreeningRoleHeld(),
            isNotificationGranted(),
            isBluetoothGranted(),
            isIgnoringBatteryOptimizations(),
        )

    private fun matchWidthParams(bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = bottom }

    private fun marginParams(bottom: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = bottom }

    private fun refreshView() {
        if (isReady()) {
            renderList()
        } else {
            setContentView(buildOnboardingContent())
        }
    }

    private fun applySystemBarPadding(target: View, basePad: Int) {
        target.setPadding(basePad, basePad, basePad, basePad)
        target.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(basePad + bars.left, basePad + bars.top, basePad + bars.right, basePad + bars.bottom)
            insets
        }
    }

    private fun buildOnboardingContent(): LinearLayout {
        mainContentMounted = false
        debugBox = null
        val density = resources.displayMetrics.density
        val pad = (20 * density).toInt()
        val gap = (12 * density).toInt()
        val smallGap = (8 * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            applySystemBarPadding(this, pad)
            setBackgroundColor(getColor(R.color.background))
            clipChildren = false
            addView(RetroProtectionBanner(this@MainActivity, active = false), matchWidthParams(bottom = smallGap))
            val onboardingStrip = createReadinessStrip().apply {
                setStages(currentStageStates())
            }
            addView(onboardingStrip, matchWidthParams(bottom = gap * 2))
            addView(
                RetroButton(this@MainActivity, getString(R.string.grant_access), getColor(R.color.primary)) {
                    requestAccess()
                },
                matchWidthParams(bottom = 0),
            )
        }
    }

    private fun renderList() {
        if (!isReady()) {
            setContentView(buildOnboardingContent())
            return
        }
        if (!mainContentMounted) {
            setContentView(buildContent())
        }
        val stages = currentStageStates()
        readinessStrip.setStages(stages)
        tabBar.setCounts(
            listOf(
                store.pendingNumbers().size,
                store.whitelistNumbers().size,
                reports.ownReports().size,
            ),
        )
        val peers = renderStats()
        protectionBanner.setStatus(
            active = true,
            readyStages = stages.count { it },
            totalStages = stages.size,
            peerCount = peers,
        )
        listContainer.removeAllViews()
        when (currentTab) {
            0 -> renderPendingItems()
            1 -> renderWhitelistItems()
            2 -> renderBlacklistItems()
        }
    }

    private fun renderStats(): Int {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(BuildConfig.REPORT_MAX_AGE_DAYS)
        val peers = reports.networkDeviceCount(cutoff, identity.publicKeyBase64() ?: "")
        statsBar.setValues(
            listOf(
                blockedCalls.count(),
                reports.spamNumberCount(cutoff),
                peers,
            ),
        )
        return peers
    }

    private fun renderPendingItems() {
        val numbers = store.pendingNumbers()
        if (numbers.isEmpty()) {
            showEmptyMessage(getString(R.string.empty_pending))
            return
        }
        numbers.forEachIndexed { index, number ->
            val display = PhoneNumberDisplay.format(number)
            val bottomGap = if (index < numbers.lastIndex) (8 * resources.displayMetrics.density).toInt() else 0
            listContainer.addView(
                VerdictListItemView(
                    this,
                    display,
                    listOf(
                        VerdictListItemView.Action(
                            getString(R.string.action_spam),
                            getColor(R.color.secondary),
                        ) {
                            reports.reportOwn(number, display)
                            store.remove(number)
                        },
                        VerdictListItemView.Action(
                            getString(R.string.action_not_spam),
                            getColor(R.color.primary),
                        ) {
                            store.addToWhitelist(number)
                        },
                    ),
                    subtitle = getString(R.string.spam_question),
                ),
                matchWidthParams(bottom = bottomGap),
            )
        }
    }

    private fun renderWhitelistItems() {
        val numbers = store.whitelistNumbers()
        if (numbers.isEmpty()) {
            showEmptyMessage(getString(R.string.empty_whitelist))
            return
        }
        numbers.forEachIndexed { index, number ->
            val bottomGap = if (index < numbers.lastIndex) (8 * resources.displayMetrics.density).toInt() else 0
            listContainer.addView(
                VerdictListItemView(
                    this,
                    PhoneNumberDisplay.format(number),
                    listOf(
                        VerdictListItemView.Action(
                            getString(R.string.action_remove),
                            getColor(R.color.secondary),
                        ) {
                            store.remove(number)
                        },
                    ),
                    bandColor = getColor(R.color.primary),
                ),
                matchWidthParams(bottom = bottomGap),
            )
        }
    }

    private fun renderBlacklistItems() {
        val ownReports = reports.ownReports()
        if (ownReports.isEmpty()) {
            showEmptyMessage(getString(R.string.empty_blacklist))
            return
        }
        ownReports.forEachIndexed { index, report ->
            val bottomGap = if (index < ownReports.lastIndex) (8 * resources.displayMetrics.density).toInt() else 0
            listContainer.addView(
                VerdictListItemView(
                    this,
                    report.displayNumber,
                    listOf(
                        VerdictListItemView.Action(
                            getString(R.string.action_remove),
                            getColor(R.color.secondary),
                        ) {
                            reports.removeOwnReport(report.phoneHash)
                        },
                    ),
                    bandColor = getColor(R.color.secondary),
                ),
                matchWidthParams(bottom = bottomGap),
            )
        }
    }

    private fun showEmptyMessage(message: String) {
        listContainer.addView(
            RetroEmptyStateView(this, message),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun renderDebug(events: List<String>) {
        val box = debugBox ?: return
        val deviceId = getString(R.string.debug_device_id, identity.publicKeyFingerprint() ?: "-")
        val power = getString(
            if (isIgnoringBatteryOptimizations()) {
                R.string.debug_power_unrestricted
            } else {
                R.string.debug_power_restricted
            },
        )
        box.setLines(listOf(deviceId, power) + events)
    }

    protected open fun isDebugEnabled(): Boolean = BuildConfig.DEBUG

    private fun isReady(): Boolean =
        isCallScreeningRoleHeld() && isNotificationGranted()

    private fun isNotificationGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            isGranted(Manifest.permission.POST_NOTIFICATIONS)

    private fun isBluetoothGranted(): Boolean =
        BLUETOOTH_PERMISSIONS.all { isGranted(it) }

    private fun isGranted(permission: String): Boolean =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun isCallScreeningRoleHeld(): Boolean =
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

    private fun requestAccess() {
        when {
            !isCallScreeningRoleHeld() -> requestCallScreeningRole()
            !isNotificationGranted() -> requestNotificationPermission()
            !isBluetoothGranted() -> requestBluetoothPermissions()
            !canScheduleExactAlarms() -> requestExactAlarmAccess()
            !isIgnoringBatteryOptimizations() -> requestBatteryExemption()
        }
    }

    private fun requestCallScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                REQUEST_ROLE_CODE,
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS_CODE,
            )
        }
    }

    private fun requestBluetoothPermissions() {
        requestPermissions(BLUETOOTH_PERMISSIONS.toTypedArray(), REQUEST_BLUETOOTH_CODE)
    }

    private fun canScheduleExactAlarms(): Boolean =
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun requestExactAlarmAccess() {
        startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")),
        )
    }

    private fun promptBatteryExemptionIfNeeded() {
        if (!isReady() || !isBluetoothGranted() || batteryExemptionPrompted || isIgnoringBatteryOptimizations()) return
        batteryExemptionPrompted = true
        requestBatteryExemption()
    }

    private fun requestBatteryExemption() {
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun isIgnoringBatteryOptimizations(): Boolean =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    private companion object {
        const val REQUEST_ROLE_CODE = 1
        const val REQUEST_NOTIFICATIONS_CODE = 2
        const val REQUEST_BLUETOOTH_CODE = 3
        val BLUETOOTH_PERMISSIONS = listOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    }
}
