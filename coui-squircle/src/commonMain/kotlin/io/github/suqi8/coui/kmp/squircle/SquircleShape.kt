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
 * `shadow`, `border`, `clip` — cannot reach them. This wraps [Path.addSquircleRect] in a shape so
 * those call sites get the same corner geometry instead of falling back to a plain rounded
 * rectangle.
 *
 * It extends [CornerBasedShape] rather than implementing [Shape] directly so the corner radius stays
 * readable *from* the shape. Consumers that know the silhouette only through this object have to
 * read the radius off it; the glass rim highlight, for one, builds its rounded-rect SDF from
 * `CornerBasedShape.topStart` and otherwise falls back to `min(width, height) / 2`, which turns the
 * rim into a stadium that no longer follows the surface. The four corner sizes are always equal —
 * [SquircleShape] carries a single radius.
 *
 * @param cornerRadius The corner radius in dp, applied to all four corners.
 * @param extension The corner-tile size as a multiple of [cornerRadius], clamped to
 *   [SquircleDefaults.ExtensionMin]..[SquircleDefaults.ExtensionMax].
 * @param squircleEnabled When `false` the outline is a plain rounded rectangle, matching the
 *   fallback the shader-backed modifiers use. Forward [isSquircleEnabled].
 */
@Immutable
class SquircleShape(
    cornerRadius: Dp,
    private val extension: Float = SquircleDefaults.Extension,
    private val squircleEnabled: Boolean = true,
) : CornerBasedShape(
    topStart = CornerSize(cornerRadius),
    topEnd = CornerSize(cornerRadius),
    bottomEnd = CornerSize(cornerRadius),
    bottomStart = CornerSize(cornerRadius),
) {

    /**
     * Draws the smooth-corner outline from the px radii [CornerBasedShape] already resolved.
     *
     * Every [SquircleShape] the public constructor builds has four equal corners, and [copy] hands
     * back a [RoundedCornerShape] rather than a squircle when they are asked to differ, so `topStart`
     * is the radius for all four.
     */
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        // The disabled silhouette is a plain rounded rectangle, and it is handed back as one: a
        // consumer can then tell the two corner families apart from the outline alone — the rim
        // highlight does exactly that, tracing an Lp superellipse for a [Outline.Generic] smooth
        // corner and a circular arc for a [Outline.Rounded] one.
        if (!squircleEnabled) {
            val radius = topStart.coerceAtMost(size.minDimension * 0.5f)
            return Outline.Rounded(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = CornerRadius(radius, radius),
                ),
            )
        }
        val path = Path()
        path.addSquircleRect(
            width = size.width,
            height = size.height,
            cornerRadius = topStart,
            extension = extension,
            squircleEnabled = true,
        )
        return Outline.Generic(path)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = if (topStart == this.topStart && topEnd == this.topEnd &&
        bottomEnd == this.bottomEnd && bottomStart == this.bottomStart
    ) {
        this
    } else {
        // A squircle is one uniform smooth corner, so a per-corner request is not representable
        // as one; hand back the plain rounded rectangle Compose would have produced instead of
        // silently flattening the request.
        RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return topStart == other.topStart &&
            extension == other.extension &&
            squircleEnabled == other.squircleEnabled
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + extension.hashCode()
        result = 31 * result + squircleEnabled.hashCode()
        return result
    }

    override fun toString(): String = "SquircleShape(cornerRadius=$topStart, extension=$extension, squircleEnabled=$squircleEnabled)"
}
