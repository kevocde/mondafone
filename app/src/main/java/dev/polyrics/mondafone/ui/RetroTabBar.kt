package dev.polyrics.mondafone.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.view.accessibility.AccessibilityNodeInfo.CollectionInfo
import android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo
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
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityDelegate = object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.collectionInfo = CollectionInfo(
                    1,
                    tabs.size,
                    false,
                    CollectionInfo.SELECTION_MODE_SINGLE,
                )
            }
        }
        buildTabs()
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
            val tabView = TabView(context, index, label, index == selectedIndex) { selectTab(index) }
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            if (index > 0) {
                params.leftMargin = (4 * density).toInt()
            }
            addView(tabView, params)
        }
        refreshTabViews()
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
        private val index: Int,
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
            color = context.getColor(R.color.ink)
            style = Paint.Style.STROKE
            strokeWidth = border
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            textSize = sp(14f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.ink)
            textSize = sp(8f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        private var isTabSelected = selected
        private var badgeCount = 0
        private var pressed = false

        private fun sp(value: Float): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

        init {
            isClickable = true
            isFocusable = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            isSelected = selected
            updateAccessibilityDescription()
            accessibilityDelegate = object : AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.isSelected = isTabSelected
                    info.collectionItemInfo = CollectionItemInfo(
                        0,
                        1,
                        index,
                        1,
                        false,
                        isTabSelected,
                    )
                    if (!isTabSelected && host.isEnabled) {
                        info.addAction(AccessibilityAction.ACTION_CLICK)
                    }
                }
            }
        }

        fun setTabSelected(value: Boolean) {
            isTabSelected = value
            isSelected = value
            invalidate()
        }

        fun setBadgeCount(count: Int) {
            badgeCount = count
            updateAccessibilityDescription()
            invalidate()
        }

        private fun formatBadgeCount(): String =
            if (badgeCount > 99) "99+" else badgeCount.toString()

        private fun updateAccessibilityDescription() {
            contentDescription = if (badgeCount > 0) "$label (${formatBadgeCount()})" else label
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = (textPaint.measureText(label) + 40 * density).toInt()
            val height = (48 * density).toInt()
            setMeasuredDimension(
                resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec),
            )
        }

        override fun onDraw(canvas: Canvas) {
            if (isTabSelected) {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), activeFill)
                canvas.drawLine(0f, 4 * density, width.toFloat(), 4 * density, inkPaint)
                canvas.drawLine(0f, 0f, 0f, height.toFloat(), inkPaint)
                canvas.drawLine(0f, 0f, width.toFloat(), 0f, inkPaint)
                canvas.drawLine(width.toFloat(), 0f, width.toFloat(), height.toFloat(), inkPaint)
                val textY = height / 2f + 2 * density - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(label, width / 2f, textY, textPaint)
            } else {
                val offset = if (pressed) shadow else 0f
                if (!pressed) {
                    val shadowPaint = Paint().apply { color = context.getColor(R.color.ink); style = Paint.Style.FILL }
                    canvas.drawRect(shadow, shadow, width.toFloat(), height.toFloat(), shadowPaint)
                }
                canvas.drawRect(offset, offset, width - shadow + offset, height - shadow + offset, inactiveFill)
                canvas.drawRect(offset, offset, width - shadow + offset, height - shadow + offset, inkPaint)
                val textY = (height - shadow) / 2f + offset - (textPaint.descent() + textPaint.ascent()) / 2
                canvas.drawText(label, (width - shadow) / 2f + offset, textY, textPaint)
            }

            if (badgeCount > 0) {
                drawBadge(canvas)
            }
        }

        private fun drawBadge(canvas: Canvas) {
            val pixelSize = 2 * density
            val rightInset = if (isTabSelected) 3 * density else shadow + 3 * density
            val baseX = width - 6 * pixelSize - rightInset
            val baseY = 3 * density

            val coinPattern = arrayOf(
                intArrayOf(0, 1, 1, 1, 1, 0),
                intArrayOf(1, 1, 2, 1, 1, 1),
                intArrayOf(1, 2, 2, 1, 1, 1),
                intArrayOf(1, 1, 1, 1, 1, 1),
                intArrayOf(1, 1, 1, 1, 1, 1),
                intArrayOf(0, 1, 1, 1, 1, 0)
            )

            val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = context.getColor(R.color.coin_glint)
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

            val text = formatBadgeCount()
            val textX = baseX + 3 * pixelSize
            val textY = baseY + 3 * pixelSize - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2
            canvas.drawText(text, textX, textY, badgeTextPaint)
        }

        override fun performClick(): Boolean {
            super.performClick()
            onTap()
            return true
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressed = true
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    pressed = false
                    invalidate()
                    performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    pressed = false
                    invalidate()
                }
            }
            return true
        }
    }
}
