// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/** `DynamicAnimation`'s visibility threshold for a pixel value holder, 1px * 0.75. */
private const val SPRING_VALUE_THRESHOLD = 0.75

/** `SpringForce.setValueThreshold`: the velocity threshold is the value threshold * 62.5. */
private const val SPRING_VELOCITY_THRESHOLD = SPRING_VALUE_THRESHOLD * 62.5

/** Effectively unbounded, `COUISegmentButtonLayout`'s `2.14748365E9f`. */
private const val SPRING_UNBOUNDED = 2.14748365E9f

/**
 * A port of `COUISpringAnimation` over `COUISpringForce`, driven one frame at a time.
 *
 * Compose's `Animatable` ends the animation when it reaches a bound. This spring clamps the value and
 * keeps integrating the velocity, so it rests against the bound until the force turns it around. The
 * segment indicator's squash depends on that rest.
 */
internal class CouiSpring(
    initialValue: Float,
    bounce: Float,
    response: Float,
) {
    var value: Float = initialValue
        private set
    var velocity: Float = 0f
    var isRunning: Boolean = false
        private set
    var minValue: Float = -SPRING_UNBOUNDED
    var maxValue: Float = SPRING_UNBOUNDED

    private var target: Float = initialValue
    private var pendingTarget: Float = Float.NaN
    private var dampingRatio: Double = 0.0
    private var naturalFrequency: Double = 0.0

    init {
        configure(bounce, response)
    }

    /** `COUISpringForce.setBounce` and `setResponse`. */
    fun configure(bounce: Float, response: Float) {
        dampingRatio = (1f - bounce).toDouble()
        naturalFrequency = 2.0 * PI / response
    }

    fun setStart(start: Float) {
        value = start
    }

    /** `COUISpringAnimation.animateToFinalPosition`: a running spring retargets on its next frame. */
    fun animateTo(finalPosition: Float) {
        if (isRunning) {
            pendingTarget = finalPosition
        } else {
            target = finalPosition
            isRunning = true
        }
    }

    fun cancel() {
        isRunning = false
        pendingTarget = Float.NaN
    }

    fun snapTo(newValue: Float) {
        cancel()
        value = newValue
        target = newValue
        velocity = 0f
    }

    /** `COUISpringAnimation.updateValueAndVelocity`. Returns whether the spring came to rest. */
    fun step(deltaSeconds: Double): Boolean {
        if (!isRunning) return false
        var position = value.toDouble()
        var speed = velocity.toDouble()
        if (!pendingTarget.isNaN()) {
            val half = deltaSeconds / 2.0
            integrate(position, speed, half) { p, v ->
                position = p
                speed = v
            }
            target = pendingTarget
            pendingTarget = Float.NaN
            integrate(position, speed, half) { p, v ->
                position = p
                speed = v
            }
        } else {
            integrate(position, speed, deltaSeconds) { p, v ->
                position = p
                speed = v
            }
        }
        value = min(max(position.toFloat(), minValue), maxValue)
        velocity = speed.toFloat()
        val atRest = abs(velocity.toDouble()) < SPRING_VELOCITY_THRESHOLD && abs((value - target).toDouble()) < SPRING_VALUE_THRESHOLD
        if (atRest) {
            value = target
            velocity = 0f
            isRunning = false
        }
        return atRest
    }

    /** The closed-form step of `COUISpringForce.updateValues`. */
    private inline fun integrate(position: Double, speed: Double, seconds: Double, result: (Double, Double) -> Unit) {
        val displacement = position - target
        val zeta = dampingRatio
        val omega = naturalFrequency
        if (zeta == 1.0) {
            val a = speed + omega * displacement
            val b = displacement + a * seconds
            val decay = exp(-omega * seconds)
            result(b * decay + target, a * decay - omega * b * decay)
        } else {
            val damped = omega * sqrt(1.0 - zeta * zeta)
            val a = (zeta * omega * displacement + speed) / damped
            val decay = exp(-zeta * omega * seconds)
            val oscillation = decay * (cos(damped * seconds) * displacement + sin(damped * seconds) * a)
            val nextSpeed = -omega * oscillation * zeta +
                decay * (-damped * displacement * sin(damped * seconds) + a * damped * cos(damped * seconds))
            result(oscillation + target, nextSpeed)
        }
    }
}

/** `COUISegmentButtonLayout`'s damping-curve rubber band: approaches [limit] as [distance] grows. */
internal fun segmentRubberBand(distance: Float, limit: Float): Float {
    if (limit == 0f || distance == 0f) return 0f
    val reach = abs(distance) * SEGMENT_DRAG_FACTOR / limit
    return (1f - 1f / (reach + 1f)) * limit * sign(distance)
}

/** `COUISegmentButtonLayout.calculateDynamicValue`: eases from [start] to [end] with the width change. */
private fun widthDrivenValue(from: Float, to: Float, start: Float, end: Float, power: Float): Float {
    if (from <= 0f || to <= 0f) return start
    return (start - (start - end) * (abs(from - to) / max(from, to)).pow(power)).coerceIn(0f, 1f)
}

/** The per-move damping of the drag, `COUISegmentButtonLayout`'s `0.12f`. */
private const val SEGMENT_DRAG_FACTOR = 0.12f

/** The indicator never squashes below this share of its height. */
internal const val SEGMENT_SQUASH_RATIO = 0.9f

/** The width and height springs both kick off with these velocities, in px/s like the native view. */
private const val SEGMENT_WIDTH_KICK = 1000f
private const val SEGMENT_HEIGHT_KICK = -500f

