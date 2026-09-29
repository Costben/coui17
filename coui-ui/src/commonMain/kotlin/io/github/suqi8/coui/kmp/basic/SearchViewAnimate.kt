// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The ColorOS 17 magnetic search bar, mirroring `COUISearchViewAnimate`
 * (`com/coui/appcompat/searchview/COUISearchViewAnimate`, layout
 * `res/layout/coui_search_view_animate_layout.xml`).
 *
 * It is a different native class from `COUISearchBar` — which [SearchBar] ports and which is a plain
 * capsule plus an expandable result panel — so it lives in its own file rather than extending
 * [SearchBar]: the two have no shared state model, and folding them together would change
 * [SearchBar]'s public API for existing callers.
 *
 * The bar animates between two states (`COUISearchViewAnimate.STATE_NORMAL` = 0 and `STATE_EDIT` = 1,
 * surfaced here as [SearchViewState] and driven by [expanded]):
 * - collapsed, only the 24dp `coui_search_view_icon` glyph shows;
 * - expanded, the input field and the trailing `SearchFunctionalButton` (the Cancel label) slide and
 *   fade in.
 *
 * Enter and exit are `initSmoothEnterAnimatorSet` / `initSmoothExitAnimatorSet`: the offset and scale
 * tracks run for [SearchViewAnimateDefaults.REVEAL_DURATION_MILLIS] (450ms,
 * `DEFAULT_SEARCH_VIEW_OFFSET_CHANGE_DURATION` / `DEFAULT_SEARCH_VIEW_SCALE_CHANGE_DURATION`) and the
 * functional-button alpha track for [SearchViewAnimateDefaults.BUTTON_ALPHA_DURATION_MILLIS] (350ms,
 * `DEFAULT_BUTTON_ALPHA_CHANGE_DURATION`) or
 * [SearchViewAnimateDefaults.NON_INSTANT_BUTTON_ALPHA_DURATION_MILLIS] (100ms,
 * `NON_INSTANT_SEARCH_BUTTON_ALPHA_CHANGE_DURATION`), all on `COUIMoveEaseInterpolator`
 * (`pathInterpolator(0.3, 0, 0.1, 1)`, [SearchViewAnimateDefaults.MoveEase]).
 *
 * [collapsePercent] is `setSearchViewAnimateHeightPercent`: the app feeds it the collapse progress of
 * the host (0 = fully collapsed to the icon, 1 = fully open) and the bar derives its own height,
 * vertical translation, content alpha and background tint from it, exactly like the native method.
 *
 * @param query The text shown in the input field.
 * @param onQueryChange Called with the updated text.
 * @param expanded Whether the bar is in the edit state (`SearchViewState.Edit`).
 * @param onExpandedChange Called when the bar wants to enter or leave the edit state, e.g. when the
 *   collapsed icon is tapped or the Cancel button is pressed.
 * @param modifier The modifier to be applied to the bar.
 * @param hint The placeholder shown while [query] is empty.
 * @param type Which of the two `COUISearchViewAnimate` layouts is used: `TYPE_INSTANT_SEARCH` (0)
 *   places the functional button directly after the input field, `TYPE_NON_INSTANT_SEARCH` (1) puts
 *   the 1dp `search_linear_divider` between them.
 * @param collapsePercent The collapse progress fed to `setSearchViewAnimateHeightPercent`, 0..1.
 * @param onSearch Called with the current [query] when the IME search action fires.
 * @param cancelText The label of the trailing functional button (`string/coui_search_view_cancel`).
 * @param cancelButton Replaces the default trailing `SearchFunctionalButton` entirely.
 * @param leadingIcon The glyph shown while collapsed (defaults to the 24dp `coui_search_view_icon`).
 * @param colors The [SearchViewAnimateColors] of the bar.
 */
