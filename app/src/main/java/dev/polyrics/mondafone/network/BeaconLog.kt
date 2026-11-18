package dev.polyrics.mondafone.network

import android.util.Log

object BeaconLog {

    private const val TAG = "MondafoneBeacon"

    fun log(event: String) {
        Log.d(TAG, event)
    }
}
