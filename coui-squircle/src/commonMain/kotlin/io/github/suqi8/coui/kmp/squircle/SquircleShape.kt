// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.squircle

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
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
 * @param cornerRadius The corner radius in dp, applied to all four corners.
 * @param extension The corner-tile size as a multiple of [cornerRadius], clamped to
 *   [SquircleDefaults.ExtensionMin]..[SquircleDefaults.ExtensionMax].
 * @param squircleEnabled When `false` the outline is a plain rounded rectangle, matching the
 *   fallback the shader-backed modifiers use. Forward [isSquircleEnabled].
 */
@Immutable
class SquircleShape(
    private val cornerRadius: Dp,
    private val extension: Float = SquircleDefaults.Extension,
    private val squircleEnabled: Boolean = true,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        with(density) {
            path.addSquircleRect(
                width = size.width,
                height = size.height,
                cornerRadius = cornerRadius.toPx(),
                extension = extension,
                squircleEnabled = squircleEnabled,
            )
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return cornerRadius == other.cornerRadius &&
            extension == other.extension &&
            squircleEnabled == other.squircleEnabled
    }

    override fun hashCode(): Int {
        var result = cornerRadius.hashCode()
        result = 31 * result + extension.hashCode()
        result = 31 * result + squircleEnabled.hashCode()
        return result
    }

    override fun toString(): String = "SquircleShape(cornerRadius=$cornerRadius, extension=$extension, squircleEnabled=$squircleEnabled)"
}
