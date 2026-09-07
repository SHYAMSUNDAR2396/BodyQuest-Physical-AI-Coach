package com.bodyquest.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.bodyquest.core.PoseLandmark
import com.bodyquest.core.PoseResult
import com.bodyquest.ui.theme.BqAccent
import com.bodyquest.ui.theme.BqGood

// Bone connections for the subset of landmarks the plan's coaching engine cares about (spec §7).
private val BONES = listOf(
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
    PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
    PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
    PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
    PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
)

private const val VISIBILITY_FLOOR = 0.4f

/**
 * Draws the pose skeleton over the camera preview. Landmarks are normalized [0,1] in the
 * *frame's* coordinate space; since PreviewView here always shows the full upright frame
 * at the overlay's own aspect (see CalibrationScreen), a direct x*width/y*height mapping
 * is correct without a separate transform matrix.
 * ponytail: assumes preview fills the overlay at the frame's own aspect ratio; revisit if
 * the preview ever crops/letterboxes independently of this Canvas.
 */
@Composable
fun SkeletonOverlay(pose: PoseResult?, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val landmarks = pose?.landmarks ?: return@Canvas
        fun point(index: Int): Offset? {
            val lm = landmarks.getOrNull(index) ?: return null
            if (lm.visibility < VISIBILITY_FLOOR) return null
            return Offset(lm.x * size.width, lm.y * size.height)
        }

        BONES.forEach { (a, b) ->
            val pa = point(a)
            val pb = point(b)
            if (pa != null && pb != null) {
                drawLine(color = BqAccent, start = pa, end = pb, strokeWidth = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        val joints = listOf(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
        )
        joints.forEach { idx ->
            point(idx)?.let { drawCircle(color = BqGood, radius = 10f, center = it, style = Stroke(width = 4f)) }
        }
    }
}
