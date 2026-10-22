package dev.polyrics.mondafone

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import dev.polyrics.mondafone.spam.PrefsSpamStore
import dev.polyrics.mondafone.ui.RetroButton
import dev.polyrics.mondafone.ui.RetroTitle
import dev.polyrics.mondafone.ui.SpamReportView

class MainActivity : Activity() {

    private val store by lazy { PrefsSpamStore(this) }
    private lateinit var statusLabel: TextView
    private lateinit var debugLabel: TextView
    private lateinit var pendingContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ROLE_CODE && isCallScreeningRoleHeld()) {
            requestNotificationPermission()
        } else {
            refreshStatus()
        }
    }

    private fun buildContent(): LinearLayout {
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
            statusLabel = textLabel("", 14f, mono = true)
            addView(statusLabel, marginParams(bottom = gap * 2))
            addView(
                RetroButton(this@MainActivity, getString(R.string.grant_access), getColor(R.color.primary)) {
                    requestAccess()
                },
                marginParams(bottom = gap * 2),
            )
            pendingContainer = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(pendingContainer, marginParams(bottom = gap * 2))
            addView(textLabel(getString(R.string.debug_title), 16f, bold = true), marginParams(bottom = gap / 2))
            debugLabel = textLabel("", 14f, mono = true)
            addView(debugLabel, marginParams(bottom = gap))
            addView(
                RetroButton(this@MainActivity, getString(R.string.clear_store), getColor(R.color.secondary)) {
                    clearSpamStore()
                },
                marginParams(bottom = 0),
            )
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

    private fun refreshStatus() {
        statusLabel.text = buildReport()
        debugLabel.text = buildDebugReport()
        renderPendingVerdict()
    }

    private fun renderPendingVerdict() {
        pendingContainer.removeAllViews()
        val pending = store.pendingNumbers().firstOrNull() ?: return
        pendingContainer.addView(
            SpamReportView(this, pending) { isSpam ->
                store.mark(pending, isSpam)
                renderPendingVerdict()
            },
        )
    }

    private fun buildDebugReport(): String {
        val stats = store.stats()
        val pendingCount = store.pendingNumbers().size
        return buildString {
            appendLine("* Pending = $pendingCount")
            appendLine("* Total marked = ${stats.total}")
            appendLine("* Spam = ${stats.spam}")
            append("* Not spam = ${stats.notSpam}")
        }
    }

    private fun clearSpamStore() {
        store.clear()
        refreshStatus()
    }

    private fun buildReport(): String = buildString {
        appendLine(checkLine("Call screening role", isCallScreeningRoleHeld()))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            append(checkLine("Notifications", isGranted(Manifest.permission.POST_NOTIFICATIONS)))
        }
    }

    private fun checkLine(label: String, ok: Boolean): String =
        (if (ok) "[ OK ] " else "[ .. ] ") + label

    private fun isGranted(permission: String): Boolean =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun isReady(): Boolean {
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        return isCallScreeningRoleHeld() && notificationsGranted
    }

    private fun requestAccess() {
        if (!isCallScreeningRoleHeld()) {
            requestCallScreeningRole()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !isGranted(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            requestNotificationPermission()
        }
    }

    private fun isCallScreeningRoleHeld(): Boolean =
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

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

    private companion object {
        const val REQUEST_ROLE_CODE = 1
        const val REQUEST_NOTIFICATIONS_CODE = 2
    }
}
