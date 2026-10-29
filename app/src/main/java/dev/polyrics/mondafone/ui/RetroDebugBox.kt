package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import dev.polyrics.mondafone.R

class RetroDebugBox(
    context: Context,
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density
    private val shadowPx = 4 * density

    private val surfacePaint = Paint().apply { color = context.getColor(R.color.surface) }
    private val shadowPaint = Paint().apply { color = Color.BLACK }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }

    private val linesContainer = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    init {
        orientation = VERTICAL
        setWillNotDraw(false)
        val pad = (12 * density).toInt()
        setPadding(pad, pad, pad + shadowPx.toInt(), pad + shadowPx.toInt())
        addView(titleView(context))
        addView(linesContainer)
    }

    fun setLines(lines: List<String>) {
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
            text = context.getString(R.string.debug_title)
            textSize = 14f
            setTextColor(Color.BLACK)
            setTypeface(typeface, Typeface.BOLD)
        }
}
