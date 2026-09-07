package com.bodyquest.movement

import com.bodyquest.core.Landmark
import com.bodyquest.core.PoseLandmark
import com.bodyquest.core.PoseResult
import com.bodyquest.core.angleDegrees

/**
 * Shared squat geometry, used by both [SquatStateMachine] (rep counting) and
 * `FormAnalysisEngine` (quality scoring) so the two never disagree about what a
 * "knee angle" or "visible enough" means (ponytail: one definition, not two).
 */
object SquatKinematics {
    const val DEFAULT_VISIBILITY_FLOOR = 0.4f

    private fun visible(vararg landmarks: Landmark, floor: Float) = landmarks.all { it.visibility >= floor }

    /** Left/right knee angle individually — null per side if that leg's landmarks aren't visible. */
    fun kneeAngles(pose: PoseResult, floor: Float = DEFAULT_VISIBILITY_FLOOR): Pair<Float?, Float?> {
        fun angleFor(hipIdx: Int, kneeIdx: Int, ankleIdx: Int): Float? {
            val hip = pose.landmark(hipIdx) ?: return null
            val knee = pose.landmark(kneeIdx) ?: return null
            val ankle = pose.landmark(ankleIdx) ?: return null
            if (!visible(hip, knee, ankle, floor = floor)) return null
            return angleDegrees(hip, knee, ankle)
        }
        return angleFor(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE) to
            angleFor(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
    }

    /** Average of both sides' knee angle, or whichever single side is visible. Null if neither is. */
    fun averageKneeAngle(pose: PoseResult, floor: Float = DEFAULT_VISIBILITY_FLOOR): Float? {
        val (left, right) = kneeAngles(pose, floor)
        return when {
            left != null && right != null -> (left + right) / 2f
            left != null -> left
            right != null -> right
            else -> null
        }
    }

    /**
     * Knee-separation-to-ankle-separation ratio: the standard frontal-view valgus signal
     * ("keep your knees in line with your feet"). 1.0 = knees track the feet's stance width;
     * well below 1.0 = knees caving in.
     *
     * Deliberately not "distance of the knee from the hip-ankle line" — any bent knee is
     * off that line by geometric necessity (a bend requires non-collinear points), which
     * would conflate squat depth with valgus. Comparing knee width to ankle width cancels
     * that out because both legs bend the same way.
     */
    fun kneeValgusRatio(pose: PoseResult, floor: Float = DEFAULT_VISIBILITY_FLOOR): Float? {
        val leftKnee = pose.landmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.landmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.landmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.landmark(PoseLandmark.RIGHT_ANKLE)
        if (leftKnee == null || rightKnee == null || leftAnkle == null || rightAnkle == null) return null
        if (!visible(leftKnee, rightKnee, leftAnkle, rightAnkle, floor = floor)) return null

        val ankleWidth = kotlin.math.abs(rightAnkle.x - leftAnkle.x).takeIf { it > 1e-4f } ?: return null
        val kneeWidth = kotlin.math.abs(rightKnee.x - leftKnee.x)
        return kneeWidth / ankleWidth
    }

    /** Torso lean from vertical, in degrees (0 = upright). Null if shoulders/hips aren't visible. */
    fun torsoLeanDeg(pose: PoseResult, floor: Float = DEFAULT_VISIBILITY_FLOOR): Float? {
        val ls = pose.landmark(PoseLandmark.LEFT_SHOULDER)
        val rs = pose.landmark(PoseLandmark.RIGHT_SHOULDER)
        val lh = pose.landmark(PoseLandmark.LEFT_HIP)
        val rh = pose.landmark(PoseLandmark.RIGHT_HIP)
        if (ls == null || rs == null || lh == null || rh == null || !visible(ls, rs, lh, rh, floor = floor)) return null
        val shoulderMidX = (ls.x + rs.x) / 2f
        val shoulderMidY = (ls.y + rs.y) / 2f
        val hipMidX = (lh.x + rh.x) / 2f
        val hipMidY = (lh.y + rh.y) / 2f
        val dx = shoulderMidX - hipMidX
        val dy = shoulderMidY - hipMidY // negative when shoulders are above hips, as expected
        return Math.toDegrees(kotlin.math.atan2(dx.toDouble(), -dy.toDouble())).toFloat().let { kotlin.math.abs(it) }
    }
}
