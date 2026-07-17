# Vector — Fitness Tracker — Context Summary

## Project basics
- **Path**: `/Users/kunaltigga/AndroidStudioProjects/FitnessTracker`
- **Repo**: `https://github.com/kunalt07/fitness-tracker` (public)
- **Last commit on `main`**: `16352f4` — "Animate Stats charts on entry — sweep, grow, trace" (working tree clean, all pushed to `origin/main`)
- **History rewritten**: `Co-Authored-By: Claude` trailer scrubbed from all 12 commits via `git filter-branch` + force-push. SHAs changed. Safety net at `refs/original/refs/heads/main` (local only). Do NOT re-add the trailer on future commits — user does not want Claude as contributor.
- **App name**: Vector (was Fitness Tracker)
- **Package**: `com.example.fitness_tracker` (intentionally NOT renamed; stays for Firebase compat)
- **Tagline**: AI-coached, local-first Android fitness app — Home/Plan/Log/Diet/Stats tabs

## Stack
- Kotlin + Jetpack Compose, Compose BOM `2026.02.01`, Material 3 (Expressive)
- Room `2.7.0-rc03` + KSP, single Room DB at **v13** with manual migrations (`data/FitnessDatabase.kt`)
- kotlinx-serialization `1.7.3` (added for backup/restore JSON)
- Firebase AI `17.+` via `GenerativeBackend.googleAI()` — Gemini 2.5 Flash
- minSdk/targetSdk **37** (Android 16)
- AGP 9.2.1, Kotlin 2.2.10
- **No DI**, **no tests**, **no Hilt**. Repository singleton via `companion object get(context)`.

## Build
- **Debug APK**: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (~70MB)
- **Release APK**: `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (~5.5MB)
- **Keystore**: `vector-release.jks` at repo root (gitignored), password `QHWKEagItClAKY08MQei`, alias `vector`. `keystore.properties` also gitignored. R8 + ProGuard rules at `app/proguard-rules.pro`.
- `google-services.json` IS committed (per user choice — Firebase API key isn't a cryptographic secret)

## Color system
- **Brand**: deep violet `#5B3FFF` light / `#9B89FF` dark — used ONLY on primary CTAs (Generate plan, Use this plan, Log set, Start workout, Regenerate) and the nav-bar selection pill
- `primaryContainer` neutralized to gray `#EDEDED` / `#262626` so brand violet doesn't leak into selected surfaces
- **Plan tab accent**: teal `#0EA5A0`
- **Diet tab accent**: amber `#E89B3D` default; switches to selected diet-type color (Veg green, Non-veg red, Vegan emerald, Eggetarian amber)
- **Stats accent**: steel blue `#4A6FA5`; PR badges in gold `#D9A341`
- **Streak**: coral `#E76F51`
- **Per-muscle palette** in `muscleGroupColor()` and `focusKeywordColor()`
- **Dark mode is pitch black**: `#000000` bg/surface, `#161616` surfaceVariant. AMOLED-friendly.
- **Light surfaces**: `#FCFCFC`

## Theme switcher
- `ui/theme/ThemeMode.kt` — enum + `ThemeModeStore` singleton (SharedPreferences + StateFlow)
- `ui/theme/Theme.kt` — `ThemeMode.resolveDarkTheme()` extension
- `MainActivity` observes `ThemeModeStore`, recomposes whole tree on change, and flips `isAppearanceLightStatusBars` via `WindowCompat` in lockstep with resolved theme
- Profile → Settings → "Theme" row → AlertDialog with 3 RadioButtons

## Architecture highlights
- `FitnessApp.kt` hosts NavHost + `FloatingNavBar` (rounded pill, 360ms FastOutSlowIn tween on selection, no tap ripple, swipe gestures). `Surface` is fully opaque (`surface`, elevation 4dp).
  - `LocalSnackbarHost` CompositionLocal for app-wide undo
  - 5 tabs: Home / Plan / Log / Diet / Stats; Profile route via Home avatar
- Local-only profile via single-row Room `user_profile` table (`auth/AuthRepository.kt`)
- AI prompt context helpers in `FitnessRepository`: `summarizeRecentHistory`, `weightGoalContext`, `nutritionContext`
- Plan AI results live in memory only — `PlanViewModel.init` calls `clearCachedPlan()` so each launch starts blank
- Per-day weekly split with chip-and-suggestion picker (`FocusPicker`); equipment uses same `ChipPicker` pattern
- **Plan → Log handoff**: generated plan queues exercises into `pending_plan`; Log auto-starts a session for them
- AI errors mapped to friendly snackbar messages (not raw exceptions)

