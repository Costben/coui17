// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
@file:Suppress("ktlint:standard:property-naming")

package io.github.suqi8.coui.kmp.kit.shape

// Ported from COUI-Kit (ItosEO/COUI-Kit@8a09da4) coui/src/main/java/com/itos/coui/shape/CouiShape.kt.

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt

internal const val CouiCornerWeight = 2.5f

/** Equal-radius Lp zero-isoline for Oplus G2 corners, sharing its boundary with menu contour glow. */
@Immutable
internal data class CouiShape(val radius: Dp, val cornerWeight: Float = CouiCornerWeight) : Shape {
    init {
        require(radius.value.isFinite() && radius.value >= 0f) { "Corner radius must be finite and non-negative" }
        require(cornerWeight.isFinite() && cornerWeight >= 2f) {
            "Corner weight must be finite and >= 2 to stay consistent with contour shaders"
        }
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = (radius.value * density.density).coerceIn(0f, size.minDimension.coerceAtLeast(0f) / 2f)
        if (r == 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        val weight = cornerWeight.toDouble()
        val diagonal = 0.5.pow(1.0 / weight)
        // Sample each half-corner where slope <= 1 so chord error stays within 0.05px.
        val segments = ceil(sqrt(r * (weight - 1.0) * diagonal / (4.0 * 0.05))).toInt().coerceAtLeast(1)
        val halfCorner = DoubleArray(segments + 1) { index ->
            (1.0 - (diagonal * index / segments).pow(weight)).pow(1.0 / weight)
        }
        val path = Path()
        path.moveTo(r, 0f)
        repeat(4) { corner ->
            for (index in 0..segments * 2) {
                val firstHalf = index <= segments
                val halfIndex = if (firstHalf) index else segments * 2 - index
                val a = diagonal * halfIndex / segments
                val b = halfCorner[halfIndex]
                val x = (r * if (firstHalf) a else b).toFloat()
                val y = (r * if (firstHalf) b else a).toFloat()
                when (corner) {
                    0 -> path.lineTo(size.width - r + x, r - y)
                    1 -> path.lineTo(size.width - r + y, size.height - r + x)
                    2 -> path.lineTo(r - x, size.height - r + y)
                    else -> path.lineTo(r - y, r - x)
                }
            }
        }
        path.close()
        // shortcut: Path uses public raster anti-aliasing instead of vendor fwidth coverage; replace when GPU clip parity is available.
        return Outline.Generic(path)
    }
}

/** Resolves corner radius directly from [CouiShape] or validates the explicit radius for arbitrary [Shape]s. */
internal fun Shape.resolveCouiCornerRadius(explicitRadius: Dp): Dp {
    val radius = if (this is CouiShape) {
        require(!explicitRadius.isSpecified || explicitRadius == this.radius) {
            "Explicit radius must match CouiShape radius"
        }
        this.radius
    } else {
        explicitRadius
    }
    require(radius.value.isFinite() && radius.value >= 0f) {
        "Custom Shape requires a finite non-negative contour radius"
    }
    return radius
}
