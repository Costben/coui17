// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.TopAppBarSearchBar
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.couiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.rememberCouiBackdrop
import io.github.suqi8.coui.kmp.kit.effect.CouiMetalShaderWarmup
import io.github.suqi8.coui.kmp.kit.effect.drawCouiGradientBackdrop
import io.github.suqi8.coui.kmp.kit.effect.isKitSearchBarSupported
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SkikoKitSurfaceReproTest {

    /**
     * Repro 5: CouiGradientBlur (`passes[1]`) must update when CouiBackdrop content changes
     * at fixed size (e.g. scrolling).
     */
    @Test
    fun couiGradientBackdropTracksContentUpdateAtFixedSize() = runSkikoComposeUiTest(
        size = Size(240f, 160f),
        density = Density(1f),
    ) {
        var useRed by mutableStateOf(false)
        var backdropRef: io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop? = null
        setContent {
            val backdrop = rememberCouiBackdrop()
            backdropRef = backdrop
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().couiBackdrop(backdrop)) {
                    Box(Modifier.fillMaxSize().graphicsLayer { }) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(if (useRed) Color.Red else Color.Blue)
                        }
                    }
                }
                // Green cover proves gradient blur renders from backdrop
                Box(Modifier.fillMaxWidth().height(80.dp).background(Color.Green))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .drawCouiGradientBackdrop(
                            backdrop = backdrop,
                            fadeHeightPx = 80f,
                            referenceBlurRadiusPx = 20f,
                        ),
                )
            }
        }
        waitForIdle()
        val pm1 = onRoot().captureToImage().toPixelMap()
        val p1 = pm1[120, 20]
        val pBottom = pm1[120, 75]
        val pBelow = pm1[120, 120]
        assertTrue(
            p1.blue > 0.8f && p1.red < 0.2f,
            "initial gradient blur should be blue, got p(120,20)=$p1 p(120,75)=$pBottom p(120,120)=$pBelow hasContent=${backdropRef?.hasContent} diag=${backdropRef?.diagnosticState()}",
        )

        useRed = true
        waitForIdle()
        val p2 = onRoot().captureToImage().toPixelMap()[120, 20]
        assertTrue(p2.red > 0.8f && p2.blue < 0.2f, "updated gradient blur must turn red, got $p2")
    }

    /**
     * Repro 6: HighlightedIconButton (backed by TopAppBarButton + CouiFloatingSurfaces +
     * CouiBackdrop(LayerBackdrop)) must:
     * 1. Render its glass disc + icon cleanly without edge darkening on Skiko.
     * 2. Track underlying backdrop content changes when the page behind it updates.
     */
    @Test
    fun highlightedIconButtonTracksBackdropUpdateAndHasCleanGlass() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        var useWarm by mutableStateOf(false)
        setContent {
            COUITheme {
                val backdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                        Box(Modifier.fillMaxSize().graphicsLayer { }) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawRect(if (useWarm) Color(0xFFFF6600) else Color(0xFF0066FF))
                            }
                        }
                    }
                    CompositionLocalProvider(LocalBackdrop provides backdrop) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            HighlightedIconButton(
                                onClick = {},
                                backdrop = backdrop,
                            ) {
                                Box(Modifier.size(8.dp).background(Color.Black))
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        // Sample inside the 40dp glass button ring (center is 100,100; icon is 8x8 -> sample at 100,88)
        // and outside the button (10,10) to prove the glass material itself rendered (lighter/frosted
        // than the raw blue/orange backdrop, NOT clipped to 0x0).
        val coolPixels = onRoot().captureToImage().toPixelMap()
        val coolBg = coolPixels[10, 10]
        val coolIcon = coolPixels[100, 100]
        val coolGlass = coolPixels[100, 88]
        assertTrue(
            coolIcon.red < 0.15f && coolIcon.green < 0.15f && coolIcon.blue < 0.15f,
            "icon at center must be visible black, got $coolIcon",
        )
        assertTrue(
            coolGlass.red > coolBg.red + 0.15f && coolGlass.blue > coolGlass.red,
            "cool glass must actually render frosted light surface over raw blue bg: glass=$coolGlass bg=$coolBg",
        )

        useWarm = true
        waitForIdle()
        val warmPixels = onRoot().captureToImage().toPixelMap()
        val warmBg = warmPixels[10, 10]
        val warmGlass = warmPixels[100, 88]
        assertTrue(
            warmGlass.blue > warmBg.blue + 0.15f && warmGlass.red > warmGlass.blue,
            "warm glass must update and render frosted light surface over raw orange bg: glass=$warmGlass bg=$warmBg",
        )
    }

    /**
     * Repro 7: HighlightedIconButton on Frame 1 (manual clock, single frame) must NOT drop its
     * icon/content when boundsSize is initially IntSize.Zero before onSizeChanged settles.
     */
    @Test
    fun highlightedIconButtonDoesNotDropIconOnFirstFrame() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        mainClock.autoAdvance = false
        setContent {
            COUITheme {
                val backdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.White))
                    CompositionLocalProvider(LocalBackdrop provides backdrop) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            HighlightedIconButton(
                                onClick = {},
                                backdrop = backdrop,
                            ) {
                                Box(Modifier.size(12.dp).background(Color.Red))
                            }
                        }
                    }
                }
            }
        }
        // Single initial frame before extra recomposition frames settle
        mainClock.advanceTimeByFrame()
        val frame1Pixels = onRoot().captureToImage().toPixelMap()
        val center = frame1Pixels[100, 100]
        assertTrue(
            center.red > 0.7f && center.green < 0.25f && center.blue < 0.25f,
            "frame 1 must already draw the red icon instead of disappearing for 1 frame, got $center (${center.toArgb().toUInt().toString(16)})",
        )
    }

    /**
     * Repro 8: HighlightedIconButton placed near the top edge over a split bright-white / red
     * backdrop must blur across the split inside the glass disc without dark/transparent rim artifacts.
     */
    @Test
    fun highlightedIconButtonBlursSplitBackdropCleanlyWithoutDarkRim() = runSkikoComposeUiTest(
        size = Size(200f, 120f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                val backdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(Color.White, size = Size(size.width / 2f, size.height))
                            drawRect(
                                Color(0xFFFF3333),
                                topLeft = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                                size = Size(size.width / 2f, size.height),
                            )
                        }
                    }
                    CompositionLocalProvider(LocalBackdrop provides backdrop) {
                        Box(
                            Modifier.fillMaxSize().padding(top = 4.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            HighlightedIconButton(
                                onClick = {},
                                backdrop = backdrop,
                            ) {}
                        }
                    }
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        // Button is 40x40 centered horizontally at x=100, top at y=4 (y=4..44)
        // Check interior left (x=90, y=24), center (x=100, y=24), right (x=110, y=24), top-inner (x=100, y=10)
        val left = pixels[90, 24]
        val center = pixels[100, 24]
        val right = pixels[110, 24]
        val topInner = pixels[100, 10]
        // None of these glass pixels over white/red may go dark (red channel must remain bright)
        for ((label, c) in listOf("left" to left, "center" to center, "right" to right, "topInner" to topInner)) {
            assertTrue(
                c.red > 0.7f,
                "glass disc pixel $label over white/red must stay bright (no dark blur edge bleed), got $c",
            )
        }
        // Blur must blend the red side into the white side (left pixel inside disc picks up warm tint)
        assertTrue(
            left.red - left.blue > 0.02f && right.blue > 0.25f,
            "blur inside glass disc must mix across the white/red boundary: left=$left center=$center right=$right",
        )
    }

    /**
     * Repro 9: TopAppBarSearchBar must take the animated kit metaball/floating-surface path on
     * Skiko (iOS/macOS) and expand/collapse cleanly without throwing or producing blank frames.
     */
    @Test
    fun topAppBarSearchBarExpandsAndCollapsesOnSkiko() = runSkikoComposeUiTest(
        size = Size(360f, 120f),
        density = Density(1f),
    ) {
        assertTrue(isKitSearchBarSupported(), "Skiko must enable animated kit TopAppBarSearchBar")
        var expanded by mutableStateOf(false)
        var query by mutableStateOf("")
        setContent {
            COUITheme {
                val backdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color(0xFF4A90E2)))
                    CompositionLocalProvider(LocalBackdrop provides backdrop) {
                        TopAppBarSearchBar(
                            query = query,
                            onQueryChange = { query = it },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            title = "首页",
                            placeholder = "搜索",
                            gradientColor = Color.Transparent,
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        )
                    }
                }
            }
        }
        waitForIdle()
        val collapsedImg = onRoot().captureToImage().toPixelMap()
        // Collapsed search button sits on the right side; center of the bar is still raw blue backdrop.
        assertTrue(collapsedImg[180, 22].blue > 0.7f, "collapsed center should show blue backdrop")

        expanded = true
        repeat(4) { mainClock.advanceTimeByFrame() }
        waitForIdle()
        val expandedImg = onRoot().captureToImage().toPixelMap()
        // Expanded glass search capsule covers the center of the toolbar with frosted light surface.
        assertTrue(
            expandedImg[150, 22].red > collapsedImg[150, 22].red + 0.1f,
            "expanded search capsule must render frosted glass across center: expanded=${expandedImg[150, 22]} collapsed=${collapsedImg[150, 22]}",
        )

        expanded = false
        repeat(4) { mainClock.advanceTimeByFrame() }
        waitForIdle()
    }

    /**
     * Repro 10: CouiMetalShaderWarmup runs through all staggered stages cleanly without throwing
     * or disturbing surrounding layout.
     */
    @Test
    fun couiMetalShaderWarmupCompletesCleanly() = runSkikoComposeUiTest(
        size = Size(120f, 120f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize().background(Color(0xFF224466))) {
                    CouiMetalShaderWarmup(forceRunForTest = true)
                }
            }
        }
        repeat(15) {
            mainClock.advanceTimeByFrame()
        }
        waitForIdle()
        val px = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(px.blue > px.red && abs(px.alpha - 1f) < 0.01f, "center pixel untouched by warmup: $px")
    }
}
