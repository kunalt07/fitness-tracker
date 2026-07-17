package com.example.fitness_tracker.diet

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitness_tracker.data.CalorieGoal
import com.example.fitness_tracker.data.DietPreference
import com.example.fitness_tracker.data.DietType
import com.example.fitness_tracker.data.FitnessRepository
import com.example.fitness_tracker.data.FoodEntry
import com.example.fitness_tracker.data.MacroTotals
import com.example.fitness_tracker.data.Meal
import com.example.fitness_tracker.data.MealCategory
import com.example.fitness_tracker.data.PlannedMeal
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DietViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = FitnessRepository.get(app)

    private val allMeals: StateFlow<List<Meal>> =
        repo.meals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dietPreference: StateFlow<DietPreference?> =
        repo.dietPreference.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val totalsToday: StateFlow<MacroTotals> =
        repo.observeMacroTotalsForDay()
            .stateIn(viewModelScope, SharingStarted.Eagerly, MacroTotals(0, 0, 0, 0))

    /** Food entries logged today — for the Home food sheet's removable list. */
    val foodsToday: StateFlow<List<FoodEntry>> =
        repo.observeFoodsForDay()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun deleteFoodEntry(id: Long) {
        viewModelScope.launch { repo.deleteFood(id) }
    }

    /** A food name + macros offered as autocomplete in the quick-add sheet. */
    data class FoodSuggestion(val name: String, val calories: Int, val proteinG: Int)

    /**
     * Merged local autocomplete pool: previously-logged foods first (most relevant),
     * then today's day-plan meals, then the diet menu. Deduped by name (case-insensitive).
     */
    val foodSuggestions: StateFlow<List<FoodSuggestion>> =
        kotlinx.coroutines.flow.combine(
            allMeals,
            repo.dayPlan,
            repo.recentDistinctFoods,
        ) { meals, plan, recent ->
            val out = LinkedHashMap<String, FoodSuggestion>()
            fun add(name: String, calories: Int, proteinG: Int) {
                val key = name.trim().lowercase()
                if (key.isNotEmpty() && calories > 0 && key !in out) {
                    out[key] = FoodSuggestion(name.trim(), calories, proteinG)
                }
            }
            recent.forEach { add(it.name, it.calories, it.proteinG) }
            plan.forEach { add(it.name, it.calories, it.proteinG) }
            meals.forEach { add(it.name, it.calories, it.proteinG) }
            out.values.toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _aiEstimating = MutableStateFlow(false)
    val aiEstimating: StateFlow<Boolean> = _aiEstimating.asStateFlow()

    /**
     * AI fallback for the food autocomplete: estimate calories + protein for any
     * typed food name (single serving) and hand them back via [onResult].
     */
    fun estimateFood(query: String, onResult: (name: String, calories: Int, proteinG: Int) -> Unit) {
        val q = query.trim()
        if (q.isBlank() || _aiEstimating.value) return
        _aiEstimating.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = "Estimate calories and protein for a typical single serving of " +
                    "\"$q\". Reply with JSON only, no markdown: " +
                    """{ "name": "<cleaned food name>", "calories": <int>, "protein_g": <int> }"""
                val resp = generativeModel.generateContent(content { text(prompt) })
                val o = JSONObject(stripCodeFences(resp.text?.trim().orEmpty()))
                val name = o.optString("name").trim().ifBlank { q }
                val calories = o.optInt("calories", 0).coerceAtLeast(0)
                val proteinG = o.optInt("protein_g", 0).coerceAtLeast(0)
                if (calories > 0) {
                    withContext(Dispatchers.Main) { onResult(name, calories, proteinG) }
                }
            } catch (e: Exception) {
                Log.e("DietViewModel", "estimateFood failed", e)
            } finally {
                _aiEstimating.value = false
            }
        }
    }

    val calorieGoal: StateFlow<CalorieGoal?> =
        repo.calorieGoal.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Meals filtered by the user's selected diet (or all when no preference set). */
    val visibleMeals: StateFlow<List<Meal>> = run {
        kotlinx.coroutines.flow.combine(allMeals, dietPreference) { meals, pref ->
            val type = pref?.type ?: DietType.NON_VEG
            // Non-veg sees everything. Veg sees veg + vegan + eggetarian. Vegan sees vegan only.
            // Eggetarian sees veg + eggetarian + vegan.
            meals.filter { meal ->
                val tags = meal.dietTypes.split(",").mapNotNull { name ->
                    runCatching { DietType.valueOf(name) }.getOrNull()
                }.toSet()
                when (type) {
                    DietType.NON_VEG -> true
                    DietType.VEG -> DietType.VEG in tags || DietType.VEGAN in tags ||
                        DietType.EGGETARIAN in tags
                    DietType.VEGAN -> DietType.VEGAN in tags
                    DietType.EGGETARIAN -> DietType.VEG in tags || DietType.VEGAN in tags ||
                        DietType.EGGETARIAN in tags
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    private val _suggestState = MutableStateFlow<SuggestState>(SuggestState.Idle)
    val suggestState: StateFlow<SuggestState> = _suggestState.asStateFlow()

    sealed interface SuggestState {
        data object Idle : SuggestState
        data class Loading(val category: MealCategory) : SuggestState
        data class Error(val message: String) : SuggestState
    }

    // --- Daily "what to eat" plan: goal- & training-aware. The meals live in the
    // repository (shared with Home, persisted per day); the VM only owns the
    // generation status that drives the Diet-tab section's spinner/error. ---
    val dayPlan: StateFlow<List<PlannedMeal>> = repo.dayPlan
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _planStatus = MutableStateFlow<PlanStatus>(PlanStatus.Idle)
    val planStatus: StateFlow<PlanStatus> = _planStatus.asStateFlow()

    sealed interface PlanStatus {
        data object Idle : PlanStatus
        data object Loading : PlanStatus
        data class Error(val message: String) : PlanStatus
    }

    private val generativeModel = Firebase
        .ai(backend = GenerativeBackend.googleAI())
        .generativeModel(modelName = "gemini-2.5-flash")

    init {
        viewModelScope.launch {
            repo.seedDefaultMealsIfEmpty()
        }
        // Hydrate today's persisted day plan into the shared repo flow (also makes
        // it available on Home, which shares this singleton repository).
        viewModelScope.launch { repo.loadDayPlan() }
    }

    fun setDietPreference(type: DietType) {
        viewModelScope.launch { repo.saveDietPreference(type) }
    }

    /**
     * Asks Gemini for [n] new meal cards in [category] respecting the current diet
     * preference and inserts them as AI-generated meals. Idempotent: failures don't
     * leave the menu in a half-state.
     */
    fun suggestMore(category: MealCategory, n: Int = 3) {
        if (_suggestState.value is SuggestState.Loading) return
        _suggestState.value = SuggestState.Loading(category)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pref = repo.getDietPreference()?.type ?: DietType.NON_VEG
                val existingNames = allMeals.value
                    .filter { it.category == category }
                    .map { it.name }
                // Goal-/training-aware context the app already has: weight goal,
                // today's nutrition budget, and 14-day training tallies. Each is
                // nullable — a fresh user with no data gets clean generic meals.
                val weightGoal = repo.weightGoalContext()
                val nutrition = repo.nutritionContext()
                val training = repo.summarizeRecentHistory()
                val prompt = buildPrompt(
                    category, pref, n, existingNames,
                    weightGoal = weightGoal,
                    nutrition = nutrition,
                    training = training,
                )
                val resp = generativeModel.generateContent(content { text(prompt) })
                val raw = resp.text?.trim().orEmpty()
                val parsed = parseMealsJson(raw, category, pref)
                if (parsed.isEmpty()) {
                    _suggestState.value = SuggestState.Error("Couldn't parse new meals.")
                    return@launch
                }
                parsed.forEach { repo.addMeal(it) }
                _suggestState.value = SuggestState.Idle
            } catch (e: Exception) {
                Log.e("DietViewModel", "suggestMore failed", e)
                _suggestState.value = SuggestState.Error(
                    com.example.fitness_tracker.ai.friendlyAiError(e),
                )
            }
        }
    }

    private fun buildPrompt(
        category: MealCategory,
        diet: DietType,
        n: Int,
        existingNames: List<String>,
        weightGoal: String? = null,
        nutrition: String? = null,
        training: String? = null,
    ): String = buildString {
        appendLine(
            "You are a Registered Dietitian. Suggest $n meal ideas for the " +
                "'${category.label}' category that fit a '${diet.label}' diet.",
        )
        appendLine()

        // Only emit the goal-aware blocks when we actually have a signal, so a
        // fresh user (no goal / no logs / no training) still gets clean generic
        // suggestions instead of empty scaffolding.
        val signals = listOfNotNull(weightGoal, nutrition, training)
        if (signals.isNotEmpty()) {
            appendLine("USER CONTEXT — tailor the meals to this:")
            signals.forEach { appendLine("- $it") }
            appendLine()
            appendLine("OPTIMIZE FOR THE GOAL:")
            if (weightGoal != null) {
                appendLine("- Bias macros toward the weight goal: if bulking, higher protein with a modest calorie surplus; if cutting, higher protein with a modest deficit.")
            }
            if (training != null) {
                appendLine("- Favor protein and carbs that support recovery for the most-recently-trained muscle groups shown above.")
            }
            if (nutrition != null) {
                appendLine("- Size portions to fill the REMAINING calorie/protein budget for today, not exceed it.")
            }
            appendLine()
        }

        appendLine("RULES:")
        appendLine("- Different from the user's current list. Avoid these names: ${existingNames.joinToString(", ")}")
        appendLine("- Realistic portions, common ingredients.")
        appendLine("- Respect the diet strictly. ${diet.strictnessHint()}")
        appendLine("- Each meal should be balanced (protein + carbs + fat) and easy to make.")
        appendLine()
        appendLine("OUTPUT FORMAT — JSON only, no markdown:")
        appendLine(
            """{ "meals": [
              {
                "name": "<short name>",
                "description": "<one-line summary>",
                "ingredients": ["item1 with qty", "item2 with qty", "..."],
                "steps": ["step 1", "step 2", "..."],
                "calories": <int>,
                "protein_g": <int>,
                "carbs_g": <int>,
                "fat_g": <int>
              }
            ] }""".trimIndent(),
        )
        appendLine("Use 3-5 ingredients and 3-5 steps per meal. Whole numbers for macros.")
    }

    private fun parseMealsJson(raw: String, category: MealCategory, diet: DietType): List<Meal> {
        val cleaned = stripCodeFences(raw)
        val root = runCatching { JSONObject(cleaned) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("meals") ?: return emptyList()
        val out = mutableListOf<Meal>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name").trim().ifBlank { continue }
            val ingredients = o.optJSONArray("ingredients")?.let { ja ->
                (0 until ja.length()).map { ja.optString(it).trim() }
                    .filter { it.isNotBlank() }
            }.orEmpty()
            val steps = o.optJSONArray("steps")?.let { ja ->
                (0 until ja.length()).map { ja.optString(it).trim() }
                    .filter { it.isNotBlank() }
            }.orEmpty()
            if (ingredients.isEmpty() || steps.isEmpty()) continue
            out += Meal(
                name = name,
                description = o.optString("description").trim(),
                ingredients = ingredients.joinToString("\n"),
                steps = steps.joinToString("\n"),
                calories = o.optInt("calories", 0).coerceAtLeast(0),
                proteinG = o.optInt("protein_g", 0).coerceAtLeast(0),
                carbsG = o.optInt("carbs_g", 0).coerceAtLeast(0),
                fatG = o.optInt("fat_g", 0).coerceAtLeast(0),
                category = category,
                dietTypes = diet.dietTags(),
                isAiGenerated = true,
                createdAt = System.currentTimeMillis(),
            )
        }
        return out
    }

    /**
     * One-shot daily meal plan tailored to diet mix, weight goal, calorie budget, and
     * training. Persisted via [FitnessRepository.saveDayPlan] so it shows on Home too.
     */
    fun suggestDayPlan() {
        if (_planStatus.value is PlanStatus.Loading) return
        _planStatus.value = PlanStatus.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val weightGoal = repo.weightGoalContext()
                val budget = repo.calorieBudgetContext()
                val training = repo.summarizeRecentHistory()
                val consistency = repo.workoutConsistencyContext()
                val prompt = buildDayPlanPrompt(weightGoal, budget, training, consistency)
                val resp = generativeModel.generateContent(content { text(prompt) })
                val meals = parseDayPlanJson(resp.text?.trim().orEmpty())
                if (meals.isEmpty()) {
                    _planStatus.value = PlanStatus.Error("Couldn't build a day plan. Try again.")
                } else {
                    repo.saveDayPlan(meals)
                    _planStatus.value = PlanStatus.Idle
                }
            } catch (e: Exception) {
                Log.e("DietViewModel", "suggestDayPlan failed", e)
                _planStatus.value = PlanStatus.Error(
                    com.example.fitness_tracker.ai.friendlyAiError(e),
                )
            }
        }
    }

    private fun buildDayPlanPrompt(
        weightGoal: String?,
        budget: String?,
        training: String?,
        consistency: String?,
    ): String = buildString {
        appendLine(
            "You are a Registered Dietitian. Suggest 5 meals that together span a MIX of " +
                "dietary types — include Vegetarian, Non-vegetarian, Vegan, and Eggetarian " +
                "options across the five (do NOT make them all one dietary type). Choose the " +
                "category for each — Breakfast, Lunch, Dinner, Snack, or High-protein — freely " +
                "to best fit the user's weight goal and how consistently they train.",
        )
        appendLine()
        // Same nullable-signal pattern as suggestMore: emit goal blocks only when we
        // have something, so a fresh user still gets a clean generic set of ideas.
        val signals = listOfNotNull(weightGoal, budget, training, consistency)
        if (signals.isNotEmpty()) {
            appendLine("USER CONTEXT — tailor the meals to this:")
            signals.forEach { appendLine("- $it") }
            appendLine()
            appendLine("OPTIMIZE FOR THE GOAL:")
            if (budget != null) appendLine("- IMPORTANT: the 5 meals' calories must ADD UP to roughly the daily calorie goal shown above — if some calories are already logged, sum to the REMAINING budget instead. Aim within ±100 kcal of that total; don't fall well short or overshoot.")
            if (weightGoal != null) appendLine("- Bias macros toward the weight goal: bulking → higher protein with a modest surplus; cutting → higher protein with a modest deficit.")
            if (consistency != null) appendLine("- Match the training consistency: if the user trains often, lean toward High-protein and calorie-dense options for recovery and growth; if they train little, favor lighter, leaner meals.")
            if (training != null) appendLine("- Favor protein and carbs that support recovery for the most-recently-trained muscle groups shown above.")
            appendLine()
        }
        appendLine("RULES:")
        appendLine("- Realistic portions, common ingredients. Keep each description to one line.")
        appendLine("- Tag each meal with the dietary type it actually is (Vegetarian / Non-vegetarian / Vegan / Eggetarian).")
        appendLine("- Vary both dietary type and category across the 5 meals.")
        appendLine()
        appendLine("OUTPUT FORMAT — JSON only, no markdown:")
        appendLine(
            """{ "meals": [
              {
                "category": "<Breakfast|Lunch|Dinner|Snack|High-protein>",
                "diet_type": "<Vegetarian|Non-vegetarian|Vegan|Eggetarian>",
                "name": "<short name>",
                "description": "<one-line>",
                "calories": <int>,
                "protein_g": <int>,
                "ingredients": ["item1 with qty", "item2 with qty", "..."],
                "steps": ["step 1", "step 2", "..."]
              }
            ] }""".trimIndent(),
        )
        appendLine("Return exactly 5 meals covering a mix of dietary types. Use 3-5 ingredients and 3-5 steps per meal. Whole numbers for macros.")
    }

    private fun parseDayPlanJson(raw: String): List<PlannedMeal> {
        val cleaned = stripCodeFences(raw)
        val root = runCatching { JSONObject(cleaned) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("meals") ?: return emptyList()
        val out = mutableListOf<PlannedMeal>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name").trim().ifBlank { continue }
            val ingredients = o.optJSONArray("ingredients")?.let { ja ->
                (0 until ja.length()).map { ja.optString(it).trim() }.filter { it.isNotBlank() }
            }.orEmpty()
            val steps = o.optJSONArray("steps")?.let { ja ->
                (0 until ja.length()).map { ja.optString(it).trim() }.filter { it.isNotBlank() }
            }.orEmpty()
            out += PlannedMeal(
                category = o.optString("category").trim().ifBlank { "Meal" },
                dietType = o.optString("diet_type").trim(),
                name = name,
                description = o.optString("description").trim(),
                calories = o.optInt("calories", 0).coerceAtLeast(0),
                proteinG = o.optInt("protein_g", 0).coerceAtLeast(0),
                ingredients = ingredients,
                steps = steps,
            )
        }
        return out
    }

    private fun stripCodeFences(s: String): String {
        val t = s.trim()
        if (!t.startsWith("```")) return t
        return t.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    }

    /** Quick single-item food log from the Home tile (macros optional). */
    fun quickAddFood(name: String, calories: Int, proteinG: Int) {
        if (name.isBlank() || calories <= 0) return
        viewModelScope.launch {
            repo.addFood(
                name = name.trim(),
                calories = calories,
                proteinG = proteinG,
                carbsG = 0,
                fatG = 0,
            )
        }
    }

    /** Log this meal into today's food entries. */
    fun logMeal(meal: Meal) {
        viewModelScope.launch {
            repo.addFood(
                name = meal.name,
                calories = meal.calories,
                proteinG = meal.proteinG,
                carbsG = meal.carbsG,
                fatG = meal.fatG,
            )
        }
    }

    suspend fun deleteMealWithUndo(id: Long): (suspend () -> Unit)? {
        val snapshot = repo.getMeal(id) ?: return null
        repo.deleteMeal(id)
        return { repo.addMeal(snapshot.copy(id = 0)) }
    }

    fun saveCalorieGoal(targetCalories: Int, proteinTargetG: Int?) {
        if (targetCalories < 500 || targetCalories > 6000) return
        viewModelScope.launch { repo.saveCalorieGoal(targetCalories, proteinTargetG) }
    }
}