@Composable
fun SearchViewAnimate(
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    type: SearchViewAnimateType = SearchViewAnimateDefaults.Type,
    collapsePercent: Float = SearchViewAnimateDefaults.CollapsePercent,
    onSearch: (String) -> Unit = {},
    cancelText: String = SearchViewAnimateDefaults.CANCEL_TEXT,
    cancelButton: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector = COUIIcons.Basic.Search,
    colors: SearchViewAnimateColors = SearchViewAnimateDefaults.colors(),
) {
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentOnSearch by rememberUpdatedState(onSearch)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)

    // initButtonEnterAnimator / initButtonExitAnimator pick the alpha duration by type.
    val buttonAlphaDuration = if (type == SearchViewAnimateType.InstantSearch) {
        SearchViewAnimateDefaults.BUTTON_ALPHA_DURATION_MILLIS
    } else {
        SearchViewAnimateDefaults.NON_INSTANT_BUTTON_ALPHA_DURATION_MILLIS
    }
    val revealProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = SearchViewAnimateDefaults.REVEAL_DURATION_MILLIS,
            easing = SearchViewAnimateDefaults.MoveEase,
        ),
        label = "searchViewAnimateReveal",
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = buttonAlphaDuration,
            easing = SearchViewAnimateDefaults.MoveEase,
        ),
        label = "searchViewAnimateButtonAlpha",
    )

    // setSearchViewAnimateHeightPercent: the bar's own collapse geometry.
    val height = (SearchViewAnimateDefaults.WrapperHeight * collapseHeightScale(collapsePercent))
        .coerceAtLeast(SearchViewAnimateDefaults.CollapsedMinHeight)
    val translationY = (SearchViewAnimateDefaults.AnimateHeight / 2) * (1f - collapsePercent)
    val contentAlpha = (collapsePercent - 0.5f) * 2f

    // setSearchViewAnimateHeightPercent blends the background from ?couiColorDivider (fully collapsed)
    // to coui_search_view_selector_color_normal (fully open) on ArgbEvaluator at clampMarginValue(f).
    val collapsedBackground = lerp(
        colors.dividerColor,
        colors.background,
        clampMarginValue(collapsePercent),
    )

    // playPressAnimation / playReleaseAnimation: 150ms COUIEaseInterpolator ramp between the normal and
    // pressed selectors.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        targetValue = if (pressed) colors.pressedBackground else collapsedBackground,
        animationSpec = tween(
            durationMillis = SearchViewAnimateDefaults.PRESS_FEEDBACK_DURATION_MILLIS,
            easing = SearchViewAnimateDefaults.Ease,
        ),
        label = "searchViewAnimateBackground",
    )

    val inputTextStyle = COUITheme.textStyles.main.copy(fontSize = SearchViewAnimateDefaults.InputTextSize)
    val cancelTextStyle = COUITheme.textStyles.button.copy(fontSize = SearchViewAnimateDefaults.CancelTextSize)

    Box(
        modifier = modifier
            .graphicsLayer {
                this.translationY = translationY.toPx()
                alpha = contentAlpha
            }
            .height(height)
            .clip(CircleShape)
            .background(backgroundColor)
            // dispatchTouchEvent / onInterceptTouchEvent: a tap on the collapsed bar expands it; while
            // collapsed only the icon itself is allowed to pass the touch through.
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !expanded,
            ) { currentOnExpandedChange(true) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SearchViewAnimateDefaults.WrapperHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = SearchViewAnimateDefaults.SearchIconMargin)
                    .size(SearchViewAnimateDefaults.IconSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.size(SearchViewAnimateDefaults.IconSize),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = revealProgress },
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = currentOnQueryChange,
                    enabled = expanded,
                    singleLine = true,
                    textStyle = inputTextStyle.copy(color = colors.inputTextColor),
                    cursorBrush = SolidColor(colors.cursorColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { currentOnSearch(query) }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty() && hint.isNotEmpty()) {
                                Text(
                                    text = hint,
                                    color = colors.hintColor,
                                    style = inputTextStyle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            if (type == SearchViewAnimateType.NonInstantSearch) {
                // onMeasure: the divider is laid out with the functional button and the input field
                // gives up (2 * coui_search_view_background_end_gap + button + divider) of end margin.
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandHorizontally(
                        animationSpec = tween(
                            SearchViewAnimateDefaults.REVEAL_DURATION_MILLIS,
                            easing = SearchViewAnimateDefaults.MoveEase,
                        ),
                    ) + fadeIn(tween(buttonAlphaDuration, easing = SearchViewAnimateDefaults.MoveEase)),
                    exit = shrinkHorizontally(
                        animationSpec = tween(
                            SearchViewAnimateDefaults.REVEAL_DURATION_MILLIS,
                            easing = SearchViewAnimateDefaults.MoveEase,
                        ),
                    ) + fadeOut(tween(buttonAlphaDuration, easing = SearchViewAnimateDefaults.MoveEase)),
                ) {
                    Spacer(
                        modifier = Modifier
                            .padding(start = SearchViewAnimateDefaults.ButtonDividerMarginStart)
                            .width(SearchViewAnimateDefaults.DividerWidth)
                            .height(SearchViewAnimateDefaults.DividerHeight)
                            .background(colors.dividerColor),
                    )
                }
            }
            val buttonStartMargin = if (type == SearchViewAnimateType.InstantSearch) {
                SearchViewAnimateDefaults.CancelMarginLarge
            } else {
                SearchViewAnimateDefaults.CancelMarginSmall
            }
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = buttonAlpha }
                    .padding(start = buttonStartMargin, end = SearchViewAnimateDefaults.CancelMarginSmall),
            ) {
                if (cancelButton != null) {
                    cancelButton()
                } else {
                    SearchViewAnimateFunctionalButton(
                        text = cancelText,
                        style = cancelTextStyle,
                        color = colors.cancelTextColor,
                        enabled = expanded,
                        onClick = { currentOnExpandedChange(false) },
                    )
                }
            }
        }
    }
}

