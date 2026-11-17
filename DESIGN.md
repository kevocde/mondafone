---
name: Mondafone
description: Civil-defense and neighborhood-watch dispatch design system for a zero-configuration spam-call guardian
colors:
  signal-sage: "#5A9E7C"
  terracotta-brick: "#D46F5A"
  warm-bone: "#F2ECE1"
  dispatch-stock: "#FAF6EE"
  navy-charcoal: "#141A1F"
  beacon-ochre: "#E0AC40"
  coin-glint: "#F4D58D"
  masthead-paper: "#FAF6EE"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "32sp"
    fontWeight: 700
  headline:
    fontFamily: "sans-serif"
    fontSize: "15sp"
    fontWeight: 700
  title:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 700
  body:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 700
  small:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    fontWeight: 700
  label:
    fontFamily: "sans-serif"
    fontSize: "10sp"
    fontWeight: 700
  badge:
    fontFamily: "sans-serif"
    fontSize: "8sp"
    fontWeight: 700
  value:
    fontFamily: "monospace"
    fontSize: "14sp"
    fontWeight: 700
  value-large:
    fontFamily: "monospace"
    fontSize: "22sp"
    fontWeight: 700
  value-small:
    fontFamily: "monospace"
    fontSize: "12sp"
    fontWeight: 400
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "20dp"
  xxl: "24dp"
components:
  protection-banner:
    backgroundColor: "{colors.dispatch-stock}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.headline}"
    height: "94dp"
    padding: "10dp"
  readiness-cell:
    backgroundColor: "{colors.dispatch-stock}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.label}"
    height: "48dp"
    width: "72dp"
  button-primary:
    backgroundColor: "{colors.signal-sage}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.body}"
    height: "48dp"
    padding: "0 12dp"
  button-primary-compact:
    backgroundColor: "{colors.signal-sage}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.small}"
    height: "48dp"
    padding: "0 12dp"
  button-danger:
    backgroundColor: "{colors.terracotta-brick}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.body}"
    height: "48dp"
    padding: "0 12dp"
  button-danger-compact:
    backgroundColor: "{colors.terracotta-brick}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.small}"
    height: "48dp"
    padding: "0 12dp"
  tab-active:
    backgroundColor: "{colors.signal-sage}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.title}"
    height: "48dp"
    padding: "0 20dp"
  tab-inactive:
    backgroundColor: "{colors.dispatch-stock}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.title}"
    height: "48dp"
    padding: "0 20dp"
  badge-coin:
    backgroundColor: "{colors.beacon-ochre}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.badge}"
  panel:
    backgroundColor: "{colors.dispatch-stock}"
    textColor: "{colors.navy-charcoal}"
    padding: "14dp"
  stat-item:
    backgroundColor: "{colors.dispatch-stock}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.value-large}"
    width: "96dp"
    height: "68dp"
  list-item:
    backgroundColor: "{colors.warm-bone}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.value}"
    padding: "12dp"
  empty-state:
    backgroundColor: "{colors.warm-bone}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.title}"
    height: "108dp"
  prompt-card:
    backgroundColor: "{colors.dispatch-stock}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.body}"
    padding: "24dp"
  debug-box:
    backgroundColor: "{colors.dispatch-stock}"
    textColor: "{colors.navy-charcoal}"
    typography: "{typography.value-small}"
    padding: "12dp"
---

# Design System: Mondafone

## Overview

**Creative North Star: "The Neighborhood Watch Dispatch (Boletín de Guardia Vecinal)"**

Mondafone looks like an official civil-defense and neighborhood-watch protection bulletin printed on warm bone stock: stamped status headers, a geometric Canvas shield-and-node emblem, semantic color bands across every module, and hard architectural offset shadows in deep navy-charcoal ink. The visual language turns an invisible background service (`CallScreeningService` + serverless BLE P2P sync) into a tangible, inspectable guard station on duty.

Every interactive control and live module is physical and mechanical. Buttons, inactive tabs, and readiness stage cells depress into their solid offset shadow on touch. Every module carries a top color band or diagonal Canvas hatch strip so state reads before a single word is read.

