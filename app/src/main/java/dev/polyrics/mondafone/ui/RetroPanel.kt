package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
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
        color = context.getColor(R.color.ink)
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }

    init {
        setWillNotDraw(false)
        val pad = (12 * density).toInt()
        setPadding(pad, pad, pad, pad)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, surfacePaint)
        canvas.drawRect(0f, 0f, w, h, borderPaint)
        drawCornerTicks(canvas, w, h)
        super.onDraw(canvas)
    }

    private fun drawCornerTicks(canvas: Canvas, w: Float, h: Float) {
        val inset = 5 * density
        val len = 6 * density
        canvas.drawLine(inset, inset, inset + len, inset, borderPaint)
        canvas.drawLine(inset, inset, inset, inset + len, borderPaint)
        canvas.drawLine(w - inset - len, inset, w - inset, inset, borderPaint)
        canvas.drawLine(w - inset, inset, w - inset, inset + len, borderPaint)
        canvas.drawLine(inset, h - inset, inset + len, h - inset, borderPaint)
        canvas.drawLine(inset, h - inset - len, inset, h - inset, borderPaint)
        canvas.drawLine(w - inset - len, h - inset, w - inset, h - inset, borderPaint)
        canvas.drawLine(w - inset, h - inset - len, w - inset, h - inset, borderPaint)
    }
}
