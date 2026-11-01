// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * One rebound parameter set of `SpringOverScroller`, whose RK4 integration solves
 *
 * `x'' = tension * (target - x) - friction * x'`
 *
 * (the Origami form; `ReboundConfig` converts the two configured values through
 * `tensionFromOrigamiValue` and `frictionFromOrigamiValue`).
 *
 * @param tension The spring constant, 1/s². [angularFrequency] is its square root.
 * @param friction The velocity damping, 1/s. [dampingRatio] is `friction / (2 * angularFrequency)`.
 * @param restDisplacement How close to the target, in px, counts as back in place.
 * @param restSpeed The speed, in px/s, under which the settle ends as a lost velocity
 *   (`lostVelocity`); `0` drops that condition and leaves [restDisplacement] alone.
 */
internal class SpringParams(
    val tension: Double,
    val friction: Double,
    val restDisplacement: Double,
    val restSpeed: Double,
) {
    /** `sqrt(tension)`, rad/s. */
    val angularFrequency: Double = sqrt(tension)

    /** `2 * PI / angularFrequency`, seconds. */
    val naturalPeriod: Double = 2.0 * PI / angularFrequency

    /** `friction / (2 * angularFrequency)`. */
    val dampingRatio: Double = friction / (2.0 * angularFrequency)
}

internal object SpringMath {
    const val MAX_FRAME_DELTA_SECONDS = 0.016f
    const val MIN_FRAME_DELTA_SECONDS = 0.001f

    /**
     * The rebound after the finger lets go, i.e. `ReboundOverScroller.springBack`.
     *
     * The scroller starts from `ReboundConfig(12.19, 16.0)`, but every vertical host
     * (`COUINestedScrollView`, `COUIScrollView`) then calls `setSpringBackTensionMultiple(2.15)`, so
     * the pair that actually runs is `friction = (12.19 - 8) * 3 + 25 = 37.57` and
     * `tension = (2.15 * 16 - 30) * 3.62 + 194 = 209.93`. That is an angular frequency of 14.49
     * rad/s (a 0.434s natural period) with a damping ratio of 1.30, and it stops on displacement
     * alone: `SPRING_BACK_STOP_THRESHOLD` 0.25px.
     */
    val SPRING_BACK = SpringParams(tension = 209.93, friction = 37.57, restDisplacement = 0.25, restSpeed = 0.0)

    /**
     * The deceleration of a fling that runs into the boundary:
     * `COUISpringInterpolator(DEFAULT_STIFFNESS 40.0, DEFAULT_DAMPINGRATIO 1.15, …)`.
     *
     * An angular frequency of `sqrt(40) = 6.3246` rad/s (a 0.9935s natural period) with a damping
     * ratio of 1.15. The fling ends when it loses the velocity `getSplineMinVelocity()` asks for
     * (`REST_SPEED_THRESHOLD` 5px/s), or when it is within `mDisplacementFromRestThreshold` 0.05px.
     */
    val FLING_REBOUND = SpringParams(
        tension = 40.0,
        friction = 2.0 * 1.15 * sqrt(40.0),
        restDisplacement = 0.05,
        restSpeed = 5.0,
    )

    /**
     * Obtain the movable distance after converting damping
     *
     * Damping formula: x - x^2 + x^3/3
     * */
    fun obtainDampingDistance(normalizedInput: Float, range: Float): Float {
        val x = max(0.0f, min(normalizedInput, 1.0f)).toDouble()
        val dampedFactor = x - x.pow(2.0) + (x.pow(3.0) / 3.0)
        return (dampedFactor * range).toFloat()
    }

    /**
     * Retrieve the damping distance and restore it to the original movement distance
     *
     * Restore formula: range - (range^(2/3)) * (range - 3 * absPixelOffset)^(1/3)
     * */
    fun obtainTouchDistance(currentPixelOffset: Float, range: Float): Float {
        var absPixelOffset = abs(currentPixelOffset)
        val absMaxOffset = abs(obtainDampingDistance(1.0f, range))

        if (absPixelOffset <= 0f) return 0f
        if (absPixelOffset >= absMaxOffset) {
            absPixelOffset = absMaxOffset
        }

        val base = range - (3.0 * absPixelOffset)
        val part2 = range.toDouble().pow(2.0 / 3.0) * sign(base) * abs(base).pow(1.0 / 3.0)
        return (range - part2).toFloat()
    }
}

