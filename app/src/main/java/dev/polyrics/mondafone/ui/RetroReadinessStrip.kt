package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.Button
import android.widget.LinearLayout
import dev.polyrics.mondafone.R

class RetroReadinessStrip(
    context: Context,
    private val stageLabels: List<String>,
    private val onStageTap: () -> Unit = {},
) : LinearLayout(context) {

    private val cells = stageLabels.map { label ->
        StageCellView(context, label, onStageTap)
    }

    init {
        orientation = HORIZONTAL
        clipChildren = false
        val gap = (6 * resources.displayMetrics.density).toInt()
        cells.forEachIndexed { index, cell ->
            val params = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            if (index > 0) params.leftMargin = gap
            addView(cell, params)
        }
    }

    fun setStages(states: List<Boolean>) {
        states.forEachIndexed { index, granted ->
            cells.getOrNull(index)?.setGranted(granted)
        }
    }

    private class StageCellView(
        context: Context,
        private val label: String,
        private val onTap: () -> Unit,
    ) : View(context) {

        private val density = resources.displayMetrics.density
        private val borderPx = 1.5f * density

        private val grantedFillPaint = Paint().apply { color = context.getColor(R.color.surface) }
        private val pendingFillPaint = Paint().apply { color = context.getColor(R.color.coin_glint) }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            style = Paint.Style.STROKE
            strokeWidth = borderPx
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9.5f, resources.displayMetrics)
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        private var granted = false

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            isScreenReaderFocusable = true
            isClickable = true
            isFocusable = true
            updateDescription()
            accessibilityDelegate = object : AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = Button::class.java.name
                    info.text = contentDescription
                    if (host.isEnabled) {
                        info.addAction(AccessibilityAction.ACTION_CLICK)
                    }
                }
            }
        }

        fun setGranted(value: Boolean) {
            granted = value
            updateDescription()
            invalidate()
        }

        private fun updateDescription() {
            contentDescription = "$label: ${if (granted) "OK" else "[ ]"}"
        }

        override fun performClick(): Boolean {
            super.performClick()
            onTap()
            return true
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                performClick()
            }
            return true
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(
                resolveSize((72 * density).toInt(), widthMeasureSpec),
                resolveSize((32 * density).toInt(), heightMeasureSpec),
            )
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val fill = if (granted) grantedFillPaint else pendingFillPaint
            canvas.drawRect(0f, 0f, w, h, fill)
            canvas.drawRect(0f, 0f, w, h, borderPaint)

            val text = "${if (granted) "[OK]" else "[ ]"} $label"
            val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(text, w / 2f, textY, textPaint)
        }
    }
}
