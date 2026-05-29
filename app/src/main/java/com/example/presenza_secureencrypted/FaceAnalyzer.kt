package com.example.presenza_secureencrypted

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.Classifications
import kotlin.math.pow
import kotlin.math.sqrt

class FaceAnalyzer(
    context: Context,
    private val onFaceDetected: (FaceLandmarkerResult, Float, Boolean, Int) -> Unit,
) : ImageAnalysis.Analyzer {

    private val options = FaceLandmarker.FaceLandmarkerOptions.builder()
        .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
        .setRunningMode(RunningMode.LIVE_STREAM)
        .setOutputFaceBlendshapes(true)
        .setResultListener { result, _ ->
            processResult(result)
        }
        .setNumFaces(3) // Detect up to 3 faces to warn if more than 1
        .build()

    private val faceLandmarker = FaceLandmarker.createFromOptions(context, options)

    private fun processResult(result: FaceLandmarkerResult) {
        val faceCount = result.faceLandmarks().size
        
        if (faceCount > 0) {
            val landmarks = result.faceLandmarks()[0]
            
            // Calculate Mouth Aspect Ratio (MAR)
            val p13 = landmarks[13]
            val p14 = landmarks[14]
            val p78 = landmarks[78]
            val p308 = landmarks[308]

            val verticalDist = dist(p13.x(), p13.y(), p14.x(), p14.y())
            val horizontalDist = dist(p78.x(), p78.y(), p308.x(), p308.y())
            val mar = if (horizontalDist > 0) verticalDist / horizontalDist else 0f

            // Calculate Blink Detection
            val leftEyeTop = landmarks[159]
            val leftEyeBottom = landmarks[145]
            val rightEyeTop = landmarks[386]
            val rightEyeBottom = landmarks[374]

            val leftEyeDist = dist(leftEyeTop.x(), leftEyeTop.y(), leftEyeBottom.x(), leftEyeBottom.y())
            val rightEyeDist = dist(rightEyeTop.x(), rightEyeTop.y(), rightEyeBottom.x(), rightEyeBottom.y())

            // A threshold for blink detection
            val isBlinking = leftEyeDist < 0.015f || rightEyeDist < 0.015f
            
            onFaceDetected(result, mar, isBlinking, faceCount)
        } else {
            onFaceDetected(result, 0f, false, 0)
        }
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }

    override fun analyze(image: ImageProxy) {
        val frameTime = System.currentTimeMillis()
        
        // Convert ImageProxy to Bitmap
        val bitmap = image.toBitmap()
        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            // Flip if using front camera
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        faceLandmarker.detectAsync(mpImage, frameTime)
        
        image.close()
    }
}
