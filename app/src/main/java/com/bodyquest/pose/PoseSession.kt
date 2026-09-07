package com.bodyquest.pose

import android.content.Context
import com.bodyquest.core.ImageFrame
import com.bodyquest.core.PoseEngine
import com.bodyquest.core.PoseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Glues [PoseEngine] to the camera analysis thread: owns lazy engine init (loading the
 * model off the main thread) and republishes each result as a [StateFlow] the UI collects.
 * `onFrame` runs on CameraX's analysis executor — already a background thread — so calling
 * the blocking [PoseEngine.process] there is exactly its documented contract.
 */
class PoseSession(private val context: Context) {
    private var engine: PoseEngine? = null

    private val _result = MutableStateFlow<PoseResult?>(null)
    val result: StateFlow<PoseResult?> = _result

    private val _delegateLabel = MutableStateFlow("initializing")
    val delegateLabel: StateFlow<String> = _delegateLabel

    suspend fun warmUp() {
        if (engine != null) return
        withContext(Dispatchers.Default) {
            val mp = MediaPipePoseEngine.create(context)
            engine = mp
            _delegateLabel.value = mp.delegateUsed.name
        }
    }

    /** Called from the camera analysis thread; blocking by design. Returns the result for
     *  callers (e.g. a workout session) that need it synchronously, not just via the flow. */
    fun onFrame(frame: ImageFrame): PoseResult? {
        val current = engine ?: return null
        val result = current.process(frame)
        _result.value = result
        return result
    }

    fun close() {
        engine?.close()
        engine = null
    }
}
