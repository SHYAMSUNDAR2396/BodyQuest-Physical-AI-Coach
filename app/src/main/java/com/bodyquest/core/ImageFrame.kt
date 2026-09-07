package com.bodyquest.core

import android.graphics.Bitmap

/**
 * A single camera frame handed to the pose engine. Bitmap is the natural type here:
 * MediaPipe's Tasks Vision API consumes frames via BitmapImageBuilder, and pushing
 * Android types into `core`/`pose` doesn't hurt testability of `movement`/`form`/`coach`,
 * which only ever see PoseResult/Landmark (plain data classes, no Android imports).
 */
data class ImageFrame(
    val bitmap: Bitmap,
    val rotationDegrees: Int,
    val isFrontCamera: Boolean,
    val timestampMs: Long,
)
