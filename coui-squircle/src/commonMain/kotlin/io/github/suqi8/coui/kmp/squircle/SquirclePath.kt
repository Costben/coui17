// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.squircle

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import kotlin.math.max
import kotlin.math.min

/** Shared default constants for every squircle API in this module. */
object SquircleDefaults {

    /**
     * Corner-tile size as a multiple of `cornerRadius`: the square the corner curve occupies, so the
     * straight edges stop `cornerRadius * Extension` short of each corner.
     *
     * ColorOS 17 draws smooth corners with the hwui "G2" signed-distance field
     * (`q = abs(p) - halfSize + r`; its corner region is exactly `q.x > 0 && q.y > 0`, i.e. `r` wide),
     * which makes the tile `1.0 * cornerRadius`. The retired ColorOS 16 legacy path
     * (`COUIShapePath.getRoundRectPath`, tile `128.19/100`) is where the previous `1.2819` came from.
     */
    val Extension = 1.0f

    /** Inclusive lower bound for [Extension]. */
    val ExtensionMin = 1f

    /** Inclusive upper bound for [Extension]. */
    val ExtensionMax = 2f
}

/**
 * Cubic Bézier handle ratio used by [addSquircleRect]. Must stay in lock-step with the value the
 * pre-baked SDF was generated for (`coui-squircle/build.gradle.kts` `bakeSquircleSdf`) so
 * path-based and shader-backed silhouettes line up.
 *
 * Tuned to the `n = 2.5` Lp superellipse COUI draws for panels (hwui "G2",
 * `|x|^n + |y|^n = R^n`). One cubic cannot equal an Lp ball, so this matches its 45° inset instead:
 * `n = 2.5` needs `h / T = 0.3123`, i.e. `1 - 0.3123 = 0.6877`. The retired `0.643` matched
 * `n ≈ 2.31` (5–7% flatter than target).
 */
internal const val SQUIRCLE_CONTROL = 0.6877f

/** The [SQUIRCLE_CONTROL] counterpart for a circular arc: the cubic quarter-circle constant. */
private const val CIRCLE_CONTROL = 0.5522848f

/**
 * Share of half the shorter side (π/4) above which a corner starts to blend toward a circular arc,
 * reaching it when the corner spans the whole half side.
 *
 * The blend is opt-in per control through [circleBlendWeight]'s `capsuleCircleBlend`; the popup
 * panel's 28dp superellipse passes `false` and keeps its curve to the capsule ends.
 */
internal const val CIRCLE_BLEND_START = 0.7853982f

/**
 * How far a corner of [tile] px has moved from the smooth corner toward a circular arc, `0..1`, on a
 * rect whose shorter side is `2 * halfMin`. [tile] is the unclamped corner tile, so any radius at or
 * past the capsule radius is fully circular.
 *
 * [capsuleCircleBlend] is the caller's choice: `true` lets the corner blend into an arc as it
 * approaches the capsule radius, while `false` keeps the smooth superellipse all the way to the
 * capsule ends. [SquircleShape] and [addSquircleRect] default to `true`; the popup panel's shape
 * passes `false`.
 */
internal fun circleBlendWeight(tile: Float, halfMin: Float, capsuleCircleBlend: Boolean = true): Float {
    if (!capsuleCircleBlend) return 0f
    val threshold = halfMin * CIRCLE_BLEND_START
    val range = halfMin - threshold
    return if (range <= 0f) 1f else ((tile - threshold) / range).coerceIn(0f, 1f)
}

// Distance from the corner point to each Bézier handle of a corner [tile] px wide.
private fun cornerHandle(tile: Float, circleWeight: Float): Float = tile * (1f - (SQUIRCLE_CONTROL + (CIRCLE_CONTROL - SQUIRCLE_CONTROL) * circleWeight))

