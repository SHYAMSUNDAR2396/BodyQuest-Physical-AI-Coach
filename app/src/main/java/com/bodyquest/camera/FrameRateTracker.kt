package com.bodyquest.camera

/**
 * Rolling FPS counter for the debug/perf panel (spec §35): real measurement, not a
 * guessed number. Not thread-safe by design — call only from the analysis dispatcher.
 */
class FrameRateTracker(private val windowMs: Long = 1000) {
    private val timestamps = ArrayDeque<Long>()

    fun onFrame(nowMs: Long = System.currentTimeMillis()): Int {
        timestamps.addLast(nowMs)
        while (timestamps.isNotEmpty() && nowMs - timestamps.first() > windowMs) {
            timestamps.removeFirst()
        }
        return timestamps.size
    }
}
