package com.bodyquest.workout

import com.bodyquest.coach.CorrectionEngine
import com.bodyquest.coach.CorrectionEvent
import com.bodyquest.core.PoseResult
import com.bodyquest.form.FormAnalysisEngine
import com.bodyquest.form.RepSample
import com.bodyquest.form.SquatFormSnapshot
import com.bodyquest.movement.MovementPhase
import com.bodyquest.movement.RepEvent
import com.bodyquest.movement.SquatStateMachine

data class RepResult(
    val repNumber: Int,
    val formSnapshot: SquatFormSnapshot,
    val correction: CorrectionEvent?,
    val verifiedCorrection: CorrectionEvent?,
)

/**
 * Orchestrates one squat set end-to-end: feeds every camera frame to the movement engine,
 * buffers the frames belonging to the in-progress rep, and on each completed rep runs form
 * analysis and correction/verification — the plan's full SEE→ANALYZE→CORRECT→VERIFY loop,
 * wired together rather than left as three engines that never talk to each other.
 */
class SquatWorkoutSession(
    private val movement: SquatStateMachine = SquatStateMachine(),
    private val formEngine: FormAnalysisEngine = FormAnalysisEngine(),
    private val correctionEngine: CorrectionEngine = CorrectionEngine(),
) {
    private val currentRepFrames = mutableListOf<RepSample>()
    private val _completedReps = mutableListOf<RepResult>()
    val completedReps: List<RepResult> get() = _completedReps

    val phase: MovementPhase get() = movement.phase

    fun onFrame(pose: PoseResult, timestampMs: Long): RepResult? {
        currentRepFrames += RepSample(pose, timestampMs)
        val events = movement.onFrame(pose, timestampMs)

        val repCompleted = events.filterIsInstance<RepEvent.RepCompleted>().firstOrNull() ?: run {
            // Cap buffered frames so a stalled/never-completing rep can't grow unbounded.
            if (currentRepFrames.size > MAX_BUFFERED_FRAMES) currentRepFrames.removeAt(0)
            return null
        }

        val snapshot = formEngine.analyzeSquatRep(currentRepFrames.toList())
        currentRepFrames.clear()

        val outcome = correctionEngine.onRepCompleted(snapshot, timestampMs)
        val result = RepResult(
            repNumber = repCompleted.repNumber,
            formSnapshot = snapshot,
            correction = outcome.newCorrection,
            verifiedCorrection = outcome.verified,
        )
        _completedReps += result
        return result
    }

    companion object {
        private const val MAX_BUFFERED_FRAMES = 300 // ~10s at 30fps; a real rep never gets close
    }
}
