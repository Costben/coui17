// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.window.WindowListPopup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ROTATED_POPUP_TAG = "rotated-popup"

@OptIn(ExperimentalTestApi::class)
class ListPopupRotationTest {
    @Test
    fun rotatedOverlayPopupStaysOnScreenInteractiveAndDismissesOnOutsideTap() {
        val rotations = listOf(
            PopupMenuRotation.None,
            PopupMenuRotation.Degrees90,
            PopupMenuRotation.Degrees270,
            PopupMenuRotationDefaults.classifyDegrees(720f),
        )
        for (rotation in rotations) {
            runSkikoComposeUiTest(size = Size(360f, 800f), density = Density(1f)) {
                var show by mutableStateOf(true)
                var clicked by mutableStateOf<String?>(null)
                var dismissed by mutableStateOf(false)
                setContent {
                    COUITheme {
                        Scaffold {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 120.dp),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Box(Modifier.size(200.dp, 48.dp)) {
                                    val positionProvider = remember(rotation) {
                                        popupMenuRotationPositionProvider(rotation)
                                    }
                                    OverlayListPopup(
                                        show = show,
                                        popupPositionProvider = positionProvider,
                                        popupModifier = Modifier
                                            .popupMenuRotation(rotation)
                                            .testTag(ROTATED_POPUP_TAG),
                                        onDismissRequest = {
                                            dismissed = true
                                            show = false
                                        },
                                    ) {
                                        ListPopupColumn {
                                            listOf("Copy", "Share", "Delete").forEachIndexed { index, label ->
                                                DropdownImpl(
                                                    item = DropdownItem(text = label),
                                                    optionSize = 3,
                                                    isSelected = false,
                                                    index = index,
                                                    groupKey = 0,
                                                    onSelectedIndexChange = { clicked = label },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                waitForIdle()

                val bounds = onNodeWithTag(ROTATED_POPUP_TAG).fetchSemanticsNode().boundsInWindow
                assertTrue(bounds.width > 50f && bounds.height > 50f, "Popup must have non-zero bounds for $rotation: $bounds")
                assertTrue(
                    bounds.left >= 0f && bounds.right <= 360f && bounds.top >= 0f && bounds.bottom <= 800f,
                    "Rotated popup ($rotation) must stay inside the 360x800 viewport, got $bounds",
                )

                // Clicking an item inside the rotated popup must still trigger selection.
                onNodeWithText("Share").performClick()
                waitForIdle()
                assertEquals("Share", clicked, "Row click must work at rotation $rotation")

                // Tapping near the bottom of the portrait screen (outside the rotated panel) must dismiss.
                onAllNodes(isRoot()).onLast().performTouchInput {
                    down(Offset(180f, 760f))
                    up()
                }
                waitForIdle()
                assertTrue(dismissed, "Outside tap must dismiss rotated popup ($rotation)")
                assertFalse(show)
            }
        }
    }

    @Test
    fun rotatedWindowPopupStaysOnScreenAndDismissesOnOutsideTap() {
        for (rotation in listOf(PopupMenuRotation.Degrees90, PopupMenuRotation.Degrees270)) {
            runSkikoComposeUiTest(size = Size(360f, 800f), density = Density(1f)) {
                var show by mutableStateOf(true)
                var dismissed by mutableStateOf(false)
                setContent {
                    COUITheme {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 120.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(Modifier.size(200.dp, 48.dp)) {
                                val positionProvider = remember(rotation) {
                                    popupMenuRotationPositionProvider(rotation)
                                }
                                WindowListPopup(
                                    show = show,
                                    popupPositionProvider = positionProvider,
                                    popupModifier = Modifier
                                        .popupMenuRotation(rotation)
                                        .testTag(ROTATED_POPUP_TAG),
                                    onDismissRequest = {
                                        dismissed = true
                                        show = false
                                    },
                                ) {
                                    ListPopupColumn {
                                        listOf("Copy", "Share", "Delete").forEachIndexed { index, label ->
                                            DropdownImpl(
                                                item = DropdownItem(text = label),
                                                optionSize = 3,
                                                isSelected = false,
                                                index = index,
                                                groupKey = 0,
                                                onSelectedIndexChange = {},
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                waitForIdle()

                val bounds = onNodeWithTag(ROTATED_POPUP_TAG).fetchSemanticsNode().boundsInWindow
                assertTrue(
                    bounds.left >= 0f && bounds.right <= 360f && bounds.top >= 0f && bounds.bottom <= 800f,
                    "Rotated WindowListPopup ($rotation) must stay inside the 360x800 viewport, got $bounds",
                )

                onAllNodes(isRoot()).onLast().performTouchInput {
                    down(Offset(180f, 760f))
                    up()
                }
                waitForIdle()
                assertTrue(dismissed, "Outside tap must dismiss rotated WindowListPopup ($rotation)")
                assertFalse(show)
            }
        }
    }
}
