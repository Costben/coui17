// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.spring
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloatingBarDynamicsTest {
    @Test
    fun retargetingEveryFrameStillMoves() = runBlocking {
        val frames = Channel<Long>(Channel.UNLIMITED)
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(frames.receive())
        }
        val motion = COUITouchSpring(0f)
        val spec = spring<Float>(dampingRatio = 0.85f, stiffness = 1754f)
        for (frame in 0L..12L) {
            val job = launch(clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(100f + frame, spec) }
            val previous = motion.value
            frames.send(frame * 16_000_000L)
            yield()
            if (frame > 0) assertTrue(motion.value > previous, "Drag must advance on frame $frame")
            job.cancelAndJoin()
        }
        assertTrue(motion.value > 90f)
    }

    @Test
    fun retargetingKeepsPositionAndVelocity() = runBlocking {
        val frames = Channel<Long>(Channel.UNLIMITED)
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(frames.receive())
        }
        val motion = COUITouchSpring(0f)
        val spec = spring<Float>(dampingRatio = 0.85f, stiffness = 1754f)
        var job = launch(clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(100f, spec) }
        for (frame in 0L..2L) {
            frames.send(frame * 16_000_000L)
            yield()
        }
        val position = motion.value
        val velocity = motion.velocity
        assertTrue(position > 0f && velocity > 0f)
        job.cancelAndJoin()
        assertEquals(velocity, motion.velocity, absoluteTolerance = 0.001f)
        job = launch(clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(150f, spec) }
        assertEquals(position, motion.value, absoluteTolerance = 0.001f)
        assertEquals(velocity, motion.velocity, absoluteTolerance = 0.001f)
        frames.send(48_000_000L)
        yield()
        assertTrue(motion.value > position)
        frames.send(64_000_000L)
        yield()
        assertTrue(motion.value > position)
        job.cancelAndJoin()
    }

    @Test
    fun completedSpringDoesNotConsumeIdleTimeOnNextGesture() = runBlocking {
        val frames = Channel<Long>(Channel.UNLIMITED)
        val clock = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(frames.receive())
        }
        val motion = COUITouchSpring(0f)
        val spec = spring<Float>(dampingRatio = 0.85f, stiffness = 1754f)
        var job = launch(clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(100f, spec) }
        for (frame in 0L..120L) {
            if (job.isCompleted) break
            frames.send(frame * 16_000_000L)
            yield()
        }
        assertTrue(job.isCompleted)
        assertEquals(100f, motion.value)
        job = launch(clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(200f, spec) }
        frames.send(10_000_000_000L)
        yield()
        assertEquals(100f, motion.value)
        frames.send(10_016_000_000L)
        yield()
        assertTrue(motion.value > 100f && motion.value < 200f)
        job.cancelAndJoin()
    }

    @Test
    fun releaseVelocityUsesTheNativeMillisecondUnits() {
        assertEquals(0.3f, couiNativeTouchVelocity(1000f), absoluteTolerance = 0.00001f)
        assertEquals(-0.6f, couiNativeTouchVelocity(-2000f), absoluteTolerance = 0.00001f)
    }

    @Test
    fun accessoryFadesOnlyAfterHalfItsTransformedAreaOverlaps() {
        val accessory = Rect(0f, 0f, 100f, 100f)
        assertEquals(1f, couiAccessoryOverlapAlpha(accessory, Rect(50f, 0f, 150f, 100f)))
        assertEquals(0.5f, couiAccessoryOverlapAlpha(accessory, Rect(25f, 0f, 150f, 100f)))
        assertEquals(0f, couiAccessoryOverlapAlpha(accessory, Rect(-10f, -10f, 150f, 150f)))
        assertEquals(1f, couiAccessoryOverlapAlpha(accessory, Rect(100f, 0f, 150f, 100f)))
        assertEquals(1f, couiAccessoryOverlapAlpha(accessory, Rect(0f, 100f, 100f, 150f)))
    }
}
