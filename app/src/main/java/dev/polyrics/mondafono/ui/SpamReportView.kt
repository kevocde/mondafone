package dev.polyrics.mondafono.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import dev.polyrics.mondafono.R

class SpamReportView(
    context: Context,
    number: String,
    onVerdict: (Boolean) -> Unit,
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val shadowPaint = Paint().apply { color = context.getColor(R.color.ink) }
    private val panelPaint = Paint().apply { color = context.getColor(R.color.surface) }
    private val bandPaint = Paint().apply { color = context.getColor(R.color.coin) }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
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
            setTextColor(context.getColor(R.color.ink))
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
        val bandH = 8 * density
        val cardW = width - shadow
        val cardH = height - shadow
        canvas.drawRect(shadow, shadow, width.toFloat(), height.toFloat(), shadowPaint)
        canvas.drawRect(0f, 0f, cardW, cardH, panelPaint)
        canvas.drawRect(0f, 0f, cardW, bandH, bandPaint)
        canvas.drawLine(0f, bandH, cardW, bandH, borderPaint)
        canvas.drawRect(0f, 0f, cardW, cardH, borderPaint)
    }
}
