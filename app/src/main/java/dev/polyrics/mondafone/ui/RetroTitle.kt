package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

class RetroTitle(
    context: Context,
    private val text: String,
) : View(context) {

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isAccessibilityHeading = true
        contentDescription = text
        accessibilityDelegate = object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = TextView::class.java.name
                info.isHeading = true
                info.text = text
            }
        }
    }

    private val density = resources.displayMetrics.density
    private val strokeWidthPx = 4 * density

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36 * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val strokePaint = Paint(fillPaint).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeJoin = Paint.Join.ROUND
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (fillPaint.measureText(text) + strokeWidthPx * 2).toInt()
        val height = (fillPaint.descent() - fillPaint.ascent() + strokeWidthPx * 2).toInt()
        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(height, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val x = strokeWidthPx
        val y = strokeWidthPx - fillPaint.ascent()
        canvas.drawText(text, x, y, strokePaint)
        canvas.drawText(text, x, y, fillPaint)
    }
}
