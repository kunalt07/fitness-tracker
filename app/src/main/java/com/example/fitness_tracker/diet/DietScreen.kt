package com.example.fitness_tracker.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import com.example.fitness_tracker.ui.rememberFullSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.LocalSnackbarHost
import com.example.fitness_tracker.data.DietType
import com.example.fitness_tracker.data.Meal
import com.example.fitness_tracker.data.MealCategory
import com.example.fitness_tracker.data.PlannedMeal
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillChip
import com.example.fitness_tracker.ui.PillCta
import com.example.fitness_tracker.ui.QuietTextButton
import com.example.fitness_tracker.ui.ScreenTitle
import com.example.fitness_tracker.ui.SectionLabel
import kotlinx.coroutines.launch

@Composable
internal fun DietScreen(
    contentPadding: PaddingValues,
    viewModel: DietViewModel = viewModel(),
) {
    val pref by viewModel.dietPreference.collectAsState()
    val meals by viewModel.visibleMeals.collectAsState()
    val suggest by viewModel.suggestState.collectAsState()
    val dayPlan by viewModel.dayPlan.collectAsState()
    val planStatus by viewModel.planStatus.collectAsState()
    val totals by viewModel.totalsToday.collectAsState()
    val goal by viewModel.calorieGoal.collectAsState()
    val snackbarHost = LocalSnackbarHost.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val topInset = contentPadding.calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()

    var showGoalSheet by rememberSaveable { mutableStateOf(false) }
    var openMealId by rememberSaveable { mutableStateOf<Long?>(null) }
    val openMeal = remember(meals, openMealId) { meals.firstOrNull { it.id == openMealId } }
    // Tapped AI day-plan meal (hold the object; it has no id).
    var openPlanned by remember { mutableStateOf<PlannedMeal?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topInset)
            .padding(bottom = bottomInset),
    ) {
        // Title row + cog. "Diet Plan" sits up top; "Menu" is a smaller-but-same-
        // family subheading directly underneath.
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                ) {
                    Text(
                        text = "Diet Plan",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Menu",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            IconButton(onClick = { showGoalSheet = true }) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Calorie goal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Diet type picker — each option carries a recognisable color:
        //   Veg = green dot, Non-veg = red dot, Vegan = leaf green, Eggetarian = amber.
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(DietType.entries.toList()) { type ->
                DietTypeChip(
                    type = type,
                    selected = pref?.type == type,
                    onClick = { viewModel.setDietPreference(type) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Today's calories summary (compact)
        if (totals.calories > 0 || goal != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = buildString {
                        append("Logged today: ${totals.calories}")
                        goal?.let { append(" / ${it.targetCalories}") }
                        append(" kcal")
                        if (totals.protein > 0) append("  ·  ${totals.protein}g protein")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Page-wide accent follows whichever diet type is selected. Veg → green,
        // Non-veg → red, Vegan → emerald, Eggetarian → amber. Falls back to the
        // default Diet accent (amber) before any preference is chosen.
        val pageAccent = activeDietColors(pref)

        // AI "what to eat" for today — sits above the browsable Menu. Only shows
        // once the user taps the floating Suggest button (Idle renders nothing).
        DaySuggestSection(
            meals = dayPlan,
            status = planStatus,
            accent = pageAccent,
            onOpen = { openPlanned = it },
        )

        // One row per category
        MealCategory.entries.forEach { category ->
            val rowMeals = meals.filter { it.category == category }
            if (rowMeals.isEmpty() && suggest !is DietViewModel.SuggestState.Loading) return@forEach

            CategoryHeader(
                category = category,
                isLoading = (suggest as? DietViewModel.SuggestState.Loading)?.category == category,
                onSuggestMore = { viewModel.suggestMore(category) },
                accent = pageAccent,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rowMeals, key = { it.id }) { m ->
                    MealCard(meal = m, accent = pageAccent, onTap = { openMealId = m.id })
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Error banner if a suggest call failed
        (suggest as? DietViewModel.SuggestState.Error)?.let { err ->
            Text(
                text = err.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

        // Floating "Suggest meals" button — bottom-right, clears the nav bar.
        DaySuggestFab(
            loading = planStatus is DietViewModel.PlanStatus.Loading,
            accent = activeDietColors(pref),
            onClick = { viewModel.suggestDayPlan() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = bottomInset + 16.dp),
        )
    }

    // Detail sheet
    if (openMeal != null) {
        MealDetailSheet(
            meal = openMeal,
            onLog = {
                viewModel.logMeal(openMeal)
                openMealId = null
            },
            onDismiss = { openMealId = null },
            onDelete = if (openMeal.isAiGenerated) {
                {
                    val deleting = openMeal
                    openMealId = null
                    scope.launch {
                        val restore = viewModel.deleteMealWithUndo(deleting.id)
                        if (restore != null) {
                            com.example.fitness_tracker.postUndoSnackbar(
                                host = snackbarHost,
                                scope = scope,
                                message = "Meal removed",
                                restore = restore,
                            )
                        }
                    }
                }
            } else null,
        )
    }

    openPlanned?.let { planned ->
        PlannedMealDetailSheet(meal = planned, onDismiss = { openPlanned = null })
    }

    if (showGoalSheet) {
        CalorieGoalSheet(
            initialCalories = goal?.targetCalories,
            initialProtein = goal?.proteinTargetG,
            onSave = { kcal, p ->
                viewModel.saveCalorieGoal(kcal, p)
                showGoalSheet = false
            },
            onDismiss = { showGoalSheet = false },
        )
    }
}

/**
 * "Suggested meals" section — the AI day plan (Breakfast/Lunch/Dinner/Snack) shown
 * as pills at the top of Diet Plan. Renders nothing until the user triggers it.
 */
@Composable
private fun DaySuggestSection(
    meals: List<PlannedMeal>,
    status: DietViewModel.PlanStatus,
    accent: DietTypeColors,
    onOpen: (PlannedMeal) -> Unit,
) {
    val loading = status is DietViewModel.PlanStatus.Loading
    val error = status as? DietViewModel.PlanStatus.Error
    // Nothing generated, not loading, no error → render nothing.
    if (meals.isEmpty() && !loading && error == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        SectionLabel("SUGGESTED MEALS")
        Spacer(modifier = Modifier.height(10.dp))
        when {
            loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accent.main,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Planning your day…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            error != null -> {
                Text(
                    error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    meals.forEach { PlannedMealPill(meal = it, onClick = { onOpen(it) }) }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Total: ${meals.sumOf { it.calories }} kcal · " +
                        "${meals.sumOf { it.proteinG }}g protein",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun PlannedMealPill(
    meal: PlannedMeal,
    onClick: () -> Unit,
) {
    // Minimal row: diet-color dot + name, macros on the right. Tap for prep.
    val dietType = dietTypeFromLabel(meal.dietType)
    val dotColor = dietType?.let { dietTypeColors(it).main }
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            meal.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (meal.calories > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "${meal.calories} kcal · ${meal.proteinG}g",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannedMealDetailSheet(
    meal: PlannedMeal,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberFullSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = buildString {
                    append(meal.category)
                    if (meal.dietType.isNotBlank()) append(" · ${meal.dietType}")
                }.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = meal.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (meal.description.isNotBlank()) {
                Text(
                    text = meal.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                MacroStat("${meal.calories}", "kcal")
                MacroStat("${meal.proteinG}g", "protein")
            }

            if (meal.ingredients.isNotEmpty()) {
                SectionLabel("Ingredients")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    meal.ingredients.forEach { line ->
                        Text(
                            text = "·  $line",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            if (meal.steps.isNotEmpty()) {
                SectionLabel("Steps")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    meal.steps.forEachIndexed { i, line ->
                        Text(
                            text = "${i + 1}.  $line",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            QuietTextButton(label = "Close", onClick = onDismiss)
        }
    }
}

/** Best-effort map an AI diet-type label back to [DietType] for coloring. */
private fun dietTypeFromLabel(s: String): DietType? {
    val t = s.lowercase()
    return when {
        "vegan" in t -> DietType.VEGAN
        "egg" in t -> DietType.EGGETARIAN
        "non" in t -> DietType.NON_VEG
        "veg" in t -> DietType.VEG
        else -> null
    }
}

@Composable
private fun DaySuggestFab(
    loading: Boolean,
    accent: DietTypeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accent.main)
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = androidx.compose.ui.graphics.Color.White,
            )
        } else {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            if (loading) "Planning…" else "Suggest meals",
            style = MaterialTheme.typography.labelLarge,
            color = androidx.compose.ui.graphics.Color.White,
        )
    }
}

/**
 * Color tokens for each [DietType]. Choices follow the Indian veg/non-veg dot
 * convention so the affordance reads instantly.
 *
 *   main        — solid accent (selected chip fill, kcal text, AI sparkle)
 *   container   — translucent tint of main (Suggest-more pill background)
 *   onContainer — ink for content sitting on `container`
 */
private data class DietTypeColors(
    val main: androidx.compose.ui.graphics.Color,
    val container: androidx.compose.ui.graphics.Color,
    val onContainer: androidx.compose.ui.graphics.Color,
)

@Composable
private fun dietTypeColors(type: DietType): DietTypeColors {
    val main = when (type) {
        DietType.VEG        -> androidx.compose.ui.graphics.Color(0xFF1F8A4C) // green
        DietType.NON_VEG    -> androidx.compose.ui.graphics.Color(0xFFD04A3A) // red
        DietType.VEGAN      -> androidx.compose.ui.graphics.Color(0xFF14A37F) // leaf / emerald
        DietType.EGGETARIAN -> androidx.compose.ui.graphics.Color(0xFFE0A82E) // egg yolk amber
    }
    return DietTypeColors(
        main = main,
        container = main.copy(alpha = 0.16f),
        onContainer = main,
    )
}

/**
 * The "current" diet color for screen-wide accents (CategoryHeader,
 * MealCard kcal text, AI sparkle, etc.). Falls back to the Diet feature
 * accent (amber) when no preference has been chosen yet.
 */
@Composable
private fun activeDietColors(pref: com.example.fitness_tracker.data.DietPreference?): DietTypeColors {
    if (pref != null) return dietTypeColors(pref.type)
    val fallback = com.example.fitness_tracker.ui.theme.featureAccent(
        com.example.fitness_tracker.ui.theme.Feature.DIET,
    )
    return DietTypeColors(
        main = fallback.main,
        container = fallback.container,
        onContainer = fallback.onContainer,
    )
}

/**
 * One chip for a [DietType]. When selected, the chip fills with the type's color
 * and shows white text. When unselected, it stays neutral but carries a small
 * colored dot so the user can still tell options apart at a glance.
 */
@Composable
private fun DietTypeChip(
    type: DietType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = dietTypeColors(type)
    val container = if (selected) colors.main else MaterialTheme.colorScheme.surfaceVariant
    val ink = if (selected) androidx.compose.ui.graphics.Color.White
    else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .background(color = container, shape = RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading dot — visible only when unselected (when selected, the whole pill IS the color).
        if (!selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = colors.main, shape = RoundedCornerShape(50)),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelLarge,
            color = ink,
        )
    }
}

@Composable
private fun CategoryHeader(
    category: MealCategory,
    isLoading: Boolean,
    onSuggestMore: () -> Unit,
    accent: DietTypeColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = category.label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = accent.main,
            )
        } else {
            Row(
                modifier = Modifier
                    .clickable { onSuggestMore() }
                    .background(
                        color = accent.container,
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = accent.onContainer,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Suggest more",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.onContainer,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    accent: DietTypeColors,
    onTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { onTap() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (meal.isAiGenerated) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI suggestion",
                    tint = accent.main,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = meal.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${meal.calories} kcal",
                style = MaterialTheme.typography.labelLarge,
                color = accent.main,
            )
            Text(
                text = "${meal.proteinG}g P",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealDetailSheet(
    meal: Meal,
    onLog: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberFullSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (meal.description.isNotBlank()) {
                Text(
                    text = meal.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Macros row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                MacroStat("${meal.calories}", "kcal")
                MacroStat("${meal.proteinG}g", "protein")
                MacroStat("${meal.carbsG}g", "carbs")
                MacroStat("${meal.fatG}g", "fat")
            }

            SectionLabel("Ingredients")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                meal.ingredients.split("\n").filter { it.isNotBlank() }.forEach { line ->
                    Text(
                        text = "·  $line",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            SectionLabel("Steps")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                meal.steps.split("\n").filter { it.isNotBlank() }.forEachIndexed { i, line ->
                    Text(
                        text = "${i + 1}.  $line",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDelete != null) {
                    QuietTextButton(label = "Remove", onClick = onDelete)
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                QuietTextButton(label = "Close", onClick = onDismiss)
            }
            PillCta(label = "Add to today", onClick = onLog)
        }
    }
}

@Composable
private fun MacroStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalorieGoalSheet(
    initialCalories: Int?,
    initialProtein: Int?,
    onSave: (Int, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberFullSheetState()
    var calories by rememberSaveable {
        mutableStateOf(initialCalories?.toString() ?: "")
    }
    var protein by rememberSaveable {
        mutableStateOf(initialProtein?.toString() ?: "")
    }
    val parsedKcal = calories.toIntOrNull()
    val parsedProtein = protein.toIntOrNull()
    val canSave = parsedKcal != null && parsedKcal in 500..6000

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Calorie goal",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            MinimalTextField(
                value = calories,
                onValueChange = { v -> calories = v.filter { c -> c.isDigit() }.take(4) },
                label = "Calories (kcal)",
                placeholder = "2200",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            MinimalTextField(
                value = protein,
                onValueChange = { v -> protein = v.filter { c -> c.isDigit() }.take(3) },
                label = "Protein target (g, optional)",
                placeholder = "150",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                QuietTextButton(label = "Cancel", onClick = onDismiss)
            }
            PillCta(
                label = "Save",
                enabled = canSave,
                onClick = { parsedKcal?.let { onSave(it, parsedProtein) } },
            )
        }
    }
}

private val DietType.label: String
    get() = when (this) {
        DietType.VEG -> "Veg"
        DietType.NON_VEG -> "Non-veg"
        DietType.VEGAN -> "Vegan"
        DietType.EGGETARIAN -> "Eggetarian"
    }

private val MealCategory.label: String
    get() = when (this) {
        MealCategory.BREAKFAST -> "Breakfast"
        MealCategory.LUNCH -> "Lunch"
        MealCategory.DINNER -> "Dinner"
        MealCategory.SNACK -> "Snacks"
        MealCategory.HIGH_PROTEIN -> "High-protein"
    }

