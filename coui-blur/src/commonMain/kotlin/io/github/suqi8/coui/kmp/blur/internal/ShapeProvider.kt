// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur.internal

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Caches [Shape] outlines. [shapeBlock] is mutable so a single provider survives modifier
 * updates; the outline cache is keyed on the resolved [Shape] instance.
 *
 * A shape whose geometry moves must therefore return a value that changes with it — the cache
 * compares with `equals`, so re-emitting an equal value reuses the outline while a new one
 * invalidates it. Handing back one frozen instance that reads the animation inside
 * [Shape.createOutline] pins the first outline forever.
 */
@Stable
internal class ShapeProvider(var shapeBlock: () -> Shape) {

    private var cachedShape: Shape? = null
    private var cachedOutline: Outline? = null
    private var cachedSize: Size = Size.Unspecified
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedDensity: Float? = null

    val innerShape: Shape
        get() = shapeBlock()

    val shape = object : Shape {

        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            val shape = shapeBlock()
            if (cachedShape != shape) {
                cachedShape = shape
                cachedOutline = null
            }
            if (cachedOutline == null || cachedSize != size || cachedLayoutDirection != layoutDirection || cachedDensity != density.density) {
                cachedSize = size
                cachedLayoutDirection = layoutDirection
                cachedDensity = density.density
                cachedOutline = shape.createOutline(size, layoutDirection, density)
            }
            return cachedOutline!!
        }
    }
}
