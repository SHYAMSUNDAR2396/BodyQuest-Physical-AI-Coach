package com.bodyquest.movement

import com.bodyquest.core.PoseResult

/**
 * Squat rep counter driven by knee angle, not naive threshold-crossing (spec §10).
 * A rep only counts if the athlete's knee angle actually reaches [bottomEnterDeg] between
 * two visits to "standing" — a partial dip, a pause, or jitter around a threshold never
 * fires a duplicate or a phantom rep.
 *
 * Smoothing: exponential moving average on the knee angle absorbs per-frame landmark noise.
 * Hysteresis: enter/exit thresholds for each transition are offset from each other so the
 * signal has to move meaningfully, not just cross one line, to flip phase.
 * Debounce: a candidate transition must hold for [framesToConfirm] consecutive frames.
 */
class SquatStateMachine(
    private val standingEnterDeg: Float = 160f,
    private val descendEnterDeg: Float = 150f,
    private val bottomEnterDeg: Float = 110f,
    private val ascendEnterDeg: Float = 120f,
    private val emaAlpha: Float = 0.4f,
    private val framesToConfirm: Int = 2,
    private val visibilityFloor: Float = 0.4f,
) {
    var phase: MovementPhase = MovementPhase.STANDING
        private set

    private var smoothedAngle: Float? = null
    private var repCount = 0
    private var minAngleThisRep = Float.MAX_VALUE
    private var repStartMs: Long = 0
    private var reachedBottom = false

    private var pendingPhase: MovementPhase? = null
    private var pendingFrames = 0

    /** Feed one frame; returns any events that occurred (phase change and/or a completed rep). */
    fun onFrame(pose: PoseResult, timestampMs: Long): List<RepEvent> {
        val raw = SquatKinematics.averageKneeAngle(pose, visibilityFloor) ?: return emptyList()
        val smoothed = smoothedAngle?.let { it + emaAlpha * (raw - it) } ?: raw
        smoothedAngle = smoothed

        if (phase == MovementPhase.STANDING && repStartMs == 0L) repStartMs = timestampMs
        minAngleThisRep = minOf(minAngleThisRep, smoothed)

        val candidate = candidatePhase(smoothed) ?: return emptyList()
        if (candidate == phase) {
            pendingPhase = null
            pendingFrames = 0
            return emptyList()
        }

        if (candidate != pendingPhase) {
            pendingPhase = candidate
            pendingFrames = 1
        } else {
            pendingFrames++
        }

        if (pendingFrames < framesToConfirm) return emptyList()

        // Confirmed transition.
        pendingPhase = null
        pendingFrames = 0
        return applyTransition(candidate, timestampMs)
    }

    /** Which phase the current smoothed angle alone would suggest, given hysteresis bands. */
    private fun candidatePhase(angle: Float): MovementPhase? = when (phase) {
        MovementPhase.STANDING -> if (angle <= descendEnterDeg) MovementPhase.DESCENDING else null
        MovementPhase.DESCENDING -> when {
            angle <= bottomEnterDeg -> MovementPhase.BOTTOM
            angle >= standingEnterDeg -> MovementPhase.STANDING // gave up on the descent, bounced back up
            else -> null
        }
        MovementPhase.BOTTOM -> if (angle >= ascendEnterDeg) MovementPhase.ASCENDING else null
        MovementPhase.ASCENDING -> when {
            angle >= standingEnterDeg -> MovementPhase.STANDING
            angle <= bottomEnterDeg -> MovementPhase.BOTTOM // sank back down instead of finishing the rep
            else -> null
        }
    }

    private fun applyTransition(next: MovementPhase, timestampMs: Long): List<RepEvent> {
        val events = mutableListOf<RepEvent>()
        if (next == MovementPhase.BOTTOM) reachedBottom = true

        val completedRep = phase == MovementPhase.ASCENDING && next == MovementPhase.STANDING && reachedBottom
        phase = next
        events += RepEvent.PhaseChanged(next)

        if (completedRep) {
            repCount++
            events += RepEvent.RepCompleted(
                repNumber = repCount,
                minAngleDeg = minAngleThisRep,
                durationMs = timestampMs - repStartMs,
            )
        }

        if (next == MovementPhase.STANDING) {
            // Reset per-rep tracking whether the cycle completed or was abandoned partway.
            reachedBottom = false
            minAngleThisRep = Float.MAX_VALUE
            repStartMs = timestampMs
        }
        return events
    }
}
