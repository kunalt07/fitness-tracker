package com.example.fitness_tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitness_tracker.data.Exercise
import com.example.fitness_tracker.data.ExerciseKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseSheet(
    initial: Exercise,
    onDismiss: () -> Unit,
    onSave: (name: String, muscleGroup: String, kind: ExerciseKind) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberFullSheetState()
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var muscleGroup by rememberSaveable(initial.id) { mutableStateOf(initial.muscleGroup) }
    var kind by rememberSaveable(initial.id) { mutableStateOf(initial.kind) }

    val canSave = name.trim().isNotEmpty() &&
        (name != initial.name || muscleGroup != initial.muscleGroup || kind != initial.kind)

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
                "Edit exercise",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )

            MinimalTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = "Name",
                singleLine = true,
            )
            MinimalTextField(
                value = muscleGroup,
                onValueChange = { muscleGroup = it.take(20) },
                label = "Muscle group",
                placeholder = "Chest, Back, Legs…",
                singleLine = true,
            )

            Text(
                "Measured as",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseKind.entries.forEach { k ->
                    PillChip(
                        selected = kind == k,
                        label = kindLabel(k),
                        onClick = { kind = k },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuietTextButton(label = "Delete", onClick = onDelete)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuietTextButton(label = "Cancel", onClick = onDismiss)
                }
            }

            PillCta(
                label = "Save",
                enabled = canSave,
                onClick = { onSave(name.trim(), muscleGroup.trim(), kind) },
            )
        }
    }
}

private fun kindLabel(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.REPS -> "Reps × weight"
    ExerciseKind.TIME -> "Time"
    ExerciseKind.DISTANCE -> "Distance"
}
