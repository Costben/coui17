// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SWITCH_TAG = "switch"

/** The ColorOS reference density: the track is 133px wide and the thumb travels 49px. */
private val TestDensity = Density(3.5f)

/** Well under half the travel, so that a drag can start and still be nearer to where it began. */
private const val TOUCH_SLOP = 10f

private const val SLOW_STEP_MILLIS = 500L

@OptIn(ExperimentalTestApi::class)
class SwitchThumbDragTest {

    private val trackY = 42f

    @Test
    fun releasedThumbLandsOnTheNearerSide() {
        val start = SwitchDefaults.ThumbStartOffset
        val middle = SwitchDefaults.Travel / 2
        assertFalse(switchAttachChecked(start + middle - 1.dp, 0f))
        assertFalse(switchAttachChecked(start + middle, 0f), "the exact middle goes to off")
        assertTrue(switchAttachChecked(start + middle + 1.dp, 0f))
    }

    @Test
    fun aFlingOverridesTheNearerSide() {
        val fling = SwitchDefaults.FLING_VELOCITY
        assertTrue(switchAttachChecked(SwitchDefaults.ThumbStartOffset, fling))
        assertFalse(switchAttachChecked(SwitchDefaults.ThumbEndOffset, -fling))
        assertFalse(switchAttachChecked(SwitchDefaults.ThumbStartOffset, fling - 1f), "slower than a fling")
    }

    @Test
    fun thumbStretchPeaksAtAThirdOfTheTrack() {
        assertEquals(1f, switchThumbStretch(0f), 1e-4f)
        assertEquals(1.3f, switchThumbStretch(0.34725848f), 1e-4f)
        assertEquals(1f, switchThumbStretch(1f), 1e-4f)
        assertTrue(switchThumbStretch(1.1f) < 1f, "overshooting the end narrows the thumb")
    }

    @Test
    fun aTapTogglesExactlyOnce() = dragTest {
        val changes = mutableListOf<Boolean>()
        setSwitch(onChange = changes::add)
        onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(Offset(20f, trackY))
            up()
        }
        waitForIdle()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun aSemanticsClickStillToggles() = dragTest {
        val changes = mutableListOf<Boolean>()
        setSwitch(onChange = changes::add)
        onNodeWithTag(SWITCH_TAG).performClick()
        waitForIdle()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun draggingPastTheMiddleTurnsOnWhenItLands() = dragTest {
        val changes = mutableListOf<Boolean>()
        setSwitch(onChange = changes::add)
        onNodeWithTag(SWITCH_TAG).performTouchInput {
            // The thumb covers about a third of the finger's distance, so the finger crosses the track.
            down(Offset(2f, trackY))
            slowMoveTo(65f)
            slowMoveTo(130f)
            slowMoveTo(130f)
            up()
        }
        waitForIdle()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun aSlowDragShortOfTheMiddleChangesNothing() = dragTest {
        val changes = mutableListOf<Boolean>()
        setSwitch(onChange = changes::add)
        onNodeWithTag(SWITCH_TAG).performTouchInput {
            // 45px of finger is under 5dp of thumb, which stays short of the middle; a 1:1 thumb would pass it.
            down(Offset(20f, trackY))
            slowMoveTo(65f)
            slowMoveTo(65f)
            slowMoveTo(65f)
            up()
        }
        waitForIdle()
        assertEquals(emptyList(), changes, "neither a toggle nor a tap")
    }

    @Test
    fun aFastFlickShortOfTheMiddleStillTurnsOn() = dragTest {
        val changes = mutableListOf<Boolean>()
        setSwitch(onChange = changes::add)
        onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(Offset(20f, trackY))
            // 3000px/s, which the velocity scaled onto the thumb's travel reads as past a fling.
            moveTo(Offset(26f, trackY), delayMillis = 2)
            moveTo(Offset(32f, trackY), delayMillis = 2)
            moveTo(Offset(38f, trackY), delayMillis = 2)
            up()
        }
        waitForIdle()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun rightToLeftDragsTowardsTheStart() = dragTest {
        val changes = mutableListOf<Boolean>()
        setSwitch(layoutDirection = LayoutDirection.Rtl, onChange = changes::add)
        onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(Offset(130f, trackY))
            slowMoveTo(90f)
            slowMoveTo(0f)
            slowMoveTo(0f)
            up()
        }
        waitForIdle()
        assertEquals(listOf(true), changes)
    }

    @Test
    fun aDisabledSwitchIgnoresDrags() = dragTest {
        val changes = mutableListOf<Boolean>()
        setSwitch(enabled = false, onChange = changes::add)
        onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(Offset(20f, trackY))
            slowMoveTo(80f)
            up()
        }
        waitForIdle()
        assertEquals(emptyList(), changes)
    }

    @Test
    fun aRefusedChangeCanBeDraggedAgain() = dragTest {
        val changes = mutableListOf<Boolean>()
        // The state never follows the callback, so the thumb has to find its way back to off.
        setSwitch(accept = false, onChange = changes::add)
        repeat(2) {
            onNodeWithTag(SWITCH_TAG).performTouchInput {
                down(Offset(2f, trackY))
                slowMoveTo(65f)
                slowMoveTo(130f)
                slowMoveTo(130f)
                up()
            }
            waitForIdle()
        }
        assertEquals(listOf(true, true), changes)
    }

    private fun TouchInjectionScope.slowMoveTo(x: Float) {
        moveTo(Offset(x, trackY), delayMillis = SLOW_STEP_MILLIS)
    }

    private fun dragTest(block: SkikoComposeUiTest.() -> Unit) = runSkikoComposeUiTest(Size(300f, 300f), TestDensity) { block() }

    private fun SkikoComposeUiTest.setSwitch(
        enabled: Boolean = true,
        accept: Boolean = true,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onChange: (Boolean) -> Unit,
    ) = setContent {
        var checked by remember { mutableStateOf(false) }
        SwitchTestTheme(layoutDirection) {
            Switch(
                checked = checked,
                onCheckedChange = {
                    onChange(it)
                    if (accept) checked = it
                },
                modifier = Modifier.testTag(SWITCH_TAG),
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun SwitchTestTheme(layoutDirection: LayoutDirection, content: @Composable () -> Unit) {
    val base = LocalViewConfiguration.current
    val viewConfiguration = remember(base) {
        object : ViewConfiguration by base {
            override val touchSlop: Float = TOUCH_SLOP
        }
    }
    COUITheme {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection,
            LocalViewConfiguration provides viewConfiguration,
            content = content,
        )
    }
}
