# ReliableAlarm — Design System
> Version 1.0 | Energetic · Rounded · Friendly

---

## 1. Design Philosophy

**One sentence:** ReliableAlarm feels like a coach in your pocket — warm, energetic, and genuinely on your side.

**Personality:** Not clinical like a utility app. Not aggressive like a productivity app.
Think Duolingo meets Nike Run Club — it celebrates wins, makes streaks feel like achievements,
and never looks like it was assembled from Stack Overflow snippets.

**The rule:** Every screen should feel like it was designed, not built.

---

## 2. Color System

### Core Palette

| Token                  | Hex       | Usage                                              |
|------------------------|-----------|----------------------------------------------------|
| `background_main`      | `#F5F3EF` | App background — warm off-white, not clinical      |
| `surface_card`         | `#FFFFFF` | Card surfaces                                      |
| `surface_dark`         | `#1C1C1E` | Hero cards, high-emphasis surfaces                 |
| `surface_elevated`     | `#EFEDE9` | Subtle inset areas, input backgrounds              |

### Accent Colors

| Token                  | Hex       | Usage                                              |
|------------------------|-----------|----------------------------------------------------|
| `accent_orange`        | `#FF5C00` | PRIMARY — streak fire, CTAs, active states         |
| `accent_orange_light`  | `#FFF0E8` | Orange tint backgrounds                            |
| `accent_orange_dark`   | `#CC4900` | Pressed state for orange                           |
| `success_green`        | `#00C48C` | Completed / success states                         |
| `success_green_light`  | `#E6FAF5` | Green tint backgrounds                             |
| `danger_red`           | `#FF3B30` | Missed alarm, destructive actions                  |
| `danger_red_light`     | `#FFF0EF` | Red tint backgrounds                               |
| `streak_gold`          | `#FFB800` | Best streak, trophy, achievement badges            |
| `streak_gold_light`    | `#FFF8E0` | Gold tint backgrounds                              |

### Text Colors

| Token                  | Hex       | Usage                                              |
|------------------------|-----------|----------------------------------------------------|
| `text_primary`         | `#1C1C1E` | Headlines, primary content                         |
| `text_secondary`       | `#48484A` | Body text, descriptions                            |
| `text_tertiary`        | `#8E8E93` | Labels, captions, placeholders                     |
| `text_inverse`         | `#FFFFFF` | Text on dark surfaces                              |
| `text_inverse_soft`    | `#EBEBF5` | Secondary text on dark surfaces (60% opacity)      |

### How to use colors

- **Orange is sacred.** Only use `accent_orange` for the single most important action or
  number on each screen. Never use it for 3+ elements simultaneously.
