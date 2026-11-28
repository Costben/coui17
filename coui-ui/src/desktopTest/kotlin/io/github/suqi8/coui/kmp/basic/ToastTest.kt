// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies ColorOS 17 [Toast] and [ToastHost] behavior: single-slot replacement without vertical
 * sliding, exact `OplusToastDefaultAnimation` enter/exit keyframes, coalescing during exit, idle
 * zero-node behavior, interactive action/dismiss handling, theme colors, and centered line height.
 */
@OptIn(ExperimentalTestApi::class)
class ToastTest {

    @Test
    fun idleHostEmitsNoNodesAndTracksState() {
        runSkikoComposeUiTest(size = Size(400f, 800f), density = Density(1f)) {
            val state = ToastHostState()
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize()) {
                        ToastHost(
                            state = state,
                            modifier = Modifier.testTag(HOST_TAG),
                        )
                    }
                }
            }
            waitForIdle()
            assertFalse(state.hasActiveToast)
            assertNull(state.currentToast)
            onNodeWithTag(HOST_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun enterHoldAndExitFollowColorOs17Timings() {
        runSkikoComposeUiTest(size = Size(400f, 800f), density = Density(1f)) {
            mainClock.autoAdvance = false
            val state = ToastHostState()
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize()) {
                        ToastHost(
                            state = state,
                            modifier = Modifier.testTag(HOST_TAG),
                            toast = { data ->
                                Toast(
                                    data = data,
                                    modifier = Modifier.testTag(TOAST_TAG),
                                    onDismiss = state::dismiss,
                                )
                            },
                        )
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            state.showToast("Saved to gallery", durationMillis = 2000L)
            mainClock.advanceTimeByFrame()
            assertTrue(state.hasActiveToast)
            val motion = assertNotNull(state.motionState)
            assertEquals(1f, motion.elevationFraction.value, 0.01f)

            // At 66ms container alpha finishes (1.0) while content alpha just started after 50ms delay.
            mainClock.advanceTimeBy(80)
            assertEquals(1f, motion.containerAlpha.value, 0.01f)
            assertTrue(motion.contentAlpha.value > 0f && motion.contentAlpha.value < 0.5f)
            assertTrue(motion.containerScale.value > 0.9f && motion.containerScale.value < 1f)

            // After 333ms total enter duration, container and content are fully settled.
            mainClock.advanceTimeBy(300)
            assertEquals(1f, motion.containerScale.value, 0.001f)
            assertEquals(1f, motion.containerAlpha.value, 0.001f)
            assertEquals(1f, motion.contentAlpha.value, 0.001f)
            assertEquals(1f, motion.elevationFraction.value, 0.001f)
            onNodeWithText("Saved to gallery").assertIsDisplayed()

            // Still holding near the end of the 2000ms hold window.
            mainClock.advanceTimeBy(1800)
            assertTrue(state.hasActiveToast)
            assertEquals(1f, motion.containerAlpha.value, 0.001f)

            // Cross into exit (at ~100ms into exit, content is fading while elevation/container wait for 150ms delay).
            mainClock.advanceTimeBy(260)
            assertTrue(motion.contentAlpha.value < 0.7f)
            assertEquals(1f, motion.elevationFraction.value, 0.01f)
            assertEquals(1f, motion.containerAlpha.value, 0.01f)

            // At ~190ms into exit, elevation has reached 0 while container alpha is still fading until 250ms.
            mainClock.advanceTimeBy(100)
            assertEquals(0f, motion.elevationFraction.value, 0.01f)
            assertTrue(motion.containerAlpha.value < 1f)

            // After full 250ms exit completes, toast clears and host emits nothing again.
            mainClock.advanceTimeBy(150)
            assertFalse(state.hasActiveToast)
            assertNull(state.currentToast)
            onNodeWithTag(HOST_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun replacementTransitionsInPlaceAndCoalescesDuringExit() {
        runSkikoComposeUiTest(size = Size(400f, 800f), density = Density(1f)) {
            mainClock.autoAdvance = false
            val state = ToastHostState()
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize()) {
                        ToastHost(
                            state = state,
                            toast = { data ->
                                Toast(
                                    data = data,
                                    modifier = Modifier.testTag(TOAST_TAG),
                                    onDismiss = state::dismiss,
                                )
                            },
                        )
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            state.showToast("First message", durationMillis = 2000L)
            mainClock.advanceTimeBy(400)
            val firstBounds = onNodeWithTag(TOAST_TAG).getUnclippedBoundsInRoot()
            assertEquals("First message", state.currentToast?.message)

            // Interrupt with Second, then send Third and Fourth while First is exiting.
            state.showToast("Second message", durationMillis = 2000L)
            mainClock.advanceTimeBy(80)
            // First is still playing its 250ms exit animation.
            assertEquals("First message", state.currentToast?.message)

            state.showToast("Third message", durationMillis = 2000L)
            state.showToast("Fourth message", durationMillis = 2000L)

            // Advance past First's remaining exit + Fourth's enter.
            mainClock.advanceTimeBy(600)
            assertEquals("Fourth message", state.currentToast?.message)
            onNodeWithText("Fourth message").assertIsDisplayed()

            // Verify single-slot bottom-center placement stays strictly identical (zero vertical sliding).
            val fourthBounds = onNodeWithTag(TOAST_TAG).getUnclippedBoundsInRoot()
            assertEquals(firstBounds.bottom.value, fourthBounds.bottom.value, 0.5f)
            assertEquals(firstBounds.top.value, fourthBounds.top.value, 0.5f)
        }
    }

    @Test
    fun actionAndDismissButtonsWorkWithoutDoubleFiring() {
        runSkikoComposeUiTest(size = Size(400f, 800f), density = Density(1f)) {
            mainClock.autoAdvance = false
            val state = ToastHostState()
            var actionCount = 0
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize()) {
                        ToastHost(state = state)
                    }
                }
            }
            mainClock.advanceTimeByFrame()

            state.showToast(
                message = "Clipboard link detected",
                durationMillis = 6000L,
                actionLabel = "Open",
                withDismissAction = true,
                onAction = { actionCount += 1 },
            )
            mainClock.advanceTimeBy(400)
            onNodeWithText("Clipboard link detected").assertIsDisplayed()
            onNodeWithText("Open").assertIsDisplayed()

            // Click action twice quickly during exit; callback must only fire once.
            onNodeWithText("Open").performClick()
            mainClock.advanceTimeBy(50)
            onNodeWithText("Open").performClick()
            assertEquals(1, actionCount)

            // Finish exit animation.
            mainClock.advanceTimeBy(300)
            assertFalse(state.hasActiveToast)

            // Show again and tap the dismiss icon; action callback must not fire.
            state.showToast(
                message = "Another link",
                durationMillis = 6000L,
                actionLabel = "Open",
                withDismissAction = true,
                onAction = { actionCount += 1 },
            )
            mainClock.advanceTimeBy(400)
            onNodeWithContentDescription(ToastDefaults.DismissContentDescription).performClick()
            mainClock.advanceTimeBy(350)
            assertEquals(1, actionCount)
            assertFalse(state.hasActiveToast)
        }
    }

    @Test
    fun defaultsMatchColorOs17LightDarkColorsAndCenteredTypography() {
        var lightColors: ToastColors? = null
        var darkColors: ToastColors? = null
        var resolvedStyle: androidx.compose.ui.text.TextStyle? = null

        runSkikoComposeUiTest(size = Size(400f, 400f), density = Density(1f)) {
            setContent {
                COUITheme(colors = lightColorScheme()) {
                    lightColors = ToastDefaults.colors()
                    resolvedStyle = ToastDefaults.textStyle()
                }
                COUITheme(colors = darkColorScheme()) {
                    darkColors = ToastDefaults.colors()
                }
            }
            waitForIdle()
        }

        val light = assertNotNull(lightColors)
        assertEquals(Color(0xFFFFFFFF), light.containerColor)
        assertEquals(Color(0xFF000000), light.contentColor)
        assertEquals(Color(0x80000000), light.spotShadowColor)
        assertEquals(Color.Transparent, light.ambientShadowColor)

        val dark = assertNotNull(darkColors)
        assertEquals(Color(0xFF333333), dark.containerColor)
        assertEquals(Color(0xE6FFFFFF), dark.contentColor)
        assertEquals(Color(0x88AAAAAA), dark.spotShadowColor)
        assertEquals(Color.Transparent, dark.ambientShadowColor)

        val style = assertNotNull(resolvedStyle)
        assertEquals(14.sp, style.fontSize)
        assertEquals(20.sp, style.lineHeight)
        assertEquals(FontWeight.Medium, style.fontWeight)
        assertEquals(
            LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
            style.lineHeightStyle,
        )
    }

    @Test
    fun plainToastEnforcesMinHeightAndMaxWidth() {
        runSkikoComposeUiTest(size = Size(400f, 400f), density = Density(1f)) {
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize()) {
                        Toast(
                            message = "OK",
                            modifier = Modifier.testTag(SHORT_TAG),
                        )
                        Toast(
                            message = "This is a very long toast message that should wrap up to two lines and stay within max width",
                            modifier = Modifier.testTag(LONG_TAG),
                        )
                    }
                }
            }
            waitForIdle()

            val shortBounds = onNodeWithTag(SHORT_TAG).getUnclippedBoundsInRoot()
            assertTrue(shortBounds.bottom - shortBounds.top >= 48.dp)

            val longBounds = onNodeWithTag(LONG_TAG).getUnclippedBoundsInRoot()
            assertTrue(longBounds.right - longBounds.left <= 328.dp + 0.5.dp)
            assertTrue(longBounds.bottom - longBounds.top >= 48.dp)
        }
    }

    @Test
    fun nonPositiveDurationThrows() {
        val state = ToastHostState()
        assertFailsWith<IllegalArgumentException> {
            state.showToast("Invalid", durationMillis = 0L)
        }
        assertFailsWith<IllegalArgumentException> {
            state.showToast("Invalid", durationMillis = -100L)
        }
    }
}

private const val HOST_TAG = "toast-host"
private const val TOAST_TAG = "toast-capsule"
private const val SHORT_TAG = "toast-short"
private const val LONG_TAG = "toast-long"
