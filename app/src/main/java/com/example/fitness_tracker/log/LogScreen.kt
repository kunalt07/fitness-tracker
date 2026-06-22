package com.example.fitness_tracker.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.fitness_tracker.ui.rememberFullSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.data.Exercise
import com.example.fitness_tracker.data.ExerciseKind
import com.example.fitness_tracker.data.SetWithExerciseRow
import com.example.fitness_tracker.data.WorkoutTemplate
import com.example.fitness_tracker.ui.BottomCtaBar
import com.example.fitness_tracker.ui.ExercisePickerDialog
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillChip
import com.example.fitness_tracker.ui.PillCta
import com.example.fitness_tracker.ui.QuietTextButton
import com.example.fitness_tracker.ui.ScreenTitle
import kotlinx.coroutines.launch

private sealed interface SheetMode {
    data class New(val prefillExerciseId: Long?) : SheetMode
    data class Edit(val setId: Long) : SheetMode
}

@Composable
fun LogScreen(
    contentPadding: PaddingValues,
    viewModel: LogViewModel = viewModel(),
) {
    val active by viewModel.activeSession.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val sets by viewModel.sets.collectAsState()
    val planned by viewModel.plannedExerciseIds.collectAsState()
    val restRemaining by viewModel.restRemainingSec.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val saveTemplatePromptId by viewModel.saveTemplatePrompt.collectAsState()
    val critique by viewModel.critique.collectAsState()
    val quickLog by viewModel.quickLog.collectAsState()
    val templateMuscle by viewModel.templateMuscleByTemplateId.collectAsState()
    val todayTotals by viewModel.todayTotals.collectAsState()
    var quickLogOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = com.example.fitness_tracker.LocalSnackbarHost.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var sheetMode by rememberSaveable(stateSaver = SheetModeSaver) {
        mutableStateOf<SheetMode?>(null)
    }

    val plannedExercises = remember(planned, exercises) {
        planned.mapNotNull { id -> exercises.firstOrNull { it.id == id } }
    }

    val topInset = contentPadding.calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content fills the entire screen; reserves bottom padding so the
        // last item isn't permanently hidden behind the floating CTA stack.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset)
                .padding(bottom = bottomInset + 160.dp),
        ) {
        ScreenTitle(if (active == null) "Workout" else "In progress")
        TodayProgressLine(totals = todayTotals)

        if (active != null && plannedExercises.isNotEmpty()) {
            PlannedRow(
                exercises = plannedExercises,
                onTap = { id -> sheetMode = SheetMode.New(prefillExerciseId = id) },
            )
        }

        if (active == null && templates.isNotEmpty()) {
            TemplatesRow(
                templates = templates,
                muscleByTemplateId = templateMuscle,
                onLaunch = { viewModel.startFromTemplate(it.id) },
                onDelete = { t ->
                    scope.launch {
                        val restore = viewModel.deleteTemplateWithUndo(t.id)
                        if (restore != null) {
                            com.example.fitness_tracker.postUndoSnackbar(
                                host = snackbarHost,
                                scope = scope,
                                message = "Template deleted",
                                restore = restore,
                            )
                        }
                    }
                },
            )
        }

        when {
            active == null -> CenteredHint("No session yet.")
            sets.isEmpty() -> CenteredHint("Tap “Log set” to begin.")
            else -> {
                val groups = remember(sets) { groupConsecutiveSets(sets) }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                ) {
                        groups.forEachIndexed { gIdx, group ->
                            item(key = "h-${group.headerKey}") {
                                ExerciseGroupHeader(
                                    name = group.exerciseName,
                                    muscleGroup = group.muscleGroup,
                                    isFirst = gIdx == 0,
                                )
                            }
                            itemsIndexed(
                                items = group.sets,
                                key = { _, s -> s.id },
                            ) { idx, s ->
                                GroupedSetRow(
                                    indexInGroup = idx + 1,
                                    set = s,
                                    onTap = { sheetMode = SheetMode.Edit(s.id) },
                                    onDelete = {
                                        scope.launch {
                                            val restore = viewModel.deleteSetWithUndo(s.id)
                                            if (restore != null) {
                                                com.example.fitness_tracker.postUndoSnackbar(
                                                    host = snackbarHost,
                                                    scope = scope,
                                                    message = "Set deleted",
                                                    restore = restore,
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Soft fade above the floating CTA stack so content scrolls away
        // gracefully instead of cutting off at a hard edge.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .padding(bottom = bottomInset)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )

        // Floating action stack: action row + rest banner + primary CTA.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = bottomInset),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (active != null && sets.isNotEmpty()) {
                    RepeatLastPill(onClick = { viewModel.repeatLastSet() })
                }
                IconButton(
                    onClick = { quickLogOpen = true },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = "Quick log",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (active != null) {
                    QuietTextButton(label = "End session", onClick = { viewModel.endSession() })
                }
            }

            if (restRemaining > 0) {
                RestBanner(remainingSec = restRemaining, onSkip = viewModel::cancelRest)
            }

            BottomCtaBar(
                label = if (active == null) "Start workout" else "Log set",
                onClick = {
                    if (active == null) viewModel.startSession()
                    else sheetMode = SheetMode.New(prefillExerciseId = null)
                },
            )
        }
    }

    if (saveTemplatePromptId != null) {
        SaveTemplateDialog(
            onSave = { name -> viewModel.confirmEndSession(name.takeIf { it.isNotBlank() }) },
            onSkip = { viewModel.confirmEndSession(null) },
            onDismiss = { viewModel.dismissSaveTemplatePrompt() },
        )
    }

    if (critique !is com.example.fitness_tracker.UiState.Initial) {
        CritiqueSheet(
            state = critique,
            onDismiss = { viewModel.dismissCritique() },
        )
    }

    if (quickLogOpen) {
        QuickLogSheet(
            state = quickLog,
            onParse = { text -> viewModel.parseQuickLog(text) },
            onConfirm = {
                viewModel.confirmQuickLog()
                quickLogOpen = false
            },
            onDismiss = {
                viewModel.dismissQuickLog()
                quickLogOpen = false
            },
        )
    }

    val mode = sheetMode
    if (mode != null && active != null) {
        SetSheet(
            mode = mode,
            exercises = exercises,
            sets = sets,
            lookupLastSet = { id -> viewModel.lastSetFor(id) },
            createExercise = { name, kind -> viewModel.createExercise(name, kind) },
            onUpdateExercise = { id, name, mg, k -> viewModel.updateExercise(id, name, mg, k) },
            onDeleteExercise = { id ->
                scope.launch {
                    val restore = viewModel.deleteExerciseWithUndo(id)
                    if (restore != null) {
                        com.example.fitness_tracker.postUndoSnackbar(
                            host = snackbarHost,
                            scope = scope,
                            message = "Exercise deleted",
                            restore = restore,
                        )
                    }
                }
            },
            onDismiss = { sheetMode = null },
            onSubmitNew = { exId, reps, weight, dur, dist ->
                viewModel.logSet(exId, reps, weight, dur, dist)
                sheetMode = null
            },
            onSubmitEdit = { id, reps, weight, dur, dist ->
                viewModel.updateSet(id, reps, weight, dur, dist)
                sheetMode = null
            },
        )
    }
}

@Composable
private fun PlannedRow(
    exercises: List<Exercise>,
    onTap: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "From your plan",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(exercises, key = { it.id }) { ex ->
                MuscleTintedChip(
                    label = ex.name,
                    muscleGroup = ex.muscleGroup,
                    onClick = { onTap(ex.id) },
                )
            }
        }
    }
}

@Composable
private fun RestBanner(remainingSec: Int, onSkip: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Rest · ${formatRest(remainingSec)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            QuietTextButton(label = "Skip", onClick = onSkip)
        }
    }
}

private fun formatRest(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun CenteredHint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodayProgressLine(totals: TodayTotals) {
    val parts = buildList {
        add("${totals.sets} set${if (totals.sets == 1) "" else "s"}")
        if (totals.exercises > 0) {
            add("${totals.exercises} exercise${if (totals.exercises == 1) "" else "s"}")
        }
        val volume = totals.volume
        when {
            volume.weightKgReps > 0 -> add(formatTodayWeight(volume.weightKgReps))
            volume.durationSec > 0 -> add(formatTodayDuration(volume.durationSec))
            volume.distanceMeters > 0 -> add(formatTodayDistance(volume.distanceMeters))
        }
    }
    val text = if (totals.sets == 0) "Nothing logged today" else parts.joinToString("  ·  ")
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 0.dp, bottom = 12.dp),
    )
}

private fun formatTodayWeight(kg: Double): String = when {
    kg >= 1000 -> "%.1fk kg".format(kg / 1000.0)
    kg % 1.0 == 0.0 -> "${kg.toInt()} kg"
    else -> "%.1f kg".format(kg)
}

private fun formatTodayDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

private fun formatTodayDistance(meters: Long): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000.0) else "$meters m"

/** A run of consecutive sets that share the same exercise. */
private data class ExerciseGroup(
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val sets: List<SetWithExerciseRow>,
    /** Stable key including position so the same exercise can appear twice in a session. */
    val headerKey: String,
)

private fun groupConsecutiveSets(sets: List<SetWithExerciseRow>): List<ExerciseGroup> {
    if (sets.isEmpty()) return emptyList()
    val groups = mutableListOf<ExerciseGroup>()
    var current = mutableListOf<SetWithExerciseRow>()
    var groupIndex = 0

    fun flush() {
        if (current.isEmpty()) return
        val first = current.first()
        groups += ExerciseGroup(
            exerciseId = first.exerciseId,
            exerciseName = first.exerciseName,
            muscleGroup = first.muscleGroup,
            sets = current.toList(),
            headerKey = "${first.exerciseId}-$groupIndex",
        )
        groupIndex += 1
    }

    for (s in sets) {
        if (current.isNotEmpty() && current.first().exerciseId != s.exerciseId) {
            flush()
            current = mutableListOf()
        }
        current += s
    }
    flush()
    return groups
}

@Composable
private fun ExerciseGroupHeader(
    name: String,
    muscleGroup: String,
    isFirst: Boolean,
) {
    val tint = com.example.fitness_tracker.ui.theme.muscleGroupColor(muscleGroup)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isFirst) 8.dp else 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 10.dp)
                .size(width = 3.dp, height = 16.dp)
                .background(tint, shape = RoundedCornerShape(2.dp)),
        )
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun GroupedSetRow(
    indexInGroup: Int,
    set: SetWithExerciseRow,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    val tint = com.example.fitness_tracker.ui.theme.muscleGroupColor(set.muscleGroup)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(start = 0.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Aligned vertical bar that matches the header's stripe position.
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(width = 3.dp, height = 28.dp)
                .background(tint.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp)),
        )
        Text(
            text = "${indexInGroup}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = formatSetSummary(set),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Delete set",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetSheet(
    mode: SheetMode,
    exercises: List<Exercise>,
    sets: List<SetWithExerciseRow>,
    lookupLastSet: suspend (Long) -> LastSet?,
    createExercise: suspend (String, ExerciseKind) -> Exercise?,
    onUpdateExercise: (Long, String, String, ExerciseKind) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSubmitNew: (Long, Int, Double, Int, Int) -> Unit,
    onSubmitEdit: (Long, Int, Double, Int, Int) -> Unit,
) {
    val sheetState = rememberFullSheetState()
    val scope = rememberCoroutineScope()

    val editingRow = (mode as? SheetMode.Edit)?.let { m ->
        sets.firstOrNull { it.id == m.setId }
    }
    val initialExerciseId = when (mode) {
        is SheetMode.New -> mode.prefillExerciseId
        is SheetMode.Edit -> editingRow?.exerciseId
    }

    // Key form state on the sheet identity so reopening (or switching New <-> Edit)
    // re-derives the initial values from the row being edited.
    val formKey = when (mode) {
        is SheetMode.New -> "new:${mode.prefillExerciseId ?: ""}"
        is SheetMode.Edit -> "edit:${mode.setId}"
    }
    var showPicker by rememberSaveable(formKey) { mutableStateOf(false) }
    var selectedId by rememberSaveable(formKey) { mutableStateOf(initialExerciseId) }
    var reps by rememberSaveable(formKey) {
        mutableStateOf(editingRow?.reps?.takeIf { it > 0 }?.toString() ?: "")
    }
    var weight by rememberSaveable(formKey) {
        mutableStateOf(editingRow?.weightKg?.takeIf { it > 0.0 }?.let(::formatDoubleInput) ?: "")
    }
    var durationMin by rememberSaveable(formKey) {
        mutableStateOf(editingRow?.durationSec?.takeIf { it > 0 }?.let { (it / 60).toString() } ?: "")
    }
    var durationSec by rememberSaveable(formKey) {
        mutableStateOf(
            editingRow?.durationSec?.takeIf { it > 0 }?.let { (it % 60).toString().padStart(2, '0') } ?: "",
        )
    }
    var distanceMeters by rememberSaveable(formKey) {
        mutableStateOf(editingRow?.distanceMeters?.takeIf { it > 0 }?.toString() ?: "")
    }
    var lastSetHint by rememberSaveable(formKey) { mutableStateOf<String?>(null) }

    val selectedExercise = exercises.firstOrNull { it.id == selectedId }
    val kind = selectedExercise?.kind ?: ExerciseKind.REPS

    val canSubmit = when {
        selectedId == null -> false
        kind == ExerciseKind.REPS -> reps.toIntOrNull()?.let { it > 0 } == true
        kind == ExerciseKind.TIME -> totalSeconds(durationMin, durationSec) > 0
        kind == ExerciseKind.DISTANCE -> distanceMeters.toIntOrNull()?.let { it > 0 } == true
        else -> false
    }

    LaunchedEffect(selectedId, mode) {
        val id = selectedId
        if (id == null || mode is SheetMode.Edit) {
            lastSetHint = null
            return@LaunchedEffect
        }
        val ex = exercises.firstOrNull { it.id == id } ?: run {
            lastSetHint = null
            return@LaunchedEffect
        }
        lastSetHint = lookupLastSet(id)?.let { last ->
            "Last: " + when (ex.kind) {
                ExerciseKind.REPS -> buildString {
                    append("${last.reps} reps")
                    if (last.weightKg > 0.0) append(" · ${formatWeight(last.weightKg)} kg")
                }
                ExerciseKind.TIME -> formatTime(last.durationSec)
                ExerciseKind.DISTANCE -> "${last.distanceMeters} m"
            }
        }
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
                if (mode is SheetMode.Edit) "Edit set" else "New set",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )

            ExerciseField(
                name = selectedExercise?.name,
                onTap = { showPicker = true },
            )

            lastSetHint?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            when (kind) {
                ExerciseKind.REPS -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MinimalTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { c -> c.isDigit() }.take(3) },
                        label = "Reps",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    MinimalTextField(
                        value = weight,
                        onValueChange = { input ->
                            weight = input.filter { it.isDigit() || it == '.' }.take(6)
                        },
                        label = "Weight (kg)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                ExerciseKind.TIME -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MinimalTextField(
                        value = durationMin,
                        onValueChange = { durationMin = it.filter { c -> c.isDigit() }.take(3) },
                        label = "Min",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    MinimalTextField(
                        value = durationSec,
                        onValueChange = {
                            val cleaned = it.filter { c -> c.isDigit() }.take(2)
                            durationSec = cleaned
                        },
                        label = "Sec",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                ExerciseKind.DISTANCE -> MinimalTextField(
                    value = distanceMeters,
                    onValueChange = { distanceMeters = it.filter { c -> c.isDigit() }.take(6) },
                    label = "Distance (m)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }

            PillCta(
                label = if (mode is SheetMode.Edit) "Save changes" else "Save set",
                enabled = canSubmit,
                onClick = {
                    val id = selectedId ?: return@PillCta
                    val r = reps.toIntOrNull() ?: 0
                    val w = weight.toDoubleOrNull() ?: 0.0
                    val sec = totalSeconds(durationMin, durationSec)
                    val m = distanceMeters.toIntOrNull() ?: 0
                    when (mode) {
                        is SheetMode.New -> onSubmitNew(id, r, w, sec, m)
                        is SheetMode.Edit -> onSubmitEdit(mode.setId, r, w, sec, m)
                    }
                },
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    var editingExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingExercise = exercises.firstOrNull { it.id == editingExerciseId }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = exercises,
            onDismiss = { showPicker = false },
            onPick = { ex ->
                selectedId = ex.id
                showPicker = false
            },
            onCreate = { name, k ->
                scope.launch {
                    val created = createExercise(name, k)
                    if (created != null) {
                        selectedId = created.id
                        showPicker = false
                    }
                }
            },
            onEdit = { ex -> editingExerciseId = ex.id },
        )
    }

    if (editingExercise != null) {
        com.example.fitness_tracker.ui.EditExerciseSheet(
            initial = editingExercise,
            onDismiss = { editingExerciseId = null },
            onSave = { name, mg, k ->
                onUpdateExercise(editingExercise.id, name, mg, k)
                editingExerciseId = null
            },
            onDelete = {
                val id = editingExercise.id
                editingExerciseId = null
                // If the deleted exercise was selected, clear selection.
                if (selectedId == id) selectedId = null
                showPicker = false
                onDeleteExercise(id)
            },
        )
    }
}

private val SheetModeSaver = androidx.compose.runtime.saveable.Saver<SheetMode?, String>(
    save = { sheet ->
        when (sheet) {
            null -> ""
            is SheetMode.New -> "N:${sheet.prefillExerciseId ?: ""}"
            is SheetMode.Edit -> "E:${sheet.setId}"
        }
    },
    restore = { s ->
        when {
            s.isEmpty() -> null
            s.startsWith("N:") -> SheetMode.New(s.removePrefix("N:").toLongOrNull())
            s.startsWith("E:") -> s.removePrefix("E:").toLongOrNull()?.let { SheetMode.Edit(it) }
            else -> null
        }
    },
)

private fun formatWeight(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.1f".format(kg)

private fun formatDoubleInput(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.1f".format(kg)

private fun formatTime(seconds: Int): String {
    if (seconds < 60) return "${seconds}s"
    val m = seconds / 60
    val s = seconds % 60
    return if (s == 0) "${m}m" else "%dm %02ds".format(m, s)
}

private fun totalSeconds(min: String, sec: String): Int {
    val m = min.toIntOrNull() ?: 0
    val s = sec.toIntOrNull() ?: 0
    return m * 60 + s
}

internal fun formatSetSummary(set: SetWithExerciseRow): String = when (set.exerciseKind) {
    ExerciseKind.REPS -> buildString {
        append("${set.reps} reps")
        if (set.weightKg > 0.0) append("  ·  ${formatWeight(set.weightKg)} kg")
    }
    ExerciseKind.TIME -> formatTime(set.durationSec)
    ExerciseKind.DISTANCE -> "${set.distanceMeters} m"
}

@Composable
private fun TemplatesRow(
    templates: List<WorkoutTemplate>,
    muscleByTemplateId: Map<Long, String>,
    onLaunch: (WorkoutTemplate) -> Unit,
    onDelete: (WorkoutTemplate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Quick start",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(templates, key = { it.id }) { t ->
                TemplateChip(
                    template = t,
                    muscleGroup = muscleByTemplateId[t.id],
                    onTap = { onLaunch(t) },
                    onLongPress = { onDelete(t) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickLogSheet(
    state: LogViewModel.QuickLogState,
    onParse: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberFullSheetState()
    var input by rememberSaveable { mutableStateOf("") }

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
                "Quick log",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Type what you did. The coach will turn it into sets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MinimalTextField(
                value = input,
                onValueChange = { input = it.take(280) },
                label = "What did you do?",
                placeholder = "e.g. 4x10 squats at 80, 3x bench at 60, 20 min run",
                minLines = 3,
            )

            when (state) {
                LogViewModel.QuickLogState.Idle -> Unit
                LogViewModel.QuickLogState.Parsing -> com.example.fitness_tracker.ui.SkeletonGroup(
                    spacing = 12.dp,
                ) {
                    com.example.fitness_tracker.ui.SkeletonRow(titleFraction = 0.55f, metaFraction = 0.3f)
                    com.example.fitness_tracker.ui.SkeletonRow(titleFraction = 0.65f, metaFraction = 0.35f)
                    com.example.fitness_tracker.ui.SkeletonRow(titleFraction = 0.5f, metaFraction = 0.25f)
                }
                is LogViewModel.QuickLogState.Error -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                is LogViewModel.QuickLogState.Preview -> {
                    Text(
                        "Found ${state.parsed.size} set${if (state.parsed.size == 1) "" else "s"}:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.parsed.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    p.exerciseName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = formatParsed(p),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietTextButton(label = "Cancel", onClick = onDismiss)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                when (state) {
                    is LogViewModel.QuickLogState.Preview -> PillCta(
                        label = "Save all",
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(0.5f),
                    )
                    else -> PillCta(
                        label = "Parse",
                        enabled = input.isNotBlank() && state !is LogViewModel.QuickLogState.Parsing,
                        onClick = { onParse(input) },
                        modifier = Modifier.fillMaxWidth(0.5f),
                    )
                }
            }
        }
    }
}

private fun formatParsed(p: LogViewModel.ParsedSet): String = when (p.kind) {
    ExerciseKind.REPS -> buildString {
        append("${p.reps} reps")
        if (p.weightKg > 0) append("  ·  ${if (p.weightKg % 1.0 == 0.0) p.weightKg.toInt() else p.weightKg} kg")
    }
    ExerciseKind.TIME -> formatTime(p.durationSec)
    ExerciseKind.DISTANCE -> "${p.distanceMeters} m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CritiqueSheet(
    state: com.example.fitness_tracker.UiState,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Coach's notes",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when (state) {
                com.example.fitness_tracker.UiState.Loading -> com.example.fitness_tracker.ui.SkeletonGroup(
                    spacing = 8.dp,
                ) {
                    com.example.fitness_tracker.ui.SkeletonBlock(modifier = Modifier.fillMaxWidth(0.95f))
                    com.example.fitness_tracker.ui.SkeletonBlock(modifier = Modifier.fillMaxWidth(0.85f))
                    com.example.fitness_tracker.ui.SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f))
                    com.example.fitness_tracker.ui.SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f))
                }
                is com.example.fitness_tracker.UiState.Success -> Text(
                    text = state.outputText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is com.example.fitness_tracker.UiState.Error -> Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                com.example.fitness_tracker.UiState.Initial -> Unit
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                QuietTextButton(label = "Close", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun MuscleTintedChip(label: String, muscleGroup: String, onClick: () -> Unit) {
    val tint = com.example.fitness_tracker.ui.theme.muscleGroupColor(muscleGroup)
    val container = tint.copy(alpha = 0.16f)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(tint, shape = androidx.compose.foundation.shape.CircleShape),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun RepeatLastPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "Repeat last",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ExerciseField(name: String?, onTap: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                "Exercise",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                name ?: "Pick exercise",
                style = MaterialTheme.typography.bodyLarge,
                color = if (name == null) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TemplateChip(
    template: WorkoutTemplate,
    muscleGroup: String?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val tint = muscleGroup?.let { com.example.fitness_tracker.ui.theme.muscleGroupColor(it) }
    val container = tint?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.surfaceVariant
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        modifier = Modifier.combinedClickable(
            onClick = onTap,
            onLongClick = onLongPress,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tint != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(tint, shape = androidx.compose.foundation.shape.CircleShape),
                )
                Text(
                    template.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
            } else {
                Text(
                    template.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun SaveTemplateDialog(
    onSave: (String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Save as template?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Reuse the exercises from this session next time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MinimalTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = "Name",
                    placeholder = "Push Day, Leg Day…",
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuietTextButton(label = "Don't save", onClick = onSkip)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    PillCta(
                        label = "Save",
                        enabled = name.isNotBlank(),
                        onClick = { onSave(name) },
                        modifier = Modifier.fillMaxWidth(0.5f),
                    )
                }
            }
        }
    }
}

