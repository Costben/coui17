// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The dark capsule has to read like the system's: a translucent material inset that is *not* darker
 * than the page around it, and a bright hairline along its straight edges.
 *
 * `COUISearchBar` strokes the capsule with the view-level `COUIMaterialStrokeEffect`
 * (`TYPE_FRAMEWORK_CAPSULE_8`), which composites the line colour itself. The metaball blend the
 * bottom bars use instead maps the line to `2 * improvedColor`, so a dark fill tops out at roughly
 * twice its own luminance: sampled on the reference device the fused search capsule's edge measured
 * 61 over a 57 body, where the system draws 78 over 39. That dim band around the outline is what
 * read as a darkened ring, and it disappeared once the search path asked for the plain line.
 *
 * Geometry at 3.875: a 328x140dp scene puts the 296x40dp capsule at x 62..1209, y 193..348.
 */
@OptIn(ExperimentalTestApi::class)
class SearchBarDarkEdgeTest {

    private val density = 3.875f
    private val pageWidth = 328f
    private val pageHeight = 140f
    private val capsuleTop = 50f
    private val capsuleHeight = 40f
    private val capsuleInset = 16f

    private fun render(expanded: Boolean, dark: Boolean = true): PixelMap {
        var pixels: PixelMap? = null
        runSkikoComposeUiTest(
            size = Size(pageWidth * density, pageHeight * density),
            density = Density(density),
        ) {
            setContent { Scene(expanded, dark) }
            waitForIdle()
            pixels = onRoot().captureToImage().toPixelMap()
        }
        return requireNotNull(pixels)
    }

    @Composable
    private fun Scene(expanded: Boolean, dark: Boolean) {
        val scheme = if (dark) darkColorScheme() else lightColorScheme()
        COUITheme(colors = scheme) {
            Box(Modifier.fillMaxSize().background(if (dark) scheme.background else Color(0xFFF0F1F2))) {
                SearchBar(
                    query = "",
                    onQueryChange = {},
                    onSearch = {},
                    onExpandedChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    expanded = expanded,
                    label = "Search settings",
                    // 50dp of page above and below the 40dp capsule.
                    insideMargin = DpSize(capsuleInset.dp, capsuleTop.dp),
                    cancelText = "Cancel",
                )
            }
        }
    }

    /** 8-bit grey of a sample; every colour in these scenes is neutral. */
    private fun PixelMap.grey(x: Float, y: Float): Int = (this[x.toInt(), y.toInt()].red * 255f).toInt()

    private fun px(dp: Float) = dp * density

    /**
     * Walks a 0..12dp band outside the capsule on the four sides and the four diagonals and fails
     * when any sample is darker than the page. The page is sampled 20dp away from the outline.
     */
    private fun assertNoDarkHalo(map: PixelMap, expanded: Boolean) {
        val left = px(capsuleInset)
        val right = map.width - px(capsuleInset)
        val top = px(capsuleTop)
        val bottom = px(capsuleTop + capsuleHeight)
        val midX = (left + right) / 2
        val midY = (top + bottom) / 2
        val page = map.grey(midX, px(capsuleTop - 20))
        val reach = (12 * density).toInt()

        val samples = buildList {
            for (i in 0..reach) {
                add("top+$i" to map.grey(midX, top - i))
                add("bottom+$i" to map.grey(midX, bottom + i))
                add("left+$i" to map.grey(left - i, midY))
                add("right+$i" to map.grey(right + i, midY))
            }
        }
        val darkest = samples.minBy { it.second }
        println(
            "[search-dark-edge] expanded=$expanded page=$page darkest=${darkest.first}:${darkest.second}",
        )
        // One 8-bit level of tolerance covers rounding; the device's own page dithers by more than
        // that right next to its search capsule.
        assertTrue(
            darkest.second >= page - 1,
            "the band around the capsule must not be darker than the page: ${darkest.first} read " +
                "${darkest.second} against a page of $page",
        )
    }

