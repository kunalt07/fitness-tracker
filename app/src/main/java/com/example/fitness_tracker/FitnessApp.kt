package com.example.fitness_tracker

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.fitness_tracker.log.LogScreen
import com.example.fitness_tracker.plan.PlanScreen
import com.example.fitness_tracker.stats.StatsScreen

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}


/**
 * Show an "X deleted · Undo" snackbar. Caller has already done the delete; if the
 * user taps Undo, we invoke [restore]. Returns immediately; runs in [scope].
 */
fun postUndoSnackbar(
    host: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    message: String,
    restore: suspend () -> Unit,
) {
    scope.launch {
        val result = host.showSnackbar(
            message = message,
            actionLabel = "Undo",
            duration = androidx.compose.material3.SnackbarDuration.Short,
            withDismissAction = false,
        )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            restore()
        }
    }
}

internal enum class TopLevelDest(
    val route: String,
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
) {
    Home("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Plan("plan", "Plan", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb),
    // M3 rounded variant — solid filled dumbbell, both states use it
    // (selected stays solid; unselected just changes tint).
    Log("log", "Log", Icons.Rounded.FitnessCenter, Icons.Rounded.FitnessCenter),
    Diet("diet", "Diet", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    Stats("stats", "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
}

@Composable
fun FitnessApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHost = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackbarHost provides snackbarHost) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
    ) { scaffoldPadding ->
        // Bottom inset that screens should reserve so their content doesn't get
        // covered by the floating nav: bar height + system gesture inset + breathing room.
        val sysBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val screenPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding(),
            bottom = (NAV_BAR_HEIGHT_DP.dp + 24.dp + sysBottom),
        )
        Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
            NavHost(
                navController = navController,
                startDestination = TopLevelDest.Home.route,
            ) {
                composable(TopLevelDest.Home.route) {
                    com.example.fitness_tracker.home.HomeScreen(
                        contentPadding = screenPadding,
                        onNavigate = { dest ->
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenProfile = {
                            navController.navigate("profile") {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable("profile") {
                    com.example.fitness_tracker.profile.ProfileScreen(
                        contentPadding = screenPadding,
                        onBack = { navController.popBackStack() },
                        onOpenSplit = { navController.popBackStack(); navController.navigate(TopLevelDest.Plan.route) },
                        onOpenReminder = { navController.popBackStack(); navController.navigate(TopLevelDest.Plan.route) },
                    )
                }
                composable(TopLevelDest.Plan.route) {
                    PlanScreen(
                        contentPadding = screenPadding,
                        onPlanApplied = {
                            navController.navigate(TopLevelDest.Log.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
                composable(TopLevelDest.Log.route) { LogScreen(contentPadding = screenPadding) }
                composable(TopLevelDest.Diet.route) {
                    com.example.fitness_tracker.diet.DietScreen(contentPadding = screenPadding)
                }
                composable(TopLevelDest.Stats.route) { StatsScreen(contentPadding = screenPadding) }
            }

            // Floating nav bar overlays the content; only the pill chrome is opaque.
            Box(
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                FloatingNavBar(
                    currentRoute = currentRoute,
                    hierarchy = backStackEntry?.destination?.hierarchy,
                    onNavigate = { dest ->
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
    }
}

private const val NAV_BAR_HEIGHT_DP = 64
private const val SWIPE_THRESHOLD_PX = 60f

@Composable
private fun FloatingNavBar(
    currentRoute: String?,
    hierarchy: Sequence<androidx.navigation.NavDestination>?,
    onNavigate: (TopLevelDest) -> Unit,
) {
    val sysBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val entries = TopLevelDest.entries
    val selectedIndex = entries.indexOfFirst { dest ->
        currentRoute?.let { hierarchy?.any { d -> d.route == dest.route } } == true
    }.let { if (it < 0) 0 else it }

    // Animated float so the pill slides smoothly when selection changes.
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nav-pill-position",
    )

    val density = LocalDensity.current
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = sysBarPadding.calculateBottomPadding())
            .padding(horizontal = 48.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(28.dp),
                )
                .pointerInput(selectedIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = {
                            val steps = (dragAccumulator / SWIPE_THRESHOLD_PX).roundToInt()
                            if (steps != 0) {
                                val target = (selectedIndex + steps)
                                    .coerceIn(0, entries.size - 1)
                                if (target != selectedIndex) onNavigate(entries[target])
                            }
                            dragAccumulator = 0f
                        },
                        onDragCancel = { dragAccumulator = 0f },
                        onHorizontalDrag = { _, delta -> dragAccumulator += delta },
                    )
                },
            shape = RoundedCornerShape(28.dp),
            // Fully opaque so no content scrolls visibly behind it. Soft
            // shadow only — anything stronger reads as a "stacked" layer.
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
        ) {
            // Each tab is a fixed-width slot so the bar's total width is constant
            // and the sliding pill always lines up with exactly one slot.
            val slotWidthDp = 64.dp
            BoxWithConstraints(
                modifier = Modifier
                    .height(NAV_BAR_HEIGHT_DP.dp)
                    .width(slotWidthDp * entries.size)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                val slotWidthPx = with(density) { (maxWidth / entries.size).toPx() }
                val pillOffsetX = (animatedIndex * slotWidthPx).roundToInt()

                // Sliding selection background.
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pillOffsetX, 0) }
                        .width(slotWidthDp)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(22.dp),
                        ),
                )

                // Tap targets — same shape for every tab, layered above the pill.
                Row(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    entries.forEachIndexed { index, dest ->
                        NavPill(
                            dest = dest,
                            selected = index == selectedIndex,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    if (index != selectedIndex) onNavigate(dest)
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavPill(
    dest: TopLevelDest,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) dest.iconFilled else dest.iconOutlined,
            contentDescription = dest.label,
            tint = tint,
        )
        Text(
            text = dest.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
