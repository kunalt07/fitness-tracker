package com.example.fitness_tracker.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A rounded-rect placeholder that pulses opacity. Use in clusters with
 * [SkeletonGroup] (or alone) to mock the layout of pending content so the
 * shape lands instantly and only the *content* fades in when it arrives.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    Box(
        modifier = modifier
            .height(height)
            .alpha(alpha)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(cornerRadius),
            ),
    )
}

/** Convenience wrapper to build a stack of skeleton lines with consistent spacing. */
@Composable
fun SkeletonGroup(
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = { content() },
    )
}

/** A two-line placeholder shaped like a labeled item (title + meta). */
@Composable
fun SkeletonRow(
    modifier: Modifier = Modifier,
    titleFraction: Float = 0.6f,
    metaFraction: Float = 0.35f,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(titleFraction), height = 16.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(metaFraction), height = 12.dp)
    }
}

/** A bullet-shaped placeholder with a leading dot. */
@Composable
fun SkeletonBullet(
    modifier: Modifier = Modifier,
    fraction: Float = 0.85f,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkeletonBlock(modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.04f), height = 8.dp, cornerRadius = 4.dp)
        SkeletonBlock(modifier = Modifier.fillMaxWidth(fraction), height = 16.dp)
    }
}
