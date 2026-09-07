package com.bodyquest.camera

import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageAnalysis
import com.bodyquest.core.ImageFrame

/**
 * Bridges CameraX's analysis stream to [ImageFrame]. `ImageProxy.toBitmap()` (CameraX 1.3+)
 * already does the YUV->RGB conversion, so the only work left here is applying rotation
 * and front-camera mirroring and handing a plain Bitmap downstream.
 *
 * Runs on CameraX's own analysis executor; STRATEGY_KEEP_ONLY_LATEST means a slow
 * downstream consumer causes dropped frames, never a growing queue (spec §34).
 */
class PoseFrameAnalyzer(
    private val isFrontCamera: Boolean,
    private val onFrame: (ImageFrame) -> Unit,
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val rotation = image.imageInfo.rotationDegrees
            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
                if (isFrontCamera) postScale(-1f, 1f)
            }
            val raw = image.toBitmap()
            val upright = android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            onFrame(
                ImageFrame(
                    bitmap = upright,
                    rotationDegrees = 0,
                    isFrontCamera = isFrontCamera,
                    timestampMs = image.imageInfo.timestamp / 1_000_000,
                ),
            )
        } finally {
            image.close()
        }
    }
}