/**
 * The trailing `COUISearchViewAnimate$SearchFunctionalButton`: the Cancel label on the
 * `coui_searchview_cancel_button_bg_radius` (24dp) capsule press ripple
 * `COUITextViewCompatUtil.setPressRippleDrawable` applies.
 */
@Composable
private fun SearchViewAnimateFunctionalButton(
    text: String,
    style: TextStyle,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = SearchViewAnimateDefaults.CancelButtonMinHeight)
            .clip(RoundedCornerShape(SearchViewAnimateDefaults.CancelButtonBackgroundRadius))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = SearchViewAnimateDefaults.CancelButtonPaddingHorizontal,
                vertical = SearchViewAnimateDefaults.CancelButtonPaddingVertical,
            )
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The two animation layouts of `COUISearchViewAnimate`: `TYPE_INSTANT_SEARCH` (0) and
 * `TYPE_NON_INSTANT_SEARCH` (1).
 */
enum class SearchViewAnimateType {
    /** `COUISearchViewAnimate.TYPE_INSTANT_SEARCH` (0): no divider between input and button. */
    InstantSearch,

    /** `COUISearchViewAnimate.TYPE_NON_INSTANT_SEARCH` (1): the 1dp `search_linear_divider` shows. */
    NonInstantSearch,
}

/**
 * The two states of `COUISearchViewAnimate`: `STATE_NORMAL` (0) and `STATE_EDIT` (1).
 *
 * @property expanded Whether the state is the expanded edit state; the composable takes this directly
 *   as its `expanded` parameter.
 */
enum class SearchViewState(val expanded: Boolean) {
    /** `COUISearchViewAnimate.STATE_NORMAL` (0): only the icon shows. */
    Normal(false),

    /** `COUISearchViewAnimate.STATE_EDIT` (1): the input field and the functional button show. */
    Edit(true),
}

/** Contains default values used by [SearchViewAnimate]. */
object SearchViewAnimateDefaults {

