package com.example.fitness_tracker.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.data.SetWithExerciseRow
import com.example.fitness_tracker.data.WorkoutSession
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Inline session list — used inside Stats to show recent activity.
 * Self-contained: holds its own VM, snackbar wiring, expansion state.
 */
@Composable
fun SessionsList(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(),
) {
    val sessions by viewModel.sessions.collectAsState()
    val expandedId by viewModel.expandedSessionId.collectAsState()
    val expandedSets by viewModel.expandedSets.collectAsState()
    val snackbarHost = com.example.fitness_tracker.LocalSnackbarHost.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    if (sessions.isEmpty()) {
        Text(
            "No sessions yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        sessions.forEach { session ->
            SessionRow(
                session = session,
                expanded = expandedId == session.id,
                sets = if (expandedId == session.id) expandedSets else emptyList(),
                onToggle = { viewModel.toggle(session.id) },
                onDelete = {
                    scope.launch {
                        val restore = viewModel.deleteWithUndo(session.id)
                        if (restore != null) {
                            com.example.fitness_tracker.postUndoSnackbar(
                                host = snackbarHost,
                                scope = scope,
                                message = "Session deleted",
                                restore = restore,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SessionRow(
    session: WorkoutSession,
    expanded: Boolean,
    sets: List<SetWithExerciseRow>,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatDate(session.startedAt),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    formatTime(session.startedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Delete session",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (sets.isEmpty()) {
                    Text(
                        "No sets in this session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    sets.forEach { s ->
                        val tint = com.example.fitness_tracker.ui.theme.muscleGroupColor(s.muscleGroup)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(width = 3.dp, height = 24.dp)
                                    .background(
                                        color = tint,
                                        shape = RoundedCornerShape(2.dp),
                                    ),
                            )
                            Text(
                                s.exerciseName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = com.example.fitness_tracker.log.formatSetSummary(s),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(epochMs: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs))

private fun formatTime(epochMs: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMs))
