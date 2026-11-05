// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * One rebound parameter set of `SpringOverScroller`. Its motion is the linear spring
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

    /** `friction / (2 * angularFrequency)`. */
    val dampingRatio: Double = friction / (2.0 * angularFrequency)
}

/**
 * `SpringOverScroller.ReboundOverScroller.SPRING_BACK_STOP_THRESHOLD`: how close to the target, in
 * px, the finger-up rebound has to get before it snaps home.
 */
private const val SPRING_BACK_STOP_THRESHOLD = 0.25

/**
 * `COUIPhysicalAnimationUtil.calcRealOverScrollDist`: the share of the finger travel that reaches
 * the content while it is already at its edge.
 */
private const val OVER_SCROLL_DRAG_DAMPING = 0.4

/**
 * Upper bound on `touchDistance / range` before the exponential saturates. At 50 the curve is
 * within 2e-9 of `range`, so the clamp is only there to keep the value finite.
 */
private const val MAX_OVER_SCROLL_RATIO = 50.0

/** Upper bound on `|offset| / range` before the inverse logarithm goes to infinity. */
private const val MAX_OVER_SCROLL_OFFSET_RATIO = 0.9999

/** Frame timestamps are nanoseconds; the spring integrates in seconds. */
private const val NANOS_PER_SECOND = 1_000_000_000f

internal object SpringMath {
    const val MAX_FRAME_DELTA_SECONDS = 0.016f
    const val MIN_FRAME_DELTA_SECONDS = 0.001f

    /**
     * Longest catch-up the first step of a settle may cover. The finger-up callback is dispatched
     * through the UI dispatcher, so it can reach the settle a frame — or two — after the gesture
     * actually ended; capping that first step at one frame would leave the whole rebound that much
     * behind the reference, which springs from the touch-up itself. The closed form is exact for
     * any step, so the only job of this bound is to keep a stall (a resumed process, a blocked
     * main thread) from teleporting the content.
     */
    const val MAX_FIRST_STEP_SECONDS = 0.1f

    /**
     * The rebound after the finger lets go: `SpringOverScroller.ReboundOverScroller.springBack`
     * (`com.coui.appcompat.scroll.SpringOverScroller`, ColorOS 17 — the same class the Settings APK
     * vendors, `apks/dex/Settings/classes.dex`).
     *
     * `springBack` swaps in `mSpringBackConfig`, and `COUIRecyclerView.setLayoutManager` re-tensions
     * it for the scroll direction. For a vertical list the Origami pair it configures is
     * `(12.19, 2.15 * 16 = 34.4)`, i.e. `friction = (12.19 - 8) * 3 + 25 = 37.57` and
     * `tension = (34.4 - 30) * 3.62 + 194 = 209.93` (1/s²) — omega 14.49, zeta 1.30 on paper.
     *
     * That is not the motion the list runs. `calculateWithRk4` takes the position of its first
     * derivative from `mTempState.mPosition` — the *previous* step's predicted position — instead of
     * the current one, so the restoring force always lags a frame and the spring comes out softer
     * and lighter than its config: an effective omega of 13.9 at 60Hz to 14.2 at 120Hz, with zeta
     * 1.05..1.07. The reference device agrees with the integrator, not the config: the Settings list
     * measures omega 14.0 / zeta 1.05 over three runs (0.8px residual), which is the pair below.
     *
     * It still stops on displacement alone once it is within `SPRING_BACK_STOP_THRESHOLD` 0.25px,
     * and it ignores the finger-up velocity — see [COUIOverscrollEffect.applyToFling].
     */
    val SPRING_BACK = SpringParams(
        tension = 195.9,
        friction = 29.5,
        restDisplacement = SPRING_BACK_STOP_THRESHOLD,
        restSpeed = 0.0,
    )

