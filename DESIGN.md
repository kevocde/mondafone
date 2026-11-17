---
name: Mondafone
description: Retro-print pastel design system for a zero-configuration spam-call guardian
colors:
  guardian-green: "#7CBE96"
  pastel-brick: "#DA8A78"
  warm-newsprint: "#F4F0E2"
  bright-paper: "#FBF8EE"
  ink-black: "#000000"
  mustard-coin: "#E3B84F"
  coin-glint: "#F3D78C"
  masthead-white: "#FFFFFF"
typography:
  display:
    fontFamily: "sans-serif"
    fontSize: "36sp"
    fontWeight: 700
  headline:
    fontFamily: "sans-serif"
    fontSize: "18sp"
    fontWeight: 700
  title:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 700
  body:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 400
  small:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    fontWeight: 400
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
    fontSize: "16sp"
    fontWeight: 400
  value-large:
    fontFamily: "monospace"
    fontSize: "22sp"
    fontWeight: 400
  value-small:
    fontFamily: "monospace"
    fontSize: "12sp"
    fontWeight: 400
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  xxl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.guardian-green}"
    textColor: "{colors.ink-black}"
    typography: "{typography.body}"
    height: "48dp"
    padding: "0 12dp"
  button-primary-compact:
    backgroundColor: "{colors.guardian-green}"
    textColor: "{colors.ink-black}"
    typography: "{typography.small}"
    height: "32dp"
    padding: "0 12dp"
  button-danger:
    backgroundColor: "{colors.pastel-brick}"
    textColor: "{colors.ink-black}"
    typography: "{typography.body}"
    height: "48dp"
    padding: "0 12dp"
  button-danger-compact:
    backgroundColor: "{colors.pastel-brick}"
    textColor: "{colors.ink-black}"
    typography: "{typography.small}"
    height: "32dp"
    padding: "0 12dp"
  tab-active:
    backgroundColor: "{colors.guardian-green}"
    textColor: "{colors.ink-black}"
    typography: "{typography.title}"
    height: "40dp"
    padding: "0 16dp"
  tab-inactive:
    backgroundColor: "{colors.bright-paper}"
    textColor: "{colors.ink-black}"
    typography: "{typography.title}"
    height: "40dp"
    padding: "0 16dp"
  badge-coin:
    backgroundColor: "{colors.mustard-coin}"
    textColor: "{colors.ink-black}"
    typography: "{typography.badge}"
  panel:
    backgroundColor: "{colors.bright-paper}"
    textColor: "{colors.ink-black}"
    padding: "12dp"
  stat-item:
    backgroundColor: "{colors.bright-paper}"
    textColor: "{colors.ink-black}"
    typography: "{typography.value-large}"
    width: "96dp"
    height: "64dp"
  list-item:
    backgroundColor: "{colors.bright-paper}"
    textColor: "{colors.ink-black}"
    typography: "{typography.value}"
    padding: "8dp 16dp"
  prompt-card:
    backgroundColor: "{colors.bright-paper}"
    textColor: "{colors.ink-black}"
    typography: "{typography.body}"
    padding: "24dp"
  debug-box:
    backgroundColor: "{colors.bright-paper}"
    textColor: "{colors.ink-black}"
    typography: "{typography.value-small}"
    padding: "12dp"
---

# Design System: Mondafone

## Overview

**Creative North Star: "The Risograph Guardian"**

Mondafone looks like a protection notice run off a small risograph press: warm paper stock, two pastel ink plates with strictly assigned meanings, and hard offset shadows that read as deliberate print misregistration. The mood is sober, warm, mechanical — a guard on duty, not a toy. The retro layer exists to make a silent background service feel tangible and trustworthy when the user does open it, never to entertain.

Every control is tactile and mechanical. Buttons look pressable because they are: the solid offset shadow collapses under the finger and the surface shifts into the space the shadow occupied, like a machine key. Depth is information — if it carries the hard shadow, you can press it or it is reporting live state; if it is flat, it is only paper.

The system is drawn entirely with Android `Canvas` and native `View`s — no widget library, no image assets, no Compose/Material/AndroidX — and every drawn control carries native `AccessibilityDelegate` semantics (button roles, headings, tab collections, live content descriptions) so the print-shop surface never costs screen-reader users anything.

**Key Characteristics:**
- Warm paper backgrounds with flat pastel fills and straight 2dp ink borders.
- One depth cue: a solid ink offset shadow (4dp, no blur), reserved for pressable or live elements.
- Press physics: shadow collapses, surface translates by the shadow offset.
- Square corners everywhere; the pixel-art coin badge is the only non-rectangular silhouette.
- Sans-serif for voices, monospace reserved for machine-truth values; ASCII markers (`>`, `x`) mark relevance, never decoration.

## Colors

A two-verdict pastel ink system printed on warm paper: green says safe, brick says danger, paper holds still, and a mustard coin appears only when something is waiting.

