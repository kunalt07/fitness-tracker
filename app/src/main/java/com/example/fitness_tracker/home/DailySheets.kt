package com.example.fitness_tracker.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import com.example.fitness_tracker.ui.rememberFullSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
