import java.util.Properties
import java.util.UUID

plugins {
    alias(libs.plugins.android.application)
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) localPropertiesFile.inputStream().use { load(it) }
}

val beaconCron = localProperties.getProperty("mondafone.beacon.cron", "*/5 * * * *")
val beaconWindowMs = localProperties.getProperty("mondafone.beacon.window.ms", "10000")
val beaconAppSecret = localProperties.getProperty("mondafone.beacon.app.secret")
    ?: UUID.randomUUID().toString().also { generated ->
        localPropertiesFile.appendText("\nmondafone.beacon.app.secret=$generated\n")
    }

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

        buildConfigField("String", "BEACON_CRON", "\"$beaconCron\"")
        buildConfigField("long", "BEACON_WINDOW_MS", "${beaconWindowMs}L")
        buildConfigField("String", "BEACON_APP_SECRET", "\"$beaconAppSecret\"")
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
