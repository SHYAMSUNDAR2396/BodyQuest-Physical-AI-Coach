package com.bodyquest.coach

import com.bodyquest.form.FormIssue
import com.bodyquest.form.SquatFormSnapshot

data class CorrectionOutcome(
    val verified: CorrectionEvent?,
    val newCorrection: CorrectionEvent?,
)

/**
 * Turns per-rep form snapshots into the closed SEE→CORRECT→VERIFY loop (spec §13/§14).
 * At most one correction is active at a time — if several issues fire on the same rep,
 * only the highest-priority one is spoken, so the athlete is never given a list. The next
 * completed rep's snapshot for that same metric is what verifies (or doesn't) the fix.
 */
class CorrectionEngine(
    private val issueThreshold: Float = 0.75f,
    private val improvementMargin: Float = 0.02f,
    private val phraseProvider: CoachingPhraseProvider = CoachingPhrases,
) {
    private var pending: CorrectionEvent? = null
    private val _history = mutableListOf<CorrectionEvent>()
    val history: List<CorrectionEvent> get() = _history

    fun onRepCompleted(snapshot: SquatFormSnapshot, timestampMs: Long): CorrectionOutcome {
        val verified = verifyPending(snapshot, timestampMs)
        val newCorrection = if (pending == null) raiseIfNeeded(snapshot, timestampMs) else null
        return CorrectionOutcome(verified, newCorrection)
    }

    private fun verifyPending(snapshot: SquatFormSnapshot, timestampMs: Long): CorrectionEvent? {
        val current = pending ?: return null
        val after = scoreFor(snapshot, current.issue) ?: return null // couldn't re-measure this rep; leave pending
        val improved = after > current.beforeScore + improvementMargin
        val verified = current.copy(afterScore = after, improved = improved, timestamp = timestampMs)
        _history[_history.lastIndex] = verified
        pending = null
        return verified
    }

    private fun raiseIfNeeded(snapshot: SquatFormSnapshot, timestampMs: Long): CorrectionEvent? {
        val issue = FormIssue.entries
            .sortedBy { it.priority }
            .firstOrNull { issue -> (scoreFor(snapshot, issue) ?: 1f) < issueThreshold }
            ?: return null
        val before = scoreFor(snapshot, issue) ?: return null
        val event = CorrectionEvent(
            issue = issue,
            instruction = phraseProvider.instructionFor(issue),
            beforeScore = before,
            timestamp = timestampMs,
        )
        pending = event
        _history += event
        return event
    }

    private fun scoreFor(snapshot: SquatFormSnapshot, issue: FormIssue): Float? = when (issue) {
        FormIssue.KNEE_ALIGNMENT -> snapshot.kneeAlignment
        FormIssue.TORSO_LEAN -> snapshot.torsoStability
        FormIssue.INSUFFICIENT_DEPTH -> snapshot.depth
        FormIssue.TEMPO -> snapshot.tempo
        FormIssue.ASYMMETRY -> snapshot.symmetry
    }
}
