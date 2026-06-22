package com.example.fitness_tracker.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import com.example.fitness_tracker.ui.rememberFullSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.UiState
import com.example.fitness_tracker.data.WeeklySplitDay
import com.example.fitness_tracker.ui.BottomCtaBar
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillChip
import com.example.fitness_tracker.ui.PillCta
import com.example.fitness_tracker.ui.PillCtaSecondary
import com.example.fitness_tracker.ui.ScreenTitle
import com.example.fitness_tracker.ui.MarkdownText
import com.example.fitness_tracker.ui.QuietTextButton
import com.example.fitness_tracker.ui.SectionLabel
import com.example.fitness_tracker.ui.SkeletonBlock
import com.example.fitness_tracker.ui.SkeletonBullet
import com.example.fitness_tracker.ui.SkeletonGroup
import kotlinx.coroutines.launch

private val GOALS = listOf("Strength", "Hypertrophy", "Cardio", "Mobility", "Fat loss")
private val DURATIONS = listOf(15, 30, 45, 60)

// Curated suggestion list — common equipment, ordered roughly by frequency of use
// in mainstream programs. The picker filters this list as the user types, and
// also lets them add anything they type that isn't on the list (free-form add).
private val EQUIPMENT_SUGGESTIONS = listOf(
    "Dumbbells", "Barbell", "Kettlebell", "Pull-up bar", "Bench", "Squat rack",
    "Cable machine", "Resistance bands", "TRX / suspension", "Foam roller",
    "Jump rope", "Medicine ball", "Smith machine", "EZ bar", "Trap bar",
    "Plyo box", "Sandbag", "Battle ropes", "Rower", "Treadmill",
    "Stationary bike", "Yoga mat", "Bodyweight only",
)

