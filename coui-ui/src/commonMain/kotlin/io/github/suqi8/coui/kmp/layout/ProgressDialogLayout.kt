// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.LinearProgressIndicator
import io.github.suqi8.coui.kmp.basic.ProgressIndicatorColors
import io.github.suqi8.coui.kmp.basic.ProgressIndicatorDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Internal shared layout for the horizontal progress dialog variants
 * (COUIRotatingDialogBuilder / `coui_progress_dialog_horizontal.xml`): the small centered card of
 * the rotating loading dialog, with the rotating spinner replaced by a horizontal progress bar
 * and an optional message below it.
 *
 * The bar is determinate while [progress] is non-null and indefinite when it is null, mirroring
 * `COUIProgressBar`'s two modes; it reuses [LinearProgressIndicator] so both modes carry the
 * `ProgressBarRangeInfo` semantics.
 *
 * @param show Whether the dialog is currently shown.
 * @param popupHost A composable that provides the dialog container (e.g., DialogLayout or Dialog).
 * @param modifier The modifier to be applied to the dialog content.
 * @param progress The current progress between 0.0f and 1.0f, or null for an indefinite bar.
 * @param text The optional message shown below the bar.
 * @param colors The [ProgressDialogColors] of the message and the bar.
 * @param backgroundColor The background color of the dialog card.
 * @param enableWindowDim Whether to enable window dimming.
 * @param onDismissRequest Invoked when the user taps outside or presses back. When null,
 *   the dialog cannot be dismissed by the user (matching a non-cancelable COUI dialog).
 * @param onDismissFinished Invoked when the hide animation completes.
 * @param topInset Optional top inset override forwarded to [DialogContentLayout].
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
internal fun ProgressDialogContentLayout(
    show: Boolean,
    popupHost: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    text: String? = null,
    colors: ProgressDialogColors = ProgressDialogDefaults.progressDialogColors(),
    backgroundColor: Color = Color.Unspecified,
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    topInset: Dp? = null,
) {
    DialogContentLayout(
        show = show,
        titleColor = Color.Unspecified,
        summaryColor = Color.Unspecified,
        backgroundColor = backgroundColor,
        outsideMargin = DialogDefaults.outsideMargin,
        // The progress layout manages its own margins; the card is not padded by the panel.
        insideMargin = DpSize(0.dp, 0.dp),
        popupHost = popupHost,
        modifier = modifier,
        title = null,
        summary = null,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        // COUI progress dialogs are always centered with the center-dialog transitions.
        forceCentered = true,
        cornerRadius = ProgressDialogDefaults.CornerRadius,
        topInset = topInset,
    ) {
        Column(
            modifier = Modifier
                .width(ProgressDialogDefaults.CardWidth)
                .heightIn(min = ProgressDialogDefaults.CardMinHeight)
                .padding(horizontal = ProgressDialogDefaults.ContentHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progress,
                colors = colors.progressColors,
                height = ProgressDialogDefaults.ProgressBarHeight,
            )
            if (text != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ProgressDialogDefaults.TextMarginTop),
                    text = text,
                    fontSize = COUITheme.textStyles.body2.fontSize,
                    textAlign = TextAlign.Center,
                    color = colors.textColor,
                )
            }
        }
    }
}

@ExperimentalCOUIApi
object ProgressDialogDefaults {

    /**
     * The width of the progress card. COUI has no separate token for the horizontal card; the port
     * reuses the rotating card width `coui_spinner_layout_width` (152dp) of
     * [LoadingDialogDefaults.CardWidth].
     */
    val CardWidth = LoadingDialogDefaults.CardWidth

    /**
     * The minimum height of the progress card. COUI
     * `coui_spinner_layout_min_height` (120dp), shared with [LoadingDialogDefaults.CardMinHeight].
     */
    val CardMinHeight = LoadingDialogDefaults.CardMinHeight

    /**
     * The corner radius of the progress card. COUI `coui_round_corner_m_radius`, ColorOS 17's
     * successor `coui_round_corner_m_radius_normal_16_1` (15dp), shared with
     * [LoadingDialogDefaults.CornerRadius].
     */
    val CornerRadius = LoadingDialogDefaults.CornerRadius

    /**
     * The horizontal padding inside the card, so the bar does not touch the card edge. Port choice;
     * the native template is a plain `LinearProgressIndicator` inside the rotating card frame.
     */
    val ContentHorizontalPadding = 16.dp

    /**
     * The height of the horizontal bar. COUI `coui_loading_dialog_progress_height` (4dp), the
     * default height of [LinearProgressIndicator].
     */
    val ProgressBarHeight = ProgressIndicatorDefaults.DefaultLinearProgressIndicatorHeight

    /** The gap between the bar and the message below it. */
    val TextMarginTop = 12.dp

    /**
     * The default [ProgressDialogColors].
     *
     * @param textColor The color of the message below the bar.
     * @param progressColors The colors of the horizontal bar, reused from
     *   `ProgressIndicatorDefaults.progressIndicatorColors()`.
     */
    @OptIn(ExperimentalCOUIApi::class)
    @Composable
    fun progressDialogColors(
        textColor: Color = COUITheme.colorScheme.onBackground,
        progressColors: ProgressIndicatorColors = ProgressIndicatorDefaults.progressIndicatorColors(),
    ): ProgressDialogColors = remember(textColor, progressColors) {
        ProgressDialogColors(
            textColor = textColor,
            progressColors = progressColors,
        )
    }

    /**
     * The default color of the message below the bar. COUI progress_tips uses
     * couiColorLabelPrimary, the same `onBackground` label color of
     * [LoadingDialogDefaults.textColor].
     */
    @Composable
    fun textColor() = COUITheme.colorScheme.onBackground
}

/**
 * Colors used by a horizontal progress dialog.
 *
 * @param textColor The color of the message below the bar.
 * @param progressColors The colors of the bar track and fill.
 */
@ExperimentalCOUIApi
@Immutable
data class ProgressDialogColors(
    val textColor: Color,
    val progressColors: ProgressIndicatorColors,
)
