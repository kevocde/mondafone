package dev.polyrics.mondafono.network

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid

object BeaconScanFilter {

    fun filters(): List<ScanFilter> = listOf(
        ScanFilter.Builder()
            .setServiceData(ParcelUuid(BeaconProtocol.SERVICE_UUID), byteArrayOf())
            .build(),
    )
}
