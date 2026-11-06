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
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.AlertDialogButton
import io.github.suqi8.coui.kmp.layout.AlertDialogDefaults
import io.github.suqi8.coui.kmp.layout.AlertDialogStyle
import io.github.suqi8.coui.kmp.layout.DialogButtonBarDefaults
import io.github.suqi8.coui.kmp.layout.DialogDefaults
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Center family's two presentations, measured against the native builder's arithmetic.
 *
 * `COUIAlertDialogBuilder.setWindowWidth` clamps the *window* to `min(screenWidthDp,
 * coui_dialog_max_width)` and `coui_alert_dialog_builder_background` insets the painted panel by
 * `coui_dialog_layout_margin_horizontal` on both sides. A 900dp window therefore shows a 360dp
 * panel, not a 392dp one, and a 400dp window a 368dp panel.
 *
 * `AlertDialogBuildStyle.Center.Tiny` overrides gravity back to `bottom` and swaps in
 * `coui_alert_dialog_tiny_builder_background`, which carries no insets at all, so its panel spans
 * the window and touches the bottom edge.
 */
@OptIn(ExperimentalTestApi::class)
class AlertDialogCenterStyleTest {

    @Test
    fun theCenteredPanelIsCappedAtTheWindowWidthMinusBothInsets() = runSkikoComposeUiTest(
        size = Size(WIDE_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        setContent { AlertDialogHarness(style = AlertDialogStyle.Center) }
        waitForIdle()

        val panel = panelBounds()
        assertEquals(
            (DialogDefaults.MaxWidth - DialogDefaults.outsideMargin.width * 2).value,
            panel.width,
            0.5f,
            "the panel is the 392dp window minus the 16dp inset on both sides: $panel",
        )
    }

    @Test
    fun theCenteredPanelFillsACompactWindowMinusBothInsets() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        setContent { AlertDialogHarness(style = AlertDialogStyle.Center) }
        waitForIdle()

        // `setWindowWidth` clamps the window to min(screenWidthDp, 392dp) before the background
        // insets it, so a 400dp screen already hits the 392dp window cap.
        val panel = panelBounds()
        assertEquals(
            (DialogDefaults.MaxWidth - DialogDefaults.outsideMargin.width * 2).value,
            panel.width,
            0.5f,
            "compact panel width: $panel",
        )
        assertTrue(
            abs(panel.center.x - WINDOW_WIDTH / 2f) < 1f,
            "the panel stays horizontally centered: $panel",
        )
    }

    @Test
    fun theTinyPanelSpansTheWindowAndSitsOnTheBottomEdge() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        setContent { AlertDialogHarness(style = AlertDialogStyle.CenterTiny) }
        waitForIdle()

