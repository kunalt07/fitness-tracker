package com.example.fitness_tracker.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness_tracker.data.Exercise
import com.example.fitness_tracker.data.ExerciseSeriesPoint
import com.example.fitness_tracker.data.MuscleGroupTally
import com.example.fitness_tracker.data.PersonalRecord
import com.example.fitness_tracker.data.VolumeTotals
import com.example.fitness_tracker.ui.PillChip
import com.example.fitness_tracker.ui.ScreenTitle
import com.example.fitness_tracker.ui.SectionLabel
import com.example.fitness_tracker.ui.theme.muscleGroupColor

@Composable
fun StatsScreen(
    contentPadding: PaddingValues,
    viewModel: StatsViewModel = viewModel(),
) {
    val s by viewModel.state.collectAsState()
    val prs by viewModel.personalRecords.collectAsState()
    val chartable by viewModel.chartableExercises.collectAsState()
    val selectedId by viewModel.selectedExerciseId.collectAsState()
    val series by viewModel.exerciseSeries.collectAsState()
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
        ScreenTitle("Stats")

        WindowChips(
            current = s.window,
            onSelect = viewModel::selectWindow,
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BigStatRow(
                values = listOf(
                    "Streak" to "${s.streakDays}d",
                    "Sessions" to "${s.totalSessions}",
                    "Sets" to "${s.totalSets}",
                ),
            )

            VolumeCard(volume = s.volume)

            if (s.muscleBalance.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Muscle balance")
                    MuscleBalanceBar(tallies = s.muscleBalance)
                }
            }

            if (prs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Personal records")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        prs.forEach { pr -> PersonalRecordRow(pr) }
                    }
                }
            }

            if (chartable.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Progress")
                    ExercisePicker(
                        exercises = chartable,
                        selectedId = selectedId,
                        onSelect = viewModel::selectExercise,
                    )
                    if (selectedId != null) {
                        ProgressChart(points = series)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("This week")
                WeeklyBars(values = s.weeklySetsPerDay, labels = s.weekDayLabels)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Sessions")
                com.example.fitness_tracker.history.SessionsList()
            }
        }
    }
}

@Composable
private fun WindowChips(current: StatsWindow, onSelect: (StatsWindow) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(StatsWindow.entries.toList()) { w ->
            PillChip(
                selected = current == w,
                label = w.label,
                onClick = { onSelect(w) },
            )
        }
    }
}

