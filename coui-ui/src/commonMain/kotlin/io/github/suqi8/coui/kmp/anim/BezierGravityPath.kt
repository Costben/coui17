// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.anim

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Tuning for the popup's Bezier gravity trajectory (COUI `BezierGravityConfig`, embedded in
 * `PopupGravityMotionParams` as `gravityDistanceMinPx` / `gravityDistanceMaxPx` / `gravityArcMaxPx`).
 *
 * @property dMin Shortest anchor→menu centre distance, in pixels, that still produces arc sag.
 *   At or below it the trajectory is a straight line.
 * @property dMax Distance, in pixels, at which the sag reaches its full `gMax`.
 * @property gMax Sag amplitude cap, in pixels, reached at [dMax] and beyond.
 */
@Immutable
data class BezierGravityConfig(
    val dMin: Float,
    val dMax: Float,
    val gMax: Float,
) {
    companion object {
        /** COUI `BezierGravityConfig.DEFAULT` (dMin 30 / dMax 400 / gMax 80). */
        val Default = BezierGravityConfig(dMin = 30f, dMax = 400f, gMax = 80f)
    }
}

/**
 * A cubic Bezier sampled by the popup path spring (COUI `BezierGravityPathGeometry`): start `A`,
 * the two gravity-offset control points `P1` / `P2`, and end `B`.
 */
@Immutable
data class BezierGravityPathGeometry(
    val ax: Float,
    val ay: Float,
    val p1x: Float,
    val p1y: Float,
    val p2x: Float,
    val p2y: Float,
    val bx: Float,
    val by: Float,
) {
    /**
     * Cubic Bezier evaluation (COUI `BezierGravityPathMath.pointOnBezier`). The path spring drives
     * `t` from 0 to 1; the spring is underdamped on entry, so `t` may overshoot past 1, as in COUI.
     */
    fun pointAt(t: Float): Offset {
        val inv = 1f - t
        val inv2 = inv * inv
        val t2 = t * t
        val inv3 = inv2 * inv
        val t3 = t2 * t
        val w1 = inv2 * 3f * t
        val w2 = inv * 3f * t2
        return Offset(
            x = ax * inv3 + p1x * w1 + p2x * w2 + bx * t3,
            y = ay * inv3 + p1y * w1 + p2y * w2 + by * t3,
        )
    }
}

/** Control-point positions along the chord after clamping and ordering (COUI `BezierPathControlT`). */
@Immutable
internal data class BezierPathControlT(val effectiveP1T: Float, val effectiveP2T: Float)

/**
 * Geometry for the popup's Bezier gravity trajectory.
 *
 * The trajectory starts at the popup path anchor, expressed as an offset from the menu centre, and
 * ends at the menu centre (the origin), so the popup flies in from the finger/anchor along an arc
 * that sags away from the chord instead of travelling in a straight line.
 */
object BezierGravityPathMath {
    private const val ALIGNMENT_EPS = 1.0E-4f
    private const val DIST_SPAN_EPS = 1.0E-4f

    /** Below this the chord counts as vertical and the perpendicular is pinned to +Y. */
    private const val PERP_Y_NEAR_ZERO_EPS = 0.001f

    /** Chords shorter than this are degenerate and produce a straight eased line. */
    private const val DEGENERATE_CHORD_LEN_PX = 1f

    private const val PATH_T_MIN = 0.05f
    private const val PATH_T_MAX = 0.95f

    /** Half the minimum spacing enforced between the two control-point positions. */
    private const val PATH_T_HALF_GAP = 0.04f
    private const val PATH_T_ORDER_MIN_SPAN = 0.08f
    private const val PATH_T_CENTER_FACTOR = 0.5f

    /**
     * Arc sag in pixels (COUI `BezierGravityPathMath.adaptiveGravityPx`): ramps from 0 to `gMax`
     * as the chord grows from `dMin` to `dMax`, scaled down by how axis-aligned the chord is
     * (`min(|dx|,|dy|) / max(|dx|,|dy|)`), so a diagonal hop sags the most and a purely
     * horizontal/vertical one sag is unchanged.
     */
    fun adaptiveGravityPx(dx: Float, dy: Float, len: Float, config: BezierGravityConfig): Float {
        val span = config.dMax - config.dMin
        val distanceFraction = when {
            span > DIST_SPAN_EPS -> ((len - config.dMin) / span).coerceIn(0f, 1f)
            len <= config.dMin -> 0f
            else -> 1f
        }
        val absDx = abs(dx)
        val absDy = abs(dy)
        return config.gMax * distanceFraction * (min(absDx, absDy) / (max(absDx, absDy) + ALIGNMENT_EPS))
    }