The entire interface is built exclusively with Android framework `Canvas` and native `View`s — zero third-party UI libraries, zero raster assets, zero rounded corners — while exposing full native `AccessibilityDelegate` semantics (`Button` roles, headings, tab collections, and live stage descriptions) and `sp`-scaled typography.

**Key Characteristics:**
- Warm Bone Paper ground (`#F2ECE1`) with Bright Dispatch Card Stock (`#FAF6EE`) and 2dp Navy-Charcoal Ink (`#141A1F`) borders.
- Top semantic color bands (6dp–10dp high, framed by a 2dp horizontal ink rule) on stat tiles, readiness cells, ticket rows, and prompt cards, with diagonal Canvas ink hatching on ungranted or empty states.
- Stamped Protection Banner (`RetroProtectionBanner`) pairing the `MONDAFONE` header with a live `[ ACTIVE GUARD ]` / `[ SETUP REQUIRED ]` stamp, a 45°/90° geometric Canvas shield/P2P emblem, and live stage/peer metrics.
- 4-stage horizontal Readiness Strip (`RetroReadinessStrip`) tracking `FILTER`, `ALERTS`, `BLE NET`, and `POWER` with 48dp touch targets and press-collapse physics.
- Slotted dispatch ticket rows (`VerdictListItemView`) with top semantic bands, `> number` monospace headers, inline classification prompt subtitles, and 48dp-touch-target compact keys.

## Colors

A three-signal pastel dispatch palette inked in deep navy-charcoal on warm bone paper: sage green certifies safety and active guard status, terracotta brick condemns spam, and beacon ochre flags pending items and setup stages.

### Primary
- **Signal Sage** (`#5A9E7C`, `primary`): active protection stamp (`[ ACTIVE GUARD ]`), verified readiness stage bands (`[OK]`), safe/whitelist verdicts (`No Spam`), active folder tab fill, and the network-devices stat band.

### Secondary
- **Terracotta Brick** (`#D46F5A`, `secondary`): spam condemnation (`Spam`), remove actions (`x`), blacklisted ticket bands, and the blocked-calls stat band.

### Tertiary
- **Beacon Ochre** (`#E0AC40`, `coin`): pending tab coin badges, `[ SETUP REQUIRED ]` stamp, ungranted readiness stage bands (paired with diagonal ink hatching), pending verdict ticket bands, and the spam-numbers stat band.
- **Coin Glint** (`#F4D58D`, `coin_glint`): highlight pixels inside the 6×6 pixel-art coin badge.

### Neutral
- **Warm Bone Paper** (`#F2ECE1`, `background`): screen ground, banner header bar fill, and slotted ticket row fill.
- **Dispatch Card Stock** (`#FAF6EE`, `surface`): raised cards, stat tiles, readiness cells, inactive tabs, and the main list panel.
- **Navy-Charcoal Ink** (`#141A1F`, `ink`): every 2dp border stroke, every 4dp/3dp hard offset shadow, diagonal hatch lines, corner registration ticks, and all text.
- **Masthead Paper** (`#FAF6EE`, `masthead`): fill of the outlined app wordmark.

### Named Rules
**The Semantic Top-Band Rule.** Every stat tile, readiness stage cell, verdict ticket row, and prompt card carries a horizontal top band (6dp–8dp high, closed by a 2dp ink line) colored by its semantic role (`primary` sage for safe/verified/network, `secondary` brick for blocked/spam, `coin` ochre for pending/setup). Side-border callout stripes are never used.

**The Hatch-Plus-Hue Rule.** Pending permission stages and empty states never rely on color alone: their top band draws 45° diagonal ink hatch lines across the band so state is unmistakable in monochrome or high-glare light.

## Typography

**Display Font:** sans-serif (system default, bold)
**Body Font:** sans-serif (system default)
**Value Font:** monospace (system default)

**Character:** Sober sans-serif carries all human voices, labels, questions, and empty-state prose, while bold tabular monospace is strictly reserved for machine-truth values (phone numbers, zero-padded counters, `[OK]` / `[  ]` / `[ -- ]` status codes, and debug logs). Every text size is specified in `sp` (`ComplexUnit.COMPLEX_UNIT_SP`) to honor system font scaling.

