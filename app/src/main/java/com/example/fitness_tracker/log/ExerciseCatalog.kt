package com.example.fitness_tracker.log

/**
 * Curated per-muscle-group exercise catalog for the workout picker.
 *
 * This is NOT every exercise in existence — it's a broad, hand-picked list of
 * the movements commonly programmed by well-known coaches, bodybuilders and
 * athletes. `recommended` entries are the staples shown by default; the full
 * list (recommended + the rest) is what the picker's search box filters over.
 * Picking a name that isn't in the DB yet creates it on the fly.
 */
data class CatalogExercise(
    val name: String,
    val recommended: Boolean,
    val bodyweight: Boolean = false,
)

// r = recommended (shown by default), e = extra (search-only). bw flags no-gear.
private fun r(name: String, bw: Boolean = false) = CatalogExercise(name, true, bw)
private fun e(name: String, bw: Boolean = false) = CatalogExercise(name, false, bw)

private val CHEST = listOf(
    r("Barbell Bench Press"), r("Incline Barbell Bench Press"), r("Dumbbell Bench Press"),
    r("Incline Dumbbell Press"), r("Weighted Dip"), r("Cable Crossover"),
    r("Machine Chest Press"), r("Pec Deck Fly"), r("Push-Up", bw = true),
    e("Decline Bench Press"), e("Decline Dumbbell Press"), e("Dumbbell Fly"),
    e("Incline Dumbbell Fly"), e("Low-to-High Cable Fly"), e("High-to-Low Cable Fly"),
    e("Svend Press"), e("Landmine Press"), e("Smith Machine Bench Press"),
    e("Floor Press"), e("Wide Push-Up", bw = true), e("Diamond Push-Up", bw = true),
    e("Decline Push-Up", bw = true), e("Archer Push-Up", bw = true),
    e("Plyometric Push-Up", bw = true),
)

private val TRICEPS = listOf(
    r("Close-Grip Bench Press"), r("Tricep Pushdown"), r("Overhead Cable Extension"),
    r("Skull Crusher"), r("Weighted Dip"), r("Dumbbell Overhead Extension"),
    r("Bench Dip", bw = true),
    e("Rope Pushdown"), e("Straight-Bar Pushdown"), e("Reverse-Grip Pushdown"),
    e("EZ-Bar Skull Crusher"), e("Dumbbell Kickback"), e("Cable Kickback"),
    e("JM Press"), e("Tate Press"), e("Diamond Push-Up", bw = true),
    e("Close-Grip Push-Up", bw = true),
)

private val LEGS = listOf(
    r("Back Squat"), r("Front Squat"), r("Romanian Deadlift"), r("Leg Press"),
    r("Bulgarian Split Squat"), r("Walking Lunge"), r("Leg Extension"),
    r("Lying Leg Curl"), r("Hip Thrust"), r("Standing Calf Raise"),
    e("Hack Squat"), e("Goblet Squat"), e("Box Squat"), e("Smith Machine Squat"),
    e("Sumo Deadlift"), e("Stiff-Leg Deadlift"), e("Good Morning"), e("Step-Up"),
    e("Reverse Lunge"), e("Seated Leg Curl"), e("Sissy Squat"), e("Seated Calf Raise"),
    e("Nordic Curl", bw = true), e("Bodyweight Squat", bw = true), e("Jump Squat", bw = true),
    e("Pistol Squat", bw = true), e("Wall Sit", bw = true), e("Glute Bridge", bw = true),
    e("Cossack Squat", bw = true),
)

private val SHOULDERS = listOf(
    r("Overhead Barbell Press"), r("Seated Dumbbell Press"), r("Lateral Raise"),
    r("Arnold Press"), r("Face Pull"), r("Rear Delt Fly"), r("Cable Lateral Raise"),
    e("Military Press"), e("Push Press"), e("Machine Shoulder Press"), e("Front Raise"),
    e("Cable Front Raise"), e("Upright Row"), e("Reverse Pec Deck"),
    e("Bent-Over Lateral Raise"), e("Landmine Press"),
    e("Pike Push-Up", bw = true), e("Handstand Push-Up", bw = true), e("Handstand Hold", bw = true),
)

private val BICEPS = listOf(
    r("Barbell Curl"), r("Dumbbell Curl"), r("Hammer Curl"), r("Incline Dumbbell Curl"),
    r("Preacher Curl"), r("Cable Curl"), r("Chin-Up", bw = true),
    e("EZ-Bar Curl"), e("Concentration Curl"), e("Spider Curl"), e("Drag Curl"),
    e("Reverse Curl"), e("Zottman Curl"), e("Cross-Body Hammer Curl"),
    e("Cable Rope Hammer Curl"), e("Bayesian Cable Curl"), e("21s"),
)

private val CORE = listOf(
    r("Plank", bw = true), r("Hanging Leg Raise", bw = true), r("Cable Crunch"),
    r("Ab Wheel Rollout"), r("Russian Twist", bw = true), r("Bicycle Crunch", bw = true),
    r("Weighted Sit-Up"),
    e("Crunch", bw = true), e("Sit-Up", bw = true), e("V-Up", bw = true),
    e("Hollow Hold", bw = true), e("Dead Bug", bw = true), e("Mountain Climber", bw = true),
    e("Leg Raise", bw = true), e("Toes-to-Bar", bw = true), e("Dragon Flag", bw = true),
    e("Side Plank", bw = true), e("Pallof Press"), e("Decline Sit-Up"),
    e("Cable Woodchopper"), e("Flutter Kicks", bw = true), e("L-Sit", bw = true),
)

private val BACK = listOf(
    r("Deadlift"), r("Pull-Up", bw = true), r("Barbell Row"), r("Lat Pulldown"),
    r("Seated Cable Row"), r("T-Bar Row"), r("Dumbbell Row"), r("Face Pull"),
    e("Chin-Up", bw = true), e("Chest-Supported Row"), e("Pendlay Row"), e("Meadows Row"),
    e("Machine Row"), e("Straight-Arm Pulldown"), e("Wide-Grip Pulldown"),
    e("Close-Grip Pulldown"), e("Rack Pull"), e("Trap-Bar Deadlift"),
    e("Inverted Row", bw = true), e("Superman", bw = true), e("Shrug"), e("Dumbbell Shrug"),
)

/** Muscle group name (matches the grid cards) → its catalog. */
val EXERCISE_CATALOG: Map<String, List<CatalogExercise>> = mapOf(
    "Chest" to CHEST,
    "Triceps" to TRICEPS,
    "Legs" to LEGS,
    "Shoulders" to SHOULDERS,
    "Biceps" to BICEPS,
    "Core" to CORE,
    "Back" to BACK,
)
