# Mondafone Agent Guide

## Project Scope

Mondafone is a minimal Android call anti-spam app written in Kotlin. It is intentionally limited and always have to use Android framework APIs over other Libs:

- `CallScreeningService` for rejecting and filtering incoming calls before they ring.
- `Telecom` and `TelephonyManager` for call state and telecom integration.
- `ContactsContract` for allowlist lookups against the user's contacts.
- Native Android `Activity`, `Canvas`, and `View` for the UI.

The application keeps a minimal, low-footprint UI. Do not add Compose, AndroidX, Material, AppCompat, Room, WorkManager, or coroutine dependencies unless explicitly requested.

## Repository Layout

- `app/src/main/java/dev/polyrics/mondafone/MainActivity.kt`: permission requests and monitor service startup.
- `app/src/main/java/dev/polyrics/mondafone/spam/`: verdict store (`SpamStore`/`PrefsSpamStore`) and `SpamReportDecider` rules (incoming, min duration, not contact, not registered).
- `app/src/main/java/dev/polyrics/mondafone/contacts/`: `ContactsLookup` queries against `ContactsContract`.
- `app/src/main/java/dev/polyrics/mondafone/monitor/`: `CallMonitorService` foreground daemon, `ContentObserver` on `CallLog`, and `CallLogReader`.
- `app/src/main/java/dev/polyrics/mondafone/ui/`: `RetroButton`, `SpamReportView` and `SpamReportOverlay` (canvas-drawn retro dialog over `TYPE_APPLICATION_OVERLAY`).
- `app/src/test/java/`: local unit tests for decision logic (JUnit 4).
- `app/src/main/AndroidManifest.xml`: permissions, monitor service, and launcher activity.
- `app/src/main/res/values/colors.xml`: retro pastel palette (see Design).
- `app/build.gradle.kts`: Android build configuration and release shrinking.
- `gradle/libs.versions.toml`: plugin version catalog; keep it dependency-free apart from required build plugins.

## Build And Verify

Use the Gradle wrapper from the project root:

```bash
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
- Do not request `READ_CALL_LOG`, `READ_CONTACTS`, or other sensitive permissions unless the screening design explicitly requires them.
- Request the `ROLE_CALL_SCREENING` role through `RoleManager`, never by assuming granted access.
- Store rules and blocked-call history in app-specific storage unless a user-visible export is requested.
- Never block a call without recording a decision entry the user can review.
- Keep screening decisions fast and synchronous; do no network or heavy I/O on the screening callback path.
- Run on minSdk 31; do not add compat shims for older APIs.
- Use R8 and resource shrinking for release builds.

## Change Checklist

1. Check whether the change adds a dependency or resource that can be avoided with a framework API.
2. Build both debug and release variants when changing Kotlin, the manifest, or Gradle configuration.
3. Check the release APK size after dependency or resource changes.
4. Test screening role grant/revocation, block rule matching, contacts allowlist, and activity destruction on a physical device with real incoming calls.

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
