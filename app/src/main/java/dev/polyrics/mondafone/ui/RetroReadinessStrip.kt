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
        private val borderPx = 2 * density
        private val shadowPx = 3 * density
        private val bandPx = 6 * density

        private val surfacePaint = Paint().apply { color = context.getColor(R.color.surface) }
        private val primaryPaint = Paint().apply { color = context.getColor(R.color.primary) }
        private val coinPaint = Paint().apply { color = context.getColor(R.color.coin) }
        private val shadowPaint = Paint().apply { color = context.getColor(R.color.ink) }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            style = Paint.Style.STROKE
            strokeWidth = borderPx
        }
        private val hatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            style = Paint.Style.STROKE
            strokeWidth = density
        }
        private val statePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            textSize = sp(10f)
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            textSize = sp(9f)
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private var granted = false
        private var pressed = false

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

        private fun sp(value: Float): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

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
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressed = true
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    pressed = false
                    invalidate()
                    performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    pressed = false
                    invalidate()
                }
            }
            return true
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(
                resolveSize((72 * density).toInt(), widthMeasureSpec),
                resolveSize((48 * density).toInt(), heightMeasureSpec),
            )
        }

        override fun onDraw(canvas: Canvas) {
            val offset = if (pressed) shadowPx else 0f
            val bodyW = width - shadowPx
            val bodyH = height - shadowPx
            if (!pressed) {
                canvas.drawRect(shadowPx, shadowPx, width.toFloat(), height.toFloat(), shadowPaint)
            }
            canvas.save()
            canvas.translate(offset, offset)
            canvas.drawRect(0f, 0f, bodyW, bodyH, surfacePaint)
            val bandFill = if (granted) primaryPaint else coinPaint
            canvas.drawRect(0f, 0f, bodyW, bandPx, bandFill)
            if (!granted) {
                drawDiagonalHatch(canvas, bodyW, bandPx)
            }
            canvas.drawLine(0f, bandPx, bodyW, bandPx, borderPaint)
            canvas.drawRect(0f, 0f, bodyW, bodyH, borderPaint)

            val code = if (granted) "[OK]" else "[  ]"
            canvas.drawText(code, bodyW / 2f, bandPx + 15 * density, statePaint)
            canvas.drawText(label, bodyW / 2f, bodyH - 7 * density, labelPaint)
            canvas.restore()
        }

        private fun drawDiagonalHatch(canvas: Canvas, w: Float, h: Float) {
            val step = 6 * density
            var x = -h
            while (x < w) {
                canvas.drawLine(x.coerceAtLeast(0f), h, (x + h).coerceAtMost(w), 0f, hatchPaint)
                x += step
            }
        }
    }
}
