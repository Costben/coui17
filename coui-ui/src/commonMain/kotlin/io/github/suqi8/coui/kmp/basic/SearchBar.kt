// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.isBlurEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A [SearchBar] component with COUI style.
 *
 * @param inputField the input field to input a query in the [SearchBar].
 * @param onExpandedChange the callback to be invoked when the [SearchBar]'s expanded state is
 *   changed.
 * @param modifier the [Modifier] to be applied to the [SearchBar].
 * @param insideMargin The margin inside the [SearchBar].
 * @param expanded whether the [SearchBar] is expanded and showing search results.
 * @param outsideEndAction the action to be shown at the end side of the [SearchBar] when it is
 *   expanded.
 * @param content the content to be shown when the [SearchBar] is expanded.
 */
@Composable
fun SearchBar(
    inputField: @Composable () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    insideMargin: DpSize = SearchBarDefaults.InsideMargin,
    expanded: Boolean = false,
    outsideEndAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val actionSlideDistancePx = with(LocalDensity.current) { ActionSlideDistance.roundToPx() }
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = insideMargin.height, horizontal = insideMargin.width),
            ) {
                inputField()
            }
            // COUISearchBar edit-state transition: background resize, button slide and fade,
            // all with COUIMoveEaseInterpolator. Bound to a local so the non-null branch
            // stays smart-cast inside the trailing lambdas below.
            val endAction = outsideEndAction
            if (endAction != null) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandHorizontally(animationSpec = tween(BackgroundResizeDurationMillis, easing = MoveEase)) +
                        slideInHorizontally(
                            animationSpec = tween(ActionOffsetDurationMillis, easing = MoveEase),
                            initialOffsetX = { actionSlideDistancePx },
                        ) +
                        fadeIn(animationSpec = tween(ActionAlphaDurationMillis, easing = MoveEase)),
                    exit = shrinkHorizontally(animationSpec = tween(BackgroundResizeDurationMillis, easing = MoveEase)) +
                        slideOutHorizontally(
                            animationSpec = tween(ActionOffsetDurationMillis, easing = MoveEase),
                            targetOffsetX = { actionSlideDistancePx },
                        ) +
                        fadeOut(animationSpec = tween(ActionAlphaDurationMillis, easing = MoveEase)),
                ) {
                    Box(
                        modifier = Modifier.padding(
                            start = ActionExtraStartPadding,
                            end = ActionEndPadding,
                        ),
                    ) {
                        endAction()
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
        ) {
            content()
        }
    }

    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = expanded,
        onBackCompleted = {
            currentOnExpandedChange(false)
        },
    )
}

