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
    private var counts = List(tabs.size) { 0 }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setWillNotDraw(false)
        clipChildren = false
    }

    fun selectTab(index: Int) {
        if (index == selectedIndex || index !in tabs.indices) return
        selectedIndex = index
        refreshTabViews()
        onTabSelected(index)
    }

    fun setCounts(newCounts: List<Int>) {
        if (newCounts.size != tabs.size) return
        counts = newCounts
        refreshTabViews()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (childCount == 0) buildTabs()
    }

    private fun buildTabs() {
        tabs.forEachIndexed { index, label ->
            val tabView = TabView(context, label, index == selectedIndex) { selectTab(index) }
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            if (index > 0) {
                params.leftMargin = (4 * density).toInt()
            }
            addView(tabView, params)
        }
    }

    private fun refreshTabViews() {
        for (i in 0 until childCount) {
            (getChildAt(i) as? TabView)?.apply {
                setTabSelected(i == selectedIndex)
                setBadgeCount(counts.getOrNull(i) ?: 0)
            }
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
        private val badgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.coin) }
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
        private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8 * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        private var isTabSelected = selected
        private var badgeCount = 0

        fun setTabSelected(value: Boolean) {
            isTabSelected = value
            invalidate()
        }

        fun setBadgeCount(count: Int) {
            badgeCount = count
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
            if (isTabSelected) {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), activeFill)
                canvas.drawLine(0f, 0f, 0f, height.toFloat(), inkPaint)
                canvas.drawLine(0f, 0f, width.toFloat(), 0f, inkPaint)
                canvas.drawLine(width.toFloat(), 0f, width.toFloat(), height.toFloat(), inkPaint)
                val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(label, width / 2f, textY, textPaint)
            } else {
                val shadowPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
                canvas.drawRect(shadow, shadow, width.toFloat(), height.toFloat(), shadowPaint)
                canvas.drawRect(0f, 0f, width - shadow, height - shadow, inactiveFill)
                canvas.drawRect(0f, 0f, width - shadow, height - shadow, inkPaint)
                val textY = (height - shadow) / 2f - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(label, (width - shadow) / 2f, textY, textPaint)
            }

            if (badgeCount > 0) {
                drawBadge(canvas)
            }
        }

        private fun drawBadge(canvas: Canvas) {
            val pixelSize = 2 * density
            val offset = 2 * density
            val baseX = width - 6 * pixelSize - offset
            val baseY = -offset

            val coinPattern = arrayOf(
                intArrayOf(0, 1, 1, 1, 1, 0),
                intArrayOf(1, 1, 2, 1, 1, 1),
                intArrayOf(1, 2, 2, 1, 1, 1),
                intArrayOf(1, 1, 1, 1, 1, 1),
                intArrayOf(1, 1, 1, 1, 1, 1),
                intArrayOf(0, 1, 1, 1, 1, 0)
            )

            val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
                color = Color.parseColor("#FFE699")
            }

            for (row in coinPattern.indices) {
                for (col in coinPattern[row].indices) {
                    val value = coinPattern[row][col]
                    if (value > 0) {
                        val x = baseX + col * pixelSize
                        val y = baseY + row * pixelSize
                        val paint = if (value == 2) highlightPaint else badgeFill
                        canvas.drawRect(x, y, x + pixelSize, y + pixelSize, paint)
                    }
                }
            }

            canvas.drawRect(baseX, baseY, baseX + 6 * pixelSize, baseY + 6 * pixelSize, inkPaint.apply { style = Paint.Style.STROKE })

            val text = if (badgeCount > 99) "99+" else badgeCount.toString()
            val textX = baseX + 3 * pixelSize
            val textY = baseY + 3 * pixelSize - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2
            canvas.drawText(text, textX, textY, badgeTextPaint)
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
