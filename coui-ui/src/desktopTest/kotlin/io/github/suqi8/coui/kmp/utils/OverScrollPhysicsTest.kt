// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * The drag damping and the release rebound of the list overscroll, pinned to the ColorOS 17
 * implementation they are ported from.
 *
 * The drag curve is `COUIPhysicalAnimationUtil.calcRealOverScrollDist`
 * (`com.coui.appcompat.animation.COUIPhysicalAnimationUtil`), called from
 * `COUIRecyclerView.scrollByInternal` for every drag delta the list cannot consume. The rebound is
 * `SpringOverScroller.ReboundOverScroller.springBack` (`com.coui.appcompat.scroll.SpringOverScroller`).
 */
class OverScrollPhysicsTest {

    /** Reference device geometry the numbers below were measured on: 1240x2772 px at 620 dpi. */
    private val pxPerDp = 3.875f
    private val screenHeightPx = 2772f

    private fun overscrollOfDrag(dragPx: Float, rangePx: Float = screenHeightPx): Float = SpringMath.obtainOverScrollOffset(dragPx, rangePx)

    @Test
    fun aSixHundredPixelDragOverscrollsAboutTwoHundredAndThirtyPixels() {
        val dragPx = 600f
        // The same gesture expressed in dp and converted back with the device density, so the test
        // keeps meaning if the geometry above is ever re-measured on another screen.
        val dragDp = dragPx / pxPerDp
        val offset = overscrollOfDrag(dragDp * pxPerDp)

        // `range * (1 - exp(-0.4 * drag / range))` at 600 / 2772.
        assertEquals(229.9f, offset, absoluteTolerance = 2f)
        assertTrue(offset in 215f..245f, "a 600px drag must overscroll about 230px, got $offset")
        assertTrue(offset / dragPx in 0.36f..0.40f, "the drag ratio must land near 0.38, got ${offset / dragPx}")

        // The curve this replaced, `x - x^2 + x^3/3`, is still what the grid spring chain uses. It
        // overscrolls 479.5px for the same drag — the old build measured 455px on the reference
        // device — so the band above is what keeps the list off it.
        val previousCurve = SpringMath.obtainDampingDistance(dragDp * pxPerDp / screenHeightPx, screenHeightPx)
        assertEquals(479.5f, previousCurve, absoluteTolerance = 1f)
        assertTrue(previousCurve > 245f, "the replaced cubic must stay outside the new band, got $previousCurve")
    }

    @Test
    fun theCurveAgreesWithTheReferencePerStepRecursion() {
        // `COUIRecyclerView` applies `0.4 * delta * (1 - |offset| / range)` to every unconsumed drag
        // delta, step by step, rather than to the accumulated travel. The closed form below has to
        // land on the same place for a normal touch stream.
        var reference = 0f
        var touch = 0f
        repeat(24) {
            val delta = 25f
            reference += 0.4f * delta * (1f - reference / screenHeightPx)
            touch += delta
        }

        assertEquals(reference, overscrollOfDrag(touch), absoluteTolerance = 2f)
    }

    @Test
    fun theCurveIsMonotonicAndSaturatesAtTheScrollRange() {
        var previous = 0f
        var touch = 0f
        while (touch < 20_000f) {
            touch += 10f
            val offset = overscrollOfDrag(touch)
            assertTrue(offset >= previous, "the curve must not fold back at $touch px, got $offset after $previous")
            assertTrue(offset <= screenHeightPx, "the curve must stay inside the range, got $offset")
            previous = offset
        }

        assertTrue(previous > 0.90f * screenHeightPx, "a very long drag must approach the range, got $previous")
    }

    @Test
    fun theInverseRecoversTheFingerTravel() {
        for (drag in listOf(10f, 154.84f, 600f, 5_000f, -10f, -600f, -5_000f)) {
            val offset = overscrollOfDrag(drag)
            val recovered = SpringMath.obtainOverScrollTouchDistance(offset, screenHeightPx)
            assertEquals(drag, recovered, absoluteTolerance = 0.5f)
        }
    }

