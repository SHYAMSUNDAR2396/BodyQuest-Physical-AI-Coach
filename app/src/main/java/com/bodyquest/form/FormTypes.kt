package com.bodyquest.form

/**
 * Per-rep squat form metrics (spec §12). Each field is null when the landmarks needed to
 * compute it weren't visible/confident enough — an invalidated metric, never a guessed one.
 * All scores are normalized 0..1, higher is better.
 */
data class SquatFormSnapshot(
    val depth: Float?,
    val kneeAlignment: Float?,
    val torsoStability: Float?,
    val tempo: Float?,
    val symmetry: Float?,
) {
    /** Weighted composite of whichever metrics were computable this rep. */
    val formScore: Float? by lazy {
        val weighted = listOf(depth to 0.30f, kneeAlignment to 0.25f, torsoStability to 0.20f, tempo to 0.15f, symmetry to 0.10f)
            .mapNotNull { (value, weight) -> value?.let { it * weight to weight } }
        if (weighted.isEmpty()) return@lazy null
        val totalWeight = weighted.sumOf { it.second.toDouble() }.toFloat()
        weighted.sumOf { it.first.toDouble() }.toFloat() / totalWeight
    }
}

enum class FormIssue(val priority: Int, val metricLabel: String) {
    // Lower number = higher priority when several issues fire on the same rep (spec §13).
    KNEE_ALIGNMENT(1, "Knee alignment"),
    TORSO_LEAN(2, "Torso stability"),
    INSUFFICIENT_DEPTH(3, "Depth"),
    TEMPO(4, "Tempo"),
    ASYMMETRY(5, "Symmetry"),
}
