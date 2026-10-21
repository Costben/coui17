// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor
import io.github.suqi8.coui.kmp.utils.hasFocusReassignBug
import kotlinx.coroutines.delay

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
 * @param label the hint shown while [query] is empty. The device keeps the hint behind the cursor
 *   in the expanded search bar until the first character is typed, so it is not a collapsed-only
 *   label.
 * @param enabled the enabled state of this input field. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this input
 *   field in different states. See [SearchBarDefaults.searchBarColors].
 * @param stroke the material rim and inner shadow drawn over the capsule, or `null` for a flat
 *   fill. Defaults to [SearchBarDefaults.searchBarStroke]. Pass `null` when the caller paints the
 *   edge on a wider capsule that this field only occupies part of.
 * @param leadingIcon the leading icon to be displayed at the start of the input field.
 * @param trailingIcon the trailing icon to be displayed at the end of the input field.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this input field. You can use this to change the search bar's
 *   appearance or preview the search bar in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param glowColor the caustic light cast below the capsule, or `null` to disable it. COUI paints
 *   it through `COUIMaterialEffect.setOutlineCausticShadowColor` with `coui_caustic_color_1`; the
 *   light is clipped to the outside of the capsule, so it never brightens the translucent fill.
 *   The device only lights the capsule up while the search bar is being edited — its collapsed
 *   pill on the settings home casts no light at all — so a collapsed stand-in should pass `null`.
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
    glowColor: Color? = SearchBarDefaults.causticGlowColor(),
) {
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentOnSearch by rememberUpdatedState(onSearch)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    // COUISearchBarDrawingProxyDrawable.setBackgroundRect: corner radius = rect.height() / 2.
    val capsuleShape = CircleShape

    val actualLeadingIcon = leadingIcon ?: {
        // coui_search_view_icon: 20dp glyph filled with couiColorSecondNeutral.
        Icon(
            modifier = Modifier.padding(start = SearchBarDefaults.LeadingIconStartPadding, end = SearchBarDefaults.LeadingIconEndPadding),
            imageVector = COUIIcons.Basic.Search,
            tint = colors.iconColor,
            contentDescription = "Search",
        )
    }

    val actualTrailingIcon = trailingIcon ?: {
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            // Clear button: a 36dp circular touch target holding an 18dp filled circle with a
            // white cross (ic_edit_text_delete_search_view).
            val clearCircleColor = colors.clearIconColor
            Box(
                modifier = Modifier
                    .padding(start = SearchBarDefaults.TrailingIconStartPadding, end = SearchBarDefaults.TrailingIconEndPadding)
                    .size(SearchBarDefaults.TrailingIconTouchTargetSize)
                    .clip(capsuleShape)
                    .clickable { currentOnQueryChange("") }
                    .semantics { contentDescription = "Search Cleanup" },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(SearchBarDefaults.TrailingIconVisualSize)) {
                    drawCircle(color = clearCircleColor)
                    // ic_edit_text_delete_search_view cross: 8.2dp span, 1.6dp stroke, always white.
                    val half = 4.1.dp.toPx()
                    val stroke = 1.6.dp.toPx()
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - half, center.y - half),
                        end = Offset(center.x + half, center.y + half),
                        strokeWidth = stroke,
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(center.x - half, center.y + half),
                        end = Offset(center.x + half, center.y - half),
                        strokeWidth = stroke,
                    )
                }
            }
        }
    }

    val focused = internalInteractionSource.collectIsFocusedAsState().value
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val textAlpha = remember { Animatable(1f) }

    val textColor = LocalContentColor.current
    // COUI input text: 16sp regular (couiEditTextTextAppearance).
    val inputTextStyle = COUITheme.textStyles.main
        .copy(fontSize = SearchBarDefaults.InputFieldFontSize)
        .merge(textStyle)
        .copy(color = textColor)

    val cursorBrush = SolidColor(COUITheme.colorScheme.primary)
    // The hint is an EditText hint, not a collapsed-only label: the device's expanded search bar
    // still reads "搜索设置项" behind the cursor until the first character is typed.
    val labelText by remember(query, label) {
        derivedStateOf { if (query.isEmpty()) label else "" }
    }

    // On API 26-27, focus is incorrectly reassigned after clearFocus(), preventing the
    // SearchBar from closing. Workaround: disable the TextField when collapsed and use
    // pointerInput to handle tap-to-expand. https://issuetracker.google.com/issues/433382598
    val workaroundEnabled = !hasFocusReassignBug || expanded
    val expandOnTapModifier = if (workaroundEnabled || !enabled) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) { detectTapGestures { currentOnExpandedChange(true) } }
    }

    BasicTextField(
        value = query,
        onValueChange = currentOnQueryChange,
        modifier = modifier
            .then(expandOnTapModifier)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) currentOnExpandedChange(true) }
            .semantics {
                onClick {
                    focusRequester.requestFocus()
                    true
                }
            },
        enabled = enabled && workaroundEnabled,
        singleLine = true,
        textStyle = inputTextStyle,
        cursorBrush = cursorBrush,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { currentOnSearch(query) }),
        interactionSource = internalInteractionSource,
        decorationBox = { innerTextField ->
            // COUISearchBar capsule on the material path: the toolbar material layer
            // (COUIMaterialBlurEffect top bar), not coui_search_bar_no_blur_solid. The material
            // edge is COUISearchBar's COUIShadowEdgeDrawable pass, painted over the fill.
            val edgeModifier = if (stroke != null) {
                Modifier.materialShadowEdge(stroke = stroke)
            } else {
                Modifier
            }
            Box {
                if (glowColor != null) {
                    // COUISearchBar's outline caustic is an outside-only light: the capsule is
                    // punched out of the glow layer, so the translucent fill stays its own colour.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawWithContent {
                                val radius = size.height / 2f
                                val capsule = Path().apply {
                                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, radius, radius))
                                }
                                clipPath(capsule, ClipOp.Difference) { this@drawWithContent.drawContent() }
                            },
                    ) {
                        SearchBarCausticGlow(
                            color = glowColor,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = colors.backgroundColor,
                            shape = capsuleShape,
                        )
                        .then(edgeModifier),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actualLeadingIcon()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = SearchBarDefaults.InputFieldMinHeight),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            val mergedLabelStyle = remember(textStyle) {
                                TextStyle(fontSize = SearchBarDefaults.InputFieldFontSize).merge(textStyle)
                            }
                            // Hint text: couiColorLabelSecondary (Widget.COUI.EditText textColorHint).
                            Text(
                                text = labelText,
                                style = mergedLabelStyle,
                                color = colors.labelColor,
                            )
                            Box(modifier = Modifier.graphicsLayer { alpha = textAlpha.value }) {
                                innerTextField()
                            }
                        }
                        actualTrailingIcon()
                    }
                }
            }
        },
    )

    LaunchedEffect(expanded) {
        if (expanded) {
            // Explicitly request focus when expanded. On API 26-27, the workaround disables
            // the TextField when collapsed, so the initial tap doesn't grant focus — this
            // ensures the keyboard appears after the TextField becomes enabled again.
            focusRequester.requestFocus()
        } else if (focused) {
            delay(100)
            if (query.isNotEmpty()) {
                textAlpha.animateTo(0f)
                currentOnQueryChange("")
                textAlpha.snapTo(1f)
            }
            focusManager.clearFocus()
        }
    }
}

