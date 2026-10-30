package dev.polyrics.mondafone.spam

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

class FrameworkRegionProvider(private val context: Context) : RegionProvider {

    override fun regionIso(): String {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        return sequenceOf(
            telephony?.simCountryIso,
            telephony?.networkCountryIso,
            Locale.getDefault().country,
        )
            .filterNotNull()
            .map { it.uppercase(Locale.ROOT) }
            .firstOrNull { it.length == ISO_LENGTH }
            ?: FALLBACK_REGION
    }

    private companion object {
        const val ISO_LENGTH = 2
        const val FALLBACK_REGION = "US"
    }
}
