package com.example.fitness_tracker.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.auth.AuthViewModel
import com.example.fitness_tracker.log.LogViewModel
import com.example.fitness_tracker.plan.PlanViewModel
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillCta

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenSplit: () -> Unit,
    onOpenReminder: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    logViewModel: LogViewModel = viewModel(),
    planViewModel: PlanViewModel = viewModel(),
) {
    val profile by authViewModel.profile.collectAsState()
    val todayTotals by logViewModel.todayTotals.collectAsState()
    val reminder by planViewModel.reminderSettings.collectAsState()
    val split by planViewModel.weeklySplit.collectAsState()

    var confirmingClear by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()
    val topInset = contentPadding.calculateTopPadding()
    val bottomInset = contentPadding.calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(top = topInset)
            .padding(bottom = bottomInset),
    ) {
        // Top bar with back button and title.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { editingProfile = true }) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Identity row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarMark(
                initials = (profile?.name).orEmpty().initials(),
                size = 64.dp,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile?.name ?: "Guest",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                profile?.email?.takeIf { it.isNotBlank() }?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats card.
        StatCardRow(
            modifier = Modifier.padding(horizontal = 24.dp),
            stats = listOf(
                "Today" to "${todayTotals.sets}",
                "Exercises" to "${todayTotals.exercises}",
                "Volume" to formatVolume(todayTotals),
            ),
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Settings rows.
        SectionLabel("Settings", modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(8.dp))

        SettingsRow(
            icon = Icons.Outlined.CalendarToday,
            title = "Weekly split",
            subtitle = run {
                val daysSet = split.count { it.focus.isNotBlank() }
                when (daysSet) {
                    0 -> "Not set up yet"
                    7 -> "All 7 days set"
                    else -> "$daysSet of 7 days set"
                }
            },
            onClick = onOpenSplit,
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        SettingsRow(
            icon = Icons.Outlined.NotificationsActive,
            title = "Daily reminder",
            subtitle = reminder?.let { r ->
                if (!r.enabled) "Off"
                else "On · ${formatHourMinute(r.hour, r.minute)}"
            } ?: "Off",
            onClick = onOpenReminder,
        )

        Spacer(modifier = Modifier.height(28.dp))

        SettingsRow(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = "Clear profile",
            subtitle = "Resets your name and email. Workouts stay.",
            tint = MaterialTheme.colorScheme.error,
            onClick = { confirmingClear = true },
        )

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (confirmingClear) {
        AlertDialog(
            onDismissRequest = { confirmingClear = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingClear = false
                        authViewModel.clearProfile()
                    },
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClear = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Clear profile?") },
            text = {
                Text(
                    "Your name and email will be reset. " +
                        "All workouts, sessions, and stats stay intact.",
                )
            },
        )
    }

    if (editingProfile) {
        EditProfileDialog(
            initialName = profile?.name.orEmpty(),
            initialEmail = profile?.email.orEmpty(),
            onDismiss = { editingProfile = false },
            onSave = { name, email ->
                authViewModel.saveProfile(name, email)
                editingProfile = false
            },
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), email.trim()) },
                enabled = name.trim().isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MinimalTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = "Name",
                    singleLine = true,
                )
                MinimalTextField(
                    value = email,
                    onValueChange = { email = it.take(80) },
                    label = "Email (optional)",
                    singleLine = true,
                )
            }
        },
    )
}

@Composable
private fun AvatarMark(
    initials: String,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatCardRow(
    modifier: Modifier = Modifier,
    stats: List<Pair<String, String>>,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        stats.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = tint,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun String.initials(): String {
    if (isBlank()) return ""
    val parts = trim().split(Regex("\\s+|@")).filter { it.isNotBlank() }
    return parts.take(2).joinToString("") { it.first().uppercaseChar().toString() }
}

private fun formatHourMinute(hour: Int, minute: Int): String {
    val h12 = ((hour + 11) % 12) + 1
    val ampm = if (hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, minute, ampm)
}

private fun formatVolume(totals: com.example.fitness_tracker.log.TodayTotals): String {
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
