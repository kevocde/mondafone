package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import dev.polyrics.mondafone.R

class VerdictListItemView(
    context: Context,
    number: String,
    actions: List<Action>,
    bandColor: Int = context.getColor(R.color.coin),
) : LinearLayout(context) {

    data class Action(val label: String, val color: Int, val onClick: () -> Unit)

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density
    private val stripePx = 6 * density
    private val backgroundPaint = Paint().apply { color = context.getColor(R.color.background) }
    private val stripePaint = Paint().apply { color = bandColor }
    private val borderPaint = Paint().apply {
        color = context.getColor(R.color.ink)
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
        val leftPad = (16 * density).toInt()
        val rightPad = (12 * density).toInt()
        val vPad = (10 * density).toInt()
        setPadding(leftPad, vPad, rightPad, vPad)

        addView(numberLabel(number), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        actions.forEach { action ->
            addView(
                RetroButton(context, action.label, action.color, compact = true, onTap = action.onClick),
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = (8 * density).toInt()
                },
            )
        }
    }

    private fun numberLabel(number: String): TextView =
        TextView(context).apply {
            text = "> $number"
            textSize = 14f
            maxLines = 1
            setTextColor(context.getColor(R.color.ink))
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
        }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)
        canvas.drawRect(0f, 0f, stripePx, h, stripePaint)
        canvas.drawLine(stripePx, 0f, stripePx, h, borderPaint)
        canvas.drawRect(0f, 0f, w, h, borderPaint)
        super.onDraw(canvas)
    }
}
