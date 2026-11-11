package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import dev.polyrics.mondafone.R

class RetroStatItem(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density
    private val shadowPx = 4 * density

    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 22 * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10 * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var value = 0
    private var label = ""

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isScreenReaderFocusable = true
    }

    fun setStat(value: Int, label: String) {
        this.value = value
        this.label = label
        contentDescription = "$label: $value"
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize((96 * density).toInt(), widthMeasureSpec),
            resolveSize((64 * density).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val panelWidth = width - shadowPx
        val panelHeight = height - shadowPx
        canvas.drawRect(shadowPx, shadowPx, width.toFloat(), height.toFloat(), shadowPaint)
        canvas.drawRect(0f, 0f, panelWidth, panelHeight, surfacePaint)
        canvas.drawRect(0f, 0f, panelWidth, panelHeight, borderPaint)
        drawCentered(canvas, value.toString(), valuePaint, panelWidth, panelHeight * VALUE_Y)
        drawCentered(canvas, label, labelPaint, panelWidth, panelHeight * LABEL_Y)
    }

    private fun drawCentered(canvas: Canvas, text: String, paint: Paint, panelWidth: Float, baselineY: Float) {
        val y = baselineY - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, panelWidth / 2f, y, paint)
    }

    private companion object {
        const val VALUE_Y = 0.38f
        const val LABEL_Y = 0.78f
    }
}
