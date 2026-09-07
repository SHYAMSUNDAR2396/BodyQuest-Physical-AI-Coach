package com.bodyquest.ui.nav

/** Every screen in the app is a real, navigable destination (spec §21). */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val WORKOUT_SELECTION = "workout_selection"
    const val EXERCISE_SELECTION = "exercise_selection/{workoutId}"
    const val CALIBRATION = "calibration/{exerciseId}"
    const val LIVE_COACH = "live_coach/{exerciseId}"
    const val SET_SUMMARY = "set_summary/{sessionId}"
    const val WORKOUT_SUMMARY = "workout_summary/{sessionId}"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    fun exerciseSelection(workoutId: String) = "exercise_selection/$workoutId"
    fun calibration(exerciseId: String) = "calibration/$exerciseId"
    fun liveCoach(exerciseId: String) = "live_coach/$exerciseId"
    fun setSummary(sessionId: String) = "set_summary/$sessionId"
    fun workoutSummary(sessionId: String) = "workout_summary/$sessionId"
}
