# Vector

A local-first Android fitness app with AI coaching, built in Kotlin + Jetpack Compose.

Track workouts, log meals, measure progress, and let Gemini generate workout plans and meal suggestions tailored to your data — all running on-device with a single sign-in-free Room database.

The name reflects what fitness tracking actually is — direction (cut / bulk / maintain) plus magnitude (sets, weight, calories).

## What's in it

**Five tabs**

- **Home** — daily greeting, hero "Today" card with focus + stats, four daily check-in tiles (Quick log / Weight / Calories / Water), week bars + streak.
- **Plan** — AI workout generator (Gemini 2.5 Flash). Editable weekly split, day picker with today indicator, goal/duration chips, history-aware prompts.
- **Log** — workout tracker with grouped sets (Hevy-style), kind-aware set entry (reps × weight / time / distance), Quick log free-text via AI, repeat-last pill, rest timer, save-as-template + AI critique on session end.
- **Diet** — scrollable menu of meals filtered by Veg / Non-veg / Vegan / Eggetarian. ~26 hand-picked starters, AI "Suggest more" per category, full recipe detail sheet.
- **Stats** — window switcher (Week / Month / All), streak / sessions / sets, volume card, muscle balance bar, personal records, per-exercise progress chart, sessions list.

**Plus** a Profile screen accessed via the Home avatar — local profile (name + email), settings access, clear data.

## Architecture highlights

- **Local-first.** All workouts, sessions, sets, meals, body weight, and goals live in a single Room database (currently v13 with manual migrations across all schema bumps). No cloud, no auth backend, no account required.
- **AI integration.** Firebase AI / Google AI backend (Gemini 2.5 Flash) powers workout plans, meal suggestions, free-text set logging, and post-workout critiques. Plans cache per day to avoid burning tokens.
- **Material 3 Expressive.** `MaterialExpressiveTheme`, `MotionScheme.expressive()`, animated floating bottom nav with horizontal swipe, gradient fades behind floating CTAs.
- **Brand color.** Monochrome surfaces (`#FCFCFC` / `#101010`) with a single green primary (`#1F8A4C` / `#7BD7A2`). Per-muscle palette for stripes, balance bars, planned chips, and today's focus.
- **Geometric sans-serif** (Roboto), Light/Medium weights, tight tracking on display sizes.

## Tech stack

- Kotlin + Jetpack Compose, Compose BOM `2026.02.01`
- Material 3 `1.5.0-alpha22` (Expressive APIs)
- Room `2.7.0-rc03` with KSP
- Navigation Compose
- Firebase AI `17.+` (Google AI backend)
- AlarmManager + BroadcastReceivers for daily reminders
- minSdk / targetSdk **37** (Android 16)

## Build & run

### Prerequisites
- Android Studio (Hedgehog or newer recommended)
- JDK 11
- An Android device or emulator running API 37 (Android 16)

### 1. Clone

```bash
git clone https://github.com/kunalt07/fitness-tracker.git
cd fitness-tracker
```

### 2. Firebase setup (required for AI features)

The repo includes a `google-services.json` for the development Firebase project, so the app will build and the AI features will work out of the box for casual exploration.

If you're going to actually use this seriously or fork it, **swap in your own Firebase project** so your usage doesn't compete with mine for free-tier quota:

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app with package name `com.example.fitness_tracker`.
3. In Firebase console: **Build → AI Logic → Get started**, choose **Gemini Developer API**, enable it.
4. Download the new `google-services.json` and replace `app/google-services.json`.

The app uses `GenerativeBackend.googleAI()` (the free Gemini Developer API), not the paid Vertex AI backend, so you don't need to enable billing.

### 3. Build

```bash
./gradlew assembleDebug
```

Or just open in Android Studio and hit Run.

### 4. First launch

You'll see a "Welcome" screen — type a name + optional email. The profile saves locally and the rest of the app unlocks.

## Notable features that aren't obvious

- **Undo on every delete.** Sets, sessions, templates, exercises, meals — all dismiss into a snackbar with restore.
- **Loading skeletons** during AI calls — shimmer-shaped to match the eventual layout.
- **Daily reminder notification.** Schedule a time, get a nudge with today's focus. Survives device reboots.
- **Session resume.** Reopen the app within 4 hours of starting a session and pick up where you left off.
- **Plan auto-load.** First Plan-tab visit each day generates silently using today's focus + your weight goal + recent training history. Cached.
- **Per-exercise progress chart** with estimated 1RM (Epley formula).
- **Personal records** ranked by 1RM, displayed with last-time hint.
- **Body weight + target.** Setting both feeds the AI prompt with bulk/cut/maintenance context, biasing workout and meal suggestions accordingly.

## Project structure

```
app/src/main/java/com/example/fitness_tracker/
  auth/           Local profile (no Firebase Auth — single Room row)
  data/           Room entities, DAOs, FitnessRepository, seed data
  diet/           Diet tab — meal menu + AI suggestions
  history/        Sessions list (rendered inside Stats now)
  home/           Home tab + daily check-in sheets
  log/            Workout logging — set sheet, quick log, critique
  plan/           AI workout plan + weekly split + reminders
  profile/        Profile screen (settings + clear data)
  reminder/       AlarmManager + BroadcastReceivers
  stats/          Stats tab + per-exercise progress chart
  ui/             Shared components (theme, MinimalUi, Markdown, skeletons)
```

## Things deliberately not in the project

- **No DI framework** (Hilt/Koin) — repository singletons via `companion object get(context)`. Pragmatic at this scale.
- **No tests** — focus is the live app and architecture, not coverage.
- **No launcher icon yet** — uses the Android Studio default. PRs welcome.
- **No CSV export** — planned.
- **No Health Connect integration** — the app is deliberate-logger, not passive-tracker.
- **No iOS / Compose Multiplatform** — Android only for now.

## License

MIT — see `LICENSE` if added, or use freely.

---

Built as a portfolio + learning project. Not affiliated with any commercial fitness service.