## Onboarding (`onboarding/`)
- First-launch three-step full-screen flow, once per device, after WelcomeScreen name capture, before main app. Every step skippable.
- Step 1: four weekly-split presets (PPL, Bro split, Upper/Lower, Full body 3-day) as cards with Sun…Sat dot strip → writes seven `WeeklySplitDay` rows
- Step 2: current/target weight decimal fields → `DailyViewModel.saveBodyWeight`
- Step 3: diet picker (Diet-tab accent colors)
- `OnboardingStore` singleton (SharedPreferences, mirrors `ThemeModeStore`)
- `MainActivity` gate: `profile == null` → Welcome; `!onboardingComplete` → Onboarding; else → FitnessApp

## Backup / restore (`data/BackupDao.kt`, `BackupRepository.kt`)
- Every Room table → single JSON document and back, via stock SAF dialogs
- Every Room entity is `@Serializable`; JSON shape mirrors schema 1:1 (no DTOs)
- `BackupDao` centralises bulk full-table reads/deletes/inserts. Restore wipes child tables before parents, re-inserts parents before children (FK order), whole restore in one Room transaction (atomic).
- Payload carries `schemaVersion`; restoring a future-schema backup bails with friendly message
- Profile → Data section: Export + Restore rows. Restore shows destructive-confirm dialog first.

## Personal-record (PR) badges
- Gold "PR" pill on Log set rows that are the all-time best estimated-1RM (Epley) for their exercise
- `AggregateDao` sibling query returns winning `set_entries.id` set; Repository exposes as Flow; `LogViewModel` → `Set<Long>` for O(1) row-render lookup. Reactive — log heavier set, badge moves.
- Same gold PR badges also on Stats tab.

## Stats chart animations (`stats/StatsScreen.kt`)
- All three Canvas charts animate in on entry, each via `Animatable` + `tween(FastOutSlowInEasing)`, keyed on its data so it replays when stats change:
  - `MuscleBalanceBar` — segments sweep left→right (700ms)
  - `WeeklyBars` — bars grow up from baseline, per-bar stagger (0.4 span, 800ms)
  - `ProgressChart` — 1RM line traces in by path length, dots pop as the trace reaches each point (800ms)
- No new dependency. Replays on tab re-entry (Stats recomposes).

## Home hero
- Reads `pending_plan` count: "N exercises queued for today" caption + CTA becomes "Start planned workout"
- Quiet secondary "Plan today with AI" link when nothing queued and no session running (in-app discovery path to AI generator)

## Feature backlog (user's priority order)
1. ✅ **Onboarding flow** — done (`9a06a10`)
2. ✅ **Backup / restore** — done (`9457cf3`)
3. ✅ **"Last time" data in set-entry sheet** — done (shipped in `48c0883`/now `e2ca16f`; Log shows previous session's reps × weight, excludes current session).

**Backlog now empty.** No open items. Ask user for next direction.

## Recent commit history (all on main, all pushed — SHAs post-scrub)
- `16352f4` — Animate Stats charts on entry — sweep, grow, trace
- `e2ca16f` — Fun animations + quick-logging polish (Home streak flame, dot-grid heatmap, count-up stats, Food quick-add; Log "last time" hint, PR confetti + haptic, rest-timer ring)
- `b9ab719` — Inline PR badge on Log set rows
- `a124863` — Surface today's planned workout on Home hero
- `111a488` — Polish: light-mode status bar, calmer nav slide, solid regenerate button
- `90c9853` — Map AI errors to friendly snackbar messages
- `9457cf3` — JSON backup/restore via SAF
- `9a06a10` — First-launch onboarding
- `f519c86` — Solidify nav bar, strip Log dock card so controls float bare
- `4a8cb1a` — Deep violet brand, theme picker, IME-aware Plan, translucent docks
- `4a69a78` — Cleanup: drop AppIcons, LICENSE, sheet-state deprecation fix
- `f67c23b` — Release build config (signing, R8, ProGuard)
- `cdd1104` — Rebrand to Vector — launcher artwork, color system, Plan/Diet UX

## User context
- User goes by `kunalt07` on GitHub
- Building iteratively as a portfolio project
- Iterates a LOT on UI details — likes to see changes immediately, reverts often
- Prefers minimal/clean aesthetics, dislikes anything "solid" or "boxy"
- Has working signed release APK on phone
- Usually picks up next session where the current one ended
- Reverted real GPU blur (Haze) experiment — wanted glass look but not the dependency

## Known weak spots / ongoing tensions
- Translucent dock + backdrop pattern produces visible "white rectangles" in light mode (Material 3 translucent surface IS a rectangle when content scrolls behind). Real blur (Haze) was the only proper fix; user reverted it.
- Floating dock has no card chrome — controls float over scroll content. Quick log pencil icon may be hard to read against busy content (no individual backdrop).

## Session-resume cues
- Working tree clean at `16352f4`. Backlog empty — if user says "continue", ask for direction (recent focus: animations). Candidate next animation work not yet done: NavHost tab-switch content transitions; new micro-interactions (button press scale, chip select bounce).
- Keystore + password are critical — flag for off-machine backup if user asks about distribution.
- User tends to ask for "blur"/"glass" — be honest real blur needs Haze; frame the tradeoff cleanly.
