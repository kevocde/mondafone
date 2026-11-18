# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Non-technical people harassed by spam and scam calls who want automatic protection without configuration. They install the app once, grant the requested permissions, and expect it to silently guard their phone; their only ongoing interaction is reviewing blocked calls and pending verdicts in the app.

## Product Purpose

Mondafone rejects spam calls before the phone rings, using Android's `CallScreeningService`. It combines the user's own verdicts (whitelist / spam reports) with a global spam probability built from reports received from nearby devices, and it records every blocking decision so the user can review it. Success means spam calls never ring and the user never has to think about configuration.

## Positioning

Two things a neighboring product could not truthfully copy:

- **Serverless P2P trust network:** spam reports are exchanged directly between nearby devices over BLE beacons and GATT sync, signed with a per-device ECC key from Android Keystore. There is no backend, no account, and no central database to trust, breach, or shut down.
- **Extreme simplicity:** a ~161 KB APK built only on Android framework APIs — no third-party libraries, no sensitive permissions (no call log, contacts, or phone state), no ads, no tracking.

## Operating Context

Distributed as a public FOSS release and on the Play Store. Runs on Android 12+ (minSdk 31). The app depends on OEM-specific behavior (exact alarms, battery-optimization exemptions), so onboarding chains the required grants. Verification of screening and BLE sync happens on physical devices; the emulator telephony stack does not exercise call screening reliably.

## Capabilities and Constraints

- Call screening order: contacts (native) → whitelist → own reports → global chance `1 - 2^-n` over foreign reporters. Whitelist wins over own reports.
- Reports are stored as `HMAC-SHA256(appSecret, canonicalNumber)`; plain numbers live only in the own-report alias table for display. Remote reports are persisted only after ECDSA signature verification, monotonic sequence check, and freshness validation.
- The BLE beacon advertises a time-rotating ID derived from the device fingerprint via HMAC; the fingerprint never leaves the radio in clear. Scanning always uses hardware `ScanFilter`s; location is never requested.
- Beacon windows are scheduled with `setAlarmClock`; night quiet hours (22:00–05:00) are skipped. No 24/7 foreground service.
- No network access of any kind; every screening decision is fast, synchronous, and on-device.
- UI is built exclusively from native `Canvas`/`View` drawing — no Compose, AndroidX, Material, image assets, or rounded shapes.

## Brand Commitments

- Name: **Mondafone** (`dev.polyrics.mondafone`).
- Binding aesthetic: a retro-print design system (flat pastel surfaces, 2dp ink borders, hard rectangular 4dp shadows, square corners, sans-serif type, ASCII markers). The palette is guardian-green primary (safe/trust), pastel brick secondary (danger), warm retro paper backgrounds, and a mustard coin accent, defined in `app/src/main/res/values/colors.xml`. This system deliberately overrides Material 3 defaults.
- Voice: sober and minimal; ASCII characters give relevance to content, never decoration.

## Evidence on Hand

No testimonials, press, or external proof assets exist. Future work must not fabricate them. `AGENTS.md` is the authoritative project guide; the debug box in debug builds is the live evidence surface for screening and beacon behavior.

## Product Principles

1. **Zero-configuration protection:** the app must work correctly after onboarding with no settings to tune.
2. **Privacy by architecture, not policy:** nothing sensitive can leak because the app never holds it (no sensitive permissions, hashed numbers, rotating IDs, no network).
3. **Every decision is reviewable:** never block a call without a recorded entry the user can inspect and reverse.
4. **Minimal footprint:** framework APIs only; the APK stays as small as possible.
5. **No central point of failure or trust:** the P2P network must function with arbitrary devices appearing and disappearing.
