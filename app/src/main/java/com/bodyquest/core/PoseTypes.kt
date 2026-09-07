package com.bodyquest.core

/** One body landmark in normalized [0,1] image coordinates, plus MediaPipe's visibility score. */
data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float,
)

/** MediaPipe Pose Landmarker's 33-point index set — the subset BodyQuest actually uses (spec §7). */
object PoseLandmark {
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
}

data class PoseResult(
    val landmarks: List<Landmark>,
    val timestampMs: Long,
    val inferenceTimeMs: Long,
) {
    val isEmpty: Boolean get() = landmarks.isEmpty()

    fun landmark(index: Int): Landmark? = landmarks.getOrNull(index)
}

/** The one seam between the ML runtime and the rest of the app (plan §"the one interface that matters"). */
interface PoseEngine {
    fun process(frame: ImageFrame): PoseResult
    fun close()
}
