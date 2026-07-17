# Vector — Roadmap

Priority-ordered feature backlog. Newest ideas at the bottom of each section.
Move items to **Done** with the commit SHA when shipped. Keep `CONTEXT.md`
as the "current state of the world"; keep this as "what's next / what's decided not to do."

## Now (next up)

### Goal- & training-aware Diet AI
Make meal suggestions optimal for the user's goal, driven by their food-category
choice **and** exercise performance. The suggestion feature already exists
(`DietViewModel.suggestMore → buildPrompt → parseMealsJson`) but is goal-blind —
it only sees category + diet type + existing names. This feeds it the context the
app already has, so macros support the objective and recovery.

**Scope: 1 file — `diet/DietViewModel.kt`. Prompt-only. No schema/UI/parse/error change.**

1. `suggestMore()` — before `buildPrompt`, gather three existing repo helpers
   (all `suspend fun(): String?`, already called on `Dispatchers.IO`):
   - `repo.weightGoalContext()` — bulk vs cut, current/target kg
   - `repo.nutritionContext()` — today's kcal + protein vs goal
   - `repo.summarizeRecentHistory()` — 14-day muscle-group tallies + top lifts (the exercise-performance signal)
   Pass all three into `buildPrompt`.

2. `buildPrompt(...)` — add three nullable params and, when ≥1 is non-null, emit:
   - a `USER CONTEXT — tailor the meals to this:` block listing the non-null lines
   - an `OPTIMIZE FOR THE GOAL:` block:
     - bias macros toward the weight goal (bulk → higher protein + surplus; cut → higher protein + modest deficit)
     - favor protein/carbs for the most-recently-trained muscle groups (recovery)
     - if today's nutrition is shown, size portions to fill the *remaining* budget, not exceed it

**Design calls**
- Emit the context block only when ≥1 signal present, so a fresh user (no goal / no
  logs / no training) still gets clean generic suggestions — no empty scaffolding.
- Keep category + `strictnessHint()` diet logic exactly as-is.
- Error path already routes through `friendlyAiError()`; parse path already solid.

**Verify (in Android Studio, needs a device — AI output can't be checked offline)**
Set a weight goal + log a workout, then Diet tab → suggest meals → confirm macros
skew toward the goal and the trained muscles. Fresh-user path (no data) still works.

**Ref (file:line at time of planning)**
- `diet/DietViewModel.kt:93` suggestMore, `:121` buildPrompt
- `data/FitnessRepository.kt:161` weightGoalContext, `:134` nutritionContext, `:480` summarizeRecentHistory

## Next (queued, not started)
- Directional nav slide tied to swipe direction (needs from/to index tracking) — only if fade-through feels flat

## Later / ideas (unranked)
- _park half-formed ideas here so they aren't lost_

## Done
- Diet day plan + Home food overhaul — `45c6c1a` (Diet "Suggest meals" floating button → AI day plan: mixed diet types, any category, sums to calorie goal, weight-/consistency-tuned; pills w/ diet color + tap-for-prep; persisted per-day in `cached_diet_plan`, shared to Home via repo StateFlow. Home Food sheet: tap-to-log side-scroll suggestion pills, name autocomplete (menu/plan/logs + AI fallback), removable entries, over-goal confirm popup.)
- Goal- & training-aware Diet AI — `1aa853c` (prompt-only; feeds weight goal + nutrition budget + training tallies into suggestMore)
- Motion polish: button press scale + chip select bounce + nav fade-through — `4e711f0` (verified smooth on release; debug build was janky)
- Fill Log muscle grid to screen height — `1e0d7c6`
- Muscle-group Log picker + fixed frozen daily reset — `21aa4fc`
- Animate Stats charts on entry — `16352f4`
- Fun animations + quick-logging polish — `e2ca16f`
- Inline PR badge on Log set rows — `b9ab719` / `d2eed50`
- Surface today's planned workout on Home hero — `94da4e4`
- JSON backup/restore via SAF — `9457cf3`
- First-launch onboarding — `9a06a10`

## Won't do (with reason)
- Real GPU blur / glass (Haze dependency) — reverted; user wants the look but not the dep.
