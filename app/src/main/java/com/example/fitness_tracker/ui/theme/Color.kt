package com.example.fitness_tracker.ui.theme

import androidx.compose.ui.graphics.Color

// Restrained palette: near-white / near-black surfaces and ink,
// with a single green accent reserved for primary actions.
// Decorative color appears only via the muscle-group palette.

// Light scheme
val Primary = Color(0xFF5B3FFF)              // brand deep violet (CTAs only)
val OnPrimary = Color(0xFFFFFFFF)
// `primaryContainer` is intentionally NOT a green tint. Many surfaces in M3
// (selected tiles, today-pill, time picker, avatar bg, etc.) read this token —
// keeping it green would paint the whole app pale mint. We use a neutral gray
// here so brand green appears only on actual primary actions.
val PrimaryContainer = Color(0xFFEDEDED)
val OnPrimaryContainer = Color(0xFF111111)

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
val PrimaryDark = Color(0xFF9B89FF)           // brand violet, lifted for dark
val OnPrimaryDark = Color(0xFF1A0F4F)
// Neutralized — see light-scheme comment above.
val PrimaryContainerDark = Color(0xFF262626)
val OnPrimaryContainerDark = Color(0xFFEDEDED)

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

// Pitch-black surfaces in dark mode. SurfaceVariant lifts to #161616 so
// chips / cards / "subtle differences" still have a visible step against
// the pure-black background without losing the OLED-friendly black canvas.
val BackgroundDark = Color(0xFF000000)
val OnBackgroundDark = Color(0xFFECECEC)
val SurfaceDark = Color(0xFF000000)
val OnSurfaceDark = Color(0xFFECECEC)
val SurfaceVariantDark = Color(0xFF161616)
val OnSurfaceVariantDark = Color(0xFFB5B5B5)
val OutlineDark = Color(0xFF555555)

// --- Per-feature accents ---
// Each tab carries one signature color so the user can identify a screen at a
// glance. Brand violet stays reserved for primary CTAs and the nav-bar pill.
//
// Picks are based on category convention:
//   • Plan → teal: distinguishes from brand violet while still reading as a
//     "cool / generative" hue. Linear's accent and Apple's Notes are similar.
//   • Diet → amber:  MyFitnessPal / Yuka / Cronometer all warm-color food
//   • Stats → steel blue: Whoop / Apple Health / GitHub Insights use cool tones
//     for analytics surfaces
//
// Each has a light (`AccentX`) and dark (`AccentXDark`) variant tuned for
// AA-contrast against the surface in their respective scheme.

val AccentPlan = Color(0xFF0EA5A0)            // teal (AI / generated content)
val AccentPlanDark = Color(0xFF5EE2D9)
val AccentPlanContainer = Color(0xFFD8F3F1)
val AccentPlanContainerDark = Color(0xFF0E3F3D)
val OnAccentPlanContainer = Color(0xFF073B39)
val OnAccentPlanContainerDark = Color(0xFFD8F3F1)

val AccentDiet = Color(0xFFE89B3D)            // amber (food / nutrition)
val AccentDietDark = Color(0xFFF4B870)
val AccentDietContainer = Color(0xFFFCEAD0)
val AccentDietContainerDark = Color(0xFF513413)
val OnAccentDietContainer = Color(0xFF4A2E08)
val OnAccentDietContainerDark = Color(0xFFFCEAD0)

val AccentStats = Color(0xFF4A6FA5)           // steel blue (analytics)
val AccentStatsDark = Color(0xFF7FA0CF)
val AccentStatsContainer = Color(0xFFD9E3F4)
val AccentStatsContainerDark = Color(0xFF24365A)
val OnAccentStatsContainer = Color(0xFF11253F)
val OnAccentStatsContainerDark = Color(0xFFD9E3F4)

// Special-purpose accents (cross-cutting state, used sparingly).
val AccentPr = Color(0xFFD9A341)              // gold (personal records)
val AccentPrDark = Color(0xFFE6C063)
val AccentStreak = Color(0xFFE76F51)          // coral (streak indicator)
val AccentStreakDark = Color(0xFFF09279)

// Resolve the right accent for the current Material theme.
// Caller does: val planAccent = featureAccent(Feature.PLAN)
//
// Note: Compose's `isSystemInDarkTheme()` is the right call site, but we need
// MaterialTheme context to be resolved. The composable wrapper below makes
// that ergonomic.

enum class Feature { PLAN, DIET, STATS, PR, STREAK }

data class FeatureAccent(val main: Color, val container: Color, val onContainer: Color)

@androidx.compose.runtime.Composable
fun featureAccent(feature: Feature): FeatureAccent {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    return when (feature) {
        Feature.PLAN -> FeatureAccent(
            main = if (dark) AccentPlanDark else AccentPlan,
            container = if (dark) AccentPlanContainerDark else AccentPlanContainer,
            onContainer = if (dark) OnAccentPlanContainerDark else OnAccentPlanContainer,
        )
        Feature.DIET -> FeatureAccent(
            main = if (dark) AccentDietDark else AccentDiet,
            container = if (dark) AccentDietContainerDark else AccentDietContainer,
            onContainer = if (dark) OnAccentDietContainerDark else OnAccentDietContainer,
        )
        Feature.STATS -> FeatureAccent(
            main = if (dark) AccentStatsDark else AccentStats,
            container = if (dark) AccentStatsContainerDark else AccentStatsContainer,
            onContainer = if (dark) OnAccentStatsContainerDark else OnAccentStatsContainer,
        )
        Feature.PR -> FeatureAccent(
            main = if (dark) AccentPrDark else AccentPr,
            container = if (dark) AccentPrDark.copy(alpha = 0.18f) else AccentPr.copy(alpha = 0.18f),
            onContainer = if (dark) AccentPrDark else AccentPr,
        )
        Feature.STREAK -> FeatureAccent(
            main = if (dark) AccentStreakDark else AccentStreak,
            container = if (dark) AccentStreakDark.copy(alpha = 0.18f) else AccentStreak.copy(alpha = 0.18f),
            onContainer = if (dark) AccentStreakDark else AccentStreak,
        )
    }
}

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
