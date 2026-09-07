package com.bodyquest.core

private val KEY_LANDMARKS = listOf(
    PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
    PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
)

/** Whether enough of the body is visible to safely start a rep-counted set (spec §8). */
fun PoseResult.hasFullBodyVisible(floor: Float = 0.4f): Boolean {
    if (isEmpty) return false
    return KEY_LANDMARKS.all { idx -> (landmark(idx)?.visibility ?: 0f) >= floor }
}
