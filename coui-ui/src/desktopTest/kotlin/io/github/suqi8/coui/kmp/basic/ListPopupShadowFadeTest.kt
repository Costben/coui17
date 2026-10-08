// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.BezierGravityPathGeometry
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertTrue

/** Compare a fading frame with the settled shadow, independently of its implementation. */
@OptIn(ExperimentalTestApi::class)
class ListPopupShadowFadeTest {
    @Test
    fun fadingFramesKeepTheShadowOutsideTheMenuBounds() {
        for (scale in listOf(1f, 0.65f)) {
            runSkikoComposeUiTest(size = Size(400f, 300f), density = Density(1f)) {
                var opacity by mutableFloatStateOf(1f)
                setContent {
                    COUITheme {
                        val backdrop = rememberLayerBackdrop { drawContent() }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.White))
                            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                                ListPopupContent(
                                    enableGlass = true,
                                    popupContentSize = IntSize(168, 88),
                                    onPopupContentSizeChange = {},
                                    fractionProgress = { scale },
                                    alphaProgress = { opacity },
                                    pathProgress = { 0f },
                                    gravityPath = BezierGravityPathGeometry(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                                    popupLayoutPosition = PopupLayoutPosition(showBelow = false, showAbove = true, isRightAligned = true),
                                    localTransformOrigin = TransformOrigin.Center,
                                ) {
                                    Box(Modifier.size(168.dp, 88.dp).testTag("body"))
                                }
                            }
                        }
                    }
                }
                waitForIdle()
                val settled = onRoot().captureToImage().toPixelMap()
                // Sample just outside each straight edge; bounds come from the known test geometry.
                val left = (200f - 84f * scale).toInt()
                val right = (200f + 84f * scale).toInt()
                val bottom = (150f + 44f * scale).toInt()
                val samples = listOf(left - 3 to 150, right + 3 to 150, 200 to bottom + 3)
                for (alpha in listOf(0.25f, 0.5f, 0.9f, 0.999f)) {
                    opacity = alpha
                    waitForIdle()
                    val faded = onRoot().captureToImage().toPixelMap()
                    for ((x, y) in samples) {
                        val restDarkness = 1f - settled[x, y].red
                        val fadeDarkness = 1f - faded[x, y].red
                        println("[popup-shadow] scale=$scale alpha=$alpha sample=$x,$y rest=$restDarkness faded=$fadeDarkness")
                        assertTrue(restDarkness > 0.01f, "The settled shadow must cover the reference sample")
                        assertTrue(
                            kotlin.math.abs(fadeDarkness - restDarkness * alpha) < 2f / 255f,
                            "The exterior shadow must fade continuously, without a rectangular cutoff",
                        )
                    }
                }
            }
        }
    }
}
