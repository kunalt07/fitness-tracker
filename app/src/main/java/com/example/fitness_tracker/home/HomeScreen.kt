package com.example.fitness_tracker.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.TopLevelDest
import com.example.fitness_tracker.log.LogViewModel
import com.example.fitness_tracker.plan.PlanViewModel
import com.example.fitness_tracker.stats.StatsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun HomeScreen(
    contentPadding: PaddingValues,
    onNavigate: (TopLevelDest) -> Unit,
    onOpenProfile: () -> Unit,
    planViewModel: PlanViewModel = viewModel(),
    logViewModel: LogViewModel = viewModel(),
    statsViewModel: StatsViewModel = viewModel(),
    dailyViewModel: DailyViewModel = viewModel(),
    dietViewModel: com.example.fitness_tracker.diet.DietViewModel = viewModel(),
    authViewModel: com.example.fitness_tracker.auth.AuthViewModel = viewModel(),
) {
    val dietTotals by dietViewModel.totalsToday.collectAsState()
    val dietGoal by dietViewModel.calorieGoal.collectAsState()
    val profile by authViewModel.profile.collectAsState()
    val split by planViewModel.weeklySplit.collectAsState()
    val todayTotals by logViewModel.todayTotals.collectAsState()
    val activeSession by logViewModel.activeSession.collectAsState()
    val plannedExerciseIds by logViewModel.plannedExerciseIds.collectAsState()
    val reminder by planViewModel.reminderSettings.collectAsState()
    val statsState by statsViewModel.state.collectAsState()
    val bodyWeightToday by dailyViewModel.bodyWeightToday.collectAsState()
    val readinessToday by dailyViewModel.readinessToday.collectAsState()
    val waterToday by dailyViewModel.waterToday.collectAsState()
    val weightGoal by dailyViewModel.weightGoal.collectAsState()
    var showWeight by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var showFood by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var showReadiness by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }

    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val focus = split.firstOrNull { it.dayOfWeek == today }?.focus?.takeIf { it.isNotBlank() }

    val scroll = rememberScrollState()
    var showReminder by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val notifPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { /* User's choice will be reflected next time we check. */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Greeting + bell + avatar.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                profile?.name?.takeIf { it.isNotBlank() }?.let { name ->
                    Text(
                        text = name.substringBefore(" "),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            IconButton(onClick = { showReminder = true }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Reminders",
                    tint = if (reminder?.enabled == true) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HomeAvatar(
                displayName = profile?.name ?: profile?.email,
                onClick = onOpenProfile,
            )
        }

        // Hero today card.
        TodayHeroCard(
            focus = focus,
            isActive = activeSession != null,
            plannedCount = plannedExerciseIds.size,
            streak = statsState.streakDays,
            todayTotals = todayTotals,
            onPrimaryAction = { onNavigate(TopLevelDest.Log) },
            onPlanToday = { onNavigate(TopLevelDest.Plan) },
        )

        // Daily check-ins.
        Text(
            text = "Today's check-in",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DailyTile(
                icon = Icons.Outlined.Scale,
                label = "Weight",
                value = formatWeightTileValue(bodyWeightToday?.weightKg, weightGoal?.targetKg),
                filled = bodyWeightToday != null,
                onClick = { showWeight = true },
            )
            DailyTile(
                icon = Icons.Outlined.Restaurant,
                label = "Food",
                value = formatCaloriesTileValue(
                    consumed = dietTotals.calories,
                    target = dietGoal?.targetCalories,
                ),
                filled = dietTotals.calories > 0,
                onClick = { showFood = true },
                onLongPress = { onNavigate(TopLevelDest.Diet) },
            )
            DailyTile(
                icon = Icons.Outlined.WaterDrop,
                label = "Water",
                value = waterToday?.glasses?.let { "$it" },
                filled = (waterToday?.glasses ?: 0) > 0,
                onClick = { dailyViewModel.bumpWater(1) },
                onLongPress = { dailyViewModel.bumpWater(-1) },
            )
        }

        // This week. Streak now lives on the hero flame, so this section
        // just labels the weekly bar grid.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val activeDays = statsState.weeklySetsPerDay.count { it > 0 }
            val goalMet = activeDays >= WEEKLY_ACTIVE_GOAL
            val streakColor = com.example.fitness_tracker.ui.theme.featureAccent(
                com.example.fitness_tracker.ui.theme.Feature.STREAK,
            ).main
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Activity score — turns the week into a target to fill. Goes
                // coral the moment the weekly goal lands.
                Text(
                    text = if (goalMet) "🔥 $activeDays/7 active" else "$activeDays/7 active",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (goalMet) streakColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HomeWeekDots(
                values = statsState.weeklySetsPerDay,
                labels = statsState.weekDayLabels,
                goalMet = goalMet,
                celebrateColor = streakColor,
                onClick = { onNavigate(TopLevelDest.Stats) },
            )
        }

        Box(modifier = Modifier.padding(bottom = 8.dp))
    }

    if (showWeight) {
        BodyWeightSheet(
            initialKg = bodyWeightToday?.weightKg,
            initialTargetKg = weightGoal?.targetKg,
            onDismiss = { showWeight = false },
            onSave = { kg, target ->
                dailyViewModel.saveBodyWeight(kg, target)
                showWeight = false
            },
        )
    }

    if (showFood) {
        FoodQuickAddSheet(
            consumed = dietTotals.calories,
            target = dietGoal?.targetCalories,
            onDismiss = { showFood = false },
            onOpenDiet = {
                showFood = false
                onNavigate(TopLevelDest.Diet)
            },
            onSave = { name, cal, protein ->
                dietViewModel.quickAddFood(name, cal, protein)
                showFood = false
            },
        )
    }

    if (showReadiness) {
        ReadinessSheet(
            initial = readinessToday?.level,
            onDismiss = { showReadiness = false },
            onSave = { level ->
                dailyViewModel.saveReadiness(level)
                showReadiness = false
            },
        )
    }

    if (showReminder) {
        com.example.fitness_tracker.plan.ReminderSettingsSheet(
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
                planViewModel.saveReminder(enabled, hour, minute)
                showReminder = false
            },
            onDismiss = { showReminder = false },
        )
    }
}

