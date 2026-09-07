package com.bodyquest.form

import com.bodyquest.core.PoseResult
import com.bodyquest.movement.SquatKinematics
import kotlin.math.abs

data class RepSample(val pose: PoseResult, val timestampMs: Long)

/**
 * Deterministic biomechanics scoring (spec §11): every number here is computed from joint
 * geometry, never guessed by an LLM. One rep's worth of frames in, one [SquatFormSnapshot] out.
 */
class FormAnalysisEngine {

    fun analyzeSquatRep(samples: List<RepSample>): SquatFormSnapshot {
        if (samples.isEmpty()) return SquatFormSnapshot(null, null, null, null, null)

        val depth = analyzeDepth(samples)
        val kneeAlignment = analyzeKneeAlignment(samples)
        val torsoStability = analyzeTorsoStability(samples)
        val tempo = analyzeTempo(samples)
        val symmetry = analyzeSymmetry(samples)

        return SquatFormSnapshot(depth, kneeAlignment, torsoStability, tempo, symmetry)
    }

    private fun analyzeDepth(samples: List<RepSample>): Float? {
        val minAngle = samples.mapNotNull { SquatKinematics.averageKneeAngle(it.pose) }.minOrNull() ?: return null
        // Shallow (150deg, barely a bend) scores 0; a full squat (80deg) scores 1.
        return ((150f - minAngle) / (150f - 80f)).coerceIn(0f, 1f)
    }

    private fun analyzeKneeAlignment(samples: List<RepSample>): Float? {
        val ratios = samples.mapNotNull { SquatKinematics.kneeValgusRatio(it.pose) }
        if (ratios.isEmpty()) return null
        val worst = ratios.minOrNull() ?: return null
        // Knees tracking the feet (ratio >= 1) score perfectly; knees collapsed to half the
        // ankle stance width (ratio 0.5) or less is fully-bad valgus.
        return ((worst - 0.5f) / 0.5f).coerceIn(0f, 1f)
    }

    private fun analyzeTorsoStability(samples: List<RepSample>): Float? {
        val leans = samples.mapNotNull { SquatKinematics.torsoLeanDeg(it.pose) }
        if (leans.isEmpty()) return null
        val worstLean = leans.maxOrNull() ?: return null
        return (1f - worstLean / 45f).coerceIn(0f, 1f)
    }

    private fun analyzeTempo(samples: List<RepSample>): Float? {
        if (samples.size < 2) return null
        val durationMs = samples.last().timestampMs - samples.first().timestampMs
        if (durationMs <= 0) return null
        return when {
            durationMs < 400 -> 0f // dangerously fast, uncontrolled
            durationMs < 1200 -> (durationMs - 400) / (1200f - 400f)
            durationMs <= 4000 -> 1f // comfortable, controlled range
            durationMs <= 6000 -> 1f - (durationMs - 4000) / (6000f - 4000f)
            else -> 0f // stalled
        }.coerceIn(0f, 1f)
    }

    private fun analyzeSymmetry(samples: List<RepSample>): Float? {
        val diffs = samples.mapNotNull { sample ->
            val (left, right) = SquatKinematics.kneeAngles(sample.pose)
            if (left != null && right != null) abs(left - right) else null
        }
        if (diffs.isEmpty()) return null
        val worstDiff = diffs.maxOrNull() ?: return null
        // More than 25 degrees of left/right knee-angle divergence is fully asymmetric.
        return (1f - worstDiff / 25f).coerceIn(0f, 1f)
    }
}
