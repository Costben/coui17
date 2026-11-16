// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.slider

// Ported from COUI-Kit (ItosEO/COUI-Kit@8a09da4) coui/src/main/java/com/itos/coui/slider/CouiSliderMotion.kt. Extended with custom keypoints, magnet snapping, moveByFinger guard, and haptic controls.

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import io.github.suqi8.coui.kmp.kit.motion.CouiFrameHandle
import io.github.suqi8.coui.kmp.kit.motion.CouiSpring
import io.github.suqi8.coui.kmp.kit.motion.postCouiFrameCallback
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Port of COUISeekBarAdvanced / SectionAnimationHandler keeping logical value and visual position separate. */
internal class CouiSliderMotion(
    private val range: ClosedFloatingPointRange<Float>,
    private val steps: Int,
    private val section: Boolean,
    private val density: Float,
    initialValue: Float,
    private val onValueChange: (Float) -> Unit,
    private val onValueChangeFinished: () -> Unit,
    private val keyPointFractions: FloatArray = floatArrayOf(),
    private val magnetThreshold: Float = 0f,
    private val hapticEffect: Int = 1,
    private val moveByFinger: Boolean = false,
    private val feedback: (Int, Boolean) -> Unit,
) {
    private val intervals = steps + 1
    private val span = range.endInclusive - range.start
    private var lastExternalValue = initialValue
    private var pendingRelease = false
    private var downX = 0f
    private var anchorSection = -1
    private var anchorX = 0f
    private var direction = 0
    private var lastFingerX = 0f
    private var fingerOffset = 0f
    private var dragAnimValue = 0f
    private var dragAnimStart = 0f
    private var dragAnimEnd = Float.NaN
    private var fastRelease = false
    private var bumpingEnd = false
    private var transition: CouiFloatAnimator? = null
    private var dragSnap: CouiFloatAnimator? = null
    private var flingHandle: CouiFrameHandle? = null
    private var pressed = false
    private var dragging = false
    private var lastStepHapticMark: TimeMark? = null

    val height: CouiSpring = CouiSpring(0f) {
        if (pendingRelease && heightFraction >= 0.5f) {
            pendingRelease = false
            height.animateTo(0f, 0.5f)
        }
    }
    val thumbRadius = CouiSpring(6f)
    val texture = CouiSpring(0f)
    val thumbCover = CouiSpring(100f)
    val position = CouiSpring(0f)
    val glitter = CouiSpring(0f)
    var value by mutableFloatStateOf(initialValue.coerceIn(range))
        private set
    var length = 0f
        private set
    val heightFraction: Float get() = (height.value / 100f).coerceIn(0f, 1f)
    val fraction: Float get() = normalize(value)
    val visualFraction: Float get() = if (length > 0f) (position.value / length).coerceIn(0f, 1f) else fraction

    fun syncValue(externalValue: Float) {
        if (externalValue == lastExternalValue) return
        lastExternalValue = externalValue
        val next = externalValue.coerceIn(range)
        // Callers may round quantized levels to integers; avoid interrupting visual motion for the same step.
        if (next == value || (steps > 0 && index(normalize(next)) == index(fraction))) return
        cancelGesture()
        stopPositionAnimations()
        value = next
        position.snapTo(fraction * length)
    }

    fun resize(nextLength: Float) {
        if (nextLength == length) return
        cancelGesture()
        stopPositionAnimations()
        length = nextLength.coerceAtLeast(0f)
        position.snapTo(fraction * length)
    }

    fun press(fingerX: Float) {
        stopPositionAnimations()
        resetAnchor()
        pressed = true
        dragging = false
        pendingRelease = false
        bumpingEnd = false
        downX = fingerX
        height.animateTo(100f, 0.5f)
        thumbRadius.animateTo(8f, 0.5f, 0.3f, minimumChange = 1f / density)
        texture.animateTo(100f, 0.3f)
        thumbCover.snapTo(100f)
        thumbCover.animateTo(0f, 0.5f)
    }

    fun drag(fingerX: Float, velocity: Float) {
        if (!pressed || length <= 0f) return
        dragging = true
        val x = fingerX.coerceIn(0f, length)
        if (section) {
            dragSection(x)
        } else {
            val frac = resolveFraction(x / length)
            setFraction(frac, vibrate = true)
            // Native FlexibleFollowHand uses permille visibility threshold and keeps a uniform response across long jumps.
            position.animateTo(frac * length, 0.15f, minimumChange = length / 1000f)
        }
        if (x < length) bumpingEnd = false
        if (!section && x >= length && velocity > 0f && !bumpingEnd) startGlitter(velocity)
    }

    fun release(fingerX: Float, velocity: Float, frameSeconds: Float) {
        if (!pressed) return
        releaseAppearance()
        pressed = false
        if (!dragging) {
            if (!moveByFinger) {
                return
            }
            val target = quantize(normalizePosition(fingerX))
            if (section) {
                setFraction(target, vibrate = true)
                animateSection(target * length, 200L)
            } else {
                if (target != fraction && (target == 0f || target == 1f) && hapticEffect != 0) feedback(306, false)
                position.animateTo(target * length, 0.3f, onFrame = {
                    setFraction(normalizePosition(it), vibrate = false, truncate = true)
                }) {
                    setFraction(target, vibrate = false)
                    onValueChangeFinished()
                }
            }
            return
        }
        dragging = false
        if (section) {
            if (dragSnap?.isRunning == true) {
                fastRelease = true
            } else {
                val segment = length / intervals
                val target = (index(fraction) + ((fingerX.coerceIn(0f, length) - fraction * length) / segment).toInt())
                    .coerceIn(0, intervals)
                snapSection(target)
                resetAnchor()
            }
        } else {
            val frac = resolveFraction(normalizePosition(fingerX))
            position.snapTo(frac * length)
            setFraction(frac, vibrate = true)
            if (abs(velocity) >= 100f) {
                startFling(velocity.coerceIn(-8000f, 8000f), frameSeconds)
            } else {
                onValueChangeFinished()
            }
        }
    }

    fun cancelGesture() {
        if (pressed) releaseAppearance()
        pressed = false
        dragging = false
        fastRelease = false
        stopPositionAnimations()
        resetAnchor()
    }

    fun setFromAction(nextValue: Float) {
        cancelGesture()
        setFraction(quantize(normalize(nextValue)), vibrate = true)
        position.snapTo(fraction * length)
        onValueChangeFinished()
    }

    fun dispose() {
        pressed = false
        stopPositionAnimations()
        listOf(height, thumbRadius, texture, thumbCover, position, glitter).forEach { it.cancel() }
    }

    private fun releaseAppearance() {
        if (height.isRunning && heightFraction < 0.5f) {
            pendingRelease = true
        } else {
            height.animateTo(0f, 0.5f)
        }
        thumbRadius.animateTo(6f, 0.5f, minimumChange = 1f / density)
        texture.animateTo(0f, 0.3f)
        thumbCover.animateTo(100f, 0.3f)
    }

    private fun dragSection(x: Float) {
        val segment = length / intervals
        if (anchorSection < 0) {
            anchorSection = index(normalizePosition(downX))
            anchorX = anchorSection * segment
            setFraction(anchorSection.toFloat() / intervals, vibrate = true)
            position.snapTo(anchorX)
            direction = if (x < anchorX) -1 else 1
            lastFingerX = x
            return
        }
        val nextDirection = when {
            x > lastFingerX + 0.5f -> 1
            x < lastFingerX - 0.5f -> -1
            else -> 0
        }
        if (nextDirection != 0 && nextDirection == -direction) {
            direction = nextDirection
            if (anchorSection != index(fraction)) {
                anchorSection = index(fraction)
                anchorX = anchorSection * segment
                dragAnimValue = 0f
            }
            dragSnap?.cancel()
        }
        fingerOffset = x - anchorX
        val crossed = ((fingerOffset + if (fingerOffset >= 0f) 0.1f else -0.1f) / segment).toInt()
        val offset = crossed * segment
        if ((anchorSection + crossed).coerceIn(0, intervals) != index(fraction)) {
            startDragSnap(anchorX + offset)
        } else if (dragSnap?.isRunning != true) {
            position.snapTo((anchorX + offset + (fingerOffset - offset) * 0.6f).coerceIn(0f, length))
        }
        lastFingerX = x
    }

    private fun startDragSnap(target: Float) {
        if (position.value == target || (dragSnap?.isRunning == true && dragAnimEnd == target)) return
        dragSnap?.cancel()
        dragAnimStart = anchorX
        dragAnimEnd = target
        dragSnap = CouiFloatAnimator(
            from = dragAnimValue,
            to = target - anchorX,
            durationMs = 100L,
            interpolate = DragInterpolator::transform,
            onUpdate = { animatedValue ->
                dragAnimValue = animatedValue
                val x = (dragAnimStart + dragAnimValue * 0.4f + fingerOffset * 0.6f).coerceIn(0f, length)
                position.snapTo(x)
                val selected = if (dragAnimEnd > dragAnimStart) {
                    (x / (length / intervals)).roundToInt()
                } else {
                    ceil(x / (length / intervals)).toInt()
                }
                setFraction(selected.coerceIn(0, intervals).toFloat() / intervals, vibrate = true)
            },
            onEnd = { _ ->
                if (fastRelease) {
                    fastRelease = false
                    val targetSection = index(normalizePosition(lastFingerX))
                    resetAnchor()
                    snapSection(targetSection)
                }
            },
        ).also { it.start() }
    }

    private fun snapSection(target: Int) {
        setFraction(target.toFloat() / intervals, vibrate = false)
        val x = fraction * length
        if (abs(position.value - x) < 0.5f) {
            position.snapTo(x)
            onValueChangeFinished()
        } else {
            animateSection(x, 150L)
        }
    }

    private fun animateSection(target: Float, durationMs: Long) {
        transition?.cancel()
        transition = CouiFloatAnimator(
            from = position.value,
            to = target,
            durationMs = durationMs,
            interpolate = ::accelerateDecelerate,
            onUpdate = { position.snapTo(it) },
            onEnd = { cancelled -> if (!cancelled) onValueChangeFinished() },
        ).also { it.start() }
    }

    private fun startFling(initialVelocity: Float, frameSeconds: Float) {
        flingHandle?.cancel()
        var velocity = initialVelocity
        var x = position.value

        fun scheduleNext() {
            flingHandle = postCouiFrameCallback {
                flingHandle = null
                // Unconstrained single-axis branch of Oplus World.solve rather than AndroidX exponential decay.
                velocity /= 1f + 15f * frameSeconds
                x += velocity * frameSeconds
                val clamped = x.coerceIn(0f, length)
                if (x >= length && !bumpingEnd) startGlitter(velocity)
                position.snapTo(clamped)
                setFraction(normalizePosition(clamped), vibrate = false)
                // Finish inertia at visible bounds; revisit constrained body if overscroll bounce or exact end timing is needed.
                if (x != clamped || abs(velocity) < 0.1f) {
                    onValueChangeFinished()
                } else {
                    scheduleNext()
                }
            }
        }
        scheduleNext()
    }

    private fun startGlitter(velocity: Float) {
        bumpingEnd = true
        glitter.animateTo(0f, 0.6f, velocity = abs(velocity))
    }

    private fun setFraction(next: Float, vibrate: Boolean, truncate: Boolean = false) {
        val raw = next.coerceIn(0f, 1f)
        val selected = if (steps > 0 || section) {
            val i = if (truncate) (raw * intervals).toInt() else index(raw)
            i.toFloat() / intervals
        } else {
            resolveFraction(raw)
        }
        val nextValue = range.start + span * selected
        if (value == nextValue) return
        value = nextValue
        onValueChange(nextValue)
        if (vibrate && hapticEffect != 0) {
            val isEdge = selected == 0f || selected == 1f
            if (hapticEffect == 1) {
                if (isEdge) feedback(306, false)
            } else {
                val isSnappedKeyPoint = keyPointFractions.isNotEmpty() && keyPointFractions.any { abs(it - selected) < 1e-4f }
                if (isEdge) {
                    feedback(306, false)
                } else if (section || isSnappedKeyPoint) {
                    feedback(308, section)
                } else if (dragging) {
                    val last = lastStepHapticMark
                    if (last == null || last.elapsedNow() >= 35.milliseconds) {
                        lastStepHapticMark = TimeSource.Monotonic.markNow()
                        feedback(305, false)
                    }
                }
            }
        }
    }

    private fun resolveFraction(raw: Float): Float {
        if (keyPointFractions.isNotEmpty() && magnetThreshold > 0f) {
            var closest = keyPointFractions[0]
            var bestDist = abs(closest - raw)
            for (i in 1 until keyPointFractions.size) {
                val cand = keyPointFractions[i]
                val dist = abs(cand - raw)
                if (dist < bestDist) {
                    bestDist = dist
                    closest = cand
                }
            }
            if (bestDist < magnetThreshold) {
                return closest
            }
        }
        return raw
    }

    private fun stopPositionAnimations() {
        fastRelease = false
        transition?.cancel()
        transition = null
        dragSnap?.cancel()
        dragSnap = null
        position.cancelComplete()
        flingHandle?.cancel()
        flingHandle = null
    }

    private fun resetAnchor() {
        fastRelease = false
        dragSnap?.cancel()
        dragSnap = null
        anchorSection = -1
        anchorX = 0f
        direction = 0
        fingerOffset = 0f
        dragAnimValue = 0f
        dragAnimStart = 0f
        dragAnimEnd = Float.NaN
        lastFingerX = 0f
    }

    private fun normalize(next: Float): Float = if (span > 0f) ((next - range.start) / span).coerceIn(0f, 1f) else 0f
    private fun normalizePosition(x: Float): Float = if (length > 0f) (x / length).coerceIn(0f, 1f) else fraction
    private fun index(next: Float): Int = (next * intervals).roundToInt().coerceIn(0, intervals)
    private fun quantize(next: Float): Float = if (steps > 0 || section) index(next).toFloat() / intervals else next

    private companion object {
        val DragInterpolator = CubicBezierEasing(0f, 0f, 0.25f, 1f)

        fun accelerateDecelerate(input: Float): Float = (cos((input + 1.0) * PI) / 2.0 + 0.5).toFloat()
    }
}

