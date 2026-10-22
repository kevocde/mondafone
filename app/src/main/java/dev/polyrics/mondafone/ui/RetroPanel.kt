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
    private val shadowPx = 4 * density
    private val borderPx = 2 * density

    private val shadowPaint = Paint().apply { color = Color.BLACK }
    private val surfacePaint = Paint().apply { color = context.getColor(R.color.surface) }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }

    init {
        setWillNotDraw(false)
        val innerPad = (shadowPx + 8 * density).toInt()
        setPadding(innerPad, innerPad, innerPad, innerPad)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(shadowPx, shadowPx, width.toFloat(), height.toFloat(), shadowPaint)
        canvas.drawRect(0f, 0f, width - shadowPx, height - shadowPx, surfacePaint)
        canvas.drawRect(0f, 0f, width - shadowPx, height - shadowPx, borderPaint)
        super.onDraw(canvas)
    }
}
