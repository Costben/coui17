// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.State
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * OlivelinkDefault press motion, summed with deformation scale deltas within the same touch session.
 * Create and invoke on the main thread; once [scope] is cancelled, the spring stops and no further
 * input is accepted. [size] is in pixels; prefer [rememberCouiPressMotion] in Compose.
 */
internal class CouiPressMotion(scope: CoroutineScope, private val density: Float) {
    private val spring = CouiSpring(1f, 0.001f)
    private var disposed = false
    private var candy = false
    val scale: State<Float> = spring.state
    private val lifetime = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            disposed = true
            spring.cancel()
        }
    }

    fun press(size: IntSize) {
        if (disposed || size.width <= 0 || size.height <= 0) return
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        val minArea = 20f * density * 20f * density
        val maxArea = 150f * density * 150f * density
        val areaProgress = ((width * height - minArea) / (maxArea - minArea)).coerceIn(0f, 1f)
        val areaScale = 0.85f + 0.12f * Interpolator.transform(areaProgress)
        val aspect = max(width, height) / min(width, height)
        candy = aspect > 2f
        val aspectScale = 0.85f + 0.15f * Interpolator.transform(((aspect - 2f) / 13f).coerceIn(0f, 1f))
        val target = (if (candy) (areaScale + aspectScale) * 0.5f else areaScale).coerceIn(0.4f, 1f)
        spring.animateTo(target, if (candy) 0.3f else 0.25f, minimumChange = 0.001f, velocity = 0f)
    }

    fun release() {
        if (disposed) return
        spring.animateTo(1f, 0.5f, if (candy) 0.5f else 0.6f, minimumChange = 0.0001f)
    }

    internal fun dispose() {
        disposed = true
        spring.cancel()
        lifetime.cancel()
    }

    private companion object {
        val Interpolator = CubicBezierEasing(0f, 0f, 0f, 1f)
    }
}
