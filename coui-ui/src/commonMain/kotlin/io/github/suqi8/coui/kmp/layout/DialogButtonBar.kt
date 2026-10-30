// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.MaterialStrokeParams
import io.github.suqi8.coui.kmp.basic.SpotLightDefaults
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
) {
    /**
     * The three-property form, kept so callers written before [recommended] existed still compile;
     * the action is not the bar's recommended button.
     */
    constructor(
        text: String,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) : this(
        text = text,
        enabled = enabled,
        onClick = onClick,
        recommended = false,
    )

    /** The three-property [copy], kept for binary compatibility with callers built before 1.2.0. */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the copy that also takes recommended.",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        text: String = this.text,
        enabled: Boolean = this.enabled,
        onClick: () -> Unit = this.onClick,
    ): DialogButtonBarAction = copy(
        text = text,
        enabled = enabled,
        onClick = onClick,
        recommended = recommended,
    )
}

/**
 * A COUI dialog button bar (`COUIButtonBarLayout`) that automatically flips between a horizontal
 * row and a vertical stack.
 *
 * `COUIButtonBarLayout.onMeasure` keeps the bar horizontal only when every label fits and there are
 * exactly two buttons; in every other case it stacks. The two tiers differ by more than direction —
 * the buttons appear in the opposite order and the stacked tier may pull itself up against the
 * content above — so see [DialogButtonBarDefaults] for both metric sets.
 *
 * ## The layer stack
 *
 * ColorOS 17 draws an alert-dialog button out of five stacked layers, and this bar reproduces the
 * three that a Compose surface can carry:
 *
 * 1. **容器层** — the capsule itself. `COUIAlertDialogBottomButtonSecondary` is a
 *    `Widget.COUI.Button.Large` with `drawableColor=?couiColorContainer8` and
 *    `drawableRadius=-1.0dp`; `COUIButtonDrawable` reads a negative radius as "stadium", clamping it
 *    to half the measured height. On a 44dp bar that is a 22dp radius, i.e. a capsule. The fill is
 *    [DialogButtonBarDefaults.buttonFill] — see its KDoc for why it is a translucent white rather
 *    than the `?couiColorContainer8` the style names.
 * 2. **光效层** — `COUIButton.isSpotlightEffectEnabled()` clips a `COUISpotLightEffectDrawable` to
 *    the button path, and `COUIShadowEdgeDrawable` (edge style 0 / shadow style 0, corner type 2,
 *    weight 2.0) paints the rim. Only the recommend button (`PRIMARY`) gets the spotlight: it is
 *    [TextButton]'s `spotLight`, driven by `pressSpotLight` with `CAPSULE_PARAMS_1`, the default of
 *    `COUISpotLightEffect`. The rim is the capsule's `edgeStroke`; the panel's own
 *    `materialShadowEdge` is a separate, larger outline around the whole dialog.
 * 3. **交互层** — `COUITouchMotionAnimator` scales the capsule down while pressed and
 *    `COUIMaskEffectDrawable` washes it. Both live in [TextButton].
 *
 * The geometry is the part that has to be exact, and it is *not* "buttons fill the bar": the
 * capsule is inset from its cell on all four sides, which is what makes the two buttons read as two
 * floating capsules over the panel instead of one white slab.
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
 * @param showDivider Retained for source compatibility and inert, like
 *   `COUIButtonBarLayout.setShowDividerWhenHasItems`, which ColorOS 17 reduced to an empty method.
 *   The alert-dialog bar has no divider views at all — `coui_center_alert_dialog_button_panel.xml`
 *   holds nothing but the three button cells — because the capsules are already separated by
 *   [DialogButtonBarDefaults.ButtonBarGap].
 * @param hasContentAbove Whether the dialog shows a title, message or custom panel above the bar.
 *   COUI adds [DialogButtonBarDefaults.StackedButtonPaddingTopExtra] to the top-most stacked button
 *   only when nothing sits above it; that dimen is 0dp in ColorOS 17, so the flag no longer moves
 *   the bar.
 * @param colors The [TextButtonColors] of the non-recommended buttons.
 * @param recommendColors The [TextButtonColors] of the recommended button.
 * @param dividerColor Retained for source compatibility; the alert-dialog bar draws no dividers.
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
    colors: TextButtonColors = DialogButtonBarDefaults.buttonColors(),
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
                barWidth = barWidth,
                hasContentAbove = hasContentAbove,
                colors = colors,
                recommendColors = recommendColors,
                textStyle = textStyle,
            )
        } else {
            RowButtonBar(
                negative = negative,
                positive = positive,
                neutral = neutral,
                colors = colors,
                recommendColors = recommendColors,
                textStyle = textStyle,
            )
        }
    }
}

/**
 * The pre-`recommendColors` overload, kept so callers written before the recommend tier existed
 * still compile.
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
    colors: TextButtonColors = DialogButtonBarDefaults.buttonColors(),
    dividerColor: Color = COUITheme.colorScheme.dividerLine,
    textStyle: TextStyle = COUITheme.textStyles.button,
) {
    DialogButtonBar(
        negative = negative,
        positive = positive,
        modifier = modifier,
        neutral = neutral,
        dynamicLayout = dynamicLayout,
        showDivider = showDivider,
        hasContentAbove = hasContentAbove,
        colors = colors,
        // `recommendColors` exists only on the primary overload; passing it pins this forward to the
        // primary instead of re-selecting this same overload (which would recurse forever).
        recommendColors = ButtonDefaults.textButtonColorsPrimary(),
        dividerColor = dividerColor,
        textStyle = textStyle,
    )
}

/**
 * Whether every label fits its cell, inverting `COUIButtonBarLayout.needSetButVertical`.
 *
 * COUIButtonBarLayout asks `getHorizontalButtonContentWidth(i)` for each button:
 *
 * ```
 * (i / buttonCount) - (mHorButHorPadding * 2) - mButtonParentPaddingHorizontalEdge
 *                   - mButtonParentPaddingHorizontalCentral
 * ```
 *
 * i.e. an even share of the bar minus the label's own two paddings (12dp each,
 * `coui_alert_dialog_button_horizontal_padding`) minus the cell padding on the outer seam (16dp,
 * `..._parent_padding_horizontal`) and on the inner seam (4dp, `..._parent_padding_horizontal_inside`).
 * The cell padding is what this port previously forgot, so a label that only just fits the raw share
 * was kept in the row and then squeezed by the capsule insets.
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
        // `needSetButVertical`: the bar's width minus its two side insets and the gaps, split into
        // equal cells, minus the label padding inside each cell.
        val cellWidth = (
            barWidth -
                DialogButtonBarDefaults.ButtonBarPaddingHorizontal * 2 -
                DialogButtonBarDefaults.ButtonBarGap * (buttonCount - 1)
            ) / buttonCount
        (cellWidth - DialogButtonBarDefaults.ButtonHorizontalPadding * 2).roundToPx()
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
 * The horizontal tier of `COUIButtonBarLayout` (`setButtonsHorizontal` + `setHorButtonsParentPadding`):
 * equal-weight cells ordered negative / neutral / positive, each capsule inset from its cell by
 * 16dp on the outer edge, 4dp on the inner edge, 4dp on top and 16dp at the bottom.
 *
 * Those four insets are `coui_alert_dialog_button_parent_padding_horizontal` /
 * `..._horizontal_inside` / `..._vertical` / `..._bottom`, and they are what a screenshot of the
 * real dialog shows: a 64dp-tall band whose capsules are 44dp tall, 16dp in from each panel side,
 * 16dp above the panel's bottom edge, and 8dp apart (the two 4dp inner insets meeting).
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
private fun RowButtonBar(
    negative: DialogButtonBarAction?,
    positive: DialogButtonBarAction?,
    neutral: DialogButtonBarAction?,
    colors: TextButtonColors,
    recommendColors: TextButtonColors,
    textStyle: TextStyle,
) {
    val actions = listOfNotNull(negative, neutral, positive)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = DialogButtonBarDefaults.ButtonBarPaddingHorizontal,
                top = DialogButtonBarDefaults.ButtonBarPaddingTop,
                end = DialogButtonBarDefaults.ButtonBarPaddingHorizontal,
                bottom = DialogButtonBarDefaults.ButtonBarPaddingBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(DialogButtonBarDefaults.ButtonBarGap),
    ) {
        actions.forEach { action ->
            DialogBarButton(
                action = action,
                modifier = Modifier.weight(1f),
                minHeight = DialogButtonBarDefaults.ButtonHeight,
                colors = colors,
                recommendColors = recommendColors,
                textStyle = textStyle,
            )
        }
    }
}

/**
 * The vertical tier of `COUIButtonBarLayout` (`setButtonsVertical` + `setVerButtonsParentPadding`):
 * full-width capsules ordered neutral / positive / negative, all of them 16dp in from the panel
 * sides, 8dp apart and 16dp above the panel's bottom edge.
 *
 * `resetVerButsPadding` gives every stacked button the same `coui_alert_dialog_button_height`
 * min height the horizontal tier uses, and `coui_bottom_alert_dialog_vertical_button_padding_top_extra_new`
 * / `..._bottom_extra_new` are both 0dp, so the stacked capsule is the same 44dp capsule turned
 * 90°: the two tiers differ in direction and order only.
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
private fun StackedButtonBar(
    negative: DialogButtonBarAction?,
    positive: DialogButtonBarAction?,
    neutral: DialogButtonBarAction?,
    barWidth: Dp,
    hasContentAbove: Boolean,
    colors: TextButtonColors,
    recommendColors: TextButtonColors,
    textStyle: TextStyle,
) {
    // COUI resortButton brings neutral, then positive, then negative to the front, so the stack
    // reads top-down neutral / positive / negative.
    val actions = listOfNotNull(neutral, positive, negative)
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelWidthPx = with(density) {
        (
            barWidth -
                DialogButtonBarDefaults.ButtonBarPaddingHorizontal * 2 -
                DialogButtonBarDefaults.ButtonHorizontalPadding * 2
            ).roundToPx()
    }
    val topPadding = DialogButtonBarDefaults.ButtonBarPaddingTop +
        if (hasContentAbove) 0.dp else DialogButtonBarDefaults.StackedButtonPaddingTopExtra
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // `mSetTopMarginFlag && buttonCount > 1` adds `coui_bottom_alert_dialog_buttonbar_margintop`
            // (0dp in ColorOS 17) above a stacked bar of more than one button.
            .padding(top = if (actions.size > 1) DialogButtonBarDefaults.StackedBarMarginTop else 0.dp)
            .padding(
                start = DialogButtonBarDefaults.ButtonBarPaddingHorizontal,
                end = DialogButtonBarDefaults.ButtonBarPaddingHorizontal,
                bottom = DialogButtonBarDefaults.ButtonBarPaddingBottom,
            )
            .padding(top = topPadding),
        verticalArrangement = Arrangement.spacedBy(DialogButtonBarDefaults.ButtonBarGap),
    ) {
        actions.forEach { action ->
            // A stacked capsule is as wide as the panel, so the only thing that can grow it is a
            // label that wraps: COUI then swaps `coui_alert_dialog_button_height` for
            // `coui_alert_dialog_button_min_height_multiline`.
            val wraps = remember(action.text, labelWidthPx, textStyle) {
                labelWidthPx > 0 && textMeasurer.measure(
                    text = action.text,
                    style = textStyle,
                    constraints = Constraints(maxWidth = labelWidthPx),
                ).lineCount > 1
            }
            DialogBarButton(
                action = action,
                modifier = Modifier.fillMaxWidth(),
                minHeight = if (wraps) {
                    DialogButtonBarDefaults.ButtonMultilineHeight
                } else {
                    DialogButtonBarDefaults.ButtonHeight
                },
                colors = colors,
                recommendColors = recommendColors,
                textStyle = textStyle,
            )
        }
    }
}

/**
 * One capsule of a [DialogButtonBar], shared by both tiers.
 *
 * The capsule is the [TextButton]'s own bounds — `COUIButton` sets its drawable as the view
 * background, so the 44dp `minHeight` *is* the capsule and the 12dp/4dp paddings are interior
 * space for the label, not space around the capsule. [DialogButtonBarDefaults.ButtonInsideMargin]
 * carries those interior paddings; the space around the capsule belongs to the bar, which is why
 * it is padding on the [Row] / [Column] rather than here.
 *
 * `minHeight` rather than `height`: `COUIAlertDialogBottomButtonSecondary` overrides
 * `Widget.COUI.Button.Large.ButtonNew`'s fixed `android:layout_height=44dp` with `wrap_content`,
 * leaving `android:minHeight=@dimen/coui_btn_large_height_min` (44dp) as the floor, and
 * `android:singleLine=false` lets a wrapped label grow the capsule to
 * `coui_alert_dialog_button_min_height_multiline` (56dp) instead of clipping it.
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
private fun DialogBarButton(
    action: DialogButtonBarAction,
    minHeight: Dp,
    colors: TextButtonColors,
    recommendColors: TextButtonColors,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    // COUIButton always draws a rim over its capsule (COUIShadowEdgeDrawable, fed by
    // COUIMaterialEffect.getStrokeParamsByType). A secondary button uses TYPE_CONTENT_CAPSULE_5,
    // whose 0.8 alpha is why the native capsule's edge reads almost white; a recommended button is
    // a PRIMARY button and takes the much dimmer TYPE_CONTENT_CAPSULE_4.
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val edgeStroke = if (action.recommended) {
        MaterialStrokeParams.ContentCapsule4
    } else {
        MaterialStrokeParams.contentCapsule5(isDark)
    }
    TextButton(
        text = action.text,
        onClick = action.onClick,
        modifier = modifier.heightIn(min = minHeight),
        enabled = action.enabled,
        // `Widget.COUI.Button` leaves `scaleEnable` at its default, so the capsule takes the
        // COUITouchMotion press scale as well as the press wash.
        pressScaleEnabled = true,
        // `drawableRadius=-1.0dp` makes COUIButtonDrawable clamp the radius to half the measured
        // height, so the capsule stays a stadium when a wrapped label grows it to 56dp.
        cornerRadius = minHeight / 2,
        // The cell already fixes the width; a min width would only fight the equal-weight split.
        minWidth = 0.dp,
        minHeight = minHeight,
        colors = if (action.recommended) recommendColors else colors,
        insideMargin = DialogButtonBarDefaults.ButtonInsideMargin,
        textStyle = textStyle,
        edgeStroke = edgeStroke,
        // `COUIButton.isSpotlightEffectEnabled()`: only the PRIMARY (recommend) button carries the
        // light. It is painted under the label and follows the finger while the capsule is held.
        spotLight = if (action.recommended) SpotLightDefaults.capsuleParams1(isDark) else null,
    )
}

object DialogButtonBarDefaults {
    /**
     * The space between a button cell and the panel's side edge.
     *
     * `coui_alert_dialog_button_parent_padding_horizontal` = 16dp, applied by
     * `COUIButtonBarLayout.setHorButtonsParentPadding` to the outer edge of the first and last cell
     * and by `setVerButtonsParentPadding` to both sides of every stacked cell. The painted capsule
     * fills its cell edge to edge, so this is also what a screenshot measures: 62.5px at 3.854
     * px/dp, i.e. 16.2dp including the capsule's anti-aliased edge.
     */
    val ButtonBarPaddingHorizontal = 16.dp

    /**
     * The space between a capsule and the content above it.
     *
     * `coui_alert_dialog_button_parent_padding_vertical` = 4dp.
     */
    val ButtonBarPaddingTop = 4.dp

    /**
     * The space between a capsule and the panel's bottom edge.
     *
     * `coui_alert_dialog_button_parent_padding_bottom` = 16dp. This is the inset whose absence made
     * the ported buttons spill over the panel's bottom corners.
     */
    val ButtonBarPaddingBottom = 16.dp

    /**
     * The gap between the two capsules at the panel's centre line.
     *
     * The cells themselves are flush; each contributes its inner edge
     * `coui_alert_dialog_button_parent_padding_horizontal_inside` = 4dp, so the capsules end up 8dp
     * apart. A ColorOS 17 screenshot measures 33.6px at 3.854 px/dp, i.e. 8.7dp across the two
     * capsules' anti-aliased edges.
     */
    val ButtonBarGap = 8.dp

    /**
     * The height of a capsule.
     *
     * `coui_alert_dialog_button_height` = 44dp, used both as the bar's `minHeight`
     * (`coui_center_alert_dialog_button_panel.xml`) and as each button's `android:minHeight`
     * (`COUIAlertDialogBottomButtonSecondary`).
     */
    val ButtonHeight = 44.dp

    /**
     * The height a capsule grows to when its label wraps.
     *
     * `coui_alert_dialog_button_min_height_multiline` = 56dp, the floor `COUIButtonBarLayout` uses
     * for a multiline label. Only the stacked tier can reach it: a horizontal cell is measured
     * against its label first and the bar flips to a stack rather than wrapping.
     */
    val ButtonMultilineHeight = 56.dp

    /**
     * The label's interior padding, i.e. the space between the capsule's edge and its text.
     *
     * `coui_alert_dialog_button_horizontal_padding` = 12dp horizontally;
     * `coui_bottom_alert_dialog_horizontal_button_padding_top_extra_new` and
     * `..._bottom_extra_new` = 4dp vertically, on top of the 44dp capsule height. A wrapped label
     * grows the capsule to `coui_alert_dialog_button_min_height_multiline` (56dp) instead.
     */
    val ButtonInsideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp)

    /**
     * The label's horizontal padding on its own, kept as the single source of
     * [ButtonInsideMargin]'s horizontal value.
     */
    val ButtonHorizontalPadding = 12.dp

    /**
     * The extra top padding a stacked button gets when nothing sits above the bar,
     * `coui_bottom_alert_dialog_vertical_button_padding_top_extra_new` = 0dp.
     */
    val StackedButtonPaddingTopExtra = 0.dp

    /**
     * The top margin of a stacked bar with more than one button,
     * `coui_bottom_alert_dialog_buttonbar_margintop` = 0dp.
     */
    val StackedBarMarginTop = 0.dp

    /**
     * The fill of a non-recommended capsule.
     *
     * The style names `?couiColorContainer8` composited over `?couiColorBackgroundElevated`, but
     * that is the recipe for the *non*-blur path. With the blur window on — which is what a
     * ColorOS 17 dialog uses — `COUIButton.onDraw` skips `drawButtonBackground` entirely
     * (`if (!isBlurEffectEnabled() || mIsDrawMaterialEffect)`) and the platform renders
     * `COUIMaterialEffect.setViewBlurEffect(TYPE_CONTENT_SECONDARY_BUTTON)` instead, i.e. a
     * frosted, brightened copy of the panel behind the capsule.
     *
     * That effect is `CONTENT_SECONDARY_BUTTON_PARAMS_LIGHT` / `_DARK` in `COUIMaterialBlurEffect`:
     * a 25px blur of what lies behind the capsule, then one `SRC_OVER` white layer — `#CCFFFFFF` in
     * light theme and only `#33FFFFFF` in dark. The capsule therefore owns no colour of its own; it
     * is the panel underneath, brightened, and whatever the panel lets through (a blue page behind
     * a dark dialog tints its buttons blue as well) shows in it.
     *
     * Light, measured on the ColorOS 17 dialog: the capsule reads (246,246,246)–(249,249,249) over
     * a (229,229,229) panel, a 67% white layer (229 + 0.667 × 26 = 246.3), so the port keeps that
     * measured result. Dark: (86,86,86) over a (44,44,44) panel, and (81–86, 90, 97) over a
     * blue-tinted (44,49,55) one in Settings — 20% white either way, the `#33FFFFFF` of the recipe.
     * Using the light value in dark theme renders the capsule at (185,185,185), so light the white
     * label all but disappears.
     */
    @Composable
    fun buttonFill(): Color = if (isDarkPanel()) DarkButtonFill else LightButtonFill

    /**
     * The fill of a disabled non-recommended capsule.
     *
     * `COUIButton.changeButtonAlphaColor` returns `?couiColorContainer8` when the button is not
     * enabled and not pressed — `#14000000` in light theme, `#1AFFFFFF` in dark — so a disabled
     * capsule sinks back into the panel instead of standing out of it.
     */
    @Composable
    fun disabledButtonFill(): Color = if (isDarkPanel()) DarkDisabledButtonFill else LightDisabledButtonFill

    /** Light-theme capsule fill: the measured 67% white layer, see [buttonFill]. */
    private val LightButtonFill = Color(0xAAFFFFFF)

    /** Dark-theme capsule fill: `CONTENT_SECONDARY_BUTTON_PARAMS_DARK`'s `#33FFFFFF`. */
    private val DarkButtonFill = Color(0x33FFFFFF)

    /** `coui_color_container8_light`. */
    private val LightDisabledButtonFill = Color(0x14000000)

    /** `coui_color_container8_dark`. */
    private val DarkDisabledButtonFill = Color(0x1AFFFFFF)

    /** The capsule recipes switch on the night configuration, read here off the theme background. */
    @Composable
    private fun isDarkPanel(): Boolean = COUITheme.colorScheme.background.luminance() < 0.5f

    /**
     * The colours of an ordinary (non-recommended) capsule: the translucent fill with the
     * `?couiColorLabelPrimary` label.
     *
     * This is [DialogButtonBar]'s own default, exposed so a caller that has to build the colours
     * anyway — to override one of them, or to hand [dangerButtonColors] to the other button — does
     * not have to re-derive the recipe.
     */
    @Composable
    fun buttonColors(): TextButtonColors = ButtonDefaults.textButtonColorsBorderless(
        color = buttonFill(),
        disabledColor = disabledButtonFill(),
        textColor = COUITheme.colorScheme.onBackground,
        disabledTextColor = COUITheme.colorScheme.disabledOnSurface,
    )

    /**
     * The label colour of a destructive action, `#ffea3447`.
     *
     * A red dialog button is *not* a button-bar style in ColorOS 17: the fill stays the ordinary
     * translucent capsule and only the label turns red, which is why Settings does it by hand —
     * `StyleDialogUtil` and `ScreenLockFeature` both call
     * `button.setTextColor(R.color.color_delete_alert_dialog_button_warning_color)`. That colour is
     * `#ffea3447`, and its disabled counterpart
     * `color_delete_alert_dialog_button_warning_color_disable` is the same red at 40% alpha.
     *
     * The other reds in the same resource table (`coui_color_label_theme_red` and
     * `coui_color_primary_text_red`, both `#db382c`) are the *theme* accent reds used by list rows
     * and preference titles; they are a stop darker than what a delete dialog paints.
     */
    val DangerTextColor = Color(0xFFEA3447)

    /** [DangerTextColor] at 40% alpha, `color_delete_alert_dialog_button_warning_color_disable`. */
    val DangerDisabledTextColor = Color(0x66EA3447)

    /**
     * The colours of a destructive action: the same capsule as [buttonFill] with a red label.
     *
     * Pass it as `DialogButtonBar(colors = DialogButtonBarDefaults.dangerButtonColors())` when the
     * whole bar is destructive, or per action through [DialogButtonBarAction.recommended] /
     * a custom [TextButtonColors] when only one button deletes something.
     */
    @Composable
    fun dangerButtonColors(): TextButtonColors = ButtonDefaults.textButtonColorsBorderless(
        color = buttonFill(),
        disabledColor = disabledButtonFill(),
        textColor = DangerTextColor,
        disabledTextColor = DangerDisabledTextColor,
    )
}
