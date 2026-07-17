package com.example.fitness_tracker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Sheet state used everywhere in the app: starts hidden, only the fully-expanded
 * resting position is allowed (no half-height "partially expanded" state).
 *
 * Wraps the new [rememberBottomSheetState] API so call sites stay readable;
 * replaces the deprecated `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFullSheetState(): SheetState =
    rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    // Top padding kept tight (8dp) — each screen already adds the system
    // status-bar inset above this composable, so any extra padding here
    // shows up as a visible gap between the status bar and the title.
    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

/**
 * Shared press-feedback: dips to [PRESS_SCALE] while held, springs back on release.
 * Draw-only ([graphicsLayer]) so it never triggers relayout. Wire the returned
 * source into the button's `interactionSource` and apply [Modifier.graphicsLayer].
 */
private const val PRESS_SCALE = 0.96f

@Composable
private fun rememberPressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESS_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    return scale
}

@Composable
fun PillCta(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle? = null,
) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .height(52.dp)
            .let {
                if (elevated) it.shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(50),
                    clip = false,
                ) else it
            },
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(label, style = textStyle ?: MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BottomCtaBar(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    // Minimal floating CTA: tighter inset, flat (no shadow), thinner than before.
    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
        PillCta(label = label, enabled = enabled, onClick = onClick, elevated = false)
    }
}

/**
 * Secondary pill — same height/shape as [PillCta] but outlined with a
 * transparent fill, so it reads as quieter on a transparent backdrop.
 */
@Composable
fun PillCtaSecondary(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(52.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun QuietTextButton(
    label: String,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val scale = rememberPressScale(source)
    TextButton(
        onClick = onClick,
        interactionSource = source,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PillChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    // Pop-and-settle on select: snap up, then springy bounce back to rest.
    val scale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            scale.snapTo(1.08f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }
    FilterChip(
        modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
            )
        },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
        ),
        border = null,
    )
}

@Composable
fun EmptyHint(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
