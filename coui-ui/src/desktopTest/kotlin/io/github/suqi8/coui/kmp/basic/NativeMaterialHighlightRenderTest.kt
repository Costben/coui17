// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/** Differential checks against libhwui's blendEdge, not a white-over-surface approximation. */
@OptIn(ExperimentalTestApi::class)
class NativeMaterialHighlightRenderTest {
    @Test
    fun bottomRimUsesNativeMaterialBrightnessInsteadOfWhite() = runSkikoComposeUiTest(size = Size(640f, 480f), density = Density(4f)) {
        val body = 32f / 255f
        setContent {
            COUITheme(colors = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    MetaBallSurface(
                        particles = listOf(MetaBallParticle(80.dp, 60.dp, 100.dp, 40.dp)),
                        modifier = Modifier.fillMaxSize(),
                        backdrop = null,
                        color = Color(body, body, body),
                        rimColor = Color.White.copy(alpha = 0.8f),
                        rimBottomScale = 1f,
                    )
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        // Straight upper edge: far ramp alpha=0.8, d=-1.5px; no angular fade or AA.
        val expected = nativeWhiteEdge(body, body, 0.8f)
        val actual = pixels[320, 161].red
        println("[native-balance] bottom-top=$actual; native=$expected")
        assertTrue(abs(actual - expected) < 2f / 255f, "The edge must mix material-derived RGB while preserving the surface alpha")
    }

    @Test
    fun toolbarRimsRemainVisibleWithoutTurningIntoWhiteRings() = runSkikoComposeUiTest(size = Size(640f, 640f), density = Density(4f)) {
        setContent {
            COUITheme(colors = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    HighlightedIconButton(onClick = {}) {}
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val top = (241..248).maxOf { pixels[320, it].red }
        val bottom = (391..398).maxOf { pixels[320, it].red }
        println("[native-balance] toolbar-top=$top; toolbar-bottom=$bottom")
        val expected = nativeWhiteEdge(36f / 255f, 36f / 255f, 0.6f) * 0.78f
        assertTrue(top in 0.17f..0.25f, "The toolbar circle must remain visible after native RGB blending: top=$top")
        assertTrue(bottom in 0.18f..0.36f, "The lower toolbar rim must stay visible and softer")
    }

    @Test
    fun glassRimUsesActualBackdropAndPreservesTintAwayFromTheEdge() = runSkikoComposeUiTest(size = Size(1040f, 480f), density = Density(4f)) {
        val raw = listOf(0.06f, 0.45f, 0.85f)
        val tint = 32f / 255f
        setContent {
            COUITheme(colors = darkColorScheme()) {
                val backdrop = rememberLayerBackdrop()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.Black)) {
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            raw.forEachIndexed { i, value ->
                                drawRect(Color(value, value, value), topLeft = androidx.compose.ui.geometry.Offset(i * 320f, 0f), size = Size(320f, size.height))
                            }
                        }
                    }
                    MetaBallSurface(
                        particles = listOf(
                            MetaBallParticle.circle(40.dp, 60.dp, 40.dp),
                            MetaBallParticle(120.dp, 60.dp, 40.dp, 40.dp),
                            MetaBallParticle.circle(200.dp, 60.dp, 40.dp),
                        ),
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop,
                        blurRadius = 0f,
                        color = Color(tint, tint, tint),
                        containerAlpha = 0.6f,
                        rimColor = Color.White.copy(alpha = 0.8f),
                        rimBottomScale = 1f,
                    )
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        raw.forEachIndexed { i, value ->
            val x = 160 + i * 320
            val body = value * 0.4f + tint * 0.6f
            // EDGE_STYLE_0 far fade on a 40dp circle at density 4: perimeter=320px,
            // mapper's far band=190..488px, and the upper midpoint distance=320px.
            val fade = (168f / 298f).let { it * it * (3f - 2f * it) }
            val expected = nativeWhiteEdge(value, body, 0.8f * fade)
            val top = pixels[x, 161].red
            println("[native-balance] backdrop=$value; glass-top=$top; native=$expected")
            assertTrue(abs(top - expected) < 12f / 255f, "The rim must sample the real backdrop, including before the material tint")
            assertTrue(abs(pixels[x, 240].red - body) < 3f / 255f, "The edge must not alter the interior tint")
        }
        System.getenv("COUI_RENDER_ARTIFACT_DIR")?.let { directory ->
            val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) image.setRGB(x, y, pixels[x, y].toArgb())
            ImageIO.write(image, "png", File(directory, "native-highlight-backdrops.png"))
        }
    }
}

private fun nativeWhiteEdge(raw: Float, body: Float, alpha: Float): Float {
    val gray = (raw * 1.2f).coerceIn(0f, 1f)
    val improved = (raw + (1f - gray).pow(4) * 0.1f).coerceIn(0f, 1f)
    return body * (1f - alpha) + improved * 2f * alpha
}
