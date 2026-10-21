// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertTrue

private val CanvasColor = Color(0xFFE8E8E8)

/** Pixels sampled at the center of the middle tab, past the capsule's bottom edge. */
private const val SAMPLE_X = 160
private const val CAPSULE_BOTTOM = 40
private const val BACKGROUND_Y = 60

/**
 * A frameless [TabRow] has no container inset, so its indicator shadow has to paint past the row's
 * bottom edge. Clipping the shadow to the row bounds would shear it off in a single hard step.
 */
@OptIn(ExperimentalTestApi::class)
class TabRowFramelessShadowTest {

    @Test
    fun framelessTabRowPaintsItsShadowBelowTheCapsule() = runSkikoComposeUiTest(size = Size(320f, 80f)) {
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Box(Modifier.fillMaxSize().background(CanvasColor)) {
                    TabRow(
                        tabs = listOf("A", "B", "C"),
                        selectedTabIndex = 1,
                        onTabSelected = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        waitForIdle()

        val pixels = onRoot().captureToImage().toPixelMap()
        fun luminance(x: Int, y: Int): Float {
            val color = pixels[x, y]
            return 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
        }

        val belowCapsule = luminance(SAMPLE_X, CAPSULE_BOTTOM + 1)
        val background = luminance(SAMPLE_X, BACKGROUND_Y)
        assertTrue(
            belowCapsule < background - 0.02f,
            "expected the indicator shadow below the capsule, got $belowCapsule against $background",
        )
    }
}
