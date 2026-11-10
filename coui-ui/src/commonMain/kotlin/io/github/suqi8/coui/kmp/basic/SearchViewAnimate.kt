// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Close
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
 * - collapsed, the bar shows the 24dp `coui_search_view_icon` glyph *and the hint text* — the native
 *   class only hides the functional button and the divider in `changeStateWithOutAnimation(0)`, the
 *   search view itself stays at alpha 1;
 * - expanded, the trailing `SearchFunctionalButton` (the Cancel label) fades in and the input field
 *   narrows to make room for it.
 *
 * Only the trailing button and the divider are tied to [expanded]: `initButtonEnterAnimator` runs for
 * [SearchViewAnimateDefaults.BUTTON_ALPHA_DURATION_MILLIS] (350ms,
 * `DEFAULT_BUTTON_ALPHA_CHANGE_DURATION`) on the instant layout with a 100ms start delay, and for
 * [SearchViewAnimateDefaults.NON_INSTANT_BUTTON_ALPHA_DURATION_MILLIS] (100ms,
 * `NON_INSTANT_SEARCH_BUTTON_ALPHA_CHANGE_DURATION`) on the non-instant layout, both on
 * `COUIMoveEaseInterpolator` (`pathInterpolator(0.3, 0, 0.1, 1)`,
 * [SearchViewAnimateDefaults.MoveEase]).
 *
 * The native view never rotates the glyph by itself: `startSearchIconEnterAnimator` /
 * `startSearchIconExitAnimator` are public helpers that no code in the class calls, so the icon stays
 * upright unless the host drives it.
 *
 * [collapsePercent] is `setSearchViewAnimateHeightPercent`: the app feeds it the collapse progress of
 * the host (0 = fully collapsed to the icon, 1 = fully open) and the bar derives its own height,
 * vertical translation, glyph and input alpha and background tint from it. The background itself is
 * drawn by `onDraw` and is *not* part of the fade.
 *
 * @param query The text shown in the input field.
 * @param onQueryChange Called with the updated text.
 * @param expanded Whether the bar is in the edit state (`SearchViewState.Edit`).
 * @param onExpandedChange Called when the bar wants to enter or leave the edit state, e.g. when the
 *   collapsed bar is tapped or the Cancel button is pressed.
 * @param modifier The modifier to be applied to the bar.
 * @param hint The placeholder shown while [query] is empty.
 * @param type Which of the two `COUISearchViewAnimate` layouts is used: `TYPE_INSTANT_SEARCH` (0)
 *   places the functional button directly after the input field, `TYPE_NON_INSTANT_SEARCH` (1) puts
 *   the 1dp `search_linear_divider` between them.
 * @param collapsePercent The collapse progress fed to `setSearchViewAnimateHeightPercent`, 0..1.
 * @param onSearch Called with the current [query] when the IME search action fires.
 * @param cancelText The label of the trailing functional button (`string/coui_search_view_cancel`).
 * @param cancelButton Replaces the default trailing `SearchFunctionalButton` entirely.
 * @param leadingIcon The glyph shown at the start (defaults to the 24dp `coui_search_view_icon`).
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

    // initButtonEnterAnimator / initButtonExitAnimator pick the alpha duration by type, and the
    // instant layout additionally waits 100ms (`setStartDelay(mSearchViewType != 0 ? 0L : 100L)`).
    val instant = type == SearchViewAnimateType.InstantSearch
    val buttonAlphaDuration = if (instant) {
        SearchViewAnimateDefaults.BUTTON_ALPHA_DURATION_MILLIS
    } else {
        SearchViewAnimateDefaults.NON_INSTANT_BUTTON_ALPHA_DURATION_MILLIS
    }
    val buttonEnter = fadeIn(
        animationSpec = tween(
            durationMillis = buttonAlphaDuration,
            delayMillis = if (instant) SearchViewAnimateDefaults.INSTANT_BUTTON_ALPHA_DELAY_MILLIS else 0,
            easing = SearchViewAnimateDefaults.MoveEase,
        ),
    )
    val buttonExit = fadeOut(
        animationSpec = tween(durationMillis = buttonAlphaDuration, easing = SearchViewAnimateDefaults.MoveEase),
    )

    // setSearchViewAnimateHeightPercent: the bar's own collapse geometry.
    val density = LocalDensity.current
    val height = (SearchViewAnimateDefaults.WrapperHeight * collapseHeightScale(collapsePercent))
        .coerceAtLeast(with(density) { SearchViewAnimateDefaults.CollapsedMinHeightPx.toDp() })
    val translationY = (SearchViewAnimateDefaults.AnimateHeight / 2) * (1f - collapsePercent)
    // `mSearchView.setAlpha(f4)` / `mSearchIcon.setAlpha(f4)`: only the glyph and the input fade with
    // the collapse progress — the background keeps its evaluated tint at full alpha.
    val contentAlpha = (collapsePercent - 0.5f) * 2f

    // setSearchViewAnimateHeightPercent blends the background from ?couiColorDivider (fully collapsed)
    // to coui_search_view_selector_color_normal (fully open) with ArgbEvaluator at clampMarginValue(f).
    val collapsedBackground = argbLerp(
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
            .graphicsLayer { this.translationY = translationY.toPx() }
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
                    .size(SearchViewAnimateDefaults.IconSize)
                    .graphicsLayer { alpha = contentAlpha },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.size(SearchViewAnimateDefaults.IconSize),
                )
            }
            // `animated_search_view` carries `android:layout_marginStart="8dp"` after the glyph.
            Spacer(Modifier.width(SearchViewAnimateDefaults.IconInputGap))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = SearchViewAnimateDefaults.InputEndPadding)
                    .graphicsLayer { alpha = contentAlpha },
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = currentOnQueryChange,
                    modifier = Modifier.fillMaxWidth(),
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
            // `search_close_btn`: the SearchView shows its clear affordance while the query is not empty.
            if (expanded && query.isNotEmpty()) {
                SearchViewAnimateClearButton(
                    discColor = SearchViewAnimateClearDiscColor(),
                    onClick = { currentOnQueryChange("") },
                )
            }
            if (!instant) {
                // `mButtonDivider` is GONE while collapsed and fades with the functional button.
                AnimatedVisibility(visible = expanded, enter = buttonEnter, exit = buttonExit) {
                    Spacer(
                        modifier = Modifier
                            .padding(start = SearchViewAnimateDefaults.ButtonDividerMarginStart)
                            .width(SearchViewAnimateDefaults.DividerWidth)
                            .height(SearchViewAnimateDefaults.DividerHeight)
                            .background(colors.dividerColor),
                    )
                }
            }
            val buttonStartMargin = if (instant) {
                SearchViewAnimateDefaults.CancelMarginLarge
            } else {
                SearchViewAnimateDefaults.CancelMarginSmall
            }
            AnimatedVisibility(visible = expanded, enter = buttonEnter, exit = buttonExit) {
                Box(
                    modifier = Modifier
                        .widthIn(max = SearchViewAnimateDefaults.CancelButtonMaxWidth)
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
}

/**
 * The trailing `COUISearchViewAnimate$SearchFunctionalButton`: the Cancel label on the
 * `coui_searchview_cancel_button_bg_radius` capsule press ripple
 * `COUITextViewCompatUtil.setPressRippleDrawable` applies. The layout gives it
 * `android:minWidth="0dp"` / `android:minHeight="0dp"`, so it is only as tall as its text, and
 * `setMaxWidth(coui_search_function_button_max_width)` caps it at 152dp with an end ellipsis.
 */
@Composable
private fun SearchViewAnimateFunctionalButton(
    text: String,
    style: TextStyle,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val squircleEnabled = isSquircleEnabled()
    val buttonShape = remember(squircleEnabled) {
        SquircleShape(
            cornerRadius = SearchViewAnimateDefaults.CancelButtonBackgroundRadius,
            squircleEnabled = squircleEnabled,
        )
    }
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = SearchViewAnimateDefaults.CancelButtonMinHeight)
            .widthIn(max = SearchViewAnimateDefaults.CancelButtonMaxWidth)
            .clip(buttonShape)
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
 * `search_close_btn`: an 18dp `?attr/couiColorHintNeutral` disc with a white cross
 * (`drawable/ic_edit_text_delete_search_view`, a 36dp viewport holding a radius-9 circle and a
 * #ffffff cross), laid out in the 36dp slot the SearchView reserves at the end of the input.
 */
@Composable
private fun SearchViewAnimateClearButton(
    discColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SearchViewAnimateDefaults.ClearButtonSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(SearchViewAnimateDefaults.ClearButtonDiscSize)
                .background(discColor, CircleShape),
        )
        Icon(
            imageVector = COUIIcons.Basic.Close,
            contentDescription = SearchViewAnimateDefaults.CLEAR_TEXT,
            tint = Color.White,
            modifier = Modifier.size(SearchViewAnimateDefaults.ClearButtonGlyphSize),
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
    /** `COUISearchViewAnimate.STATE_NORMAL` (0): only the icon and the hint show. */
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
     * The accessibility label of the clear affordance, the system's
     * `string/support_abc_searchview_description_clear`.
     */
    const val CLEAR_TEXT = "Clear"

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
     * The floor of the collapsed wrapper height (COUI `dimen/coui_search_view_collapsed_min_height`,
     * `1px`). The floor only has to keep the collapsing wrapper measurable, so it is taken in device
     * pixels via [CollapsedMinHeightPx] rather than rounded up to a dp.
     *
     * Kept as the dp form for callers that lay the bar out in dp.
     */
    val CollapsedMinHeight = 1.dp

    /** [CollapsedMinHeight] as the token declares it, a single device pixel. */
    val CollapsedMinHeightPx = 1f

    /**
     * The glyph flip `startSearchIconExitAnimator` / `startSearchIconEnterAnimator` animate to
     * (`mSearchIcon.animate().setDuration(150L).rotationY(80.0f)`). No code inside
     * `COUISearchViewAnimate` calls those helpers, so [SearchViewAnimate] never applies it by itself —
     * the constant is kept for hosts that drive the same animation.
     */
    const val ICON_ROTATION_Y = 80f

    /** The duration of the glyph flip, from the same `setDuration(150L)` call. */
    const val ICON_ROTATION_DURATION_MILLIS = 150

    /**
     * The size of the leading search glyph, the `coui_search_view_icon` ImageView in
     * `coui_search_view_animate_layout.xml` (COUI `dimen/coui_search_view_icon_size`, 24dp).
     */
    val IconSize = 24.dp

    /** The start margin of the leading glyph (COUI `dimen/coui_search_view_search_icon_margin`, 12dp). */
    val SearchIconMargin = 12.dp

    /**
     * The gap between the glyph and the input field: `animated_search_view` carries
     * `android:layout_marginStart="8dp"` in `coui_search_view_animate_layout.xml`.
     */
    val IconInputGap = 8.dp

    /** The corner radius of the bar. COUI `dimen/coui_search_view_corner` (18dp); the capsule clamps it to `height / 2`. */
    val CornerRadius = 18.dp

    /** The input and hint text size (COUI `dimen/coui_search_view_input_text_size`, 15sp). */
    val InputTextSize = 15.sp

    /** The end padding of the input field (COUI `dimen/coui_search_view_auto_complete_padding_end`, 16dp). */
    val InputEndPadding = 16.dp

    /** The Cancel label size (COUI `dimen/coui_search_view_cancel_text_size`, 14sp). */
    val CancelTextSize = 14.sp

    /**
     * The radius of the Cancel press ripple. `drawable/coui_searchview_cancel_button_bg` hardcodes
     * `android:radius="17.0dp"` even though `dimen/coui_searchview_cancel_button_bg_radius` says 24dp;
     * the rendered radius is the drawable's.
     */
    val CancelButtonBackgroundRadius = 17.dp

    /** The minimum height of the Cancel button (the layout declares `android:minHeight="0dp"`). */
    val CancelButtonMinHeight = 0.dp

    /** The max width of the Cancel button (COUI `dimen/coui_search_function_button_max_width`, 152dp). */
    val CancelButtonMaxWidth = 152.dp

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

    /** The slot the clear affordance occupies (`search_close_btn`, a 36dp drawable viewport). */
    val ClearButtonSize = 36.dp

    /** The visible disc of the clear affordance (`ic_edit_text_delete_search_view`, radius 9 of a 36 viewport). */
    val ClearButtonDiscSize = 18.dp

    /** The cross inside the clear disc (10dp of the 36dp viewport). */
    val ClearButtonGlyphSize = 12.dp

    /** `COUISearchViewAnimate.DEFAULT_SEARCH_VIEW_OFFSET_CHANGE_DURATION` / `..._SCALE_...` (450ms). */
    const val REVEAL_DURATION_MILLIS = 450

    /** `COUISearchViewAnimate.DEFAULT_BUTTON_ALPHA_CHANGE_DURATION` (350ms). */
    const val BUTTON_ALPHA_DURATION_MILLIS = 350

    /** `COUISearchViewAnimate.NON_INSTANT_SEARCH_BUTTON_ALPHA_CHANGE_DURATION` (100ms). */
    const val NON_INSTANT_BUTTON_ALPHA_DURATION_MILLIS = 100

    /** `setStartDelay(this.mSearchViewType != 0 ? 0L : 100L)` on the instant layout's button enter. */
    const val INSTANT_BUTTON_ALPHA_DELAY_MILLIS = 100

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
     * the divider and the collapsed tint, `couiColorLabelSecondary` (light) /
     * `coui_search_view_hint_color_dark` for the placeholder, `couiColorPrimaryNeutral` for the text,
     * `couiColorPrimaryText` for the Cancel label, `couiColorSecondNeutral` for the glyph and
     * `couiColorHintNeutral` for the clear disc.
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
 * @param hintColor The placeholder color (`couiColorLabelSecondary` in light themes,
 *   `coui_search_view_hint_color_dark` in dark ones).
 * @param inputTextColor The input text color (`couiColorPrimaryNeutral`).
 * @param cancelTextColor The Cancel label color (`couiColorPrimaryText`).
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

/**
 * `ArgbEvaluator.evaluate(fraction, start, end)` of `setSearchViewAnimateHeightPercent`: the native
 * evaluator interpolates the four 8-bit channels linearly, so this stays in sRGB rather than going
 * through the Oklab space `Color.lerp` uses.
 */
private fun argbLerp(start: Color, end: Color, fraction: Float): Color = Color(
    red = start.red + (end.red - start.red) * fraction,
    green = start.green + (end.green - start.green) * fraction,
    blue = start.blue + (end.blue - start.blue) * fraction,
    alpha = start.alpha + (end.alpha - start.alpha) * fraction,
)

/** coui_search_view_selector_color_normal in light themes (#14000000). */
@Composable
private fun SearchViewAnimateSelectorNormal(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_NORMAL_DARK else SEARCH_VIEW_ANIMATE_NORMAL_LIGHT

/** coui_search_view_selector_color_pressed in light themes (#1e000000). */
@Composable
private fun SearchViewAnimateSelectorPressed(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_PRESSED_DARK else SEARCH_VIEW_ANIMATE_PRESSED_LIGHT

/**
 * `normalHintColor`: the light style points at `coui_search_view_hint_color_selector`, which resolves
 * to `?attr/couiColorLabelSecondary` (#8a000000), the dark style overrides it with
 * `coui_search_view_hint_color_dark` (#33ffffff).
 */
@Composable
private fun SearchViewAnimateHintColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_HINT_DARK else SEARCH_VIEW_ANIMATE_HINT_LIGHT

/** `inputTextColor`: `?attr/couiColorPrimaryNeutral` (#e6000000 light) / #ffffffff dark. */
@Composable
private fun SearchViewAnimateInputTextColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_INPUTTEXT_DARK else SEARCH_VIEW_ANIMATE_INPUTTEXT_LIGHT

/** `functionalButtonTextColor`: `?attr/couiColorPrimaryText` (#0080ff light / #5c9dff dark). */
@Composable
private fun SearchViewAnimateCancelTextColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_CANCELTEXT_DARK else SEARCH_VIEW_ANIMATE_CANCELTEXT_LIGHT

/** `?attr/couiColorHintNeutral` (#42000000 light / #4dffffff dark), the clear disc. */
@Composable
private fun SearchViewAnimateClearDiscColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SEARCH_VIEW_ANIMATE_CLEAR_DISC_DARK else SEARCH_VIEW_ANIMATE_CLEAR_DISC_LIGHT

/** coui_search_view_selector_color_normal (night) #1affffff. */
private val SEARCH_VIEW_ANIMATE_NORMAL_DARK = Color(0x1AFFFFFF)

/** coui_search_view_selector_color_normal #14000000. */
private val SEARCH_VIEW_ANIMATE_NORMAL_LIGHT = Color(0x14000000)

/** coui_search_view_selector_color_pressed (night) #33ffffff. */
private val SEARCH_VIEW_ANIMATE_PRESSED_DARK = Color(0x33FFFFFF)

/** coui_search_view_selector_color_pressed #1e000000. */
private val SEARCH_VIEW_ANIMATE_PRESSED_LIGHT = Color(0x1E000000)

/** coui_search_view_hint_color_dark #33ffffff. */
private val SEARCH_VIEW_ANIMATE_HINT_DARK = Color(0x33FFFFFF)

/** couiColorLabelSecondary #8a000000, the light style's hint selector. */
private val SEARCH_VIEW_ANIMATE_HINT_LIGHT = Color(0x8A000000)

/** coui_search_view_input_text_color_dark #ffffffff. */
private val SEARCH_VIEW_ANIMATE_INPUTTEXT_DARK = Color(0xFFFFFFFF)

/** couiColorPrimaryNeutral #e6000000, the light style's input text color. */
private val SEARCH_VIEW_ANIMATE_INPUTTEXT_LIGHT = Color(0xE6000000)

/** coui_color_primary_text_blue_dark #5c9dff. */
private val SEARCH_VIEW_ANIMATE_CANCELTEXT_DARK = Color(0xFF5C9DFF)

/** coui_color_primary_text_blue #0080ff. */
private val SEARCH_VIEW_ANIMATE_CANCELTEXT_LIGHT = Color(0xFF0080FF)

/** coui_color_hint_neutral_dark #4dffffff. */
private val SEARCH_VIEW_ANIMATE_CLEAR_DISC_DARK = Color(0x4DFFFFFF)

/** coui_color_hint_neutral #42000000. */
private val SEARCH_VIEW_ANIMATE_CLEAR_DISC_LIGHT = Color(0x42000000)
