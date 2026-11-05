// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.util.VelocityTracker1D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.time.TimeSource

/**
 * The flash a COUI seek bar gives when its progress runs into an end
 * (`COUISeekBarAdvanced` + `GlitterEffectRenderer`, on by default through
 * `couiSeekBarShowGlitterEffect`).
 *
 * Nothing shows at rest. When a drag pushes the progress onto an end while the finger still moves
 * towards it, or a fling carries it there, a critically damped spring (response 0.6) is kicked with
 * the finger's speed and settles back to zero; its value maps to the opacity of a white gradient
 * over the active track that brightens towards the edge it hit. The flash fires once per contact
 * with an edge: it is re-armed only after the progress leaves that edge again.
 *
 * Positions and velocities are measured in the direction the value grows, so the caller resolves
 * layout direction and orientation before reporting them.
 */
@Stable
internal class SliderGlitter(
    private val uptimeMillis: () -> Long = TimeSource.Monotonic.markNow().let { mark ->
        { mark.elapsedNow().inWholeMilliseconds }
    },
) {
    private val pulse = Animatable(0f)
    private var bumpingEdges = false
    private val tracker = VelocityTracker1D(isDataDifferential = true)

    /** Whether the last flash hit the end of the value range rather than its start. */
    var atEnd by mutableStateOf(true)
        private set

    /** The flash's opacity in 0..1. Read it in the draw phase. */
    val alpha: Float get() = glitterAlpha(pulse.value)

    /** A new touch: `COUISeekBarCore` clears the edge contact on `ACTION_DOWN`. */
    fun onDragStarted() {
        bumpingEdges = false
        tracker.resetTracking()
    }

    /**
     * One drag frame (`executeTouchGlitterEffectAnim`, then `resetBumpingEdges`).
     *
     * @param fraction the progress the drag asks for, 0..1, before any snapping
     * @param delta the drag distance of this frame, in px
     * @param bothEdges whether the start edge flashes too: true for a bar that grows from the
     *   middle, and for a range whose lower thumb can run into the start
     */
    fun onDrag(scope: CoroutineScope, fraction: Float, delta: Float, bothEdges: Boolean) {
        tracker.addDataPoint(uptimeMillis(), delta)
        val velocity = tracker.calculateVelocity(MAX_VELOCITY)
        when {
            fraction >= 1f -> if (!bumpingEdges && velocity > 0f) start(scope, velocity, end = true)
            bothEdges && fraction <= 0f -> if (!bumpingEdges && velocity < 0f) start(scope, velocity, end = false)
            else -> bumpingEdges = false
        }
    }

    /**
     * One fling frame (`executeFlingGlitterEffectAnim`): the flash fires on the frame the fling
     * crosses onto an edge.
     *
     * @param fraction the progress of this frame, 0..1
     * @param lastFraction the progress of the previous frame
     * @param velocity the release velocity, in px/s
     */
    fun onFling(scope: CoroutineScope, fraction: Float, lastFraction: Float, velocity: Float, bothEdges: Boolean) {
        if (bumpingEdges) return
        when {
            fraction >= 1f && lastFraction < 1f -> start(scope, velocity, end = true)
            bothEdges && fraction <= 0f && lastFraction > 0f -> start(scope, velocity, end = false)
        }
    }

    private fun start(scope: CoroutineScope, velocity: Float, end: Boolean) {
        bumpingEdges = true
        atEnd = end
        scope.launch {
            // `GlitterEffectRenderer.startAnimation`: start from the current value, aim at 0, and
            // carry the finger's speed as the start velocity.
            pulse.animateTo(0f, GlitterSpring, initialVelocity = min(abs(velocity), MAX_VELOCITY))
        }
    }

    /**
     * The gradient over an active track that spans [start] to [end] on the axis it grows along,
     * bright on the edge the flash hit; `null` while nothing shows.
     *
     * @param valueEndAtFar whether the end of the value range lies at [end]
     */
    fun brush(start: Float, end: Float, horizontal: Boolean, valueEndAtFar: Boolean): Brush? {
        val alpha = alpha
        if (alpha <= 0f || end <= start) return null
        val max = GlitterMaxColor.copy(alpha = GlitterMaxColor.alpha * alpha)
        val brightAtFar = atEnd == valueEndAtFar
        val from = if (brightAtFar) GlitterMinColor else max
        val to = if (brightAtFar) max else GlitterMinColor
        return if (horizontal) {
            Brush.horizontalGradient(listOf(from, to), startX = start, endX = end)
        } else {
            Brush.verticalGradient(listOf(from, to), startY = start, endY = end)
        }
    }

    private companion object {
        /** The cap `computeCurrentVelocity(1000, 8000)` and the fling path both put on the speed. */
        const val MAX_VELOCITY = 8000f
    }
}

/**
 * `GlitterEffectRenderer.computeGlitterEffectAlpha`: `1 - exp(-ln(85) / 360 * x)`, as a fraction of
 * full opacity.
 */
internal fun glitterAlpha(value: Float): Float = (1f - exp(-GlitterAlphaRate * value)).coerceIn(0f, 1f)

private val GlitterAlphaRate = (ln(85.0) / 360.0).toFloat()

/** `GLITTER_EFFECT_SPRING_RESPONSE` 0.6 with `SPRING_BOUNCE` 0: stiffness (2π / response)². */
private val GlitterSpring = spring<Float>(
    dampingRatio = 1f,
    stiffness = ((2.0 * PI / 0.6) * (2.0 * PI / 0.6)).toFloat(),
)

/** `coui_seekbar_glitter_effect_min_color`. */
private val GlitterMinColor = Color(0x00FFFFFF)

/** `coui_seekbar_glitter_effect_max_color`. */
private val GlitterMaxColor = Color(0xA0FFFFFF)
