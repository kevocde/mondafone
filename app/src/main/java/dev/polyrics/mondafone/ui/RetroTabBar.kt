package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import dev.polyrics.mondafone.R

class RetroTabBar(
    context: Context,
    private val tabs: List<String>,
    private val onTabSelected: (Int) -> Unit,
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private var selectedIndex = 0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setWillNotDraw(false)
    }

    fun selectTab(index: Int) {
        if (index == selectedIndex || index !in tabs.indices) return
        selectedIndex = index
        refreshTabViews()
        onTabSelected(index)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (childCount == 0) buildTabs()
    }

    private fun buildTabs() {
        tabs.forEachIndexed { index, label ->
            addView(TabView(context, label, index == selectedIndex) { selectTab(index) })
        }
    }

    private fun refreshTabViews() {
        for (i in 0 until childCount) {
            (getChildAt(i) as? TabView)?.setTabSelected(i == selectedIndex)
        }
    }

    private class TabView(
        context: Context,
        private val label: String,
        selected: Boolean,
        private val onTap: () -> Unit,
    ) : View(context) {

        private val density = resources.displayMetrics.density
        private val border = 2 * density
        private val shadow = 4 * density

        private val activeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.primary) }
        private val inactiveFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.surface) }
        private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = border
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14 * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        private var isTabSelected = selected

        fun setTabSelected(value: Boolean) {
            isTabSelected = value
            invalidate()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = (textPaint.measureText(label) + 32 * density).toInt()
            val height = (40 * density).toInt()
            setMeasuredDimension(
                resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec),
            )
        }

        override fun onDraw(canvas: Canvas) {
            val fill = if (isTabSelected) activeFill else inactiveFill
            canvas.drawRect(
                shadow, shadow, width.toFloat(), height.toFloat(),
                inkPaint.apply { style = Paint.Style.FILL; color = Color.BLACK },
            )
            canvas.drawRect(0f, 0f, width - shadow, height - shadow, fill)
            canvas.drawRect(0f, 0f, width - shadow, height - shadow, inkPaint)
            val textY = (height - shadow) / 2f - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(label, (width - shadow) / 2f, textY, textPaint)
        }

        override fun performClick(): Boolean {
            onTap()
            return super.performClick()
        }

        override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
            when (event.action) {
                android.view.MotionEvent.ACTION_UP -> performClick()
            }
            return true
        }
    }
}