    /**
     * The same rebound for a horizontally scrolling list, i.e. the other branch of
     * `COUIRecyclerView.setLayoutManager` (`androidx/recyclerview/widget/COUIRecyclerView.java:1716`
     * in the battery APK; `HORIZONTAL_SPRING_BACK_TENSION_MULTIPLE` is `:40`, and
     * `COUIHorizontalScrollView` re-declares the same 3.2 at `com/coui/appcompat/scrollview/COUIHorizontalScrollView.java:36`).
     * A horizontal list gets `3.2 * 16 = 51.2` Origami tension instead of 2.15 * 16, i.e.
     * `tension = (51.2 - 30) * 3.62 + 194 = 270.74` on paper — omega 16.45, zeta 1.14.
     *
     * Through the same `calculateWithRk4` lag that softens [SPRING_BACK] that becomes an effective
     * omega of 15.76 at 60Hz to 16.09 at 120Hz with zeta 0.93..0.94, so the pair below is the
     * midpoint of those three frame rates. Unlike the vertical one there is no device measurement
     * behind it — it is the integrator's own effective dynamics, nothing fitted.
     */
    val SPRING_BACK_HORIZONTAL = SpringParams(
        tension = 254.2,
        friction = 29.9,
        restDisplacement = SPRING_BACK_STOP_THRESHOLD,
        restSpeed = 0.0,
    )

    /**
     * The deceleration of a fling that runs into the boundary:
     * `COUISpringInterpolator(DEFAULT_STIFFNESS 40.0, DEFAULT_DAMPINGRATIO 1.15, …)`.
     *
     * An angular frequency of `sqrt(40) = 6.3246` rad/s (a 0.9935s natural period) with a damping
     * ratio of 1.15. The fling ends when it loses the velocity `getSplineMinVelocity()` asks for
     * (`REST_SPEED_THRESHOLD` 5px/s), or when it is within `mDisplacementFromRestThreshold` 0.05px.
     *
     * That end condition is why overscroll no longer uses this curve. The settle is bounded by the
     * velocity, so its length follows the speed the fling still had when it hit the edge: about
     * 0.15s at 300px/s, 0.9s at 6 000px/s, and close to 2s at 20 000px/s. A list thrown from one end
     * to the other arrives with the large values, so the same gesture bounced back quickly or took
     * seconds depending on how hard it was thrown. [SPRING_BACK] stops on displacement instead and
     * is back in about 1.1s from any velocity a fling can leave, which is why both overscroll entry
     * points now use it; this pair is kept as [SpringEngine]'s default and for the desktop tests.
     */
    val FLING_REBOUND = SpringParams(
        tension = 40.0,
        friction = 2.0 * 1.15 * sqrt(40.0),
        restDisplacement = 0.05,
        restSpeed = 5.0,
    )

    /**
     * The overscroll curve of `COUIGridSpringChainScrollView`: `x - x^2 + x^3/3` over the normalized
     * drag, so the content stops dead at `range / 3`.
     *
     * Only the grid spring chain still uses this; list overscroll moved to
     * [obtainOverScrollOffset], which is the curve ColorOS 17 actually runs.
     */
    fun obtainDampingDistance(normalizedInput: Float, range: Float): Float {
        val x = max(0.0f, min(normalizedInput, 1.0f)).toDouble()
        val dampedFactor = x - x.pow(2.0) + (x.pow(3.0) / 3.0)
        return (dampedFactor * range).toFloat()
    }

