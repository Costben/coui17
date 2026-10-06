// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import io.github.suqi8.coui.kmp.anim.COUIInEase
import io.github.suqi8.coui.kmp.anim.COUIOutEase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the bar-level motion values in [NavigationBarMotion] to the ColorOS 17 navigation bar.
 *
 * These are the bar's canonical durations: three translate durations (350ms enter, 250ms show,
 * 200ms hide) plus two hard-coded 100ms alpha durations — so a drift here means the port stopped
 * matching the design it models.
 *
 * The easings are asserted through the `PathInterpolator` control points rather than by identity,
 * because that is the value the design carries: the in-ease is `pathInterpolator(0, 0, 0.1, 1)`, the
 * out-ease is `pathInterpolator(0.3, 0, 1, 1)` and the exit alpha runs linear.
 */
class NavigationBarMotionTest {

    @Test
    fun durationsMatchTheDecompiledConstants() {
        assertEquals(250, NavigationBarMotion.ShowTranslateDurationMillis)
        assertEquals(200, NavigationBarMotion.HideTranslateDurationMillis)
        assertEquals(100, NavigationBarMotion.EnterAlphaDurationMillis)
        assertEquals(350, NavigationBarMotion.EnterTranslateDurationMillis)
        assertEquals(100, NavigationBarMotion.ExitAlphaDurationMillis)
    }

    @Test
    fun theBarSlideAndTheMenuEnterRunOnTheInEase() {
        // COUINavigationView: mShowTranslateAnimation and mEnterTranslateAnimation both set
        // COUIInEaseInterpolator; the bar slides in over 250ms, the menu enters over 350ms.
        assertTween(
            spec = NavigationBarMotion.ShowSpec,
            durationMillis = 250,
            easing = COUIInEase,
        )
        assertTween(
            spec = NavigationBarMotion.EnterTranslateSpec,
            durationMillis = 350,
            easing = COUIInEase,
        )
    }

    @Test
    fun theBarSlideOutRunsOnTheOutEase() {
        // COUINavigationView.mHideTranslateAnimation: 200ms on COUIOutEaseInterpolator.
        assertTween(
            spec = NavigationBarMotion.HideSpec,
            durationMillis = 200,
            easing = COUIOutEase,
        )
    }

    @Test
    fun bothAlphaTracksAreHundredMilliseconds() {
        // AnimatorSet.playTogether(mEnterAnimation, mEnterTranslateAnimation) starts the two enter
        // tracks together; the enter alpha is COUIInEaseInterpolator and the exit alpha is
        // COUILinearInterpolator.
        assertTween(
            spec = NavigationBarMotion.EnterAlphaSpec,
            durationMillis = 100,
            easing = COUIInEase,
        )
        assertTween(
            spec = NavigationBarMotion.ExitAlphaSpec,
            durationMillis = 100,
            easing = LinearEasing,
        )
    }

    @Test
    fun theEasingsCarryTheDecompiledControlPoints() {
        // CubicBezierEasing keeps its control points private, so the curve is pinned by evaluating it
        // against a reference solver carrying the PathInterpolator control points.
        assertMatchesCubicBezier(COUIInEase, 0f, 0f, 0.1f, 1f, "COUIInEaseInterpolator")
        assertMatchesCubicBezier(COUIOutEase, 0.3f, 0f, 1f, 1f, "COUIOutEaseInterpolator")
    }

    @Test
    fun everyEasingIsPinnedAtBothEnds() {
        listOf(COUIInEase, COUIOutEase, LinearEasing).forEach { easing ->
            assertEquals(0f, easing.transform(0f), "transform(0)")
            assertEquals(1f, easing.transform(1f), "transform(1)")
            assertTrue(easing.transform(0.5f) in 0f..1f, "transform(0.5) stays in range")
        }
    }
}

/** Asserts that [spec] is a [TweenSpec] with exactly this duration and this easing instance. */
private fun assertTween(spec: AnimationSpec<Float>, durationMillis: Int, easing: Any) {
    val tween = spec as? TweenSpec<Float>
    assertTrue(tween != null, "expected a TweenSpec, got ${spec::class.simpleName}")
    assertEquals(durationMillis, tween.durationMillis)
    assertSame(easing, tween.easing)
}

private val SamplePositions = listOf(0.1f, 0.25f, 0.5f, 0.75f, 0.9f)

/** Asserts that [easing] is the cubic Bezier through `(x1, y1)` and `(x2, y2)`. */
private fun assertMatchesCubicBezier(
    easing: Any,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    name: String,
) {
    val curve = easing as? CubicBezierEasing
    assertTrue(curve != null, "$name should be a CubicBezierEasing, got ${easing::class.simpleName}")
    SamplePositions.forEach { x ->
        val expected = cubicBezier(x1, y1, x2, y2, x)
        val actual = curve.transform(x)
        assertTrue(
            kotlin.math.abs(actual - expected) < 0.005f,
            "$name at x=$x: expected $expected, got $actual",
        )
    }
}

/**
 * Evaluates a CSS-style cubic Bezier easing with control points `(x1, y1)` and `(x2, y2)`: solve for
 * the curve parameter whose x equals [x], then return that parameter's y.
 */
private fun cubicBezier(x1: Float, y1: Float, x2: Float, y2: Float, x: Float): Float {
    if (x <= 0f) return 0f
    if (x >= 1f) return 1f
    var low = 0f
    var high = 1f
    var t = x
    repeat(40) {
        if (bezierAxis(t, x1, x2) < x) low = t else high = t
        t = (low + high) / 2f
    }
    return bezierAxis(t, y1, y2)
}

/** One axis of a cubic Bezier from 0 to 1 with the given two control values. */
private fun bezierAxis(t: Float, p1: Float, p2: Float): Float {
    val u = 1f - t
    return 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t
}
