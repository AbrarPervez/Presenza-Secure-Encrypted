package com.example.presenza_secureencrypted

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

class RecognitionFragment : Fragment() {
    private var _binding: FragmentRecognitionBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var audioAnalyzer: AudioAnalyzer
    private lateinit var lipSyncDetector: LipSyncDetector
    private lateinit var faceNetModel: FaceNetModel
    private lateinit var database: AppDatabase
    private lateinit var firebaseManager: FirebaseManager

    private var isLivenessVerified = false
    private var isProcessing = false
    private var verificationCode: String = ""
    private var smoothedScore = 0f

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && 
            permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startCamera()
            audioAnalyzer.start()
        } else {
            Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
            parentFragmentManager?.popBackStack()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var isEnrollmentMode = false
    private var rollNo: String? = null
    private var firstName: String? = null
    private var lastName: String? = null
    private var section: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            isEnrollmentMode = it.getBoolean("IS_ENROLLMENT", false)
            rollNo = it.getString("ROLL_NO")
            firstName = it.getString("FIRST_NAME")
            lastName = it.getString("LAST_NAME")
            section = it.getString("SECTION")
        }

        faceNetModel = FaceNetModel(requireContext())
        database = AppDatabase.getDatabase(requireContext())
        firebaseManager = FirebaseManager()
        lipSyncDetector = LipSyncDetector()
        audioAnalyzer = AudioAnalyzer { _ -> }

        generateVerificationCode()

        if (allPermissionsGranted()) {
            startCamera()
            audioAnalyzer.start()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }

        binding.imageCaptureButton.setOnClickListener { if (!isProcessing) takePhoto() }
        binding.btnBack.setOnClickListener { parentFragmentManager?.popBackStack() }
        binding.tvVerificationCode.setOnClickListener { generateVerificationCode() }

        cameraExecutor = Executors.newFixedThreadPool(2)
    }

    private fun generateVerificationCode() {
        verificationCode = Random.nextInt(1000, 10000).toString()
        binding.tvVerificationCode.text = "CODE: $verificationCode"
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageRotationEnabled(true)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer(requireContext()) { _, mar, _, _ ->
                        if (!isProcessing && !isLivenessVerified) {
                            val currentAmp = audioAnalyzer.currentAmplitude
                            activity?.runOnUiThread { processLiveness(mar, currentAmp) }
                        }
                    })
                }
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture, imageAnalyzer)
            } catch (exc: Exception) { Log.e(TAG, "Binding failed", exc) }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processLiveness(mar: Float, amplitude: Float) {
        if (isProcessing) return
        lipSyncDetector.addData(mar, amplitude)

        val bestCorr = lipSyncDetector.getBestCorrelation()
        smoothedScore = (smoothedScore * 0.8f) + (bestCorr * 0.2f) // More smoothing to reduce "twitchiness"
        
        val status = lipSyncDetector.getSignalStatus()
        
        if (lipSyncDetector.isLipSyncValid(bestCorr) && !isLivenessVerified) {
            isLivenessVerified = true
            isProcessing = true
            binding.tvAntiSpoofing.text = "Liveness Verified!"
            binding.tvAntiSpoofing.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            takePhoto()
        } else if (!isLivenessVerified) {
            binding.tvInstruction.text = when(status) {
                1 -> "Speak Louder"
                2 -> "Move mouth more clearly"
                else -> "Say the code: $verificationCode"
            }
            binding.tvAntiSpoofing.text = "Anti-Spoofing: ${maxOf(0, (smoothedScore * 100).toInt())}%"
        }
    }

    private fun takePhoto() {
        binding.imageCaptureButton.isEnabled = false
        binding.imageCaptureButton.alpha = 0.5f
        isProcessing = true

        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    cameraProvider?.unbindAll()
                    processCapturedImage(image)
                }
                override fun onError(exc: ImageCaptureException) { resetState() }
            }
        )
    }

    private fun processCapturedImage(image: ImageProxy) {
        val bitmap = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        faceDetector.process(InputImage.fromBitmap(bitmap, rotation))
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val box = faces[0].boundingBox
                    val left = maxOf(0, box.left)
                    val top = maxOf(0, box.top)
                    val faceBitmap = Bitmap.createBitmap(bitmap, left, top, minOf(bitmap.width - left, box.width()), minOf(bitmap.height - top, box.height()))
                    if (isEnrollmentMode) enrollUser(faceBitmap) else verifyIdentity(faceBitmap)
                } else {
                    Toast.makeText(context, "No face detected", Toast.LENGTH_SHORT).show()
                    resetState()
                }
                image.close()
            }
            .addOnFailureListener { image.close(); resetState() }
    }

    private fun resetState() {
        isProcessing = false
        isLivenessVerified = false
        binding.imageCaptureButton.isEnabled = true
        binding.imageCaptureButton.alpha = 1.0f
        smoothedScore = 0f
        startCamera()
    }

    private fun enrollUser(faceBitmap: Bitmap) {
        val embedding = faceNetModel.getFaceEmbedding(faceBitmap)
        lifecycleScope.launch {
            try {
                val user = User(rollNo = rollNo ?: "", name = "$firstName $lastName", section = section ?: "", embedding = embedding)
                withContext(Dispatchers.IO) { database.userDao().insert(user) }
                firebaseManager.enrollStudent(rollNo ?: "", firstName ?: "", lastName ?: "", section ?: "", embedding.toList())
                shutdownAndExit("Enrollment Successful")
            } catch (e: Exception) { resetState() }
        }
    }

    private fun verifyIdentity(faceBitmap: Bitmap) {
        val currentEmbedding = faceNetModel.getFaceEmbedding(faceBitmap)
        lifecycleScope.launch {
            val users = withContext(Dispatchers.IO) { database.userDao().getAll() }
            var bestMatch: User? = null
            var maxSim = 0f
            for (u in users) {
                val sim = faceNetModel.cosineSimilarity(currentEmbedding, u.embedding)
                if (sim > maxSim) { maxSim = sim; bestMatch = u }
            }
            
            if (maxSim > 0.75f && bestMatch != null) {
                // TOGGLE ATTENDANCE: Add if missing, Remove if already present today
                val result = firebaseManager.toggleAttendance(bestMatch.rollNo, bestMatch.name)
                val message = if (result.getOrDefault(true)) {
                    "Welcome ${bestMatch.name}! Attendance Recorded."
                } else {
                    "Attendance Removed for ${bestMatch.name}."
                }
                shutdownAndExit(message)
            } else {
                Toast.makeText(context, "Identity Verification Failed", Toast.LENGTH_SHORT).show()
                resetState()
            }
        }
    }

    private fun shutdownAndExit(message: String) {
        activity?.runOnUiThread {
            cameraProvider?.unbindAll()
            audioAnalyzer.stop()
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            parentFragmentManager?.popBackStackImmediate()
        }
    }

    private fun allPermissionsGranted() = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioAnalyzer.stop()
        faceNetModel.close()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object { private const val TAG = "RecognitionFragment" }
}