/**
 * A [SearchBar] that owns its own [InputField] and its edit-state "cancel" capsule.
 *
 * On Android, this delegates to the COUI-Kit native search implementation with native capsule geometry,
 * press feedback, spotlight illumination, and split cancel capsule dynamics.
 *
 * @param query the query text to be shown in the input field.
 * @param onQueryChange the callback to be invoked when the input service updates the query.
 * @param onSearch the callback invoked when the input service triggers the [ImeAction.Search] action.
 * @param onExpandedChange the callback to be invoked when the bar's expanded state is changed.
 * @param modifier the [Modifier] to be applied to the bar.
 * @param expanded whether the bar is expanded and showing search results.
 * @param label the hint shown while [query] is empty.
 * @param enabled the enabled state of the input field.
 * @param textStyle Style configuration applied at character level to the input text.
 * @param colors the colors of the field and of the cancel capsule.
 * @param stroke the material rim of the capsules, or `null` for no rim. Ignored on Android
 *   (Android uses COUI-Kit native capsule styling without outer stroke).
 * @param leadingIcon the leading icon of the input field. If provided, overrides the default search icon.
 * @param trailingIcon the trailing icon of the input field. If provided, overrides the default clear icon.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for the input field.
 * @param glowColor the caustic light cast below the capsules, or `null` to disable it. Ignored on Android
 *   (Android uses COUI-Kit native spotlight and shadow pipeline).
 * @param backdrop the backdrop blurred under the capsules, or `null` for a flat fill.
 * @param insideMargin the margin around the bar's content, `(16dp, 6dp)` by default: the 40dp
 *   capsule inside the 52dp search band.
 * @param cancelText the label of the cancel capsule, or `null` for a bar without one.
 * @param onCancel the callback invoked when the cancel capsule is clicked; collapses by default.
 * @param metaBallFusion whether the cancel capsule may merge with the field. Ignored on Android
 *   (Android follows COUI-Kit native dual-capsule spring dynamics).
 * @param clearContentDescription the accessibility label of the default clear button.
 * @param content the content to be shown when the bar is expanded.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: SearchBarColors = SearchBarDefaults.searchBarColors(),
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    glowColor: Color? = null,
    backdrop: Backdrop? = null,
    insideMargin: DpSize = SearchBarDefaults.InsideMargin,
    cancelText: String? = null,
    onCancel: () -> Unit = { onExpandedChange(false) },
    metaBallFusion: Boolean = SearchBarDefaults.metaBallFusion(),
    clearContentDescription: String = SearchBarDefaults.ClearContentDescription,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    SearchBarImpl(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        expanded = expanded,
        label = label,
        enabled = enabled,
        textStyle = textStyle,
        colors = colors,
        stroke = stroke,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        glowColor = glowColor,
        backdrop = backdrop,
        insideMargin = insideMargin,
        cancelText = cancelText,
        onCancel = onCancel,
        metaBallFusion = metaBallFusion,
        clearContentDescription = clearContentDescription,
        content = content,
    )
}

/**
 * A text field to input a query in a search bar with COUI style.
 *
 * @param query the query text to be shown in the input field.
 * @param onQueryChange the callback to be invoked when the input service updates the query. An
 *   updated text comes as a parameter of the callback.
 * @param onSearch the callback to be invoked when the input service triggers the
 *   [ImeAction.Search] action. The current [query] comes as a parameter of the callback.
 * @param expanded whether the search bar is expanded and showing search results.
 * @param onExpandedChange the callback to be invoked when the search bar's expanded state is
 *   changed.
 * @param modifier the [Modifier] to be applied to this input field.
 * @param label the hint shown while [query] is empty.
 * @param enabled the enabled state of this input field.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this input
 *   field in different states. See [SearchBarDefaults.searchBarColors].
 * @param stroke the material rim and inner shadow drawn over the capsule, or `null` for a flat
 *   fill. Ignored on Android (Android uses COUI-Kit native capsule styling without outer stroke).
 * @param leadingIcon the leading icon to be displayed at the start of the input field. If provided,
 *   overrides the default search icon.
 * @param trailingIcon the trailing icon to be displayed at the end of the input field. If provided,
 *   overrides the default clear icon.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this input field.
 * @param glowColor the caustic light cast below the capsule, or `null` to disable it. Ignored on
 *   Android (Android uses COUI-Kit native spotlight and shadow pipeline).
 * @param backdrop the backdrop blurred under the capsule, or `null` for a flat fill.
 * @param clearContentDescription the accessibility label of the default clear button.
 */
@Composable
fun InputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: SearchBarColors = SearchBarDefaults.searchBarColors(),
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    glowColor: Color? = null,
    backdrop: Backdrop? = null,
    clearContentDescription: String = SearchBarDefaults.ClearContentDescription,
) {
    InputFieldImpl(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        label = label,
        enabled = enabled,
        textStyle = textStyle,
        colors = colors,
        stroke = stroke,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        glowColor = glowColor,
        backdrop = backdrop,
        clearContentDescription = clearContentDescription,
    )
}

