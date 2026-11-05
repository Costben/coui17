// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Both rebound curves `SpringOverScroller` defines. Overscroll now uses [SpringMath.SPRING_BACK]
 * from both entry points; [SpringMath.FLING_REBOUND] stays as `SpringEngine`'s default, so its end
 * conditions are still worth pinning.
 */
class SpringEngineThresholdTest {

    private val frame = 1f / 60f

    private fun settleFrames(params: SpringParams, startValue: Float, initialVel: Float = 0f): Int {
        val engine = SpringEngine()
        engine.start(startValue = startValue, targetValue = 0f, initialVel = initialVel, params = params)
        var frames = 0
        while (frames < 600) {
            frames++
            if (engine.step(frame)) return frames
        }
        error("the spring never settled; still at ${engine.currentPos} after $frames frames")
    }

    @Test
    fun theFingerUpReboundIsTheFasterOfTheTwoCurves() {
        val release = settleFrames(SpringMath.SPRING_BACK, startValue = 100f) * frame
        val fling = settleFrames(SpringMath.FLING_REBOUND, startValue = 100f) * frame

        // `springBack` (omega 14.0 rad/s, zeta 1.05, stops at 0.25px) is back in about 0.67s from
        // 100px; the fling curve (omega 6.32, zeta 1.15, stops once it is slower than 5px/s) needs
        // about 1.35s.
        assertTrue(release in 0.60f..0.75f, "the finger-up rebound should take ~0.67s, took ${release}s")
        assertTrue(fling in 1.25f..1.45f, "the fling rebound should take ~1.35s, took ${fling}s")
    }

    @Test
    fun neitherReboundOvershoots() {
        for (params in listOf(SpringMath.SPRING_BACK, SpringMath.FLING_REBOUND)) {
            val engine = SpringEngine()
            engine.start(startValue = 120f, targetValue = 0f, initialVel = 0f, params = params)
            var frames = 0
            while (frames < 600 && !engine.step(frame)) {
                assertTrue(
                    engine.currentPos >= -0.001,
                    "an overdamped rebound never crosses the target, got ${engine.currentPos}",
                )
                frames++
            }
            assertEquals(0.0, engine.currentPos)
            assertEquals(0.0, engine.velocity)
        }
    }

    @Test
    fun aFastPassThroughTheTargetIsNotTheEndOfTheAnimation() {
        // Only the fling curve gates on speed, so it is the one that must keep running here.
        val engine = SpringEngine()
        engine.start(
            startValue = 0f,
            targetValue = 100f,
            initialVel = 6000f,
            params = SpringMath.FLING_REBOUND,
        )

        var frames = 0
        var crossedAtSpeed = false
        while (frames < 240) {
            val settled = engine.step(frame)
            if (!settled && engine.currentPos > 100.0) crossedAtSpeed = true
            if (settled) break
            frames++
        }

        assertTrue(crossedAtSpeed, "a 6000px/s kick must carry the spring past its target")
    }

    @Test
    fun theReleaseReboundStopsOnDisplacementAlone() {
        val engine = SpringEngine()
        engine.start(startValue = 2f, targetValue = 0f, initialVel = 0f, params = SpringMath.SPRING_BACK)

        var frames = 0
        while (frames < 300 && !engine.step(frame)) frames++

        assertTrue(frames < 60, "a 2px spring back is over in about 0.28s, took $frames frames")
        assertEquals(0.0, engine.currentPos)
    }
}
