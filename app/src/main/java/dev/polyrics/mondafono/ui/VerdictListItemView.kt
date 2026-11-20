package dev.polyrics.mondafono.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import dev.polyrics.mondafono.R

class VerdictListItemView(
    context: Context,
    number: String,
    actions: List<Action>,
    bandColor: Int = context.getColor(R.color.coin),
    subtitle: String? = null,
) : LinearLayout(context) {

    data class Action(val label: String, val color: Int, val onClick: () -> Unit)

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density
    private val bandPx = 6 * density
    private val backgroundPaint = Paint().apply { color = context.getColor(R.color.background) }
    private val bandPaint = Paint().apply { color = bandColor }
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
        val hPad = (12 * density).toInt()
        val topPad = (12 * density).toInt()
        val bottomPad = (8 * density).toInt()
        setPadding(hPad, topPad, hPad, bottomPad)

        addView(numberLabel(number, subtitle), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        actions.forEach { action ->
            addView(
                RetroButton(context, action.label, action.color, compact = true, onTap = action.onClick),
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = (8 * density).toInt()
                },
            )
        }
    }

    private fun numberLabel(number: String, subtitle: String?): TextView =
        TextView(context).apply {
            val primaryLine = "> $number"
            text = if (subtitle.isNullOrEmpty()) {
                primaryLine
            } else {
                SpannableStringBuilder(primaryLine).apply {
                    setSpan(TypefaceSpan("monospace"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val start = length
                    append("\n")
                    append(subtitle)
                    setSpan(TypefaceSpan("sans-serif"), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(StyleSpan(Typeface.NORMAL), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(RelativeSizeSpan(0.82f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            textSize = 14f
            maxLines = if (subtitle.isNullOrEmpty()) 1 else 2
            setTextColor(context.getColor(R.color.ink))
            typeface = Typeface.MONOSPACE
            if (subtitle.isNullOrEmpty()) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)
        canvas.drawRect(0f, 0f, w, bandPx, bandPaint)
        canvas.drawLine(0f, bandPx, w, bandPx, borderPaint)
        canvas.drawRect(0f, 0f, w, h, borderPaint)
        super.onDraw(canvas)
    }
}
