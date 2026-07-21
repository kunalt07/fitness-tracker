# Vector — User Flows

Every user-reachable flow in the app, mapped from code as of the current working tree.
Use this to spot dead ends, redundant paths, and friction. Each section lists the
**entry points**, the **steps**, and **↳ design notes** (optimization candidates — not yet decided).

Screens: **Home · Plan · Log · Diet · Stats** (5-tab floating pill nav) + **Profile** (via Home avatar).
Nav = tap a pill or horizontal swipe (60px threshold) between adjacent tabs. `FitnessApp.kt:230–340`.

---

## 0. First launch / cold start

**Gate:** `MainActivity.kt:49–55` — three-tier decision every launch:

```
profile == null ──────────────► WelcomeScreen (capture name + email)
      │ (profile saved)
      ▼
!onboardingComplete ──────────► OnboardingFlow (3 steps, all skippable)
      │ (markComplete)
      ▼
else ─────────────────────────► FitnessApp (Home tab)
```

### Onboarding (`onboarding/OnboardingFlow.kt`) — 3 steps, each independently skippable
1. **Weekly split** — pick preset (PPL / Bro / Upper-Lower / Full-body 3-day) or build custom 7-day → `saveWeeklySplit`.
2. **Weight goal** — current kg (required to advance), target kg (optional), daily calorie goal (optional) → `saveBodyWeight` + `saveCalorieGoal`.
3. **Diet type** — Veg / Non-veg / Vegan / Eggetarian → `setDietPreference`. "Finish" → `markComplete` → Home.

