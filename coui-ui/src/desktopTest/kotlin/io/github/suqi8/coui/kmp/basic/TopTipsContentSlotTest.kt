// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The additive `content` slot must replace the built-in message while keeping the built-in row
 * layout, padding and trailing cluster, and it must not change the plain [TopTips.text] body when
 * no slot is supplied.
 *
 * The sliding-marquee branch of [MarqueeText] is deliberately not exercised: it animates forever,
 * and the marquee wiring under test is the `marqueeEnabled` short-circuit, which stays on the
 * plain single-line branch.
 */
@OptIn(ExperimentalTestApi::class)
class TopTipsContentSlotTest {

    @Test
    fun theCustomSlotReplacesThePlainTextBodyAndKeepsTheRowLayout() = runSkikoComposeUiTest(
        size = Size(400f, 200f),
        density = Density(1f),
    ) {
        setContent {
            TopTipsHarness(
                text = "plain message",
                contentType = ContentType.Custom,
            )
        }
        waitForIdle()

        val tips = onNodeWithTag("tips").fetchSemanticsNode().boundsInRoot
        val slot = onNodeWithTag("slot").fetchSemanticsNode().boundsInRoot
        onNodeWithText("custom slot").assertExists()
        assertTrue(
            onAllNodesWithText("plain message").fetchSemanticsNodes().isEmpty(),
            "the custom slot replaces the built-in message",
        )
        // The slot keeps the built-in content inset (coui_toptips_view_content_end_margin, 16dp).
        assertTrue(slot.left - tips.left >= 15f, "the slot keeps the row padding: $tips / $slot")
        // And the trailing cluster is still the built-in one.
        onNodeWithText("Got it").assertExists()
    }

    @Test
    fun thePlainTextBodyIsUnchangedWithoutASlot() = runSkikoComposeUiTest(
        size = Size(400f, 200f),
        density = Density(1f),
    ) {
        setContent { TopTipsHarness(text = "plain message") }
        waitForIdle()
        onNodeWithText("plain message").assertExists()
        onNodeWithTag("slot").assertDoesNotExist()
    }

    @Test
    fun marqueeEnabledStillDecidesWhetherAnOverflowingMessageScrolls() = runSkikoComposeUiTest(
        size = Size(240f, 200f),
        density = Density(1f),
    ) {
        val message = "A very long top tips message that can never fit inside the banner"
        setContent { TopTipsHarness(text = message, marqueeEnabled = false) }
        waitForIdle()
        assertEquals(
            1,
            onAllNodesWithText(message).fetchSemanticsNodes().size,
            "with the marquee switched off an overflowing message stays one ellipsized line",
        )
    }
}

private enum class ContentType { None, Custom }

@Composable
private fun TopTipsHarness(
    text: String,
    marqueeEnabled: Boolean = true,
    contentType: ContentType = ContentType.None,
) {
    COUITheme {
        TopTips(
            text = text,
            actionText = "Got it",
            onAction = {},
            marqueeEnabled = marqueeEnabled,
            modifier = Modifier
                .testTag("tips")
                .fillMaxWidth(),
            content = if (contentType == ContentType.Custom) {
                { Text(text = "custom slot", modifier = Modifier.testTag("slot")) }
            } else {
                null
            },
        )
    }
}