/** Binary-compatibility bridge for the [InputField] overload that predates `clearContentDescription`. */
@Deprecated(
    message = "Migrated to the overload that adds clearContentDescription.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun InputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: SearchBarColors = SearchBarDefaults.searchBarColors(),
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    glowColor: Color? = null,
    backdrop: Backdrop? = null,
) {
    InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        label = label,
        enabled = enabled,
        textStyle = textStyle,
        colors = colors,
        stroke = stroke,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        glowColor = glowColor,
        backdrop = backdrop,
    )
}

/** Binary-compatibility bridge for the [InputField] overload that predates `backdrop`. */
@Deprecated(
    message = "Migrated to the overload that adds backdrop.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun InputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: SearchBarColors = SearchBarDefaults.searchBarColors(),
    stroke: MaterialStrokeParams? = SearchBarDefaults.searchBarStroke(),
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    glowColor: Color? = SearchBarDefaults.causticGlowColor(),
) {
    InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        label = label,
        enabled = enabled,
        textStyle = textStyle,
        colors = colors,
        stroke = stroke,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
        glowColor = glowColor,
        backdrop = null,
    )
}

@Composable
internal expect fun SearchBarImpl(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean,
    label: String,
    enabled: Boolean,
    textStyle: TextStyle?,
    colors: SearchBarColors,
    stroke: MaterialStrokeParams?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
    glowColor: Color?,
    backdrop: Backdrop?,
    insideMargin: DpSize,
    cancelText: String?,
    onCancel: () -> Unit,
    metaBallFusion: Boolean,
    clearContentDescription: String,
    content: @Composable ColumnScope.() -> Unit,
)

@Composable
internal expect fun InputFieldImpl(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean,
    textStyle: TextStyle?,
    colors: SearchBarColors,
    stroke: MaterialStrokeParams?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    interactionSource: MutableInteractionSource?,
    glowColor: Color?,
    backdrop: Backdrop?,
    clearContentDescription: String,
)

/** Contains default values used by [SearchBar] and [InputField]. */
object SearchBarDefaults {
    /**
     * The blur sigma of `FRAMEWORK_TOP_BAR_BLUR_PARAMS`, in pixels: the preset `COUISearchBar` uses
     * when it is set to blur (material type 2). Its default preset blurs nothing.
     */
    const val NATIVE_BLUR_SIGMA_PX = 20f

    /**
     * The blur sigma of `FRAMEWORK_TOP_BAR_PARAMS`, the preset `COUISearchBar` resolves by default
     * (`ToolbarMaterialEffectResolver` maps preset 0 to `TYPE_FRAMEWORK_TOP_BAR`).
     *
     * The preset's `blurRadius` is 0: the capsule is the preset's two colour layers over a sharp
     * backdrop, which is why a `SearchBar` with a backdrop shows the content behind it unblurred.
     */
    const val NATIVE_TOP_BAR_BLUR_SIGMA_PX = 0f

    /**
     * The default inside margin of the [SearchBar].
     * COUI `coui_search_bar_responsive_horizontal_padding_compat` (16dp) x (52dp - 40dp) / 2.
     */
    val InsideMargin = DpSize(16.dp, 6.dp)

    /**
     * The default minimum height of the [InputField] background capsule.
     * COUI `coui_search_bar_normal_background_height` (40dp).
     */
    val InputFieldMinHeight = 40.dp

    /**
     * The default font size for the [InputField] text and label.
     * COUI `coui_search_view_input_text_size` (15sp).
     */
    val InputFieldFontSize = 15.sp

    /**
     * The start padding for the default leading icon: the COUI 4dp icon container start gap plus
     * (36dp container - 20dp glyph) / 2.
     */
    val LeadingIconStartPadding = 12.dp

    /**
     * The end padding for the default leading icon: (36dp container - 20dp glyph) / 2, equal to
     * COUI `coui_search_view_hint_padding_start`.
     */
    val LeadingIconEndPadding = 8.dp

