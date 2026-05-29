package com.example.presenza_secureencrypted

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.presenza_secureencrypted.databinding.FragmentRecognitionBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecognitionFragment : Fragment() {
    private var _binding: FragmentRecognitionBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentCode: String = ""
    private var isCodeSpoken: Boolean = false
    private var isAutoCapturing: Boolean = false

    private lateinit var faceNetModel: FaceNetModel
    private lateinit var firebaseManager: FirebaseManager
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    private val handler = Handler(Looper.getMainLooper())
    private val codeRunnable = object : Runnable {
        override fun run() {
            updateVerificationCode()
            handler.postDelayed(this, 10000)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && 
            permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startCamera()
            startSpeechRecognition()
        } else {
            Toast.makeText(context, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
            startSpeechRecognition()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }

        binding.imageCaptureButton.setOnClickListener { takePhoto() }
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        if (arguments?.getBoolean("IS_ENROLLMENT") == true) {
            binding.tvInstruction.text = "Enrolling: ${arguments?.getString("FIRST_NAME")}"
        }

        faceNetModel = FaceNetModel(requireContext())
        firebaseManager = FirebaseManager()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCodeFlashing()
    }

    private fun startCodeFlashing() {
        handler.post(codeRunnable)
    }

    private fun updateVerificationCode() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        currentCode = (1..6)
            .map { chars.random() }
            .joinToString("")
        binding.tvVerificationCode.text = "CODE: $currentCode"
        isCodeSpoken = false // Reset for new code
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(context, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                // Restart listening on error (like timeout)
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    speechRecognizer?.startListening(intent)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.forEach { match ->
                    if (match.replace(" ", "").contains(currentCode, ignoreCase = true)) {
                        isCodeSpoken = true
                        Log.d(TAG, "Code matched: $match")
                    }
                }
                // Keep listening
                speechRecognizer?.startListening(intent)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.forEach { match ->
                    if (match.replace(" ", "").contains(currentCode, ignoreCase = true)) {
                        isCodeSpoken = true
                        Log.d(TAG, "Code matched (partial): $match")
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private var marHistory = mutableListOf<Float>()
    private var hasBlinked = false
    
    private fun updateAnalysisUI(mar: Float, isBlinking: Boolean, faceCount: Int) {
        if (faceCount > 1) {
            binding.tvAntiSpoofing.text = "Error: Multiple faces detected!"
            binding.tvAntiSpoofing.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            return // Stop further processing
        } else if (faceCount == 0) {
            binding.tvAntiSpoofing.text = "Align face within the frame"
            binding.tvAntiSpoofing.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            return
        }

        if (isBlinking) hasBlinked = true
        
        marHistory.add(mar)
        if (marHistory.size > 10) marHistory.removeAt(0)
        
        val isMoving = (marHistory.maxOrNull() ?: 0f) - (marHistory.minOrNull() ?: 0f) > 0.05f
        
        val statusText = StringBuilder()
        if (isMoving) statusText.append("Liveness: Movement Detected")
        else statusText.append("Liveness: Please speak the code")
        
        if (hasBlinked) statusText.append(" | Blink OK")
        else statusText.append(" | Please blink")

        if (isCodeSpoken) statusText.append(" | Code OK")
        else statusText.append(" | Speak Code")

        binding.tvAntiSpoofing.text = statusText.toString()
        
        if (isMoving && hasBlinked && isCodeSpoken) {
            binding.tvAntiSpoofing.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            if (!isAutoCapturing) {
                isAutoCapturing = true
                Toast.makeText(context, "Liveness Verified! Capturing...", Toast.LENGTH_SHORT).show()
                handler.postDelayed({ takePhoto() }, 500) // Small delay for UI to update
            }
        } else {
            binding.tvAntiSpoofing.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer(requireContext()) { result, mar, isBlinking, faceCount ->
                        activity?.runOnUiThread {
                            updateAnalysisUI(mar, isBlinking, faceCount)
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processCapturedImage(image)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    isAutoCapturing = false // Allow retry
                }
            }
        )
    }

    private fun processCapturedImage(image: ImageProxy) {
        val bitmap = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        val inputImage = InputImage.fromBitmap(bitmap, rotation)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val boundingBox = face.boundingBox
                    
                    // Ensure bounding box is within bitmap bounds
                    val left = maxOf(0, boundingBox.left)
                    val top = maxOf(0, boundingBox.top)
                    val width = minOf(bitmap.width - left, boundingBox.width())
                    val height = minOf(bitmap.height - top, boundingBox.height())
                    
                    val faceBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
                    
                    if (arguments?.getBoolean("IS_ENROLLMENT") == true) {
                        enrollFace(faceBitmap)
                    } else {
                        verifyIdentity(faceBitmap)
                    }
                } else {
                    Toast.makeText(context, "No face detected in capture", Toast.LENGTH_SHORT).show()
                    isAutoCapturing = false
                }
                image.close()
            }
            .addOnFailureListener {
                Log.e(TAG, "Face detection failed", it)
                image.close()
                isAutoCapturing = false
            }
    }

    private fun enrollFace(faceBitmap: Bitmap) {
        val embedding = faceNetModel.getFaceEmbedding(faceBitmap)
        val rollNo = arguments?.getString("ROLL_NO") ?: ""
        val firstName = arguments?.getString("FIRST_NAME") ?: ""
        val lastName = arguments?.getString("LAST_NAME") ?: ""
        val section = arguments?.getString("SECTION") ?: ""

        lifecycleScope.launch {
            val result = firebaseManager.enrollStudent(
                rollNo, firstName, lastName, section, embedding.toList()
            )
            result.onSuccess {
                Toast.makeText(context, "Student Enrolled Successfully!", Toast.LENGTH_LONG).show()
                // Pop twice to go back to Admin console
                parentFragmentManager.popBackStack()
                parentFragmentManager.popBackStack()
            }.onFailure {
                Toast.makeText(context, "Enrollment Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                isAutoCapturing = false
            }
        }
    }

    private fun verifyIdentity(faceBitmap: Bitmap) {
        val currentEmbedding = faceNetModel.getFaceEmbedding(faceBitmap)
        
        lifecycleScope.launch {
            val result = firebaseManager.getAllStudents()
            result.onSuccess { students ->
                var bestMatchName = ""
                var bestMatchRollNo = ""
                var maxSimilarity = 0f

                for (student in students) {
                    val storedEmbedding = student["face_embedding"] as? List<*>
                    if (storedEmbedding != null) {
                        val floatArray = storedEmbedding.map { (it as Number).toFloat() }.toFloatArray()
                        val similarity = faceNetModel.cosineSimilarity(currentEmbedding, floatArray)
                        
                        if (similarity > maxSimilarity) {
                            maxSimilarity = similarity
                            bestMatchName = "${student["firstName"]} ${student["lastName"]}"
                            bestMatchRollNo = student["rollNo"] as? String ?: ""
                        }
                    }
                }

                if (maxSimilarity > 0.7f) { // Identity verified
                    firebaseManager.recordAttendance(bestMatchRollNo, bestMatchName, "Present")
                    Toast.makeText(context, "Verified: $bestMatchName", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Identity Verification Failed: No Match Found", Toast.LENGTH_LONG).show()
                    isAutoCapturing = false
                }
            }.onFailure {
                Toast.makeText(context, "Error fetching data: ${it.message}", Toast.LENGTH_SHORT).show()
                isAutoCapturing = false
            }
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(codeRunnable)
        speechRecognizer?.destroy()
        faceNetModel.close()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "RecognitionFragment"
    }
}