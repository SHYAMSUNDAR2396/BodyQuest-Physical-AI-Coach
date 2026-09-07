package com.bodyquest.form

import com.bodyquest.testutil.squatPose
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormAnalysisEngineTest {

    private val engine = FormAnalysisEngine()

    /** A rep descending from standing to a target bottom angle and back, evenly timed. */
    private fun repSamples(
        bottomAngle: Float,
        rightBottomAngle: Float = bottomAngle,
        valgusOffset: Float = 0f,
        torsoLeanDeg: Float = 0f,
        totalDurationMs: Long = 2000,
        steps: Int = 20,
    ): List<RepSample> {
        val stepMs = totalDurationMs / steps
        return (0..steps).map { i ->
            // Triangle wave: 170 -> bottomAngle -> 170.
            val t = i / steps.toFloat()
            val fraction = if (t < 0.5f) t * 2 else (1 - t) * 2
            val left = 170f - fraction * (170f - bottomAngle)
            val right = 170f - fraction * (170f - rightBottomAngle)
            RepSample(
                pose = squatPose(left, right, kneeValgusOffset = valgusOffset * fraction, torsoLeanDeg = torsoLeanDeg * fraction),
                timestampMs = i * stepMs,
            )
        }
    }

    @Test
    fun `good squat scores well across every metric`() {
        val snapshot = engine.analyzeSquatRep(repSamples(bottomAngle = 85f))

        assertTrue("depth=${snapshot.depth}", snapshot.depth!! > 0.8f)
        assertTrue("kneeAlignment=${snapshot.kneeAlignment}", snapshot.kneeAlignment!! > 0.9f)
        assertTrue("torsoStability=${snapshot.torsoStability}", snapshot.torsoStability!! > 0.9f)
        assertTrue("tempo=${snapshot.tempo}", snapshot.tempo!! > 0.8f)
        assertTrue("symmetry=${snapshot.symmetry}", snapshot.symmetry!! > 0.9f)
        assertTrue("formScore=${snapshot.formScore}", snapshot.formScore!! > 0.85f)
    }

    @Test
    fun `knee valgus is flagged without affecting other metrics`() {
        val good = engine.analyzeSquatRep(repSamples(bottomAngle = 85f))
        val valgus = engine.analyzeSquatRep(repSamples(bottomAngle = 85f, valgusOffset = 0.5f))

        assertTrue("expected knee alignment to drop, was ${valgus.kneeAlignment}", valgus.kneeAlignment!! < good.kneeAlignment!! - 0.2f)
        assertTrue("depth should be unaffected", kotlin.math.abs(valgus.depth!! - good.depth!!) < 0.05f)
    }

    @Test
    fun `shallow squat scores low on depth only`() {
        val shallow = engine.analyzeSquatRep(repSamples(bottomAngle = 140f))
        val good = engine.analyzeSquatRep(repSamples(bottomAngle = 85f))

        assertTrue("depth=${shallow.depth}", shallow.depth!! < 0.3f)
        assertTrue("alignment should be unaffected", shallow.kneeAlignment!! > good.kneeAlignment!! - 0.05f)
    }

    @Test
    fun `excessive torso lean is flagged`() {
        val leaning = engine.analyzeSquatRep(repSamples(bottomAngle = 85f, torsoLeanDeg = 40f))
        assertTrue("torsoStability=${leaning.torsoStability}", leaning.torsoStability!! < 0.3f)
    }

    @Test
    fun `asymmetric depth between legs lowers the symmetry score`() {
        val asymmetric = engine.analyzeSquatRep(repSamples(bottomAngle = 80f, rightBottomAngle = 140f))
        assertTrue("symmetry=${asymmetric.symmetry}", asymmetric.symmetry!! < 0.3f)
    }

    @Test
    fun `too-fast tempo scores near zero, comfortable tempo scores near one`() {
        val fast = engine.analyzeSquatRep(repSamples(bottomAngle = 85f, totalDurationMs = 200))
        val comfortable = engine.analyzeSquatRep(repSamples(bottomAngle = 85f, totalDurationMs = 2000))

        assertTrue("fast tempo=${fast.tempo}", fast.tempo!! < 0.2f)
        assertTrue("comfortable tempo=${comfortable.tempo}", comfortable.tempo!! > 0.9f)
    }

    @Test
    fun `empty rep produces no metrics rather than fabricated zeros`() {
        val snapshot = engine.analyzeSquatRep(emptyList())
        assertNull(snapshot.depth)
        assertNull(snapshot.formScore)
    }
}
