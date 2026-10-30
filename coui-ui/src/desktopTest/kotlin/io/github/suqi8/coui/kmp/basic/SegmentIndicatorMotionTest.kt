// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val FRAME_SECONDS = 1.0 / 120.0
private const val REST_HEIGHT = 124f
private const val SEGMENT_WIDTH = 480f

class SegmentIndicatorMotionTest {

    private fun motion() = SegmentIndicatorMotion(center = 240f, width = SEGMENT_WIDTH, height = REST_HEIGHT)

    private fun SegmentIndicatorMotion.selectSecond() = select(
        targetCenter = 720f,
        targetWidth = SEGMENT_WIDTH,
        restHeight = REST_HEIGHT,
        firstCenter = 240f,
        lastCenter = 720f,
        maxExtraWidth = 27f,
        compact = false,
    )

    @Test
    fun selectingSquashesTheIndicatorOnItsWayOver() {
        val motion = motion()
        motion.selectSecond()
        var minHeight = Float.MAX_VALUE
        var maxWidth = 0f
        repeat(240) {
            motion.advance(FRAME_SECONDS)
            minHeight = minOf(minHeight, motion.height)
            maxWidth = maxOf(maxWidth, motion.width)
        }
        assertEquals(REST_HEIGHT * SEGMENT_SQUASH_RATIO, minHeight, 0.01f, "the height bottoms out at 0.9h")
        assertTrue(maxWidth > SEGMENT_WIDTH, "the width swells while the height flattens, got $maxWidth")
    }

    @Test
    fun theSquashRestsAgainstItsBoundBeforeRecovering() {
        val motion = motion()
        motion.selectSecond()
        var framesAtBound = 0
        repeat(240) {
            motion.advance(FRAME_SECONDS)
            if (motion.height == REST_HEIGHT * SEGMENT_SQUASH_RATIO) framesAtBound++
        }
        assertTrue(framesAtBound >= 3, "a bound must clamp, not end, the spring: rested $framesAtBound frames")
    }

    @Test
    fun everySpringSettlesOnTheTarget() {
        val motion = motion()
        motion.selectSecond()
        repeat(600) { motion.advance(FRAME_SECONDS) }
        assertEquals(720f, motion.centerX)
        assertEquals(SEGMENT_WIDTH, motion.width)
        assertEquals(REST_HEIGHT, motion.height)
        assertFalse(motion.isPositionRunning)
    }

    @Test
    fun theRubberBandSaturatesAtItsLimit() {
        val limit = 20f
        assertEquals(0f, segmentRubberBand(0f, limit))
        assertTrue(segmentRubberBand(50f, limit) < segmentRubberBand(500f, limit))
        assertTrue(segmentRubberBand(100_000f, limit) < limit)
        assertEquals(-segmentRubberBand(120f, limit), segmentRubberBand(-120f, limit))
    }

    @Test
    fun theIndicatorLeansTowardTheFingerAndSpringsBack() {
        val motion = motion()
        motion.beginDrag(300f)
        motion.dragTo(400f, overDistance = 20f)
        repeat(240) { motion.advance(FRAME_SECONDS) }
        assertEquals(240f + segmentRubberBand(100f, 20f), motion.centerX, 1f)

        motion.settle(240f)
        repeat(240) { motion.advance(FRAME_SECONDS) }
        assertEquals(240f, motion.centerX)
    }

    @Test
    fun aTargetChangeKeepsTheSpringsMomentum() {
        val spring = CouiSpring(0f, bounce = 0f, response = 0.4f)
        spring.animateTo(100f)
        repeat(12) { spring.step(FRAME_SECONDS) }
        val speed = spring.velocity
        spring.animateTo(200f)
        spring.step(FRAME_SECONDS)
        assertTrue(abs(spring.velocity) >= abs(speed), "retargeting must not restart from rest")
    }
}

class SegmentCapsuleTest {

    @Test
    fun theCapsuleFillsItsRectAndEasesPastTheCircularTangent() {
        val rect = Rect(10f, 20f, 490f, 144f)
        val path = Path().apply { addSegmentCapsule(rect) }
        val bounds = path.getBounds()
        assertEquals(rect.left, bounds.left, 0.01f)
        assertEquals(rect.top, bounds.top, 0.01f)
        assertEquals(rect.right, bounds.right, 0.01f)
        assertEquals(rect.bottom, bounds.bottom, 0.01f)
    }

    @Test
    fun aRectTooNarrowForTheTailStillFitsItsBounds() {
        val rect = Rect(0f, 0f, 130f, 124f)
        val bounds = Path().apply { addSegmentCapsule(rect) }.getBounds()
        assertEquals(130f, bounds.width, 0.01f)
        assertEquals(124f, bounds.height, 0.01f)
    }
}
