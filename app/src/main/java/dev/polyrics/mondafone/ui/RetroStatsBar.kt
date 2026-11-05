package dev.polyrics.mondafone.ui

import android.content.Context
import android.widget.LinearLayout

class RetroStatsBar(
    context: Context,
    private val statLabels: List<String>,
) : LinearLayout(context) {

    private val items = statLabels.map { label ->
        RetroStatItem(context).apply { setStat(0, label) }
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
