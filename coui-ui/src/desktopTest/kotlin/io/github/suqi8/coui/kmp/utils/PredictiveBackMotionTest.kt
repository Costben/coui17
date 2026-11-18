// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PredictiveBackMotionTest {

    @Test
    fun trackerIgnoresLeadingZeroSentinelAndScalesDisplacement() {
        var now = 1_000L
        val tracker = PredictiveBackDragTracker(
            followFactor = 0.65f,
            pauseTimeoutMs = 90L,
            velocityWindowMs = 100L,
            clockMillis = { now },
        )

        assertEquals(Offset.Zero, tracker.record(0f, 0f, frameTimeMillis = 1_000L))
        assertEquals(Offset.Zero, tracker.record(100f, 200f, frameTimeMillis = 1_010L))

        val offset = tracker.record(200f, 100f, frameTimeMillis = 1_030L)
        assertEquals(65f, offset.x, 1e-3f)
        assertEquals(-65f, offset.y, 1e-3f)
    }

    @Test
    fun trackerComputesVelocityAndClampsToMaxSpeed() {
        var now = 2_000L
        val tracker = PredictiveBackDragTracker(
            followFactor = 0.65f,
            pauseTimeoutMs = 90L,
            velocityWindowMs = 100L,
            clockMillis = { now },
        )

        tracker.record(100f, 100f, frameTimeMillis = 1_950L)
        tracker.record(300f, 100f, frameTimeMillis = 2_000L)

        // Raw dx = 200px in 0.05s = 4000 px/s, scaled by 0.65 = 2600 px/s -> clamped to 1500.
        val clamped = tracker.velocityPxPerSec(maxSpeedPx = 1_500f)
        assertEquals(1_500f, clamped.x, 1e-2f)
        assertEquals(0f, clamped.y, 1e-2f)

        // Pausing longer than 90ms zeroes out release fling velocity.
        now = 2_120L
        assertEquals(Offset.Zero, tracker.velocityPxPerSec(maxSpeedPx = 1_500f))
    }

    @Test
    fun blurRadiusTracksProgressAndOffsetAndClearsAtZero() {
        assertEquals(
            0f,
            calculatePredictiveBackBlurRadiusPx(
                progressFraction = 0f,
                offsetDistancePx = 0f,
                maxBlurRadiusPx = 84f,
                blurOffsetSpanPx = 480f,
            ),
            1e-3f,
        )
        assertEquals(
            42f,
            calculatePredictiveBackBlurRadiusPx(
                progressFraction = 0.2f,
                offsetDistancePx = 240f,
                maxBlurRadiusPx = 84f,
                blurOffsetSpanPx = 480f,
            ),
            1e-3f,
        )
        val clamped = calculatePredictiveBackBlurRadiusPx(
            progressFraction = 1.5f,
            offsetDistancePx = 900f,
            maxBlurRadiusPx = 84f,
            blurOffsetSpanPx = 480f,
        )
        assertTrue(clamped <= 84f + 1e-3f && clamped >= 84f - 1e-3f)
    }
}
