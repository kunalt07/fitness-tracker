package com.example.fitness_tracker.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.data.DietType
import com.example.fitness_tracker.data.WeeklySplitDay
import com.example.fitness_tracker.diet.DietViewModel
import com.example.fitness_tracker.home.DailyViewModel
import com.example.fitness_tracker.plan.PlanViewModel
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillCta
import com.example.fitness_tracker.ui.QuietTextButton
import com.example.fitness_tracker.ui.SectionLabel

private enum class Step { Split, WeightGoal, Diet }

/**
 * First-launch onboarding. Three steps — pick a weekly-split preset, set
 * weight goal, choose diet — each individually skippable. The user's name
 * is captured upstream by [com.example.fitness_tracker.auth.WelcomeScreen]
 * before this flow runs.
 *
 * Calls [onFinish] when the user completes or skips through the last step;
 * the gate in MainActivity persists `onboardingComplete` and swaps to the
 * main app.
 */
@Composable
fun OnboardingFlow(
    onFinish: () -> Unit,
    planViewModel: PlanViewModel = viewModel(),
    dailyViewModel: DailyViewModel = viewModel(),
    dietViewModel: DietViewModel = viewModel(),
) {
    var step by rememberSaveable { mutableStateOf(Step.Split) }

    fun next() {
        step = when (step) {
            Step.Split -> Step.WeightGoal
            Step.WeightGoal -> Step.Diet
            Step.Diet -> { onFinish(); return }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            StepIndicator(step = step)

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(220)) { it / 6 * dir } + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(tween(220)) { -it / 6 * dir } + fadeOut(tween(220))
                        )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding-step",
            ) { current ->
                when (current) {
                    Step.Split -> SplitStep(
                        onPick = { preset ->
                            planViewModel.saveWeeklySplit(preset.toRows())
                            next()
                        },
                        onCustom = { rows ->
                            planViewModel.saveWeeklySplit(rows)
                            next()
                        },
                        onSkip = { next() },
                    )
                    Step.WeightGoal -> WeightGoalStep(
                        onSave = { current, target, calories ->
                            dailyViewModel.saveBodyWeight(current, target)
                            if (calories != null && calories > 0) {
                                dietViewModel.saveCalorieGoal(calories, null)
                            }
                            next()
                        },
                        onSkip = { next() },
                    )
                    Step.Diet -> DietStep(
                        onPick = { type ->
                            dietViewModel.setDietPreference(type)
                            next()
                        },
                        onSkip = { next() },
                    )
                }
            }
        }
    }
}

// --- Step indicator ----------------------------------------------------

@Composable
private fun StepIndicator(step: Step) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Step.entries.forEach { s ->
            val active = s.ordinal <= step.ordinal
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .weight(1f)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

// --- Step 1: weekly split preset ---------------------------------------

private enum class SplitPreset(val label: String, val days: List<String>, val tagline: String) {
    PPL(
        label = "Push / Pull / Legs",
        days = listOf("Push", "Pull", "Legs", "Push", "Pull", "Legs", "Rest"),
        tagline = "6 days a week, classic split",
    ),
    BRO(
        label = "Bro split",
        days = listOf("Chest", "Back", "Shoulders", "Arms", "Legs", "Rest", "Rest"),
        tagline = "5 days, one major group per day",
    ),
    UPPER_LOWER(
        label = "Upper / Lower",
        days = listOf("Upper", "Lower", "Rest", "Upper", "Lower", "Rest", "Rest"),
        tagline = "4 days a week, balanced",
    ),
    FULL_BODY(
        label = "Full body 3-day",
        days = listOf("Full body", "Rest", "Full body", "Rest", "Full body", "Rest", "Rest"),
        tagline = "3 days a week, time-efficient",
    );

    /**
     * Convert the 7-string focus list into [WeeklySplitDay] rows. Index 0
     * maps to Sunday (`dayOfWeek = 1`) per the existing convention.
     */
    fun toRows(): List<WeeklySplitDay> = days.mapIndexed { i, focus ->
        WeeklySplitDay(
            dayOfWeek = i + 1,
            focus = if (focus.equals("Rest", ignoreCase = true)) "" else focus,
        )
    }
}

private val DAY_FULL_ONB = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
)

// Quick-pick focuses for the custom split editor — split styles + muscle groups.
private val SPLIT_FOCUS_SUGGESTIONS = listOf(
    "Push", "Pull", "Legs", "Upper", "Lower", "Full body",
    "Chest", "Back", "Shoulders", "Arms", "Biceps", "Triceps",
    "Quads", "Hamstrings", "Glutes", "Calves", "Core", "Cardio", "Mobility", "Rest",
)

