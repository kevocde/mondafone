package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import dev.polyrics.mondafone.R

class RetroDebugBox(
    context: Context,
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density
    private val shadowPx = 4 * density
    private val titleText = context.getString(R.string.debug_title)
    private var currentLines: List<String> = emptyList()

    private val surfacePaint = Paint().apply { color = context.getColor(R.color.surface) }
    private val shadowPaint = Paint().apply { color = Color.BLACK }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }

    private val linesContainer = LinearLayout(context).apply {
        orientation = VERTICAL
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    init {
        orientation = VERTICAL
        setWillNotDraw(false)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        isScreenReaderFocusable = true
        val pad = (12 * density).toInt()
        setPadding(pad, pad, pad + shadowPx.toInt(), pad + shadowPx.toInt())
        addView(titleView(context))
        addView(linesContainer)
        contentDescription = buildAccessibilitySummary()
        accessibilityDelegate = object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription = buildAccessibilitySummary()
            }
        }
    }

    fun setLines(lines: List<String>) {
        currentLines = lines
        contentDescription = buildAccessibilitySummary()
        linesContainer.removeAllViews()
        lines.forEach { line ->
            linesContainer.addView(
                TextView(context).apply {
                    text = line
                    textSize = 12f
                    setTextColor(Color.BLACK)
                    typeface = Typeface.MONOSPACE
                },
            )
        }
    }

    private fun buildAccessibilitySummary(): String =
        if (currentLines.isEmpty()) titleText else "$titleText\n${currentLines.joinToString("\n")}"

    override fun onDraw(canvas: Canvas) {
        val surfaceWidth = width - shadowPx
        val surfaceHeight = height - shadowPx
        canvas.drawRect(shadowPx, shadowPx, width.toFloat(), height.toFloat(), shadowPaint)
        canvas.drawRect(0f, 0f, surfaceWidth, surfaceHeight, surfacePaint)
        canvas.drawRect(0f, 0f, surfaceWidth, surfaceHeight, borderPaint)
        super.onDraw(canvas)
    }

    private fun titleView(context: Context): TextView =
        TextView(context).apply {
            text = titleText
            textSize = 14f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
}
