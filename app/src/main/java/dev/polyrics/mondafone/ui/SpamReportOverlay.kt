package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager

class SpamReportOverlay(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)

    var isShowing = false
        private set
    private var currentView: SpamReportView? = null

    fun show(number: String, onVerdict: (Boolean) -> Unit) {
        if (isShowing) return
        val view = SpamReportView(appContext, number) { isSpam ->
            onVerdict(isSpam)
            dismiss()
        }
        windowManager.addView(view, layoutParams())
        currentView = view
        isShowing = true
    }

    private fun dismiss() {
        currentView?.let { windowManager.removeView(it) }
        currentView = null
        isShowing = false
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    )
}
