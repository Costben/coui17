// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.squircle.addSquircleRoundRect
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The rim highlight decides which corner curve to trace from the outline family alone — an
 * [Outline.Generic] path is the smooth ColorOS corner, anything else is a circular arc — and it
 * reads the radii off the shape. These lock both halves of that contract down, and pin the smooth
 * corner to the Lp superellipse it claims to be.
 */
class SquircleOutlineFamilyTest {

    private val smoothCorner = SquircleShape(cornerRadius = 28.dp)

    private fun outlineOf(shape: SquircleShape, size: Float = 200f) = shape.createOutline(
        androidx.compose.ui.geometry.Size(size, size),
        LayoutDirection.Ltr,
        DensityStub,
    )

    @Test
    fun enabledShapeHandsBackASmoothCornerPath() {
        assertTrue(outlineOf(smoothCorner) is Outline.Generic, "squircle outline must be a path")
    }

    @Test
    fun disabledShapeHandsBackARoundedOutlineSoTheStrokeCanFallBackToAnArc() {
        val outline = outlineOf(SquircleShape(cornerRadius = 28.dp, squircleEnabled = false))
        assertTrue(outline is Outline.Rounded, "a disabled squircle must be an arc outline")
        // The shader clamps to min(halfWidth, halfHeight); the outline has to clamp the same way.
        assertEquals(28f * DensityStub.density, outline.roundRect.topLeftCornerRadius.x, 1e-3f)
    }

    /**
     * Walks the corner the path actually emits and measures how far it sits from the Lp superellipse
     * `|x|^n + |y|^n = r^n` with `n = 2.5`, in units of the corner radius. The cubic approximates
     * that curve to under half a percent of the radius; a circular arc, the shape this replaced,
     * misses it by 7.2% — which is the 7.8px the device showed at the popup's 28dp radius.
     */
    @Test
    fun smoothCornerSitsOnTheLpSuperellipseAndAnArcDoesNot() {
        val radius = 108.5f
        val smooth = cornerDeviationFromSuperellipse(
            Path().apply { addSquircleRect(2 * radius, 2 * radius, radius) },
            radius,
        )
        val arc = cornerDeviationFromSuperellipse(
            Path().apply {
                addSquircleRoundRect(
                    rect = Rect(0f, 0f, 2 * radius, 2 * radius),
                    topLeft = radius,
                    topRight = radius,
                    bottomRight = radius,
                    bottomLeft = radius,
                    squircleEnabled = false,
                )
            },
            radius,
        )
        println("[corner] smooth path misses the Lp n=2.5 superellipse by ${smooth}px (r=$radius)")
        println("[corner] circular arc misses it by ${arc}px (r=$radius)")
        assertTrue(smooth < 0.005f * radius, "smooth corner deviates ${smooth}px, want < ${0.005f * radius}")
        assertTrue(arc > 0.07f * radius, "arc deviates ${arc}px, want > ${0.07f * radius}")
    }

    /**
     * Largest distance, in pixels, between the path's top-left corner and the superellipse, measured
     * as a radial miss: each path sample is pushed onto the superellipse along its own ray from the
     * corner centre and the two radii are compared.
     */
    private fun cornerDeviationFromSuperellipse(path: Path, radius: Float): Float {
        val measure = PathMeasure()
        measure.setPath(path, false)
        val centreX = radius
        val centreY = radius
        var worst = 0f
        var travelled = 0f
        while (travelled < measure.length) {
            val p = measure.getPosition(travelled)
            val dx = p.x - centreX
            val dy = p.y - centreY
            // Corner region only: both offsets point outwards along the two edges.
            if (dx < 0f && dy < 0f) {
                val angle = kotlin.math.atan2(-dy, -dx)
                val cosA = kotlin.math.cos(angle)
                val sinA = kotlin.math.sin(angle)
                val lp = radius / (cosA.pow(2.5f) + sinA.pow(2.5f)).pow(1f / 2.5f)
                val radial = sqrt(dx * dx + dy * dy)
                worst = maxOf(worst, abs(radial - lp))
            }
            travelled += 0.25f
        }
        return worst
    }

    private object DensityStub : androidx.compose.ui.unit.Density {
        override val density: Float = 1f
        override val fontScale: Float = 1f
    }
}
