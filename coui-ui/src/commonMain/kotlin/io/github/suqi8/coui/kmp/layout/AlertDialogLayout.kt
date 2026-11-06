// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.TextButtonColors
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Internal shared layout for the centered alert dialog variants, the COUI
 * `AlertDialogBuildStyle.Center` family: the regular title / message / content panel, closed by a
 * cancel / neutral / confirm button bar.
 *
 * [AlertDialogStyle.Center] is `coui_center_alert_dialog_layout.xml` drawn with window gravity
 * `center` and `Animation.COUI.Dialog.Alpha` — the 250ms scale 0.8 → 1 with the matching fade-in,
 * and a 150ms fade-out. `COUIAlertDialogBuilder` keeps that gravity on every window size (the
 * `Center` style does not set `isForceCenterStyleInLargeScreen`), so the panel is force-centered
 * here as well.
 *
 * [AlertDialogStyle.CenterTiny] is `coui_center_alert_dialog_layout_tiny.xml`. Despite the name,
 * `AlertDialogBuildStyle.Center.Tiny` overrides `android:gravity` back to `bottom` and keeps
 * `Animation.COUI.Dialog` (the 250ms bottom slide), caps the window at
 * `coui_tiny_dialog_max_height` (352dp), drops both the 16dp side inset and the bottom margin
 * (the tiny background is a bare inset-free shape) and closes with the full-width tiny capsules of
 * `coui_center_alert_dialog_button_panel_tiny.xml`.
 *
 * `.Rotate`, `.ListDialog` and `.PhotoPage` are not ported. None of the system apps on the
 * reference build reference the first two; `.PhotoPage` pairs the *bottom* layout with center
 * gravity and is used by the gallery's photo page alone.
 *
 * @param show Whether the dialog is currently shown.
 * @param popupHost A composable that provides the dialog container (e.g., DialogLayout or Dialog).
 * @param onConfirm Invoked when the confirm (positive) button is clicked.
 * @param onCancel Invoked when the cancel (negative) button is clicked, the user taps outside, or
 *   presses back (COUI reports the back key as the negative selection).
 * @param modifier The modifier to be applied to the dialog content.
 * @param title The title of the dialog.
 * @param summary The message of the dialog.
 * @param confirmText The label of the confirm (positive) button.
 * @param cancelText The label of the cancel (negative) button.
 * @param neutralText The label of the optional third (neutral) button. The bar only appears when it
 *   is not null, matching `setNeutralButton`.
 * @param onNeutral Invoked when the neutral button is clicked.
 * @param recommendedButton The action `COUIButtonBarLayout` renders as its recommend (primary)
 *   button, i.e. the `boolean` overload of `setPositiveButton` / `setNegativeButton` /
 *   `setNeutralButton` in the native builder.
 * @param style Which member of the Center family to draw.
 * @param colors The [AlertDialogColors] of the title, message and panel.
 * @param enableWindowDim Whether to enable window dimming.
 * @param onDismissFinished Invoked when the hide animation completes; not invoked if the hide is
 *   cancelled mid-flight.
 * @param cornerRadius Optional corner radius override. If null, [AlertDialogDefaults.CornerRadius].
 * @param maxWidth The maximum width of the dialog window; the panel is 16dp narrower on each side.
 * @param topInset Optional top inset override forwarded to [DialogContentLayout].
 * @param content An optional extra body slot rendered above the button bar. The title and summary
 *   are rendered by the panel itself, exactly like [DialogContentLayout].
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
internal fun AlertDialogContentLayout(
    show: Boolean,
    popupHost: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    confirmText: String = AlertDialogDefaults.ConfirmText,
    cancelText: String = AlertDialogDefaults.CancelText,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    recommendedButton: AlertDialogButton? = null,
    style: AlertDialogStyle = AlertDialogStyle.Center,
    colors: AlertDialogColors = AlertDialogDefaults.alertDialogColors(),
    enableWindowDim: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    cornerRadius: Dp? = null,
    maxWidth: Dp = AlertDialogDefaults.MaxWidth,
    topInset: Dp? = null,
    buttonColors: TextButtonColors = DialogButtonBarDefaults.buttonColors(),
    recommendColors: TextButtonColors = ButtonDefaults.textButtonColorsPrimary(),
    content: (@Composable () -> Unit)? = null,
) {
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val currentOnNeutral by rememberUpdatedState(onNeutral)
    val tiny = style == AlertDialogStyle.CenterTiny

    val requestCancel = remember { { currentOnCancel() } }
    val cancelAction = remember(cancelText, requestCancel, recommendedButton) {
        DialogButtonBarAction(
            text = cancelText,
            onClick = requestCancel,
            recommended = recommendedButton == AlertDialogButton.Negative,
        )
    }
    val confirmAction = remember(confirmText, recommendedButton) {
        DialogButtonBarAction(
            text = confirmText,
            onClick = { currentOnConfirm() },
            recommended = recommendedButton == AlertDialogButton.Positive,
        )
    }
    val neutralAction = if (neutralText == null) {
        null
    } else {
        remember(neutralText, recommendedButton) {
            DialogButtonBarAction(
                text = neutralText,
                onClick = { currentOnNeutral?.invoke() },
                recommended = recommendedButton == AlertDialogButton.Neutral,
            )
        }
    }

    DialogContentLayout(
        show = show,
        titleColor = colors.titleColor,
        summaryColor = colors.summaryColor,
        backgroundColor = colors.backgroundColor,
        // The tiny background (`coui_alert_dialog_tiny_builder_background`) is a bare
        // `coui_alert_dialog_tiny_background` with no insets: no 16dp side margin and no 32dp
        // bottom margin, so the panel spans the window and touches the bottom edge.
        outsideMargin = if (tiny) DpSize(0.dp, 0.dp) else DialogDefaults.outsideMargin,
        insideMargin = if (tiny) AlertDialogDefaults.TinyInsideMargin else DialogDefaults.insideMargin,
        popupHost = popupHost,
        modifier = modifier,
        title = title,
        summary = summary,
        enableWindowDim = enableWindowDim,
        onDismissRequest = requestCancel,
        onDismissFinished = onDismissFinished,
        // The tiny tier is a bottom dialog: `AlertDialogBuildStyle.Center.Tiny` resets gravity to
        // `bottom` and keeps `Animation.COUI.Dialog`, and its inset-free panel has to reach the
        // window edge.
        defaultWindowInsetsPadding = !tiny,
        // `COUIAlertDialogBuilder` keeps the centered presentation on every window size, so the
        // large-screen auto switch of DialogContentLayout is bypassed.
        forceCentered = !tiny,
        largeScreen = if (tiny) false else null,
        maxWidth = maxWidth,
        cornerRadius = cornerRadius,
        bottomCornerRadius = if (tiny) 0.dp else null,
        maxHeight = if (tiny) AlertDialogDefaults.TinyMaxHeight else DialogDefaults.MaxHeight,
        titleFontSize = if (tiny) {
            AlertDialogDefaults.TinyTitleFontSize
        } else {
            COUITheme.textStyles.title4.fontSize
        },
        messageFontSize = if (tiny) {
            AlertDialogDefaults.TinyMessageFontSize
        } else {
            COUITheme.textStyles.footnote1.fontSize
        },
        titleBottomMargin = if (tiny) AlertDialogDefaults.TinyTitleMarginBottom else null,
        messageTopPadding = if (tiny) AlertDialogDefaults.TinyMessageMarginTop else null,
        messageBottomPadding = if (tiny) AlertDialogDefaults.TinyMessageMarginBottom else null,
        topInset = topInset,
    ) {
        content?.invoke()
        DialogButtonBar(
            negative = cancelAction,
            positive = confirmAction,
            neutral = neutralAction,
            tiny = tiny,
            // Only the custom panel carries a bottom inset: `coui_center_alert_dialog_layout.xml`
            // puts `coui_alert_dialog_customer_layout_padding_bottom` (8dp) on `customPanel` and
            // nothing at all between the message scroll view and the button panel, whose own
            // `coui_alert_dialog_scroll_padding_bottom_message` already supplies that gap. Adding it
            // unconditionally pushed the bar 8dp away from the message and made the panel 8dp taller
            // than the real dialog. The tiny layout has no such padding at all.
            modifier = Modifier.padding(
                top = when {
                    // `COUIAlertDialogBuilder.initSingleContentPadding` pads a tiny bar 12dp down
                    // from the panel top when the dialog shows nothing above it.
                    tiny -> if (title == null && summary == null && content == null) {
                        AlertDialogDefaults.TinyButtonBarPaddingBottom
                    } else {
                        0.dp
                    }

                    content == null -> 0.dp

                    else -> AlertDialogDefaults.ButtonBarMarginTop
                },
            ),
            colors = buttonColors,
            recommendColors = recommendColors,
        )
    }
}

