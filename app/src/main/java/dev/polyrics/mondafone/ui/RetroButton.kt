package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class RetroButton(
    context: Context,
    private val label: String,
    fillColor: Int,
    private val compact: Boolean = false,
    private val onTap: () -> Unit,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val border = 2 * density
    private val shadow = if (compact) 3 * density else 4 * density
    private val textSizeDp = if (compact) 12f else 16f
    private val heightDp = if (compact) 32f else 48f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = textSizeDp * density
        textAlign = Paint.Align.CENTER
    }

    private val body = RectF()
    private var pressed = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (textPaint.measureText(label) + 24 * density).toInt()
        val height = (heightDp * density).toInt()
        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(height, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val offset = if (pressed) shadow else 0f
        body.set(0f, 0f, width - shadow, height - shadow)
        body.offset(offset, offset)
        if (!pressed) {
            canvas.drawRect(body.left + shadow, body.top + shadow, body.right + shadow, body.bottom + shadow, inkPaint)
        }
        canvas.drawRect(body, fillPaint)
        canvas.drawRect(body, inkPaint.apply { style = Paint.Style.STROKE; strokeWidth = border })
        inkPaint.style = Paint.Style.FILL
        val textY = body.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(label, body.centerX(), textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> setPressedState(true)
            MotionEvent.ACTION_UP -> {
                setPressedState(false)
                if (body.contains(event.x, event.y)) onTap()
            }
            MotionEvent.ACTION_CANCEL -> setPressedState(false)
        }
        return true
    }

    private fun setPressedState(value: Boolean) {
        pressed = value
        invalidate()
    }
}