/**
 * The sliding indicator of the segmented control: a port of the four springs of
 * `COUISegmentButtonLayout`, in pixels along the row's content.
 *
 * On a selection change the position spring carries the indicator over while the width spring is
 * kicked outward and the height spring inward, so the capsule flattens and stretches on its way and
 * rounds out again on arrival. Between selections the drag spring lets it lean toward the finger
 * through a rubber band.
 */
@Stable
internal class SegmentIndicatorMotion(
    center: Float,
    width: Float,
    height: Float,
) {
    var centerX: Float by mutableFloatStateOf(center)
        private set
    var width: Float by mutableFloatStateOf(width)
        private set
    var height: Float by mutableFloatStateOf(height)
        private set

    private var active by mutableStateOf(false)

    private val position = CouiSpring(center, bounce = 0.1f, response = 0.4f)
    private val drag = CouiSpring(center, bounce = 0f, response = 0.6f)
    private val widthSpring = CouiSpring(width, bounce = 0.5f, response = 0.6f)
    private val heightSpring = CouiSpring(height, bounce = 0.5f, response = 0.6f)

    private var dragAnchor = 0f
    private var dragRest = center
    private var reanchor = false

    /** Whether the indicator is travelling to a selection, during which it ignores the finger. */
    val isPositionRunning: Boolean get() = position.isRunning

    fun snapTo(center: Float, width: Float, height: Float) {
        position.snapTo(center)
        drag.snapTo(center)
        widthSpring.snapTo(width)
        heightSpring.snapTo(height)
        centerX = center
        this.width = width
        this.height = height
        dragRest = center
        active = false
    }

    /**
     * `COUISegmentButtonLayout.animateIndicatorToPosition`.
     *
     * @param targetCenter The centre of the selected segment.
     * @param targetWidth The width of the selected segment.
     * @param restHeight The indicator's height at rest.
     * @param firstCenter The centre of the first segment, the lower bound of the position spring.
     * @param lastCenter The centre of the last segment, the upper bound of the position spring.
     * @param maxExtraWidth How far a growing indicator may overshoot [targetWidth].
     * @param compact Whether this is the `SegmentButton.Tiny` control, whose growing indicator
     *   settles with less bounce.
     */
    fun select(
        targetCenter: Float,
        targetWidth: Float,
        restHeight: Float,
        firstCenter: Float,
        lastCenter: Float,
        maxExtraWidth: Float,
        compact: Boolean,
    ) {
        drag.cancel()
        dragRest = targetCenter
        val grows = targetWidth > width

        val positionBounce = if (grows && compact) widthDrivenValue(width, targetWidth, 0.1f, 0.03f, 1f) else 0.1f
        position.configure(positionBounce, 0.4f)
        position.minValue = min(firstCenter, lastCenter)
        position.maxValue = max(firstCenter, lastCenter)
        position.setStart(centerX.coerceIn(position.minValue, position.maxValue))
        position.animateTo(targetCenter.coerceIn(position.minValue, position.maxValue))

        val widthBounce = if (grows) 0.5f else widthDrivenValue(width, targetWidth, 0.5f, 0.1f, 2f)
        widthSpring.maxValue = if (grows) targetWidth + maxExtraWidth else SPRING_UNBOUNDED
        widthSpring.configure(widthBounce, widthDrivenValue(width, targetWidth, 0.6f, 0.4f, 2f))
        widthSpring.setStart(width)
        widthSpring.velocity = SEGMENT_WIDTH_KICK
        widthSpring.animateTo(targetWidth)

        val squashed = restHeight * SEGMENT_SQUASH_RATIO
        heightSpring.minValue = squashed
        heightSpring.setStart(max(squashed, height))
        heightSpring.velocity = SEGMENT_HEIGHT_KICK
        heightSpring.animateTo(restHeight)

        active = true
    }

    /** Anchors the rubber band where the finger pressed. */
    fun beginDrag(x: Float) {
        dragAnchor = x
        dragRest = centerX
        reanchor = false
    }

    /** `COUISegmentButtonLayout.handleDrag`: leans toward [x], at most [overDistance] from rest. */
    fun dragTo(x: Float, overDistance: Float) {
        if (reanchor) {
            dragAnchor = x
            reanchor = false
        }
        val distance = x - dragAnchor
        if (distance == 0f) return
        drag.setStart(centerX)
        drag.animateTo(dragRest + segmentRubberBand(distance, overDistance))
        active = true
    }

    /** Springs back to [center] after the finger lets go without a selection change. */
    fun settle(center: Float) {
        if (centerX == center) return
        drag.setStart(centerX)
        drag.animateTo(center)
        active = true
    }

    /** Drives the springs for as long as this is collected. */
    suspend fun animate() {
        snapshotFlow { active }.collectLatest { running ->
            if (!running) return@collectLatest
            var lastFrame = -1L
            while (active) {
                withFrameNanos { frame ->
                    if (lastFrame >= 0L) advance((frame - lastFrame) / 1_000_000_000.0)
                    lastFrame = frame
                }
            }
        }
    }

    /** Moves every running spring forward by [deltaSeconds]. */
    fun advance(deltaSeconds: Double) {
        if (position.isRunning) {
            val finished = position.step(deltaSeconds)
            centerX = position.value
            if (finished) reanchor = true
        }
        if (drag.isRunning) {
            drag.step(deltaSeconds)
            centerX = drag.value
        }
        if (widthSpring.isRunning) {
            widthSpring.step(deltaSeconds)
            width = widthSpring.value
        }
        if (heightSpring.isRunning) {
            heightSpring.step(deltaSeconds)
            height = heightSpring.value
        }
        active = position.isRunning || drag.isRunning || widthSpring.isRunning || heightSpring.isRunning
    }
}
