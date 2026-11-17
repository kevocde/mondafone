package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.widget.Button
import dev.polyrics.mondafone.R

class RetroButton(
    context: Context,
    private val label: String,
    fillColor: Int,
    private val compact: Boolean = false,
    private val onTap: () -> Unit,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val border = 2 * density
    private val shadow = if (compact) 3 * density else 4 * density
    private val textSizeSp = if (compact) 12f else 16f
    private val touchHeightDp = 48f
    private val visualHeightDp = if (compact) 36f else 48f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor }
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.ink) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSp, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val body = RectF()
    private var pressed = false

    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = label
        accessibilityDelegate = object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Button::class.java.name
                info.text = label
                if (host.isEnabled) {
                    info.addAction(AccessibilityAction.ACTION_CLICK)
                }
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        onTap()
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minW = (48 * density).toInt()
        val width = maxOf(minW, (textPaint.measureText(label) + 24 * density).toInt())
        val height = (touchHeightDp * density).toInt()
        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(height, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val offset = if (pressed) shadow else 0f
        val drawnHeight = minOf(height.toFloat(), visualHeightDp * density)
        val vInset = (height - drawnHeight) / 2f
        body.set(0f, vInset, width - shadow, vInset + drawnHeight - shadow)
        body.offset(offset, offset)
        if (!pressed) {
            canvas.drawRect(body.left + shadow, body.top + shadow, body.right + shadow, body.bottom + shadow, inkPaint)
        }
        canvas.drawRect(body, fillPaint)
        canvas.drawRect(body, inkPaint.apply { style = Paint.Style.STROKE; strokeWidth = border })
        inkPaint.style = Paint.Style.FILL
        val textY = body.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(label, body.centerX(), textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> setPressedState(true)
            MotionEvent.ACTION_UP -> {
                setPressedState(false)
                if (event.x in 0f..width.toFloat() && event.y in 0f..height.toFloat()) performClick()
            }
            MotionEvent.ACTION_CANCEL -> setPressedState(false)
        }
        return true
    }

    private fun setPressedState(value: Boolean) {
        pressed = value
        invalidate()
    }
}