@Composable
private fun BigStatRow(values: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        values.forEach { (label, value) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VolumeCard(volume: VolumeTotals) {
    val rows = buildList {
        if (volume.weightKgReps > 0) add("Weight moved" to "${formatKg(volume.weightKgReps)} kg")
        if (volume.durationSec > 0) add("Time held" to formatDuration(volume.durationSec))
        if (volume.distanceMeters > 0) add("Distance" to formatDistance(volume.distanceMeters))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Volume")
        if (rows.isEmpty()) {
            Text(
                "No sets logged in this window.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleBalanceBar(tallies: List<MuscleGroupTally>) {
    val total = tallies.sumOf { it.sets }.coerceAtLeast(1)
    val ranked = tallies.take(8)
    val colors = ranked.map { muscleGroupColor(it.muscleGroup) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var x = 0f
                val r = CornerRadius(size.height / 2f, size.height / 2f)
                ranked.forEachIndexed { i, t ->
                    val w = size.width * (t.sets / total.toFloat())
                    drawRoundRect(
                        color = colors[i],
                        topLeft = Offset(x, 0f),
                        size = Size(w, size.height),
                        cornerRadius = r,
                    )
                    x += w
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ranked.forEachIndexed { i, t ->
                val pct = (t.sets * 100 / total)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[i], shape = CircleShape),
                    )
                    Text(
                        text = t.muscleGroup,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f),
                    )
                    Text(
                        text = "${t.sets} sets · $pct%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyBars(values: List<Int>, labels: List<String>) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val ink = MaterialTheme.colorScheme.onSurface
    val track = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelMedium

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = values.size
                if (n == 0) return@Canvas
                val gap = 12f
                val barWidth = (size.width - gap * (n - 1)) / n
                val cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f)
                for (i in 0 until n) {
                    val x = i * (barWidth + gap)
                    drawRoundRect(
                        color = track,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = cornerRadius,
                    )
                    val ratio = values[i] / max.toFloat()
                    if (ratio > 0f) {
                        val h = size.height * ratio
                        drawRoundRect(
                            color = ink,
                            topLeft = Offset(x, size.height - h),
                            size = Size(barWidth, h),
                            cornerRadius = cornerRadius,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { l ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(l, style = labelStyle, color = labelColor)
                }
            }
        }
    }
}

private fun formatKg(kg: Double): String =
    when {
        kg >= 10_000 -> "%,.0f".format(kg)
        kg >= 1_000 -> "%,.0f".format(kg)
        kg % 1.0 == 0.0 -> kg.toInt().toString()
        else -> "%.1f".format(kg)
    }

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

private fun formatDistance(meters: Long): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000.0) else "$meters m"

@Composable
private fun PersonalRecordRow(pr: PersonalRecord) {
    val tint = com.example.fitness_tracker.ui.theme.muscleGroupColor(pr.muscleGroup)
    val prAccent = com.example.fitness_tracker.ui.theme.featureAccent(
        com.example.fitness_tracker.ui.theme.Feature.PR,
    )
    val daysAgo = ((System.currentTimeMillis() - pr.performedAt) / (24L * 60 * 60 * 1000)).coerceAtLeast(0)
    val freshness = when (daysAgo) {
        0L -> "today"
        1L -> "yesterday"
        else -> "${daysAgo}d ago"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .width(3.dp)
                .height(36.dp)
                .background(tint, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                pr.exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${formatKg(pr.weightKg)} kg × ${pr.reps}  ·  $freshness",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "≈ ${formatKg(pr.estimatedOneRm)} kg",
            style = MaterialTheme.typography.titleMedium,
            color = prAccent.main,
        )
    }
}

@Composable
private fun ExercisePicker(
    exercises: List<Exercise>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(exercises, key = { it.id }) { ex ->
            PillChip(
                selected = ex.id == selectedId,
                label = ex.name,
                onClick = {
                    if (ex.id == selectedId) onSelect(null) else onSelect(ex.id)
                },
            )
        }
    }
}

@Composable
private fun ProgressChart(points: List<ExerciseSeriesPoint>) {
    if (points.isEmpty()) {
        Text(
            "No data yet — log a few sets and check back.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val first = points.first()
    val last = points.last()
    val deltaKg = last.estimatedOneRm - first.estimatedOneRm
    val deltaText = when {
        points.size < 2 -> ""
        deltaKg > 0 -> "+${formatKg(deltaKg)} kg"
        deltaKg < 0 -> "${formatKg(deltaKg)} kg"
        else -> "no change"
    }
    val statsAccent = com.example.fitness_tracker.ui.theme.featureAccent(
        com.example.fitness_tracker.ui.theme.Feature.STATS,
    )
    val ink = statsAccent.main
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "Estimated 1RM",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (deltaText.isNotEmpty()) {
                Text(
                    deltaText,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (deltaKg >= 0) statsAccent.main
                    else MaterialTheme.colorScheme.error,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (points.isEmpty()) return@Canvas
                val ys = points.map { it.estimatedOneRm.toFloat() }
                val xs = points.map { it.performedAt }
                val yMin = ys.min()
                val yMax = ys.max()
                val ySpan = (yMax - yMin).takeIf { it > 0f } ?: 1f
                val xMin = xs.min()
                val xMax = xs.max()
                val xSpan = (xMax - xMin).takeIf { it > 0L } ?: 1L

                val padTop = 8f
                val padBottom = 8f
                val plotH = size.height - padTop - padBottom

                fun project(p: ExerciseSeriesPoint): Offset {
                    val xRatio = if (points.size == 1) 0.5f
                    else (p.performedAt - xMin).toFloat() / xSpan
                    val yRatio = (p.estimatedOneRm.toFloat() - yMin) / ySpan
                    return Offset(
                        x = xRatio * size.width,
                        y = padTop + (1f - yRatio) * plotH,
                    )
                }

                // Baseline track at the bottom.
                drawRoundRect(
                    color = track,
                    topLeft = Offset(0f, size.height - 2f),
                    size = Size(size.width, 2f),
                    cornerRadius = CornerRadius(1f, 1f),
                )

                // Line segments.
                val coords = points.map(::project)
                for (i in 0 until coords.size - 1) {
                    drawLine(
                        color = ink,
                        start = coords[i],
                        end = coords[i + 1],
                        strokeWidth = 4f,
                    )
                }
                // Dots.
                coords.forEach { c ->
                    drawCircle(color = ink, radius = 5f, center = c)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${formatKg(first.weightKg)} kg × ${first.reps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${formatKg(last.weightKg)} kg × ${last.reps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

