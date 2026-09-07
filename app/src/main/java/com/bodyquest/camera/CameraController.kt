package com.bodyquest.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

/**
 * Owns the CameraX bind/unbind lifecycle so the Compose layer doesn't have to know about
 * ProcessCameraProvider. Back camera by default per spec §7/§23 (the athlete faces the phone).
 */
class CameraController(private val context: Context) {
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null

    fun start(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrame: (com.bodyquest.core.ImageFrame) -> Unit,
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                val cameraProvider = future.get()
                provider = cameraProvider

                val newPreview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val newAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                newAnalysis.setAnalyzer(analysisExecutor, PoseFrameAnalyzer(isFrontCamera = false, onFrame = onFrame))

                // Unbind only what *this* controller previously bound, never unbindAll() — that's
                // a device-wide nuke. If a previous screen's teardown lands after this screen has
                // already bound its own camera (a real timing overlap during Compose navigation),
                // unbindAll() would kill the new binding permanently with nothing left to rebind it.
                if (preview != null || analysis != null) {
                    cameraProvider.unbind(preview, analysis)
                }
                preview = newPreview
                analysis = newAnalysis

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    newPreview,
                    newAnalysis,
                )
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun stop() {
        provider?.unbind(preview, analysis)
        preview = null
        analysis = null
    }
}