@ExperimentalCOUIApi
object AlertDialogDefaults {

    /** The default confirm (positive) button label. */
    val ConfirmText = "OK"

    /** The default cancel (negative) button label. */
    val CancelText = "Cancel"

    /**
     * The default corner radius of the centered panel. COUI
     * `coui_round_corner_xxl_radius`, the same [DialogDefaults.CornerRadius] the centered
     * presentation of [DialogContentLayout] uses.
     */
    val CornerRadius = DialogDefaults.CornerRadius

    /**
     * The default upper bound on the dialog *window* width. COUI `coui_dialog_max_width`, shared
     * with [DialogDefaults.MaxWidth]; the painted panel is 2 × `coui_dialog_layout_margin_horizontal`
     * narrower.
     */
    val MaxWidth = DialogDefaults.MaxWidth

    /**
     * The gap between a custom content panel and the button bar. COUI
     * `coui_alert_dialog_customer_layout_padding_bottom` (8dp), the `android:paddingBottom` of
     * `customPanel` in `coui_center_alert_dialog_layout.xml`.
     *
     * It applies only when the dialog has custom content. A title + message dialog stacks the
     * button panel straight under the message scroll view, whose own
     * `coui_alert_dialog_scroll_padding_bottom_message` (16dp) is the whole gap.
     */
    val ButtonBarMarginTop = 8.dp