    /**
     * Orders and separates the two control-point positions (COUI
     * `BezierGravityPathMath.normalizePathTParameters`). Both are clamped into
     * `[PATH_T_MIN, PATH_T_MAX]`, then sorted; if they end up closer than 0.08 they are spread to
     * `mid ± 0.04` around their midpoint.
     */
    internal fun normalizePathTParameters(p1T: Float, p2T: Float): BezierPathControlT {
        var larger = p1T.coerceIn(PATH_T_MIN, PATH_T_MAX)
        var smaller = p2T.coerceIn(PATH_T_MIN, PATH_T_MAX)
        if (larger <= smaller) {
            val swap = larger
            larger = smaller
            smaller = swap
        }
        if (larger - smaller < PATH_T_ORDER_MIN_SPAN) {
            val mid = ((larger + smaller) * PATH_T_CENTER_FACTOR).coerceIn(0.09f, 0.91f)
            smaller = (mid - PATH_T_HALF_GAP).coerceIn(PATH_T_MIN, PATH_T_MAX)
            larger = (mid + PATH_T_HALF_GAP).coerceIn(PATH_T_MIN, PATH_T_MAX)
        }
        // COUI's BezierPathControlT(effectiveP1T, effectiveP2T) takes the sorted pair, so P1 sits
        // at the lower chord parameter and P2 at the higher one.
        return BezierPathControlT(effectiveP1T = smaller, effectiveP2T = larger)
    }

    /**
     * Builds the trajectory (COUI `BezierGravityPathMath.controlPoints`).
     *
     * Both control points sit at their normalised positions along the chord and are then pushed off
     * it by the same perpendicular vector of length [adaptiveGravityPx] — the bulge direction is
     * flipped to have a non-negative Y, so the arc always sags downwards.
     *
     * @param start Trajectory start, normally the popup path anchor relative to the menu centre.
     * @param end Trajectory end, normally the menu centre (`Offset.Zero`).
     * @param p1T First control point position along the chord, from `PopupGravityMotion`.
     * @param p2T Second control point position along the chord, from `PopupGravityMotion`.
     */
    fun controlPoints(
        start: Offset,
        end: Offset,
        config: BezierGravityConfig,
        p1T: Float,
        p2T: Float,
    ): BezierGravityPathGeometry {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < DEGENERATE_CHORD_LEN_PX) {
            return BezierGravityPathGeometry(
                ax = start.x,
                ay = start.y,
                p1x = start.x,
                p1y = start.y,
                p2x = end.x,
                p2y = end.y,
                bx = end.x,
                by = end.y,
            )
        }

        val controlT = normalizePathTParameters(p1T, p2T)
        val gravity = adaptiveGravityPx(dx, dy, len, config)

        val invLen = 1f / len
        var normalX = -dy * invLen
        var normalY = invLen * dx
        if (normalY < 0f) {
            normalX = -normalX
            normalY = -normalY
        }
        val offsetX: Float
        val offsetY: Float
        if (abs(normalY) >= PERP_Y_NEAR_ZERO_EPS) {
            offsetX = normalX * gravity
            offsetY = normalY * gravity
        } else {
            offsetX = 0f
            offsetY = gravity
        }

        return BezierGravityPathGeometry(
            ax = start.x,
            ay = start.y,
            p1x = start.x + controlT.effectiveP1T * dx + offsetX,
            p1y = start.y + controlT.effectiveP1T * dy + offsetY,
            p2x = start.x + controlT.effectiveP2T * dx + offsetX,
            p2y = start.y + controlT.effectiveP2T * dy + offsetY,
            bx = end.x,
            by = end.y,
        )
    }
}

/**
 * Popup gravity trajectory tuning (COUI `PopupGravityMotionParams`, defaulted by
 * `PopupGravityMotionParamsExtKt.get`) minus the six springs, which live on the popup defaults so
 * callers can retune them per component.
 *
 * @property gravityDistanceMinPx COUI `gravityDistanceMinPx`.
 * @property gravityDistanceMaxPx COUI `gravityDistanceMaxPx`.
 * @property gravityArcMaxPx COUI `gravityArcMaxPx`.
 * @property p1T COUI `p1T`, first control point position along the anchor→menu chord.
 * @property p2T COUI `p2T`, second control point position along the anchor→menu chord.
 * @property pathAnchorEdgeOffsetY COUI `pathAnchorEdgeOffsetY`, pushed along the placement axis:
 *   added below the anchor bottom when the menu opens downwards, subtracted above the anchor top
 *   when it opens upwards.
 */
@Immutable
data class PopupGravityMotion(
    val gravityDistanceMinPx: Float = 30f,
    val gravityDistanceMaxPx: Float = 400f,
    val gravityArcMaxPx: Float = 130f,
    val p1T: Float = 0.35f,
    val p2T: Float = 0.65f,
    val pathAnchorEdgeOffsetY: Float = 0f,
) {
    /** COUI `PopupGravityMotionParams` default instance for `coui_popup_motion`. */
    val gravityConfig: BezierGravityConfig
        get() = BezierGravityConfig(
            dMin = gravityDistanceMinPx,
            dMax = gravityDistanceMaxPx,
            gMax = gravityArcMaxPx,
        )

    companion object {
        val Default = PopupGravityMotion()
    }
}
