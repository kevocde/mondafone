package dev.polyrics.mondafone

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.polyrics.mondafone.spam.PhoneNumberDisplay
import dev.polyrics.mondafone.ui.RetroButton
import dev.polyrics.mondafone.ui.RetroDebugBox
import dev.polyrics.mondafone.ui.RetroPanel
import dev.polyrics.mondafone.ui.RetroTabBar
import dev.polyrics.mondafone.ui.RetroTitle
import dev.polyrics.mondafone.ui.VerdictListItemView

class MainActivity : Activity() {

    private val store by lazy { (application as MondafoneApp).verdictStore }
    private val reports by lazy { (application as MondafoneApp).spamReportStore }
    private val identity by lazy { (application as MondafoneApp).deviceIdentity }
    private val beaconLog by lazy { (application as MondafoneApp).beaconEventLog }
    private lateinit var listContainer: LinearLayout
    private lateinit var tabBar: RetroTabBar
    private var debugBox: RetroDebugBox? = null
    private var currentTab = 0
    private var batteryExemptionPrompted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        store.registerOnChangeListener { runOnUiThread { renderList() } }
        reports.registerOnChangeListener { runOnUiThread { renderList() } }
        beaconLog.setListener { events -> runOnUiThread { renderDebug(events) } }
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
                if (isBluetoothGranted()) (application as MondafoneApp).scheduleBeacon()
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
        val density = resources.displayMetrics.density
        val pad = (24 * density).toInt()
        val gap = (16 * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(getColor(R.color.background))
            clipChildren = false
            addView(RetroTitle(this@MainActivity, getString(R.string.app_name)))
            addView(textLabel(getString(R.string.app_subtitle), 18f, bold = true), marginParams(bottom = gap * 2))
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
            debugBox = RetroDebugBox(this@MainActivity)
            addView(
                debugBox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = gap },
            )
            renderDebug(beaconLog.lines())
        }
    }

    private fun textLabel(text: String, sizeSp: Float, bold: Boolean = false, mono: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(Color.BLACK)
            if (mono) typeface = Typeface.MONOSPACE
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

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

    private fun buildOnboardingContent(): LinearLayout {
        debugBox = null
        val density = resources.displayMetrics.density
        val pad = (24 * density).toInt()
        val gap = (16 * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(getColor(R.color.background))
            addView(RetroTitle(this@MainActivity, getString(R.string.app_name)))
            addView(textLabel(getString(R.string.app_subtitle), 18f, bold = true), marginParams(bottom = gap * 2))
            addView(
                RetroButton(this@MainActivity, getString(R.string.grant_access), getColor(R.color.primary)) {
                    requestAccess()
                },
                marginParams(bottom = 0),
            )
        }
    }

    private fun renderList() {
        if (!isReady()) {
            setContentView(buildOnboardingContent())
            return
        }
        tabBar.setCounts(
            listOf(
                store.pendingNumbers().size,
                store.whitelistNumbers().size,
                reports.ownReports().size,
            ),
        )
        listContainer.removeAllViews()
        when (currentTab) {
            0 -> renderPendingItems()
            1 -> renderWhitelistItems()
            2 -> renderBlacklistItems()
        }
    }

    private fun renderPendingItems() {
        val numbers = store.pendingNumbers()
        if (numbers.isEmpty()) {
            showEmptyMessage(getString(R.string.empty_pending))
            return
        }
        numbers.forEach { number ->
            val display = PhoneNumberDisplay.format(number)
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
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() },
            )
        }
    }

    private fun renderWhitelistItems() {
        val numbers = store.whitelistNumbers()
        if (numbers.isEmpty()) {
            showEmptyMessage(getString(R.string.empty_whitelist))
            return
        }
        numbers.forEach { number ->
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
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() },
            )
        }
    }

    private fun renderBlacklistItems() {
        val ownReports = reports.ownReports()
        if (ownReports.isEmpty()) {
            showEmptyMessage(getString(R.string.empty_blacklist))
            return
        }
        ownReports.forEach { report ->
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
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() },
            )
        }
    }

    private fun showEmptyMessage(message: String) {
        val pad = (32 * resources.displayMetrics.density).toInt()
        listContainer.addView(
            textLabel(message, 14f, mono = true).apply {
                setPadding(pad, pad, pad, pad)
                gravity = android.view.Gravity.CENTER
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun renderDebug(events: List<String>) {
        val deviceId = getString(R.string.debug_device_id, identity.publicKeyFingerprint() ?: "-")
        val power = getString(
            if (isIgnoringBatteryOptimizations()) {
                R.string.debug_power_unrestricted
            } else {
                R.string.debug_power_restricted
            },
        )
        debugBox?.setLines(listOf(deviceId, power) + events)
    }

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
        if (!isReady() || batteryExemptionPrompted || isIgnoringBatteryOptimizations()) return
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
