package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import dev.polyrics.mondafone.R

class SpamReportView(
    context: Context,
    number: String,
    onVerdict: (Boolean) -> Unit,
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val shadowPaint = Paint().apply { color = Color.BLACK }
    private val panelPaint = Paint().apply { color = context.getColor(R.color.surface) }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setWillNotDraw(false)
        val pad = (24 * density).toInt()
        setPadding(pad, pad, pad, pad)

        addView(label(context.getString(R.string.spam_question), bold = true))
        addView(label("> $number <", mono = true))
        addView(buttonRow(onVerdict))
    }

    private fun label(text: String, bold: Boolean = false, mono: Boolean = false): TextView =
        TextView(context).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            if (mono) typeface = Typeface.MONOSPACE
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buttonRow(onVerdict: (Boolean) -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            val margin = (8 * density).toInt()
            addView(
                RetroButton(context, context.getString(R.string.answer_yes),
                    context.getColor(R.color.secondary)) { onVerdict(true) },
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    .apply { setMargins(margin, margin, margin, margin) },
            )
            addView(
                RetroButton(context, context.getString(R.string.answer_no),
                    context.getColor(R.color.primary)) { onVerdict(false) },
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    .apply { setMargins(margin, margin, margin, margin) },
            )
        }

    override fun onDraw(canvas: Canvas) {
        val shadow = 4 * density
        canvas.drawRect(shadow, shadow, width.toFloat(), height.toFloat(), shadowPaint)
        canvas.drawRect(0f, 0f, width - shadow, height - shadow, panelPaint)
        canvas.drawRect(0f, 0f, width - shadow, height - shadow, borderPaint)
    }
}
