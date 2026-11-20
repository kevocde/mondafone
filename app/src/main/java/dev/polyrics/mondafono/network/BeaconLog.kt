package dev.polyrics.mondafono.network

import android.util.Log

object BeaconLog {

    private const val TAG = "MondafonoBeacon"

    fun log(event: String) {
        Log.d(TAG, event)
    }
}
