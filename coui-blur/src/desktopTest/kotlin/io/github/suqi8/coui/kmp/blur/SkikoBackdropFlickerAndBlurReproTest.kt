// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(InternalCouiBlurApi::class)

package io.github.suqi8.coui.kmp.blur

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SkikoBackdropFlickerAndBlurReproTest {

    /**
     * Repro 1: Content inside a child graphicsLayer changes (e.g. scrolling LazyColumn /
     * HorizontalPager / animated child) while sibling textureBlur does NOT recompose.
     * On Skiko, the blurred surface must reflect the updated content instead of staying frozen
     * on the old frame.
     */
    @Test
    fun textureBlurTracksChildGraphicsLayerColorChangeWithoutConsumerRecomposition() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        var useRed by mutableStateOf(false)
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    // Isolated child OwnedLayer, just like LazyColumn / HorizontalPager / AnimatedContent.
                    Box(Modifier.fillMaxSize().graphicsLayer { }) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(if (useRed) Color.Red else Color.Blue)
                        }
                    }
                }
                // Opaque green cover between backdrop and blur so we prove pixels come from backdrop.
                Box(Modifier.fillMaxSize().background(Color.Green))
                Box(
                    Modifier
                        .size(120.dp)
                        .textureBlur(backdrop, RectangleShape, blurRadius = 20f),
                )
            }
        }
        waitForIdle()

        val blueFrame = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(
            blueFrame.blue > 0.8f && blueFrame.red < 0.2f,
            "initial blur frame should be blue, got $blueFrame (${blueFrame.toArgb().toUInt().toString(16)})",
        )

        useRed = true
        waitForIdle()

        val redFrame = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(
            redFrame.red > 0.8f && redFrame.blue < 0.2f,
            "updated blur frame must track child graphicsLayer change to red, got $redFrame (${redFrame.toArgb().toUInt().toString(16)})",
        )
    }

    /**
     * Repro 2: Same as Repro 1, without child graphicsLayer — plain draw-phase state change
     * inside LayerBackdrop while sibling textureBlur has its own OwnedLayer (placeWithLayer).
     */
    @Test
    fun textureBlurTracksDrawPhaseChangeInsideBackdrop() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        var useRed by mutableStateOf(false)
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(if (useRed) Color.Red else Color.Blue)
                    }
                }
                Box(Modifier.fillMaxSize().background(Color.Green))
                Box(
                    Modifier
                        .size(120.dp)
                        .textureBlur(backdrop, RectangleShape, blurRadius = 20f),
                )
            }
        }
        waitForIdle()
        val p1 = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(p1.blue > 0.8f && p1.red < 0.2f, "initial frame should be blue, got $p1")

        useRed = true
        waitForIdle()
        val p2 = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(p2.red > 0.8f && p2.blue < 0.2f, "updated frame should be red, got $p2")
    }

    /**
     * Repro 3: Toggling layerBackdrop off and back on (like bilico-iOS Shell.kt layerBackdropIf)
     * after underlying content changed must NOT flash stale content or blank/transparent pixels.
     */
    @Test
    fun togglingLayerBackdropDoesNotShowBlankOrStaleFrame() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        var recordEnabled by mutableStateOf(true)
        var useRed by mutableStateOf(false)
        var backdropRef: LayerBackdrop? = null
        setContent {
            val backdrop = rememberLayerBackdrop()
            backdropRef = backdrop
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (recordEnabled) Modifier.layerBackdrop(backdrop) else Modifier),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(if (useRed) Color.Red else Color.Blue)
                    }
                }
                Box(Modifier.fillMaxSize().background(Color.Green))
                Box(
                    Modifier
                        .size(120.dp)
                        .textureBlur(backdrop, RectangleShape, blurRadius = 15f),
                )
            }
        }
        waitForIdle()
        val p1 = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(p1.blue > 0.8f, "initial should be blue, got $p1")

        // Detach layerBackdrop, change content, then reattach
        recordEnabled = false
        useRed = true
        waitForIdle()
        assertTrue(!backdropRef!!.isReadyForSampling, "detached backdrop must not report ready for sampling")

        recordEnabled = true
        waitForIdle()
        val p2 = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(p2.red > 0.8f && p2.blue < 0.2f, "reattached backdrop should immediately show red, got $p2")
    }

    /**
     * Repro 4: progressiveTextureBlur over a solid bright color must NOT darken or corrupt
     * the top/left/right edges due to Skia ImageFilter.makeBlur(CLAMP, crop = null).
     */
    @Test
    fun progressiveTextureBlurPreservesUniformColorAtTopAndSideEdges() = runSkikoComposeUiTest(
        size = Size(240f, 200f),
        density = Density(1f),
    ) {
        val solid = Color(0xFFF5F6F8)
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(solid))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .progressiveTextureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            blurRadius = 32f,
                            gradient = ProgressiveBlur.Top.copy(curve = 1.8f),
                        ),
                )
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val probeCoords = listOf(
            0 to 0,
            1 to 1,
            120 to 0,
            120 to 1,
            239 to 0,
            0 to 40,
            239 to 40,
            120 to 40,
            120 to 85,
        )
        for ((x, y) in probeCoords) {
            val c = pixels[x, y]
            val delta = maxOf(abs(c.red - solid.red), abs(c.green - solid.green), abs(c.blue - solid.blue))
            assertTrue(
                delta < 0.04f,
                "progressiveTextureBlur edge/interior at ($x, $y) drifted from $solid to $c (delta=$delta)",
            )
        }
    }

    /**
     * Repro 5: Dialog/BottomSheet pattern — textureBlur sits inside an ancestor graphicsLayer
     * whose translationY starts off-screen (e.g. +800px at animation progress=0) and animates to 0.
     * At progress=1, the blur MUST sample the on-screen backdrop (red/blue), NOT stay frozen on the
     * off-screen transparent/edge-clamped pixels from progress=0.
     */
    @Test
    fun dialogSlideInGraphicsLayerTranslationUpdatesBackdropSample() = runSkikoComposeUiTest(
        size = Size(393f, 852f),
        density = Density(3f),
    ) {
        var offsetY by mutableFloatStateOf(1600f)
        var backdropDrawCount = 0
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color(0xFFCC2222), size = Size(size.width, size.height * 0.5f))
                        drawRect(
                            Color(0xFF2244CC),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.5f),
                            size = Size(size.width, size.height * 0.5f),
                        )
                    }
                }
                // Simulate DialogContentLayout: outer graphicsLayer translating from off-screen
                // (+1600px) to on-screen (0f), then drawBackdrop AFTER graphicsLayer.
                Box(
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(200.dp)
                        .graphicsLayer {
                            translationY = offsetY
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RectangleShape },
                            effects = { blur(200f) },
                            onDrawBehind = { backdropDrawCount++ },
                        ),
                )
            }
        }
        waitForIdle()
        val countBefore = backdropDrawCount

        // Move dialog onto screen (same as animationProgress 0f -> 1f in DialogContentLayout / BottomSheetContentLayout)
        offsetY = 0f
        waitForIdle()
        val countAfter = backdropDrawCount
        println("DIALOG_DRAW_COUNT before=$countBefore after=$countAfter")
        assertTrue(
            countAfter > countBefore,
            "DrawBackdropNode MUST redraw when ancestor graphicsLayer translationY changes! before=$countBefore after=$countAfter",
        )
    }

    /**
     * Repro 6: Large blur radius (200px, scaleFactor=16) on a centered/bottom dialog over a 2-tone
     * backdrop at density=3f without translation animation, ALSO testing horizontal/vertical variation.
     */
    @Test
    fun largeRadiusScaleFactor16BlurWorksAtDensity3() = runSkikoComposeUiTest(
        size = Size(393f, 852f),
        density = Density(3f),
    ) {
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Canvas(Modifier.fillMaxSize()) {
                        // Left half red, right half green
                        drawRect(Color(0xFFDD2222), size = Size(size.width * 0.5f, size.height))
                        drawRect(
                            Color(0xFF22CC44),
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.5f, 0f),
                            size = Size(size.width * 0.5f, size.height),
                        )
                    }
                }
                Box(
                    Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(220.dp)
                        .textureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            blurRadius = 67f,
                        ),
                )
            }
        }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        val yMid = pixels.height - 330
        val pLeft = pixels[150, yMid]
        val pCenter = pixels[pixels.width / 2, yMid]
        val pRight = pixels[pixels.width - 150, yMid]
        println("SF16_SPLIT_PIXELS left=$pLeft center=$pCenter right=$pRight")
        assertTrue(pLeft.red > pLeft.green + 0.2f, "Left should be reddish, got $pLeft")
        assertTrue(pRight.green > pRight.red + 0.2f, "Right should be greenish, got $pRight")
    }

    /**
     * Repro 7: Dynamically attaching layerBackdrop to an already-laid-out static Box (without
     * changing its size or position) must immediately publish coordinates and record so consumers
     * render blurred pixels instead of falling back to blank/solid.
     */
    @Test
    fun dynamicallyAttachingLayerBackdropToStaticBoxPublishesCoordinatesAndBlurs() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        var recordEnabled by mutableStateOf(false)
        var backdropRef: LayerBackdrop? = null
        setContent {
            val backdrop = rememberLayerBackdrop()
            backdropRef = backdrop
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (recordEnabled) Modifier.layerBackdrop(backdrop) else Modifier),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color.Red)
                    }
                }
                Box(Modifier.fillMaxSize().background(Color.Green))
                Box(
                    Modifier
                        .size(120.dp)
                        .textureBlur(backdrop, RectangleShape, blurRadius = 15f),
                )
            }
        }
        waitForIdle()
        assertTrue(!backdropRef!!.isReadyForSampling, "unattached backdrop should not be ready")

        recordEnabled = true
        waitForIdle()
        assertTrue(backdropRef!!.isReadyForSampling, "dynamically attached backdrop must report ready")
        val pixel = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(
            pixel.red > 0.8f && pixel.green < 0.2f,
            "dynamically attached backdrop must blur red source instead of staying blank, got $pixel",
        )
    }

    /**
     * Repro 8: When a DrawBackdropNode attaches before its first draw (size == Size.Unspecified),
     * snapshot reads inside `effects` must still be observed once the first draw establishes size,
     * so later State mutations (e.g. ChipTab checkedLayerAlpha or scroll veil) update the effect.
     */
    @Test
    fun drawBackdropObservesStateReadInsideEffectsAfterFirstDraw() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        val tintState = mutableStateOf(Color.Transparent)
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    drawRect(Color(0xFF404040))
                }
                Box(
                    Modifier
                        .size(120.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RectangleShape },
                            effects = {
                                val overlay = tintState.value
                                textureBlurEffect(
                                    blurRadiusX = 12f,
                                    blurRadiusY = 12f,
                                    colors = if (overlay.alpha > 0f) {
                                        BlurColors(listOf(BlendColorEntry(overlay, BlurBlendMode.SrcOver)))
                                    } else {
                                        BlurColors()
                                    },
                                )
                            },
                        ),
                )
            }
        }
        waitForIdle()
        val before = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(
            before.red < 0.45f && before.green < 0.45f && before.blue < 0.45f,
            "initial dark backdrop should stay dark, got $before",
        )

        tintState.value = Color.White.copy(alpha = 0.85f)
        waitForIdle()
        val after = onRoot().captureToImage().toPixelMap()[60, 60]
        assertTrue(
            after.red > 0.75f && after.green > 0.75f && after.blue > 0.75f,
            "state read inside effects must trigger RenderEffect update after first draw, got before=$before after=$after",
        )
    }
}
