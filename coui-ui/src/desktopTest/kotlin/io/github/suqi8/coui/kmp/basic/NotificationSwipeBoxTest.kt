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
        val particles = notificationSwipeParticles(
            width = 360.dp,
            height = 180.dp,
            distance = 144.dp,
            actionRecoils = listOf(1f, 1f),
        )
        assertEquals(216.dp, particles[0].width)
        assertEquals(68.dp, particles[1].height)
        assertEquals(8.dp, particles[1].centerY - particles[1].height / 2)
        assertEquals(10.dp, particles[1].centerX - particles[2].centerX - particles[1].width)
    }

    /** A delete-only notification has one cell, and it sits against the outer padding. */
    @Test
    fun deleteOnlyNotificationRevealsOneCell() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var deleted = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(null, { deleted++ }, null, COUIIcons.Basic.Close, state = state) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        onNodeWithContentDescription("Settings").assertDoesNotExist()
        onRoot().performTouchInput { swipe(Offset(340f, 42f), Offset(280f, 42f), 700) }
        waitForIdle()
        assertTrue(state.isRevealed)
        assertEquals(74.dp, NotificationSwipeDefaults.revealWidth(1) * state.revealFraction)
        assertTrue(
            onNodeWithContentDescription("Delete").fetchSemanticsNode().boundsInRoot.left > 290f,
            "The single cell must sit against the outer padding",
        )
        assertEquals(0, deleted)
    }

    /** A drag that stops on the menu only reveals it; it never falls through to the delete. */
    @Test
    fun dragThatStopsAtTheMenuOnlyRevealsIt() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var deleted = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(null, { deleted++ }, null, COUIIcons.Basic.Close, state = state, dismissOnFullSwipe = true) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        onRoot().performTouchInput { swipe(Offset(340f, 42f), Offset(270f, 42f), 2000) }
        waitForIdle()
        assertTrue(state.isRevealed)
        assertEquals(0, deleted)
        onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    /** Carried past the dismiss threshold, the same swipe deletes and leaves nothing behind. */
    @Test
    fun fullSwipePastTheThresholdDeletes() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var deleted = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(null, { deleted++ }, null, COUIIcons.Basic.Close, state = state, dismissOnFullSwipe = true) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        // The 74dp menu plus the 40dp merge.
        assertEquals(114.dp, NotificationSwipeDefaults.dismissThreshold(NotificationSwipeDefaults.revealWidth(1)))
        onRoot().performTouchInput { swipe(Offset(340f, 42f), Offset(100f, 42f), 700) }
        waitForIdle()
        assertEquals(1, deleted)
        assertFalse(state.isRevealed)
        assertEquals(0f, state.revealFraction)
        onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    /** Releasing past the menu but short of the merge springs back to the menu. */
    @Test
    fun releasingShortOfTheThresholdKeepsTheMenu() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var deleted = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(null, { deleted++ }, null, COUIIcons.Basic.Close, state = state, dismissOnFullSwipe = true) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        // About 87dp after the touch slop: past the 74dp menu, short of its 40dp merge.
        onRoot().performTouchInput { swipe(Offset(340f, 42f), Offset(235f, 42f), 2000) }
        waitForIdle()
        assertEquals(0, deleted)
        assertTrue(state.isRevealed)
        assertEquals(74.dp, NotificationSwipeDefaults.revealWidth(1) * state.revealFraction)
        onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    /** The threshold is the merge, where the device ticks: a slow release just past it still deletes. */
    @Test
    fun slowReleaseJustPastTheMergeDeletes() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var deleted = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(null, { deleted++ }, null, null, state = state, dismissOnFullSwipe = true) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        // About 120dp after the touch slop, against the 114dp threshold.
        onRoot().performTouchInput { swipe(Offset(340f, 42f), Offset(202f, 42f), 2000) }
        waitForIdle()
        assertEquals(1, deleted)
        assertFalse(state.isRevealed)
        onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    /** The built-in trash keyframes its lid from flat to open and back, as SystemUI's lottie does. */
    @Test
    fun trashLidOpensAndClosesAcrossTheLottie() {
        assertEquals(-18f, notificationTrashLidRotation(0f), 0.0001f)
        assertEquals(-9f, notificationTrashLidRotation(0.25f), 0.0001f)
        assertEquals(0f, notificationTrashLidRotation(0.5f), 0.0001f)
        assertEquals(-9f, notificationTrashLidRotation(0.75f), 0.0001f)
        assertEquals(-18f, notificationTrashLidRotation(1f), 0.0001f)
    }

    /** Past the menu the card keeps following the finger, and the delete cell fills the space. */
    @Test
    fun cardKeepsFollowingTheFingerPastTheMenu() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var deleted = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(null, { deleted++ }, null, COUIIcons.Basic.Close, state = state, dismissOnFullSwipe = true) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        onRoot().performTouchInput {
            down(Offset(340f, 42f))
            repeat(12) { moveBy(Offset(-20f, 0f)) }
        }
        waitForIdle()
        assertTrue(state.offsetPx < -200f, "The card must follow the finger past the menu, was ${state.offsetPx}")
        val delete = onNodeWithContentDescription("Delete").fetchSemanticsNode().boundsInRoot
        assertTrue(delete.width > 150f, "The delete cell must stretch into the vacated space, was ${delete.width}")
        assertEquals(0, deleted)
        onRoot().performTouchInput { repeat(6) { moveBy(Offset(20f, 0f)) } }
        waitForIdle()
        onRoot().performTouchInput { up() }
        waitForIdle()
        assertEquals(0, deleted, "Dragging back inside the threshold before the release must not delete")
    }

    /** The row only goes away on release; a held drag past the threshold must not delete by itself. */
    @Test
    fun deleteWaitsForTheRelease() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState()
        var deleted = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(null, { deleted++ }, null, COUIIcons.Basic.Close, state = state, dismissOnFullSwipe = true) {
                    Box(Modifier.fillMaxWidth().height(84.dp))
                }
            }
        }
        onRoot().performTouchInput {
            down(Offset(340f, 42f))
            repeat(12) { moveBy(Offset(-20f, 0f)) }
        }
        waitForIdle()
        assertEquals(0, deleted)
        onRoot().performTouchInput { up() }
        waitForIdle()
        assertEquals(1, deleted)
        assertFalse(state.isRevealed)
        onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    /** Merged, the delete cell spans both slots and ends one gap short of the card. */
    @Test
    fun mergedDeleteCellSwallowsTheSettingsCell() {
        val particles = notificationSwipeParticles(
            width = 360.dp,
            height = 84.dp,
            distance = 144.dp,
            actionRecoils = listOf(1f, 1f),
            actionCount = 2,
            overshoot = 60.dp,
            merge = 1f,
        )
        val (card, delete, settings) = particles
        assertEquals(156.dp, card.width)
        assertEquals(190.dp, delete.width)
        assertEquals(card.width + NotificationSwipeDefaults.ActionGap, delete.centerX - delete.width / 2)
        assertEquals(0.2f, settings.scaleX, 0.0001f)
        // The settings cell stopped moving at the merge distance and sits inside the delete cell.
        assertEquals(360.dp - 4.dp - 30.dp - 70.dp - 40.dp, settings.centerX)
        val split = notificationSwipeParticles(
            width = 360.dp,
            height = 84.dp,
            distance = 144.dp,
            actionRecoils = listOf(1f, 1f),
            actionCount = 2,
            overshoot = 20.dp,
        )
        assertEquals(80.dp, split[1].width)
        assertEquals(1f, split[2].scaleX)
        assertEquals(360.dp - 4.dp - 30.dp - 70.dp - 20.dp, split[2].centerX)
    }
}
