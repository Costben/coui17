// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class, ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Close
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSwipeBoxTest {
    @Test
    fun cardAndMenuBridgeBeforeRecoilAndSeparateAfterwards() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        var recoil by androidx.compose.runtime.mutableFloatStateOf(0f)
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Green)) {
                MetaBallSurface(
                    particles = notificationSwipeParticles(360.dp, 84.dp, 143.dp, deleteRecoil = 1f, settingsRecoil = recoil),
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Blue,
                    backdrop = null,
                    rimColor = null,
                    extension = NotificationSwipeDefaults.BlendRange,
                    extensionFactorX = 1.2f,
                    extensionFactorY = 2f,
                )
            }
        }
        waitForIdle()
        val bridged = onRoot().captureToImage().toPixelMap()[217, 42]
        assertTrue(bridged.blue > 0.75f, "The field must join the positive 1dp gap before recoil")
        runOnIdle { recoil = 1f }
        waitForIdle()
        val separated = onRoot().captureToImage().toPixelMap()[221, 42]
        assertTrue(separated.green > 0.9f, "The restored 9dp gap must expose the backdrop")
        println("MELT: bridge=blue / separated=green")
    }

    @Test
    fun hiddenActionsCannotBeClickedAndLeftSwipeRevealsBoth() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var deleted = 0
        var contentTaps = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(
                    onSettings = {},
                    onDismiss = { deleted++ },
                    settingsIcon = COUIIcons.Basic.Close,
                    dismissIcon = COUIIcons.Basic.Close,
                    state = state,
                ) {
                    Box(Modifier.fillMaxWidth().height(84.dp).clickable { contentTaps++ })
                }
            }
        }
        onNodeWithContentDescription("Delete").assertDoesNotExist()
        onRoot().performTouchInput { swipe(Offset(330f, 42f), Offset(160f, 42f), 700) }
        waitForIdle()
        assertTrue(state.isRevealed)
        onNodeWithContentDescription("Settings").assertIsDisplayed()
        onNodeWithContentDescription("Delete").performClick()
        waitForIdle()
        assertEquals(1, deleted)
        assertEquals(0, contentTaps, "Clipped notification content must not intercept the menu tap")
        assertFalse(state.isRevealed)
        onNodeWithContentDescription("Delete").assertDoesNotExist()
        println("GESTURE: both actions revealed / delete=1 / closed")
    }

    @Test
    fun shortSlowDragReturnsAndVerticalDragDoesNotOpenMenu() = runSkikoComposeUiTest(size = Size(360f, 120f), density = Density(1f)) {
        val state = NotificationSwipeState()
        setContent {
            COUITheme {
                NotificationSwipeBox({}, {}, COUIIcons.Basic.Close, COUIIcons.Basic.Close, state = state) {
                    Box(Modifier.fillMaxWidth().height(120.dp))
                }
            }
        }
        onRoot().performTouchInput { swipe(Offset(300f, 42f), Offset(260f, 42f), 1000) }
        waitForIdle()
        assertFalse(state.isRevealed)
        assertEquals(0f, state.revealFraction)
        onRoot().performTouchInput { swipe(Offset(200f, 20f), Offset(200f, 100f), 500) }
        waitForIdle()
        assertFalse(state.isRevealed)
    }

    @Test
    fun leftSwipeKeepsItsPhysicalDirectionWithRtlContent() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        setContent {
            COUITheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NotificationSwipeBox({}, {}, COUIIcons.Basic.Close, COUIIcons.Basic.Close, state = state) {
                        Box(Modifier.fillMaxWidth().height(84.dp))
                    }
                }
            }
        }
        onRoot().performTouchInput { swipe(Offset(330f, 42f), Offset(150f, 42f), 700) }
        waitForIdle()
        assertTrue(state.isRevealed)
        onNodeWithContentDescription("Delete").assertIsDisplayed()
        assertTrue(onNodeWithContentDescription("Delete").fetchSemanticsNode().boundsInRoot.left > 290f)
    }

    /**
     * A drag frame only moves the card, so it must not recompose the box itself. The `composed`
     * modifier is materialised on every composition of the box and counts them.
     */
    @Test
    fun draggingDoesNotRecomposeTheSwipeBox() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var compositions = 0
        val counting = Modifier.composed {
            compositions++
            Modifier
        }
        setContent {
            COUITheme {
                NotificationSwipeBox({}, {}, COUIIcons.Basic.Close, COUIIcons.Basic.Close, modifier = counting, state = state) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        waitForIdle()
        val settled = compositions
        onRoot().performTouchInput {
            down(Offset(330f, 42f))
            repeat(10) { moveBy(Offset(-12f, 0f)) }
        }
        waitForIdle()
        assertTrue(state.revealFraction > 0f, "The drag must have moved the card")
        assertEquals(settled, compositions, "A drag frame must re-place the card, not recompose the box")
        onRoot().performTouchInput { up() }
    }

    @Test
    fun swappingTheStateMovesTheCardClipToo() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val revealed = NotificationSwipeState()
        val closed = NotificationSwipeState()
        var state by mutableStateOf(revealed)
        setContent {
            COUITheme {
                NotificationSwipeBox({}, {}, COUIIcons.Basic.Close, COUIIcons.Basic.Close, state = state) {
                    Box(Modifier.fillMaxWidth().height(84.dp).background(Color.Red))
                }
            }
        }
        onRoot().performTouchInput { swipe(Offset(330f, 42f), Offset(160f, 42f), 700) }
        waitForIdle()
        assertTrue(revealed.isRevealed)
        assertTrue(onRoot().captureToImage().toPixelMap()[340, 42].green > 0.5f, "The revealed card must vacate the menu strip")
        runOnIdle { state = closed }
        waitForIdle()
        val strip = onRoot().captureToImage().toPixelMap()[340, 42]
        assertTrue(strip.red > 0.9f && strip.green < 0.1f, "The closed state's card must cover the strip again")
    }

    /** An initially revealed (or restored) state must stay open once the real anchors arrive. */
    @Test
    fun initiallyRevealedStateStaysOpenAfterMeasuring() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState(initiallyRevealed = true)
        setContent {
            COUITheme {
                NotificationSwipeBox({}, {}, COUIIcons.Basic.Close, COUIIcons.Basic.Close, state = state) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        waitForIdle()
        assertTrue(state.isRevealed)
        assertEquals(1f, state.revealFraction)
        onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    @Test
    fun settingsFollowsDeleteAndTallCardsKeepTopAlignedMenu() {
        assertEquals(1f, notificationActionProgress(74.dp, 0))
        assertEquals(0f, notificationActionProgress(74.dp, 1))
        val particles = notificationSwipeParticles(360.dp, 180.dp, 144.dp, 1f, 1f)
        assertEquals(216.dp, particles[0].width)
        assertEquals(68.dp, particles[1].height)
        assertEquals(8.dp, particles[1].centerY - particles[1].height / 2)
        assertEquals(10.dp, particles[1].centerX - particles[2].centerX - particles[1].width)
    }
}