    /** The default animation layout: `COUISearchViewAnimate.mSearchViewType` starts at 1. */
    val Type = SearchViewAnimateType.NonInstantSearch

    /** The default collapse progress: `COUISearchViewAnimate.mCollapsingHeightPercent` starts at 1. */
    val CollapsePercent = 1f

    /** The Cancel label. COUI `string/coui_search_view_cancel` ("Cancel"). */
    const val CANCEL_TEXT = "Cancel"

    /**
     * The height of the search view wrapper row, and the layout's `android:layout_height`
     * (COUI `dimen/coui_search_view_wrapper_height`, 40dp).
     */
    val WrapperHeight = 40.dp

    /**
     * The height of the whole animated bar (COUI `dimen/coui_search_view_animate_height`, 50dp). It
     * only feeds the collapse translation: `translationY = animateHeight / 2 * (1 - f)`.
     */
    val AnimateHeight = 50.dp

    /**
     * The floor of the collapsed wrapper height (COUI `dimen/coui_search_view_collapsed_min_height`).
     * COUI declares it as `1px` — a single device pixel, enough to keep the collapsing wrapper
     * measurable — and this port expresses that floor as 1dp.
     */
    val CollapsedMinHeight = 1.dp

    /**
     * The size of the leading search glyph, the `coui_search_view_icon` ImageView in
     * `coui_search_view_animate_layout.xml` (COUI `dimen/coui_search_view_icon_size`, 24dp).
     */
    val IconSize = 24.dp

    /** The start margin of the leading glyph (COUI `dimen/coui_search_view_search_icon_margin`, 12dp). */
    val SearchIconMargin = 12.dp

    /** The corner radius of the bar. COUI `dimen/coui_search_view_corner` (18dp); the capsule clamps it to `height / 2`. */
    val CornerRadius = 18.dp

    /** The input and hint text size (COUI `dimen/coui_search_view_input_text_size`, 15sp). */
    val InputTextSize = 15.sp

    /** The Cancel label size (COUI `dimen/coui_search_view_cancel_text_size`, 14sp). */
    val CancelTextSize = 14.sp

    /** The Cancel button capsule radius (COUI `dimen/coui_searchview_cancel_button_bg_radius`, 24dp). */
    val CancelButtonBackgroundRadius = 24.dp

    /** The minimum height of the Cancel button (COUI `dimen/coui_search_bar_inner_search_icon_size`, 36dp). */
    val CancelButtonMinHeight = 36.dp

    /** The horizontal padding of the Cancel button press area (COUI `dimen/coui_search_cancel_button_bg_padding_horizontal`, 8dp). */
    val CancelButtonPaddingHorizontal = 8.dp

    /** The vertical padding of the Cancel button press area (COUI `dimen/coui_search_cancel_button_bg_padding_vertical`, 4dp). */
    val CancelButtonPaddingVertical = 4.dp

    /**
     * The start margin of the Cancel button in the `TYPE_NON_INSTANT_SEARCH` layout
     * (COUI `dimen/coui_search_view_cancel_margin_small`, 4dp).
     */
    val CancelMarginSmall = 4.dp

    /**
     * The start margin of the Cancel button in the `TYPE_INSTANT_SEARCH` layout
     * (COUI `dimen/coui_search_view_cancel_margin_large`, 8dp).
     */
    val CancelMarginLarge = 8.dp

    /** The start margin of the divider between input and button (COUI `dimen/coui_search_view_button_divider_margin_start`, 0dp). */
    val ButtonDividerMarginStart = 0.dp

    /** The width of the divider (COUI `dimen/coui_search_linear_divider_width`, 1dp on xxhdpi). */
    val DividerWidth = 1.dp

    /** The height of the divider (the `android:height` of `drawable/search_linear_divider`, 16dp). */
    val DividerHeight = 16.dp

