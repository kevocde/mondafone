package dev.polyrics.mondafono.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import dev.polyrics.mondafono.R

class RetroEmptyStateView(
    context: Context,
    private val message: String,
) : View(context) {

    private val density = resources.displayMetrics.density

    private val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isScreenReaderFocusable = true
        contentDescription = message
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize((260 * density).toInt(), widthMeasureSpec),
            resolveSize((72 * density).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val textY = height / 2f - (messagePaint.descent() + messagePaint.ascent()) / 2f
        canvas.drawText(message, width / 2f, textY, messagePaint)
    }
}
