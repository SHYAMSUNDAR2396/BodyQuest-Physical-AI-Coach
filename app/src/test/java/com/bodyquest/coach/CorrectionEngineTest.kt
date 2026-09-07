package com.bodyquest.coach

import com.bodyquest.form.FormIssue
import com.bodyquest.form.SquatFormSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionEngineTest {

    private fun snapshot(
        depth: Float = 0.9f,
        kneeAlignment: Float = 0.9f,
        torsoStability: Float = 0.9f,
        tempo: Float = 0.9f,
        symmetry: Float = 0.9f,
    ) = SquatFormSnapshot(depth, kneeAlignment, torsoStability, tempo, symmetry)

    @Test
    fun `no issues raises no correction`() {
        val engine = CorrectionEngine()
        val outcome = engine.onRepCompleted(snapshot(), 1000)
        assertNull(outcome.newCorrection)
        assertNull(outcome.verified)
    }

    @Test
    fun `bad knee alignment raises exactly that correction`() {
        val engine = CorrectionEngine()
        val outcome = engine.onRepCompleted(snapshot(kneeAlignment = 0.5f), 1000)

        assertEquals(FormIssue.KNEE_ALIGNMENT, outcome.newCorrection?.issue)
        assertEquals(0.5f, outcome.newCorrection?.beforeScore)
    }

    @Test
    fun `multiple simultaneous issues only raise the highest priority one`() {
        val engine = CorrectionEngine()
        // Knee alignment (priority 1) and depth (priority 3) are both bad; only alignment should fire.
        val outcome = engine.onRepCompleted(snapshot(kneeAlignment = 0.5f, depth = 0.4f), 1000)

        assertEquals(FormIssue.KNEE_ALIGNMENT, outcome.newCorrection?.issue)
    }

    @Test
    fun `the plan's exact 71 to 89 scenario verifies as improved`() {
        val engine = CorrectionEngine()
        val first = engine.onRepCompleted(snapshot(kneeAlignment = 0.71f), 1000)
        assertEquals(FormIssue.KNEE_ALIGNMENT, first.newCorrection?.issue)
        assertEquals(0.71f, first.newCorrection?.beforeScore)

        val second = engine.onRepCompleted(snapshot(kneeAlignment = 0.89f), 2000)
        assertEquals(0.71f, second.verified?.beforeScore)
        assertEquals(0.89f, second.verified?.afterScore)
        assertEquals(true, second.verified?.improved)
    }

    @Test
    fun `unimproved metric verifies as not improved and is raised again`() {
        val engine = CorrectionEngine()
        engine.onRepCompleted(snapshot(kneeAlignment = 0.5f), 1000)
        val second = engine.onRepCompleted(snapshot(kneeAlignment = 0.51f), 2000)

        assertEquals(false, second.verified?.improved)
        // Still below threshold, so the coach raises it again rather than going silent.
        assertEquals(FormIssue.KNEE_ALIGNMENT, second.newCorrection?.issue)
        assertEquals(0.51f, second.newCorrection?.beforeScore)
    }

    @Test
    fun `once fixed, a different lower-priority issue can surface next`() {
        val engine = CorrectionEngine()
        engine.onRepCompleted(snapshot(kneeAlignment = 0.5f, depth = 0.4f), 1000)
        val second = engine.onRepCompleted(snapshot(kneeAlignment = 0.9f, depth = 0.4f), 2000)

        assertTrue(second.verified?.improved == true)
        assertEquals(FormIssue.INSUFFICIENT_DEPTH, second.newCorrection?.issue)
    }

    @Test
    fun `history accumulates every correction event`() {
        val engine = CorrectionEngine()
        engine.onRepCompleted(snapshot(kneeAlignment = 0.5f), 1000)
        engine.onRepCompleted(snapshot(kneeAlignment = 0.9f), 2000)

        assertEquals(1, engine.history.size)
        assertEquals(true, engine.history[0].improved)
    }
}