    /** Peak luminance of the outermost band of the capsule, i.e. the rim the system draws. */
    private fun rimPeak(map: PixelMap, edgeY: Float, inward: Boolean): Int {
        val step = if (inward) 1 else -1
        var peak = 0
        for (i in 0 until (2 * density).toInt()) {
            peak = maxOf(peak, map.grey((map.width / 2).toFloat(), edgeY + step * i))
        }
        return peak
    }

    @Test
    fun theCollapsedDarkCapsuleDoesNotDarkenThePage() {
        assertNoDarkHalo(render(expanded = false), expanded = false)
    }

    @Test
    fun theExpandedDarkCapsuleDoesNotDarkenThePage() {
        assertNoDarkHalo(render(expanded = true), expanded = true)
    }

    /**
     * The system's `FRAMEWORK_CAPSULE_PARAMS_8_DARK` line (alpha 0.15, 1.8dp) over the body reads
     * 78 against a 39 body on the device. The metaball blend could not get there — it stops at
     * `2 * improvedColor` — so the fused path must reach the plain line's brightness.
     */
    @Test
    fun theFusedRimReachesTheStrokeLineBrightness() {
        val map = render(expanded = true)
        val body = map.grey((map.width / 2).toFloat(), px(capsuleTop + 4))
        val rim = rimPeak(map, px(capsuleTop), inward = true)
        println("[search-dark-edge] fused rim=$rim body=$body")
        assertTrue(rim >= body + 18, "the fused rim must light the edge above the body: rim=$rim body=$body")
    }

    @Test
    fun theCollapsedFusedRimReachesTheStrokeLineBrightness() {
        val map = render(expanded = false)
        val body = map.grey((map.width / 2).toFloat(), px(capsuleTop + 4))
        val rim = rimPeak(map, px(capsuleTop), inward = true)
        println("[search-dark-edge] collapsed fused rim=$rim body=$body")
        assertTrue(rim >= body + 18, "the collapsed rim must light the edge above the body: rim=$rim body=$body")
    }

    /**
     * The light preset's `COLOR_DODGE` + `LUMINOSITY` pair resolves to a near-white body (245 on the
     * device) with `FRAMEWORK_CAPSULE_PARAMS_8_LIGHT`'s 0.8-alpha line on top (254). The line is its
     * own layer, so it has to stay visible over a body that is only 40% opaque — otherwise the
     * capsule loses its edge in the light theme.
     */
    @Test
    fun theLightFusedRimStaysVisibleOverANearWhiteBody() {
        val map = render(expanded = true, dark = false)
        val body = map.grey((map.width / 2).toFloat(), px(capsuleTop + 4))
        val rim = rimPeak(map, px(capsuleTop), inward = true)
        println("[search-dark-edge] light rim=$rim body=$body")
        assertTrue(body >= 240, "the light body must be the near-white material, was $body")
        assertTrue(rim >= body + 4, "the light rim must stay visible: rim=$rim body=$body")
    }

    /**
     * The dark fill is `FRAMEWORK_TOP_BAR_PARAMS_DARK`'s `SrcOver` layer (`#CC262626`), not the
     * `coui_search_bar_no_blur_solid` grey: over the `#1E1E1E` page the material composites to 36,
     * while the old `#28767676` solid read 44.
     */
    @Test
    fun theDarkFillIsTheMaterialTintOverThePage() {
        val map = render(expanded = true)
        val page = map.grey((map.width / 2).toFloat(), px(capsuleTop - 20))
        val body = map.grey((map.width / 2).toFloat(), px(capsuleTop + 4))
        // `SrcOver` runs in 8-bit sRGB: 0xCC over the page, i.e. 80% of 0x26 plus 20% of the page.
        val expected = (0.8f * 0x26 + 0.2f * page).toInt()
        println("[search-dark-edge] fill=$body page=$page expected=$expected")
        assertTrue(
            kotlin.math.abs(body - expected) <= 2,
            "the fill must be #CC262626 over the page: body=$body expected=$expected",
        )
    }
}
