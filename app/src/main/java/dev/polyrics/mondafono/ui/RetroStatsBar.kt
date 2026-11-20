package dev.polyrics.mondafono.ui

import android.content.Context
import android.widget.LinearLayout
import dev.polyrics.mondafono.R

class RetroStatsBar(
    context: Context,
    private val statLabels: List<String>,
) : LinearLayout(context) {

    private val bandColors = listOf(
        context.getColor(R.color.secondary),
        context.getColor(R.color.coin),
        context.getColor(R.color.primary),
    )

    private val items = statLabels.mapIndexed { index, label ->
        val bandColor = bandColors.getOrElse(index) { context.getColor(R.color.primary) }
        RetroStatItem(context, bandColor).apply { setStat(0, label) }
    }

    init {
        orientation = HORIZONTAL
        clipChildren = false
        val gap = (8 * resources.displayMetrics.density).toInt()
        items.forEachIndexed { index, item ->
            val params = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            if (index > 0) params.leftMargin = gap
            addView(item, params)
        }
    }

    fun setValues(values: List<Int>) {
        values.forEachIndexed { index, value ->
            items.getOrNull(index)?.setStat(value, statLabels[index])
        }
    }
}
