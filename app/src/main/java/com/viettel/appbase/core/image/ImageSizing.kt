package com.viettel.appbase.core.image

import kotlin.math.max
import kotlin.math.roundToInt

/** Pure size math for ImagePreprocessor, kept separate so it is unit-testable. */
object ImageSizing {
    /** Largest power-of-two subsampling that still keeps the longest side >= [maxSide]. */
    fun sampleSize(
        width: Int,
        height: Int,
        maxSide: Int,
    ): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    /** Size that fits within [maxSide] on the longest side, preserving aspect ratio; never upscales. */
    fun fit(
        width: Int,
        height: Int,
        maxSide: Int,
    ): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toFloat() / longest
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }
}
