// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.RotatingProgressIndicator
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Internal shared layout for the loading dialog variants (COUIRotatingDialogBuilder,
 * `coui_progress_dialog_rotating.xml`): a small centered card holding a rotating spinner
 * and an optional message below it. With a cancel action it takes the cancelable form
 * (`coui_cancelable_progress_dialog_rotating.xml`): an optional title, the spinner, the message
 * and a one-button bar.
 *
 * @param show Whether the dialog is currently shown.
 * @param popupHost A composable that provides the dialog container (e.g., DialogLayout or Dialog).
 * @param modifier The modifier to be applied to the dialog content.
 * @param text The optional message shown below the spinner.
 * @param textColor The color of the message text.
 * @param spinnerColor The color of the spinner.
 * @param backgroundColor The background color of the dialog card.
 * @param enableWindowDim Whether to enable window dimming.
 * @param onDismissRequest Invoked when the user taps outside or presses back. When null,
 *   the dialog cannot be dismissed by the user (matching a non-cancelable COUI dialog).
 * @param onDismissFinished Invoked when the hide animation completes.
 * @param topInset Optional top inset override forwarded to [DialogContentLayout].
 * @param title The optional title above the spinner (`COUIRotatingDialogBuilder.setDialogTitle`).
 * @param titleColor The color of [title].
 * @param cancelAction The optional cancel button (`COUIRotatingDialogBuilder.setCancelButton`).
 *   With it the dialog takes the `COUIAlertDialog.Rotating.Cancelable` form.
 */
@Composable
internal fun LoadingDialogContentLayout(
    show: Boolean,
    popupHost: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    textColor: Color = Color.Unspecified,
    spinnerColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Unspecified,
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    topInset: Dp? = null,
    title: String? = null,
    titleColor: Color = Color.Unspecified,
    cancelAction: DialogButtonBarAction? = null,
) {
    val cancelable = cancelAction != null
    val cardWidth = if (cancelable) LoadingDialogDefaults.CancelableCardWidth else LoadingDialogDefaults.CardWidth
    val outsideMargin = DialogDefaults.outsideMargin
    DialogContentLayout(
        show = show,
        titleColor = titleColor,
        summaryColor = Color.Unspecified,
        backgroundColor = backgroundColor,
        outsideMargin = outsideMargin,
        // The rotating layout manages its own margins; the card is not padded by the panel.
        insideMargin = DpSize(0.dp, 0.dp),
        // `COUIRotatingDialogBuilder.create()` always calls `setBlurBackgroundDrawable(false)` unless
        // the app opts in through `setBlurBackgroundWindow(true)`, so the card is the opaque
        // `coui_alert_dialog_background` (`?couiColorSurface`, #ffffff / #1e1e1e), never the frosted
        // dialog material. Clearing the backdrop inside the host makes the panel take its flat fill.
        popupHost = { visible, hostContent ->
            popupHost(visible) {
                CompositionLocalProvider(LocalBackdrop provides null) { hostContent() }
            }
        },
        modifier = modifier,
        title = null,
        summary = null,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        // COUI rotating dialogs are always centered with the center-dialog transitions.
        forceCentered = true,
        // `coui_alert_dialog_builder_rotate_background` is an inset with no insets, so the painted
        // card fills the whole `contentMaxWidth`; [DialogContentLayout] subtracts the regular
        // dialog's horizontal inset from `maxWidth`, which is added back here.
        maxWidth = cardWidth + outsideMargin.width * 2,
        cornerRadius = LoadingDialogDefaults.CornerRadius,
        topInset = topInset,
    ) {
        Column(
            modifier = Modifier
                .width(cardWidth)
                // coui_cancelable_progress_dialog_rotating drops the min height of the plain card.
                .heightIn(min = if (cancelable) 0.dp else LoadingDialogDefaults.CardMinHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (title != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DialogDefaults.insideMargin.width)
                        .padding(top = DialogDefaults.insideMargin.height, bottom = LoadingTitleMarginBottom),
                    text = title,
                    // COUIDialogTextAppearance.Title, as drawn by the regular dialog title.
                    style = COUITheme.textStyles.title4,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = titleColor,
                )
            }
            if (text != null || cancelable || title != null) {
                LoadingSpinner(
                    color = spinnerColor,
                    modifier = Modifier.padding(top = SpinnerMarginTop, bottom = SpinnerMarginBottom),
                )
                if (text != null) {
                    Text(
                        modifier = Modifier
                            // The plain form centres a fixed `coui_spinner_layout_width` frame in the
                            // wider card, so its message wraps within that frame.
                            .widthIn(max = if (cancelable) cardWidth else SpinnerLayoutWidth)
                            .padding(horizontal = TextMarginHorizontal)
                            .padding(bottom = if (cancelable) TextMarginBottomCancelable else TextMarginBottom),
                        text = text,
                        // progress_tips: couiTextBodyM (14sp).
                        style = COUITheme.textStyles.body2,
                        textAlign = TextAlign.Center,
                        color = textColor,
                    )
                }
            } else {
                // Without a message the spinner simply centers inside the card.
                LoadingSpinner(color = spinnerColor)
            }
            if (cancelAction != null) {
                DialogButtonBar(
                    negative = cancelAction,
                    positive = null,
                )
            }
        }
    }
}

