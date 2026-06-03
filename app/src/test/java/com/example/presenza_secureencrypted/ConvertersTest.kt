package com.example.presenza_secureencrypted

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun testFloatArrayConversion() {
        val originalArray = floatArrayOf(1.0f, 2.5f, -3.0f, 0.0f)
        val json = converters.fromFloatArray(originalArray)
        
        // Check if it's a valid JSON string (roughly)
        assertTrue(json.contains("2.5"))
        
        val convertedBack = converters.toFloatArray(json)
        assertArrayEquals(originalArray, convertedBack, 0.0001f)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