    @Test
    fun theReleaseReboundIsTheReferenceSpringBack() {
        val params = SpringMath.SPRING_BACK

        // The Origami pair `springBack` configures is (12.19, 2.15 * 16), i.e. 37.57 / 209.93 on
        // paper, but `calculateWithRk4` integrates from the previous step's predicted position, so
        // the spring the list actually runs is omega 14.0 / zeta 1.05 — what the reference device
        // measures.
        assertEquals(195.9, params.tension, absoluteTolerance = 0.01)
        assertEquals(29.5, params.friction, absoluteTolerance = 0.01)
        assertEquals(13.996, params.angularFrequency, absoluteTolerance = 0.005)
        assertEquals(1.054, params.dampingRatio, absoluteTolerance = 0.001)
        assertEquals(0.25, params.restDisplacement)

        // A 230px overscroll — what a 600px drag leaves — is back in place in about 0.75s, and is
        // down to a few pixels by 0.47s.
        val engine = SpringEngine()
        engine.start(startValue = 230f, targetValue = 0f, initialVel = 0f, params = params)
        var frames = 0
        var visuallyBackAt = -1
        while (frames < 600 && !engine.step(1f / 60f)) {
            frames++
            if (visuallyBackAt < 0 && engine.currentPos <= 4.7) visuallyBackAt = frames
        }

        assertEquals(0.750f, frames / 60f, absoluteTolerance = 0.02f)
        assertEquals(0.467f, visuallyBackAt / 60f, absoluteTolerance = 0.02f)
        assertEquals(0.0, engine.currentPos)
    }

    @Test
    fun theHorizontalReboundIsTheStifferSpringBack() {
        val vertical = SpringMath.SPRING_BACK
        val horizontal = SpringMath.SPRING_BACK_HORIZONTAL

        // `COUIRecyclerView.setLayoutManager` hands a horizontal list 3.2 * 16 Origami tension
        // instead of 2.15 * 16, i.e. 270.74 against 209.93 on paper; `calculateWithRk4`'s one-frame
        // lag softens both, to omega 15.94 / zeta 0.94 here against 14.0 / 1.05 for the vertical one.
        assertEquals(254.2, horizontal.tension, absoluteTolerance = 0.01)
        assertEquals(29.9, horizontal.friction, absoluteTolerance = 0.01)
        assertEquals(15.943, horizontal.angularFrequency, absoluteTolerance = 0.005)
        assertEquals(0.938, horizontal.dampingRatio, absoluteTolerance = 0.001)
        assertTrue(
            horizontal.tension > vertical.tension,
            "the horizontal rebound must be the stiffer one, got ${horizontal.tension} against ${vertical.tension}",
        )

        // A 200px horizontal overscroll is back in place in about 0.45s.
        val engine = SpringEngine()
        engine.start(startValue = 200f, targetValue = 0f, initialVel = 0f, params = horizontal)
        var frames = 0
        while (frames < 600 && !engine.step(1f / 60f)) frames++

        assertEquals(0.450f, frames / 60f, absoluteTolerance = 0.02f)
        assertEquals(0.0, engine.currentPos)
    }

    @Test
    fun theReleaseMovesTheContentOnTheVeryFirstFrame() = runBlocking {
        val engine = SpringEngine()
        var frameTimeNanos = 1_000_000_000L
        var polls = 0
        var firstFramePoll = -1
        var firstFramePos = Float.NaN
        var frames = 0

        engine.runSettleAnimation(
            startValue = 200f,
            initialVelocity = 0f,
            params = SpringMath.SPRING_BACK,
            nextFrameNanos = {
                polls++
                frameTimeNanos += 16_666_667L
                frameTimeNanos
            },
            onFrame = { pos ->
                if (frames == 0) {
                    firstFramePoll = polls
                    firstFramePos = pos
                }
                frames++
            },
            onSettle = {},
        )

        // The settle has to advance on the first frame it is handed. Spending that frame only to
        // measure the interval made the content sit still for one frame after every release.
        assertEquals(1, firstFramePoll, "the first frame must already move the spring")
        assertTrue(firstFramePos < 200f, "the first frame must move the spring, got $firstFramePos")
        assertTrue(frames > 10, "the spring should still take many frames, took $frames")
    }

