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

    private val runtimePermissions = arrayOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.POST_NOTIFICATIONS,
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
                textSize = 16f
                setTextColor(Color.BLACK)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
            }
            addView(statusLabel)
            addView(RetroButton(this@MainActivity, getString(R.string.grant_access), getColor(R.color.primary)) {
                requestAccess()
            })
        }
    }

    private fun refreshStatus() {
        val ready = hasRuntimePermissions() && Settings.canDrawOverlays(this)
        statusLabel.text = getString(if (ready) R.string.status_active else R.string.status_inactive)
        if (ready) startMonitorService()
    }

    private fun hasRuntimePermissions(): Boolean = runtimePermissions.all {
        checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAccess() {
        if (!hasRuntimePermissions()) {
            requestPermissions(runtimePermissions, REQUEST_CODE)
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
