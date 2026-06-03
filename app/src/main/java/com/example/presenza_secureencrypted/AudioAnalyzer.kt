package com.example.presenza_secureencrypted

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.abs

class AudioAnalyzer(private val onAmplitudeChanged: (Float) -> Unit) {

    private var audioRecord: AudioRecord? = null
    private var isRunning = false
    private var recordingThread: Thread? = null

    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    var currentAmplitude: Float = 0f
        private set

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        isRunning = true
        audioRecord?.startRecording()

        recordingThread = Thread {
            val buffer = ShortArray(bufferSize)
            while (isRunning) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    var maxAbs = 0
                    for (i in 0 until readSize) {
                        maxAbs = maxOf(maxAbs, abs(buffer[i].toInt()))
                    }
                    // Normalize to 0.0 - 1.0 range
                    currentAmplitude = maxAbs / 32768f
                    onAmplitudeChanged(currentAmplitude)
                }
            }
        }
        recordingThread?.start()
    }

    fun stop() {
        isRunning = false
        recordingThread?.join()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