// Muscle / focus suggestions for the weekly split. Lower-case used for matching;
// display title-cases on render. Keywords here line up with `focusKeywordColor()`
// in Color.kt so the chosen pills get the right per-muscle accent color.
private val MUSCLE_SUGGESTIONS = listOf(
    "Chest", "Back", "Shoulders", "Arms", "Biceps", "Triceps",
    "Legs", "Quads", "Hamstrings", "Glutes", "Calves",
    "Core", "Cardio", "Mobility", "Rest",
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanScreen(
    contentPadding: PaddingValues,
    onPlanApplied: () -> Unit = {},
    viewModel: PlanViewModel = viewModel(),
) {
    var goal by rememberSaveable { mutableStateOf(GOALS.first()) }
    var duration by rememberSaveable { mutableIntStateOf(30) }
    var equipment by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedDay by rememberSaveable { mutableIntStateOf(viewModel.todayDayOfWeek()) }
    var showSettingsMenu by rememberSaveable { mutableStateOf(false) }
    var showSplitEditor by rememberSaveable { mutableStateOf(false) }
    var showReminder by rememberSaveable { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val split by viewModel.weeklySplit.collectAsState()
    val reminder by viewModel.reminderSettings.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val notifPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { /* The user's choice will be reflected next time we check. */ }

    val selectedFocus = split.firstOrNull { it.dayOfWeek == selectedDay }?.focus.orEmpty()
    val anySplitSet = split.any { it.focus.isNotBlank() }

    val scroll = rememberScrollState()
    val topInset = contentPadding.calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()

    // Reserve more room at the bottom in success state — the "Generate again" row
    // adds ~50dp on top of the primary CTA, so markdown content needs that much
    // clearance below to keep the last line scrollable above the dock.
    val bottomReserve = 100.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                // imePadding() pushes content up when the keyboard opens so the
                // focused text field + its suggestion chips stay visible. Without
                // it the keyboard covers the bottom half of the screen and the
                // suggestions get hidden + scrolling appears stuck.
                .imePadding()
                .padding(top = topInset)
                .padding(bottom = bottomInset + bottomReserve),
        ) {
            ScreenTitle("Plan a workout")

            TodayBlock(
                selectedDay = selectedDay,
                isToday = selectedDay == viewModel.todayDayOfWeek(),
                todayDayOfWeek = viewModel.todayDayOfWeek(),
                focus = selectedFocus,
                anySplitSet = anySplitSet,
                split = split,
                onDayChange = { selectedDay = it },
                onOpenSettings = { showSettingsMenu = true },
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Goal")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GOALS) { g ->
                            PillChip(selected = goal == g, label = g, onClick = { goal = g })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Duration")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DURATIONS) { d ->
                            PillChip(
                                selected = duration == d,
                                label = "$d min",
                                onClick = { duration = d },
                            )
                        }
                    }
                }

                EquipmentPicker(
                    serialized = equipment,
                    onChange = { equipment = it },
                )

                MinimalTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes",
                    placeholder = "Sore back, no jumping…",
                    minLines = 2,
                )
            }

            when (val s = uiState) {
                UiState.Loading -> PlanSkeleton(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 24.dp),
                )
                is UiState.Error -> Text(
                    text = s.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp),
                )
                is UiState.Success -> {
                    val planAccent = com.example.fitness_tracker.ui.theme.featureAccent(
                        com.example.fitness_tracker.ui.theme.Feature.PLAN,
                    )
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 32.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = planAccent.main,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Your plan",
                                style = MaterialTheme.typography.labelLarge,
                                color = planAccent.main,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        MarkdownText(text = s.outputText)
                    }
                }
                UiState.Initial -> Spacer8()
            }
        }

        // Floating CTA — bare button, no card around it.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            if (uiState is UiState.Success) {
                // Two pills centered side-by-side over transparent space:
                //   • circular icon button for regenerate
                //   • primary "Use this plan" pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            )
                            .clickable {
                                viewModel.generatePlan(
                                    goal = goal,
                                    durationMin = duration,
                                    equipment = equipment,
                                    notes = notes,
                                    overrideFocus = selectedFocus.takeIf { it.isNotBlank() },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Regenerate plan",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        Box(modifier = Modifier.width(220.dp)) {
                            PillCta(
                                label = "Use this plan",
                                onClick = {
                                    scope.launch {
                                        if (viewModel.applyPlan() > 0) onPlanApplied()
                                    }
                                },
                            )
                        }
                    }
                }
            } else {
                val hasHistory by viewModel.hasHistory.collectAsState()
                Column {
                    if (hasHistory && uiState !is UiState.Loading) {
                        HistoryHintPill(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                        )
                    }
                    PillCta(
                        label = if (uiState is UiState.Loading) "Generating…" else "Generate plan",
                        enabled = uiState !is UiState.Loading,
                        onClick = {
                            viewModel.generatePlan(
                                goal = goal,
                                durationMin = duration,
                                equipment = equipment,
                                notes = notes,
                                overrideFocus = selectedFocus.takeIf { it.isNotBlank() },
                            )
                        },
                    )
                }
            }
        }
    }

    if (showSettingsMenu) {
        val daysSet = split.count { it.focus.isNotBlank() }
        val splitSummary = when (daysSet) {
            0 -> "Not set up yet"
            7 -> "All 7 days set"
            else -> "$daysSet of 7 days set"
        }
        val reminderSummary = reminder?.let { r ->
            if (!r.enabled) "Off"
            else "On · " + formatHourMinute(r.hour, r.minute)
        } ?: "Off"
        PlanSettingsMenuSheet(
            splitSummary = splitSummary,
            reminderSummary = reminderSummary,
            onOpenSplit = {
                showSettingsMenu = false
                showSplitEditor = true
            },
            onOpenReminder = {
                showSettingsMenu = false
                showReminder = true
            },
            onDismiss = { showSettingsMenu = false },
        )
    }

    if (showSplitEditor) {
        SplitEditorSheet(
            current = split,
            onSave = {
                viewModel.saveWeeklySplit(it)
                showSplitEditor = false
            },
            onDismiss = { showSplitEditor = false },
        )
    }

    if (showReminder) {
        ReminderSettingsSheet(
            initialEnabled = reminder?.enabled == true,
            initialHour = reminder?.hour ?: 18,
            initialMinute = reminder?.minute ?: 0,
            onSave = { enabled, hour, minute ->
                if (enabled && android.os.Build.VERSION.SDK_INT >= 33) {
                    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                viewModel.saveReminder(enabled, hour, minute)
                showReminder = false
            },
            onDismiss = { showReminder = false },
        )
    }
}

