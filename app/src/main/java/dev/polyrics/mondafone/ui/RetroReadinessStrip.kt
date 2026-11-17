package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
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
        private val bandPx = 6 * density

        private val surfacePaint = Paint().apply { color = context.getColor(R.color.surface) }
        private val primaryPaint = Paint().apply { color = context.getColor(R.color.primary) }
        private val coinPaint = Paint().apply { color = context.getColor(R.color.coin) }
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
            textSize = 10 * density
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            textSize = 9 * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private var granted = false

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            isScreenReaderFocusable = true
            isClickable = true
            setOnClickListener { onTap() }
            updateDescription()
        }

        fun setGranted(value: Boolean) {
            granted = value
            updateDescription()
            invalidate()
        }

        private fun updateDescription() {
            contentDescription = "$label: ${if (granted) "OK" else "[ ]"}"
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(
                resolveSize((72 * density).toInt(), widthMeasureSpec),
                resolveSize((42 * density).toInt(), heightMeasureSpec),
            )
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            canvas.drawRect(0f, 0f, w, h, surfacePaint)
            val bandFill = if (granted) primaryPaint else coinPaint
            canvas.drawRect(0f, 0f, w, bandPx, bandFill)
            if (!granted) {
                drawDiagonalHatch(canvas, w, bandPx)
            }
            canvas.drawLine(0f, bandPx, w, bandPx, borderPaint)
            canvas.drawRect(0f, 0f, w, h, borderPaint)

            val code = if (granted) "[OK]" else "[  ]"
            canvas.drawText(code, w / 2f, bandPx + 14 * density, statePaint)
            canvas.drawText(label, w / 2f, h - 6 * density, labelPaint)
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
