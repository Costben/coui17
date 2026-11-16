// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class, ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
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
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
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
    fun aVerticalSwipeThatStartsOnACardScrollsTheList() = runSkikoComposeUiTest(size = Size(360f, 400f), density = Density(1f)) {
        val scroll = ScrollState(0)
        setContent {
            COUITheme {
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    repeat(6) {
                        NotificationSwipeBox({}, {}, COUIIcons.Basic.Close, COUIIcons.Basic.Close) {
                            Box(Modifier.fillMaxWidth().height(160.dp))
                        }
                    }
                }
            }
        }
        onRoot().performTouchInput { swipe(Offset(200f, 320f), Offset(200f, 80f), 300) }
        waitForIdle()
        assertTrue(scroll.value > 100)
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

    @Test
    fun settleSpringMatchesSystemUiSnapBackConfig() {
        assertEquals(200f, NotificationSwipeDefaults.SettleSpring.stiffness)
        assertEquals(0.75f, NotificationSwipeDefaults.SettleSpring.dampingRatio)
    }

    @Test
    fun conicContinuousCurvatureMatchesSystemUiBezierCorner() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        assertEquals(16.dp, NotificationSwipeDefaults.CardCornerRadius)
        assertEquals(16.dp, NotificationSwipeDefaults.CornerRadius)
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Green)) {
                MetaBallSurfaceWithRim(
                    particles = notificationSwipeParticles(360.dp, 84.dp, distance = 0.dp),
                    blurColors = null,
                    rimColor = null,
                    rimLineWidth = NotificationSwipeDefaults.RimLineWidth,
                    rimLineAngle = 0f,
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Blue,
                    backdrop = null,
                    rimBottomScale = 1f,
                    edgeStyle = GradientStrokeStyle.NOTIFICATION_DARK,
                    conicCornerWeight = NotificationSwipeDefaults.CONIC_CORNER_WEIGHT,
                )
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        // At (4.5, 4.5), circular 16dp would be inside, while CONIC (24dp span, weight 1.1) stays outside (5.714px).
        val outsideConic = pixels[4, 4]
        val insideConic = pixels[7, 7]
        assertTrue(outsideConic.green > 0.95f && outsideConic.blue < 0.05f, "Expected (4,4) outside CONIC corner, got $outsideConic")
        assertTrue(insideConic.blue > 0.85f && insideConic.green < 0.15f, "Expected (7,7) inside CONIC corner, got $insideConic")
    }

    @Test
    fun materialMixAndRimPresetsMatchSystemUi() {
        assertEquals(
            listOf(
                BlendColorEntry(Color(0x19404040), BlurBlendMode.Luminosity),
                BlendColorEntry(Color(0x4D737373), BlurBlendMode.ColorDodge),
            ),
            NotificationSwipeDefaults.blurColors(isDark = false),
        )
        assertEquals(
            listOf(
                BlendColorEntry(Color(0x59404040), BlurBlendMode.Luminosity),
                BlendColorEntry(Color(0x66737373), BlurBlendMode.ColorDodge),
            ),
            NotificationSwipeDefaults.blurColors(isDark = true),
        )
        val darkEdge = GradientStrokeStyleMapper.toEdgeArray16(
            style = NotificationSwipeDefaults.edgeStyle(isDark = true),
            width = 328f * 3f,
            height = 60f * 3f,
            lineWidth = 8f,
            lineAlpha = 1f,
            lineAngle = 0f,
            density = 3f,
        )
        assertEquals(0.35f, darkEdge[7], 1e-4f)
        assertEquals(0.50f, darkEdge[8], 1e-4f)
        assertEquals(4.46f, darkEdge[14], 1e-4f)
        assertEquals(1.1f, darkEdge[15], 1e-4f)

        val lightEdge = GradientStrokeStyleMapper.toEdgeArray16(
            style = NotificationSwipeDefaults.edgeStyle(isDark = false),
            width = 328f * 3f,
            height = 60f * 3f,
            lineWidth = 8f,
            lineAlpha = 1f,
            lineAngle = 0f,
            density = 3f,
        )
        assertEquals(0.40f, lightEdge[7], 1e-4f)
        assertEquals(0.56f, lightEdge[8], 1e-4f)
        assertEquals(3.0f, lightEdge[14], 1e-4f)
        assertEquals(1.0f, lightEdge[15], 1e-4f)
    }

    @Test
    fun clickingDeleteSweepsLeftBeforeCallingOnDismiss() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState(initiallyRevealed = true)
        var deleted = 0
        setContent {
            COUITheme {
                NotificationSwipeBox(
                    onSettings = {},
                    onDismiss = { deleted++ },
                    settingsIcon = COUIIcons.Basic.Close,
                    dismissIcon = null,
                    state = state,
                    dismissOnFullSwipe = true,
                ) {
                    Box(Modifier.fillMaxWidth().height(84.dp).background(Color.White))
                }
            }
        }
        waitForIdle()
        val revealOffset = state.offsetPx
        mainClock.autoAdvance = false
        onNodeWithContentDescription("Delete").performTouchInput {
            down(center)
            up()
        }
        mainClock.advanceTimeBy(80L)
        assertEquals(0, deleted, "Delete must sweep the row out to the left before invoking onDismiss")
        assertTrue(
            state.offsetPx < revealOffset - 20f,
            "Expected row to slide left from $revealOffset during dismiss sweep, was ${state.offsetPx}",
        )

        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(1, deleted)
        assertFalse(state.isRevealed)
        assertEquals(0f, state.offsetPx)
        onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    @Test
    fun pressingActionButtonCastsRadialSpotLightClippedToConicCurve() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        val state = NotificationSwipeState(initiallyRevealed = true)
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Green)) {
                COUITheme(colors = COUITheme.colorScheme.copy(background = Color(0xFF111111))) {
                    NotificationSwipeBox(
                        onSettings = {},
                        onDismiss = {},
                        settingsIcon = COUIIcons.Basic.Close,
                        dismissIcon = COUIIcons.Basic.Close,
                        state = state,
                        containerColor = Color(0xFF202020),
                        rimColor = Color.Transparent,
                    ) {
                        Box(Modifier.fillMaxWidth().height(84.dp).background(Color.Red))
                    }
                }
            }
        }
        waitForIdle()

        // Settings button at default reveal occupies x=[226, 286), y=[8, 76); centered 24dp icon
        // occupies [244, 268) x [30, 54). Press near top-left clear of the icon at local (12, 12).
        val settingsBounds = onNodeWithContentDescription("Settings").fetchSemanticsNode().boundsInRoot
        val cellLeft = settingsBounds.left.toInt()
        val cellTop = settingsBounds.top.toInt()

        val restingPixels = onRoot().captureToImage().toPixelMap()
        val restingNearPress = restingPixels[cellLeft + 12, cellTop + 12]
        val restingFarCorner = restingPixels[cellLeft + 50, cellTop + 58]

        mainClock.autoAdvance = false
        onNodeWithContentDescription("Settings").performTouchInput {
            down(Offset(12f, 12f))
        }
        mainClock.advanceTimeBy(180L)

        val pressedPixels = onRoot().captureToImage().toPixelMap()
        val pressedNearPress = pressedPixels[cellLeft + 12, cellTop + 12]
        val pressedFarCorner = pressedPixels[cellLeft + 50, cellTop + 58]
        // (5, 5) is inside circular 16dp (~4.69px inset) but outside CONIC 16dp (~5.71px inset).
        val conicOutside = pressedPixels[cellLeft + 5, cellTop + 5]
        val conicInside = pressedPixels[cellLeft + 10, cellTop + 10]

        val nearGain = pressedNearPress.red - restingNearPress.red
        val farGain = pressedFarCorner.red - restingFarCorner.red
        assertTrue(nearGain > 0.05f, "Pressed region must emit spotlight glow, gain=$nearGain")
        assertTrue(
            nearGain > farGain + 0.01f,
            "Spotlight must diffuse radially (nearGain=$nearGain > farGain=$farGain) instead of uniform COUIIndication",
        )
        assertTrue(
            conicOutside.green > 0.9f && conicOutside.red < 0.1f,
            "Spotlight and pressed cell must clip outside CONIC corner (5,5), got $conicOutside",
        )
        assertTrue(
            conicInside.green < 0.35f,
            "Pressed cell must cover inside CONIC corner (10,10), got $conicInside",
        )

        onNodeWithContentDescription("Settings").performTouchInput { up() }
        mainClock.autoAdvance = true
        waitForIdle()
    }
}
