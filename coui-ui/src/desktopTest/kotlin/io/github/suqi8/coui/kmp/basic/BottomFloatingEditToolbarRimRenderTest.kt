// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BottomFloatingEditToolbarRimRenderTest {
    @Test
    fun cornerLightStaysInTheRimInsteadOfFillingTheCornerInterior() = runSkikoComposeUiTest(
        size = Size(640f, 480f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme(colors = darkColorScheme()) {
                val backdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.Black))
                    BottomFloatingEditToolbar(backdrop = backdrop, containerColor = Color(0xFF333333)) {
                        repeat(3) { index ->
                            Box(Modifier.width(24.dp).testTag("edit-item-$index"))
                        }
                    }
                }
            }
        }
        waitForIdle()
        val first = onNodeWithTag("edit-item-0").fetchSemanticsNode().boundsInRoot
        val last = onNodeWithTag("edit-item-2").fetchSemanticsNode().boundsInRoot
        val left = first.left - 48f // Native vertical inner padding: 12dp at density 4.
        val right = last.right + 48f
        val top = first.top - 16f // 56dp menu, 48dp items: 4dp inset on each edge.
        val bottom = top + 224f
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val halfWidth = (right - left) / 2f
        val radius = 112f
        val pixels = onRoot().captureToImage().toPixelMap()
        val body = pixels[centerX.toInt(), centerY.toInt()].red
        var brightestCornerInterior = body
        var samples = 0
        for (y in top.toInt() until bottom.toInt()) {
            for (x in left.toInt() until right.toInt()) {
                val qx = abs(x + 0.5f - centerX) - halfWidth + radius
                val qy = abs(y + 0.5f - centerY)
                if (qx <= 0f || qy <= 0f) continue
                val distance = (qx.pow(2.5f) + qy.pow(2.5f)).pow(0.4f) - radius
                // Sample well inside the corner, beyond the 2dp inward halo plus AA margin.
                if (distance < -12f) {
                    brightestCornerInterior = max(brightestCornerInterior, pixels[x, y].red)
                    samples++
                }
            }
        }
        println("[edit-rim] body=$body cornerInterior=$brightestCornerInterior samples=$samples")
        assertTrue(samples > 100, "The test must sample the edit toolbar's corner interiors")
        assertTrue(
            brightestCornerInterior <= body + 3f / 255f,
            "Corner light must stay within the edge band: body=$body, corner=$brightestCornerInterior",
        )
        val topRim = (top.toInt() until top.toInt() + 8).maxOf { pixels[centerX.toInt(), it].red }
        assertTrue(topRim > body + 3f / 255f, "The fix must retain a visible upper rim")
    }
}