private class SpringOperator(dampingRatio: Float, naturalPeriod: Float) {
    private val dampingCoefficient: Double
    private val stiffnessOverMass: Double

    init {
        val angularFrequency = (2.0 * PI) / naturalPeriod
        stiffnessOverMass = angularFrequency * angularFrequency // k/m = ω^2
        dampingCoefficient = 2.0 * dampingRatio * angularFrequency // c/m = 2 * ζ * ω
    }

    /**
     * Calculate the new velocity using Euler's formula
     * */
    fun updateVelocity(
        currentVelocity: Double,
        deltaTime: Float,
        currentPosition: Double,
        targetPosition: Double,
    ): Double {
        val velocityDecayFactor = 1.0 - dampingCoefficient * deltaTime
        val velocityIncreaseFromSpring = stiffnessOverMass * (targetPosition - currentPosition) * deltaTime
        return currentVelocity * velocityDecayFactor + velocityIncreaseFromSpring
    }
}

internal class SpringEngine {
    private var springOperator: SpringOperator? = null
    private var params: SpringParams = SpringMath.FLING_REBOUND
    var velocity: Double = 0.0
    var currentPos: Double = 0.0
    private var targetPos: Double = 0.0

    /**
     * The two end conditions of the parameter set, either of which finishes the settle:
     * `lostVelocity()` for a fling (`|v|` under [SpringParams.restSpeed]), or the spring coming
     * within [SpringParams.restDisplacement] of the target (`SPRING_BACK_STOP_THRESHOLD` for a
     * spring back). A fast pass through the target is therefore never mistaken for the end.
     */
    private fun isAtEquilibrium(): Boolean {
        if (params.restSpeed > 0.0 && abs(velocity) < params.restSpeed) return true
        return abs(currentPos - targetPos) < params.restDisplacement
    }

    /**
     * @param params Which of `SpringOverScroller`'s two rebound curves drives this settle:
     *   [SpringMath.SPRING_BACK] after the finger lets go, [SpringMath.FLING_REBOUND] when a fling
     *   runs into the boundary.
     */
    fun start(
        startValue: Float,
        targetValue: Float,
        initialVel: Float,
        params: SpringParams = SpringMath.FLING_REBOUND,
    ) {
        currentPos = startValue.toDouble()
        targetPos = targetValue.toDouble()
        velocity = initialVel.toDouble()
        this.params = params

        springOperator = SpringOperator(params.dampingRatio.toFloat(), params.naturalPeriod.toFloat())
    }

    fun step(deltaTime: Float): Boolean {
        val operator = springOperator ?: return false
        val dt = deltaTime.coerceIn(SpringMath.MIN_FRAME_DELTA_SECONDS, SpringMath.MAX_FRAME_DELTA_SECONDS)
        velocity = operator.updateVelocity(velocity, dt, currentPos, targetPos)
        currentPos += dt * velocity

        if (isAtEquilibrium()) {
            currentPos = targetPos
            velocity = 0.0
            return true
        }

        return false
    }
}

/**
 * Drive this spring from [startValue] to [targetValue] with [initialVelocity], calling [onFrame] each
 * frame until it settles (or is cancelled), then [onSettle]. Shared by both overscroll implementations.
 *
 * @param params The rebound curve to settle on; see [SpringEngine.start].
 */
internal suspend fun SpringEngine.runSettleAnimation(
    startValue: Float,
    targetValue: Float = 0f,
    initialVelocity: Float,
    params: SpringParams = SpringMath.FLING_REBOUND,
    onFrame: (currentPos: Float) -> Unit,
    onSettle: () -> Unit,
) {
    start(startValue = startValue, targetValue = targetValue, initialVel = initialVelocity, params = params)
    var lastFrameTimeNanos = -1L
    var isFinished = false
    try {
        while (!isFinished && currentCoroutineContext().isActive) {
            isFinished = withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos == -1L) {
                    lastFrameTimeNanos = frameTimeNanos
                    return@withFrameNanos false
                }
                val dt = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                lastFrameTimeNanos = frameTimeNanos
                val finished = step(dt)
                onFrame(currentPos.toFloat())
                finished
            }
        }
    } finally {
        onSettle()
    }
}
