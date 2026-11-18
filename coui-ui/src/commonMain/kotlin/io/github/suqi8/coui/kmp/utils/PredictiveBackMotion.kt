// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.kit.effect.createBlurRenderEffect
import io.github.suqi8.coui.kmp.kit.effect.isKitRenderEffectSupported
import kotlin.math.abs

internal expect fun predictiveBackUptimeMillis(): Long

/**
 * Default parameters for follow-finger predictive back trajectories, release fling physics, and
 * progressive background blur on the underlying route.
 */
object PredictiveBackMotionDefaults {
    const val DRAG_FOLLOW_FACTOR: Float = 0.65f
    val MaxFlingSpeed: Dp = 1600.dp
    const val MIN_CARD_FLING_EXIT_MILLIS: Int = 260
    const val MIN_IMAGE_FLING_EXIT_MILLIS: Int = 240
    const val FLING_RETURN_STIFFNESS: Float = 700f
    val BackBlurMaxRadius: Dp = 28.dp
    val BackBlurOffsetSpan: Dp = 160.dp
    const val BACK_BLUR_THRESHOLD_PX: Float = 0.5f

    /** Critically damped spring used to return a flung page/card/image smoothly to rest. */
    fun flingReturnSpringSpec(): SpringSpec<Offset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = FLING_RETURN_STIFFNESS,
        visibilityThreshold = Offset(0.5f, 0.5f),
    )

    /** Deceleration tween used when an image without a return thumbnail flings out and fades. */
    fun flingDecaySpec(durationMillis: Int): TweenSpec<Offset> = tween(
        durationMillis = durationMillis.coerceAtLeast(1),
        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    )
}

/**
 * Tracks finger XY displacement and release velocity across a predictive back gesture.
 *
 * Call [record] on every back event and [velocityPxPerSec] when the gesture commits or cancels past
 * the landing threshold. Both returned displacement and velocity are scaled by [followFactor] so
 * the caller's spring animation starts seamlessly from the tracked position and speed.
 */
class PredictiveBackDragTracker internal constructor(
    private val followFactor: Float,
    private val pauseTimeoutMs: Long,
    private val velocityWindowMs: Long,
    private val clockMillis: () -> Long,
) {
    constructor(
        followFactor: Float = PredictiveBackMotionDefaults.DRAG_FOLLOW_FACTOR,
        pauseTimeoutMs: Long = 90L,
        velocityWindowMs: Long = 100L,
    ) : this(followFactor, pauseTimeoutMs, velocityWindowMs, ::predictiveBackUptimeMillis)

    private data class Sample(val timeMs: Long, val pos: Offset)

    private var startTouch: Offset? = null
    private val samples = ArrayDeque<Sample>(8)

    /**
     * Records a pointer sample and returns the scaled displacement from the gesture's origin.
     *
     * Initial `(0, 0)` sentinel coordinates emitted on some devices before touch coordinates arrive
     * are ignored so the anchor never jumps on frame 0.
     */
    fun record(touchX: Float, touchY: Float, frameTimeMillis: Long = 0L): Offset {
        val touch = Offset(touchX, touchY)
        if (touch == Offset.Zero && startTouch == null) return Offset.Zero
        val start = startTouch ?: touch.also { startTouch = it }
        val timeMs = if (frameTimeMillis > 0L) frameTimeMillis else clockMillis()
        if (samples.size >= 8) samples.removeFirst()
        samples.addLast(Sample(timeMs, touch))
        return (touch - start) * followFactor
    }

    /**
     * Estimates the release velocity in px/s, clamped to [maxSpeedPx] and scaled by [followFactor].
     *
     * Returns [Offset.Zero] if the finger paused longer than [pauseTimeoutMs] before release
     * (after verifying that [clockMillis] and event timestamps share the same clock domain).
     */
    fun velocityPxPerSec(maxSpeedPx: Float): Offset {
        val newest = samples.lastOrNull() ?: return Offset.Zero
        val nowMs = clockMillis()
        if (abs(nowMs - newest.timeMs) < 5_000L && nowMs - newest.timeMs > pauseTimeoutMs) {
            return Offset.Zero
        }
        val windowStart = newest.timeMs - velocityWindowMs
        val oldest = samples.firstOrNull { it.timeMs >= windowStart && it !== newest }
            ?: samples.firstOrNull()
            ?: return Offset.Zero
        val dtSec = (newest.timeMs - oldest.timeMs) / 1000f
        if (dtSec <= 0.004f) return Offset.Zero
        val rawVel = (newest.pos - oldest.pos) / dtSec
        val scaledVel = rawVel * followFactor
        val speed = scaledVel.getDistance()
        return if (maxSpeedPx > 0f && speed > maxSpeedPx && speed > 0f) {
            scaledVel * (maxSpeedPx / speed)
        } else {
            scaledVel
        }
    }

    /** Clears recorded samples and resets the gesture anchor. */
    fun reset() {
        startTouch = null
        samples.clear()
    }
}

