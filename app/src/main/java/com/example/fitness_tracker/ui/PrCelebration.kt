package com.example.fitness_tracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class Confetto(
    val xFrac: Float,
    val startDelay: Float,
    val driftFrac: Float,
    val fall: Float,
    val size: Float,
    val color: Color,
    val rot: Float,
    val rotSpeed: Float,
)

/**
 * Full-screen, one-shot celebration: a confetti burst plus a centred "New PR"
 * banner. Hand-rolled on Canvas so it pulls in no animation dependency. Calls
 * [onDone] once the burst has fully played so the caller can clear it.
 */
@Composable
fun PrCelebrationOverlay(
    exerciseName: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) {
    val palette = listOf(
        Color(0xFF5B3FFF), // brand violet
        Color(0xFF9B89FF), // violet light
        Color(0xFFE76F51), // streak coral
        Color(0xFFD9A341), // PR gold
        Color(0xFF0EA5A0), // plan teal
        Color(0xFFE89B3D), // diet amber
    )
    // Seeded once per overlay instance so the pattern is stable across frames.
    val confetti = remember(exerciseName) {
        val rng = Random(exerciseName.hashCode() xor 0x9E3779B1.toInt())
        List(90) {
            Confetto(
                xFrac = rng.nextFloat(),
                startDelay = rng.nextFloat() * 0.25f,
                driftFrac = (rng.nextFloat() - 0.5f) * 0.5f,
                fall = 0.85f + rng.nextFloat() * 0.5f,
                size = 8f + rng.nextFloat() * 12f,
                color = palette[rng.nextInt(palette.size)],
                rot = rng.nextFloat() * 360f,
                rotSpeed = (rng.nextFloat() - 0.5f) * 4f,
            )
        }
    }

    val progress = remember { Animatable(0f) }
    val bannerScale = remember { Animatable(0f) }
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(exerciseName) {
        bannerScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
        progress.animateTo(1f, tween(durationMillis = 1600))
        currentOnDone()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = progress.value
            confetti.forEach { c ->
                val local = ((p - c.startDelay) / (1f - c.startDelay)).coerceIn(0f, 1f)
                if (local <= 0f) return@forEach
                // Fade out over the last quarter of the flight.
                val alpha = (1f - ((local - 0.75f) / 0.25f)).coerceIn(0f, 1f)
                val x = c.xFrac * size.width + c.driftFrac * size.width * local
                val y = -40f + local * (size.height + 80f) * c.fall
                val angle = c.rot + c.rotSpeed * local * 360f
                rotate(degrees = angle, pivot = Offset(x, y)) {
                    drawRect(
                        color = c.color.copy(alpha = alpha),
                        topLeft = Offset(x - c.size / 2f, y - c.size / 2f),
                        size = Size(c.size, c.size * 0.6f),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .graphicsLayer {
                    scaleX = bannerScale.value
                    scaleY = bannerScale.value
                    alpha = bannerScale.value
                },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🏆", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "New PR!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFD9A341),
                )
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