### Hierarchy
- **Display** (bold, 32sp): the `Mondafone` masthead wordmark — `masthead` fill with a 4dp `ink` outline and a 2dp bottom registration rule.
- **Headline** (bold, 15sp): the banner headline (`"The end of spam calls"`).
- **Title** (bold, 14sp): folder tab labels, empty-state messages, and debug header.
- **Body** (bold, 16sp): primary button labels and prompt questions.
- **Small** (bold, 12sp): compact row-action button labels and banner detail metrics.
- **Label** (bold, 10sp): uppercase stat-tile captions (`BLOCKED CALLS`, `SPAM NUMBERS`, `DEVICES`) and stamp pills.
- **Badge** (bold, 8sp): count inside the pixel coin badge, capped at `99+`.
- **Value** (monospace bold, 14sp): phone numbers in ticket rows (`> +57 300 123 4567`).
- **Value Large** (monospace bold, 22sp): zero-padded stat-tile counters (`00`, `14`).
- **Value Small** (monospace, 12sp): debug log lines.

### Named Rules
**The Monospace-Is-Meaning Rule.** Monospace sets phone numbers, numeric counters, bracketed status codes (`[OK]`, `[ -- ]`), and log lines. Prose, empty-state sentences, and questions always use sans-serif.

**The One-Outline Rule.** Only the top masthead wordmark uses an ink outline (4dp stroke under `masthead` fill) and bottom rule. All other typography is solid `ink` on paper.

## Layout

Both the onboarding screen and the main screen share the same top-anchored vertical column on Warm Bone Paper (`#F2ECE1`) with `20dp` base padding plus `WindowInsets.Type.systemBars()` edge-to-edge insets. Transitioning from onboarding to active guard never shifts the header vertically.

The vertical stack flows:
1. **Masthead Wordmark (`RetroTitle`)** (`8dp` bottom margin)
2. **Protection Dispatch Banner (`RetroProtectionBanner`)** (`8dp` bottom margin)
3. **4-Stage Readiness Strip (`RetroReadinessStrip`)** (`12dp` bottom margin on main screen, `24dp` before CTA on onboarding)
4. **3-Tile Stats Bar (`RetroStatsBar`)** (`18dp` bottom margin; equal-width tiles with `8dp` gutters)
5. **Folder Tab Bar (`RetroTabBar`) + Framed Panel (`RetroPanel`)** (`0dp` gap between tabs and panel; `14dp` internal panel padding with corner registration ticks and `8dp` gaps between ticket rows)

## Elevation & Depth

Depth is expressed through a single structural device: a solid Navy-Charcoal Ink (`#141A1F`) rectangle offset down and right with zero blur (`4dp` on cards, stat tiles, primary buttons, and inactive tabs; `3dp` on readiness cells and compact buttons).

### Shadow Vocabulary
- **Raised (`4dp` solid ink offset):** `RetroProtectionBanner`, `RetroStatItem`, default `RetroButton`, inactive `RetroTabBar` tabs, `SpamReportView`, `RetroDebugBox`.
- **Raised Compact (`3dp` solid ink offset):** `RetroReadinessStrip` stage cells and compact `RetroButton`s.

### Named Rules
**The Press-Collapse Rule.** Every interactive shadowed control (`RetroButton`, inactive `RetroTabBar` tab, `RetroReadinessStrip` stage cell) collapses its shadow on `ACTION_DOWN` and translates its surface by the shadow offset.

**The Flat-Container Rule.** Grouping containers (`RetroPanel`, the active tab, and ticket rows inside the panel) never cast a drop shadow.

## Shapes

Every surface, border, and shadow is an axis-aligned rectangle with `0dp` corner radius and a `2dp` ink stroke. Inside `RetroProtectionBanner`, the 40dp emblem box houses a geometric shield and 45°/90° P2P node-link diagram drawn with `Canvas.drawPath` and `drawLine`. Inside `RetroPanel`, 6dp L-shaped registration ticks sit inset `5dp` from each corner.

**The Square-Corner Rule.** Corner radius is `0dp` everywhere.

