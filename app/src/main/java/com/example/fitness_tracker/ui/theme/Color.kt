package com.example.fitness_tracker.ui.theme

import androidx.compose.ui.graphics.Color

// Restrained palette: near-white / near-black surfaces and ink,
// with a single green accent reserved for primary actions.
// Decorative color appears only via the muscle-group palette.

// Light scheme
val Primary = Color(0xFF1F8A4C)              // brand green
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFD7F1E1)
val OnPrimaryContainer = Color(0xFF06321A)

val Secondary = Color(0xFF4A4A4A)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFF1F1F1)
val OnSecondaryContainer = Color(0xFF1A1A1A)

val Tertiary = Color(0xFF2C2C2C)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFEDEDED)
val OnTertiaryContainer = Color(0xFF111111)

val ErrorColor = Color(0xFFB3261E)
val OnErrorColor = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFF9DEDC)
val OnErrorContainer = Color(0xFF410E0B)

val Background = Color(0xFFFCFCFC)
val OnBackground = Color(0xFF111111)
val Surface = Color(0xFFFCFCFC)
val OnSurface = Color(0xFF111111)
val SurfaceVariant = Color(0xFFF2F2F2)
val OnSurfaceVariant = Color(0xFF5C5C5C)
val Outline = Color(0xFFBFBFBF)

// Dark scheme
val PrimaryDark = Color(0xFF7BD7A2)           // brand green, lifted for dark
val OnPrimaryDark = Color(0xFF003918)
val PrimaryContainerDark = Color(0xFF0F5731)
val OnPrimaryContainerDark = Color(0xFFC6F1D6)

val SecondaryDark = Color(0xFFBFBFBF)
val OnSecondaryDark = Color(0xFF1A1A1A)
val SecondaryContainerDark = Color(0xFF2A2A2A)
val OnSecondaryContainerDark = Color(0xFFE6E6E6)

val TertiaryDark = Color(0xFFD6D6D6)
val OnTertiaryDark = Color(0xFF111111)
val TertiaryContainerDark = Color(0xFF262626)
val OnTertiaryContainerDark = Color(0xFFEDEDED)

val ErrorColorDark = Color(0xFFF2B8B5)
val OnErrorColorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

val BackgroundDark = Color(0xFF101010)
val OnBackgroundDark = Color(0xFFECECEC)
val SurfaceDark = Color(0xFF101010)
val OnSurfaceDark = Color(0xFFECECEC)
val SurfaceVariantDark = Color(0xFF1F1F1F)
val OnSurfaceVariantDark = Color(0xFFB5B5B5)
val OutlineDark = Color(0xFF555555)

// --- Muscle-group palette ---
// Stable mapping: each canonical group gets a recognizable hue. Unknown groups
// fall through to a deterministic pick from FALLBACK_PALETTE so the same name
// always lights up the same color.

private val MUSCLE_COLORS = mapOf(
    "chest" to Color(0xFFE05E5E),       // red
    "back" to Color(0xFF3D8BFD),        // blue
    "legs" to Color(0xFFE8A33D),        // amber
    "shoulders" to Color(0xFF8C6BE0),   // violet
    "arms" to Color(0xFFE0735E),        // coral
    "biceps" to Color(0xFFC25E78),      // rose
    "triceps" to Color(0xFFD9686C),     // brick
    "core" to Color(0xFF14B8A6),        // teal
    "cardio" to Color(0xFFE53980),      // pink
    "rest" to Color(0xFF9AA0A6),        // grey
    "custom" to Color(0xFF6B7280),      // slate
)

private val FALLBACK_PALETTE = listOf(
    Color(0xFF1F8A4C), Color(0xFFE05E5E), Color(0xFF3D8BFD),
    Color(0xFFE8A33D), Color(0xFF8C6BE0), Color(0xFF14B8A6),
)

fun muscleGroupColor(group: String): Color {
    val key = group.trim().lowercase()
    MUSCLE_COLORS[key]?.let { return it }
    val hash = key.fold(0) { acc, c -> acc * 31 + c.code }
    val idx = ((hash % FALLBACK_PALETTE.size) + FALLBACK_PALETTE.size) % FALLBACK_PALETTE.size
    return FALLBACK_PALETTE[idx]
}

/**
 * Scan a free-text focus phrase (e.g. "Chest + Triceps", "Pull day", "Rest") for the
 * first known muscle keyword. Returns null if nothing matches — the caller can fall
 * back to the default ink color so unknown phrases stay neutral.
 */
fun focusKeywordColor(focus: String): Color? {
    if (focus.isBlank()) return null
    val lower = focus.lowercase()
    // Order matters: prefer the more specific terms first.
    val keywords = listOf(
        "triceps", "biceps", "shoulders", "chest", "back", "legs",
        "arms", "core", "cardio",
    )
    for (k in keywords) {
        if (lower.contains(k)) return MUSCLE_COLORS[k]
    }
    return null
}
