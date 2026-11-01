// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.squircle

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The corner weight is COUI's Lp exponent `n`: ColorOS 17 draws `weight_16_1_normal` surfaces
 * (n = 2.5) and `weight_16_1_large` ones (n = 3.0) from the same `sdRoundRect` SDF. These pin the
 * two things that have to agree for a surface to render at the weight it asks for — the path's Bézier
 * handle and the baked field the shader binds.
 */
class SquircleWeightTest {

    private val radius = 108.5f

    /**
     * The diagonal point of the single cubic sits `(1 + 3·(1 - c)) / 8` of the tile from the corner,
     * and the Lp superellipse puts its own at `1 - 2^(-1/n)`; the two documented weights therefore
     * need `c = 0.6877` and `c = 0.7832`, i.e. a 45° inset of `0.2421R` and `0.2063R`.
     */
    @Test
    fun handleRatioFollowsTheLpFitAtBothWeights() {
        assertEquals(0.6877f, squircleControl(SquircleDefaults.WeightNormal), 1e-6f)
        assertEquals(0.7832f, squircleControl(SquircleDefaults.WeightLarge), 1e-6f)

        // The two constants are the formula rounded to four places, not a fit of their own.
        for (exponent in listOf(2.5f, 3f)) {
            val formula = 1f - (8f * (1f - 2f.pow(-1f / exponent)) - 1f) / 3f
            assertEquals(formula, squircleControl(exponent), 1e-4f, "weight $exponent")
        }

        // Other weights evaluate the same formula instead of snapping to a bake.
        val four = 1f - (8f * (1f - 2f.pow(-1f / 4f)) - 1f) / 3f
        assertEquals(four, squircleControl(4f), 1e-6f)
        // The hwui SDF clamps the exponent at 2 (`max(n, 2.0)`); so does the path.
        assertEquals(squircleControl(2f), squircleControl(1.2f), 1e-6f)
    }

    @Test
    fun weightThreePullsTheCornerInToTheNativeDiagonalInset() {
        assertEquals(0.2421f, diagonalInset(weight = SquircleDefaults.WeightNormal), 1e-3f)
        assertEquals(0.2063f, diagonalInset(weight = SquircleDefaults.WeightLarge), 1e-3f)
        // A quarter of a 4R square stays well below the capsule blend, so the corner is the pure Lp
        // curve at both weights.
        assertTrue(
            diagonalInset(weight = SquircleDefaults.WeightLarge) <
                diagonalInset(weight = SquircleDefaults.WeightNormal),
            "a heavier weight must hug the corner tighter",
        )
    }

    /** The default weight is the normal one, so surfaces that pass no weight keep 1.1.0's pixels. */
    @Test
    fun theDefaultWeightLeavesExistingSurfacesAlone() {
        assertEquals(SquircleDefaults.WeightNormal, SquircleDefaults.Weight)
        assertEquals(0.6877f, squircleControl(SquircleDefaults.Weight), 1e-6f)

        val default = diagonalInset()
        val explicit = diagonalInset(weight = SquircleDefaults.WeightNormal)
        assertEquals(explicit, default, 1e-6f)
    }

    /** The per-corner builder the panels use carries the weight too. */
    @Test
    fun perCornerPathCarriesTheWeight() {
        val rect = Rect(0f, 0f, 4 * radius, 4 * radius)
        val path = Path().apply {
            addSquircleRoundRect(
                rect = rect,
                topLeft = radius,
                topRight = radius,
                bottomRight = radius,
                bottomLeft = radius,
                weight = SquircleDefaults.WeightLarge,
            )
        }
        assertEquals(0.2063f, cornerDiagonalInset(path, radius), 1e-3f)
    }

    /**
     * The shader has one baked field per documented weight and binds the nearer one for anything
     * else, so a weight never costs a second texture tap. Exactly halfway (2.75) keeps the normal
     * field.
     */
    @Test
    fun theShaderBindsTheNearestBakedField() {
        assertEquals(SquircleDefaults.WeightNormal, squircleSdfWeight(2.5f))
        assertEquals(SquircleDefaults.WeightLarge, squircleSdfWeight(3f))
        assertEquals(SquircleDefaults.WeightNormal, squircleSdfWeight(2.4f))
        assertEquals(SquircleDefaults.WeightLarge, squircleSdfWeight(2.76f))
        assertEquals(SquircleDefaults.WeightNormal, squircleSdfWeight(1f))
        assertEquals(SquircleDefaults.WeightLarge, squircleSdfWeight(10f))
        assertEquals(SquircleDefaults.WeightNormal, squircleSdfWeight(2.75f))
    }

    private fun diagonalInset(weight: Float = SquircleDefaults.Weight): Float = cornerDiagonalInset(
        Path().apply { addSquircleRect(4 * radius, 4 * radius, radius, weight = weight) },
        radius,
    )

    /**
     * How far the top-left corner sits from the corner point along the diagonal, in units of
     * [cornerRadius]: the path sample closest to `x == y` in corner-local coordinates.
     */
    private fun cornerDiagonalInset(path: Path, cornerRadius: Float): Float {
        val measure = PathMeasure()
        measure.setPath(path, false)
        var best = Float.MAX_VALUE
        var bestX = 0f
        var travelled = 0f
        while (travelled < measure.length) {
            val p = measure.getPosition(travelled)
            if (p.x <= cornerRadius && p.y <= cornerRadius) {
                val delta = abs(p.x - p.y)
                if (delta < best) {
                    best = delta
                    bestX = (p.x + p.y) / 2f
                }
            }
            travelled += 0.05f
        }
        return bestX / cornerRadius
    }
}
