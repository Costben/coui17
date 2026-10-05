// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TextButtonColors
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A single action in a [DialogButtonBar], addressed by role rather than position because
 * `COUIButtonBarLayout.resortButton` reorders the buttons when the bar stacks.
 *
 * @param text The button label.
 * @param enabled Whether the button is enabled.
 * @param onClick Invoked when the button is clicked.
 * @param recommended Whether this is the bar's recommended button. COUI addresses the recommend
 *   button by id (`COUIButtonBarLayout.setRecommendButtonId`, matched against the button's own id);
 *   an action carries the flag directly here. At most one action of a bar should set it.
 */
@Stable
data class DialogButtonBarAction(
    val text: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val recommended: Boolean = false,
)

/**
 * A COUI dialog button bar (`COUIButtonBarLayout`) that automatically flips between a horizontal
 * row and a vertical stack.
 *
 * `COUIButtonBarLayout.onMeasure` keeps the bar horizontal only when every label fits and there are
 * exactly two buttons; in every other case it stacks. The two tiers differ by more than direction —
 * min heights, paddings, divider thickness and insets all change, and the buttons appear in the
 * opposite order — so see [DialogButtonBarDefaults] for the stacked metrics and [DialogDefaults]
 * for the row ones.
 *
 * The recommend-button tier (`COUIButtonBarLayout.setRecommendButtonId`) highlights the action
 * flagged [DialogButtonBarAction.recommended] as a filled primary button; the others stay
 * secondary. COUI decides the fill through `getBlurWindowButtonType`, which returns `PRIMARY` for
 * the recommend button and `SECONDARY` for every other one, and `applyRecommendStyle`, which
 * switches the button to `COUIButton.ButtonType.PRIMARY`, re-tints its label with
 * `coui_btn_default_text_color` and turns on the solid-fill animation mode (`setAnimType(1)`).
 * The AI-payment variant (`setRecommendButtonType(AI_PAYMENT)`) is not implemented.
 *
 * @param negative The negative (cancel) action. Hidden when null.
 * @param positive The positive (confirm) action. Hidden when null.
 * @param modifier The modifier to be applied to the bar.
 * @param neutral The neutral (third) action. Hidden when null.
 * @param dynamicLayout Whether the bar may stack itself, like
 *   `COUIButtonBarLayout.setDynamicLayout`.
 * @param showDivider Whether the dividers between buttons are shown, like the
 *   `buttonBarShowDivider` attribute.
 * @param hasContentAbove Whether the dialog shows a title, message or custom panel above the bar.
 *   COUI adds [DialogButtonBarDefaults.StackedButtonPaddingTopExtra] to the top-most stacked button
 *   only when nothing sits above it.
 * @param colors The [TextButtonColors] of the non-recommended buttons.
 * @param recommendColors The [TextButtonColors] of the recommended button.
 * @param dividerColor The color of the dividers between buttons.
 * @param textStyle The text style of the labels, also used to measure them for the flip decision.
 */
@Composable
fun DialogButtonBar(
    negative: DialogButtonBarAction?,
    positive: DialogButtonBarAction?,
    modifier: Modifier = Modifier,
    neutral: DialogButtonBarAction? = null,
    dynamicLayout: Boolean = true,
    showDivider: Boolean = true,
    hasContentAbove: Boolean = true,
    colors: TextButtonColors = ButtonDefaults.textButtonColorsBorderless(),
    recommendColors: TextButtonColors = ButtonDefaults.textButtonColorsPrimary(),
    dividerColor: Color = COUITheme.colorScheme.dividerLine,
    textStyle: TextStyle = COUITheme.textStyles.button,
) {
    if (negative == null && positive == null && neutral == null) return

    BoxWithConstraints(modifier) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        // COUI clamps the measured bar width to coui_dialog_max_width before deciding.
        val barWidth = minOf(maxWidth, DialogDefaults.MaxWidth)
        val buttonCount = listOfNotNull(negative, positive, neutral).size

        val stacked = remember(
            barWidth,
            buttonCount,
            dynamicLayout,
            textStyle,
            density,
            negative?.text,
            positive?.text,
            neutral?.text,
        ) {
            if (!dynamicLayout) {
                false
            } else {
                val labelsFit = labelsFitInRow(
                    negative = negative,
                    positive = positive,
                    neutral = neutral,
                    barWidth = barWidth,
                    buttonCount = buttonCount,
                    textStyle = textStyle,
                    textMeasurer = textMeasurer,
                    density = density,
                )
                // COUIButtonBarLayout.onMeasure stays horizontal only when the labels fit and there
                // are exactly two buttons.
                !(labelsFit && buttonCount == 2)
            }
        }

        if (stacked) {
            StackedButtonBar(
                negative = negative,
                positive = positive,
                neutral = neutral,
                showDivider = showDivider,
                hasContentAbove = hasContentAbove,
                colors = colors,
                recommendColors = recommendColors,
                dividerColor = dividerColor,
                textStyle = textStyle,
            )
        } else {
            RowButtonBar(
                negative = negative,
                positive = positive,
                neutral = neutral,
                showDivider = showDivider,
                colors = colors,
                recommendColors = recommendColors,
                dividerColor = dividerColor,
                textStyle = textStyle,
            )
        }
    }
}

