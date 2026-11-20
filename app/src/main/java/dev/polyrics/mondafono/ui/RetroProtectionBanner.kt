package dev.polyrics.mondafono.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import dev.polyrics.mondafono.R

class RetroProtectionBanner(
    context: Context,
    active: Boolean = true,
) : View(context) {

    private val density = resources.displayMetrics.density
    private val borderPx = 2 * density
    private val shadowPx = 4 * density
    private val headerHeightPx = 30 * density
    internal val emblemBitmap: Bitmap? =
        BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground)
    private val emblemPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    private val surfacePaint = Paint().apply { color = context.getColor(R.color.surface) }
    private val backgroundPaint = Paint().apply { color = context.getColor(R.color.background) }
    private val primaryPaint = Paint().apply { color = context.getColor(R.color.primary) }
    private val coinPaint = Paint().apply { color = context.getColor(R.color.coin) }
    private val shadowPaint = Paint().apply { color = context.getColor(R.color.ink) }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        style = Paint.Style.STROKE
        strokeWidth = borderPx
    }
    private val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = sp(13f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = sp(10f)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = sp(15f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textSize = sp(12f)
        typeface = Typeface.DEFAULT
    }

    private var isGuardActive = active
    private var readyStages = if (active) 4 else 0
    private var totalStages = 4
    private var peerCount = 0

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isScreenReaderFocusable = true
        updateAccessibility()
    }

    fun setActive(active: Boolean) {
        setStatus(
            active = active,
            readyStages = if (active) totalStages else 0,
            totalStages = totalStages,
            peerCount = peerCount,
        )
    }

    fun setStatus(
        active: Boolean,
        readyStages: Int = this.readyStages,
        totalStages: Int = this.totalStages,
        peerCount: Int = this.peerCount,
    ) {
        this.isGuardActive = active
        this.readyStages = readyStages
        this.totalStages = totalStages
        this.peerCount = peerCount
        updateAccessibility()
        invalidate()
    }

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private fun stampText(): String =
        context.getString(if (isGuardActive) R.string.status_guard_active else R.string.status_setup_required)

    private fun detailText(): String =
        if (isGuardActive) {
            context.getString(R.string.status_protection_metrics, readyStages, totalStages, peerCount)
        } else {
            context.getString(R.string.status_onboarding_detail)
        }

    private fun updateAccessibility() {
        val subtitle = context.getString(R.string.app_subtitle)
        contentDescription = "${stampText()} - $subtitle. ${detailText()}"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (94 * density).toInt()
        setMeasuredDimension(
            resolveSize((320 * density).toInt(), widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val cardW = width - shadowPx
        val cardH = height - shadowPx
        canvas.drawRect(shadowPx, shadowPx, width.toFloat(), height.toFloat(), shadowPaint)
        canvas.drawRect(0f, 0f, cardW, cardH, surfacePaint)
        drawHeaderBar(canvas, cardW)
        drawBody(canvas, cardW, cardH)
        canvas.drawRect(0f, 0f, cardW, cardH, borderPaint)
    }

    private fun drawHeaderBar(canvas: Canvas, cardW: Float) {
        canvas.drawRect(0f, 0f, cardW, headerHeightPx, backgroundPaint)
        canvas.drawLine(0f, headerHeightPx, cardW, headerHeightPx, borderPaint)

        val pad = 10 * density
        val brandY = headerHeightPx / 2f - (brandPaint.descent() + brandPaint.ascent()) / 2f
        canvas.drawText(context.getString(R.string.app_name).uppercase(), pad, brandY, brandPaint)

        val label = stampText()
        val pillW = stampPaint.measureText(label) + 14 * density
        val pillH = 20 * density
        val pillLeft = cardW - pad - pillW
        val pillTop = (headerHeightPx - pillH) / 2f
        val fill = if (isGuardActive) primaryPaint else coinPaint
        canvas.drawRect(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH, fill)
        canvas.drawRect(pillLeft, pillTop, pillLeft + pillW, pillTop + pillH, borderPaint)
        val textY = pillTop + pillH / 2f - (stampPaint.descent() + stampPaint.ascent()) / 2f
        canvas.drawText(label, pillLeft + pillW / 2f, textY, stampPaint)
    }

    private fun drawBody(canvas: Canvas, cardW: Float, cardH: Float) {
        val pad = 10 * density
        val boxSize = 40 * density
        val boxTop = headerHeightPx + (cardH - headerHeightPx - boxSize) / 2f
        val fill = if (isGuardActive) primaryPaint else coinPaint
        canvas.drawRect(pad, boxTop, pad + boxSize, boxTop + boxSize, fill)
        drawAppIcon(canvas, pad + borderPx, boxTop + borderPx, boxSize - 2 * borderPx)
        canvas.drawRect(pad, boxTop, pad + boxSize, boxTop + boxSize, borderPaint)

        val textX = pad + boxSize + 10 * density
        val centerY = headerHeightPx + (cardH - headerHeightPx) / 2f
        val subtitle = context.getString(R.string.app_subtitle)
        canvas.drawText(subtitle, textX, centerY - 3 * density, headlinePaint)
        canvas.drawText(detailText(), textX, centerY + 13 * density, detailPaint)
    }

    private fun drawAppIcon(canvas: Canvas, left: Float, top: Float, size: Float) {
        val bitmap = emblemBitmap ?: return
        val drawn = size * 2f
        val dst = RectF(
            left + (size - drawn) / 2f,
            top + (size - drawn) / 2f,
            left + (size + drawn) / 2f,
            top + (size + drawn) / 2f,
        )
        canvas.save()
        canvas.clipRect(left, top, left + size, top + size)
        canvas.drawBitmap(bitmap, null, dst, emblemPaint)
        canvas.restore()
    }
}
