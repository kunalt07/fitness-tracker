# Vector — Project Plan & Feature Map

Portfolio-ready retrospective: everything built so far, grouped by subsystem, then
what's next. Pairs with `CONTEXT.md` (current state), `DECISIONS.md` (why), and
`ROADMAP.md` (queue). This doc is the "what have I actually shipped" narrative.

_State: `main` @ `1e0d7c6`. Kotlin/Compose,
Room v13, Firebase AI (Gemini 2.5 Flash). Local-first, no DI, no tests._

---

## What Vector is

AI-coached, local-first Android fitness app. Five tabs — **Home / Plan / Log /
Diet / Stats** — plus Profile. Everything lives on-device in one Room DB; the
only network call is the AI plan generator. Design language: minimal, AMOLED-black
dark mode, deep-violet brand reserved for primary CTAs, per-tab accent colors.

---

## Feature map (by subsystem)

### 1. Shell & navigation
- `FitnessApp` NavHost + custom `FloatingNavBar` (opaque rounded pill, 360ms tween,
  swipe gestures, no ripple). Screens reserve bottom padding to clear the bar.
- App-wide undo via `LocalSnackbarHost` CompositionLocal.
- Launch gate in `MainActivity`: no profile → Welcome; onboarding incomplete →
  Onboarding; else → main app.

### 2. Onboarding & profile
- First-launch 3-step flow (split preset → body weight → diet), every step skippable,
  once per device (`OnboardingStore`).
- Local-only profile, single-row `user_profile` table (`AuthRepository`).
- Profile → Settings: theme picker (Light/Dark/System, `ThemeModeStore`), Data
  section (export/restore), reminder settings.

### 3. Plan (AI)
- Per-day weekly split, chip + AI-suggestion pickers (`FocusPicker`, `ChipPicker`).
- Firebase AI (Gemini 2.5 Flash) generates plan from history/weight/nutrition context.
- Plan results in-memory only — cleared each launch (fresh start every time).
- **Plan → Log handoff**: generated plan queues into `pending_plan`; Log auto-starts.
- AI errors mapped to friendly snackbars (`ai/AiErrors.kt`).

### 4. Log (workout logging)
- Muscle-group grid (Chest/Triceps/Legs/Shoulders/Biceps/Core + full-width Back) →
  exercise chooser sheet → stages exercises into the session.
- Default exercise library per muscle group; seeding backfills missing by name.
- Set entry with "last time" hint (previous session reps × weight).
- Gold **PR badges** — all-time best Epley 1RM per exercise, reactive (log heavier → badge moves).
- Rest-timer ring, PR confetti + haptic.
- Inline Start button (clears floating nav).

### 5. Diet
- Food quick-add, macro tracking, diet-type accent colors
  (Veg green / Non-veg red / Vegan emerald / Eggetarian amber).

### 6. Stats & history
- Three animated Canvas charts on entry: `MuscleBalanceBar` (sweep),
  `WeeklyBars` (grow), `ProgressChart` (1RM trace). Replay on tab re-entry.
- PR badges surfaced here too.
- History: past sessions list (`history/SessionsList.kt`).

### 7. Reminders _(undocumented until now)_
- Daily workout nudge via `AlarmManager` exact-and-allow-while-idle (falls back to
  inexact when `SCHEDULE_EXACT_ALARM` denied on Android 12+).
- `ReminderReceiver` posts the notification; `BootReceiver` reschedules after reboot;
  `ReminderScheduler` computes next trigger. User-configurable hour/minute, on/off.

### 8. Data & persistence
- Single Room DB v13, manual migrations.
- Reactive day-boundary rollover (60s ticker + `distinctUntilChanged` + `flatMapLatest`)
  — today's totals roll at midnight without app kill.
- **Backup/restore**: whole DB ↔ one JSON via SAF, atomic transaction, FK-ordered,
  schema-version guarded (`backup/`, `BackupDao`, `BackupRepository`).

### 9. Build & release
- Signed release APK (`vector-release.jks`), R8 + ProGuard, ~5.5MB.
- Debug ~70MB. `google-services.json` committed (Firebase key not a crypto secret).

---

## Roadmap forward

Authoritative queue lives in `ROADMAP.md`. Snapshot:

**Now** — _empty; pick from Next._

**Next**
- NavHost tab-switch content transitions
- Micro-interactions: button press scale, chip select bounce

**Candidate (unranked)**
- Reminder UX polish / smart reminder timing
- Diet AI suggestions (parity with Plan AI)
- Widget or quick-tile for "start planned workout"
- Tests around Room migrations + backup round-trip (first test surface if app grows)

**Won't do** — real GPU blur / Haze dependency (reverted; keep the tradeoff).

---

## Health / debt
- No tests — highest-value first target is backup round-trip + migration safety.
- Translucent dock shows "white rectangles" in light mode (accepted, see DECISIONS).
- `CONTEXT.md` drifts behind `main` — was stale at `16352f4`; reminder subsystem and
  `21aa4fc` Log rework were undocumented. Keep it current per commit.