## Components

### Protection Dispatch Banner (`RetroProtectionBanner`)
- **Structure:** `94dp` high raised card (`surface` fill, `2dp` border, `4dp` shadow) with a `30dp` top header bar (`background` fill) displaying `MONDAFONE` on the left and a bordered status stamp (`[ ACTIVE GUARD ]` in `primary` sage or `[ SETUP REQUIRED ]` in `coin` ochre) on the right.
- **Body:** Left `40dp×40dp` framed emblem box with the geometric Canvas shield + P2P node icon; right headline (`app_subtitle`, bold 15sp) and live stage/peer status line (`status_protection_metrics` or `status_onboarding_detail`, 12sp).

### 4-Stage Readiness Strip (`RetroReadinessStrip`)
- **Structure:** 4 equal-weight interactive stage cells (`48dp` high, `6dp` gutters) for `FILTER`, `ALERTS`, `BLE NET`, and `POWER`.
- **Visuals:** `6dp` top band (`primary` sage when granted; `coin` ochre with 45° ink hatch lines when pending), `[OK]` or `[  ]` in bold 10sp monospace, and uppercase stage label in bold 9sp sans-serif. Collapses its `3dp` shadow on press and triggers `requestAccess()`.

### Stat Tiles (`RetroStatItem` / `RetroStatsBar`)
- **Structure:** `96dp×68dp` raised tiles (`4dp` shadow) with a `6dp` semantic top band (`secondary` brick for Blocked calls, `coin` ochre for Spam numbers, `primary` sage for Network devices), zero-padded 22sp bold monospace value (`00`), and uppercase 10sp bold label.

### Folder Tabs (`RetroTabBar`) & Registration Panel (`RetroPanel`)
- **Tabs:** `48dp` high, `40dp` horizontal padding around label. Active tab uses `primary` fill, open bottom edge, and a `2dp` inner top rule (`4dp` below top edge). Inactive tabs use `surface` fill, `4dp` press-collapse shadow, and `2dp` border. Pending counts render in the top-right `6×6` pixel-art coin badge (`3dp` inset so it never clips).
- **Panel:** Flat `surface` container with `2dp` border, `14dp` padding, and `6dp` L-shaped corner registration ticks inset `5dp`.

### Verdict Ticket Rows (`VerdictListItemView`)
- **Structure:** Framed ticket box on `background` fill with a `6dp` top semantic band (`coin` for Pending, `primary` for Whitelist, `secondary` for Blacklist) and `2dp` ink border.
- **Content:** Left column shows `> number` in bold 14sp monospace plus an optional sans-serif subtitle line below it (`Was this call spam?` on Pending rows); right side holds compact `RetroButton` keys (`48dp` touch height, `36dp` drawn body, `3dp` shadow).

### Hatched Empty State (`RetroEmptyStateView`)
- **Structure:** `108dp` framed card on `background` fill with a `10dp` diagonal-hatched top band, a centered `64dp×24dp` `surface` box reading `[ -- ]` in bold 13sp monospace, and the empty-state message in bold 14sp sans-serif below.

## Do's and Don'ts

### Do:
- **Do** draw every UI element with `Canvas` and native `View`s using colors resolved exclusively from `res/values/colors.xml`.
- **Do** size all text in `sp` (`TypedValue.COMPLEX_UNIT_SP`) and keep every interactive touch target at least `48dp` high.
- **Do** use top horizontal semantic bands (and diagonal Canvas hatching on ungranted/empty states) across cards, tiles, and ticket rows.
- **Do** keep the onboarding and main screen headers top-anchored at the same `20dp` base padding + system-bar insets so transitions never jump.
- **Do** wire native `AccessibilityDelegate` semantics (`Button` roles, headings, tab collections, and live state descriptions) on every custom view.

### Don't:
- **Don't** use kickers or `// CODE` eyebrows above headings.
- **Don't** use colored left-border stripes on cards or list rows; use the horizontal top band instead.
- **Don't** set prose or empty-state sentences in monospace, or phone numbers in sans-serif.
- **Don't** round corners, add gradients/blur, or introduce third-party UI libraries.
