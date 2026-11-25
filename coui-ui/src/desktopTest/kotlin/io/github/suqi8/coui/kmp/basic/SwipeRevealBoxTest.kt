// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class, ExperimentalTestApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Close
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeRevealBoxTest {
    @Test
    fun cardStyleUsesStandardCardCornerTighterThanNotificationConic() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Green)) {
                COUITheme {
                    SwipeRevealBox(
                        actions = listOf(SwipeRevealAction(COUIIcons.Basic.Close, "Delete", {})),
                        style = SwipeRevealDefaults.cardStyle(
                            containerColor = Color.Blue,
                        ),
                    ) {
                        Box(Modifier.fillMaxWidth().height(84.dp))
                    }
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        // For CardDefaults.CornerRadius (17dp) with Lp 3.0 squircle, 45-degree corner inset is
        // 17 * (1 - 2^(-1/3)) ≈ 3.51px, so (4, 4) is INSIDE the card silhouette — whereas
        // NotificationSwipeBox's 16dp CONIC (24dp Bezier span, inset ~5.71px) leaves (4, 4) outside.
        val outsideCorner = pixels[2, 2]
        val insideCardSquircle = pixels[4, 4]
        assertTrue(outsideCorner.green > 0.9f && outsideCorner.blue < 0.1f, "Expected (2,2) outside 17dp Lp 3.0 corner, got $outsideCorner")
        assertTrue(insideCardSquircle.blue > 0.85f && insideCardSquircle.green < 0.15f, "Expected (4,4) inside 17dp Lp 3.0 corner, got $insideCardSquircle")
    }

    @Test
    fun cardStyleDetachesRimAndRendersFlatOpaqueSurface() = runSkikoComposeUiTest(size = Size(360f, 64f), density = Density(1f)) {
        val fill = Color(0xFF203040)
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                COUITheme(colors = COUITheme.colorScheme.copy(background = Color(0xFF111111))) {
                    SwipeRevealBox(
                        actions = listOf(SwipeRevealAction(COUIIcons.Basic.Close, "Delete", {})),
                        style = SwipeRevealDefaults.cardStyle(
                            containerColor = fill,
                            enableBlur = false,
                            enableRim = false,
                        ),
                    ) {
                        Box(Modifier.fillMaxWidth().height(64.dp))
                    }
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val topEdge = pixels[180, 1]
        val center = pixels[180, 32]
        assertEquals(fill.red, center.red, 0.02f)
        assertEquals(fill.green, center.green, 0.02f)
        assertEquals(fill.blue, center.blue, 0.02f)
        assertTrue(
            abs(topEdge.red - center.red) < 0.03f &&
                abs(topEdge.green - center.green) < 0.03f &&
                abs(topEdge.blue - center.blue) < 0.03f,
            "Expected flat top edge with enableRim=false: top=$topEdge center=$center",
        )
    }

    @Test
    fun cardStyleButtonsAreCompactCenteredAndCriticallyDamped() {
        val style = SwipeRevealStyle()
        assertEquals(1f, style.settleSpring.dampingRatio)
        assertEquals(SwipeRevealDefaults.CompactActionSize, style.actionWidth)
        assertEquals(SwipeRevealDefaults.CompactActionSize, style.actionMaxHeight)

        val revealWidth = SwipeRevealDefaults.revealWidth(2, style)
        assertEquals(112.dp, revealWidth)
        val particles = swipeRevealParticles(
            width = 360.dp,
            height = 84.dp,
            distance = revealWidth,
            style = style,
            actionFollowsDp = listOf(0.dp, 0.dp),
            actionCount = 2,
        )
        val (card, primary, secondary) = particles
        assertEquals(248.dp, card.width)
        assertEquals(48.dp, primary.height)
        assertEquals(42.dp, primary.centerY)
        assertTrue(primary.height < card.height, "Revealed action button must be visibly more compact than the card")
        assertEquals(360.dp, primary.centerX + primary.width / 2)
        assertEquals(8.dp, primary.centerX - secondary.centerX - primary.width)
        assertEquals(CardDefaults.CornerRadius, card.cornerRadius)
        assertEquals(SwipeRevealDefaults.CompactActionCornerRadius, primary.cornerRadius)
    }

    @Test
    fun circleAndRoundRectPresetsExposeDistinctButtonGeometries() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        lateinit var circleStyle: SwipeRevealStyle
        lateinit var roundRectStyle: SwipeRevealStyle
        setContent {
            COUITheme {
                circleStyle = SwipeRevealDefaults.circleActionStyle()
                roundRectStyle = SwipeRevealDefaults.roundRectActionStyle()
            }
        }
        waitForIdle()
        assertEquals(44.dp, circleStyle.actionWidth)
        assertEquals(44.dp, circleStyle.actionMaxHeight)
        assertEquals(22.dp, circleStyle.actionCornerRadius)
        assertEquals(68.dp, roundRectStyle.actionWidth)
        assertEquals(52.dp, roundRectStyle.actionMaxHeight)
        assertEquals(12.dp, roundRectStyle.actionCornerRadius)
    }

    @Test
    fun coloredActionsRenderDistinctPerParticleFills() = runSkikoComposeUiTest(size = Size(360f, 72f), density = Density(1f)) {
        val state = SwipeRevealState(initiallyRevealed = true)
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                COUITheme {
                    SwipeRevealBox(
                        actions = listOf(
                            SwipeRevealAction(
                                label = "Pin",
                                description = "Pin",
                                containerColor = Color.Blue,
                                color = Color.White,
                                onClick = {},
                            ),
                            SwipeRevealAction(
                                label = "Delete",
                                description = "Delete",
                                containerColor = Color.Red,
                                color = Color.White,
                                onClick = {},
                            ),
                        ),
                        state = state,
                        style = SwipeRevealDefaults.roundRectActionStyle(
                            containerColor = Color.Green,
                        ),
                    ) {
                        Box(Modifier.fillMaxWidth().height(72.dp))
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithText("Pin").assertIsDisplayed()
        onNodeWithText("Delete").assertIsDisplayed()
        val pixels = onRoot().captureToImage().toPixelMap()
        // Sample inside card (green), Pin button (blue), and Delete button (red) away from text.
        val cardSample = pixels[40, 36]
        val pinBounds = onNodeWithText("Pin").fetchSemanticsNode().boundsInRoot
        val deleteBounds = onNodeWithText("Delete").fetchSemanticsNode().boundsInRoot
        val pinSample = pixels[pinBounds.left.toInt() + 4, pinBounds.top.toInt() + 6]
        val deleteSample = pixels[deleteBounds.right.toInt() - 4, deleteBounds.top.toInt() + 6]
        assertTrue(cardSample.green > 0.85f && cardSample.red < 0.15f, "Expected green card, got $cardSample")
        assertTrue(pinSample.blue > 0.85f && pinSample.green < 0.15f, "Expected blue Pin button, got $pinSample")
        assertTrue(deleteSample.red > 0.85f && deleteSample.blue < 0.15f, "Expected red Delete button, got $deleteSample")
    }

    @Test
    fun coloredActionsNeverBleedIntoRestingOrPartiallySwipedCard() = runSkikoComposeUiTest(size = Size(360f, 72f), density = Density(1f)) {
        val state = SwipeRevealState(initiallyRevealed = false)
        val cardFill = Color(0xFF2B2B2B)
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF121212))) {
                COUITheme(colors = COUITheme.colorScheme.copy(background = Color(0xFF121212))) {
                    SwipeRevealBox(
                        actions = listOf(
                            SwipeRevealAction(
                                icon = COUIIcons.Basic.Close,
                                description = "Pin",
                                containerColor = Color(0xFF0072F5),
                                color = Color.White,
                                onClick = {},
                            ),
                            SwipeRevealAction(
                                icon = null,
                                description = "Delete",
                                containerColor = Color(0xFFEA3323),
                                color = Color.White,
                                onClick = {},
                            ),
                        ),
                        state = state,
                        style = SwipeRevealDefaults.roundRectActionStyle(
                            containerColor = cardFill,
                        ),
                    ) {
                        Box(Modifier.fillMaxWidth().height(72.dp))
                    }
                }
            }
        }
        waitForIdle()
        val restingPixels = onRoot().captureToImage().toPixelMap()
        // At rest, pixels near the trailing edge of the card must remain pure neutral grey with
        // zero red bleed from the hidden delete button underneath.
        for (x in listOf(300, 325, 345, 355)) {
            val sample = restingPixels[x, 36]
            assertTrue(
                abs(sample.red - sample.blue) < 0.02f && abs(sample.red - cardFill.red) < 0.03f,
                "Resting card pixel at x=$x must stay neutral grey ($cardFill), got $sample",
            )
        }

        // Mid-swipe: pull 36px left so the red delete button is half exposed and half under the card.
        onRoot().performTouchInput {
            down(Offset(340f, 36f))
            moveBy(Offset(-44f, 0f))
        }
        waitForIdle()
        val midPixels = onRoot().captureToImage().toPixelMap()
        val cardRightPx = (360f + state.offsetPx).toInt()
        assertTrue(cardRightPx in 280..345, "Expected partial swipe, cardRightPx=$cardRightPx")
        // 12px inside the card's retreating right edge must stay pure grey (no red stain).
        val insideCard = midPixels[cardRightPx - 12, 36]
        assertTrue(
            abs(insideCard.red - insideCard.blue) < 0.02f && abs(insideCard.red - cardFill.red) < 0.03f,
            "Mid-swipe card interior at x=${cardRightPx - 12} must stay pure grey, got $insideCard",
        )
        // Exposed trailing region outside the card must render the red delete button.
        val exposedDelete = midPixels[350, 36]
        assertTrue(
            exposedDelete.red > 0.7f && exposedDelete.blue < 0.25f,
            "Exposed trailing button at x=350 must be red, got $exposedDelete",
        )
        onRoot().performTouchInput { up() }
    }

    @Test
    fun twoStepConfirmationRequiresSecondTapBeforeInvokingOnClick() = runSkikoComposeUiTest(size = Size(360f, 72f), density = Density(1f)) {
        val state = SwipeRevealState(initiallyRevealed = true)
        var deleted = 0
        setContent {
            COUITheme {
                SwipeRevealBox(
                    actions = listOf(
                        SwipeRevealAction(
                            icon = null,
                            description = "Delete",
                            confirmDescription = "Confirm delete",
                            requireConfirmation = true,
                            onClick = { deleted++ },
                        ),
                    ),
                    state = state,
                    style = SwipeRevealDefaults.circleActionStyle(),
                    dismissPrimaryOnClick = true,
                ) {
                    Box(Modifier.fillMaxWidth().height(72.dp))
                }
            }
        }
        waitForIdle()
        onNodeWithContentDescription("Delete").performClick()
        waitForIdle()
        assertEquals(0, deleted, "First tap must only arm confirmation")
        assertTrue(state.isRevealed, "Menu must stay open while awaiting confirmation")
        onNodeWithContentDescription("Confirm delete").assertIsDisplayed().performClick()
        waitForIdle()
        assertEquals(1, deleted, "Second tap must commit the action")
        assertFalse(state.isRevealed)
    }

    @Test
    fun leftSwipeRevealsActionsAndFullSwipeInvokesPrimary() = runSkikoComposeUiTest(size = Size(360f, 64f), density = Density(1f)) {
        val state = SwipeRevealState()
        var pinned = 0
        var deleted = 0
        setContent {
            COUITheme {
                SwipeRevealBox(
                    actions = listOf(
                        SwipeRevealAction(COUIIcons.Basic.Close, "Pin", { pinned++ }),
                        SwipeRevealAction(null, "Delete", { deleted++ }),
                    ),
                    state = state,
                    dismissOnFullSwipe = true,
                ) {
                    Box(Modifier.fillMaxWidth().height(64.dp))
                }
            }
        }
        onNodeWithContentDescription("Delete").assertDoesNotExist()
        onRoot().performTouchInput { swipe(Offset(330f, 32f), Offset(180f, 32f), 700) }
        waitForIdle()
        assertTrue(state.isRevealed)
        onNodeWithContentDescription("Pin").assertIsDisplayed()
        onNodeWithContentDescription("Delete").assertIsDisplayed()
        onNodeWithContentDescription("Pin").performClick()
        waitForIdle()
        assertEquals(1, pinned)
        assertFalse(state.isRevealed)

        // Full swipe past the 112dp menu + 40dp merge deletes directly.
        onRoot().performTouchInput { swipe(Offset(340f, 32f), Offset(80f, 32f), 700) }
        waitForIdle()
        assertEquals(1, deleted)
        assertFalse(state.isRevealed)
    }

    @Test
    fun notificationSwipeBoxCanDetachConicCornerBlurAndRim() = runSkikoComposeUiTest(size = Size(360f, 84f), density = Density(1f)) {
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Green)) {
                COUITheme {
                    NotificationSwipeBox(
                        onSettings = {},
                        onDismiss = {},
                        settingsIcon = COUIIcons.Basic.Close,
                        dismissIcon = COUIIcons.Basic.Close,
                        containerColor = Color.Blue,
                        cornerRadius = 12.dp,
                        enableBlur = false,
                        enableRim = false,
                        useConicCorner = false,
                    ) {
                        Box(Modifier.fillMaxWidth().height(84.dp))
                    }
                }
            }
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        // For 12dp Lp 3.0 squircle, 45-degree corner inset is ~2.48px, so (3, 3) is inside.
        val inside12dp = pixels[3, 3]
        assertTrue(inside12dp.blue > 0.85f && inside12dp.green < 0.15f, "Expected (3,3) inside detached 12dp corner, got $inside12dp")
    }
}
