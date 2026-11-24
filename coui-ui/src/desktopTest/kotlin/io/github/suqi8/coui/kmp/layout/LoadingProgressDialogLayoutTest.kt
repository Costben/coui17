// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The loading card must take the ColorOS 17 `COUIAlertDialog.Rotating` geometry: the card fills the
 * 180dp `contentMaxWidth` (`coui_alert_dialog_builder_rotate_background` has no insets), in both the
 * plain and the cancelable form, and the plain form keeps its 120dp minimum height.
 */
@OptIn(ExperimentalTestApi::class)
class LoadingProgressDialogLayoutTest {

    @Test
    fun thePlainCardFillsTheRotatingContentMaxWidth() = assertCard(cancelable = false)

    @Test
    fun theCancelableCardFillsTheRotatingContentMaxWidth() = assertCard(cancelable = true)

    private fun assertCard(cancelable: Boolean) = runSkikoComposeUiTest(
        size = Size(400f, 800f),
        density = Density(1f),
    ) {
        // The spinner loops forever, so the clock is driven by hand.
        mainClock.autoAdvance = false
        setContent {
            COUITheme {
                Box(Modifier.fillMaxSize()) {
                    LoadingDialogContentLayout(
                        show = true,
                        popupHost = { visible, content -> if (visible) content() },
                        modifier = Modifier.testTag("card"),
                        text = "Loading",
                        cancelAction = if (cancelable) DialogButtonBarAction(text = "Cancel", onClick = {}) else null,
                    )
                }
            }
        }
        mainClock.advanceTimeBy(2_000)

        val bounds = onNodeWithTag("card").getBoundsInRoot()
        assertNear(180.dp, bounds.right - bounds.left, "card width (cancelable=$cancelable)")
        if (!cancelable) {
            assertTrue(bounds.bottom - bounds.top >= 120.dp, "card height ${bounds.bottom - bounds.top}")
        }
    }
}

private fun assertNear(expected: Dp, actual: Dp, label: String) {
    assertTrue(abs(expected.value - actual.value) <= 0.5f, "$label: expected $expected, got $actual")
}
