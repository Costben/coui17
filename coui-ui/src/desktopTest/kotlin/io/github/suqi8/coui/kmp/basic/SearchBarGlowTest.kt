// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `COUISearchBar.setCausticShadow` paints the light on a sibling layer with the capsule punched
 * out, so the light only ever lands outside the outline. Both halves of that contract matter: a
 * missing glow loses the device's signature lift, and a glow that leaks inside washes out the
 * translucent fill.
 *
 * Geometry at density 4: a 320x80dp surface is 1280x320px, the field is 288x40dp centred, so the
 * capsule spans x 64..1216 and y 80..240.
 */
@OptIn(ExperimentalTestApi::class)
class SearchBarGlowTest {
    private val fill = Color(0xFF282828)
    private val glow = Color.White.copy(alpha = 0.2f)

    private fun render(glowColor: Color?): (Int, Int) -> Float {
        var pixels: androidx.compose.ui.graphics.PixelMap? = null
        runSkikoComposeUiTest(size = Size(1280f, 320f), density = Density(4f)) {
            setContent {
                COUITheme(colors = darkColorScheme()) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        InputField(
                            query = "",
                            onQueryChange = {},
                            onSearch = {},
                            label = "",
                            expanded = false,
                            onExpandedChange = {},
                            colors = SearchBarDefaults.searchBarColors(backgroundColor = fill),
                            stroke = null,
                            glowColor = glowColor,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }
                }
            }
            waitForIdle()
            pixels = onRoot().captureToImage().toPixelMap()
        }
        val map = requireNotNull(pixels)
        return { x, y -> map[x, y].red }
    }

    @Test
    fun theGlowFallsBelowTheCapsuleAndDecaysWithDistance() {
        val at = render(glow)
        val justBelow = at(640, 244)
        val farBelow = at(640, 300)
        println("[search-glow] justBelow=$justBelow; farBelow=$farBelow")
        assertTrue(justBelow > farBelow + 0.01f, "The light must be strongest at the capsule edge and fade outwards")
        assertTrue(farBelow >= 0f, "The light must not go negative")
    }

    @Test
    fun theGlowNeverBrightensTheCapsuleFill() {
        val at = render(glow)
        val centre = at(640, 160)
        val expected = fill.red
        println("[search-glow] centre=$centre; expected=$expected")
        assertTrue(
            kotlin.math.abs(centre - expected) < 0.005f,
            "The capsule is punched out of the glow layer, so its fill must stay $expected but read $centre",
        )
    }

    @Test
    fun disablingTheGlowLeavesTheBackgroundBlack() {
        val at = render(null)
        val below = at(640, 244)
        println("[search-glow] disabled=$below")
        assertTrue(below < 0.005f, "Without a glow the page under the capsule must stay at the background colour")
    }
}
