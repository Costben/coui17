// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/** Goldens generated independently from the extracted native caustic template, not production code. */
@OptIn(ExperimentalTestApi::class)
class NativeCausticShadowRenderTest {
    @Test
    fun toolbarExteriorMatchesNativeCausticProfile() = checkProfile(ToolbarSamples) {
        HighlightedIconButton(onClick = {}) {}
    }

    @Test
    fun accessoryExteriorMatchesNativeCausticProfile() = checkProfile(AccessorySamples) {
        MetaBallSurface(
            particles = listOf(MetaBallParticle.circle(80.dp, 80.dp, 40.dp)),
            modifier = Modifier.fillMaxSize(),
            backdrop = null,
            color = Color.Blue,
            rimColor = null,
            bottomGlowColor = Color.White.copy(alpha = 66f / 255f),
        )
    }

    @Test
    fun allThreeBottomSurfacesHaveSharpTopAndSofterBottomRims() = runSkikoComposeUiTest(
        size = Size(1040f, 480f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme(colors = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    MetaBallSurface(
                        particles = listOf(
                            MetaBallParticle.circle(40.dp, 60.dp, 40.dp),
                            MetaBallParticle(130.dp, 60.dp, 100.dp, 40.dp, cornerRadius = 20.dp),
                            MetaBallParticle.circle(220.dp, 60.dp, 40.dp),
                        ),
                        modifier = Modifier.fillMaxSize(),
                        backdrop = null,
                        color = Color(0xFF202020),
                        rimColor = Color.White.copy(alpha = 0.8f),
                        rimBottomScale = 1f,
                        bottomGlowColor = Color.White.copy(alpha = 66f / 255f),
                    )
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        System.getenv("COUI_RENDER_ARTIFACT_DIR")?.let { directory ->
            val image = BufferedImage(pixels.width, pixels.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) image.setRGB(x, y, pixels[x, y].toArgb())
            ImageIO.write(image, "png", File(directory, "desktop-three-surfaces.png"))
        }
        listOf(160, 520, 880).forEach { x ->
            val top = (161..168).map { pixels[x, it].red }.max()
            val bottom = (311..318).map { pixels[x, it].red }.max()
            println("[native-rim] x=$x; top=$top; bottom=$bottom")
            assertTrue(top > bottom + 1f / 255f, "Each top rim must be brighter than the lower rim")
            assertTrue(bottom > 0.2f, "Each lower rim must remain visibly above the body")
            assertTrue(pixels[x, 164].red < pixels[x, 315].red, "The lower line must fade over a wider band than the sharp upper line")
        }
    }

    private fun checkProfile(samples: List<Sample>, content: @Composable () -> Unit) = runSkikoComposeUiTest(
        size = Size(640f, 640f),
        density = Density(4f),
    ) {
        setContent {
            COUITheme(colors = darkColorScheme()) {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { content() }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val errors = samples.map { abs(pixels[it.x, it.y].red - it.alpha) }
        println("[native-caustic] samples=${samples.size}; maxPixelError=${errors.max()}")
        assertTrue(errors.max() <= 1.5f / 255f, "The exterior must match native SDF/vertical falloff within raster quantization; max error=${errors.max()}")
    }
}

private data class Sample(val x: Int, val y: Int, val alpha: Float)

private val ToolbarSamples = listOf(
    Sample(373, 384, 0.01635848f),
    Sample(368, 388, 0.02369526f),
    Sample(362, 392, 0.03328595f),
    Sample(348, 398, 0.05575478f),
    Sample(320, 404, 0.07408570f),
    Sample(291, 398, 0.05575478f),
    Sample(278, 392, 0.03522439f),
    Sample(271, 388, 0.02369526f),
    Sample(266, 384, 0.01635848f),
    Sample(377, 388, 0.00835048f),
    Sample(371, 393, 0.01383568f),
    Sample(365, 397, 0.02024079f),
    Sample(350, 404, 0.03671297f),
    Sample(320, 410, 0.04847683f),
    Sample(289, 404, 0.03671297f),
    Sample(275, 397, 0.02256566f),
    Sample(268, 393, 0.01383568f),
    Sample(262, 388, 0.00835048f),
    Sample(382, 395, 0.00000000f),
    Sample(376, 400, 0.00000000f),
    Sample(369, 404, 0.00000000f),
    Sample(353, 412, 0.00026206f),
    Sample(320, 418, 0.00523740f),
    Sample(286, 412, 0.00026206f),
    Sample(271, 404, 0.00000000f),
    Sample(263, 400, 0.00000000f),
    Sample(257, 395, 0.00000000f),
    Sample(390, 404, 0.00000000f),
    Sample(383, 410, 0.00000000f),
    Sample(375, 415, 0.00000000f),
    Sample(357, 423, 0.00000000f),
    Sample(320, 430, 0.00000000f),
    Sample(282, 423, 0.00000000f),
    Sample(265, 415, 0.00000000f),
    Sample(256, 410, 0.00000000f),
    Sample(249, 404, 0.00000000f),
    Sample(399, 414, 0.00000000f),
    Sample(391, 421, 0.00000000f),
    Sample(382, 427, 0.00000000f),
    Sample(362, 436, 0.00000000f),
    Sample(320, 444, 0.00000000f),
    Sample(277, 436, 0.00000000f),
    Sample(258, 427, 0.00000000f),
    Sample(248, 421, 0.00000000f),
    Sample(240, 414, 0.00000000f),
)

private val AccessorySamples = listOf(
    Sample(373, 384, 0.00518584f),
    Sample(368, 388, 0.00828604f),
    Sample(362, 392, 0.01288127f),
    Sample(348, 398, 0.02469885f),
    Sample(320, 404, 0.04120931f),
    Sample(291, 398, 0.02469885f),
    Sample(278, 392, 0.01328028f),
    Sample(271, 388, 0.00828604f),
    Sample(266, 384, 0.00518584f),
    Sample(377, 388, 0.00539265f),
    Sample(371, 393, 0.00957820f),
    Sample(365, 397, 0.01477639f),
    Sample(350, 404, 0.02989660f),
    Sample(320, 410, 0.04892052f),
    Sample(289, 404, 0.02989660f),
    Sample(275, 397, 0.01534826f),
    Sample(268, 393, 0.00957820f),
    Sample(262, 388, 0.00539265f),
    Sample(382, 395, 0.00474023f),
    Sample(376, 400, 0.00881751f),
    Sample(369, 404, 0.01484783f),
    Sample(353, 412, 0.03170509f),
    Sample(320, 418, 0.05231723f),
    Sample(286, 412, 0.03170509f),
    Sample(271, 404, 0.01570061f),
    Sample(263, 400, 0.00881751f),
    Sample(257, 395, 0.00474023f),
    Sample(390, 404, 0.00000000f),
    Sample(383, 410, 0.00096470f),
    Sample(375, 415, 0.00518476f),
    Sample(357, 423, 0.01907713f),
    Sample(320, 430, 0.03506963f),
    Sample(282, 423, 0.01907713f),
    Sample(265, 415, 0.00628857f),
    Sample(256, 410, 0.00096470f),
    Sample(249, 404, 0.00000000f),
    Sample(399, 414, 0.00000000f),
    Sample(391, 421, 0.00000000f),
    Sample(382, 427, 0.00000000f),
    Sample(362, 436, 0.00000000f),
    Sample(320, 444, 0.00000000f),
    Sample(277, 436, 0.00000000f),
    Sample(258, 427, 0.00000000f),
    Sample(248, 421, 0.00000000f),
    Sample(240, 414, 0.00000000f),
)