/** The `coui_rotating_loading.json` spinner at the rotating dialog size. */
@Composable
private fun LoadingSpinner(
    color: Color,
    modifier: Modifier = Modifier,
) {
    RotatingProgressIndicator(
        modifier = modifier,
        color = color,
        size = LoadingDialogDefaults.SpinnerSize,
        ringDiameter = LoadingDialogDefaults.SpinnerRingDiameter,
        strokeWidth = LoadingDialogDefaults.SpinnerStrokeWidth,
    )
}

object LoadingDialogDefaults {
    /**
     * The width of the loading card. `COUIAlertDialog.Rotating` sets `contentMaxWidth` to
     * `coui_loading_alert_dialog_max_width`, which is 180dp on ColorOS 17 (PKT110 Settings), and the
     * card (`coui_rotate_alert_dialog_layout`, a `match_parent` `COUIAlertDialogMaxLinearLayout`
     * capped at that width) fills it. The 152dp `coui_spinner_layout_width` frame is centred
     * inside it.
     */
    val CardWidth = 180.dp

    /**
     * The width of the cancelable loading card. COUI `coui_cancelable_progress_dialog_rotating`
     * wraps its content, so the panel takes the `COUIAlertDialog.Rotating.Cancelable`
     * `contentMaxWidth`, `coui_loading_alert_dialog_max_width_cancelable` (180dp).
     */
    val CancelableCardWidth = 180.dp

    /** The minimum height of the loading card. COUI `coui_spinner_layout_min_height` (120dp). */
    val CardMinHeight = 120.dp

    /**
     * The corner radius of the loading card. `coui_rotate_alert_dialog_layout` is a
     * `COUIAlertDialogMaxLinearLayout` without `clip_radius_root`, so it takes the regular dialog
     * radius, `coui_round_corner_xxxxl_radius_large_16_1` (45dp) on COUI 17.
     */
    val CornerRadius = DialogDefaults.CornerRadius

    /**
     * The size of the rotating spinner. COUI `coui_spinner_loading_anim_width/height` (36dp).
     */
    val SpinnerSize = 36.dp

    /**
     * The ring diameter of the rotating spinner: the 78-unit ring of the 84-unit
     * `coui_rotating_loading.json` comp scaled to [SpinnerSize].
     */
    val SpinnerRingDiameter = 33.43.dp

    /**
     * The stroke width of the rotating spinner: the 6-unit stroke of the 84-unit
     * `coui_rotating_loading.json` comp scaled to [SpinnerSize].
     */
    val SpinnerStrokeWidth = 2.57.dp

    /**
     * The default color of the message below the spinner. `coui_progress_dialog_rotating` gives
     * `progress_tips` `?attr/couiColorLabelSecondary` (`coui_color_label_secondary`, `#8a000000`,
     * night `#8affffff`), the theme's [onSurfaceSecondary][io.github.suqi8.coui.kmp.theme.Colors]
     * slot.
     */
    @Composable
    fun textColor() = COUITheme.colorScheme.onSurfaceSecondary

    /**
     * The default color of the spinner. `coui_rotating_loading.json` (light) and
     * `coui_rotating_loading_night.json` (dark) stroke the arc in black / white at opacity 85, and
     * the animation renderer replaces the paint alpha with that stroke opacity, so the arc renders
     * as `#D9000000` / `#D9FFFFFF`.
     */
    @Composable
    fun spinnerColor() = COUITheme.colorScheme.onBackground.copy(alpha = ROTATING_SPINNER_OPACITY)
}

/** The stroke opacity of `coui_rotating_loading.json` (`"o": 85`). */
private const val ROTATING_SPINNER_OPACITY = 0.85f

/**
 * The plain form's content frame, `coui_spinner_layout_width` (152dp): the root `FrameLayout` of
 * `coui_progress_dialog_rotating`, centred inside the [LoadingDialogDefaults.CardWidth] card.
 */
private val SpinnerLayoutWidth = 152.dp

/** COUI `coui_spinner_margin_top` (28dp). */
private val SpinnerMarginTop = 28.dp

/** COUI `coui_spinner_margin_bottom` (12dp). */
private val SpinnerMarginBottom = 12.dp

/** COUI `coui_spinner_text_margin_bottom` (28dp). */
private val TextMarginBottom = 28.dp

/** COUI `coui_cancelable_progress_dialog_rotating`: the message bottom margin above the button bar. */
private val TextMarginBottomCancelable = 16.dp

/** COUI `coui_alert_dialog_title_margin_bottom` (COUIAlertDialogTitleTemplateStyle). */
private val LoadingTitleMarginBottom = 6.dp

/** COUI `coui_spinner_text_margin_horizontal` (16dp). */
private val TextMarginHorizontal = 16.dp