/**
 * Appends a squircle-shaped rounded rectangle path. Use this for path-based effects that can't
 * ride the shader pipeline (e.g. `clipPath` reveals rebuilt per frame); for static fills/clips
 * prefer the modifier APIs.
 *
 * A corner that approaches half the shorter side blends into a circular arc while
 * `capsuleCircleBlend` is `true`, so a capsule keeps round ends. Pass `false` to keep the
 * superellipse all the way to the capsule ends instead, as the 28dp popup panel does.
 *
 * Pass `squircleEnabled = false` (typically forwarded from [isSquircleEnabled]) to append a
 * plain rounded rectangle of the same dimensions instead — useful when the surrounding visuals
 * use the shader-backed modifiers' fallback path.
 *
 * @param width The width of the rectangle in pixels; nothing is appended when not positive.
 * @param height The height of the rectangle in pixels; nothing is appended when not positive.
 * @param cornerRadius The corner radius in pixels, clamped to half the smaller side.
 * @param extension The corner-tile size as a multiple of [cornerRadius], clamped to
 *   [SquircleDefaults.ExtensionMin]..[SquircleDefaults.ExtensionMax].
 * @param squircleEnabled When `false`, appends a plain rounded rectangle instead of the squircle
 *   silhouette; typically forwarded from [isSquircleEnabled].
 * @param capsuleCircleBlend Whether a corner at the capsule radius blends into a circular arc
 *   (`true`) or keeps the continuous-curvature superellipse (`false`).
 */
fun Path.addSquircleRect(
    width: Float,
    height: Float,
    cornerRadius: Float,
    extension: Float = SquircleDefaults.Extension,
    squircleEnabled: Boolean = true,
    capsuleCircleBlend: Boolean = true,
) {
    if (width <= 0f || height <= 0f) return
    if (!squircleEnabled) {
        val radius = max(0f, cornerRadius).coerceAtMost(min(width, height) * 0.5f)
        if (radius <= 0f) {
            addRect(Rect(0f, 0f, width, height))
        } else {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = width,
                    bottom = height,
                    cornerRadius = CornerRadius(radius, radius),
                ),
            )
        }
        return
    }
    val extClamped = extension.coerceIn(SquircleDefaults.ExtensionMin, SquircleDefaults.ExtensionMax)
    val halfMin = min(width, height) * 0.5f
    val requestedTile = max(0f, cornerRadius * extClamped)
    val tile = requestedTile.coerceAtMost(halfMin)
    if (tile <= 0f) {
        addRect(Rect(0f, 0f, width, height))
        return
    }
    val handle = cornerHandle(tile, circleBlendWeight(requestedTile, halfMin, capsuleCircleBlend))
    moveTo(tile, 0f)
    lineTo(width - tile, 0f)
    cubicTo(width - handle, 0f, width, handle, width, tile)
    lineTo(width, height - tile)
    cubicTo(width, height - handle, width - handle, height, width - tile, height)
    lineTo(tile, height)
    cubicTo(handle, height, 0f, height - handle, 0f, height - tile)
    lineTo(0f, tile)
    cubicTo(0f, handle, handle, 0f, tile, 0f)
    close()
}