    /**
     * The height cap of the tiny panel.
     *
     * `coui_tiny_dialog_max_height` is 352dp, but `COUIAlertDialogMaxLinearLayout.setMarginBottom`
     * still subtracts the 32dp `coui_dialog_layout_margin_vertical` from `mMaxHeight` even though
     * the tiny background carries no inset at all, so the panel the tiny style actually paints tops
     * out at 320dp.
     */
    val TinyMaxHeight = 320.dp

    /**
     * The horizontal and top padding of the tiny title:
     * `coui_tiny_dialog_message_padding_horizontal` (16dp) on both sides plus
     * `COUIAlertDialogTitleTemplateStyle.Tiny`'s 16dp `layout_marginTop`.
     */
    val TinyInsideMargin = DpSize(16.dp, 16.dp)

    /** `COUIAlertDialogTitleTemplateStyle.Tiny`'s `layout_marginBottom` (8dp). */
    val TinyTitleMarginBottom = 8.dp

    /**
     * `COUIDialogTextAppearance.Title.Tiny`'s `android:textSize` (18dp); the non-tiny title follows
     * `couiTextHeadlineS` through `title4`.
     */
    val TinyTitleFontSize = 18.sp

    /** `COUIAlertDialogMessageStyle.Tiny`'s `android:textSize` (14dp). */
    val TinyMessageFontSize = 14.sp

    /** `center_dialog_scroll_padding_top` (4dp), the `android:paddingTop` of the tiny message scroller. */
    val TinyMessageMarginTop = 4.dp

    /** `coui_tiny_dialog_scroll_padding_bottom` (8dp), the scroller's `android:paddingBottom`. */
    val TinyMessageMarginBottom = 8.dp

    /** `coui_tiny_dialog_btn_bar_padding_vertical` (12dp), the bottom padding of the tiny bar. */
    val TinyButtonBarPaddingBottom = 12.dp

    /** `coui_tiny_dialog_btn_margin_vertical` (4dp), the margin above and below a tiny capsule. */
    val TinyButtonMarginVertical = 4.dp

    /** `coui_alert_dialog_button_color_tiny`, the fill of a tiny capsule. */
    val TinyButtonFill = Color(0xFFE6E6E6)

    /** `coui_alert_dialog_button_text_color_tiny`, the label of a tiny capsule. */
    val TinyButtonTextColor = Color(0xFF000000)

    /**
     * The default [AlertDialogColors]. Title and message reuse the shared dialog label colors and
     * the panel reuses the shared dialog background.
     *
     * @param titleColor The color of the title.
     * @param summaryColor The color of the message.
     * @param backgroundColor The background color of the panel.
     */
    @OptIn(ExperimentalCOUIApi::class)
    @Composable
    fun alertDialogColors(
        titleColor: Color = DialogDefaults.titleColor(),
        summaryColor: Color = DialogDefaults.summaryColor(),
        backgroundColor: Color = DialogDefaults.backgroundColor(),
    ): AlertDialogColors = remember(titleColor, summaryColor, backgroundColor) {
        AlertDialogColors(
            titleColor = titleColor,
            summaryColor = summaryColor,
            backgroundColor = backgroundColor,
        )
    }

