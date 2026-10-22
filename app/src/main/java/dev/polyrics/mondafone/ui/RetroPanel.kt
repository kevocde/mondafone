package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
    private val borderPath = Path()

    init {
        setWillNotDraw(false)
        val pad = (8 * density).toInt()
        setPadding(pad, 0, pad, pad)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        borderPath.reset()
        borderPath.moveTo(0f, 0f)
        borderPath.lineTo(0f, h.toFloat())
        borderPath.lineTo(w.toFloat(), h.toFloat())
        borderPath.lineTo(w.toFloat(), 0f)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), surfacePaint)
        canvas.drawPath(borderPath, borderPaint)
        super.onDraw(canvas)
    }
}
