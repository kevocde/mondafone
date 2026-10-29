package dev.polyrics.mondafone.network

import android.app.Application
import android.os.ParcelUuid
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class BeaconScanFilterTest {

    @Test
    fun buildsSingleServiceDataFilter() {
        val filters = BeaconScanFilter.filters()

        assertEquals(1, filters.size)
        assertEquals(ParcelUuid(BeaconProtocol.SERVICE_UUID), filters[0].serviceDataUuid)
        assertEquals(0, filters[0].serviceData?.size)
    }
}
