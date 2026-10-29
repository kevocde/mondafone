# Mondafone Agent Guide

## Project Scope

Mondafone is a minimal Android call anti-spam app written in Kotlin. It is intentionally limited and always use Android framework APIs over other Libs:

- `CallScreeningService` for rejecting and filtering incoming calls before they ring.
- Native Android `Activity`, `Canvas`, and `View` for the UI.

The application keeps a minimal, low-footprint UI. Do not add Compose, AndroidX, Material, AppCompat, Room, WorkManager, or coroutine dependencies unless explicitly requested.

## Repository Layout

- `app/src/main/java/dev/polyrics/mondafone/MainActivity.kt`: chained onboarding (ROLE_CALL_SCREENING → POST_NOTIFICATIONS → Bluetooth → exact alarm access), status display, in-app pending verdict review, and debug box.
- `app/src/main/java/dev/polyrics/mondafone/spam/`: verdict store (`CallVerdictStore`/`SqliteCallVerdictStore` on native SQLite via `VerdictDbHelper`, with `PrefsVerdictImporter` for the one-time legacy prefs import), `TrustCallScreeningService` (on-demand call screening), `SpamReportDecider` rules, `SpamBlockedNotifier` (blocked call notifications), `SpamVerdictPromptNotifier` (interactive verdict prompt notifications), and `SpamVerdictReceiver` (handles verdict button actions).
- `app/src/main/java/dev/polyrics/mondafone/identity/`: ECC device identity generated once in Android Keystore (`DeviceIdentity`/`KeystoreDeviceIdentity`) with Base64URL public-key encoding.
- `app/src/main/java/dev/polyrics/mondafone/network/`: BLE beacon discovery driven by `AlarmManager` (`BeaconAlarmScheduler`), where `BleBeaconService` is an ephemeral foreground service that only runs during each beacon window and stops itself afterwards, with pure helpers (`BeaconPayloadCodec`, `BeaconCycleCounter`, `BeaconEventLog`, `RotatingBeaconId`, `BeaconScanFilter`). `BootReceiver` and `ExactAlarmStateReceiver` re-arm the alarm chain. The advertised ID is a time-rotating identifier; the device fingerprint is never emitted in clear.
- `app/src/main/java/dev/polyrics/mondafone/ui/`: `RetroButton`, `RetroDebugBox`, `SpamReportView` (canvas-drawn retro dialog for in-app verdict).
- `app/src/test/java/`: local unit tests for decision, verdict-store (SQLite via Robolectric), and beacon logic (JUnit 4).
- `app/src/main/AndroidManifest.xml`: permissions (POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, RECEIVE_BOOT_COMPLETED, Bluetooth advertise/scan/connect, foreground service), screening service, beacon service, verdict/boot/exact-alarm receivers, and launcher activity.
- `app/src/main/res/values/colors.xml`: retro pastel palette (see Design).
- `app/build.gradle.kts`: Android build configuration, release shrinking, and beacon BuildConfig fields read from `local.properties` (`mondafone.beacon.cron`, `mondafone.beacon.window.ms`, `mondafone.beacon.app.secret`). The app secret is generated randomly and persisted to `local.properties` on first build when absent.
- `gradle/libs.versions.toml`: plugin version catalog; keep it dependency-free apart from required build plugins and the test-only Robolectric exception below.

## Build And Verify

Use the Gradle wrapper from the project root:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

The optimized release APK is generated at:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

Always run at least `assembleRelease` after code or manifest changes. A physical Android device is required to verify `CallScreeningService` behavior, since the emulator telephony stack does not exercise screening reliably.

## Design

Retro pastel design system for all UI:

- Palette lives in `app/src/main/res/values/colors.xml`: pastel primary, pastel secondary, and light pastel backgrounds. Never introduce non-pastel colors.
- Controls are flat surfaces with straight 2dp ink borders and hard rectangular shadows (4dp offset, no blur) to represent elevation.
- Pressed state: the shadow collapses and the surface shifts by the shadow offset.
- Square corners only; no rounded shapes, gradients, or image assets.
- Sans-serif typeface everywhere; monospace reserved for values that need emphasis (e.g. phone numbers).
- ASCII characters (`>`, `*`, `=`, `[ ]`) give relevance to text content, never as visual decoration.
- All UI is drawn with `Canvas` and native `View`s; no image assets.

