package com.example.fitness_tracker.profile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import com.example.fitness_tracker.ui.theme.resolveDarkTheme
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
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.fitness_tracker.LocalSnackbarHost
import com.example.fitness_tracker.auth.AuthViewModel
import com.example.fitness_tracker.backup.BackupRepository
import com.example.fitness_tracker.backup.BackupResult
import com.example.fitness_tracker.log.LogViewModel
import com.example.fitness_tracker.plan.PlanViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.fitness_tracker.ui.MinimalTextField
import com.example.fitness_tracker.ui.PillCta
import com.example.fitness_tracker.ui.theme.ThemeMode
import com.example.fitness_tracker.ui.theme.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var confirmingRestore by remember { mutableStateOf<android.net.Uri?>(null) }

    val context = LocalContext.current
    val themeStore = remember(context) { ThemeModeStore.get(context) }
    val themeMode by themeStore.mode.collectAsState()
    val backupRepo = remember(context) { BackupRepository.get(context) }
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val payload = backupRepo.export()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Couldn't open file for writing")
                }.isSuccess
            }
            snackbarHost.showSnackbar(
                if (ok) "Backup saved" else "Couldn't save backup"
            )
        }
    }

    val restorePickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        confirmingRestore = uri
    }

    val scroll = rememberScrollState()
    // FitnessApp's Scaffold Box already pads the NavHost by scaffoldPadding,
    // so we only consume the bottom inset here — re-applying the top inset
    // double-pads and leaves a visible gap above the back-arrow toolbar row.
    val bottomInset = contentPadding.calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
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
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        ThemeToggleRow(
            isDark = themeMode.resolveDarkTheme(),
            onChange = { dark -> themeStore.set(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) },
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Data section — backup/restore. Local-first means a phone wipe loses
        // everything; this is the user's safety net.
        SectionLabel("Data", modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(8.dp))

        SettingsRow(
            icon = Icons.Outlined.CloudUpload,
            title = "Export data",
            subtitle = "Save a JSON backup of everything",
            onClick = { exportLauncher.launch(defaultBackupFileName()) },
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        SettingsRow(
            icon = Icons.Outlined.CloudDownload,
            title = "Restore from backup",
            subtitle = "Replaces everything with a saved JSON file",
            onClick = { restorePickLauncher.launch(arrayOf("application/json", "*/*")) },
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

    confirmingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmingRestore = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingRestore = null
                        scope.launch {
                            val text = withContext(Dispatchers.IO) {
                                runCatching {
                                    context.contentResolver.openInputStream(uri)
                                        ?.bufferedReader(Charsets.UTF_8)
                                        ?.use { it.readText() }
                                        ?: error("Couldn't open backup file")
                                }
                            }
                            val outcome = text.fold(
                                onSuccess = { backupRepo.import(it) },
                                onFailure = { BackupResult.Failure(it.message ?: "Read failed") },
                            )
                            val msg = when (outcome) {
                                is BackupResult.Success -> "Restored ${outcome.rowCount} rows"
                                is BackupResult.Failure -> outcome.message
                            }
                            snackbarHost.showSnackbar(msg)
                        }
                    },
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRestore = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Restore from backup?") },
            text = {
                Text(
                    "This will replace ALL workouts, sessions, meals, and " +
                        "settings with the backup's contents. This can't be undone.",
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

/** Theme row with a sliding sun/moon toggle — sun = light, moon = dark. The
 * thumb slides between the two icons; tapping either side (or the track) flips. */
@Composable
private fun ThemeToggleRow(
    isDark: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val thumbShift by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "theme-thumb",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Brightness6,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.size(20.dp))
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        // Sliding track: two icon slots (sun / moon) with a highlighted thumb.
        val slot = 40.dp
        Box(
            modifier = Modifier
                .width(slot * 2)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onChange(!isDark) },
            contentAlignment = Alignment.CenterStart,
        ) {
            // Thumb.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(slot)
                    .offset { IntOffset((thumbShift * slot.toPx()).toInt(), 0) }
                    .padding(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(slot), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = "Light theme",
                        tint = if (!isDark) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(modifier = Modifier.size(slot), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.DarkMode,
                        contentDescription = "Dark theme",
                        tint = if (isDark) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
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

private fun defaultBackupFileName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
    return "vector-backup-$stamp.json"
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