- **Dark surfaces create drama.** Use `surface_dark` (#1C1C1E) for the hero card on any
  screen that needs visual weight — streak count, alarm ringing screen, achievement unlocked.
- **Background warmth.** `#F5F3EF` vs `#FFFFFF` is subtle but critical. The slight warmth
  stops the app looking like a hospital form.
- **Never use pure black text** on white — always `text_primary` (#1C1C1E).

---

## 3. Typography

### Font Family
**Nunito** (Google Fonts — free, already used by Duolingo, Finch)

Why Nunito:
- Rounded terminals = friendly, not corporate
- Bold weights have real visual impact for numbers
- Legible at small sizes for captions
- Available as a downloadable font in Android via `res/font/`

```xml
<!-- res/font/nunito_regular.ttf  -->
<!-- res/font/nunito_semibold.ttf -->
<!-- res/font/nunito_bold.ttf     -->
<!-- res/font/nunito_extrabold.ttf-->
```

### Type Scale

| Role            | Size   | Weight      | Usage                                    |
|-----------------|--------|-------------|------------------------------------------|
| `text_display`  | `64sp` | ExtraBold   | Streak number hero — the scoreboard      |
| `text_title`    | `24sp` | Bold        | Screen titles                            |
| `text_headline` | `20sp` | Bold        | Card titles, section headers             |
| `text_subtitle` | `17sp` | SemiBold    | Sub-headers, alarm names                 |
| `text_body`     | `15sp` | Regular     | Body copy, descriptions                  |
| `text_caption`  | `13sp` | Regular     | Labels, metadata                         |
| `text_small`    | `11sp` | SemiBold    | Tags, badges, legend items               |
| `text_micro`    | `10sp` | Bold        | ALL CAPS section labels (letterSpacing 0.12) |

### Typography Rules

- **Numbers are heroes.** Streak count, success rate, best streak — always max weight,
  max size. They should feel like a scoreboard, not a label.
- **Never ALL CAPS for more than 3 words.** Section labels (REPEAT, DETAILS) use 10sp bold
  with 0.12 letter spacing maximum.
- **Line height matters.** Set `android:lineSpacingMultiplier="1.3"` on body text blocks.
- **No Roboto, no system-ui defaults.** Always set `android:fontFamily="@font/nunito_bold"`
  explicitly on key text elements.

---

## 4. Shape & Spacing

### Corner Radius

| Token            | Value  | Usage                                          |
|------------------|--------|------------------------------------------------|
| `radius_xs`      | `8dp`  | Chips, tags, small elements                    |
| `radius_s`       | `12dp` | Input fields, small cards                      |
| `radius_m`       | `16dp` | Standard cards                                 |
| `radius_l`       | `20dp` | Hero cards, bottom sheets                      |
| `radius_xl`      | `28dp` | FAB, pill buttons                              |
| `radius_circle`  | `999dp`| Day selector circles, avatar bubbles           |

**Rule:** Default to `radius_l` (20dp) for any full-width card. The roundness signals
friendliness — don't use sharp 4dp corners anywhere in this app.

### Spacing Scale

| Token        | Value  |
|--------------|--------|
| `spacing_xs` | `4dp`  |
| `spacing_s`  | `8dp`  |
| `spacing_m`  | `16dp` |
| `spacing_l`  | `20dp` |
| `spacing_xl` | `24dp` |
| `spacing_xxl`| `32dp` |

**Breathing room rule:** Cards get `20dp` internal padding minimum. Never 8dp or 12dp
inside a card — it looks cramped and cheap.

---

## 5. Elevation & Depth

**This app uses flat depth, not shadow depth.**

| Layer            | Treatment                                          |
|------------------|----------------------------------------------------|
| Background       | `#F5F3EF` warm off-white                           |
| Cards (default)  | `cardElevation="0dp"` + `strokeWidth="1dp"` stroke `#EEECE8` |
| Cards (hero)     | `surface_dark` background, no stroke, no shadow    |
| Bottom sheets    | `cardElevation="8dp"` only                         |
| FAB              | `elevation="4dp"` only                             |

**Why flat:** Material elevation shadows look dated in 2025. Flat cards with a 1dp border
on a warm background look intentional and modern (see Notion, Linear, Stripe dashboard).

---

## 6. Component Patterns

### Cards
```xml
app:cardCornerRadius="20dp"
app:cardElevation="0dp"
app:strokeWidth="1dp"
app:strokeColor="#EEECE8"
app:cardBackgroundColor="@color/surface_card"
```

### Hero Dark Card (for streak count, alarm firing screen)
```xml
app:cardCornerRadius="24dp"
app:cardElevation="0dp"
app:strokeWidth="0dp"
app:cardBackgroundColor="@color/surface_dark"
```

### Primary Button
```xml
android:height="56dp"
app:cornerRadius="16dp"
app:backgroundTint="@color/accent_orange"
android:textColor="@android:color/white"
android:textSize="16sp"
android:textStyle="bold"
android:letterSpacing="0.01"
app:elevation="0dp"
```

### Outlined Secondary Button
```xml
android:height="48dp"
app:cornerRadius="12dp"
app:strokeColor="@color/accent_orange"
app:strokeWidth="1.5dp"
android:textColor="@color/accent_orange"
app:backgroundTint="@android:color/transparent"
app:elevation="0dp"
```

### Day Selector (Repeat grid)
- Size: `40dp × 40dp` circle
- Unselected: background `#EFEDE9`, text `text_tertiary`
- Selected: background `accent_orange`, text `white`, bold
- Margin: `3dp` each side

### Input Fields
```xml
style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
app:boxCornerRadius*="12dp"
app:boxStrokeColor="@color/accent_orange"
app:hintTextColor="@color/accent_orange"
app:boxStrokeWidthFocused="2dp"
app:boxStrokeWidth="1dp"
```

### Section Labels
```xml
android:textSize="10sp"
android:textStyle="bold"
android:textColor="@color/text_tertiary"
android:letterSpacing="0.12"
android:text="SECTION NAME"
```

---

## 7. Iconography

- Icon size in cards: `20dp × 20dp`
- Icon size standalone/hero: `40dp × 40dp`
- Icon tint on white surface: `accent_orange` for primary actions, `text_tertiary` for neutral
- Icon tint on dark surface: `#FFFFFF`
- Never use emoji in UI — always vector drawables with explicit tint

---

## 8. Screen-by-Screen Design Intent

### Home (Alarm List)
- Background `background_main`
- Each alarm card: white, `radius_l`, flat
- Alarm time in `text_display` size, bold — this is the hero of each card
- Active toggle: orange switch
- FAB: orange, `radius_xl`, bottom-right, 88dp from bottom nav

### Alarm Edit
- Time picker as hero — full-width white section at top
- Cards below for Repeat, Tasks, Details
- Save button: full-width orange, 56dp height

### Streak Screen
- Dark hero card at top with streak number in `text_display` (64sp, ExtraBold, orange)
- Stats row: two white cards with gold and green accents
- GitHub-style contribution grid for 90 days
- No spinner dropdown — use a horizontal chip selector for alarm switching

### Reliability Screen
- Progress ring or large percentage as hero
- Grouped settings in white cards

### Tasks Screen
- Each task as a card with icon, name, toggle
- Task icon in a colored bubble (orange tint background)

---

## 9. What to Never Do

- ❌ Purple (`#6650A4`) — it's the Material 3 default. We are not a Material demo.
- ❌ Grey cards on grey background — no contrast, no depth
- ❌ Emoji in UI strings (use vector drawables)
- ❌ `cardElevation` > 2dp on regular cards
- ❌ ALL CAPS text longer than 3 words
- ❌ Default Roboto/system font on key display text
- ❌ Spinner dropdowns for navigation — use chips or tabs
- ❌ `android:hint` on both `TextInputLayout` AND `TextInputEditText`
- ❌ Icon + number + label all the same size — establish hierarchy
- ❌ Symmetric layouts everywhere — let the streak number breathe asymmetrically

---

## 10. colors.xml Reference

```xml
<!-- Background -->
<color name="background_main">#F5F3EF</color>
<color name="surface_card">#FFFFFF</color>
<color name="surface_dark">#1C1C1E</color>
<color name="surface_elevated">#EFEDE9</color>
<color name="card_stroke">#EEECE8</color>

<!-- Accent -->
<color name="accent_orange">#FF5C00</color>
<color name="accent_orange_light">#FFF0E8</color>
<color name="accent_orange_dark">#CC4900</color>

<!-- Semantic -->
<color name="success_green">#00C48C</color>
<color name="success_green_light">#E6FAF5</color>
<color name="danger_red">#FF3B30</color>
<color name="danger_red_light">#FFF0EF</color>
<color name="streak_gold">#FFB800</color>
<color name="streak_gold_light">#FFF8E0</color>

<!-- Text -->
<color name="text_primary">#1C1C1E</color>
<color name="text_secondary">#48484A</color>
<color name="text_tertiary">#8E8E93</color>
<color name="text_inverse">#FFFFFF</color>
```

---

*Last updated: March 2026 | ReliableAlarm v1*