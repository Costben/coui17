// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.squircle

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * The ColorOS smooth-corner silhouette expressed as a [Shape].
 *
 * [squircleBackground] and its siblings paint the silhouette through a runtime shader, so they can
 * only be used as modifiers. APIs that accept nothing but a [Shape] — backdrop blur clipping,
 * `shadow`, `border`, `clip` — cannot reach them. This wraps [Path.addSquircleRoundRect] in a shape
 * so those call sites get the same corner geometry instead of falling back to a plain rounded
 * rectangle.
 *
 * It extends [CornerBasedShape] rather than implementing [Shape] directly so the corner radii stay
 * readable *from* the shape. Consumers that know the silhouette only through this object have to
 * read the radii off it; the glass rim highlight, for one, builds its rounded-rect SDF from the
 * [CornerBasedShape] corners and otherwise falls back to `min(width, height) / 2`, which turns the
 * rim into a stadium that no longer follows the surface.
 *
 * @property capsuleCircleBlend Whether a corner at the capsule radius blends into a circular arc
 *   (`true`) or keeps the continuous-curvature superellipse (`false`, the 28dp popup panel corner).
 *   Button capsules do not use this shape; they trace the system capsule curve instead.
 * @property weight The Lp exponent of the corner curve, [SquircleDefaults.WeightNormal] by default.
 *   Pass [SquircleDefaults.WeightLarge] where ColorOS 17 draws weight 3.0 — cards, dialog and sheet
 *   panels — so the outline matches the fill it clips.
 */