    @Test
    fun theFirstFrameCountsTheTimeSinceTheGestureEnded() = runBlocking {
        // The animation coroutine is dispatched after the gesture callback, so the first frame can
        // arrive a frame late. The delta still has to be measured from the gesture end, otherwise
        // that late frame steps almost nothing and the content visibly stalls at the peak.
        val startedAt = TimeSource.Monotonic.markNow()
        delay(20)

        val engine = SpringEngine()
        var firstFramePos = Float.NaN
        var frames = 0
        var frameTimeNanos = 1_000_000_000L

        engine.runSettleAnimation(
            startValue = 200f,
            initialVelocity = 0f,
            params = SpringMath.SPRING_BACK,
            startedAt = startedAt,
            nextFrameNanos = {
                frameTimeNanos += 16_666_667L
                frameTimeNanos
            },
            onFrame = { pos ->
                if (frames == 0) firstFramePos = pos
                frames++
            },
            onSettle = {},
        )

        // One clamped frame from 200px lands at about 196px; counting from the first callback
        // instead would leave it at 199.98px.
        assertTrue(firstFramePos < 198f, "the first frame must step the elapsed time, got $firstFramePos")
        assertTrue(frames > 10, "the spring should still take many frames, took $frames")
    }

    @Test
    fun theFirstFrameCatchesUpAFrameThatArrivedLate() = runBlocking {
        // On the reference device the finger-up callback regularly lands two frames after the
        // gesture ended — the 132 trace puts the first settle frame 32..47ms after the touch-up.
        // The settle has to count that time: capping the first step at one display frame left the
        // whole rebound a frame behind, which is exactly the +16..+25ms the frame-by-frame fit
        // showed. 33ms of SPRING_BACK from 200px is 184px; one frame would leave 196px.
        val startedAt = TimeSource.Monotonic.markNow()
        delay(33)

        val engine = SpringEngine()
        var firstFramePos = Float.NaN
        var frames = 0
        var frameTimeNanos = 1_000_000_000L

        engine.runSettleAnimation(
            startValue = 200f,
            initialVelocity = 0f,
            params = SpringMath.SPRING_BACK,
            startedAt = startedAt,
            nextFrameNanos = {
                frameTimeNanos += 16_666_667L
                frameTimeNanos
            },
            onFrame = { pos ->
                if (frames == 0) firstFramePos = pos
                frames++
            },
            onSettle = {},
        )

        assertTrue(firstFramePos < 190f, "the first frame must cover the late callback, got $firstFramePos")
        // `delay` overshoots on a loaded machine, and the step is capped at 100ms (121px), so the
        // lower bound only has to keep the assertion off a runaway catch-up.
        assertTrue(firstFramePos > 110f, "the first frame must not overshoot the elapsed time, got $firstFramePos")
    }

    @Test
    fun theFirstStepIsCappedSoAStalledCallbackCannotTeleportTheContent() = runBlocking {
        // The catch-up above is bounded: a main thread that was blocked for a third of a second
        // must not skip most of the rebound in one step. 100ms of SPRING_BACK from 200px is 121px;
        // the uncapped 300ms would be 20px.
        val startedAt = TimeSource.Monotonic.markNow()
        delay(300)

        val engine = SpringEngine()
        var firstFramePos = Float.NaN
        var frames = 0
        var frameTimeNanos = 1_000_000_000L

        engine.runSettleAnimation(
            startValue = 200f,
            initialVelocity = 0f,
            params = SpringMath.SPRING_BACK,
            startedAt = startedAt,
            nextFrameNanos = {
                frameTimeNanos += 16_666_667L
                frameTimeNanos
            },
            onFrame = { pos ->
                if (frames == 0) firstFramePos = pos
                frames++
            },
            onSettle = {},
        )

        assertTrue(firstFramePos > 100f, "the catch-up must stop at the cap, got $firstFramePos")
    }
}
