// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CONTENT_TAG = "pull-to-refresh-content"

private const val SURFACE_WIDTH_DP = 240f
private const val SURFACE_HEIGHT_DP = 400f

/** Tall enough that the content covers everything under the header band. */
private val ContentHeight = 300.dp

private val TestDensity = Density(1f)

/** Red is unambiguous against the white surface and the theme's neutral indicator. */
private val IndicatorColor = Color(0xFFFF0000)

/**
 * The band is as tall as the drag offset and the header block is bottom-anchored in it, so this
 * pull reveals the whole block. The block's own height does not move the content.
 */
private val RevealedPull = 34.dp

@OptIn(ExperimentalTestApi::class)
class PullToRefreshHeaderGapRenderTest {

    @Test
    fun theGapLiftsTheIndicatorOffTheContentTop() {
        var defaultContentTop = 0f
        var defaultIndicatorBottom = -1
        var gaplessIndicatorBottom = -1

        render(headerBottomGap = PullToRefreshDefaults.HeaderBottomGap) { contentTop, indicatorBottom ->
            defaultContentTop = contentTop
            defaultIndicatorBottom = indicatorBottom
        }
        render(headerBottomGap = 0.dp) { _, indicatorBottom ->
            gaplessIndicatorBottom = indicatorBottom
        }

        assertTrue(defaultIndicatorBottom >= 0, "the indicator should be drawn inside the revealed band")
        assertTrue(gaplessIndicatorBottom >= 0, "the indicator should be drawn inside the revealed band")
        assertTrue(
            defaultIndicatorBottom < defaultContentTop,
            "the indicator must not reach the content top: bottom=$defaultIndicatorBottom, top=$defaultContentTop",
        )
        assertEquals(
            PullToRefreshDefaults.HeaderBottomGap.value.toInt(),
            gaplessIndicatorBottom - defaultIndicatorBottom,
            "the gap should be the only thing separating the two layouts",
        )
    }

    private fun render(
        headerBottomGap: Dp,
        onMeasured: (contentTop: Float, indicatorBottom: Int) -> Unit,
    ) = runSkikoComposeUiTest(
        size = Size(SURFACE_WIDTH_DP, SURFACE_HEIGHT_DP),
        density = TestDensity,
    ) {
        lateinit var state: PullToRefreshState
        setContent {
            state = rememberPullToRefreshState()
            Box(Modifier.fillMaxSize().background(Color.White)) {
                PullToRefresh(
                    isRefreshing = false,
                    onRefresh = {},
                    pullToRefreshState = state,
                    color = IndicatorColor,
                    headerBottomGap = headerBottomGap,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(ContentHeight)
                            .testTag(CONTENT_TAG)
                            .background(Color.White),
                    )
                }
            }
        }
        waitForIdle()

        runOnIdle { state.dragOffset = with(TestDensity) { RevealedPull.toPx() } }
        waitForIdle()

        val contentTop = onNodeWithTag(CONTENT_TAG).getBoundsInRoot().top.value
        val pixels = onRoot().captureToImage().toPixelMap()

        var indicatorBottom = -1
        for (y in 0 until contentTop.toInt()) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y] == IndicatorColor) {
                    indicatorBottom = y
                    break
                }
            }
        }
        onMeasured(contentTop, indicatorBottom)
    }
}
