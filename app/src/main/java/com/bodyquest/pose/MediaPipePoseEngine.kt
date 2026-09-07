package com.bodyquest.pose

import android.content.Context
import android.util.Log
import com.bodyquest.core.ImageFrame
import com.bodyquest.core.Landmark
import com.bodyquest.core.PoseEngine
import com.bodyquest.core.PoseResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "MediaPipePoseEngine"
private const val MODEL_ASSET = "pose_landmarker_lite.task"

/**
 * Real [PoseEngine] backed by MediaPipe Tasks Vision. Tries the GPU delegate first and
 * falls back to CPU — the emulator's software renderer doesn't support MediaPipe's GPU
 * delegate, a real phone usually does (plan's "emulator constraints" section).
 *
 * RunningMode.VIDEO (not IMAGE) because CameraX hands us a monotonic timestamp per frame
 * and MediaPipe uses that ordering for temporal smoothing between poses.
 */
class MediaPipePoseEngine private constructor(
    private val landmarker: PoseLandmarker,
    val delegateUsed: Delegate,
) : PoseEngine {

    private val lastTimestampMs = AtomicLong(0)

    override fun process(frame: ImageFrame): PoseResult {
        // detectForVideo requires strictly increasing timestamps; camera frames can repeat
        // under emulator jitter, so clamp forward rather than crash the analysis thread.
        val ts = maxOf(frame.timestampMs, lastTimestampMs.get() + 1)
        lastTimestampMs.set(ts)

        val start = System.currentTimeMillis()
        val mpImage = BitmapImageBuilder(frame.bitmap).build()
        val result = landmarker.detectForVideo(mpImage, ts)
        val inferenceMs = System.currentTimeMillis() - start

        val landmarks = result.landmarks().firstOrNull()?.map {
            Landmark(x = it.x(), y = it.y(), z = it.z(), visibility = it.visibility().orElse(0f))
        } ?: emptyList()

        return PoseResult(landmarks = landmarks, timestampMs = ts, inferenceTimeMs = inferenceMs)
    }

    override fun close() = landmarker.close()

    companion object {
        fun create(context: Context): MediaPipePoseEngine {
            val (landmarker, delegate) = tryCreate(context, Delegate.GPU)
                ?: tryCreate(context, Delegate.CPU)
                ?: error("Failed to initialize PoseLandmarker on both GPU and CPU delegates")
            return MediaPipePoseEngine(landmarker, delegate)
        }

        private fun tryCreate(context: Context, delegate: Delegate): Pair<PoseLandmarker, Delegate>? = try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .setDelegate(delegate)
                .build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
            PoseLandmarker.createFromOptions(context, options) to delegate
        } catch (e: Exception) {
            Log.w(TAG, "PoseLandmarker init failed on $delegate delegate, falling back", e)
            null
        }
    }
}
