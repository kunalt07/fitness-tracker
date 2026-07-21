# Vector — Fitness Tracker — Context Summary

## Project basics
- **Path**: `/Users/kunaltigga/AndroidStudioProjects/FitnessTracker`
- **Repo**: `https://github.com/kunalt07/fitness-tracker` (public)
- **Last commit on `main`**: `46edab2` — "Docs: refresh CONTEXT to current main" (working tree clean, pushed to `origin/main`; see Recent commit history below)
- **Docs live in `docs/`**: `CONTEXT.md` (this — current state), `ROADMAP.md` (queue/done/won't-do), `PLAN.md` (shipped feature map), `DECISIONS.md` (why-log). Plan in VS Code / Claude Code; implement in Android Studio (same repo, shared files).
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
- AGP 9.3.0, Kotlin 2.2.10, KSP 2.3.2, Gradle wrapper 9.5.0
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
- **Plan → Log handoff**: generated plan queues exercises into `pending_plan`; applying a plan calls `repo.requestOpenSessionOnLog()` (one-shot StateFlow) so Log opens straight into the session view. Log consumes the flag via `consumeOpenSessionRequest()`.
- **Session-open flag is DB-backed**: `SessionDao.observeOpenCount()` counts not-ended sessions that have ≥1 logged set (empty auto-started sessions ignored). `repo.hasOpenSession: Flow<Boolean>` → both Home and Log agree across ViewModel instances (no more in-memory `activeSession` drift). Ending/discarding a session calls `repo.clearPlan()`.
- **Plan generated-result rendering** (`PlanScreen.kt`): AI markdown parsed into sections (`parsePlanSections`) and rendered as distinct exercise rows (`ExerciseResultRow`, name + sets×reps split on —/–/-/:) instead of raw `MarkdownText`. Result region `Crossfade`s skeleton→plan. Inputs wrapped in outlined `InputCard`s; equipment is a with/without icon toggle (blank string = bodyweight).
- AI errors mapped to friendly snackbar messages (not raw exceptions)

## Onboarding (`onboarding/`)
- First-launch three-step full-screen flow, once per device, after WelcomeScreen name capture, before main app. Every step skippable.
- Step 1: four weekly-split presets (PPL, Bro split, Upper/Lower, Full body 3-day) as cards with Sun…Sat dot strip → writes seven `WeeklySplitDay` rows. Also a **"Build my own"** custom editor: per-day focus text fields with quick-pick suggestion chips (`SPLIT_FOCUS_SUGGESTIONS`), "Rest"/blank → empty focus.
- Step 2: current/target weight decimal fields → `DailyViewModel.saveBodyWeight`, plus optional **daily calorie goal** field → `DietViewModel.saveCalorieGoal`
- Step 3: diet picker (Diet-tab accent colors)
- `OnboardingStore` singleton (SharedPreferences, mirrors `ThemeModeStore`)
- `MainActivity` gate: `profile == null` → Welcome; `!onboardingComplete` → Onboarding; else → FitnessApp

## Log workout page (`log/LogScreen.kt`)
- Muscle grid is the **default "workout page"** — `MuscleGroupGrid` now **multi-select** (`selected: List<String>`, `onToggleMuscleGroup`). "Start workout (N groups)" queues every exercise across the selected groups. Empty selection → plain empty session.
- Session view is a **separate mode** gated by `viewingSession` (plain `remember`, so re-entering the tab resets to the grid). While a session runs the grid shows **Continue session** / **Change group** (`PillCtaSecondary`, replaces the queued plan). Session ends → snap back to grid via `LaunchedEffect(active)`.
- The old per-muscle `ExerciseChooserSheet` picker is gone.
- Grid sizing: ≤4 vertical units fill screen height via `weight`; more → scroll at fixed 150dp tile height (`MUSCLE_MAX_UNITS_BEFORE_SCROLL`).

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

## Diet day plan + Home food logging (`45c6c1a`)
- **Diet "Suggest meals"**: floating button (bottom-right, accent-colored, clears nav) → `DietViewModel.suggestDayPlan()`. AI returns 5 meals spanning a MIX of dietary types, category chosen freely, calories summed to the goal (remaining budget if food logged), tuned by weight goal + workout consistency. Shown as pills (diet-color dot, name + kcal·protein) + a Total line; tap a pill → prep sheet (ingredients + steps).
- **Signals fed to the prompt** (all nullable, block emitted only when present): `weightGoalContext`, `calorieBudgetContext` (NEW — always surfaces goal + remaining, unlike `nutritionContext`), `summarizeRecentHistory`, `workoutConsistencyContext` (NEW — trained-days-in-14 + streak, from `setEntryDao.allSetTimestamps()`).
- **Model**: `data/PlannedMeal.kt` (`@Serializable`; category, dietType, name, description, calories, proteinG, ingredients, steps). Not a Room entity.
- **Persistence + sharing**: day plan saved per-day as JSON in the (otherwise-dead) `cached_diet_plan` table, `direction = "DAY_PLAN"` — NO schema migration. `FitnessRepository` (singleton) exposes `dayPlan: StateFlow`; both Diet and Home ViewModels share it. Diet VM keeps only a `PlanStatus` (Idle/Loading/Error); the meals list is the repo flow.
- **Menu search** (`DietScreen.kt`): search pill filters browsable category rows by meal name (case-insensitive); empty categories hidden while searching. Menu `LazyRow`s use plain `remember(pref?.type) { LazyListState() }` so they reset to the left on tab open / diet-type switch (not nav-restored).
- **Home Food sheet** (`home/DailySheets.kt` `FoodQuickAddSheet`): tap-to-log side-scroll suggestion pills (diet-colored); **name autocomplete** from menu + day-plan + past logs (`DietViewModel.foodSuggestions`) with an **"Ask AI"** fallback (`estimateFood`) that estimates macros for any food; **removable** "logged today" list (`deleteFoodEntry`); **over-goal confirm popup** (AlertDialog) when an add would exceed the calorie goal.

## Feature backlog
See `ROADMAP.md` for the live queue / done / won't-do. As of `ef4106a` the "Now" section holds the next planned item; ask the user before starting if unsure.

## Recent commit history (all on main; SHAs post-scrub)
- `46edab2` — Docs: refresh CONTEXT to current main (Log/Plan/Onboarding overhaul, build bump)
- `ef4106a` — Docs: add user-flows walkthrough (USER_FLOWS.md + HTML + screenshot)
- `3b7c9d2` — Build: bump AGP 9.3.0, KSP 2.3.2, Gradle wrapper 9.5.0
- `5c5fe47` — Log/Plan/Onboarding UX overhaul: multi-select workout page, structured plan, custom split
- `789aa64` — Add VSCode workspace settings; note verified AI context helpers in roadmap
- `473f780` — Roadmap: mark Diet day plan + Home food overhaul done
- `45c6c1a` — Diet day plan + Home food logging overhaul
- `1aa853c` — Diet AI: goal- and training-aware meal suggestions
- `4e711f0` — Motion polish: button press scale, chip bounce, nav fade-through
- `1e0d7c6` — Fill Log muscle grid to screen height instead of scrolling
- `f007a7d` — Move planning docs into docs/, add PLAN and DECISIONS
- `21aa4fc` — Muscle-group Log picker + fix frozen daily reset
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
- Working tree clean at `46edab2`, pushed to `origin/main`. Recent focus: Log multi-select workout page + session-view split, Plan structured render, onboarding custom split, Diet menu search. For next direction, read `ROADMAP.md` "Now" (still queues `swapMeal`).
- **Verify smoothness on RELEASE builds** — debug Compose is janky (proven repeatedly this project); nav/animation lag in debug is expected, gone in release. adb: `$HOME/Library/Android/sdk/platform-tools/adb`; device `2B121JEGR01006`. Release sig ≠ debug → uninstall before installing release (wipes data).
- Keystore + password are critical — flag for off-machine backup if user asks about distribution.
- User tends to ask for "blur"/"glass" — be honest real blur needs Haze; frame the tradeoff cleanly.
- AI-meal features share `FitnessRepository` singleton state across Diet/Home — keep new cross-screen data there, not per-VM.
