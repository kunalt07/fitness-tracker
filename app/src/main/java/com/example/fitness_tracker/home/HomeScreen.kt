package com.example.fitness_tracker.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.EditNote
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val reminder by planViewModel.reminderSettings.collectAsState()
    val statsState by statsViewModel.state.collectAsState()
    val bodyWeightToday by dailyViewModel.bodyWeightToday.collectAsState()
    val readinessToday by dailyViewModel.readinessToday.collectAsState()
    val waterToday by dailyViewModel.waterToday.collectAsState()
    val weightGoal by dailyViewModel.weightGoal.collectAsState()
    val quickLog by logViewModel.quickLog.collectAsState()
    var quickLogOpen by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var showWeight by androidx.compose.runtime.saveable.rememberSaveable {
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
            todayTotals = todayTotals,
            onPrimaryAction = { onNavigate(TopLevelDest.Log) },
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
                icon = Icons.Outlined.EditNote,
                label = "Quick log",
                value = null,
                filled = false,
                onClick = { quickLogOpen = true },
            )
            DailyTile(
                icon = Icons.Outlined.Scale,
                label = "Weight",
                value = formatWeightTileValue(bodyWeightToday?.weightKg, weightGoal?.targetKg),
                filled = bodyWeightToday != null,
                onClick = { showWeight = true },
            )
            DailyTile(
                icon = Icons.Outlined.Restaurant,
                label = "Calories",
                value = formatCaloriesTileValue(
                    consumed = dietTotals.calories,
                    target = dietGoal?.targetCalories,
                ),
                filled = dietTotals.calories > 0,
                onClick = { onNavigate(TopLevelDest.Diet) },
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

        // This week.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${statsState.streakDays}d streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HomeWeeklyBars(
                values = statsState.weeklySetsPerDay,
                labels = statsState.weekDayLabels,
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

    if (quickLogOpen) {
        com.example.fitness_tracker.log.QuickLogSheet(
            state = quickLog,
            onParse = { logViewModel.parseQuickLog(it) },
            onConfirm = {
                logViewModel.confirmQuickLog()
                quickLogOpen = false
            },
            onDismiss = {
                logViewModel.dismissQuickLog()
                quickLogOpen = false
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
    todayTotals: com.example.fitness_tracker.log.TodayTotals,
    onPrimaryAction: () -> Unit,
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
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            // Big focus headline.
            Text(
                text = focus ?: "Rest day",
                style = MaterialTheme.typography.displaySmall,
                color = if (focus != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Stats row.
            if (todayTotals.sets > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    StatBlock(value = "${todayTotals.sets}", label = "sets")
                    StatBlock(value = "${todayTotals.exercises}", label = "exercises")
                    StatBlock(value = formatVolumeBrief(todayTotals), label = "volume")
                }
            } else {
                Text(
                    text = "Nothing logged yet today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Primary action pill inside the card.
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
                    text = when {
                        isActive -> "Continue session"
                        todayTotals.sets > 0 -> "Resume workout"
                        else -> "Start workout"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun HomeWeeklyBars(
    values: List<Int>,
    labels: List<String>,
    onClick: () -> Unit,
) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val ink = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val today = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = values.size
                if (n == 0) return@Canvas
                val gap = 14f
                val barWidth = (size.width - gap * (n - 1)) / n
                val r = CornerRadius(barWidth / 4f, barWidth / 4f)
                for (i in 0 until n) {
                    val x = i * (barWidth + gap)
                    drawRoundRect(
                        color = track,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = r,
                    )
                    val ratio = values[i] / max.toFloat()
                    if (ratio > 0f) {
                        val h = size.height * ratio
                        drawRoundRect(
                            color = if (i == today) ink else ink.copy(alpha = 0.5f),
                            topLeft = Offset(x, size.height - h),
                            size = Size(barWidth, h),
                            cornerRadius = r,
                        )
                    }
                }
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

private fun formatVolumeBrief(totals: com.example.fitness_tracker.log.TodayTotals): String {
    val v = totals.volume
    return when {
        v.weightKgReps >= 1000 -> "%.1fk".format(v.weightKgReps / 1000.0)
        v.weightKgReps > 0 -> v.weightKgReps.toInt().toString()
        v.durationSec > 0 -> "${v.durationSec / 60}m"
        v.distanceMeters >= 1000 -> "%.1fkm".format(v.distanceMeters / 1000.0)
        v.distanceMeters > 0 -> "${v.distanceMeters}m"
        else -> "—"
    }
}