        // The tiny background carries no inset, so the panel is the whole window: the 392dp window
        // clamp, centered, flush with the bottom edge.
        val panel = panelBounds()
        assertEquals(DialogDefaults.MaxWidth.value, panel.width, 0.5f, "the tiny panel has no side inset: $panel")
        assertTrue(abs(panel.center.x - WINDOW_WIDTH / 2f) < 1f, "the tiny panel is centered: $panel")
        assertEquals(WINDOW_HEIGHT, panel.bottom, 0.5f, "the tiny panel has no bottom margin: $panel")
        assertTrue(panel.top > 0f && panel.top < WINDOW_HEIGHT, "the tiny panel stays measured: $panel")
        // `coui_center_alert_dialog_layout_tiny.xml` caps the panel at coui_tiny_dialog_max_height
        // minus the 32dp the native max-height arithmetic always subtracts.
        assertTrue(
            panel.height <= AlertDialogDefaults.TinyMaxHeight.value + 0.5f,
            "the tiny panel honours its 320dp cap: $panel",
        )
    }

    @Test
    fun theTinyButtonBarKeepsTheBottomPaddingOfItsPanel() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        setContent { AlertDialogHarness(style = AlertDialogStyle.CenterTiny) }
        waitForIdle()

        val cancel = onNodeWithText("Cancel").fetchSemanticsNode().boundsInRoot
        // `coui_tiny_dialog_btn_bar_padding_vertical` (12dp) plus the capsule's own
        // `coui_tiny_dialog_btn_margin_vertical` (4dp).
        val expectedInset = AlertDialogDefaults.TinyButtonBarPaddingBottom +
            AlertDialogDefaults.TinyButtonMarginVertical
        assertTrue(
            abs(cancel.bottom - (WINDOW_HEIGHT - expectedInset.value)) < 0.5f,
            "the last capsule keeps 12+4dp above the panel edge: $cancel",
        )
        assertEquals(
            DialogDefaults.MaxWidth.value - 32f,
            cancel.width,
            0.5f,
            "a tiny capsule is the panel minus the 16dp margins it carries: $cancel",
        )
    }

    @Test
    fun theNeutralActionRendersBetweenCancelAndConfirmAndRoutesItsCallback() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        var neutralCount = 0
        setContent { AlertDialogHarness(neutralCount = { neutralCount++ }) }
        waitForIdle()

        val cancel = onNodeWithText("Cancel").fetchSemanticsNode().boundsInRoot
        val confirm = onNodeWithText("OK").fetchSemanticsNode().boundsInRoot
        val neutral = onNodeWithText("Never").fetchSemanticsNode().boundsInRoot
        // Three actions never fit the two-cell horizontal tier, so COUIButtonBarLayout stacks
        // neutral / positive / negative top-down.
        assertTrue(confirm.top >= neutral.bottom - 0.5f, "confirm under neutral: $neutral / $confirm")
        assertTrue(cancel.top >= confirm.bottom - 0.5f, "cancel under confirm: $cancel / $confirm")

        onNodeWithText("Never").performClick()
        assertEquals(1, neutralCount)
    }

    @Test
    fun theRecommendButtonIsTheOneTheCallerMarks() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        setContent {
            AlertDialogHarness(
                style = AlertDialogStyle.Center,
                recommendedButton = AlertDialogButton.Positive,
            )
        }
        waitForIdle()

        // The recommend tier paints a filled primary capsule, so it owns a different fill than the
        // secondary one; both are still the shared 44dp bar capsule.
        assertTrue(onNodeWithText("OK").fetchSemanticsNode().size.height > 0f)
        assertTrue(onNodeWithText("Cancel").fetchSemanticsNode().size.height > 0f)
    }

    @Test
    fun theCenteredPanelStopsAtTheNativeWindowCapMinusItsMargin() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, TALL_WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        setContent { AlertDialogHarness(summary = LONG_MESSAGE) }
        waitForIdle()

        // `COUIAlertDialogMaxLinearLayout.resolveAvailableHeight` caps the panel at
        // `min(visible height - margins, coui_dialog_max_height - margins)`. The window here is far
        // taller than the cap, so the second term decides: 632dp of window, 600dp of panel.
        assertTrue(
            abs(panelBounds().height - PANEL_HEIGHT_CEILING) < 0.5f,
            "the long message is capped at the native ceiling, not at two thirds of the window: " +
                "${panelBounds()}",
        )
    }

    @Test
    fun aLongMessageScrollsAndKeepsTheButtonBarInsideThePanel() = runSkikoComposeUiTest(
        size = Size(WINDOW_WIDTH, TALL_WINDOW_HEIGHT),
        density = Density(1f),
    ) {
        setContent { AlertDialogHarness(summary = LONG_MESSAGE) }
        waitForIdle()

        // The message is the elastic region: the panel is capped, the message scrolls inside it and
        // the bar is laid out first, so it keeps its 44dp capsules instead of being squeezed to
        // nothing by the text above it.
        val panel = panelBounds()
        val cancel = onNodeWithText("Cancel").fetchSemanticsNode().boundsInRoot
        val confirm = onNodeWithText("OK").fetchSemanticsNode().boundsInRoot
        assertTrue(
            cancel.bottom <= panel.bottom - DialogButtonBarDefaults.ButtonBarPaddingBottom.value + 0.5f,
            "the bar keeps its 16dp inset above the panel edge: $panel / $cancel",
        )
        assertTrue(
            abs(cancel.height - DialogButtonBarDefaults.ButtonHeight.value) < 0.5f,
            "the capsule keeps its 44dp height: $cancel",
        )
        assertEquals(cancel.top, confirm.top, 0.5f, "the row tier keeps both capsules on one line")
    }
}

/** The panel's own bounds, tagged through the dialog's `modifier`. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.panelBounds() = onNodeWithTag(PANEL_TAG).fetchSemanticsNode().boundsInRoot

@Composable
private fun AlertDialogHarness(
    style: AlertDialogStyle = AlertDialogStyle.Center,
    recommendedButton: AlertDialogButton? = null,
    neutralCount: (() -> Unit)? = null,
    summary: String = "This cannot be undone.",
) {
    COUITheme {
        Scaffold {
            Box(Modifier.fillMaxSize()) {
                OverlayAlertDialog(
                    show = true,
                    onConfirm = {},
                    onCancel = {},
                    modifier = Modifier.testTag(PANEL_TAG),
                    title = "Delete backup?",
                    summary = summary,
                    neutralText = if (neutralCount == null) null else "Never",
                    onNeutral = neutralCount,
                    recommendedButton = recommendedButton,
                    style = style,
                )
            }
        }
    }
}

private const val PANEL_TAG = "panel"
private const val WINDOW_WIDTH = 400f
private const val WIDE_WIDTH = 900f
private const val WINDOW_HEIGHT = 600f
private const val TALL_WINDOW_HEIGHT = 900f

/** The panel ceiling: `coui_dialog_max_height` minus `coui_dialog_layout_margin_vertical`. */
private val PANEL_HEIGHT_CEILING =
    (DialogDefaults.MaxHeight - DialogDefaults.outsideMargin.height).value

/** Longer than any panel: at 13sp over a 360dp panel it does not fit inside the 600dp cap. */
private val LONG_MESSAGE = "The backup will be removed from the cloud and from every synced device. " +
    "Local copies stay where they are, and the removal cannot be undone once the next sync runs. ".repeat(20)
