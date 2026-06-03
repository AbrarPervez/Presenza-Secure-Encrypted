package com.example.presenza_secureencrypted

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceNetModelTest {

    @Test
    fun testCosineSimilarity_identical() {
        val e1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val e2 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val similarity = FaceNetModel.calculateCosineSimilarity(e1, e2)
        assertEquals(1.0f, similarity, 0.0001f)
    }

    @Test
    fun testCosineSimilarity_orthogonal() {
        val e1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val e2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        val similarity = FaceNetModel.calculateCosineSimilarity(e1, e2)
        assertEquals(0.0f, similarity, 0.0001f)
    }

    @Test
    fun testCosineSimilarity_opposite() {
        val e1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val e2 = floatArrayOf(-1.0f, 0.0f, 0.0f)
        val similarity = FaceNetModel.calculateCosineSimilarity(e1, e2)
        assertEquals(-1.0f, similarity, 0.0001f)
    }

    @Test
    fun testCosineSimilarity_typical() {
        val e1 = floatArrayOf(0.5f, 0.5f)
        val e2 = floatArrayOf(0.5f, 0.0f)
        // dot = 0.25
        // normA = sqrt(0.25 + 0.25) = 0.7071
        // normB = sqrt(0.25) = 0.5
        // sim = 0.25 / (0.7071 * 0.5) = 0.25 / 0.35355 = 0.7071
        val similarity = FaceNetModel.calculateCosineSimilarity(e1, e2)
        assertEquals(0.7071f, similarity, 0.0001f)
    }
}