    /**
     * The start padding for the default trailing (clear) button touch target.
     * COUI `coui_search_bar_edittext_end_gap` (4dp).
     */
    val TrailingIconStartPadding = 4.dp

    /**
     * The end padding for the default trailing (clear) button touch target.
     * COUI `coui_search_bar_non_instant_search_inner_gap` (4dp).
     */
    val TrailingIconEndPadding = 4.dp

    /**
     * The circular touch target size of the default trailing (clear) button.
     * COUI `coui_search_bar_inner_search_icon_size` (36dp).
     */
    val TrailingIconTouchTargetSize = 36.dp

    /**
     * The visual circle size of the default trailing (clear) button, matching the 9dp-radius
     * circle of ic_edit_text_delete_search_view.
     */
    val TrailingIconVisualSize = 18.dp

    /**
     * Creates a [SearchBarColors] with the default colors of the [InputField], following
     * COUISearchBar on the material path (the toolbar material layer capsule, couiColorLabelSecondary
     * hint, couiColorSecondNeutral glyph, couiColorHintNeutral clear circle).
     */
    @Composable
    fun searchBarColors(
        backgroundColor: Color = materialBarColor(),
        labelColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        iconColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        clearIconColor: Color = COUITheme.colorScheme.onSurfaceContainerHigh,
    ): SearchBarColors = remember(backgroundColor, labelColor, iconColor, clearIconColor) {
        SearchBarColors(
            backgroundColor = backgroundColor,
            labelColor = labelColor,
            iconColor = iconColor,
            clearIconColor = clearIconColor,
        )
    }

    /**
     * The accessibility label of the default clear button when the caller passes none.
     *
     * `SearchBarBackgroundView.ensureQuickDeleteButton` labels the system button with
     * `R.string.coui_search_clear_button_description`, which is localized per device language. A
     * multiplatform library ships no string resources, so [InputField] takes the copy as a
     * parameter; this value only keeps the previous English default.
     */
    @Suppress("ktlint:standard:property-naming")
    const val ClearContentDescription = "Search Cleanup"

    /**
     * The edge ColorOS draws on the search capsule: `TYPE_FRAMEWORK_CAPSULE_8`. On the material path
     * `COUISearchBar` resolves the rim through `getCapsuleStrokeEffectTypeInWindow`, which returns the
     * in-window capsule (`FRAMEWORK_CAPSULE_PARAMS_8_DARK`, edge alpha 0.15) instead of
     * `TYPE_FRAMEWORK_CAPSULE_1`; the capsule-1 rim (0.6) measures 177 on the device where the
     * settings capsule measures 78.
     */
    @Composable
    fun searchBarStroke(): MaterialStrokeParams = MaterialStrokeParams.frameworkCapsule8(COUITheme.colorScheme.background.luminance() < 0.5f)

