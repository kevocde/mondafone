package dev.polyrics.mondafono.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import dev.polyrics.mondafono.R

class RetroStatItem(
    context: Context,
    bandColor: Int = context.getColor(R.color.primary),
) : View(context) {

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density
    private val shadowPx = 4 * density
    private val bandPx = 6 * density

    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.surface)
    }
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bandColor
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.ink) }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = sp(22f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = sp(10f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

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
            resolveSize((68 * density).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val panelWidth = width - shadowPx
        val panelHeight = height - shadowPx
        canvas.drawRect(shadowPx, shadowPx, width.toFloat(), height.toFloat(), shadowPaint)
        canvas.drawRect(0f, 0f, panelWidth, panelHeight, surfacePaint)
        canvas.drawRect(0f, 0f, panelWidth, bandPx, bandPaint)
        canvas.drawLine(0f, bandPx, panelWidth, bandPx, borderPaint)
        canvas.drawRect(0f, 0f, panelWidth, panelHeight, borderPaint)
        val formattedValue = if (value in 0..9) "0$value" else value.toString()
        drawCentered(canvas, formattedValue, valuePaint, panelWidth, panelHeight * VALUE_Y)
        drawCentered(canvas, label.uppercase(), labelPaint, panelWidth, panelHeight * LABEL_Y)
    }

    private fun drawCentered(canvas: Canvas, text: String, paint: Paint, panelWidth: Float, baselineY: Float) {
        val y = baselineY - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, panelWidth / 2f, y, paint)
    }

    private companion object {
        const val VALUE_Y = 0.46f
        const val LABEL_Y = 0.80f
    }
}
