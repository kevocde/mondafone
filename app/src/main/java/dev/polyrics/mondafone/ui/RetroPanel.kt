package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.FrameLayout
import dev.polyrics.mondafone.R

class RetroPanel(
    context: Context,
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density

    private val surfacePaint = Paint().apply { color = context.getColor(R.color.surface) }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }

    init {
        setWillNotDraw(false)
        val pad = (12 * density).toInt()
        setPadding(pad, pad, pad, pad)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), surfacePaint)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
        super.onDraw(canvas)
    }
}
