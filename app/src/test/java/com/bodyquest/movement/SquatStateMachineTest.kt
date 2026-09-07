package com.bodyquest.movement

import com.bodyquest.testutil.squatPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SquatStateMachineTest {

    private fun ramp(from: Float, to: Float, steps: Int): List<Float> {
        val step = (to - from) / steps
        return (0..steps).map { from + step * it }
    }

    @Test
    fun `clean full rep is counted once with correct depth`() {
        val sm = SquatStateMachine()
        val angles = ramp(170f, 90f, 12) + List(3) { 90f } + ramp(90f, 170f, 12) + List(3) { 170f }
        val events = sm.feed(angles)

        val reps = events.filterIsInstance<RepEvent.RepCompleted>()
        assertEquals(1, reps.size)
        assertTrue("expected depth near 90, was ${reps[0].minAngleDeg}", reps[0].minAngleDeg < 95f)
        assertTrue(reps[0].durationMs > 0)
    }

    @Test
    fun `partial descent that never reaches bottom does not count as a rep`() {
        val sm = SquatStateMachine()
        // Dips to 140 (past descend-enter at 150) but never reaches bottom-enter (110).
        val angles = ramp(170f, 140f, 8) + ramp(140f, 170f, 8) + List(3) { 170f }
        val events = sm.feed(angles)

        assertEquals(0, events.filterIsInstance<RepEvent.RepCompleted>().size)
        assertEquals(MovementPhase.STANDING, sm.phase)
    }

    @Test
    fun `pausing at the bottom does not create duplicate reps`() {
        val sm = SquatStateMachine()
        val angles = ramp(170f, 90f, 10) + List(20) { 90f } + ramp(90f, 170f, 10) + List(3) { 170f }
        val events = sm.feed(angles)

        assertEquals(1, events.filterIsInstance<RepEvent.RepCompleted>().size)
    }

    @Test
    fun `jitter around a threshold does not spam phase changes or fake reps`() {
        val sm = SquatStateMachine()
        // Descend to standing-adjacent territory, then oscillate near the ascend-enter
        // boundary (120) without ever reaching standing or bottom again.
        val descend = ramp(170f, 95f, 10)
        val jitter = List(30) { i -> if (i % 2 == 0) 118f else 122f }
        val events = sm.feed(descend + jitter)

        assertEquals(0, events.filterIsInstance<RepEvent.RepCompleted>().size)
    }

    @Test
    fun `reversal mid-ascent back to bottom does not double count`() {
        val sm = SquatStateMachine()
        val angles =
            ramp(170f, 90f, 10) +      // full descent
                ramp(90f, 130f, 6) +   // partial ascent (into ASCENDING)
                ramp(130f, 90f, 6) +   // sinks back down instead of finishing
                ramp(90f, 170f, 10) +  // completes for real this time
                List(3) { 170f }
        val events = sm.feed(angles)

        assertEquals(1, events.filterIsInstance<RepEvent.RepCompleted>().size)
    }

    @Test
    fun `low visibility landmarks are skipped rather than corrupting the signal`() {
        val sm = SquatStateMachine()
        val invisible = squatPose(kneeAngleDeg = 90f, visibility = 0.1f)
        val events = sm.onFrame(invisible, 1000)
        assertEquals(emptyList<RepEvent>(), events)
        assertEquals(MovementPhase.STANDING, sm.phase)
    }
}
