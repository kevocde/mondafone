package dev.polyrics.mondafone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import dev.polyrics.mondafone.monitor.CallMonitorService
import dev.polyrics.mondafone.ui.RetroButton

class MainActivity : Activity() {

    private val runtimePermissions = linkedMapOf(
        "Call log" to Manifest.permission.READ_CALL_LOG,
        "Phone state" to Manifest.permission.READ_PHONE_STATE,
        "Contacts" to Manifest.permission.READ_CONTACTS,
        "Notifications" to Manifest.permission.POST_NOTIFICATIONS,
    )

    private lateinit var statusLabel: TextView

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

    private fun buildContent(): LinearLayout {
        val density = resources.displayMetrics.density
        val pad = (24 * density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(getColor(R.color.background))
            statusLabel = TextView(this@MainActivity).apply {
                textSize = 14f
                setTextColor(Color.BLACK)
                typeface = Typeface.MONOSPACE
            }
            addView(statusLabel)
            addView(RetroButton(this@MainActivity, getString(R.string.grant_access), getColor(R.color.primary)) {
                requestAccess()
            })
        }
    }

    private fun refreshStatus() {
        statusLabel.text = buildReport()
        if (isReady()) startMonitorService()
    }

    private fun buildReport(): String = buildString {
        runtimePermissions.forEach { (label, permission) ->
            appendLine(checkLine(label, isGranted(permission)))
        }
        appendLine(checkLine("Overlay window", Settings.canDrawOverlays(this@MainActivity)))
        append(checkLine("Monitor running", CallMonitorService.isRunning))
    }

    private fun checkLine(label: String, ok: Boolean): String =
        (if (ok) "[ OK ] " else "[ .. ] ") + label

    private fun isGranted(permission: String): Boolean =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun isReady(): Boolean =
        runtimePermissions.values.all(::isGranted) && Settings.canDrawOverlays(this)

    private fun requestAccess() {
        if (!runtimePermissions.values.all(::isGranted)) {
            requestPermissions(runtimePermissions.values.toTypedArray(), REQUEST_CODE)
        } else if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }

    private fun startMonitorService() {
        startForegroundService(Intent(this, CallMonitorService::class.java))
    }

    private companion object {
        const val REQUEST_CODE = 1
    }
}