/** Whether platform `RenderEffect` blur is available on the current runtime. */
fun isPredictiveBackBlurSupported(): Boolean = isKitRenderEffectSupported()

/**
 * Computes the blur radius (in pixels) for an underlying route during a follow-finger return
 * animation. Both [progressFraction] (normalized remaining scale/progress) and [offsetDistancePx]
 * keep the background blurred until the returning surface has settled back home.
 */
fun calculatePredictiveBackBlurRadiusPx(
    progressFraction: Float,
    offsetDistancePx: Float,
    maxBlurRadiusPx: Float,
    blurOffsetSpanPx: Float,
): Float {
    if (maxBlurRadiusPx <= 0f) return 0f
    val base = progressFraction.coerceIn(0f, 1f)
    val offset = if (blurOffsetSpanPx > 0f) {
        (offsetDistancePx / blurOffsetSpanPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    return maxBlurRadiusPx * maxOf(base, offset)
}

/**
 * Applies a clamped Gaussian blur [androidx.compose.ui.graphics.RenderEffect] for [radiusPx], or
 * clears it when [radiusPx] is at or below [PredictiveBackMotionDefaults.BACK_BLUR_THRESHOLD_PX].
 */
fun GraphicsLayerScope.applyPredictiveBackBlur(radiusPx: Float) {
    renderEffect = if (isKitRenderEffectSupported() && radiusPx > PredictiveBackMotionDefaults.BACK_BLUR_THRESHOLD_PX) {
        createBlurRenderEffect(radiusPx, radiusPx, TileMode.Clamp)
    } else {
        null
    }
}

/**
 * Blurs the underlying page while a follow-finger predictive back gesture is active and clears the
 * blur smoothly as [progressFraction] and [offset] return to zero.
 */
@Composable
fun Modifier.predictiveBackBackgroundBlur(
    enabled: Boolean,
    progressFraction: () -> Float,
    offset: () -> Offset = { Offset.Zero },
    maxRadius: Dp = PredictiveBackMotionDefaults.BackBlurMaxRadius,
    offsetSpan: Dp = PredictiveBackMotionDefaults.BackBlurOffsetSpan,
): Modifier {
    val supported = enabled && isPredictiveBackBlurSupported()
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { maxRadius.toPx() }
    val offsetSpanPx = with(density) { offsetSpan.toPx() }
    return graphicsLayer {
        val radiusPx = if (supported) {
            calculatePredictiveBackBlurRadiusPx(
                progressFraction = progressFraction(),
                offsetDistancePx = offset().getDistance(),
                maxBlurRadiusPx = maxRadiusPx,
                blurOffsetSpanPx = offsetSpanPx,
            )
        } else {
            0f
        }
        applyPredictiveBackBlur(radiusPx)
    }
}
