// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOUCH_SLIDER_TAG = "touch-slider"

/**
 * The COUI touch target and value model of [Slider], plus the page indicator's dot size ladder.
 *
 * The slider track is 20dp tall but the COUI view is `couiSeekBarMinHeight` = 36dp, so the extra
 * 16dp of tappable area has to survive a caller that only sets the track height.
 */
@OptIn(ExperimentalTestApi::class)
class SliderTouchTargetTest {

    @Test
    fun theTouchTargetIsThirtySixDpTallWhileTheTrackStaysTwenty() = runSkikoComposeUiTest {
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Column {
                    Slider(
                        value = 0.5f,
                        onValueChange = {},
                        modifier = Modifier.testTag(TOUCH_SLIDER_TAG),
                    )
                }
            }
        }
        val height = Density(1f).run { onNodeWithTag(TOUCH_SLIDER_TAG).getBoundsInRoot().height.toPx() }
        assertEquals(36f, height, 0.5f)
    }

    /**
     * COUISeekBarCore.calculateDamping is 1 unless a caller sets a damping, and the fast move spring
     * only feeds an offset nothing reads, so a quick drag follows the finger all the way to the end.
     */
    @Test
    fun aQuickDragFollowsTheFingerToTheEnd() = runSkikoComposeUiTest {
        var value by mutableFloatStateOf(0.1f)
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Column(Modifier.width(300.dp)) {
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.testTag(TOUCH_SLIDER_TAG),
                    )
                }
            }
        }
        onNodeWithTag(TOUCH_SLIDER_TAG).performTouchInput {
            swipeRight(startX = 40f, endX = width - 1f, durationMillis = 120)
        }
        waitForIdle()
        assertEquals(1f, value, 0.001f)

        onNodeWithTag(TOUCH_SLIDER_TAG).performTouchInput {
            swipeLeft(startX = width - 40f, endX = 1f, durationMillis = 800)
        }
        waitForIdle()
        assertEquals(0f, value, 0.001f)
    }

    /**
     * `coui_seekbar_background_radius` = 2dp is never read for a horizontal bar: COUISeekBarCore
     * draws both tracks with half their height, so a 20dp track ends in a 10dp capsule.
     */
    @Test
    fun bothTrackEndsAreCapsules() = runSkikoComposeUiTest {
        setContent {
            COUITheme(colors = lightColorScheme()) {
                Column(Modifier.testTag(TOUCH_SLIDER_TAG).width(200.dp).background(Color.White)) {
                    Slider(
                        value = 0.5f,
                        onValueChange = {},
                        colors = SliderDefaults.sliderColors(
                            foregroundColor = Color.Blue,
                            backgroundColor = Color.Red,
                        ),
                    )
                }
            }
        }
        val pixels = onNodeWithTag(TOUCH_SLIDER_TAG).captureToImage().toPixelMap()
        val centerY = pixels.height / 2
        val trackX = (0 until pixels.width).filter { pixels[it, centerY] != Color.White }

        // Two pixels in and one down from the corner: inside a 2dp corner, outside a 10dp capsule.
        assertEquals(Color.White, pixels[trackX.first() + 2, centerY - 9])
        assertEquals(Color.White, pixels[trackX.last() - 2, centerY - 9])
    }

    @Test
    fun theDefaultsExposeTheCouiPercentRange() {
        assertEquals(0f, SliderDefaults.PercentRange.start)
        assertEquals(100f, SliderDefaults.PercentRange.endInclusive)
        assertEquals(100f, SliderDefaults.FLING_VELOCITY_LIMIT)
        assertEquals(20.dp, SliderDefaults.MinHeight)
        assertEquals(36.dp, SliderDefaults.MinTouchHeight)
    }

    @Test
    fun theTrackUsesCouiColorContainerTwelve() {
        assertEquals(Color(0x1F000000), lightColorScheme().sliderBackground)
        assertEquals(Color(0x33FFFFFF), darkColorScheme().sliderBackground)
    }

    @Test
    fun thePageIndicatorWalksTheFourRungLadder() {
        val normal = 3f
        val medium = 2.25f
        val small = 1.25f
        val pitch = 12f

        assertEquals(normal, PageIndicatorDefaults.ladderRadius(0f, pitch, normal, medium, small), 1e-4f)
        assertEquals(medium, PageIndicatorDefaults.ladderRadius(pitch, pitch, normal, medium, small), 1e-4f)
        assertEquals(small, PageIndicatorDefaults.ladderRadius(pitch * 2f, pitch, normal, medium, small), 1e-4f)
        assertEquals(0f, PageIndicatorDefaults.ladderRadius(pitch * 3f, pitch, normal, medium, small), 1e-4f)
        // Between two rungs the radius eases, so it stays inside the two neighbours.
        val between = PageIndicatorDefaults.ladderRadius(pitch * 1.5f, pitch, normal, medium, small)
        assertTrue(between in small..medium)
    }
}