    /**
     * The overscroll a drag of [touchDistance] px holds:
     * `COUIPhysicalAnimationUtil.calcRealOverScrollDist`
     * (`com.coui.appcompat.animation.COUIPhysicalAnimationUtil`, ColorOS 17).
     *
     * `COUIRecyclerView.scrollByInternal` runs every drag delta the list could not consume through
     * that method before `overScrollBy`, and it multiplies the delta by `0.4`, scaled down linearly
     * by how much of `mOverscrollDistance` — the display height in px, set by `initProperty` — the
     * overscroll already uses:
     *
     *     step = delta * 0.4 * (1 - |offset| / range)
     *
     * Integrating that over the whole drag gives the curve below: 40% of the finger travel at first,
     * flattening out towards [range]. The cubic [obtainDampingDistance] instead starts at the full
     * finger travel and stops at `range / 3`, which is why a 600px drag on a 2772px-tall screen
     * overscrolls about 480px where the system overscrolls about 230px.
     *
     * The reference also truncates every step to whole pixels (`(int)` in `calcRealOverScrollDist`),
     * which over a 600px drag costs it about 10px, and its list starts moving a few pixels later
     * than ours. That is the whole of the 5% peak difference the reference device shows — 206px
     * there against 217px here for the same gesture — and it is quantisation, not the curve, so it
     * is deliberately not reproduced.
     */
    fun obtainOverScrollOffset(touchDistance: Float, range: Float): Float {
        if (range <= 0f) return 0f
        val magnitude = abs(touchDistance.toDouble() / range).coerceAtMost(MAX_OVER_SCROLL_RATIO)
        return (sign(touchDistance) * range * (1.0 - exp(-OVER_SCROLL_DRAG_DAMPING * magnitude))).toFloat()
    }

    /** Inverse of [obtainOverScrollOffset]: the finger travel that holds [offset]. */
    fun obtainOverScrollTouchDistance(offset: Float, range: Float): Float {
        if (range <= 0f) return 0f
        val normalized = (abs(offset).toDouble() / range).coerceIn(0.0, MAX_OVER_SCROLL_OFFSET_RATIO)
        return (sign(offset) * -range / OVER_SCROLL_DRAG_DAMPING * ln(1.0 - normalized)).toFloat()
    }
}

internal class SpringEngine {
    private var params: SpringParams = SpringMath.FLING_REBOUND
    var velocity: Double = 0.0
    var currentPos: Double = 0.0
    private var targetPos: Double = 0.0
    private var started = false

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
     * @param params Which rebound curve drives this settle. Overscroll passes
     *   [SpringMath.SPRING_BACK] from both entry points; the default is the fling curve, which only
     *   the desktop tests exercise.
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
        started = true
    }

    /**
     * Advances the spring by [deltaTime] seconds along the closed form of
     * `x'' = tension * (target - x) - friction * x'` — the same motion
     * `ReboundOverScroller.calculateWithRk4` integrates.
     *
     * The previous Euler step shed `2 * zeta * omega * dt` of the velocity per frame, which at
     * [SpringMath.SPRING_BACK]'s damping (`2 * 1.30 * 14.49 * 0.016 = 0.60`) is a 60% velocity loss
     * per frame and stretches the settle by about 10%. That is the same order as the difference the
     * curve was being tuned against, so the exact solution replaces it and the frame rate no longer
     * changes the trajectory.
     *
     * @param maxDeltaSeconds Upper bound on [deltaTime]. Every frame after the first one is capped
     *   at a single display frame; the first one passes [SpringMath.MAX_FIRST_STEP_SECONDS] so a
     *   settle that starts late can still count the time it lost.
     */
    fun step(deltaTime: Float, maxDeltaSeconds: Float = SpringMath.MAX_FRAME_DELTA_SECONDS): Boolean {
        if (!started) return false
        val dt = deltaTime.coerceIn(SpringMath.MIN_FRAME_DELTA_SECONDS, maxDeltaSeconds).toDouble()
        val offset = currentPos - targetPos
        val speed = velocity
        val omega = params.angularFrequency
        val zeta = params.dampingRatio

        val nextOffset: Double
        val nextVelocity: Double
        when {
            zeta < 1.0 - CRITICAL_DAMPING_TOLERANCE -> {
                val damped = omega * sqrt(1.0 - zeta * zeta)
                val decay = exp(-zeta * omega * dt)
                val angle = damped * dt
                val cosine = cos(angle)
                val sine = sin(angle)
                nextOffset = decay * (offset * cosine + (speed + zeta * omega * offset) / damped * sine)
                nextVelocity = decay * (speed * cosine - (omega * omega * offset + zeta * omega * speed) / damped * sine)
            }

            zeta > 1.0 + CRITICAL_DAMPING_TOLERANCE -> {
                val root = omega * sqrt(zeta * zeta - 1.0)
                val slow = -zeta * omega + root
                val fast = -zeta * omega - root
                val slowWeight = (speed - fast * offset) / (slow - fast)
                val fastWeight = offset - slowWeight
                val slowDecay = exp(slow * dt)
                val fastDecay = exp(fast * dt)
                nextOffset = slowWeight * slowDecay + fastWeight * fastDecay
                nextVelocity = slowWeight * slow * slowDecay + fastWeight * fast * fastDecay
            }

            else -> {
                val decay = exp(-omega * dt)
                nextOffset = (offset + (speed + omega * offset) * dt) * decay
                nextVelocity = (speed - omega * (speed + omega * offset) * dt) * decay
            }
        }

        currentPos = targetPos + nextOffset
        velocity = nextVelocity

        if (isAtEquilibrium()) {
            currentPos = targetPos
            velocity = 0.0
            return true
        }

        return false
    }
}

