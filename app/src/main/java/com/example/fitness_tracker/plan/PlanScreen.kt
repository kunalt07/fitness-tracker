package com.example.fitness_tracker.plan

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.UiState
import com.example.fitness_tracker.data.WeeklySplitDay
import com.example.fitness_tracker.ui.BottomCtaBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillChip
import com.example.fitness_tracker.ui.PillCta
import com.example.fitness_tracker.ui.PillCtaSecondary
import com.example.fitness_tracker.ui.ScreenTitle
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
    var showDurationDialog by rememberSaveable { mutableStateOf(false) }
    // Non-blank = train with equipment; blank = bodyweight only.
    var equipment by rememberSaveable { mutableStateOf("Full gym equipment") }
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InputCard {
                    SectionLabel("Goal")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GOALS) { g ->
                            PillChip(selected = goal == g, label = g, onClick = { goal = g })
                        }
                    }
                }

                InputCard {
                    SectionLabel("Duration")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DURATIONS) { d ->
                            PillChip(
                                selected = duration == d,
                                label = "$d min",
                                onClick = { duration = d },
                            )
                        }
                        // A custom (non-preset) value rides as its own selected pill.
                        if (duration !in DURATIONS) {
                            item {
                                PillChip(
                                    selected = true,
                                    label = "$duration min",
                                    onClick = { showDurationDialog = true },
                                )
                            }
                        }
                        // "+" opens a small entry dialog for a custom minute count.
                        item {
                            PillChip(
                                selected = false,
                                label = "+",
                                onClick = { showDurationDialog = true },
                            )
                        }
                    }
                }

                InputCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionLabel("Equipment")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EquipmentOption(
                                icon = Icons.Outlined.Check,
                                contentDescription = "With equipment",
                                selected = equipment.isNotBlank(),
                                onClick = { equipment = "Full gym equipment" },
                            )
                            EquipmentOption(
                                icon = Icons.Outlined.Close,
                                contentDescription = "Without equipment",
                                selected = equipment.isBlank(),
                                onClick = { equipment = "" },
                            )
                        }
                    }
                }

                InputCard {
                    MinimalTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes",
                        placeholder = "Sore back, no jumping…",
                        minLines = 2,
                    )
                }
            }

            // Crossfade the result region so the skeleton → plan (and error)
            // transitions fade instead of popping in.
            Crossfade(
                targetState = uiState,
                animationSpec = tween(260),
                label = "planResult",
            ) { s ->
                when (s) {
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
                            PlanResult(markdown = s.outputText)
                        }
                    }
                    UiState.Initial -> Spacer8()
                }
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

    if (showDurationDialog) {
        AddDurationDialog(
            initialMinutes = duration,
            onConfirm = { duration = it; showDurationDialog = false },
            onDismiss = { showDurationDialog = false },
        )
    }
}

// Slot-machine wheel geometry: 48dp rows, 5 visible (center = selection).
private val WHEEL_ITEM_HEIGHT = 48.dp
private const val WHEEL_VISIBLE = 5

/** Digital HH:MM:SS entry — three independent snapping wheel drums. Total is
 * stored as whole minutes (seconds >= 30 round up), clamped to at least 1. */
@Composable
private fun AddDurationDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hours by rememberSaveable { mutableIntStateOf(initialMinutes / 60) }
    var mins by rememberSaveable { mutableIntStateOf(initialMinutes % 60) }
    var secs by rememberSaveable { mutableIntStateOf(30) }

    val totalMin = (hours * 60 + mins + if (secs >= 30) 1 else 0).coerceAtLeast(1)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add duration") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    // Center highlight band spans all three wheels (drawn behind).
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary,
                            thickness = 1.5.dp,
                        )
                        Spacer(modifier = Modifier.height(WHEEL_ITEM_HEIGHT))
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary,
                            thickness = 1.5.dp,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WheelPicker(range = 0..23, initial = hours, onChange = { hours = it })
                        WheelColon()
                        WheelPicker(range = 0..59, initial = mins, onChange = { mins = it })
                        WheelColon()
                        WheelPicker(range = 0..59, initial = secs, onChange = { secs = it })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "≈ $totalMin min",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { QuietTextButton(label = "Set", onClick = { onConfirm(totalMin) }) },
        dismissButton = { QuietTextButton(label = "Cancel", onClick = onDismiss) },
    )
}

