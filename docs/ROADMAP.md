# Vector — Roadmap

Priority-ordered feature backlog. Newest ideas at the bottom of each section.
Move items to **Done** with the commit SHA when shipped. Keep `CONTEXT.md`
as the "current state of the world"; keep this as "what's next / what's decided not to do."

## Now (next up)

### Swap a single day-plan meal
The day plan is all-or-nothing right now — if one of the 5 meals doesn't suit
(allergy, missing ingredients, don't like it), the only option is regenerating
the whole plan. Add a per-meal "swap" that replaces just that one, keeps the
rest, and stays within the same calorie budget.

**Scope: `diet/DietViewModel.kt` (new fn + prompt) + `diet/DietScreen.kt` (swap affordance). Reuses day-plan persistence + `parseDayPlanJson`.**

1. `swapMeal(index: Int)` — regenerate ONE meal at `index`:
   - reuse the same signals (weightGoalContext, calorieBudgetContext, workoutConsistencyContext, summarizeRecentHistory)
   - prompt for a single replacement in a *different* dietary type/category from the one removed, sized to fill the gap so the day's total stays near goal (pass the other 4 meals' combined kcal)
   - on success, rebuild the list with the new meal at `index` → `repo.saveDayPlan(newList)` (persists + updates Home)
   - per-index loading marker (`StateFlow<Int?>`) so only that pill spins
2. `DietScreen.kt` — swap trigger: small refresh icon on each pill, or a "Swap this meal" row in `PlannedMealDetailSheet`. Show the per-index spinner while regenerating.

**Design calls**
- One in / one out — don't renumber or reshuffle the other meals.
- Swap failure → leave the original in place, quiet fail (snackbar).

**Verify (device, AI output)**
Generate a day plan → swap one meal → only that pill changes, total stays near goal, Home reflects the new meal.

_(Proposed by Claude — re-order or replace if something else is higher priority.)_

**Signals verified** (`FitnessRepository`, all `suspend fun(): String?`, null when no data):
`weightGoalContext` :216 · `calorieBudgetContext` :188 (goal + remaining today) · `workoutConsistencyContext` :272 (14-day trained days + streak) · `summarizeRecentHistory` :480 (muscle tallies + top lifts).

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