private class CouiFloatAnimator(
    private val from: Float,
    private val to: Float,
    private val durationMs: Long,
    private val interpolate: (Float) -> Float,
    private val onUpdate: (Float) -> Unit,
    private val onEnd: (cancelled: Boolean) -> Unit,
) {
    var isRunning: Boolean = false
        private set
    private var handle: CouiFrameHandle? = null
    private var startMark: TimeMark = TimeSource.Monotonic.markNow()

    fun start() {
        isRunning = true
        startMark = TimeSource.Monotonic.markNow()
        scheduleNext()
    }

    fun cancel() {
        if (!isRunning) return
        isRunning = false
        handle?.cancel()
        handle = null
        onEnd(true)
    }

    private fun scheduleNext() {
        handle = postCouiFrameCallback {
            handle = null
            if (!isRunning) return@postCouiFrameCallback
            val elapsedMs = startMark.elapsedNow().inWholeNanoseconds / 1_000_000.0
            val rawFraction = if (durationMs <= 0L) 1f else (elapsedMs / durationMs).toFloat().coerceIn(0f, 1f)
            onUpdate(from + (to - from) * interpolate(rawFraction))
            if (!isRunning) return@postCouiFrameCallback
            if (rawFraction >= 1f) {
                isRunning = false
                onEnd(false)
            } else {
                scheduleNext()
            }
        }
    }
}
