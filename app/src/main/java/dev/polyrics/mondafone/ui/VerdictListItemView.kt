package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class VerdictListItemView(
    context: Context,
    number: String,
    actions: List<Action>,
) : LinearLayout(context) {

    data class Action(val label: String, val color: Int, val onClick: () -> Unit)

    private val density = resources.displayMetrics.density
    private val dividerPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2 * density
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        val pad = (16 * density).toInt()
        val vPad = (12 * density).toInt()
        setPadding(pad, vPad, pad, vPad)

        addView(numberLabel(number), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        actions.forEach { action ->
            addView(
                RetroButton(context, action.label, action.color, action.onClick),
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = (8 * density).toInt()
                },
            )
        }
    }

    private fun numberLabel(number: String): TextView =
        TextView(context).apply {
            text = number
            textSize = 16f
            setTextColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
        }

    override fun onDraw(canvas: Canvas) {
        val y = height - dividerPaint.strokeWidth / 2
        canvas.drawLine(0f, y, width.toFloat(), y, dividerPaint)
        super.onDraw(canvas)
    }
}