/**
 * Whether every label fits its cell, inverting `COUIButtonBarLayout.needSetButVertical`: the bar
 * width minus the dividers is split evenly between the buttons, each cell loses its two horizontal
 * paddings, and one label wider than what is left forces the bar to stack.
 *
 * The divider subtracted here is the *stacked* tier thickness
 * (`coui_delete_alert_dialog_divider_height_verticalbutton`), which is what COUI uses in this
 * formula even though the row it may end up drawing uses the thicker one.
 */
private fun labelsFitInRow(
    negative: DialogButtonBarAction?,
    positive: DialogButtonBarAction?,
    neutral: DialogButtonBarAction?,
    barWidth: Dp,
    buttonCount: Int,
    textStyle: TextStyle,
    textMeasurer: TextMeasurer,
    density: Density,
): Boolean {
    if (buttonCount == 0) return true
    val availablePx = with(density) {
        val dividersPx = (buttonCount - 1) * DialogButtonBarDefaults.StackedDividerThickness.roundToPx()
        (barWidth.roundToPx() - dividersPx) / buttonCount -
            DialogButtonBarDefaults.ButtonHorizontalPadding.roundToPx() * 2
    }
    return listOfNotNull(positive, negative, neutral).all { action ->
        // COUI measures the raw single-line paint width of the label.
        textMeasurer.measure(
            text = action.text,
            style = textStyle,
            softWrap = false,
            maxLines = 1,
        ).size.width <= availablePx
    }
}

/**
 * The horizontal tier of `COUIButtonBarLayout` (`setButtonsHorizontal` + `resetHorButsPadding` +
 * `resetHorDividerVisibility`): equal-weight cells ordered negative / neutral / positive, with a
 * 1dp divider between each pair of present buttons.
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
private fun RowButtonBar(
    negative: DialogButtonBarAction?,
    positive: DialogButtonBarAction?,
    neutral: DialogButtonBarAction?,
    showDivider: Boolean,
    colors: TextButtonColors,
    recommendColors: TextButtonColors,
    dividerColor: Color,
    textStyle: TextStyle,
) {
    val actions = listOfNotNull(negative, neutral, positive)
    Row(
        // COUI horizontal button bar: spans the full panel width; the panel bottom inset is
        // carried by the buttons' own paddings.
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        actions.forEachIndexed { index, action ->
            if (index > 0 && showDivider) {
                Box(
                    // COUI hairline divider between the bar buttons.
                    modifier = Modifier
                        .padding(
                            top = DialogDefaults.ButtonBarDividerInsetTop,
                            bottom = DialogDefaults.ButtonBarDividerInsetBottom,
                        )
                        .width(DialogDefaults.ButtonBarDividerThickness)
                        .fillMaxHeight()
                        .background(dividerColor),
                )
            }
            TextButton(
                text = action.text,
                onClick = action.onClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enabled = action.enabled,
                pressScaleEnabled = true,
                cornerRadius = DialogButtonBarDefaults.ButtonCornerRadius,
                minHeight = DialogDefaults.ButtonBarMinHeight,
                colors = if (action.recommended) recommendColors else colors,
                insideMargin = DialogDefaults.ButtonBarInsideMargin,
                textStyle = textStyle,
            )
        }
    }
}

/**
 * The vertical tier of `COUIButtonBarLayout` (`setButtonsVertical` + `resetVerButsPadding` +
 * `resetVerDividerVisibility`): full-width buttons ordered neutral / positive / negative, each
 * 52dp tall except the bottom-most one at 64dp, split by 0.33dp dividers inset 24dp.
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
private fun StackedButtonBar(
    negative: DialogButtonBarAction?,
    positive: DialogButtonBarAction?,
    neutral: DialogButtonBarAction?,
    showDivider: Boolean,
    hasContentAbove: Boolean,
    colors: TextButtonColors,
    recommendColors: TextButtonColors,
    dividerColor: Color,
    textStyle: TextStyle,
) {
    // COUI resortButton brings neutral, then positive, then negative to the front when stacking.
    val slots = listOfNotNull(
        neutral?.let { BarSlot.Neutral to it },
        positive?.let { BarSlot.Positive to it },
        negative?.let { BarSlot.Negative to it },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // COUI adds coui_bottom_alert_dialog_buttonbar_margintop once the stacked bar holds
            // more than one button, or a single button that is the recommend one.
            .padding(
                top = if (slots.size > 1 || (slots.size == 1 && slots.first().second.recommended)) {
                    DialogButtonBarDefaults.StackedBarMarginTop
                } else {
                    0.dp
                },
            ),
    ) {
        slots.forEachIndexed { index, (slot, action) ->
            if (index > 0 && showDivider) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = DialogButtonBarDefaults.StackedDividerInsetHorizontal)
                        .fillMaxWidth()
                        .height(DialogButtonBarDefaults.StackedDividerThickness)
                        .background(dividerColor),
                )
            }
            val isTop = index == 0
            val isBottom = index == slots.lastIndex
            // A lone negative button is the one stacked case COUI falls back to the horizontal
            // panel metrics for (resetVerButsPadding gives it mHorButPanelMinHeight and the
            // horizontal paddings).
            val loneNegative = slots.size == 1 && slot == BarSlot.Negative
            TextButton(
                text = action.text,
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = action.enabled,
                pressScaleEnabled = true,
                cornerRadius = DialogButtonBarDefaults.ButtonCornerRadius,
                minHeight = when {
                    loneNegative -> DialogDefaults.ButtonBarMinHeight
                    isBottom -> DialogButtonBarDefaults.StackedButtonMinHeightBottom
                    else -> DialogButtonBarDefaults.StackedButtonMinHeight
                },
                colors = if (action.recommended) recommendColors else colors,
                insideMargin = if (loneNegative) {
                    DialogDefaults.ButtonBarInsideMargin
                } else {
                    PaddingValues(
                        start = DialogButtonBarDefaults.ButtonHorizontalPadding,
                        top = DialogButtonBarDefaults.StackedButtonPaddingVertical +
                            if (isTop && !hasContentAbove) {
                                DialogButtonBarDefaults.StackedButtonPaddingTopExtra
                            } else {
                                0.dp
                            },
                        end = DialogButtonBarDefaults.ButtonHorizontalPadding,
                        bottom = DialogButtonBarDefaults.StackedButtonPaddingVertical +
                            if (isBottom) DialogButtonBarDefaults.StackedButtonPaddingBottomExtra else 0.dp,
                    )
                },
                textStyle = textStyle,
            )
        }
    }
}

/** The COUI button roles, kept apart because the stacked tier treats them differently. */
private enum class BarSlot { Neutral, Positive, Negative }

