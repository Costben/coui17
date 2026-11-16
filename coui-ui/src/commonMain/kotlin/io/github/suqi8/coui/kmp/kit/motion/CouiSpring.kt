// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Standard COUI spring channel; retargeting while running preserves velocity,
 * whereas [cancelComplete] clears any pending target and velocity.
 */
internal class CouiSpring(
    initial: Float,
    private val minimumChange: Float = 1f,
    private val onUpdate: () -> Unit = {},
) {
    private val position = mutableFloatStateOf(initial)
    private var currentValue: Float = initial
    private var target: Float = initial
    private var pendingTarget: Float = Float.NaN
    private var onFrame: ((Float) -> Unit)? = null
    private var onEnd: (() -> Unit)? = null
    private var minValue = -Float.MAX_VALUE
    private var maxValue = Float.MAX_VALUE
    private var activeMinimumChange: Float = minimumChange
    private var frameHandle: CouiFrameHandle? = null
    private var lastFrameTimeNanos: Long = -1L
    private var frameToken: Long = 0L

    var dampingRatio: Float = 1f
        private set
    var stiffness: Float = ((2.0 * PI / DEFAULT_RESPONSE) * (2.0 * PI / DEFAULT_RESPONSE)).toFloat()
        private set

    val state: State<Float> get() = position
    val value: Float get() = position.floatValue
    var isRunning: Boolean = false
        private set
    var velocity: Float = 0f
        private set

    /** Passes optional bounds to the integrator so rebound timing and velocity stay intact. */
    fun setBounds(minimum: Float, maximum: Float) {
        require(minimum.isFinite() && maximum.isFinite() && minimum <= maximum)
        minValue = minimum
        maxValue = maximum
    }

    /** Updates the current position without cancelling velocity or any running animation. */
    fun setStartValue(start: Float) {
        currentValue = start
        position.floatValue = start
    }

    fun animateTo(
        target: Float,
        response: Float,
        bounce: Float = 0f,
        minimumChange: Float = this.minimumChange,
        velocity: Float? = null,
        onFrame: ((Float) -> Unit)? = null,
        onEnd: (() -> Unit)? = null,
    ) {
        frameToken++
        this.onFrame = onFrame
        this.onEnd = onEnd
        dampingRatio = (1f - bounce).coerceAtLeast(0f)
        stiffness = ((2.0 * PI / response) * (2.0 * PI / response)).toFloat()
        activeMinimumChange = minimumChange
        if (velocity != null) {
            this.velocity = velocity
        }
        if (isRunning) {
            pendingTarget = target
        } else {
            val start = currentValue.coerceIn(minValue, maxValue)
            currentValue = start
            position.floatValue = start
            this.target = target
            pendingTarget = Float.NaN
            isRunning = true
            lastFrameTimeNanos = -1L
            scheduleNextFrame()
        }
    }

    fun snapTo(next: Float) {
        cancelComplete()
        currentValue = next
        target = next
        position.floatValue = next
    }

    fun cancel() {
        frameToken++
        onFrame = null
        onEnd = null
        isRunning = false
        frameHandle?.cancel()
        frameHandle = null
    }

    fun cancelComplete() {
        cancel()
        pendingTarget = Float.NaN
        target = currentValue
        velocity = 0f
    }

    /** Advances the spring by [deltaSeconds] and returns true if it came to rest. */
    internal fun step(deltaSeconds: Double): Boolean {
        if (!isRunning) return false
        var pos = currentValue.toDouble()
        var speed = velocity.toDouble()
        if (!pendingTarget.isNaN()) {
            val half = deltaSeconds / 2.0
            integrate(pos, speed, half) { nextPos, nextVel ->
                pos = nextPos
                speed = nextVel
            }
            target = pendingTarget
            pendingTarget = Float.NaN
            integrate(pos, speed, half) { nextPos, nextVel ->
                pos = nextPos
                speed = nextVel
            }
        } else {
            integrate(pos, speed, deltaSeconds) { nextPos, nextVel ->
                pos = nextPos
                speed = nextVel
            }
        }
        val clamped = pos.toFloat().coerceIn(minValue, maxValue)
        currentValue = clamped
        position.floatValue = clamped
        velocity = speed.toFloat()

        val valueThreshold = activeMinimumChange * THRESHOLD_MULTIPLIER
        val velocityThreshold = valueThreshold * VELOCITY_THRESHOLD_MULTIPLIER
        val atRest = abs(velocity.toDouble()) < velocityThreshold && abs((clamped - target).toDouble()) < valueThreshold
        if (atRest) {
            val finalValue = target.coerceIn(minValue, maxValue)
            currentValue = finalValue
            position.floatValue = finalValue
            velocity = 0f
            isRunning = false
        }
        return atRest
    }

    private fun onAnimationFrame(frameTimeNanos: Long) {
        frameHandle = null
        if (!isRunning) return
        val dt = if (lastFrameTimeNanos < 0L) {
            DEFAULT_FIRST_FRAME_SECONDS
        } else {
            ((frameTimeNanos - lastFrameTimeNanos) / NANOS_PER_SECOND).coerceIn(MIN_STEP_SECONDS, MAX_STEP_SECONDS)
        }
        lastFrameTimeNanos = frameTimeNanos
        val settled = step(dt)
        val token = ++frameToken
        onFrame?.invoke(currentValue)
        onUpdate()
        if (token != frameToken) {
            if (isRunning) scheduleNextFrame()
            return
        }
        if (settled) {
            val endCallback = onEnd
            onFrame = null
            onEnd = null
            endCallback?.invoke()
        } else if (isRunning) {
            scheduleNextFrame()
        }
    }

    private fun scheduleNextFrame() {
        if (frameHandle != null || !isRunning) return
        frameHandle = postCouiFrameCallback(::onAnimationFrame)
    }

    private inline fun integrate(
        position: Double,
        speed: Double,
        seconds: Double,
        result: (Double, Double) -> Unit,
    ) {
        val targetDouble = target.toDouble()
        val displacement = position - targetDouble
        val zeta = dampingRatio.toDouble()
        val omega = sqrt(stiffness.toDouble()).coerceAtLeast(1e-6)
        when {
            zeta < 1.0 - CRITICAL_DAMPING_TOLERANCE -> {
                val damped = omega * sqrt(1.0 - zeta * zeta)
                val a = (zeta * omega * displacement + speed) / damped
                val decay = exp(-zeta * omega * seconds)
                val angle = damped * seconds
                val cosine = cos(angle)
                val sine = sin(angle)
                val oscillation = decay * (cosine * displacement + sine * a)
                val nextSpeed = -omega * oscillation * zeta +
                    decay * (-damped * displacement * sine + a * damped * cosine)
                result(oscillation + targetDouble, nextSpeed)
            }

            zeta > 1.0 + CRITICAL_DAMPING_TOLERANCE -> {
                val root = omega * sqrt(zeta * zeta - 1.0)
                val slow = -zeta * omega + root
                val fast = -zeta * omega - root
                val slowWeight = (speed - fast * displacement) / (slow - fast)
                val fastWeight = displacement - slowWeight
                val slowDecay = exp(slow * seconds)
                val fastDecay = exp(fast * seconds)
                val nextDisp = slowWeight * slowDecay + fastWeight * fastDecay
                val nextSpeed = slowWeight * slow * slowDecay + fastWeight * fast * fastDecay
                result(nextDisp + targetDouble, nextSpeed)
            }

            else -> {
                val a = speed + omega * displacement
                val b = displacement + a * seconds
                val decay = exp(-omega * seconds)
                result(b * decay + targetDouble, a * decay - omega * b * decay)
            }
        }
    }

    private companion object {
        const val DEFAULT_RESPONSE = 0.5
        const val THRESHOLD_MULTIPLIER = 0.75
        const val VELOCITY_THRESHOLD_MULTIPLIER = 62.5
        const val CRITICAL_DAMPING_TOLERANCE = 1e-3
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val DEFAULT_FIRST_FRAME_SECONDS = 1.0 / 60.0
        const val MIN_STEP_SECONDS = 0.0001
        const val MAX_STEP_SECONDS = 0.05
    }
}
