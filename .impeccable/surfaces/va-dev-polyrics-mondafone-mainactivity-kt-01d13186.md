---
version: 1
slug: "va-dev-polyrics-mondafone-mainactivity-kt-01d13186"
primary_target: "app/src/main/java/dev/polyrics/mondafone/MainActivity.kt"
related_targets: ["app/src/main/java/dev/polyrics/mondafone/ui/RetroTitle.kt","app/src/main/java/dev/polyrics/mondafone/ui/RetroStatsBar.kt","app/src/main/java/dev/polyrics/mondafone/ui/RetroStatItem.kt","app/src/main/java/dev/polyrics/mondafone/ui/RetroTabBar.kt","app/src/main/java/dev/polyrics/mondafone/ui/RetroPanel.kt","app/src/main/java/dev/polyrics/mondafone/ui/VerdictListItemView.kt","app/src/main/java/dev/polyrics/mondafone/ui/SpamReportView.kt","app/src/main/java/dev/polyrics/mondafone/ui/RetroButton.kt"]
---

# Main Activity & Call Protection Console

## Scope and Mode
- **Mode:** Operate
- **Targets:** `MainActivity.kt` and all custom views in `app/src/main/java/dev/polyrics/mondafone/ui/`

## Task and Constraints
- **Job:** Confirm active spam protection at a glance, complete chained permission onboarding with clear stage progress, monitor live call/network counters, and classify or remove phone numbers in Pending, Whitelist, and Blacklist tabs.
- **Constraints:** Native Android `Activity`, `Canvas`, and `View` only; square corners (0dp radius); 2dp ink borders; 4dp hard offset shadows on pressable/live elements; no third-party UI libraries or raster assets.

## Direction contract

THESIS: Mondafone operates as a Civil Defense & Neighborhood Watch Protection Bulletin ("Boletín de Guardia Vecinal") where active call screening and serverless BLE P2P defense read as a tangible, stamped security dispatch rather than a passive settings screen.

OWN-WORLD: Warm bone paper ground (`#F2ECE1`) and bright dispatch card stock (`#FAF6EE`) inked in deep navy-charcoal (`#141A1F`), paired with signal sage green (`#5A9E7C`) for active protection and safe verdicts, hazard terracotta brick (`#D46F5A`) for spam condemnation, and beacon ochre (`#E0AC40`) for pending badges and P2P network activity. Every card, stat tile, and verdict row carries a semantic top/side color band, 2dp ink framing, 4dp hard offset shadow, and Canvas-drawn geometric shield/node emblems or diagonal hatch textures.

STORY: Opening the app immediately proves the guard is on duty via a stamped header banner and a 4-stage permission readiness strip; the user scans live blocked/spam/peer counts and resolves pending numbers with tactile mechanical buttons.

FIRST VIEWPORT: Top stamped Protection Header Card (`MONDAFONE` + `[ GUARDIA ACTIVA ]` stamp + Canvas geometric shield/P2P emblem + status copy), followed by a 4-cell horizontal readiness strip (`[OK] FILTRO`, `[OK] AVISOS`, `[OK] RED BLE`, `[OK] ENERGÍA`), a 3-tile stats bar with semantic top color bands (`Bloqueadas`, `Números Spam`, `Dispositivos`), and the folder-tabbed verdict panel with boxed call rows and `SPAM` / `NO SPAM` tactile keys. In onboarding mode, the 4-stage checklist leads with a prominent primary action button.

FORM: Civil Defense & Neighborhood Watch Bulletin (Candidate 3 on ordered list), raised with 4-stage readiness strip, semantic cell bands, 45°/90° Canvas shield emblem, and Canvas line-hatched empty states; `seed:6f3ed428edaa8ef1`.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
