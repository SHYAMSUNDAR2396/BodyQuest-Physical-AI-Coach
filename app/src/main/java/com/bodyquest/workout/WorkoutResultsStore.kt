package com.bodyquest.workout

/**
 * Hands the just-finished set's results from Live Coach to Set Summary without a full
 * Room round-trip (Phase 8 adds real persistence). One in-memory slot is enough for a
 * single active session.
 * ponytail: in-memory only, add Room storage when history needs to survive process death.
 */
object WorkoutResultsStore {
    var lastSession: List<RepResult> = emptyList()
    var lastExerciseId: String = "squat"
}