    /** `COUISearchViewAnimate.DEFAULT_SEARCH_VIEW_OFFSET_CHANGE_DURATION` / `..._SCALE_...` (450ms). */
    const val REVEAL_DURATION_MILLIS = 450

    /** `COUISearchViewAnimate.DEFAULT_BUTTON_ALPHA_CHANGE_DURATION` (350ms). */
    const val BUTTON_ALPHA_DURATION_MILLIS = 350

    /** `COUISearchViewAnimate.NON_INSTANT_SEARCH_BUTTON_ALPHA_CHANGE_DURATION` (100ms). */
    const val NON_INSTANT_BUTTON_ALPHA_DURATION_MILLIS = 100

    /** `COUISearchViewAnimate.DEFAULT_PRESS_FEEDBACK_DURATION` (150ms). */
    const val PRESS_FEEDBACK_DURATION_MILLIS = 150

    /** `COUISearchViewAnimate.DEFAULT_FADE_DURATION` (150ms). */
    const val FADE_DURATION_MILLIS = 150

    /**
     * `COUISearchViewAnimate.CLAMP_ANIMATION_PERCENT` (0.3): the collapse fraction over which
     * `clampMarginValue` ramps from 0 to 1.
     */
    const val CLAMP_ANIMATION_PERCENT = 0.3f

    /** `COUIMoveEaseInterpolator`: `PathInterpolator(0.3f, 0f, 0.1f, 1f)`. */
    val MoveEase: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

    /** `COUIEaseInterpolator`: `PathInterpolator(0.33f, 0f, 0.67f, 1f)`. */
    val Ease: Easing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

    /**
     * Creates the default [SearchViewAnimateColors], mapping the `COUISearchViewAnimate` style:
     * `coui_search_view_selector_color_normal` / `_pressed` for the bar fill, `couiColorDivider` for
     * the divider and the collapsed tint, `coui_search_view_hint_color` for the placeholder,
     * `coui_search_view_input_text_color` for the text, `coui_searchview_cancel_text_color` for the
     * Cancel label and `couiColorSecondNeutral` for the glyph.
     */
    @Composable
    fun colors(
        background: Color = SearchViewAnimateSelectorNormal(),
        pressedBackground: Color = SearchViewAnimateSelectorPressed(),
        dividerColor: Color = COUITheme.colorScheme.dividerLine,
        hintColor: Color = SearchViewAnimateHintColor(),
        inputTextColor: Color = SearchViewAnimateInputTextColor(),
        cancelTextColor: Color = SearchViewAnimateCancelTextColor(),
        iconTint: Color = COUITheme.colorScheme.onSurfaceSecondary,
        cursorColor: Color = COUITheme.colorScheme.primary,
    ): SearchViewAnimateColors = remember(
        background,
        pressedBackground,
        dividerColor,
        hintColor,
        inputTextColor,
        cancelTextColor,
        iconTint,
        cursorColor,
    ) {
        SearchViewAnimateColors(
            background = background,
            pressedBackground = pressedBackground,
            dividerColor = dividerColor,
            hintColor = hintColor,
            inputTextColor = inputTextColor,
            cancelTextColor = cancelTextColor,
            iconTint = iconTint,
            cursorColor = cursorColor,
        )
    }
}

/**
 * Colors used by a [SearchViewAnimate].
 *
 * @param background The bar fill in the normal state (`coui_search_view_selector_color_normal`).
 * @param pressedBackground The bar fill while pressed (`coui_search_view_selector_color_pressed`).
 * @param dividerColor `couiColorDivider`: the divider between input and button, and the tint the fill
 *   starts from when the bar is fully collapsed.
 * @param hintColor The placeholder color (`coui_search_view_hint_color`).
 * @param inputTextColor The input text color (`coui_search_view_input_text_color`).
 * @param cancelTextColor The Cancel label color (`coui_searchview_cancel_text_color`).
 * @param iconTint The leading glyph tint (`couiColorSecondNeutral`).
 * @param cursorColor The text cursor color.
 */
