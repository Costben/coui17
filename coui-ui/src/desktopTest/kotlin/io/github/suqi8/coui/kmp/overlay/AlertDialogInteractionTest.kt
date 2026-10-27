// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.DialogButtonBarDefaults
import io.github.suqi8.coui.kmp.layout.DialogDefaults
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The center-style alert variant must present itself centered instead of using the bottom-anchored
 * presentation of [OverlayDialog], and its button row must route to `onConfirm` / `onCancel`.
 *
 * The window is deliberately compact (400x600) so `DialogDefaults.isLargeScreen` stays false: a
 * variant that merely inherited the large-screen centering would be bottom-anchored here, and the
 * panel would end up in the lower third instead of the middle.
 */
@OptIn(ExperimentalTestApi::class)
class AlertDialogInteractionTest {

    @Test
    fun thePanelOpensCenteredAndTheButtonRowReportsItsCallbacks() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        var confirmCount = 0
        var cancelCount = 0
        setContent {
            AlertDialogHarness(
                onConfirm = { confirmCount++ },
                onCancel = { cancelCount++ },
            )
        }
        waitForIdle()

        val title = onNodeWithText("Delete backup?").fetchSemanticsNode().boundsInRoot
        assertTrue(
            abs(title.center.x - WINDOW_WIDTH / 2f) < 1f,
            "the centered panel must be horizontally centered: $title",
        )
        val panelCenter = panelCenterY()
        assertTrue(
            abs(panelCenter - centeredPanelCenterY()) < 2f,
            "the panel must be vertically centered, not bottom-anchored: $panelCenter",
        )
        onNodeWithTag("body").assertExists()

        onNodeWithText("OK").performClick()
        assertEquals(1, confirmCount)
        onNodeWithText("Cancel").performClick()
        assertEquals(1, cancelCount)
    }

    @Test
    fun aTallBodyStaysInsideTheCenteredPanelAboveTheButtonRow() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        setContent {
            AlertDialogHarness(
                onConfirm = {},
                onCancel = {},
                bodyLines = 8,
            )
        }
        waitForIdle()

        val body = onNodeWithTag("body").fetchSemanticsNode().boundsInRoot
        val confirm = onNodeWithText("OK").fetchSemanticsNode().boundsInRoot
        assertTrue(body.top > 0f, "the centered panel must stay inside the window: $body")
        assertTrue(
            confirm.bottom <= WINDOW_HEIGHT,
            "the centered panel must stay inside the window: $confirm",
        )
        // The confirm row follows the body, so a growing body must push it, not overlap it.
        assertTrue(confirm.top >= body.bottom, "the button row sits below the body: $body / $confirm")
        val panelCenter = panelCenterY()
        assertTrue(
            abs(panelCenter - centeredPanelCenterY()) < 2f,
            "a tall body must not break the centered presentation: $panelCenter",
        )
    }
}

/**
 * The vertical centre of the panel. The panel itself carries no semantics node, so its bounds are
 * reconstructed from two descendants: `DialogContent` indents the title by `insideMargin.height`
 * from the panel top, and the merged confirm button ends `ButtonBarPaddingBottom` above the panel
 * bottom — the bar reserves that inset for the panel's own rounded footer.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.panelCenterY(): Float {
    val inset = with(density) { DialogDefaults.insideMargin.height.toPx() }
    val barBottom = with(density) { DialogButtonBarDefaults.ButtonBarPaddingBottom.toPx() }
    val title = onNodeWithText("Delete backup?").fetchSemanticsNode().boundsInRoot
    val confirm = onNodeWithText("OK").fetchSemanticsNode().boundsInRoot
    return (title.top - inset + confirm.bottom + barBottom) / 2f
}

/** The centre of the area the panel centres in: the window minus the bottom outside margin. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.centeredPanelCenterY(): Float = (WINDOW_HEIGHT - with(density) { DialogDefaults.outsideMargin.height.toPx() }) / 2f

@Composable
private fun AlertDialogHarness(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    bodyLines: Int = 0,
) {
    COUITheme {
        Scaffold {
            Box(Modifier.fillMaxSize()) {
                OverlayAlertDialog(
                    show = true,
                    onConfirm = onConfirm,
                    onCancel = onCancel,
                    title = "Delete backup?",
                    summary = "This cannot be undone.",
                    content = {
                        Text(
                            modifier = Modifier.testTag("body"),
                            text = if (bodyLines == 0) {
                                "extra body"
                            } else {
                                "extra body" + "\nmore body".repeat(bodyLines)
                            },
                        )
                    },
                )
            }
        }
    }
}

private const val WINDOW_WIDTH = 400f
private const val WINDOW_HEIGHT = 600f
