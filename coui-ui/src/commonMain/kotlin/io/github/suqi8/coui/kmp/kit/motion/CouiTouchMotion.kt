// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign

/**
 * Shared computation for COUIDeformAnimatorUnit and COUIRubberAnimatorUnit; input ownership remains
 * with each component. Create and invoke on the main thread; once [scope] is cancelled, springs stop
 * and further input is ignored. Displacements and pointers are in pixels; prefer
 * [rememberCouiTouchMotion] in Compose.
 */
internal class CouiTouchMotion(
    scope: CoroutineScope,
    private val density: Float,
    private val deformationEnabled: Boolean = true,
) {
    private var disposed = false
    private val scaleXSpring = CouiSpring(1f, 0.0005f)
    private val scaleYSpring = CouiSpring(1f, 0.0005f)
    private val translationXSpring = CouiSpring(0f, 0.5f)
    private val translationYSpring = CouiSpring(0f, 0.5f)
    private val stretchDistance = round((28f * density).toDouble()).toFloat()
    private val deformDistance = 150f * density
    private var rubberOrigin = Offset.Zero
    private var deformOrigin = Offset.Zero
    private var targetSize = IntSize.Zero
    private var maximumScale = 1.2f
    private var dragging = false
    private var moved = false

    val scaleX: State<Float> = scaleXSpring.state
    val scaleY: State<Float> = scaleYSpring.state
    val translationX: State<Float> = translationXSpring.state
    val translationY: State<Float> = translationYSpring.state
    var coordinates: LayoutCoordinates? = null

    private val lifetime = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            disposed = true
            cancel()
        }
    }

    fun begin(position: Offset, size: IntSize) {
        if (disposed) return
        cancel()
        targetSize = size
        maximumScale = maximumScale(size)
        rubberOrigin = position - Offset(
            inverseRubber(translationX.value, 0.05f, stretchDistance),
            inverseRubber(translationY.value, 0.05f, stretchDistance),
        )
        val xDominant = scaleX.value >= scaleY.value
        val currentScale = if (xDominant) scaleX.value else scaleY.value
        val distance = inverseRubber(
            min((currentScale - 1f) / (maximumScale - 1f), 1f) * deformDistance,
            0.55f,
            deformDistance,
        )
        deformOrigin = position - if (xDominant) Offset(distance, 0f) else Offset(0f, distance)
        dragging = true
        moved = translationX.value != 0f || translationY.value != 0f ||
            scaleX.value != 1f || scaleY.value != 1f
    }

    fun switchPointer(previous: Offset?, next: Offset) {
        if (previous == null) return
        val adjustment = next - previous
        rubberOrigin += adjustment
        deformOrigin += adjustment
    }

    fun move(position: Offset) {
        if (!dragging) return
        val rubberDelta = position - rubberOrigin
        if (abs(rubberDelta.x) >= 5f || abs(rubberDelta.y) >= 5f) {
            moved = true
            translationXSpring.animateTo(rubber(rubberDelta.x, 0.05f, stretchDistance), 0.15f, 0.15f)
            translationYSpring.animateTo(rubber(rubberDelta.y, 0.05f, stretchDistance), 0.15f, 0.15f)
        }
        if (!deformationEnabled) return
        val delta = position - deformOrigin
        val x = abs(delta.x)
        val y = abs(delta.y)
        if (x < 5f && y < 5f) return
        moved = true
        val sx = 1f + (maximumScale - 1f) * rubber(x, 0.55f, deformDistance) / deformDistance
        val sy = 1f + (maximumScale - 1f) * rubber(y, 0.55f, deformDistance) / deformDistance
        val xWeight = x / (x + y)
        val yWeight = y / (x + y)
        val targetX = when {
            y == 0f -> sx
            x == 0f -> 1f / sy
            else -> exp(ln(1f / sy) * yWeight + ln(sx) * xWeight)
        }
        val targetY = when {
            y == 0f -> 1f / sx
            x == 0f -> sy
            else -> exp(yWeight * ln(sy) + xWeight * ln(1f / sx))
        }
        scaleXSpring.animateTo(targetX, 0.15f, 0.15f)
        scaleYSpring.animateTo(targetY, 0.15f, 0.15f)
    }

    fun release(velocity: Offset) {
        if (!dragging) return
        dragging = false
        if (!moved) return
        val smallSide = 56f * density
        val largeSide = 100f * density
        val area = targetSize.width.toFloat() * targetSize.height
        val fraction = (
            (area - smallSide * smallSide) /
                (largeSide * largeSide - smallSide * smallSide)
            ).coerceIn(0f, 1f)
        val response = 0.42f + (0.45f - 0.42f) * HandUpAreaInterpolator.transform(fraction)
        releaseAxis(translationXSpring, 0f, velocity.x, response)
        releaseAxis(translationYSpring, 0f, velocity.y, response)
        if (!deformationEnabled) return
        releaseAxis(scaleXSpring, 1f, velocity.x, response)
        releaseAxis(scaleYSpring, 1f, velocity.y, response)
    }

    /** Stops at the current position, preserving underlying velocity and pending target without resetting deformation. */
    fun cancel() {
        scaleXSpring.cancel()
        scaleYSpring.cancel()
        translationXSpring.cancel()
        translationYSpring.cancel()
        dragging = false
    }

    /** Immediately restores the undeformed state without retaining targets, velocity, or session state. */
    fun reset() {
        dragging = false
        moved = false
        scaleXSpring.snapTo(1f)
        scaleYSpring.snapTo(1f)
        translationXSpring.snapTo(0f)
        translationYSpring.snapTo(0f)
    }

    internal fun dispose() {
        disposed = true
        cancel()
        coordinates = null
        lifetime.cancel()
    }

    private fun releaseAxis(
        axis: CouiSpring,
        target: Float,
        velocity: Float,
        response: Float,
    ) {
        val rest = abs(axis.value - target)
        if (target == 1f && rest < 0.002f) {
            axis.snapTo(1f)
            return
        }
        // Both vendor units pass true; hand-up normalization uses maxStretchDistance rather than deformation limit.
        val initialVelocity = (velocity / max(28f * density, 1f) * rest).coerceIn(-1000f, 1000f)
        val bounce = max(0.65f, min(abs(initialVelocity) / 1000f, 1f))
        axis.animateTo(target, response, bounce, velocity = initialVelocity)
    }

    private fun maximumScale(size: IntSize): Float {
        val area = size.width.toFloat() * size.height
        val shortSide = min(size.width, size.height)
        if (shortSide == 0 || area <= 38416f) return 1.2f
        val aspect = max(size.width, size.height).toFloat() / shortSide
        val decay = min(0.015f * (aspect - 1f) + 0.35f * ln(area / 38416f), 1f)
        return max(1.03f, 1.03f + (1.2f - 1.03f) * (1f - decay))
    }

    private fun rubber(distance: Float, ratio: Float, maximum: Float): Float = if (maximum <= 0f) {
        0f
    } else {
        sign(distance) * (1f - 1f / (abs(distance) * ratio / maximum + 1f)) * maximum
    }

    private fun inverseRubber(value: Float, ratio: Float, maximum: Float): Float {
        if (maximum <= 0f) return value
        val magnitude = abs(value)
        if (magnitude >= maximum || ratio <= 0f) return sign(value) * maximum
        return sign(value) * magnitude * maximum / (ratio * (maximum - magnitude))
    }

    private companion object {
        val HandUpAreaInterpolator = CubicBezierEasing(0.55026454f, 0f, 0.8915344f, 0.18783069f)
    }
}
