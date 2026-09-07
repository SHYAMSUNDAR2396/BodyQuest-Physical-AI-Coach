package com.bodyquest.movement

import com.bodyquest.testutil.squatPose

/** Feeds a sequence of knee angles (one frame each, 50ms apart) through a state machine. */
fun SquatStateMachine.feed(angles: List<Float>, stepMs: Long = 50): List<RepEvent> {
    val events = mutableListOf<RepEvent>()
    var ts = 0L
    for (angle in angles) {
        ts += stepMs
        events += onFrame(squatPose(angle), ts)
    }
    return events
}