@Composable
private fun HistoryHintPill(modifier: Modifier = Modifier) {
    val accent = com.example.fitness_tracker.ui.theme.featureAccent(
        com.example.fitness_tracker.ui.theme.Feature.PLAN,
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = accent.container,
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = accent.onContainer,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Tailored to your last 2 weeks",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.onContainer,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun DayPickerWithTodayDot(
    selectedDay: Int,
    todayDayOfWeek: Int,
    onDayChange: (Int) -> Unit,
    split: List<WeeklySplitDay> = emptyList(),
) {
    val todayDot = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        (1..7).forEach { d ->
            val focus = split.firstOrNull { it.dayOfWeek == d }?.focus.orEmpty()
            val isSelected = d == selectedDay
            val isToday = d == todayDayOfWeek
            val muscleColor = if (focus.isNotBlank()) {
                com.example.fitness_tracker.ui.theme.focusKeywordColor(focus)
            } else null

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .background(
                        color = if (isSelected) primaryContainer
                        else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onDayChange(d) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    text = DAY_SHORT[d - 1],
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) onSurface else onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = muscleColor ?: if (focus.isBlank()) {
                                androidx.compose.ui.graphics.Color.Transparent
                            } else onSurfaceVariant.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 4.dp)
                        .background(
                            color = if (isToday) todayDot
                            else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
}

private fun formatHourMinute(hour: Int, minute: Int): String {
    val h12 = ((hour + 11) % 12) + 1
    val ampm = if (hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, minute, ampm)
}

@Composable
private fun Spacer8() {
    Box(modifier = Modifier.padding(vertical = 4.dp))
}

private val DAY_SHORT = listOf("S", "M", "T", "W", "T", "F", "S")  // index 0 = Sunday
private val DAY_FULL = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
)

@Composable
private fun TodayBlock(
    selectedDay: Int,
    isToday: Boolean,
    todayDayOfWeek: Int,
    focus: String,
    anySplitSet: Boolean,
    split: List<WeeklySplitDay>,
    onDayChange: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val dayName = DAY_FULL[(selectedDay - 1).coerceIn(0, 6)]
    val title = if (isToday) "Today · $dayName" else dayName

    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Plan settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            !anySplitSet -> {
                Text(
                    text = "Build your week",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Tell the coach what you train each day. You can leave days blank for rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PillCta(
                    label = "Set up your week",
                    onClick = onOpenSettings,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            focus.isBlank() -> {
                Text(
                    text = "Rest day",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DayPickerWithTodayDot(
                    selectedDay = selectedDay,
                    todayDayOfWeek = todayDayOfWeek,
                    onDayChange = onDayChange,
                    split = split,
                )
            }
            else -> {
                val accent = com.example.fitness_tracker.ui.theme.focusKeywordColor(focus)
                Text(
                    text = focus,
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent ?: MaterialTheme.colorScheme.onSurface,
                )
                DayPickerWithTodayDot(
                    selectedDay = selectedDay,
                    todayDayOfWeek = todayDayOfWeek,
                    onDayChange = onDayChange,
                    split = split,
                )
            }
        }
    }
    Box(modifier = Modifier.padding(top = 12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitEditorSheet(
    current: List<WeeklySplitDay>,
    onSave: (List<WeeklySplitDay>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberFullSheetState()
    // Local edits — start from the current split, but ensure all 7 days are present.
    // Migrate legacy "Chest + Triceps" / "Chest & Triceps" focus strings to the
    // new comma-separated form so the chip picker renders them as pills.
    val editing = remember(current) {
        val byDay = current.associateBy { it.dayOfWeek }
        (1..7).map { d ->
            val raw = byDay[d]?.focus.orEmpty()
            val migrated = raw.replace("+", ",").replace("&", ",")
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
                .joinToString(", ")
            WeeklySplitDay(dayOfWeek = d, focus = migrated)
        }.toMutableStateList()
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Weekly split",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Set what you train each day. Leave a day blank for rest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            (1..7).forEach { d ->
                val idx = d - 1
                FocusPicker(
                    dayLabel = DAY_FULL[idx],
                    serialized = editing[idx].focus,
                    onChange = { editing[idx] = editing[idx].copy(focus = it) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                QuietTextButton(label = "Cancel", onClick = onDismiss)
                Box(modifier = Modifier.padding(horizontal = 4.dp))
                PillCta(
                    label = "Save",
                    onClick = { onSave(editing.toList()) },
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
            }
        }
    }
}

/**
 * Generic chip-and-suggestion picker. Free-text input with autocomplete on top of
 * a curated list, plus selected items as removable pills. Used for both the
 * equipment field on the Plan screen and the per-day focus fields in the weekly
 * split editor.
 *
 *   serialized:    current value from parent (comma-separated string)
 *   onChange:      called with the new serialized value
 *   suggestions:   curated list this picker filters as the user types
 *   sectionLabel:  optional small caps label above the pills (null = no label)
 *   placeholder:   placeholder text for the input field
 *   pillColor:     selected-pill color resolver — receives the label, returns a
 *                  background color. Default = brand primaryContainer.
 *   onPillColor:   text/icon color on the pill. Default = onPrimaryContainer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipPicker(
    serialized: String,
    onChange: (String) -> Unit,
    suggestions: List<String>,
    sectionLabel: String? = null,
    label: String,
    placeholder: String,
    pillColor: @Composable (String) -> androidx.compose.ui.graphics.Color = {
        MaterialTheme.colorScheme.primaryContainer
    },
    onPillColor: @Composable (String) -> androidx.compose.ui.graphics.Color = {
        MaterialTheme.colorScheme.onPrimaryContainer
    },
) {
    var query by rememberSaveable(serialized) { mutableStateOf("") }
    val selected = remember(serialized) {
        serialized.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun commit(items: List<String>) {
        val seen = mutableSetOf<String>()
        val deduped = items.filter { seen.add(it.lowercase()) }
        onChange(deduped.joinToString(", "))
    }

    fun add(item: String) {
        val clean = item.trim()
        if (clean.isEmpty()) return
        if (selected.any { it.equals(clean, ignoreCase = true) }) { query = ""; return }
        commit(selected + clean)
        query = ""
    }

    fun remove(item: String) {
        commit(selected.filterNot { it.equals(item, ignoreCase = true) })
    }

    val visibleSuggestions = remember(query, selected, suggestions) {
        if (query.isBlank()) emptyList()
        else suggestions
            .filter { it.contains(query, ignoreCase = true) }
            .filter { item -> selected.none { it.equals(item, ignoreCase = true) } }
            .take(6)
    }
    val canAddCustom = query.isNotBlank() &&
        visibleSuggestions.none { it.equals(query.trim(), ignoreCase = true) } &&
        selected.none { it.equals(query.trim(), ignoreCase = true) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (sectionLabel != null) SectionLabel(sectionLabel)

        if (selected.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selected.forEach { item ->
                    SelectedPill(
                        label = item,
                        background = pillColor(item),
                        ink = onPillColor(item),
                        onRemove = { remove(item) },
                    )
                }
            }
        }

        MinimalTextField(
            value = query,
            onValueChange = { query = it.take(40) },
            label = label,
            placeholder = placeholder,
            singleLine = true,
        )

        if (visibleSuggestions.isNotEmpty() || canAddCustom) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visibleSuggestions.forEach { s ->
                    SuggestionChip(label = s, onTap = { add(s) })
                }
                if (canAddCustom) {
                    SuggestionChip(label = "+ Add \"${query.trim()}\"", onTap = { add(query) })
                }
            }
        }
    }
}

/** Equipment picker — thin wrapper over [ChipPicker] for the Plan screen. */
@Composable
private fun EquipmentPicker(serialized: String, onChange: (String) -> Unit) {
    ChipPicker(
        serialized = serialized,
        onChange = onChange,
        suggestions = EQUIPMENT_SUGGESTIONS,
        sectionLabel = "Equipment",
        label = if (serialized.isBlank()) "Add equipment" else "Add another",
        placeholder = "Dumbbells, pull-up bar…",
    )
}

/**
 * Focus picker for one day in the weekly split. Same UX as [EquipmentPicker]
 * but draws each pill in the muscle-group accent color so the user sees, e.g.,
 * "Chest" in red and "Back" in blue immediately after tapping.
 */
@Composable
private fun FocusPicker(
    dayLabel: String,
    serialized: String,
    onChange: (String) -> Unit,
) {
    ChipPicker(
        serialized = serialized,
        onChange = onChange,
        suggestions = MUSCLE_SUGGESTIONS,
        sectionLabel = dayLabel,
        label = "Add focus",
        placeholder = "Chest, Triceps, Rest…",
        pillColor = { item ->
            // Lighten the muscle's main color into a subtle background tint.
            com.example.fitness_tracker.ui.theme.focusKeywordColor(item)
                ?.copy(alpha = 0.18f)
                ?: MaterialTheme.colorScheme.primaryContainer
        },
        onPillColor = { item ->
            com.example.fitness_tracker.ui.theme.focusKeywordColor(item)
                ?: MaterialTheme.colorScheme.onPrimaryContainer
        },
    )
}

@Composable
private fun SelectedPill(
    label: String,
    background: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(color = background, shape = RoundedCornerShape(50))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ink,
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove $label",
                tint = ink,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onTap() }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PlanSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier, spacing = 14.dp) {
        // ## Warm-up
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.35f), height = 22.dp)
        SkeletonBullet(fraction = 0.7f)
        SkeletonBullet(fraction = 0.55f)
        // ## Main
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.25f), height = 22.dp)
        SkeletonBullet(fraction = 0.85f)
        SkeletonBullet(fraction = 0.78f)
        SkeletonBullet(fraction = 0.82f)
        SkeletonBullet(fraction = 0.6f)
        // ## Cool-down
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f), height = 22.dp)
        SkeletonBullet(fraction = 0.5f)
    }
}