### Primary
- **Guardian Green** (#7CBE96): the affirmative, safe, or selected state. Grant-access button, "No Spam" / "No" verdicts, the active tab fill, and the launcher background.

### Secondary
- **Pastel Brick** (#DA8A78): every destructive or spam-condemning action. "Spam", "Yes" (confirming spam), the "x" remove buttons, and the debug box's Clear button.

### Tertiary
- **Mustard Coin** (#E3B84F): count badges only — the pixel coin on tabs that reports how many items wait behind it.
- **Coin Glint** (#F3D78C): highlight pixels inside the coin badge; never used anywhere else.

### Neutral
- **Warm Newsprint** (#F4F0E2): the page background of every screen.
- **Bright Paper** (#FBF8EE): raised surfaces — panels, stat tiles, inactive tabs, prompt cards, the debug box.
- **Ink Black** (#000000): every border stroke, every shadow, all text. (A softer `ink` resource at #1A1A1A exists in `colors.xml` but is unused; the drawn system inks with pure black.)
- **Masthead White** (#FFFFFF): the fill of the outlined app title; used nowhere else.

### Named Rules
**The Two-Verdict Rule.** Guardian Green and Pastel Brick are verdicts, not decoration. Green marks affirmative/safe/selected, brick marks danger/remove. They never appear as ornaments, dividers, or empty-state flair.

**The One Coin Rule.** Mustard Coin is the only accent allowed outside the two-verdict system, and it may only say one thing: "something is waiting here." If there is no count, there is no coin.

## Typography

**Display Font:** sans-serif (system default, bold)
**Body Font:** sans-serif (system default)
**Value Font:** monospace (system default)

**Character:** one plain sans-serif carries every voice in the app — sober and direct — while monospace is held back for values that come from the machine: phone numbers, counts, log lines. The pairing is deliberately unstyled; authority comes from the outlined masthead, not from font choice.

### Hierarchy
- **Display** (bold, 36sp): the app wordmark only — white fill with a 4dp ink outline, the system's single piece of display typography.
- **Headline** (bold, 18sp): the screen subtitle directly under the wordmark ("The end of spam calls").
- **Title** (bold, 14sp): tab labels and the debug box header.
- **Body** (regular, 16sp): button labels, questions, and primary copy.
- **Small** (regular, 12sp): compact button labels.
- **Label** (bold, 10sp): stat-tile captions ("Blocked calls", "Spam numbers", "Devices").
- **Badge** (bold, 8sp): the count inside the coin badge, capped at "99+".
- **Value** (monospace, 16sp): phone numbers in lists and verdict prompts.
- **Value Large** (monospace, 22sp): the number on a stat tile.
- **Value Small** (monospace, 12sp): debug log lines and empty-state messages.

### Named Rules
**The Monospace-Is-Meaning Rule.** Monospace marks machine truth — numbers, counts, logs. It never sets prose, questions, or labels, and prose never sets phone numbers.

**The One-Outline Rule.** The wordmark is the only outlined text in the app (white fill, 4dp stroke). All other text is solid ink on paper; if a piece of text needs emphasis, it gets bold, not an outline.

## Layout

Every screen is a single vertical column on Warm Newsprint with 24dp page padding. The vertical rhythm runs on a 16dp module: sections separate by 16dp, major blocks (the stats row) by 32dp, list rows and inline button pairs by 8dp, and the smallest gutters (between tabs) by 4dp.

The main screen stacks wordmark → subtitle → stats row → tab bar → list panel. The stats row is equal-weight tiles with 8dp gutters. The tab bar sits flush on top of the list panel with no gap — the active tab drops its bottom border and shadow so it reads as the panel's open lid, while inactive tabs stay raised. List rows inside the panel separate with a 1dp ink hairline at their bottom edge. Verdict prompt cards center their content with 24dp internal padding.

Panels that group content (the list panel) use 12dp internal padding; list rows use 16dp horizontal / 8dp vertical padding.

## Elevation & Depth

The system has exactly one depth cue: a solid Ink Black rectangle offset 4dp down and 4dp right behind the surface (3dp for compact buttons). No blur, no transparency, no tonal layering — the shadow is printed, not cast. Its meaning is semantic: **shadow = pressable or live**. Buttons, inactive tabs, the verdict prompt card, stat tiles, and the debug box carry it; the page background and the list panel — pure containers — stay flat.

The press state is the system's only motion: the shadow disappears and the surface translates by the shadow offset, so the element physically moves into the space its shadow occupied. On release it returns and the action fires.

### Shadow Vocabulary
- **Raised** (4dp solid ink offset, no blur): default buttons, inactive tabs, stat tiles, prompt cards, debug box.
- **Raised Compact** (3dp solid ink offset, no blur): compact buttons inside list rows and headers.

### Named Rules
**The Press-Collapse Rule.** A shadowed element must collapse its shadow and translate by the offset while pressed. An element that cannot collapse its shadow must not have one.

**The Flat-Container Rule.** Surfaces that only group content — the page background, the list panel, the active tab — never carry the shadow. Raising a container implies an action that does not exist.

## Shapes

The form language is the rectangle. Every surface, border, and shadow is an axis-aligned rectangle with square corners; nothing is rounded, clipped, or rotated. Bordered surfaces stroke with a straight 2dp ink line; row dividers are a 1dp ink hairline.

The single exception is the coin badge: a 6×6 pixel-art disc built from 2dp squares in Mustard Coin with Coin Glint highlight pixels, framed by a square ink outline. Its silhouette is round only in the pixel-art sense — it is still drawn from rectangles.

**The Square-Corner Rule.** Corner radius is 0dp everywhere, always. No rounded rectangles, pills, circles, or blobs; softness comes from the pastel palette, never from geometry.

## Components

### Buttons
Tactile machine keys — filled pastel rectangles that physically depress into their shadow.

- **Shape:** square-cornered rectangle, 2dp ink border, hard offset shadow (4dp; 3dp compact).
- **Primary (Guardian Green):** affirmative actions — 48dp high, 12dp horizontal padding, body text (16sp), centered, ink on green.
- **Danger (Pastel Brick):** destructive/spam verdicts — identical geometry, brick fill.
- **Compact variants:** 32dp high, small text (12sp), 3dp shadow; used inside list rows and the debug header.
- **Pressed:** shadow collapses, surface translates by the offset (see The Press-Collapse Rule).

### Tabs
File-folder tabs fused to the panel they open.

- **Active:** Guardian Green fill, 2dp ink border on three sides with the bottom edge open, no shadow — it reads as the panel's lid. 40dp high, 16dp horizontal padding, title text (bold 14sp).
- **Inactive:** Bright Paper fill, full 2dp border, 4dp hard shadow.
- **Badge:** pending counts ride the tab as the pixel-art coin (top-right, overlapping the frame) with the count in badge text (bold 8sp), capped at "99+".

### Panels
Flat paper containers.

- **Corner style:** square; 2dp ink border; Bright Paper fill; 12dp internal padding; **no shadow** (The Flat-Container Rule).
- **Shadowed card variant:** the verdict prompt card uses the same paper and border plus the 4dp hard shadow, because it asks a question the user must answer — it is pressable, so it is raised.

### Stat Tiles
Live counters in the stats row.

- **Size:** 96dp × 64dp; Bright Paper, 2dp ink border, 4dp hard shadow (they report live state).
- **Content:** Value Large (monospace 22sp) centered at ~38% height, Label (bold 10sp) at ~78%.
- **Row:** equal-weight tiles with 8dp gutters.

### List Rows
Verdict and list entries inside the panel.

- **Structure:** phone number in Value (monospace 16sp) on the left, compact action buttons on the right with 8dp between them.
- **Padding:** 16dp horizontal, 8dp vertical; rows separate with a 1dp ink hairline at the bottom edge.
- **Actions:** danger-compact for remove/spam, primary-compact for keep/whitelist.

### Verdict Prompt Card
The in-app "Was this call spam?" question.

- **Style:** Bright Paper panel with 2dp ink border and 4dp hard shadow, 24dp internal padding, centered content.
- **Content:** the question in Body (16sp, bold), the number in Value (monospace 16sp) wrapped in ASCII markers as `> number <`, then a button row ("Yes" danger, "No" primary) with 8dp margins.

### Debug Box
The live evidence surface in debug builds.

- **Style:** Bright Paper, 2dp ink border, 4dp hard shadow, 12dp padding; header in Title (bold 14sp) with a compact danger "Clear" button; log lines in Value Small (monospace 12sp).
- **Accessibility:** the whole box is screen-reader focusable with a concatenated content description; log lines are hidden from the tree individually.

### Masthead Title
The app wordmark — the system's signature.

- **Style:** Display (bold 36sp) in Masthead White with a 4dp ink outline (stroke drawn under the fill). It is an accessibility heading and the only outlined text in the app (The One-Outline Rule).

## Do's and Don'ts

### Do:
- **Do** draw every UI element with `Canvas` and native `View`s; keep the APK free of widget libraries.
- **Do** reserve Guardian Green for affirmative/safe/selected states and Pastel Brick for danger/remove actions (The Two-Verdict Rule).
- **Do** set phone numbers, counts, and log output in monospace; set questions, labels, and prose in sans-serif (The Monospace-Is-Meaning Rule).
- **Do** give every pressable element the 2dp ink border and the 4dp (3dp compact) hard shadow, and collapse the shadow on press (The Press-Collapse Rule).
- **Do** keep containers flat: no shadow on the page background, the list panel, or the active tab (The Flat-Container Rule).
- **Do** use ASCII markers (`>`, `x`) to give relevance to text content, never as visual decoration.
- **Do** wire native accessibility semantics (button role, headings, tab collection, content descriptions) into every drawn control.

### Don't:
- **Don't** round any corner — radius is 0dp on every surface, border, and shadow.
- **Don't** use gradients, blur, transparency, or soft drop shadows; depth is the one solid ink offset and nothing else.
- **Don't** introduce Compose, Material, AppCompat, or AndroidX components; the system is framework `Canvas`/`View` only.
- **Don't** add colors outside the pastel palette, and never a saturated accent.
- **Don't** set prose in monospace or phone numbers in sans-serif.
- **Don't** outline any text other than the wordmark.
