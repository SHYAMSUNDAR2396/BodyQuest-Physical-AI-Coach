package com.bodyquest.coach

import com.bodyquest.form.FormIssue

data class CorrectionEvent(
    val issue: FormIssue,
    val instruction: String,
    val beforeScore: Float,
    val afterScore: Float? = null,
    val improved: Boolean? = null,
    val timestamp: Long,
)

/** Fixed phrase table (spec §15/§32): short, supportive, never a medical claim. */
object CoachingPhrases {
    private val instructions = mapOf(
        FormIssue.KNEE_ALIGNMENT to "Your knees are moving inward. Keep them aligned with your feet.",
        FormIssue.TORSO_LEAN to "Keep your chest upright.",
        FormIssue.INSUFFICIENT_DEPTH to "Go slightly deeper.",
        FormIssue.TEMPO to "Slow down and control the movement.",
        FormIssue.ASYMMETRY to "Keep your weight even between both legs.",
    )

    fun instructionFor(issue: FormIssue): String = instructions.getValue(issue)

    const val IMPROVED = "Good. Your form is improving."
    const val NOT_YET_IMPROVED = "Still working on that — try again next rep."
}
