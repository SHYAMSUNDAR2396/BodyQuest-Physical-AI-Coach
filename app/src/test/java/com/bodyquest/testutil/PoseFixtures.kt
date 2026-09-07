package com.bodyquest.testutil

import com.bodyquest.core.Landmark
import com.bodyquest.core.PoseLandmark
import com.bodyquest.core.PoseResult
import kotlin.math.cos
import kotlin.math.sin

/**
 * Builds a synthetic (but geometrically real) squat pose, used by both movement and form
 * tests so they exercise the exact same landmark layout `SquatKinematics` expects.
 *
 * - Knee bend is exact: the ankle is rotated around the knee away from full extension by
 *   the law-of-cosines-equivalent construction described inline below.
 * - [kneeValgusOffset] shifts a knee sideways (as a fraction of hip width) to simulate
 *   valgus without changing the knee angle itself.
 * - [torsoLeanDeg] tilts the shoulder midpoint forward of the hip midpoint.
 */
fun squatPose(
    kneeAngleDeg: Float,
    rightKneeAngleDeg: Float = kneeAngleDeg,
    kneeValgusOffset: Float = 0f,
    torsoLeanDeg: Float = 0f,
    visibility: Float = 1f,
): PoseResult {
    val landmarks = MutableList(33) { Landmark(0f, 0f, 0f, 0f) }
    val hipWidth = 0.1f

    fun legJoints(hipX: Float, angleDeg: Float, valgusSign: Float): Triple<Landmark, Landmark, Landmark> {
        val kneeY = 0.6f
        val hip = Landmark(hipX, 0.3f, 0f, visibility)
        // Knee angle is the interior angle at the knee between the hip-ray and ankle-ray;
        // fixing the hip straight above the knee and rotating the ankle by (180 - angle)
        // from straight-down produces exactly that interior angle.
        val phi = Math.toRadians((180.0 - angleDeg))
        val kneeX = hipX + valgusSign * kneeValgusOffset * hipWidth
        val ankleX = hipX - 0.3f * sin(phi).toFloat() // ankle stays under the hip, not the shifted knee
        val ankleY = kneeY + 0.3f * cos(phi).toFloat()
        val knee = Landmark(kneeX, kneeY, 0f, visibility)
        val ankle = Landmark(ankleX, ankleY, 0f, visibility)
        return Triple(hip, knee, ankle)
    }

    // Positive offset = both knees shift toward the body midline (valgus), for a leg's own sign.
    val (leftHip, leftKnee, leftAnkle) = legJoints(0.5f - hipWidth / 2f, kneeAngleDeg, valgusSign = 1f)
    val (rightHip, rightKnee, rightAnkle) = legJoints(0.5f + hipWidth / 2f, rightKneeAngleDeg, valgusSign = -1f)

    landmarks[PoseLandmark.LEFT_HIP] = leftHip
    landmarks[PoseLandmark.LEFT_KNEE] = leftKnee
    landmarks[PoseLandmark.LEFT_ANKLE] = leftAnkle
    landmarks[PoseLandmark.RIGHT_HIP] = rightHip
    landmarks[PoseLandmark.RIGHT_KNEE] = rightKnee
    landmarks[PoseLandmark.RIGHT_ANKLE] = rightAnkle

    val hipMidX = (leftHip.x + rightHip.x) / 2f
    val hipMidY = (leftHip.y + rightHip.y) / 2f
    val leanRad = Math.toRadians(torsoLeanDeg.toDouble())
    val shoulderMidX = hipMidX + 0.3f * sin(leanRad).toFloat()
    val shoulderMidY = hipMidY - 0.3f * cos(leanRad).toFloat()
    landmarks[PoseLandmark.LEFT_SHOULDER] = Landmark(shoulderMidX - hipWidth / 2f, shoulderMidY, 0f, visibility)
    landmarks[PoseLandmark.RIGHT_SHOULDER] = Landmark(shoulderMidX + hipWidth / 2f, shoulderMidY, 0f, visibility)

    return PoseResult(landmarks = landmarks, timestampMs = 0, inferenceTimeMs = 0)
}
