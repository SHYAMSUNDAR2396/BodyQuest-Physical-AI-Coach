package com.bodyquest.movement

/** Generic rep-cycle phase shared by every exercise's state machine (spec §10). */
enum class MovementPhase { STANDING, DESCENDING, BOTTOM, ASCENDING }

sealed class RepEvent {
    data class PhaseChanged(val phase: MovementPhase) : RepEvent()
    data class RepCompleted(
        val repNumber: Int,
        val minAngleDeg: Float,
        val durationMs: Long,
    ) : RepEvent()
}