/**
 * The caustic light `COUISearchBar.setCausticShadow` casts under the capsule. The native level-2
 * layout is `coui_caustic_layout_b_level2` (4.899994dp) and `coui_caustic_layout_sdf_level2`
 * (9.22998dp), which is exactly [CausticShadowProfile.Toolbar].
 *
 * Measured on the device, the band reaches +18 grey levels right under the capsule and is gone by
 * 4.4dp; the raw `coui_caustic_color_1` renders +20 / 4.6dp here, which is the same light within
 * raster tolerance.
 */
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

/** COUIMaterialBlurEffect `FRAMEWORK_TOP_BAR_PARAMS_DARK` layer 1, `#CC262626`. */
private val MaterialBarDark = Color(0xCC262626)

/** COUIMaterialBlurEffect `FRAMEWORK_TOP_BAR_PARAMS_LIGHT` layer 1, `#64FFFFFF`. */
private val MaterialBarLight = Color(0x64FFFFFF)

/**
 * The search bar capsule fill on the material path: `COUISearchBar` asks
 * `ToolbarMaterialEffectResolver` for the toolbar background and the resolver returns the
 * `COUIMaterialBlurEffect` top-bar layer, not `coui_search_bar_no_blur_solid`.
 *
 * At night `#262626` at 80% over the page (#303030) lands on 40, which is the value the settings
 * search capsule measures; the blur-off fallback (`#28767676`) reads 59 and is the reason the
 * capsule used to look brighter than the toolbar icons beside it.
 */
@Composable
private fun materialBarColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) MaterialBarDark else MaterialBarLight

/** Contains default values used by [SearchBar] and [InputField]. */
object SearchBarDefaults {
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
     * `COUIMaterialEffect.setOutlineCausticShadowColor` at full strength.
     *
     * The device only shows it on the edited search bar: the collapsed pill on the settings home
     * casts no light, and the band under the edit-state capsule measures +18 grey levels over the
     * page, fading out within 4.4dp.
     */
    @Composable
    fun causticGlowColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
        Color(0x33FFFFFF)
    } else {
        Color(0x2EFFFFFF)
    }
}

/**
 * Colors used by the [InputField] of a [SearchBar].
 *
 * @param backgroundColor the color of the capsule background.
 * @param labelColor the color of the label (hint) text.
 * @param iconColor the tint of the default leading search icon.
 * @param clearIconColor the fill color of the default clear button circle.
 */
@Immutable
data class SearchBarColors(
    val backgroundColor: Color,
    val labelColor: Color,
    val iconColor: Color,
    val clearIconColor: Color,
)

/** COUIMoveEaseInterpolator: PathInterpolator(0.3f, 0f, 0.1f, 1f). */
private val MoveEase = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/** COUISearchBar DEFAULT_SEARCH_VIEW_OFFSET/SCALE_CHANGE_DURATION (450ms). */
private val BackgroundResizeDurationMillis = 450

/** COUISearchBar DEFAULT_SEARCH_VIEW_BUTTON_OFFSET_DURATION (400ms). */
private val ActionOffsetDurationMillis = 400

/** COUISearchBar DEFAULT_BUTTON_ALPHA_CHANGE_DURATION (350ms). */
private val ActionAlphaDurationMillis = 350

/** COUI `coui_search_bar_functional_button_offset_distance` (60dp). */
private val ActionSlideDistance = 60.dp

/**
 * Extra start padding for the outside end action: the COUI 20dp visual gap from the capsule
 * (8dp button start gap + 12dp ripple text padding) minus the 16dp input field margin.
 */
private val ActionExtraStartPadding = 4.dp

/**
 * End padding for the outside end action: `coui_search_bar_functional_button_end_gap_compat`
 * (4dp) + 12dp ripple text padding.
 */
private val ActionEndPadding = 16.dp