/**
 * Binary-compatibility bridge for [addSquircleRect] callers compiled before `capsuleCircleBlend`
 * existed; the published 1.1.0 bytecode links to this signature.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds capsuleCircleBlend.",
    level = DeprecationLevel.HIDDEN,
)
fun Path.addSquircleRect(
    width: Float,
    height: Float,
    cornerRadius: Float,
    extension: Float = SquircleDefaults.Extension,
    squircleEnabled: Boolean = true,
) {
    addSquircleRect(
        width = width,
        height = height,
        cornerRadius = cornerRadius,
        extension = extension,
        squircleEnabled = squircleEnabled,
        capsuleCircleBlend = true,
    )
}

/**
 * Appends a squircle-shaped rounded rectangle at an arbitrary [rect] with one radius per corner.
 *
 * [addSquircleRect] covers the static case: a rect at the origin with four equal corners. A
 * silhouette that is interpolated frame by frame — the cascading popup's anchor-to-submenu morph —
 * moves its rect and opens its corners at different rates, and it still has to trace the same corner
 * family as the surface underneath it. A plain [RoundRect]'s circular corners part from the Lp
 * superellipse along the diagonal by `0.0718 * radius`, which is 7.8px at the popup's 28dp radius.
 *
 * Radii are clamped exactly the way the shader-backed silhouette clamps them — to `min(radius,
 * min(width, height) / 2)` — so a path built here and a rim highlight built from the same radii stay
 * on one curve as the shape shrinks.
 *
 * @param rect The rectangle to append; nothing is appended when either side is not positive.
 * @param topLeft Corner radius in pixels, measured from the corner centre.
 * @param topRight Corner radius in pixels.
 * @param bottomRight Corner radius in pixels.
 * @param bottomLeft Corner radius in pixels.
 * @param extension The corner-tile size as a multiple of the radius, clamped to
 *   [SquircleDefaults.ExtensionMin]..[SquircleDefaults.ExtensionMax].
 * @param squircleEnabled When `false`, appends a plain rounded rectangle instead.
 * @param capsuleCircleBlend Whether a corner at the capsule radius blends into a circular arc
 *   (`true`) or keeps the continuous-curvature superellipse (`false`).
 */
fun Path.addSquircleRoundRect(
    rect: Rect,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
    extension: Float = SquircleDefaults.Extension,
    squircleEnabled: Boolean = true,
    capsuleCircleBlend: Boolean = true,
) {
    val width = rect.width
    val height = rect.height
    if (width <= 0f || height <= 0f) return
    val halfMin = min(width, height) * 0.5f
    fun clamp(radius: Float): Float = max(0f, radius).coerceAtMost(halfMin)
    if (!squircleEnabled) {
        val tl = clamp(topLeft)
        val tr = clamp(topRight)
        val br = clamp(bottomRight)
        val bl = clamp(bottomLeft)
        if (tl <= 0f && tr <= 0f && br <= 0f && bl <= 0f) {
            addRect(rect)
        } else {
            addRoundRect(
                RoundRect(
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom,
                    topLeftCornerRadius = CornerRadius(tl, tl),
                    topRightCornerRadius = CornerRadius(tr, tr),
                    bottomRightCornerRadius = CornerRadius(br, br),
                    bottomLeftCornerRadius = CornerRadius(bl, bl),
                ),
            )
        }
        return
    }
    val extClamped = extension.coerceIn(SquircleDefaults.ExtensionMin, SquircleDefaults.ExtensionMax)
    val tl = clamp(topLeft * extClamped)
    val tr = clamp(topRight * extClamped)
    val br = clamp(bottomRight * extClamped)
    val bl = clamp(bottomLeft * extClamped)
    if (tl <= 0f && tr <= 0f && br <= 0f && bl <= 0f) {
        addRect(rect)
        return
    }
    val l = rect.left
    val t = rect.top
    val r = rect.right
    val b = rect.bottom
    val handleTl = cornerHandle(tl, circleBlendWeight(topLeft * extClamped, halfMin, capsuleCircleBlend))
    val handleTr = cornerHandle(tr, circleBlendWeight(topRight * extClamped, halfMin, capsuleCircleBlend))
    val handleBr = cornerHandle(br, circleBlendWeight(bottomRight * extClamped, halfMin, capsuleCircleBlend))
    val handleBl = cornerHandle(bl, circleBlendWeight(bottomLeft * extClamped, halfMin, capsuleCircleBlend))
    moveTo(l + tl, t)
    lineTo(r - tr, t)
    cubicTo(r - handleTr, t, r, t + handleTr, r, t + tr)
    lineTo(r, b - br)
    cubicTo(r, b - handleBr, r - handleBr, b, r - br, b)
    lineTo(l + bl, b)
    cubicTo(l + handleBl, b, l, b - handleBl, l, b - bl)
    lineTo(l, t + tl)
    cubicTo(l, t + handleTl, l + handleTl, t, l + tl, t)
    close()
}
