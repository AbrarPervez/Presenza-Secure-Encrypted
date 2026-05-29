package com.example.presenza_secureencrypted

import android.content.Context
import android.graphics.Bitmap

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceNetModel(context: Context) {

    private val interpreter: Interpreter
    private val inputSize: Int
    private val outputSize: Int

    init {
        val model = FileUtil.loadMappedFile(context, "mobile_face_net.tflite")

        val options = Interpreter.Options()
        interpreter = Interpreter(model, options)

        // Input shape: [1, 112, 112, 3]
        val inputShape = interpreter.getInputTensor(0).shape()
        inputSize = inputShape[1]

        // Output shape: [1, 192]
        val outputShape = interpreter.getOutputTensor(0).shape()
        outputSize = outputShape[1]
    }

    private val imageProcessor by lazy {
        ImageProcessor.Builder()
            .add(
                ResizeOp(
                    inputSize,
                    inputSize,
                    ResizeOp.ResizeMethod.BILINEAR
                )
            )
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
    }

    fun getFaceEmbedding(faceBitmap: Bitmap): FloatArray {

        val tensorImage = TensorImage.fromBitmap(faceBitmap)
        val processedImage = imageProcessor.process(tensorImage)

        val outputBuffer =
            ByteBuffer.allocateDirect(outputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }

        interpreter.run(processedImage.buffer, outputBuffer)

        val result = FloatArray(outputSize)
        outputBuffer.rewind()
        outputBuffer.asFloatBuffer().get(result)

        return result
    }

    fun cosineSimilarity(e1: FloatArray, e2: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in e1.indices) {
            dotProduct += e1[i] * e2[i]
            normA += e1[i] * e1[i]
            normB += e2[i] * e2[i]
        }

        return (
                dotProduct /
                        (Math.sqrt(normA.toDouble()).toFloat() *
                                Math.sqrt(normB.toDouble()).toFloat())
                )
    }

    fun close() {
        interpreter.close()
    }
}