@Composable
private fun TodayHeroCard(
    focus: String?,
    isActive: Boolean,
    plannedCount: Int,
    streak: Int,
    todayTotals: com.example.fitness_tracker.log.TodayTotals,
    onPrimaryAction: () -> Unit,
    onPlanToday: () -> Unit,
) {
    val accent = focus?.let { com.example.fitness_tracker.ui.theme.focusKeywordColor(it) }
    val accentDot = accent ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(28.dp),
            )
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header row.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = accentDot, shape = CircleShape),
                )
                Text(
                    text = if (isActive) "Session in progress" else "Today",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                )
                // Streak flame — only once the user has a live streak going, so
                // a fresh install doesn't show a discouraging "0".
                if (streak > 0) {
                    val streakColor = com.example.fitness_tracker.ui.theme.featureAccent(
                        com.example.fitness_tracker.ui.theme.Feature.STREAK,
                    ).main
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Day streak",
                            tint = streakColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "$streak",
                            style = MaterialTheme.typography.titleMedium,
                            color = streakColor,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            // Big focus headline.
            Text(
                text = focus ?: "Rest day",
                style = MaterialTheme.typography.displaySmall,
                color = if (focus != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Stats row, or planned-exercises hint, or empty caption.
            // The hint takes priority when no session has started yet — it
            // tells the user the AI plan is queued and waiting on Log.
            when {
                todayTotals.sets > 0 -> {
                    // Count up from 0 when the stats appear — a bit of life on
                    // the numbers instead of a hard snap.
                    val spec = androidx.compose.animation.core.tween<Int>(700)
                    val animSets by androidx.compose.animation.core.animateIntAsState(
                        targetValue = todayTotals.sets, animationSpec = spec, label = "sets",
                    )
                    val animEx by androidx.compose.animation.core.animateIntAsState(
                        targetValue = todayTotals.exercises, animationSpec = spec, label = "exercises",
                    )
                    val animVol by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = todayTotals.volume.weightKgReps.toFloat(),
                        animationSpec = androidx.compose.animation.core.tween(700),
                        label = "volume",
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        StatBlock(value = "$animSets", label = "sets")
                        StatBlock(value = "$animEx", label = "exercises")
                        StatBlock(
                            value = formatVolumeBrief(todayTotals, animVol.toDouble()),
                            label = "volume",
                        )
                    }
                }
                plannedCount > 0 -> {
                    Text(
                        text = "$plannedCount ${if (plannedCount == 1) "exercise" else "exercises"} queued for today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                else -> {
                    Text(
                        text = "Nothing logged yet today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Primary action pill inside the card.
            val ctaLabel = when {
                isActive -> "Continue session"
                plannedCount > 0 -> "Start planned workout"
                todayTotals.sets > 0 -> "Resume workout"
                else -> "Start workout"
            }
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = ctaLabel,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Secondary CTA — only when nothing is queued and no session is
            // running. Points the user at the Plan tab to AI-generate one;
            // without this, a fresh user has no signal that path exists.
            if (!isActive && plannedCount == 0 && todayTotals.sets == 0) {
                Text(
                    text = "Plan today with AI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlanToday() }
                        .padding(vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

// Active days in a week that trip the coral celebration.
private const val WEEKLY_ACTIVE_GOAL = 4

@Composable
private fun HomeWeekDots(
    values: List<Int>,
    labels: List<String>,
    goalMet: Boolean,
    celebrateColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val scaleMax = (values.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
    val ink = MaterialTheme.colorScheme.primary
    val dotColor = if (goalMet) celebrateColor else ink
    val track = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val today = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    val cellShape = RoundedCornerShape(14.dp)

    // Cells pop in with a staggered bounce — a left-to-right cascade on open.
    val n = values.size
    val cellScales = remember(n) {
        List(n) { androidx.compose.animation.core.Animatable(0f) }
    }
    LaunchedEffect(n) {
        cellScales.forEachIndexed { i, anim ->
            launch {
                kotlinx.coroutines.delay(i * 60L)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                    ),
                )
            }
        }
    }

    // Haptic pop when today's cell is (or becomes) active — logging a set
    // today buzzes as the week fills.
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val todayCount = values.getOrNull(today) ?: 0
    LaunchedEffect(todayCount) {
        if (todayCount > 0) {
            haptic.performHapticFeedback(
                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            values.forEachIndexed { i, count ->
                val filled = count > 0
                // Intensity floor so a single-set day still reads clearly.
                val intensity = 0.4f + 0.6f * (count / scaleMax)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .graphicsLayer {
                            val s = cellScales.getOrNull(i)?.value ?: 1f
                            scaleX = s
                            scaleY = s
                        }
                        .clip(cellShape)
                        .background(
                            if (filled) dotColor.copy(alpha = intensity)
                            else androidx.compose.ui.graphics.Color.Transparent,
                        )
                        .then(
                            when {
                                i == today -> Modifier.border(2.dp, dotColor, cellShape)
                                !filled -> Modifier.border(1.dp, track, cellShape)
                                else -> Modifier
                            },
                        ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEachIndexed { i, l ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = l,
                        style = labelStyle,
                        color = if (i == today) MaterialTheme.colorScheme.onSurface else labelColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        Column { content() }
    }
}

@Composable
private fun SummaryRow(
    tint: androidx.compose.ui.graphics.Color,
    tintBg: androidx.compose.ui.graphics.Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color = tintBg, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.DailyTile(
    icon: ImageVector,
    label: String,
    value: String?,
    filled: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val container = if (filled) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (filled) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    val labelColor = if (filled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    val tileMod = if (onLongPress != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress)
    } else {
        Modifier.clickable { onClick() }
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .background(color = container, shape = RoundedCornerShape(20.dp))
            .then(tileMod)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = onContainer,
            modifier = Modifier.size(22.dp),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = onContainer,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun readinessIcon(level: com.example.fitness_tracker.data.Readiness?): ImageVector =
    when (level) {
        com.example.fitness_tracker.data.Readiness.TIRED -> Icons.Outlined.Bedtime
        com.example.fitness_tracker.data.Readiness.STRONG -> Icons.Outlined.Mood
        com.example.fitness_tracker.data.Readiness.OK -> Icons.Outlined.SentimentSatisfied
        null -> Icons.Outlined.SentimentSatisfied
    }

private fun readinessLabel(level: com.example.fitness_tracker.data.Readiness): String =
    when (level) {
        com.example.fitness_tracker.data.Readiness.TIRED -> "Tired"
        com.example.fitness_tracker.data.Readiness.OK -> "OK"
        com.example.fitness_tracker.data.Readiness.STRONG -> "Strong"
    }

private fun formatKgShort(kg: Double): String =
    if (kg % 1.0 == 0.0) "${kg.toInt()}" else "%.1f".format(kg)

private fun formatWeightTileValue(currentKg: Double?, targetKg: Double?): String? {
    if (currentKg == null) return null
    val cur = formatKgShort(currentKg)
    val tgt = targetKg?.let { formatKgShort(it) }
    return if (tgt != null) "$cur→$tgt" else "$cur kg"
}

private fun formatCaloriesTileValue(consumed: Int, target: Int?): String? {
    if (consumed == 0 && target == null) return null
    return if (target != null) "$consumed/$target" else "$consumed kcal"
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .let { if (enabled) it.clickable { onClick() } else it }
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                )
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeAvatar(displayName: String?, onClick: () -> Unit) {
    val initials = (displayName ?: "").trim()
        .split(Regex("\\s+|@"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = androidx.compose.foundation.shape.CircleShape,
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val period = when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..21 -> "evening"
        else -> "night"
    }
    return "Good $period"
}

private fun friendlyTime(): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

private fun formatVolumeBrief(
    totals: com.example.fitness_tracker.log.TodayTotals,
    weightKgReps: Double = totals.volume.weightKgReps,
): String {
    val v = totals.volume
    return when {
        weightKgReps >= 1000 -> "%.1fk".format(weightKgReps / 1000.0)
        weightKgReps > 0 -> weightKgReps.toInt().toString()
        v.durationSec > 0 -> "${v.durationSec / 60}m"
        v.distanceMeters >= 1000 -> "%.1fkm".format(v.distanceMeters / 1000.0)
        v.distanceMeters > 0 -> "${v.distanceMeters}m"
        else -> "—"
    }
}