@Immutable
class SquircleShape private constructor(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    private val extension: Float,
    private val squircleEnabled: Boolean,
    val capsuleCircleBlend: Boolean,
    val weight: Float,
) : CornerBasedShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart,
) {

    /**
     * A smooth-corner shape with the same radius on all four corners.
     *
     * @param cornerRadius The corner radius in dp, applied to all four corners.
     * @param extension The corner-tile size as a multiple of [cornerRadius], clamped to
     *   [SquircleDefaults.ExtensionMin]..[SquircleDefaults.ExtensionMax].
     * @param squircleEnabled When `false` the outline is a plain rounded rectangle, matching the
     *   fallback the shader-backed modifiers use. Forward [isSquircleEnabled].
     * @param capsuleCircleBlend Whether a corner at the capsule radius blends into a circular arc
     *   (`true`) or keeps the continuous-curvature superellipse (`false`).
     * @param weight The Lp exponent of the corner curve; see [SquircleDefaults.WeightLarge].
     */
    constructor(
        cornerRadius: Dp,
        extension: Float = SquircleDefaults.Extension,
        squircleEnabled: Boolean = true,
        capsuleCircleBlend: Boolean = true,
        weight: Float = SquircleDefaults.Weight,
    ) : this(
        topStart = CornerSize(cornerRadius),
        topEnd = CornerSize(cornerRadius),
        bottomEnd = CornerSize(cornerRadius),
        bottomStart = CornerSize(cornerRadius),
        extension = extension,
        squircleEnabled = squircleEnabled,
        capsuleCircleBlend = capsuleCircleBlend,
        weight = weight,
    )

    /** Binary-compatibility bridge for the constructor from before `weight` existed. */
    @Deprecated(
        message = "Migrated to the constructor that adds weight.",
        level = DeprecationLevel.HIDDEN,
    )
    constructor(
        cornerRadius: Dp,
        extension: Float = SquircleDefaults.Extension,
        squircleEnabled: Boolean = true,
        capsuleCircleBlend: Boolean = true,
    ) : this(
        cornerRadius = cornerRadius,
        extension = extension,
        squircleEnabled = squircleEnabled,
        capsuleCircleBlend = capsuleCircleBlend,
        weight = SquircleDefaults.Weight,
    )

    /**
     * A smooth-corner shape with one radius per corner, ordered like [RoundedCornerShape]. Pass
     * `0.dp` for a square corner — a sheet that only rounds its top edge, for one.
     *
     * @param topStart The radius of the top-start corner (flipped by the layout direction).
     * @param topEnd The radius of the top-end corner.
     * @param bottomEnd The radius of the bottom-end corner.
     * @param bottomStart The radius of the bottom-start corner.
     * @param extension The corner-tile size as a multiple of each radius, clamped to
     *   [SquircleDefaults.ExtensionMin]..[SquircleDefaults.ExtensionMax].
     * @param squircleEnabled When `false` the outline is a plain rounded rectangle. Forward
     *   [isSquircleEnabled].
     * @param capsuleCircleBlend Whether a corner at the capsule radius blends into a circular arc
     *   (`true`) or keeps the continuous-curvature superellipse (`false`).
     * @param weight The Lp exponent of the corner curve; see [SquircleDefaults.WeightLarge].
     */
    constructor(
        topStart: Dp,
        topEnd: Dp,
        bottomEnd: Dp,
        bottomStart: Dp,
        extension: Float = SquircleDefaults.Extension,
        squircleEnabled: Boolean = true,
        capsuleCircleBlend: Boolean = true,
        weight: Float = SquircleDefaults.Weight,
    ) : this(
        topStart = CornerSize(topStart),
        topEnd = CornerSize(topEnd),
        bottomEnd = CornerSize(bottomEnd),
        bottomStart = CornerSize(bottomStart),
        extension = extension,
        squircleEnabled = squircleEnabled,
        capsuleCircleBlend = capsuleCircleBlend,
        weight = weight,
    )

    /** Binary-compatibility bridge for the constructor from before `weight` existed. */
    @Deprecated(
        message = "Migrated to the constructor that adds weight.",
        level = DeprecationLevel.HIDDEN,
    )
    constructor(
        topStart: Dp,
        topEnd: Dp,
        bottomEnd: Dp,
        bottomStart: Dp,
        extension: Float = SquircleDefaults.Extension,
        squircleEnabled: Boolean = true,
        capsuleCircleBlend: Boolean = true,
    ) : this(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        extension = extension,
        squircleEnabled = squircleEnabled,
        capsuleCircleBlend = capsuleCircleBlend,
        weight = SquircleDefaults.Weight,
    )

    /** Draws the smooth-corner outline from the px radii [CornerBasedShape] already resolved. */
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val ltr = layoutDirection == LayoutDirection.Ltr
        val topLeft = if (ltr) topStart else topEnd
        val topRight = if (ltr) topEnd else topStart
        val bottomRight = if (ltr) bottomEnd else bottomStart
        val bottomLeft = if (ltr) bottomStart else bottomEnd
        // The disabled silhouette is a plain rounded rectangle, and it is handed back as one: a
        // consumer can then tell the two corner families apart from the outline alone — the rim
        // highlight does exactly that, tracing an Lp superellipse for a [Outline.Generic] smooth
        // corner and a circular arc for a [Outline.Rounded] one.
        if (!squircleEnabled) {
            val halfMin = size.minDimension * 0.5f
            return Outline.Rounded(
                RoundRect(
                    rect = size.toRect(),
                    topLeft = CornerRadius(topLeft.coerceAtMost(halfMin)),
                    topRight = CornerRadius(topRight.coerceAtMost(halfMin)),
                    bottomRight = CornerRadius(bottomRight.coerceAtMost(halfMin)),
                    bottomLeft = CornerRadius(bottomLeft.coerceAtMost(halfMin)),
                ),
            )
        }
        val path = Path()
        path.addSquircleRoundRect(
            rect = size.toRect(),
            topLeft = topLeft,
            topRight = topRight,
            bottomRight = bottomRight,
            bottomLeft = bottomLeft,
            extension = extension,
            squircleEnabled = true,
            capsuleCircleBlend = capsuleCircleBlend,
            weight = weight,
        )
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = SquircleShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
        extension = extension,
        squircleEnabled = squircleEnabled,
        capsuleCircleBlend = capsuleCircleBlend,
        weight = weight,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return topStart == other.topStart &&
            topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd &&
            bottomStart == other.bottomStart &&
            extension == other.extension &&
            squircleEnabled == other.squircleEnabled &&
            capsuleCircleBlend == other.capsuleCircleBlend &&
            weight == other.weight
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + extension.hashCode()
        result = 31 * result + squircleEnabled.hashCode()
        result = 31 * result + capsuleCircleBlend.hashCode()
        result = 31 * result + weight.hashCode()
        return result
    }

    override fun toString(): String = "SquircleShape(topStart=$topStart, topEnd=$topEnd, bottomEnd=$bottomEnd, " +
        "bottomStart=$bottomStart, extension=$extension, squircleEnabled=$squircleEnabled, " +
        "capsuleCircleBlend=$capsuleCircleBlend, weight=$weight)"
}
