package com.bodyquest.workout

import com.bodyquest.testutil.squatPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SquatWorkoutSessionTest {

    /** Descend to [bottomAngle] and back over [steps] frames, with a knee-valgus offset. */
    private fun feedRep(session: SquatWorkoutSession, startTs: Long, valgusOffset: Float, bottomAngle: Float = 85f, steps: Int = 16): Long {
        var ts = startTs
        val angles = (0..steps).map { i ->
            val t = i / steps.toFloat()
            val fraction = if (t < 0.5f) t * 2 else (1 - t) * 2
            170f - fraction * (170f - bottomAngle)
        } + List(3) { 170f } // hold so the debounce can confirm the final STANDING transition
        for (angle in angles) {
            ts += 100
            session.onFrame(squatPose(angle, kneeValgusOffset = valgusOffset), ts)
        }
        return ts
    }

    @Test
    fun `a full squat set flows from frames to a verified correction end to end`() {
        val session = SquatWorkoutSession()

        var ts = 0L
        ts = feedRep(session, ts, valgusOffset = 0.3f) // bad knee alignment
        assertEquals(1, session.completedReps.size)
        val first = session.completedReps[0]
        assertNotNull("expected a correction to fire on bad knee alignment", first.correction)
        assertTrue(first.formSnapshot.kneeAlignment!! < 0.75f)

        ts = feedRep(session, ts, valgusOffset = 0f) // corrected
        assertEquals(2, session.completedReps.size)
        val second = session.completedReps[1]
        assertNotNull("expected the prior correction to be verified", second.verifiedCorrection)
        assertEquals(true, second.verifiedCorrection?.improved)
    }
}
