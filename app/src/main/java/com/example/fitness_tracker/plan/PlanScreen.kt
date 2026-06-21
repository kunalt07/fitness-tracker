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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = topInset)
                .padding(bottom = bottomInset + 120.dp),
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

                MinimalTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = "Equipment",
                    placeholder = "Dumbbells, pull-up bar…",
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
                is UiState.Success -> Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("Your plan")
                    MarkdownText(text = s.outputText)
                }
                UiState.Initial -> Spacer8()
            }
        }

        // Soft gradient fade above the CTA so scrolling content dissolves into the
        // "frosted" zone instead of hitting a solid edge.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp)
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

        // Floating CTA — overlaid above the scrollable form. Content scrolls under it.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset),
        ) {
            if (uiState is UiState.Success) {
                Column {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        QuietTextButton(
                            label = "Generate again",
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
                    BottomCtaBar(
                        label = "Use this plan",
                        onClick = {
                            scope.launch {
                                if (viewModel.applyPlan() > 0) onPlanApplied()
                            }
                        },
                    )
                }
            } else {
                val hasHistory by viewModel.hasHistory.collectAsState()
                Column {
                    if (hasHistory && uiState !is UiState.Loading) {
                        HistoryHintPill(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                        )
                    }
                    BottomCtaBar(
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Tailored to your last 2 weeks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Local edits — start from the current split, but ensure all 7 days are present.
    val editing = remember(current) {
        val byDay = current.associateBy { it.dayOfWeek }
        (1..7).map { d ->
            byDay[d] ?: WeeklySplitDay(dayOfWeek = d, focus = "")
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
                MinimalTextField(
                    value = editing[idx].focus,
                    onValueChange = { editing[idx] = editing[idx].copy(focus = it.take(40)) },
                    label = DAY_FULL[idx],
                    placeholder = "Chest + Triceps, Rest…",
                    singleLine = true,
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