private val DietType.label: String
    get() = when (this) {
        DietType.VEG -> "Vegetarian"
        DietType.NON_VEG -> "Non-vegetarian"
        DietType.VEGAN -> "Vegan"
        DietType.EGGETARIAN -> "Eggetarian"
    }

private fun DietType.strictnessHint(): String = when (this) {
    DietType.VEG -> "No meat, fish, or poultry. Dairy and eggs are NOT both required — vegetarian meals can include dairy/eggs but don't have to."
    DietType.NON_VEG -> "Anything goes."
    DietType.VEGAN -> "No animal products at all: no meat, fish, dairy, eggs, honey."
    DietType.EGGETARIAN -> "No meat, fish, or poultry. Eggs and dairy allowed."
}

private fun DietType.dietTags(): String = when (this) {
    DietType.VEG -> "VEG"
    DietType.NON_VEG -> "NON_VEG"
    DietType.VEGAN -> "VEGAN,VEG"
    DietType.EGGETARIAN -> "EGGETARIAN,VEG"
}

private val MealCategory.label: String
    get() = when (this) {
        MealCategory.BREAKFAST -> "Breakfast"
        MealCategory.LUNCH -> "Lunch"
        MealCategory.DINNER -> "Dinner"
        MealCategory.SNACK -> "Snack"
        MealCategory.HIGH_PROTEIN -> "High-protein"
    }