    /**
     * The caustic light under the capsule, COUI `coui_caustic_color_1`: `#2EFFFFFF` in the light
     * theme and `#33FFFFFF` at night. `COUISearchBar.setCausticShadow` passes it to
     * `COUIMaterialEffect.setOutlineCausticShadowColor`.
     *
     * The system only reaches that call when `isCausticShadowEnable()` is true, and the flag behind
     * it (`mSystemCausticShadowState`) starts `false` and is only written by
     * `onMaterialStateChange(8, enabled)`. Both reference devices report it off: neither the
     * expanded search bar nor the settings home pill shows a light band under the capsule in either
     * theme (`#262626` capsule over a black page reads 30 under the bottom rim, with no lift below
     * it). [SearchBar] therefore takes `null` by default; pass this colour to opt in.
     */
    @Composable
    fun causticGlowColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
        Color(0x33FFFFFF)
    } else {
        Color(0x2EFFFFFF)
    }

    /**
     * `COUISearchBar`'s edit-state spring: `StateChangedToEditParamsHigh.getResponse()` and
     * `getBounce()`. The capsule width, the cancel capsule's scale and its offset all ride it, and
     * `StateChangedToNormalParamsHigh` carries the same pair for the collapse.
     *
     * The four `const val`s here keep the object's existing naming (`InsideMargin`,
     * `InputFieldMinHeight` and friends) rather than SCREAMING_SNAKE_CASE: renaming a published
     * `const val` removes its recorded API field, so they are suppressed instead.
     */
    @Suppress("ktlint:standard:property-naming")
    const val MetaBallSpringResponse = 0.5f

    /** See [MetaBallSpringResponse]; `COUISpringForce.setBounce` sets `dampingRatio = 1 - bounce`. */
    @Suppress("ktlint:standard:property-naming")
    const val MetaBallSpringBounce = 0.25f

    /**
     * `StateChangedToEditParamsHigh.getFunctionalButtonInitScale()`: the cancel capsule grows from
     * half size while the bar expands.
     */
    @Suppress("ktlint:standard:property-naming")
    const val CancelInitialScale = 0.5f

    /**
     * `COUISearchBar.mFunctionalButtonOffsetX`: `handleStateChangedAnimation`
     * (`COUISearchBar.java:972`, LTR branch) lays the background view out
     * `max(maxWidth, minWidth) + left + 20` wide, and the offset is that view's right edge minus
     * the button's (`:988-991`).
     *
     * The 20 is a **raw pixel** count, not dp: the native value goes straight to `View.layout`
     * without `applyDimension`, so on the reference 3.875× device it is 5.2dp while at 1× it is
     * 20dp. It is kept as-is to stay identical to the system rather than normalised by density.
     */
    @Suppress("ktlint:standard:property-naming")
    const val CancelSlidePx = 20f

    /**
     * Whether the cancel capsule may merge with the field, `COUISearchBar.setBallChain`'s
     * `mUseMaterialEffectAttr != 0 && isSupportBlur() && isBlurEnable()`. [isBlurEnabled] is this
     * library's equivalent: the local blur switch, the platform's, and a runtime shader to draw the
     * union. A bar without it draws two separate capsules.
     */
    @Composable
    fun metaBallFusion(): Boolean = isBlurEnabled()
}

/**
 * Colors used by the [InputField] of a [SearchBar].
 *
 * @param backgroundColor the color of the capsule background. On Android, mapped to the fallback background color.
 * @param labelColor the color of the label (hint) text. On Android, mapped to placeholder and secondary text color.
 * @param iconColor the tint of the default leading search icon. On Android, mapped to the leading search icon tint.
 * @param clearIconColor the fill color of the default clear button circle. On Android, mapped to the clear button fill color.
 *
 * Note: [SearchBarColors] specifies colors for the input field background, label, leading icon, and clear icon. Text color on Android is governed by [textStyle] or [COUITheme.colorScheme.onSurface].
 */
@Immutable
data class SearchBarColors(
    val backgroundColor: Color,
    val labelColor: Color,
    val iconColor: Color,
    val clearIconColor: Color,
)

@Composable
private fun materialBarColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
    Color(0xCC262626)
} else {
    Color(0x99FFFFFF)
}

private val MoveEase = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
private val BackgroundResizeDurationMillis = 450
private val ActionOffsetDurationMillis = 400
private val ActionAlphaDurationMillis = 350
private val ActionSlideDistance = 60.dp
private val ActionExtraStartPadding = 4.dp
private val ActionEndPadding = 16.dp

@Composable
internal fun SearchBarCausticGlow(color: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        MaterialCausticShadow(
            particles = listOf(
                CausticShadowParticle(
                    MetaBallParticle(
                        centerX = maxWidth / 2,
                        centerY = maxHeight / 2,
                        width = maxWidth,
                        height = maxHeight,
                        cornerRadius = maxHeight / 2,
                    ),
                    CausticShadowProfile.Toolbar,
                ),
            ),
            color = color,
            modifier = Modifier.matchParentSize(),
        )
    }
}