## Implementation Rules

- Keep the APK as small as possible.
- Prefer framework APIs over third-party libraries.
- Do not add image assets for UI elements; draw simple UI elements with `Canvas`, except if the user explicitly requests it.
- Do not request `READ_CALL_LOG`, `READ_CONTACTS`, `READ_PHONE_STATE`, or other sensitive permissions. Android Telecom natively skips contacts when `ROLE_CALL_SCREENING` is held.
- Bluetooth advertise/scan/connect permissions are allowed only for the beacon feature; keep `BLUETOOTH_SCAN` flagged as `neverForLocation` so location is never requested.
- Never advertise the device public-key fingerprint in clear. The beacon payload is the rotating ID `Base64URL(HMAC-SHA256(appSecret, fingerprint || epochHour)[:7])`, derived by `RotatingBeaconId`, so nothing persistent leaves the radio and distinct devices still produce distinct IDs. Trust resolution uses `candidates`/`matches` with a ±1 hour clock tolerance.
- Filter scans with hardware `ScanFilter`s on the fixed service data UUID (`BeaconScanFilter`), never with an empty filter list.
- Schedule beacon windows with `BeaconAlarmScheduler` (`setAlarmClock`, falling back to `setAndAllowWhileIdle` without exact-alarm access). Alarm-clock alarms are the only class OEM power managers (MIUI/HyperOS) do not defer, at the cost of a system next-alarm icon. Never keep a 24/7 foreground service or a `Handler`-based cron: Doze freezes handler message loops. The PendingIntent targets the service directly.
- OEM battery managers can still defer alarms: request the battery-optimization exemption from the onboarding flow, and log late windows (`window late Xs`) so deferrals are visible in the debug box.
- Request the `ROLE_CALL_SCREENING` role through `RoleManager`, never by assuming granted access.
- Store rules and blocked-call history in app-specific storage unless a user-visible export is requested.
- Never block a call without recording a decision entry the user can review.
- Keep screening decisions fast and synchronous; do no network or heavy I/O on the screening callback path.
- Run on minSdk 31; do not add compat shims for older APIs.
- Use R8 and resource shrinking for release builds.
- Robolectric is the single test-only dependency exception (`testImplementation`), used to test `android.database.sqlite` on the JVM. It must never be declared as `implementation`/`api` and never ships in the APK; if a change cannot be covered on the JVM, verify it on a physical device instead.

## Change Checklist

1. Check whether the change adds a dependency or resource that can be avoided with a framework API.
2. Build both debug and release variants when changing Kotlin, the manifest, or Gradle configuration.
3. Check the release APK size after dependency or resource changes.
4. Test screening role grant/revocation, block rule matching, and in-app pending review on a physical device with real incoming calls.
5. Test the BLE beacon on physical devices (ideally two) and confirm the `Beacon active`, `received id ...`, and `count: ...` lines in the debug box. Also verify a window still fires while the device is idle (`adb shell dumpsys deviceidle force-idle`) and after a reboot. On aggressive OEMs (e.g., Xiaomi/HyperOS) verify with the screen off for several minutes that windows stay on schedule and no `window late` lines appear.

## Code Rules
1. KISS for all the code
2. POO principes to the limit: abstraction, encapsulation, inheritance, and polymorphism.
3. SOLID for all.
4. Code Human-Understandable, keep the functions short, prioritizing the little classes and functions for do one thing or groups of related things, good code indentation.
5. Clean-Code all the time, Variables, functions, and classes should have meaningful names.
6. Don't put comments to all, and if you do include them, keep them short.
7. English first, all the code have to be in English

## Workflow
1. `main` is the main branch, so always fork it when adding new features, fix bux or whatever thing.
2. Merge directly to `main` is bad, use pull request or suggest to the user do a pull request.
3. Do commits with less of 100 lines (no strict), and less of 10 files.
4. SSD for all, and TDD, do the test first and then the code.
