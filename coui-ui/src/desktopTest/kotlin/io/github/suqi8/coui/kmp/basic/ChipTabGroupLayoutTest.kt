// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Check
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The `COUIChipTabLayout` geometry of [ChipTabGroup]: the pinned icon-only chip, the in-scroll
 * content padding, and the shadow layer that reaches past the row without taking its neighbours'
 * touches.
 */
@OptIn(ExperimentalTestApi::class)
class ChipTabGroupLayoutTest {

    private val tabs = listOf(ChipTabData("All"), ChipTabData("Photos"), ChipTabData("Videos"))

    @Test
    fun contentPaddingInsetsTheFirstTabInsideTheScroll() = runSkikoComposeUiTest(
        size = Size(360f, 200f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                ChipTabGroup(
                    tabs = tabs,
                    selectedIndex = 0,
                    onSelectedChange = {},
                    modifier = Modifier.fillMaxWidth().testTag("group"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
        }
        waitForIdle()

        assertEquals(360f, onNodeWithTag("group").getBoundsInRoot().width.value, 0.5f)
        assertEquals(16f, onNodeWithText("All").getBoundsInRoot().left.value, 0.5f)
    }

    @Test
    fun theIconOnlyChipIsPinnedBeforeTheTabs() = runSkikoComposeUiTest(
        size = Size(360f, 200f),
        density = Density(1f),
    ) {
        var iconClicks = 0
        var selected = 0
        setContent {
            COUITheme {
                ChipTabGroup(
                    tabs = tabs,
                    selectedIndex = selected,
                    onSelectedChange = { selected = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = COUIIcons.Basic.Check,
                    onLeadingIconClick = { iconClicks++ },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
        }
        waitForIdle()

        val iconChip = onAllNodes(isSelectable())[0]
        val iconBounds = iconChip.getBoundsInRoot()
        // COUIChipTab.onMeasure forces the icon-only chip to 32dp square; it keeps the 16dp margin.
        assertEquals(32f, iconBounds.width.value, 0.5f)
        assertEquals(32f, iconBounds.height.value, 0.5f)
        assertEquals(16f, iconBounds.left.value, 0.5f)
        // The first tab's label starts after the chip, the 8dp scroller padding and its own 12dp
        // start padding.
        val firstTab = onAllNodes(isSelectable())[1].getBoundsInRoot()
        assertEquals(16f + 32f + 8f, firstTab.left.value, 0.5f)

        iconChip.performClick()
        waitForIdle()
        assertEquals(1, iconClicks)
        assertEquals(0, selected, "the icon-only chip must not change the tab selection")
    }

    @Test
    fun theShadowLayerDoesNotTakeTouchesFromTheContentAbove() = runSkikoComposeUiTest(
        size = Size(360f, 300f),
        density = Density(1f),
    ) {
        var aboveClicks = 0
        setContent {
            COUITheme {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("above")
                            .clickable { aboveClicks++ },
                    )
                    ChipTabGroup(
                        tabs = tabs,
                        selectedIndex = 0,
                        onSelectedChange = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        waitForIdle()

        // 10dp above the row: well inside the shadow layer's outset, but the box's own area.
        val above = onNodeWithTag("above")
        val aboveHeight = above.getBoundsInRoot().height.value
        above.performTouchInput { click(Offset(20f, aboveHeight - 10f)) }
        waitForIdle()

        assertEquals(1, aboveClicks)
    }

    @Test
    fun selectingAnOffscreenTabScrollsItIntoView() = runSkikoComposeUiTest(
        size = Size(220f, 200f),
        density = Density(1f),
    ) {
        var selectedIndex by mutableIntStateOf(0)
        val sixTabs = listOf(
            ChipTabData("All"),
            ChipTabData("Photos"),
            ChipTabData("Videos"),
            ChipTabData("Documents"),
            ChipTabData("Music"),
            ChipTabData("Files"),
        )
        setContent {
            COUITheme {
                ChipTabGroup(
                    tabs = sixTabs,
                    selectedIndex = selectedIndex,
                    onSelectedChange = { selectedIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
        }
        waitForIdle()

        selectedIndex = 5
        waitForIdle()

        assertTrue(
            onNodeWithText("Files").getBoundsInRoot().right.value <= 220f,
            "selecting an offscreen tab must scroll it into view",
        )
    }

    @Test
    fun pressingATabShrinksItImmediatelyAndReleasesBackToRest() = runSkikoComposeUiTest(
        size = Size(360f, 200f),
        density = Density(1f),
    ) {
        var selected = 0
        setContent {
            COUITheme {
                ChipTabGroup(
                    tabs = tabs,
                    selectedIndex = selected,
                    onSelectedChange = { selected = it },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
        }
        waitForIdle()

        val photosNode = onAllNodes(isSelectable())[1]
        val restWidth = photosNode.getBoundsInRoot().width.value

        mainClock.autoAdvance = false
        photosNode.performTouchInput { down(center) }
        mainClock.advanceTimeBy(80)

        val pressedWidth = photosNode.getBoundsInRoot().width.value
        assertTrue(
            pressedWidth < restWidth - 1f,
            "pressing a ChipTab must shrink it immediately (rest=$restWidth, pressed=$pressedWidth)",
        )

        photosNode.performTouchInput { up() }
        mainClock.autoAdvance = true
        waitForIdle()

        assertEquals(1, selected)
        assertEquals(restWidth, photosNode.getBoundsInRoot().width.value, 0.5f)
    }

    @Test
    fun pressingADisabledTabDoesNotShrinkIt() = runSkikoComposeUiTest(
        size = Size(360f, 200f),
        density = Density(1f),
    ) {
        val mixedTabs = listOf(ChipTabData("All"), ChipTabData("Unavailable", enabled = false))
        setContent {
            COUITheme {
                ChipTabGroup(
                    tabs = mixedTabs,
                    selectedIndex = 0,
                    onSelectedChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
        }
        waitForIdle()

        val disabledNode = onAllNodes(isSelectable())[1]
        val restWidth = disabledNode.getBoundsInRoot().width.value

        mainClock.autoAdvance = false
        disabledNode.performTouchInput { down(center) }
        mainClock.advanceTimeBy(120)

        assertEquals(restWidth, disabledNode.getBoundsInRoot().width.value, 0.5f)

        disabledNode.performTouchInput { up() }
        mainClock.autoAdvance = true
        waitForIdle()
    }

    @Test
    fun horizontalScrollCancelsPressShrinkBeforeFingerUp() = runSkikoComposeUiTest(
        size = Size(180f, 200f),
        density = Density(1f),
    ) {
        var selected = 0
        val sixTabs = listOf(
            ChipTabData("All"),
            ChipTabData("Photos"),
            ChipTabData("Videos"),
            ChipTabData("Documents"),
            ChipTabData("Music"),
            ChipTabData("Files"),
        )
        setContent {
            COUITheme {
                ChipTabGroup(
                    tabs = sixTabs,
                    selectedIndex = selected,
                    onSelectedChange = { selected = it },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
        }
        waitForIdle()

        val photosNode = onAllNodes(isSelectable())[1]
        val restWidth = photosNode.getBoundsInRoot().width.value

        mainClock.autoAdvance = false
        photosNode.performTouchInput { down(center) }
        mainClock.advanceTimeBy(80)
        assertTrue(photosNode.getBoundsInRoot().width.value < restWidth - 1f)

        // Dragging horizontally past touch slop hands the gesture to horizontalScroll and cancels press.
        photosNode.performTouchInput { moveBy(Offset(-60f, 0f)) }
        mainClock.advanceTimeBy(1500)
        assertEquals(restWidth, photosNode.getBoundsInRoot().width.value, 0.5f)

        photosNode.performTouchInput { up() }
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(0, selected)
    }

    @Test
    fun tabsKeepChipHeightWhenTheParentPinsTheGroupToIt() = runSkikoComposeUiTest(
        size = Size(360f, 200f),
        density = Density(1f),
    ) {
        setContent {
            COUITheme {
                Box(Modifier.height(ChipTabDefaults.Height)) {
                    ChipTabGroup(
                        tabs = tabs,
                        selectedIndex = 0,
                        onSelectedChange = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        waitForIdle()

        // The scroller is laid out PressOutset taller on each side so a pressed tab can overshoot
        // without its edges being cut. Under an exact-height parent that outset must overflow the
        // reported height; letting the height cap reach the tabs instead shorts them by 2dp a side.
        val tabNodes = onAllNodes(isSelectable()).fetchSemanticsNodes()
        assertEquals(tabs.size, tabNodes.size)
        tabNodes.forEach { node ->
            assertEquals(ChipTabDefaults.Height.value, node.boundsInRoot.height, 0.5f)
        }
    }
}
