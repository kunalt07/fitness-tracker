package com.example.fitness_tracker.plan

private val MAIN_HEADER = Regex("(?i)^\\s*(#{1,6}\\s*)?(main|workout|exercises?)\\b.*")
private val OTHER_HEADER = Regex("(?i)^\\s*(#{1,6}\\s*)?(warm[- ]?up|cool[- ]?down|tip|notes?|rest)\\b.*")
private val BULLET = Regex("^\\s*([-*•]|\\d+[.)])\\s+(.*)$")
private val TRAILING_DETAILS = Regex("\\s*[—–\\-:]\\s.*$")
private val PARENS = Regex("\\([^)]*\\)")
private val SETS_X_REPS = Regex("(?i)\\b\\d+\\s*(x|×)\\s*\\d+.*")
private val NON_NAME_TAIL = Regex("\\s*[,•·:].*$")
private val MULTI_SPACE = Regex("\\s+")

/**
 * Best-effort extraction of exercise names from a Markdown workout plan.
 * Picks bullet items under a "main"-style header (or, if no header is found,
 * the longest run of bullets in the doc), then strips set/rep details.
 */
fun extractExercisesFromPlan(markdown: String): List<String> {
    if (markdown.isBlank()) return emptyList()
    val lines = markdown.lines()

    val bulletsByRegion = mutableListOf<MutableList<String>>()
    var headerKind: HeaderKind = HeaderKind.None
    var current: MutableList<String>? = null

    for (raw in lines) {
        val line = raw.trim()
        if (line.isEmpty()) continue
        when {
            MAIN_HEADER.matches(line) -> {
                headerKind = HeaderKind.Main
                current = mutableListOf<String>().also { bulletsByRegion += it }
            }
            OTHER_HEADER.matches(line) -> {
                headerKind = HeaderKind.Other
                current = null
            }
            else -> {
                val match = BULLET.matchEntire(line) ?: continue
                val item = match.groupValues[2]
                when (headerKind) {
                    HeaderKind.Main -> current?.add(item)
                    HeaderKind.None, HeaderKind.Other -> {
                        // Track bullet runs in unknown sections; we'll fall back to the longest.
                        if (current == null || headerKind == HeaderKind.Other) {
                            current = mutableListOf<String>().also { bulletsByRegion += it }
                            headerKind = HeaderKind.None
                        }
                        current?.add(item)
                    }
                }
            }
        }
    }

    val raw = bulletsByRegion.firstOrNull { it.isNotEmpty() && headerSawMain(bulletsByRegion, it) }
        ?: bulletsByRegion.maxByOrNull { it.size }
        ?: return emptyList()

    return raw.map { cleanExerciseName(it) }
        .filter { it.isNotBlank() && it.length in 3..40 }
        .distinct()
        .take(10)
}

private fun headerSawMain(all: List<List<String>>, candidate: List<String>): Boolean {
    // Heuristic: prefer a bucket only if it isn't the only one — i.e. there
    // were other (warm-up / cool-down) bullets that got dropped.
    return all.size > 1 && candidate.size in 3..10
}

private fun cleanExerciseName(item: String): String {
    var s = item.replace("**", "").replace("*", "").replace("_", "")
    s = s.replace(PARENS, "")
    s = s.replace(SETS_X_REPS, "")
    s = s.replace(TRAILING_DETAILS, "")
    s = s.replace(NON_NAME_TAIL, "")
    s = s.replace(MULTI_SPACE, " ").trim()
    return s
}

private enum class HeaderKind { None, Main, Other }