@Composable
private fun SplitStep(
    onPick: (SplitPreset) -> Unit,
    onCustom: (List<WeeklySplitDay>) -> Unit,
    onSkip: () -> Unit,
) {
    var customizing by rememberSaveable { mutableStateOf(false) }
    var focuses by rememberSaveable { mutableStateOf(List(7) { "" }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (!customizing) {
            Title("Pick a weekly split")
            Subtitle("Choose a preset or build your own. Fine-tune any day later in the Plan tab.")

            Spacer(modifier = Modifier.height(24.dp))

            SplitPreset.entries.forEach { preset ->
                PresetCard(preset = preset, onClick = { onPick(preset) })
                Spacer(modifier = Modifier.height(12.dp))
            }

            CustomSplitCard(onClick = { customizing = true })

            Spacer(modifier = Modifier.height(8.dp))

            SkipRow(label = "Skip for now", onSkip = onSkip)
        } else {
            Title("Build your week")
            Subtitle("Add what you train each day — pick more than one. Leave a day blank for rest.")

            Spacer(modifier = Modifier.height(24.dp))

            DAY_FULL_ONB.forEachIndexed { i, day ->
                com.example.fitness_tracker.plan.ChipPicker(
                    serialized = focuses[i],
                    onChange = { v -> focuses = focuses.toMutableList().also { it[i] = v } },
                    suggestions = SPLIT_FOCUS_SUGGESTIONS,
                    sectionLabel = day,
                    label = "Add focus",
                    placeholder = "Type or pick — e.g. Push, Chest…",
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            PillCta(
                label = "Save split",
                onClick = {
                    val rows = focuses.mapIndexed { i, f ->
                        val focus = f.trim().let { if (it.equals("Rest", ignoreCase = true)) "" else it }
                        WeeklySplitDay(dayOfWeek = i + 1, focus = focus)
                    }
                    onCustom(rows)
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            SkipRow(label = "Back to presets", onSkip = { customizing = false })
        }
    }
}

@Composable
private fun CustomSplitCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            Text(
                text = "Build my own",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Set each day's focus yourself",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun PresetCard(preset: SplitPreset, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            Text(
                text = preset.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = preset.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Mini week strip — Sun…Sat dots colored by whether the day trains.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val labels = listOf("S", "M", "T", "W", "T", "F", "S")
                preset.days.forEachIndexed { i, focus ->
                    val isRest = focus.equals("Rest", ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(width = 22.dp, height = 22.dp)
                            .background(
                                color = if (isRest)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = labels[i],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isRest)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

// --- Step 2: weight goal -----------------------------------------------

@Composable
private fun WeightGoalStep(
    onSave: (currentKg: Double, targetKg: Double?, calorieGoal: Int?) -> Unit,
    onSkip: () -> Unit,
) {
    var current by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }

    val currentKg = current.trim().toDoubleOrNull()
    val canContinue = currentKg != null && currentKg > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Title("What's your weight?")
        Subtitle("Used to track progress and tailor calorie suggestions. Both fields are optional.")

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel("Current weight (kg)")
        Spacer(modifier = Modifier.height(6.dp))
        MinimalTextField(
            value = current,
            onValueChange = { current = it.take(6).filter { c -> c.isDigit() || c == '.' } },
            label = "Today",
            placeholder = "e.g. 72.5",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Target weight (kg)")
        Spacer(modifier = Modifier.height(6.dp))
        MinimalTextField(
            value = target,
            onValueChange = { target = it.take(6).filter { c -> c.isDigit() || c == '.' } },
            label = "Goal",
            placeholder = "Optional",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Daily calorie goal (kcal)")
        Spacer(modifier = Modifier.height(6.dp))
        MinimalTextField(
            value = calories,
            onValueChange = { calories = it.take(5).filter { c -> c.isDigit() } },
            label = "Calories",
            placeholder = "Optional · e.g. 2200",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Spacer(modifier = Modifier.height(28.dp))

        PillCta(
            label = "Continue",
            enabled = canContinue,
            onClick = {
                val cur = currentKg ?: return@PillCta
                onSave(cur, target.trim().toDoubleOrNull(), calories.trim().toIntOrNull())
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SkipRow(label = "Skip for now", onSkip = onSkip)
    }
}

// --- Step 3: diet preference -------------------------------------------

@Composable
private fun DietStep(
    onPick: (DietType) -> Unit,
    onSkip: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<DietType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Title("How do you eat?")
        Subtitle("Diet suggestions and meal nudges will respect this choice.")

        Spacer(modifier = Modifier.height(24.dp))

        DietType.entries.forEach { type ->
            DietRow(
                type = type,
                selected = selected == type,
                onClick = { selected = type },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        PillCta(
            label = "Finish",
            enabled = selected != null,
            onClick = { selected?.let(onPick) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SkipRow(label = "Skip for now", onSkip = onSkip)
    }
}

@Composable
private fun DietRow(
    type: DietType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = dietAccent(type)
    val borderColor = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val borderWidth = if (selected) 2.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = accent, shape = RoundedCornerShape(50)),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = type.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private val DietType.title: String
    get() = when (this) {
        DietType.VEG -> "Vegetarian"
        DietType.NON_VEG -> "Non-vegetarian"
        DietType.VEGAN -> "Vegan"
        DietType.EGGETARIAN -> "Eggetarian"
    }

private val DietType.subtitle: String
    get() = when (this) {
        DietType.VEG -> "No meat, fish, or poultry"
        DietType.NON_VEG -> "Anything goes"
        DietType.VEGAN -> "No animal products"
        DietType.EGGETARIAN -> "Vegetarian + eggs"
    }

@Composable
private fun dietAccent(type: DietType): Color = when (type) {
    DietType.VEG -> Color(0xFF1F8A4C)
    DietType.NON_VEG -> Color(0xFFD04A3A)
    DietType.VEGAN -> Color(0xFF14A37F)
    DietType.EGGETARIAN -> Color(0xFFE0A82E)
}

// --- Shared bits -------------------------------------------------------

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Light,
    )
}

@Composable
private fun Subtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SkipRow(label: String, onSkip: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        QuietTextButton(label = label, onClick = onSkip)
    }
}