/** One snapping wheel drum for a 0..N range. Center item is the selection;
 * neighbours dim by distance. Fires onChange on every centered-value change. */
@Composable
private fun WheelPicker(range: IntRange, initial: Int, onChange: (Int) -> Unit) {
    val values = remember(range) { range.toList() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = (initial - range.first).coerceIn(0, values.lastIndex),
    )
    val fling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(listState)
    val density = LocalDensity.current
    val itemPx = with(density) { WHEEL_ITEM_HEIGHT.toPx() }

    // Centered index = top item + rounded fraction of a row scrolled past it.
    val centerIndex by remember {
        androidx.compose.runtime.derivedStateOf {
            val extra = ((listState.firstVisibleItemScrollOffset + itemPx / 2f) / itemPx).toInt()
            (listState.firstVisibleItemIndex + extra).coerceIn(0, values.lastIndex)
        }
    }
    LaunchedEffect(centerIndex) { onChange(values[centerIndex]) }

    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        flingBehavior = fling,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .height(WHEEL_ITEM_HEIGHT * WHEEL_VISIBLE),
        // Top/bottom pad of 2 rows so first/last value can reach the center.
        contentPadding = PaddingValues(vertical = WHEEL_ITEM_HEIGHT * ((WHEEL_VISIBLE - 1) / 2)),
    ) {
        itemsIndexed(values) { index, value ->
            val selected = index == centerIndex
            Box(
                modifier = Modifier
                    .height(WHEEL_ITEM_HEIGHT)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value.toString().padStart(2, '0'),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (selected) 24.sp else 18.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun WheelColon() {
    Text(
        text = ":",
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
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

// Weekly split rows use dayOfWeek 1=Sunday..7=Saturday. Display Monday-first to
// match the reference; "Th" disambiguates Thursday from Tuesday.
private val MON_FIRST_DAYS = listOf(2, 3, 4, 5, 6, 7, 1)
private val MON_FIRST_LABELS = listOf("M", "T", "W", "Th", "F", "S", "S")

@Composable
private fun DayPillRow(
    selectedDay: Int,
    todayDayOfWeek: Int,
    onDayChange: (Int) -> Unit,
    split: List<WeeklySplitDay> = emptyList(),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MON_FIRST_DAYS.forEachIndexed { i, d ->
            val focus = split.firstOrNull { it.dayOfWeek == d }?.focus.orEmpty()
            DayPill(
                label = MON_FIRST_LABELS[i],
                selected = d == selectedDay,
                isToday = d == todayDayOfWeek,
                focus = focus,
                onClick = { onDayChange(d) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One tall stadium day button — outlined; selected gets a thicker bright ring.
 * Keeps the per-day muscle-color dot and the today underline mark. */
@Composable
private fun DayPill(
    label: String,
    selected: Boolean,
    isToday: Boolean,
    focus: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val muscleColor = if (focus.isNotBlank()) {
        com.example.fitness_tracker.ui.theme.focusKeywordColor(focus)
    } else null

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) onSurface else onSurfaceVariant,
        )
        // Muscle-color dot — transparent on rest/blank days.
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = muscleColor
                        ?: if (focus.isBlank()) androidx.compose.ui.graphics.Color.Transparent
                        else onSurfaceVariant.copy(alpha = 0.4f),
                    shape = CircleShape,
                ),
        )
        // Today underline mark.
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 3.dp)
                .background(
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(2.dp),
                ),
        )
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
    // Breadcrumb: "Today › Monday" when viewing today, else just the day name.
    val breadcrumb = if (isToday) "Today  ›  $dayName" else dayName

    // Big title + its color per state: focus set → muscle accent; rest → muted;
    // no split at all → "Build your week" call to action.
    val focusAccent = com.example.fitness_tracker.ui.theme.focusKeywordColor(focus)
    val bigTitle = when {
        !anySplitSet -> "Build your week"
        focus.isBlank() -> "Rest day"
        else -> focus
    }
    val bigTitleColor = when {
        !anySplitSet -> MaterialTheme.colorScheme.onSurface
        focus.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> focusAccent ?: MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = breadcrumb,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = bigTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = bigTitleColor,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Plan settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (anySplitSet) {
            DayPillRow(
                selectedDay = selectedDay,
                todayDayOfWeek = todayDayOfWeek,
                onDayChange = onDayChange,
                split = split,
            )
        } else {
            Text(
                text = "Tell the coach what you train each day. You can leave days blank for rest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PillCta(label = "Set up your week", onClick = onOpenSettings)
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
internal fun ChipPicker(
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

// --- Generated-plan rendering: parse the AI markdown into sections, and render
// the Main-section exercises as distinct rows. ---

private data class PlanItem(val text: String, val isExercise: Boolean, val isBullet: Boolean)
private data class PlanSectionData(val header: String?, val items: List<PlanItem>)

private fun cleanInline(s: String): String =
    s.replace("**", "").replace("`", "").trim()

private fun parsePlanSections(markdown: String): List<PlanSectionData> {
    val sections = mutableListOf<PlanSectionData>()
    var header: String? = null
    var items = mutableListOf<PlanItem>()
    fun flush() {
        if (header != null || items.isNotEmpty()) {
            sections.add(PlanSectionData(header, items.toList()))
        }
        items = mutableListOf()
    }
    markdown.lines().forEach { raw ->
        val line = raw.trim()
        when {
            line.isEmpty() -> Unit
            line.startsWith("#") -> {
                flush()
                header = line.trimStart('#').trim()
            }
            line.startsWith("-") || line.startsWith("*") || line.startsWith("•") -> {
                val isEx = header?.contains("main", ignoreCase = true) == true
                items.add(PlanItem(cleanInline(line.drop(1)), isExercise = isEx, isBullet = true))
            }
            else -> items.add(PlanItem(cleanInline(line), isExercise = false, isBullet = false))
        }
    }
    flush()
    return sections
}

/** Split "Bench Press — 4 x 8" into name + detail. */
private fun splitExercise(raw: String): Pair<String, String> {
    val markers = listOf("—", "–", " - ", ":")
    var idx = -1
    var mlen = 0
    markers.forEach { m ->
        val i = raw.indexOf(m)
        if (i >= 0 && (idx == -1 || i < idx)) { idx = i; mlen = m.length }
    }
    return if (idx >= 0) raw.substring(0, idx).trim() to raw.substring(idx + mlen).trim()
    else raw.trim() to ""
}

@Composable
private fun PlanResult(markdown: String) {
    val sections = remember(markdown) { parsePlanSections(markdown) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        sections.forEach { section ->
            section.header?.let {
                Text(
                    text = it.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            section.items.forEach { item ->
                when {
                    item.isExercise -> ExerciseResultRow(item.text)
                    item.isBullet -> Text(
                        text = "•  ${item.text}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    else -> Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One exercise row: name (+ sets×reps). */
@Composable
private fun ExerciseResultRow(raw: String) {
    val (name, detail) = remember(raw) { splitExercise(raw) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One half of the with/without-equipment toggle: an icon-only tick/X pill. */
@Composable
private fun EquipmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = fg, modifier = Modifier.size(20.dp))
    }
}

/** Rounded, outlined container that groups one input section. Border-only (no
 * fill) so the inner chips/fields keep their own surfaces and stay legible. */
@Composable
private fun InputCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
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
