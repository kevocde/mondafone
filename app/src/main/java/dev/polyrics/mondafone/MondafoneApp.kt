package dev.polyrics.mondafone

import android.app.Application
import dev.polyrics.mondafone.identity.KeystoreDeviceIdentity

class MondafoneApp : Application() {

    override fun onCreate() {
        super.onCreate()
        runCatching { KeystoreDeviceIdentity().ensureCreated() }
    }
}
