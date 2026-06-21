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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.LocalSnackbarHost
import com.example.fitness_tracker.data.DietType
import com.example.fitness_tracker.data.Meal
import com.example.fitness_tracker.data.MealCategory
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
    val totals by viewModel.totalsToday.collectAsState()
    val goal by viewModel.calorieGoal.collectAsState()
    val snackbarHost = LocalSnackbarHost.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val topInset = contentPadding.calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()

    var showGoalSheet by rememberSaveable { mutableStateOf(false) }
    var openMealId by rememberSaveable { mutableStateOf<Long?>(null) }
    val openMeal = remember(meals, openMealId) { meals.firstOrNull { it.id == openMealId } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topInset)
            .padding(bottom = bottomInset),
    ) {
        // Title row + cog
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) { ScreenTitle("Menu") }
            IconButton(onClick = { showGoalSheet = true }) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Calorie goal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Diet type picker
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionLabel("I eat")
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(DietType.entries.toList()) { type ->
                PillChip(
                    selected = pref?.type == type,
                    label = type.label,
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

        // One row per category
        MealCategory.entries.forEach { category ->
            val rowMeals = meals.filter { it.category == category }
            if (rowMeals.isEmpty() && suggest !is DietViewModel.SuggestState.Loading) return@forEach

            CategoryHeader(
                category = category,
                isLoading = (suggest as? DietViewModel.SuggestState.Loading)?.category == category,
                onSuggestMore = { viewModel.suggestMore(category) },
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rowMeals, key = { it.id }) { m ->
                    MealCard(meal = m, onTap = { openMealId = m.id })
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

@Composable
private fun CategoryHeader(
    category: MealCategory,
    isLoading: Boolean,
    onSuggestMore: () -> Unit,
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
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Row(
                modifier = Modifier
                    .clickable { onSuggestMore() }
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Suggest more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MealCard(meal: Meal, onTap: () -> Unit) {
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
                    tint = MaterialTheme.colorScheme.primary,
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
                color = MaterialTheme.colorScheme.primary,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