object DialogButtonBarDefaults {
    /**
     * The horizontal padding of a bar button, in both tiers. COUI
     * `coui_alert_dialog_button_horizontal_padding` (12dp); also the padding
     * `COUIButtonBarLayout.needSetButVertical` subtracts from each cell.
     */
    val ButtonHorizontalPadding = 12.dp

    /**
     * The min height of a stacked bar button. COUI
     * `coui_alert_dialog_vertical_button_min_height` (44dp, `mVerButMinHeightNormal`).
     */
    val StackedButtonMinHeight = 44.dp

    /**
     * The min height of the bottom-most stacked bar button. COUI `mVerButMinHeightBottom` =
     * `coui_alert_dialog_vertical_button_min_height` (44dp) +
     * `coui_center_alert_dialog_vertical_button_paddingbottom_vertical_extra` (0dp), which leaves
     * it at the same 44dp as [StackedButtonMinHeight].
     */
    val StackedButtonMinHeightBottom = 44.dp

    /**
     * The vertical padding of a stacked bar button. COUI
     * `coui_bottom_alert_dialog_vertical_button_padding_vertical_new` (4dp,
     * `mVerButtonVecPaddingNew`).
     */
    val StackedButtonPaddingVertical = 4.dp

    /**
     * The extra top padding of the top-most stacked bar button when nothing sits above the bar.
     * COUI `coui_bottom_alert_dialog_vertical_button_padding_top_extra_new` (0dp).
     */
    val StackedButtonPaddingTopExtra = 0.dp

    /**
     * The extra bottom padding of the bottom-most stacked bar button, which carries the panel
     * bottom inset. COUI `coui_bottom_alert_dialog_vertical_button_padding_bottom_extra_new` (0dp).
     */
    val StackedButtonPaddingBottomExtra = 0.dp

    /**
     * The top margin of a stacked bar holding more than one button, or a single button that is the
     * recommend one. COUI `coui_bottom_alert_dialog_buttonbar_margintop` (0dp).
     */
    val StackedBarMarginTop = 0.dp

    /**
     * The thickness of the divider between stacked bar buttons. COUI
     * `coui_delete_alert_dialog_divider_height_verticalbutton`; the horizontal tier draws the
     * thicker `DialogDefaults.ButtonBarDividerThickness` instead.
     */
    val StackedDividerThickness = 0.33.dp

    /**
     * The horizontal inset of the divider between stacked bar buttons. COUI
     * `coui_bottom_alert_dialog_horizontal_button_margin_default`.
     */
    val StackedDividerInsetHorizontal = 24.dp

    /**
     * The corner radius of a dialog bar button. `COUIAlertDialogBottomButtonNewNormal` sets
     * `drawableRadius=0dp`, so a bar button is a square-cornered cell. Its `drawableColor` is
     * `@color/coui_transparence` with `animType=2`, which fills through
     * `getStrokeButtonAnimatorColor` and draws the borderless pass — the press feedback is the
     * mask tint over the whole cell plus the COUI press scale (`scaleEnable=true`).
     */
    val ButtonCornerRadius = 0.dp
}
