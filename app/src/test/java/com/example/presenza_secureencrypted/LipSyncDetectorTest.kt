package com.example.presenza_secureencrypted

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LipSyncDetectorTest {

    @Test
    fun testCorrelatedData() {
        val detector = LipSyncDetector()
        // Simulate correlated data (MAR increases as Amplitude increases)
        for (i in 0 until 50) {
            val value = i / 50f
            detector.addData(value, value)
        }
        
        val correlation = detector.getCorrelation()
        println("Correlation (Perfect): $correlation")
        assertTrue("Correlation should be high for perfect linear relationship", correlation > 0.9f)
        assertTrue("Lip sync should be valid", detector.isLipSyncValid())
    }

    @Test
    fun testUncorrelatedData() {
        val detector = LipSyncDetector()
        // Simulate uncorrelated random data
        val random = java.util.Random()
        for (i in 0 until 50) {
            detector.addData(random.nextFloat(), random.nextFloat())
        }
        
        val correlation = detector.getCorrelation()
        println("Correlation (Random): $correlation")
        // Random correlation is likely to be low, but not guaranteed to be 0. 
        // 0.6 is our threshold.
        assertFalse("Lip sync should be invalid for random data", detector.isLipSyncValid())
    }

    @Test
    fun testInverseCorrelatedData() {
        val detector = LipSyncDetector()
        // Simulate inverse correlated data
        for (i in 0 until 50) {
            detector.addData(i / 50f, 1f - (i / 50f))
        }
        
        val correlation = detector.getCorrelation()
        println("Correlation (Inverse): $correlation")
        assertTrue("Correlation should be negative", correlation < 0f)
        assertFalse("Lip sync should be invalid for inverse correlation", detector.isLipSyncValid())
    }
}
