package com.example.presenza_secureencrypted

import kotlin.math.sqrt

class LipSyncDetector {
    private val marHistory = mutableListOf<Float>()
    private val ampHistory = mutableListOf<Float>()
    private val maxWindowSize = 60 

    fun addData(mar: Float, amplitude: Float) {
        marHistory.add(mar)
        ampHistory.add(amplitude)
        if (marHistory.size > maxWindowSize) {
            marHistory.removeAt(0)
            ampHistory.removeAt(0)
        }
    }

    fun getBestCorrelation(): Float {
        if (marHistory.size < 25) return 0f

        // Check offsets from -10 to +10 frames (~300ms) to handle hardware sync lag
        var maxCorr = 0f
        for (offset in -10..10) {
            val corr = calculatePearson(marHistory, ampHistory, offset)
            if (corr > maxCorr) maxCorr = corr
        }
        return maxCorr
    }

    private fun calculatePearson(mar: List<Float>, amp: List<Float>, offset: Int): Float {
        val size = mar.size
        val startMar = if (offset > 0) offset else 0
        val startAmp = if (offset < 0) -offset else 0
        val n = size - Math.abs(offset)
        
        if (n < 20) return 0f

        val subMar = mar.subList(startMar, startMar + n)
        val subAmp = amp.subList(startAmp, startAmp + n)

        val avgMar = subMar.average().toFloat()
        val avgAmp = subAmp.average().toFloat()

        var num = 0f
        var denMar = 0f
        var denAmp = 0f

        for (i in 0 until n) {
            val dMar = subMar[i] - avgMar
            val dAmp = subAmp[i] - avgAmp
            num += dMar * dAmp
            denMar += dMar * dMar
            denAmp += dAmp * dAmp
        }

        val denominator = sqrt(denMar.toDouble() * denAmp.toDouble()).toFloat()
        return if (denominator > 0.0000001f) maxOf(0f, num / denominator) else 0f
    }

    fun getSignalStatus(): Int {
        if (marHistory.size < 25) return 0
        val marVar = calculateVariance(marHistory)
        val ampVar = calculateVariance(ampHistory)
        
        // Even more lenient thresholds for sensitivity
        if (ampVar < 0.00003f) return 1 
        if (marVar < 0.00003f) return 2 
        return 0
    }

    fun isLipSyncValid(currentCorr: Float): Boolean {
        val status = getSignalStatus()
        // Reduced threshold to 0.42 for better sensitivity in user portal
        return (status == 0 && currentCorr > 0.42f) || (currentCorr > 0.58f)
    }

    private fun calculateVariance(data: List<Float>): Float {
        if (data.size < 2) return 0f
        val avg = data.average().toFloat()
        return data.map { (it - avg) * (it - avg) }.average().toFloat()
    }

    fun clear() {
        marHistory.clear()
        ampHistory.clear()
    }
}
