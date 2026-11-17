package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import dev.polyrics.mondafone.R

class RetroEmptyStateView(
    context: Context,
    private val message: String,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density
    private val bandPx = 10 * density

    private val backgroundPaint = Paint().apply { color = context.getColor(R.color.background) }
    private val surfacePaint = Paint().apply { color = context.getColor(R.color.surface) }
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
    private val emblemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = sp(13f)
        typeface = Typeface.MONOSPACE
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = sp(14f)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isScreenReaderFocusable = true
        contentDescription = message
    }

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize((280 * density).toInt(), widthMeasureSpec),
            resolveSize((108 * density).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val pad = 4 * density
        val left = pad
        val top = pad
        val right = width - pad
        val bottom = height - pad

        canvas.drawRect(left, top, right, bottom, backgroundPaint)
        drawHatchBand(canvas, left, top, right, top + bandPx)
        canvas.drawLine(left, top + bandPx, right, top + bandPx, borderPaint)
        canvas.drawRect(left, top, right, bottom, borderPaint)

        val cx = width / 2f
        val boxW = 64 * density
        val boxH = 24 * density
        val boxTop = top + bandPx + 14 * density
        canvas.drawRect(cx - boxW / 2f, boxTop, cx + boxW / 2f, boxTop + boxH, surfacePaint)
        canvas.drawRect(cx - boxW / 2f, boxTop, cx + boxW / 2f, boxTop + boxH, borderPaint)
        val emblemY = boxTop + boxH / 2f - (emblemPaint.descent() + emblemPaint.ascent()) / 2f
        canvas.drawText("[ -- ]", cx, emblemY, emblemPaint)

        canvas.drawText(message, cx, bottom - 16 * density, messagePaint)
    }

    private fun drawHatchBand(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val step = 8 * density
        val h = bottom - top
        var x = left
        while (x < right - h) {
            canvas.drawLine(x, bottom, x + h, top, hatchPaint)
            x += step
        }
    }
}
