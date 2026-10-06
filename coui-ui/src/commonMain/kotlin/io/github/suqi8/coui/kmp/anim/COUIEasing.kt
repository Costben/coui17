// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.anim

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI

/**
 * The easing ColorOS uses for everything entering or growing.
 *
 * It is a `PathInterpolator` with
 * `(controlX1, controlY1, controlX2, controlY2) = (0.0f, 0.0f, 0.1f, 1.0f)`.
 */
val COUIInEase: Easing = CubicBezierEasing(0f, 0f, 0.1f, 1f)

/**
 * The easing ColorOS uses for everything leaving or shrinking.
 *
 * It is a `PathInterpolator` with
 * `(controlX1, controlY1, controlX2, controlY2) = (0.3f, 0.0f, 1.0f, 1.0f)`.
 */
val COUIOutEase: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

/**
 * The easing ColorOS uses where an animation is left unshaped.
 *
 * It is `android.view.animation.LinearInterpolator` with an empty body — no fields and no overrides —
 * so it is exactly the framework's linear curve.
 */
val COUILinear: Easing = LinearEasing

/**
 * Creates a [SpringSpec] from damping and response parameters.
 *
 * @param damping The damping ratio. 1.0 = critically damped (no overshoot),
 *   < 1.0 = underdamped (oscillates), > 1.0 = overdamped.
 * @param response The response time in seconds. Smaller values = faster animation.
 * @param visibilityThreshold The magnitude below which the animation is considered settled and the
 *   invisible settle tail is cut; null falls back to the framework default displacement threshold.
 */
fun <T> folmeSpring(
    damping: Float,
    response: Float,
    visibilityThreshold: T? = null,
): SpringSpec<T> {
    val stiffness = ((2.0 * PI / response) * (2.0 * PI / response)).toFloat()
    return spring(dampingRatio = damping, stiffness = stiffness, visibilityThreshold = visibilityThreshold)
}