↳ **Design notes**
- Every step skippable + no back button in onboarding → a user can land on Home with **zero** split, weight, or diet set. Downstream screens must all handle empty state (they do, but it's the least-guided path).
- Step 2 "current weight" is the only hard gate; everything else is soft. Consider whether diet type (step 3) should pre-select a default rather than allow null.
- Welcome captures email but app is local-only — email is cosmetic. Flag if it reads as "account/login" to users (false expectation).

---

## 1. Home — daily hub

**Entry:** default tab / startDestination. `home/HomeScreen.kt`

### 1a. Hero CTA (state machine) `HomeScreen.kt:436–462`
One primary button, label depends on state — **all four navigate to Log**:
| Condition | Label |
|---|---|
| `hasOpenSession` | Continue session |
| `plannedCount > 0` | Start planned workout |
| `todayTotals.sets > 0` | Resume workout |
| else | Start workout |

Secondary "**Plan today with AI**" link → Plan tab. Shown **only** when: no session AND no planned exercises AND nothing logged today.

↳ **Design notes**
- "Resume workout" (sets logged, session closed) vs "Continue session" (session open) is a subtle distinction — verify users read the difference.
- The AI link is the *only* in-Home discovery path to the plan generator, and it vanishes the moment anything is logged. New users who log one set lose that entry point.

### 1b. Daily check-in tiles `HomeScreen.kt:167–203`
- **Weight** tile → tap opens `BodyWeightSheet` (current + target kg, cutting/bulking/maintenance indicator).
- **Food** tile → tap opens `FoodQuickAddSheet`; **long-press → Diet tab**.
- **Water** tile → tap +1 glass; **long-press −1 glass** (no sheet).

↳ **Design notes**
- Three tiles, three *different* interaction models (sheet / sheet+longpress-nav / inline-longpress-adjust). Inconsistent — long-press means "navigate" on Food but "decrement" on Water. High mislearn risk.
- Water down = long-press has no visible affordance; discoverability near zero.

### 1c. Food quick-add sheet `home/DailySheets.kt:149–448`
Flow: suggestion pills (from AI day plan, tap-to-log) → name autocomplete (menu + plan + past logs, ≤6) → "Ask AI" fallback (estimates macros) → manual kcal/protein → **Add**. Any add that pushes over calorie goal → **confirm dialog** ("Add anyway" / "Cancel"). Logged-today list with per-row X delete. "Full diet log" → Diet tab.

↳ **Design notes**
- Rich sheet — arguably a full mini-Diet screen embedded in Home. Overlaps heavily with Diet tab logging. Decide the canonical logging surface; two doorways to the same action fragments the mental model.
- Over-goal confirm is the only blocking dialog in normal use — good friction, but only fires here, not on Diet-tab meal logging (inconsistent guard).

### 1d. Other Home affordances
- Weekly activity dots → tap → **Stats tab**. `:236`
- Bell icon → `ReminderSettingsSheet`. `:142`
- Avatar → **Profile**. `:150`

---

## 2. Plan — AI workout generator

**Entry:** Plan tab; or Profile → "Weekly split"/"Daily reminder" (deep-links here). `plan/PlanScreen.kt`

### 2a. Generate flow
1. Day picker (7-day strip) selects context; shows that day's focus from split, or "Rest day".
2. Inputs: **Goal** pills (Strength/Hypertrophy/Cardio/Mobility/Fat loss) · **Duration** (15/30/45/60) · **Equipment** with/without toggle · free-text **Notes**.
3. **Generate plan** → Gemini 2.5 Flash (fed: focus, goal, duration, equipment, notes, history summary, weight goal, nutrition context). Loading skeleton → result.
4. Result = parsed markdown sections (Warm-up / Main / Cool-down); Main exercises render as distinct rows (name + sets×reps).

### 2b. Post-generation `PlanScreen.kt:298–325`
- **Regenerate** (refresh icon) → same inputs, new plan.
- **Use this plan** → `applyPlan()`: parse exercises → resolve/create records → `stagePlan(ids)` writes `pending_plan` → `requestOpenSessionOnLog()` → navigate to **Log** (opens session view directly).

### 2c. Settings menu `PlanScreen.kt:357–380`
- **Weekly split** row → `SplitEditorSheet` (7 `FocusPicker` chip inputs, per-muscle colors, free-form add).
- **Daily reminder** row → `ReminderSettingsSheet`.

↳ **Design notes**
- Plan is input-heavy before any payoff — 4 input controls + generate. First-time users with no split see a "Build your week" CTA instead; two different first-runs to design for.
- "Use this plan" is a one-way commit to Log with no preview/edit-in-place of the parsed exercises. If the AI mis-parses an exercise name, the user finds out only in Log.
- Reminder + split are reachable from **both** Plan settings and Profile → two routes to the same sheets. Fine, but Profile versions *navigate to Plan first* — a visible tab jump that may feel like a glitch.

---

## 3. Log — workout logging

**Entry:** Log tab; or auto-opened from Plan "Use this plan" (skips grid → session view). `log/LogScreen.kt`

### 3a. Start paths
- **Manual:** muscle-group grid (6 cards + Back) → select groups → "Start workout (N groups)" → session.
- **From plan:** `openSessionOnLog` flag → `LaunchedEffect` flips straight to session view, consumes flag. `:107–113`
- **Resume:** open session within 4h window auto-loads. `LogViewModel:44,182`

### 3b. In-session
- **Log set** CTA → `SetSheet`: exercise picker (browse/create/edit) → kind-specific fields (reps+weight / min+sec / distance) → "Last: X reps · Y kg" hint → submit.
- Set rows grouped by exercise, muscle-color bar, **gold PR badge** if all-time best 1RM. Tap row = edit, long-press = delete.
- **Repeat last** pill (copies last set + starts rest). **Rest timer** banner (90s ring, Skip). PR → haptic + celebration overlay.
- **End session** pill → if 0 sets: silent discard; if sets: **save-template dialog** → then AI **critique sheet**.

↳ **Design notes**
- Two ways in (grid vs plan handoff) land in the same session view but with different mental context — the plan user never sees the grid, so "Change group" mid-session may confuse.
- Delete = long-press on a set row: destructive action on an undiscoverable gesture, no visible affordance. (Undo snackbar softens it.)
- End-session for an empty session silently deletes — correct, but no feedback; a user who tapped Start by accident gets no confirmation either way.

---

## 4. Diet — meals + AI day plan

**Entry:** Diet tab; Home Food tile long-press; Home food sheet "Full diet log". `diet/DietScreen.kt`

### 4a. Browse & log
- Diet-type chips filter visible meals (Veg sees veg+vegan+egg; Vegan sees vegan only; Non-veg sees all).
- Search pill filters by name. Category rows (Breakfast/Lunch/Dinner/Snack/High-protein), each a horizontal card scroll + "Suggest more" (AI, 3 meals/category).
- Meal card → `MealDetailSheet` (macros, ingredients, steps) → **"Add to today"** logs it. AI meals removable (undo snackbar).

### 4b. AI day plan
- **"Suggest meals" FAB** → `suggestDayPlan()`: 5 meals (mixed categories) summing ±100 kcal to goal, tuned by weight goal + calorie budget + training history + workout consistency. Persisted per-day (`cached_diet_plan`), shared to Home via repo StateFlow.
- Day-plan pills → `PlannedMealDetailSheet` (**read-only** reference — no log action).
- Calorie-goal cog → `CalorieGoalSheet` (target kcal + optional protein).
- Today's totals box: "Logged today: X / Y kcal · Z g protein".

↳ **Design notes**
- Two meal concepts coexist: **menu meals** (loggable) and **day-plan meals** (read-only pills). The day-plan pill detail sheet has no "Add to today" — a likely user expectation gap (they generated a plan; can't log from it directly here, only via Home's quick-add pills).
- "Suggest more" (per category, 3 meals) vs "Suggest meals" FAB (full day, 5 meals) are different AI actions with near-identical labels. Rename candidate.
- Diet-type filter is *subtractive* (Vegan hides most menu) — a vegan user sees a near-empty menu and must lean on AI. Intended? Worth confirming.

---

## 5. Stats — progress

**Entry:** Stats tab; Home weekly-dots tap. `stats/StatsScreen.kt`

- **Window chips** (7d/14d/30d/90d/All) drive every aggregate below.
- Big stat row: streak · sessions · sets. Volume card (weight moved / time / distance, non-zero rows only).
- **Muscle balance** stacked bar (animated), **Personal records** list (1RM, freshness), **1RM progress chart** (pick an exercise → traced line + delta), **weekly bars** (7-day), sessions list.

↳ **Design notes**
- Read-only tab — no export/share of a chart, no tap-through from a PR to its session/exercise. All insight, no next action.
- Empty windows (new user picks 90d) show mostly zero-state cards stacked — consider collapsing empties.

---

## 6. Profile / Settings / Data `profile/ProfileScreen.kt`

- **Edit** (name/email) · **Clear profile** (name/email reset, workouts kept, confirm dialog).
- Stats card (today's totals).
- **Weekly split** → nav to Plan + split editor. **Daily reminder** → nav to Plan + reminder sheet.
- **Theme** → dialog (System/Light/Dark, SharedPreferences, instant recompose).
- **Export** → SAF save → JSON of 21 tables (schemaVersion stamped).
- **Restore** → SAF pick → **destructive confirm** → wipe+reinsert in one transaction. Future-schema backup → friendly bail.

↳ **Design notes**
- Split + reminder settings live *physically in Plan* but are *listed in Profile* — tapping them causes a tab jump. Either move the sheets to be presentable from Profile, or drop the Profile duplicates.
- Theme is the only true "setting" that stays on Profile; split/reminder/data are scattered. Consider one Settings home.
- Restore is correctly gated (confirm + atomic). Export gives no "what's included" preview.

---

## Reminders / notifications (system) `reminder/`
`ReminderSettingsSheet` (time + toggle) → `saveReminder` → `ReminderScheduler` exact alarm (Android 12+ perm-aware, Doze-safe) → `ReminderReceiver` posts "Time to train · Today: [focus]" → reschedules next day. `BootReceiver` re-arms after reboot. Android 13+ prompts `POST_NOTIFICATIONS` on enable.

---

## Cross-screen handoffs (the connective tissue)

| From → To | Trigger | Carried via |
|---|---|---|
| Home hero → Log | tap CTA | in-memory session state |
| Home hero → Plan | "Plan today with AI" | nav only |
| Home Food → Diet | long-press tile / "Full diet log" | nav only |
| Plan → Log | "Use this plan" | `pending_plan` table + `openSessionOnLog` flag |
| Diet day plan → Home | generate | repo `dayPlan` StateFlow (shared singleton) |
| Profile → Plan | split / reminder rows | nav + open-sheet intent |
| Home dots / Stats | tap | nav only |

↳ **Biggest structural observations for optimization**
1. **Food logging has 2–3 doorways** (Home quick-add, Diet menu, Diet day-plan-via-Home) with inconsistent guards and no single canonical surface.
2. **Split & reminder settings have split ownership** (live in Plan, linked from Profile) causing visible tab jumps.
3. **Long-press carries load-bearing, invisible actions** (water −1, set delete, food→Diet) — a discoverability and consistency risk across the app.
4. **Read-only dead ends**: Stats (no next action), Diet day-plan pills (no log action) — places users expect to act but can't.
5. **The AI-plan discovery link on Home disappears after first logged set** — the main funnel into the generator is fragile for returning-but-idle users.
```