/** How close to a damping ratio of 1 counts as critical, where the two roots merge. */
private const val CRITICAL_DAMPING_TOLERANCE = 1e-3

/**
 * Drive this spring from [startValue] to [targetValue] with [initialVelocity], calling [onFrame] each
 * frame until it settles (or is cancelled), then [onSettle]. Shared by both overscroll implementations.
 *
 * @param params The rebound curve to settle on; see [SpringEngine.start].
 * @param startedAt When the gesture ended. The first frame's delta is measured from here — never
 *   shorter than one frame, because the fling callback arrives inside the same frame as the finger's
 *   last movement — so a release lands on the right position instead of stalling for a frame.
 * @param nextFrameNanos The frame clock. A test replaces it with a scripted one; production keeps
 *   [withFrameNanos].
 */
internal suspend fun SpringEngine.runSettleAnimation(
    startValue: Float,
    targetValue: Float = 0f,
    initialVelocity: Float,
    params: SpringParams = SpringMath.FLING_REBOUND,
    startedAt: TimeMark = TimeSource.Monotonic.markNow(),
    nextFrameNanos: suspend () -> Long = { withFrameNanos { it } },
    onFrame: (currentPos: Float) -> Unit,
    onSettle: () -> Unit,
) {
    start(startValue = startValue, targetValue = targetValue, initialVel = initialVelocity, params = params)
    var lastFrameTimeNanos = -1L
    var isFinished = false
    try {
        while (!isFinished && currentCoroutineContext().isActive) {
            val frameTimeNanos = nextFrameNanos()
            val isFirstFrame = lastFrameTimeNanos == -1L
            val dt = if (isFirstFrame) {
                // The fling callback arrives inside the same frame as the finger's last movement, so
                // the raw elapsed time here is a few milliseconds and would leave the content
                // standing still for a frame. The first step always advances at least one display
                // frame, and as much more as the callback was actually late.
                maxOf(startedAt.elapsedNow().inWholeNanoseconds / NANOS_PER_SECOND, SpringMath.MAX_FRAME_DELTA_SECONDS)
            } else {
                (frameTimeNanos - lastFrameTimeNanos) / NANOS_PER_SECOND
            }
            lastFrameTimeNanos = frameTimeNanos
            isFinished = step(
                dt,
                maxDeltaSeconds = if (isFirstFrame) SpringMath.MAX_FIRST_STEP_SECONDS else SpringMath.MAX_FRAME_DELTA_SECONDS,
            )
            onFrame(currentPos.toFloat())
        }
    } finally {
        onSettle()
    }
}