    /**
     * The default color of the title, kept for parity with [DialogDefaults.titleColor].
     */
    @Composable
    fun titleColor() = DialogDefaults.titleColor()

    /**
     * The default color of the message, kept for parity with [DialogDefaults.summaryColor].
     */
    @Composable
    fun summaryColor() = DialogDefaults.summaryColor()

    /**
     * The default background color of the panel, kept for parity with
     * [DialogDefaults.backgroundColor].
     */
    @Composable
    fun backgroundColor() = DialogDefaults.backgroundColor()

    /**
     * The colours of a tiny capsule.
     *
     * `COUIAlertDialogTinyButton.Normal` paints `coui_alert_dialog_button_color_tiny`
     * (`#E6E6E6`) under `coui_alert_dialog_button_text_color_tiny` (`#000000`); neither has a night
     * variant, so the tiny tier keeps the same light capsules in dark theme. Only the disabled fill
     * follows the theme, through `Widget.COUI.Button.Large.TinyFull`'s
     * `disabledColor=@color/coui_button_neutral_color_disable` (`#0F000000` / `#4CFFFFFF`). The
     * label colour is *not* dimmed when disabled: `COUIAlertDialogTinyButton.Normal` overrides
     * `android:textColor` unconditionally.
     */
    @OptIn(ExperimentalCOUIApi::class)
    @Composable
    fun tinyButtonColors(): TextButtonColors {
        val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
        val disabledFill = if (isDark) Color(0x4CFFFFFF) else Color(0x0F000000)
        return ButtonDefaults.textButtonColorsBorderless(
            color = TinyButtonFill,
            disabledColor = disabledFill,
            textColor = TinyButtonTextColor,
            disabledTextColor = TinyButtonTextColor,
        )
    }
}

/**
 * Colors used by a centered alert dialog.
 *
 * @param titleColor The color of the title.
 * @param summaryColor The color of the message.
 * @param backgroundColor The background color of the panel.
 */
@ExperimentalCOUIApi
@Immutable
data class AlertDialogColors(
    val titleColor: Color,
    val summaryColor: Color,
    val backgroundColor: Color,
)

/**
 * The alert-dialog build styles that belong to the COUI Center family.
 *
 * The native builder picks a style through its `COUIAlertDialogBuilder(context, styleRes)`
 * constructor; each value here maps to one `AlertDialogBuildStyle`:
 *
 * - [Center] → `AlertDialogBuildStyle.Center`, `coui_center_alert_dialog_layout.xml`, window
 *   gravity `center`, `Animation.COUI.Dialog.Alpha`.
 * - [CenterTiny] → `AlertDialogBuildStyle.Center.Tiny`, `coui_center_alert_dialog_layout_tiny.xml`,
 *   window gravity `bottom`, `Animation.COUI.Dialog`.
 *
 * `Center.Tiny` is the tier Weather's `WeatherDialogHelper` picks on a secondary display; its
 * capsule fill (`coui_alert_dialog_button_color_tiny`) is a fixed light grey with a black label and
 * has no night variant, so it stays visually identical in dark theme, exactly like the system's.
 */
@ExperimentalCOUIApi
enum class AlertDialogStyle {
    /** `AlertDialogBuildStyle.Center`: a floating panel in the middle of the window. */
    Center,

    /** `AlertDialogBuildStyle.Center.Tiny`: a compact panel pinned to the bottom edge. */
    CenterTiny,
}

/**
 * Which action of an alert dialog `COUIButtonBarLayout.setRecommendButtonId` turns into the
 * recommend (filled primary) button.
 *
 * The native builder addresses it by view id: `setPositiveButton(text, listener, true)` sets
 * `mRecommendButtonId = android.R.id.button1`, and the negative / neutral overloads do the same for
 * `button2` / `button3`. Passing `true` to more than one setter leaves the last one marked.
 */
@ExperimentalCOUIApi
enum class AlertDialogButton {
    /** The cancel action (`android.R.id.button2`). */
    Negative,

    /** The optional third action (`android.R.id.button3`). */
    Neutral,

    /** The confirm action (`android.R.id.button1`). */
    Positive,
}