@Immutable
data class SearchViewAnimateColors(
    val background: Color,
    val pressedBackground: Color,
    val dividerColor: Color,
    val hintColor: Color,
    val inputTextColor: Color,
    val cancelTextColor: Color,
    val iconTint: Color,
    val cursorColor: Color,
)

/**
 * `clampMarginValue(f)` of `COUISearchViewAnimate`: `max(0, min(1, f / CLAMP_ANIMATION_PERCENT))`.
 */
private fun clampMarginValue(f: Float): Float = (f / SearchViewAnimateDefaults.CLAMP_ANIMATION_PERCENT).coerceIn(0f, 1f)

/**
 * `clampSearchViewHeight(f)` of `COUISearchViewAnimate`: `f / 0.7f - 0.42857146f`, the factor the
 * wrapper height is scaled by as the bar collapses.
 */
private fun collapseHeightScale(f: Float): Float = f / 0.7f - 0.42857146f

/** coui_search_view_selector_color_normal in light themes (#14000000). */
@Composable
private fun SearchViewAnimateSelectorNormal(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_NORMAL_DARK else SEARCH_VIEW_ANIMATE_NORMAL_LIGHT

/** coui_search_view_selector_color_pressed in light themes (#1e000000). */
@Composable
private fun SearchViewAnimateSelectorPressed(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_PRESSED_DARK else SEARCH_VIEW_ANIMATE_PRESSED_LIGHT

/** coui_search_view_hint_color: #23000000 light / #33ffffff dark. */
@Composable
private fun SearchViewAnimateHintColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_HINT_DARK else SEARCH_VIEW_ANIMATE_HINT_LIGHT

/** coui_search_view_input_text_color: #ff000000 light / #ffffffff dark. */
@Composable
private fun SearchViewAnimateInputTextColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_INPUTTEXT_DARK else SEARCH_VIEW_ANIMATE_INPUTTEXT_LIGHT

/** coui_searchview_cancel_text_color: #ff000000 light / #ffffffff dark. */
@Composable
private fun SearchViewAnimateCancelTextColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_CANCELTEXT_DARK else SEARCH_VIEW_ANIMATE_CANCELTEXT_LIGHT

/** coui_search_view_selector_color_normal (night) #1affffff. */
private val SEARCH_VIEW_ANIMATE_NORMAL_DARK = Color(0x1AFFFFFF)

/** coui_search_view_selector_color_normal #14000000. */
private val SEARCH_VIEW_ANIMATE_NORMAL_LIGHT = Color(0x14000000)

/** coui_search_view_selector_color_pressed (night) #33ffffff. */
private val SEARCH_VIEW_ANIMATE_PRESSED_DARK = Color(0x33FFFFFF)

/** coui_search_view_selector_color_pressed #1e000000. */
private val SEARCH_VIEW_ANIMATE_PRESSED_LIGHT = Color(0x1E000000)

/** coui_search_view_hint_color (night) #33ffffff. */
private val SEARCH_VIEW_ANIMATE_HINT_DARK = Color(0x33FFFFFF)

/** coui_search_view_hint_color #23000000. */
private val SEARCH_VIEW_ANIMATE_HINT_LIGHT = Color(0x23000000)

/** coui_search_view_input_text_color (night) #ffffffff. */
private val SEARCH_VIEW_ANIMATE_INPUTTEXT_DARK = Color(0xFFFFFFFF)

/** coui_search_view_input_text_color #ff000000. */
private val SEARCH_VIEW_ANIMATE_INPUTTEXT_LIGHT = Color(0xFF000000)

/** coui_searchview_cancel_text_color_dark #ffffffff. */
private val SEARCH_VIEW_ANIMATE_CANCELTEXT_DARK = Color(0xFFFFFFFF)

/** coui_searchview_cancel_text_color_light #ff000000. */
private val SEARCH_VIEW_ANIMATE_CANCELTEXT_LIGHT = Color(0xFF000000)
