package com.example.fitness_tracker.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.fitness_tracker.ui.rememberFullSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fitness_tracker.data.FoodEntry
import com.example.fitness_tracker.data.PlannedMeal
import com.example.fitness_tracker.data.Readiness
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillCta
import com.example.fitness_tracker.ui.QuietTextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyWeightSheet(
    initialKg: Double?,
    initialTargetKg: Double?,
    onDismiss: () -> Unit,
    onSave: (currentKg: Double, targetKg: Double?) -> Unit,
) {
    val sheetState = rememberFullSheetState()
    var current by rememberSaveable {
        mutableStateOf(initialKg?.let { formatWeight(it) } ?: "")
    }
    var target by rememberSaveable {
        mutableStateOf(initialTargetKg?.let { formatWeight(it) } ?: "")
    }
    val parsedCurrent = current.toDoubleOrNull()
    val parsedTarget = target.toDoubleOrNull()
    val canSave = parsedCurrent != null && parsedCurrent > 0 && parsedCurrent < 500 &&
        (parsedTarget == null || (parsedTarget > 0 && parsedTarget < 500))

    val direction = when {
        parsedCurrent == null || parsedTarget == null -> null
        parsedTarget < parsedCurrent - 0.05 -> "Cutting"
        parsedTarget > parsedCurrent + 0.05 -> "Bulking"
        else -> "Maintenance"
    }

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
                text = "Body weight",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Setting a target lets the AI bias your workout toward bulking or cutting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MinimalTextField(
                value = current,
                onValueChange = { v -> current = v.filter { c -> c.isDigit() || c == '.' }.take(6) },
                label = "Current (kg)",
                placeholder = "75.0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            MinimalTextField(
                value = target,
                onValueChange = { v -> target = v.filter { c -> c.isDigit() || c == '.' }.take(6) },
                label = "Target (kg, optional)",
                placeholder = "72.0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            if (direction != null) {
                Text(
                    text = direction,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietTextButton(label = "Cancel", onClick = onDismiss)
            }
            PillCta(
                label = "Save",
                enabled = canSave,
                onClick = {
                    parsedCurrent?.let { onSave(it, parsedTarget) }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodQuickAddSheet(
    consumed: Int,
    target: Int?,
    suggestions: List<PlannedMeal> = emptyList(),
    entries: List<FoodEntry> = emptyList(),
    foodOptions: List<com.example.fitness_tracker.diet.DietViewModel.FoodSuggestion> = emptyList(),
    aiBusy: Boolean = false,
    onAskAi: (query: String, fill: (String, Int, Int) -> Unit) -> Unit = { _, _ -> },
    onQuickLog: (PlannedMeal) -> Unit = {},
    onRemove: (Long) -> Unit = {},
    onDismiss: () -> Unit,
    onOpenDiet: () -> Unit,
    onSave: (name: String, calories: Int, proteinG: Int) -> Unit,
) {
    val sheetState = rememberFullSheetState()
    var name by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    // Over-goal action awaiting confirmation in the popup (null = no popup).
    var pendingConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }

    val parsedCalories = calories.toIntOrNull()
    val parsedProtein = protein.toIntOrNull()
    val canSave = name.isNotBlank() && parsedCalories != null && parsedCalories > 0

    // Run [action] straight away, unless adding [cal] would push today past the
    // calorie goal — then hold it for the confirmation popup instead.
    fun guarded(cal: Int, action: () -> Unit) {
        if (target != null && consumed + cal > target) pendingConfirm = action else action()
    }

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
                text = "Log food",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (target != null) "$consumed / $target kcal today"
                else "$consumed kcal today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // AI day-plan suggestions as compact horizontal-scroll pills. Tap to log.
            if (suggestions.isNotEmpty()) {
                Text(
                    text = "SUGGESTED — TAP TO LOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { meal ->
                        val dietColor = dietColorFor(meal.dietType)
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    dietColor?.copy(alpha = 0.16f)
                                        ?: MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { guarded(meal.calories) { onQuickLog(meal) } }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            dietColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = meal.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                )
                            }
                            Text(
                                text = "${meal.calories} kcal · ${meal.proteinG}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Today's logged entries — each removable.
            if (entries.isNotEmpty()) {
                Text(
                    text = "LOGGED TODAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(14.dp),
                                )
                                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${entry.calories} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            IconButton(onClick = { onRemove(entry.id) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            MinimalTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = "Food",
                placeholder = "Chicken & rice",
                singleLine = true,
            )

            // Name autocomplete: local matches (menu / day-plan / past logs) fill
            // macros instantly; an "Ask AI" row estimates macros for anything else.
            val query = name.trim()
            val matches = if (query.isNotEmpty()) {
                foodOptions.filter {
                    it.name.contains(query, ignoreCase = true) &&
                        !it.name.equals(query, ignoreCase = true)
                }.take(6)
            } else {
                emptyList()
            }
            val showAskAi = query.length >= 2
            if (matches.isNotEmpty() || showAskAi) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    matches.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    name = opt.name
                                    calories = opt.calories.toString()
                                    protein = opt.proteinG.toString()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = opt.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${opt.calories} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (showAskAi) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !aiBusy) {
                                    onAskAi(query) { n, c, p ->
                                        name = n
                                        calories = c.toString()
                                        protein = p.toString()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (aiBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (aiBusy) "Asking AI…" else "Ask AI: \"$query\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            MinimalTextField(
                value = calories,
                onValueChange = { v -> calories = v.filter { c -> c.isDigit() }.take(5) },
                label = "Calories (kcal)",
                placeholder = "550",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            MinimalTextField(
                value = protein,
                onValueChange = { v -> protein = v.filter { c -> c.isDigit() }.take(4) },
                label = "Protein (g, optional)",
                placeholder = "40",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietTextButton(label = "Full diet log", onClick = onOpenDiet)
                QuietTextButton(label = "Cancel", onClick = onDismiss)
            }
            PillCta(
                label = "Add",
                enabled = canSave,
                onClick = {
                    val cal = parsedCalories ?: return@PillCta
                    guarded(cal) { onSave(name.trim(), cal, parsedProtein ?: 0) }
                },
            )
        }
    }

    // Over-goal confirmation popup.
    pendingConfirm?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = { Text("Over your goal") },
            text = {
                Text(
                    "You've reached your ${target ?: 0} kcal goal for today. " +
                        "Eating extra may set back your goals. Add anyway?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirm = null
                    action()
                }) { Text("Add anyway") }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirm = null }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Diet-type accent for a suggestion pill, matched to the Diet screen's palette.
 * Best-effort keyword match on the AI label; null when unrecognized.
 */
private fun dietColorFor(label: String): Color? {
    val t = label.lowercase()
    return when {
        "vegan" in t -> Color(0xFF14A37F) // leaf / emerald
        "egg" in t -> Color(0xFFE0A82E)   // egg yolk amber
        "non" in t -> Color(0xFFD04A3A)   // red
        "veg" in t -> Color(0xFF1F8A4C)   // green
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessSheet(
    initial: Readiness?,
    onDismiss: () -> Unit,
    onSave: (Readiness) -> Unit,
) {
    val sheetState = rememberFullSheetState()
    var selected by rememberSaveable { mutableStateOf(initial?.name) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "How do you feel today?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "We can use this to suggest lighter or heavier sessions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ReadinessOption(
                    emoji = "😴",
                    label = "Tired",
                    selected = selected == Readiness.TIRED.name,
                    onClick = { selected = Readiness.TIRED.name },
                )
                ReadinessOption(
                    emoji = "🙂",
                    label = "OK",
                    selected = selected == Readiness.OK.name,
                    onClick = { selected = Readiness.OK.name },
                )
                ReadinessOption(
                    emoji = "💪",
                    label = "Strong",
                    selected = selected == Readiness.STRONG.name,
                    onClick = { selected = Readiness.STRONG.name },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietTextButton(label = "Cancel", onClick = onDismiss)
            }
            PillCta(
                label = "Save",
                enabled = selected != null,
                onClick = { selected?.let { onSave(Readiness.valueOf(it)) } },
            )
        }
    }
}

@Composable
private fun RowScope.ReadinessOption(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .weight(1f)
            .background(color = container, shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = onContainer,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatWeight(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.1f".format(kg)
