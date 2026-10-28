import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val beaconPeriodMs = localProperties.getProperty("mondafone.beacon.period.ms", "300000")
val beaconWindowMs = localProperties.getProperty("mondafone.beacon.window.ms", "10000")

android {
    namespace = "dev.polyrics.mondafone"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.polyrics.mondafone"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("long", "BEACON_PERIOD_MS", "${beaconPeriodMs}L")
        buildConfigField("long", "BEACON_WINDOW_MS", "${beaconWindowMs}L")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            isShrinkResources = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    testImplementation(libs.junit)
}
