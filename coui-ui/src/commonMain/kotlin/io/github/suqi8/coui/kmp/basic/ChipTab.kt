// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The ColorOS 17 chip tab bar: a horizontally scrollable, single-line row of [ChipTab] pills.
 *
 * One tab is selected at a time, the tabs are separated by
 * [ChipTabDefaults.GroupSpacing], and the edges that are scrolled off fade out over
 * [ChipTabDefaults.FadeWidth] like `ChipMaskHorizontalScrollView` (`FADING_EDGE_WIDTH` = 12dp), whose
 * `PorterDuff.Mode.DST_OUT` mask is drawn here as a `BlendMode.DstOut` gradient that is only applied
 * to the edges that are actually clipped, exactly like its `dispatchDraw`.
 *
 * The group takes an explicit tab list rather than a `ChipTabGroupScope` content lambda: the group
 * owns the tab indices, and this codebase already models a tab bar as a list
 * (`TabRow(tabs, selectedTabIndex, onTabSelected)`), which keeps the single selection and the
 * scroll-into-view from needing a declaration-order index counter.
 *
 * Selecting a tab scrolls it into view the way `COUIChipTabGroup.checkOcclusion` +
 * `scrollToChipTabWithAnimation` do: the tab is only scrolled when one of its edges falls inside the
 * visible rect — which is inset by the fade width on the sides that are scrolled off — and the target
 * position is animated on the `SCROLL_ANIMATION_RESPONSE` (0.4) / `SCROLL_ANIMATION_BOUNCE` (0.2)
 * spring. As in the native chip click listener, the tab is not scrolled on the first composition.
 *
 * A checked tab is lifted the way `COUIChipTab` lifts it on ColorOS 17: a white rim that strokes the
 * whole silhouette, brightest across the top and bottom edges and gone by the time it reaches the
 * straight sides, an inner shade down the left edge and a faint lift down the right one in light
 * themes, the [ChipTabDefaults.CheckedShadow] drop shadow and, in dark themes, the level-1 caustic
 * light. All of them fade in with the checked layer. The shadows are drawn by the group, outside the
 * fading edge's layer, so they are not clipped to the row.
 *
 * [leadingIcon] adds the icon-only chip of `COUIChipTabLayout`: a 32dp round chip of a
 * [ChipTabDefaults.IconOnlyIconSize] icon that always wears the checked style, pinned before the
 * scrolled tabs, which then start [ChipTabDefaults.GroupSpacing] after it.
 *
 * Pressing a tab mirrors `COUIChip` / `COUIChipTab`: `COUIPressFeedbackHelper` shrinks the pill
 * toward a size- and aspect-dependent ratio with a bouncy spring release (switching to the candy
 * spring when `aspect > 2`, and synchronizing the selected shadow in [ChipTabElevationLayer]),
 * while `COUISpotLightEffect` (`TYPE_CHIP_TAB`) casts a touch-tracking spotlight over the fill when
 * runtime shaders are available, falling back to `COUIMaskEffectDrawable` (`couiColorPress`)
 * otherwise.
 *
 * Not ported, because no consumer in this library reaches them:
 * - the blur of `COUIMaterialBlurEffect` (`TYPE_CONTENT_CHIP_TAB*`): like the other controls of
 *   this library the tab keeps the blur-disabled flat fill;
 * - `COUIChipTabGroupFragmentMediator`, the fragment switch and fade-in helper;
 * - `coui_chip_tab_unit` and `coui_chip_tab_position_in_group`: the report proves no chip class
 *   references them (the app owns the pager accessibility text);
 * - `COUIStrokeDrawable` inside `drawStateEffects`, whose geometry the report could not resolve.
 *
 * @param tabs The tabs of the group, in declaration order.
 * @param selectedIndex The index of the selected tab.
 * @param onSelectedChange Called with the index of the tab the user selected.
 * @param modifier The modifier to be applied to the group.
 * @param leadingIcon The icon of the icon-only chip before the tabs, or `null` for none.
 * @param onLeadingIconClick Called when the icon-only chip is clicked.
 * @param contentPadding The padding around the tabs. Its horizontal part lies inside the scroll,
 *   like the 16dp `paddingHorizontal` of the native scroller with `clipToPadding = false`, so the
 *   tabs scroll on under it up to the fading edge; with [leadingIcon] its start is the chip's margin.
 */
@Composable
fun ChipTabGroup(
    tabs: List<ChipTabData>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    backdrop: Backdrop? = null,
) {
    val currentOnSelectedChange = rememberUpdatedState(onSelectedChange)
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val fadeWidthPx = with(density) { ChipTabDefaults.FadeWidth.toPx() }
    // COUIChipTabLayout.setIconOnlyChipVisible: the scroller's padding drops from 16dp to 8dp next to
    // the icon-only chip, which itself keeps the 16dp start margin.
    val scrollStartPadding = if (leadingIcon != null) ChipTabDefaults.GroupSpacing else contentPadding.calculateStartPadding(layoutDirection)
    val scrollEndPadding = contentPadding.calculateEndPadding(layoutDirection)
    // The tabs report their bounds in the Row inside that padding; scrollX counts from the left edge
    // of the padded content.
    val contentLeftPaddingPx = with(density) {
        (if (layoutDirection == LayoutDirection.Rtl) scrollEndPadding else scrollStartPadding).toPx()
    }
    // Placed in the scrolled content's own coordinates, i.e. the space scrollX is measured in.
    val tabBounds = remember(tabs.size) { mutableStateMapOf<Int, Rect>() }
    val tabPressScales = remember(tabs.size) { Array(tabs.size) { mutableFloatStateOf(1f) } }

    // COUIChipTabGroup only scrolls from its chip click listener, never on the first layout.
    var firstSelection by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (firstSelection) {
            firstSelection = false
            return@LaunchedEffect
        }
        scrollState.scrollTabIntoView(tabBounds[selectedIndex] ?: return@LaunchedEffect, fadeWidthPx)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            ChipTabSurface(
                selected = true,
                onClick = onLeadingIconClick,
                modifier = Modifier.padding(start = contentPadding.calculateStartPadding(layoutDirection)),
                enabled = true,
                showRedDot = false,
                iconOnly = true,
                drawElevation = true,
                backdrop = backdrop,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = COUITheme.colorScheme.onSurface,
                    modifier = Modifier.size(ChipTabDefaults.IconOnlyIconSize),
                )
            }
        }
        Box(modifier = Modifier.weight(1f, fill = false)) {
            ChipTabElevationLayer(
                tabCount = tabs.size,
                selectedIndex = selectedIndex,
                tabBounds = tabBounds,
                tabPressScales = tabPressScales,
                scrollState = scrollState,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                // FadeRect of ChipMaskHorizontalScrollView: the saveLayer'd group, erased by a white-to-
                // transparent gradient at each edge it can still scroll towards. Both this offscreen
                // layer and the scroller below clip to their own bounds, which are the chip's height;
                // they are therefore laid out [ChipTabDefaults.PressOutset] taller on each side and
                // placed back up, while the reported size keeps the row at the chip's height, so a tab
                // can overshoot on release without its edges being cut flat. The native lifts the
                // scroller's clip by 100dp instead (`COUIChipTab.updateClipExpandForShadowOnParents`).
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val outsetPx = ChipTabDefaults.PressOutset.roundToPx()
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, (placeable.height - 2 * outsetPx).coerceAtLeast(0)) {
                            placeable.place(0, -outsetPx)
                        }
                    }
                    .scrollEdgeFade(scrollState, ChipTabDefaults.FadeWidth),
            ) {
                Row(
                    // The padding sits inside the scroll, like the scroller's clipToPadding = false: the
                    // tabs scroll on under it up to the fading edge.
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(
                            start = scrollStartPadding,
                            end = scrollEndPadding,
                            top = ChipTabDefaults.PressOutset,
                            bottom = ChipTabDefaults.PressOutset,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(ChipTabDefaults.GroupSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        ChipTabSurface(
                            selected = index == selectedIndex,
                            onClick = { currentOnSelectedChange.value(index) },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                tabBounds[index] = coordinates.boundsInParent().translate(contentLeftPaddingPx, 0f)
                            },
                            enabled = tab.enabled,
                            showRedDot = tab.showRedDot,
                            iconOnly = false,
                            drawElevation = false,
                            pressScaleHolder = tabPressScales[index],
                            backdrop = backdrop,
                        ) { contentColor ->
                            ChipTabLabel(
                                label = tab.label,
                                contentColor = contentColor,
                                icon = tab.icon,
                                showRedDot = tab.showRedDot,
                                count = tab.count,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The shadows and caustic lights of a group's tabs.
 *
 * They are drawn here rather than by each tab because the tabs live inside the fading edge's
 * offscreen layer, which clips everything to the row; the native scroller lifts that clip instead
 * (`COUIChipTab.updateClipExpandForShadowOnParents`, 100dp). This layer is outset by
 * [ChipTabDefaults.ElevationOutset] on every side, follows the scroll offset, and erases what is
 * scrolled under a fading edge the same way, so a tab and its shadow fade out together.
 */
@Composable
private fun ChipTabElevationLayer(
    tabCount: Int,
    selectedIndex: Int,
    tabBounds: Map<Int, Rect>,
    tabPressScales: Array<MutableFloatState>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val outset = ChipTabDefaults.ElevationOutset
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val outsetPx = outset.roundToPx()
                val placeable = measurable.measure(
                    Constraints.fixed(constraints.maxWidth + 2 * outsetPx, constraints.maxHeight + 2 * outsetPx),
                )
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-outsetPx, -outsetPx) }
            }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val outsetPx = outset.toPx()
                val viewportWidth = size.width - 2 * outsetPx
                val fadePx = ChipTabDefaults.FadeWidth.toPx().coerceAtMost(viewportWidth / 2f)
                if (scrollState.maxValue <= 0 || fadePx <= 0f) return@drawWithContent
                val rtl = layoutDirection == LayoutDirection.Rtl
                val fadeLeft = if (rtl) scrollState.canScrollForward else scrollState.canScrollBackward
                val fadeRight = if (rtl) scrollState.canScrollBackward else scrollState.canScrollForward
                if (fadeLeft) {
                    drawRect(Color.White, size = Size(outsetPx, size.height), blendMode = BlendMode.DstOut)
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent), startX = outsetPx, endX = outsetPx + fadePx),
                        topLeft = Offset(outsetPx, 0f),
                        size = Size(fadePx, size.height),
                        blendMode = BlendMode.DstOut,
                    )
                }
                if (fadeRight) {
                    val edge = size.width - outsetPx
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.White), startX = edge - fadePx, endX = edge),
                        topLeft = Offset(edge - fadePx, 0f),
                        size = Size(fadePx, size.height),
                        blendMode = BlendMode.DstOut,
                    )
                    drawRect(Color.White, topLeft = Offset(edge, 0f), size = Size(outsetPx, size.height), blendMode = BlendMode.DstOut)
                }
            },
    ) {
        val density = LocalDensity.current
        for (index in 0 until tabCount) {
            key(index) {
                val elevationAlpha by animateFloatAsState(
                    targetValue = if (index == selectedIndex) 1f else 0f,
                    animationSpec = ChipTabDefaults.LayerSpring,
                    visibilityThreshold = ChipTabDefaults.MinVisibleChange,
                    label = "chipTabElevationAlpha",
                )
                val bounds = tabBounds[index]
                if (bounds != null && elevationAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .offset {
                                val outsetPx = outset.roundToPx()
                                // The scrolled content is laid out from the start edge.
                                val x = if (layoutDirection == LayoutDirection.Rtl) {
                                    bounds.left + scrollState.value - scrollState.maxValue
                                } else {
                                    bounds.left - scrollState.value
                                }
                                IntOffset(x.roundToInt() + outsetPx, bounds.top.roundToInt() + outsetPx)
                            }
                            .size(with(density) { DpSize(bounds.width.toDp(), bounds.height.toDp()) })
                            .graphicsLayer {
                                val scale = tabPressScales.getOrNull(index)?.floatValue ?: 1f
                                scaleX = scale
                                scaleY = scale
                            }
                            .chipTabElevation(alpha = { elevationAlpha }, cornerRadius = ChipTabDefaults.CornerRadius),
                    )
                }
            }
        }
    }
}

/**
 * A tab of a [ChipTabGroup].
 *
 * The data counterpart of [ChipTab]: the group binds a tab's selected state and click to its own
 * single-selection index, so this only carries what the native `COUIChipTab` gets from its style and
 * from the app.
 *
 * @param label The label of the tab.
 * @param icon The optional leading icon of the tab.
 * @param enabled Whether the tab can be selected.
 * @param showRedDot Whether the tab carries the red dot.
 * @param count The optional count shown after the label.
 */
@Immutable
data class ChipTabData(
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val showRedDot: Boolean = false,
    val count: Int? = null,
)

/**
 * A pill tab of a [ChipTabGroup], mirroring ColorOS's `COUIChipTab` (`Widget.COUI.Chip.ChipTab`).
 *
 * The pill is [ChipTabDefaults.Height] tall, clamped between [ChipTabDefaults.MinWidth] and
 * [ChipTabDefaults.MaxWidth], with the `chipCornerRadius` capsule the native clamps to
 * `min(width, height) / 2` — drawn on the smooth-corner silhouette of `SquircleShape`, like [Chip].
 *
 * Its fill is the dual-layer model of `COUIChipTabDrawable.drawDualLayerBackground` /
 * `drawLightOSCheckedBackground`:
 * - the base layer is `ArgbEvaluator.evaluate(mUncheckedLayerAlpha, mCheckedSurface,
 *   mUncheckedSurface)`, i.e. the checked surface fading out as the tab is unselected. The port
 *   keeps the ColorOS 17 / API 37 branch of `updateCachedThemeColors`, where `mUncheckedSurface` is
 *   `ColorUtils.setAlphaComponent(mCheckedSurface, 0)`: an unchecked tab keeps only its alpha ramp,
 *   so the pill is transparent and shows the surface behind it. The `SDK_INT <= 36` branch, which
 *   fills it with the opaque `coui_chip_tab_surface_unchecked_surface` (`#ffffffff` /
 *   `#ff000000`), is deliberately not ported;
 * - `couiColorContainer4` is then painted over it at `mUncheckedLayerAlpha`;
 * - when checked, the pill gets the glass rim of `TYPE_CONTENT_CHIP_TAB_SELECTED`: a white stroke
 *   that follows the silhouette, so the highlight stays 3px thick around the rounded ends instead of
 *   being cut off where a horizontal band would leave the shape, plus a ramp to white above the
 *   bottom edge and — in light themes only — a soft shade down the left edge and a faint lift down
 *   the right one. The geometry and strengths are the on-device measurement of the notes app
 *   (`ChipTabRimStopsLight` and friends). They are drawn as gradients rather than as a backdrop blur
 *   because a tab sits inside the scroller's offscreen fading-edge layer, and sampling the page
 *   backdrop from there re-enters the node tree.
 *
 * `setUncheckedBackgroundColorFromTheme` — `couiColorBackgroundWithCard` composited with
 * `couiColorContainer4` — is the base `COUIChipDrawable` fill; the tab's own drawable overrides the
 * background with the dual layer above and never reads it, so it is not part of this port.
 *
 * Colours are the measured values of the notes app: the checked fill is `#4B4B4B` in dark mode and
 * `#F5F6F7` in light mode, the label and icon of both states are `?attr/couiColorLabelPrimary`
 * (`onSurface`), a disabled tab is `?attr/couiColorLabelTertiary` (`onSurfaceVariantActions`) and
 * the count text is `?attr/couiColorLabelSecondary` (`onSurfaceSecondary`).
 *
 * @param label The label of the tab: [ChipTabDefaults.textStyle] (`couiTextButtonM`, 14sp), single
 *   line, ellipsized beyond [ChipTabDefaults.TextMaxWidth].
 * @param selected Whether the tab is selected.
 * @param onClick The callback when the tab is clicked.
 * @param modifier The modifier to be applied to the tab.
 * @param icon The optional leading icon, laid out in a [ChipTabDefaults.IconSize] box followed by
 *   [ChipTabDefaults.IconEndPadding] and tinted like the label (`checkedChipIconTint` /
 *   `uncheckedChipIconTint`, both `?attr/couiColorLabelPrimary`).
 * @param enabled Whether the tab is enabled. A disabled tab keeps its fill and switches the label and
 *   icon to `?attr/couiColorLabelTertiary`.
 * @param showRedDot Whether the tab carries the red dot: [ChipTabDefaults.RedDotDiameter] wide,
 *   [ChipTabDefaults.RedDotOffsetHorizontal] after the label / count / icon and
 *   [ChipTabDefaults.RedDotOffsetVertical] below the top of the pill, filled with
 *   `coui_color_additional_red` and ringed in white by [Badge]. While it is shown the end padding
 *   drops from [ChipTabDefaults.EndPadding] to [ChipTabDefaults.EndPaddingWithRedDot]
 *   (`COUIChipTabDrawable.setShowRedDot`).
 * @param count The optional count drawn after the label: [ChipTabDefaults.countTextStyle]
 *   (`couiTextBodyS`, 13sp) in `?attr/couiColorLabelSecondary`, with
 *   [ChipTabDefaults.CountStartMargin] of margin before it.
 * @param backdrop The page backdrop the pill frosts, or `null` for the flat fill. Only pass a
 *   backdrop the row is not itself recorded into.
 */
@Composable
fun ChipTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    showRedDot: Boolean = false,
    count: Int? = null,
    backdrop: Backdrop? = null,
) {
    ChipTabSurface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        showRedDot = showRedDot,
        iconOnly = false,
        drawElevation = true,
        backdrop = backdrop,
    ) { contentColor ->
        ChipTabLabel(
            label = label,
            contentColor = contentColor,
            icon = icon,
            showRedDot = showRedDot,
            count = count,
        )
    }
}

/**
 * The pill of a [ChipTab]: the dual-layer fill, the checked rim and, when [drawElevation] is set,
 * the checked shadow and caustic light.
 *
 * @param iconOnly Whether this is the 32dp round icon-only chip of `COUIChipTabLayout`
 *   (`COUIChipTab.setShowIconOnly`), sized by [ChipTabDefaults.Height] on both axes.
 * @param drawElevation Whether the pill draws its own shadow; a [ChipTabGroup] draws its tabs'
 *   shadows outside its fading edge instead.
 * @param content The content of the pill, given the label / icon colour.
 */
@Composable
private fun ChipTabSurface(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    showRedDot: Boolean,
    iconOnly: Boolean,
    drawElevation: Boolean,
    modifier: Modifier = Modifier,
    pressScaleHolder: MutableFloatState? = null,
    backdrop: Backdrop? = null,
    content: @Composable RowScope.(contentColor: Color) -> Unit,
) {
    // COUIChipTabDrawable.onStateChange: the checked layer animates between 0 and 1 on COUISpringForce
    // response 0.2 / bounce 0, cut at ALPHA_MIN_VISIBLE_CHANGE (1 / 256); the unchecked layer is its
    // complement, so one animation drives both.
    val checkedLayerAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = ChipTabDefaults.LayerSpring,
        visibilityThreshold = ChipTabDefaults.MinVisibleChange,
        label = "chipTabCheckedLayerAlpha",
    )
    val colorScheme = COUITheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val surfaceStops = if (isDark) ChipTabSurfaceStopsDark else ChipTabSurfaceStopsLight
    val container4 = if (isDark) ChipTabContainer4Dark else ChipTabContainer4Light
    val contentColor = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariantActions
    val interactionSource = remember { MutableInteractionSource() }
    val isInteractionPressed by interactionSource.collectIsPressedAsState()
    var isPointerPressed by remember { mutableStateOf(false) }
    val isPressed = enabled && (isPointerPressed || isInteractionPressed)

    val density = LocalDensity.current
    var widthPx by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(0f) }
    val widthDp = with(density) { widthPx.toDp().value }
    val heightDp = with(density) { heightPx.toDp().value }
    val isCandy = widthDp > 0f && heightDp > 0f &&
        max(widthDp / heightDp, heightDp / widthDp) > ChipTabPressMotionSpec.pressAspectActivationThreshold
    val pressTargetScale = couiPressScale(widthDp, heightDp, ChipTabPressMotionSpec)
    val currentPressTargetScale by rememberUpdatedState(pressTargetScale)

    val pressScaleState = pressScaleHolder ?: remember { mutableFloatStateOf(1f) }
    val pressProgress = remember { Animatable(0f) }
    val pressMaskProgress = remember { Animatable(0f) }

    val spotLightSupported = isRuntimeShaderSupported()
    val spotLightParams = SpotLightDefaults.chipTabParams(isDark)
    val lightX = remember { Animatable(0.5f) }
    val lightY = remember { Animatable(0.5f) }
    val lightProgress = remember { Animatable(0f) }
    var extinguishing by remember { mutableStateOf(false) }

    LaunchedEffect(pressScaleState) {
        pressScaleState.floatValue = 1f - (1f - currentPressTargetScale) * pressProgress.value
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            isPointerPressed = false
            extinguishing = true
            pressProgress.snapTo(0f)
            pressScaleState.floatValue = 1f
            pressMaskProgress.snapTo(0f)
            lightProgress.snapTo(0f)
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            val pressSpec = if (isCandy) {
                ChipTabPressMotionSpec.candyPressSpring
            } else {
                ChipTabPressMotionSpec.pressSpring
            }
            pressProgress.animateTo(1f, pressSpec) {
                pressScaleState.floatValue = 1f - (1f - currentPressTargetScale) * value
            }
        } else if (pressProgress.value != 0f || pressScaleState.floatValue != 1f) {
            val releaseSpec = if (isCandy) {
                ChipTabPressMotionSpec.candyReleaseSpring
            } else {
                ChipTabPressMotionSpec.releaseSpring
            }
            pressProgress.animateTo(0f, releaseSpec) {
                pressScaleState.floatValue = 1f - (1f - currentPressTargetScale) * value
            }
            pressScaleState.floatValue = 1f
        }
    }

    LaunchedEffect(isPressed, spotLightSupported) {
        if (spotLightSupported) {
            if (pressMaskProgress.value != 0f) pressMaskProgress.snapTo(0f)
            return@LaunchedEffect
        }
        if (isPressed) {
            pressMaskProgress.animateTo(1f, ChipTabPressMaskSpring)
        } else if (pressMaskProgress.value > 0f) {
            if (pressMaskProgress.value < CHIP_TAB_PRESSED_MASK_MIN_VISIBLE_PROGRESS) {
                try {
                    pressMaskProgress.animateTo(1f, ChipTabPressMaskSpring) {
                        if (value >= CHIP_TAB_PRESSED_MASK_MIN_VISIBLE_PROGRESS) throw ChipTabPressMaskThresholdReached()
                    }
                } catch (_: ChipTabPressMaskThresholdReached) {
                    // Minimum visible progress reached; fall through to the fade-out.
                }
            }
            pressMaskProgress.animateTo(0f, ChipTabPressMaskSpring)
        }
    }

    val lightRadius = with(density) {
        couiSpotlightRadius(
            start = spotLightParams.startRadius,
            end = spotLightParams.endRadius,
            extinguishExtra = SpotLightDefaults.ExtinguishExtraRadius,
            progress = lightProgress.value,
            extinguishing = extinguishing,
        ).toDp()
    }
    val lightHeight = with(density) { spotLightParams.lightHeight.toDp() }

    val sizeModifier = if (iconOnly) {
        // COUIChipTab.onMeasure forces the icon-only chip to a 32dp square.
        Modifier.size(ChipTabDefaults.Height)
    } else {
        Modifier
            .widthIn(max = ChipTabDefaults.MaxWidth)
            .defaultMinSize(minWidth = ChipTabDefaults.MinWidth, minHeight = ChipTabDefaults.Height)
    }
    val contentPadding = when {
        iconOnly -> PaddingValues(horizontal = ChipTabDefaults.IconOnlyPadding)
        showRedDot -> PaddingValues(start = ChipTabDefaults.StartPadding, end = ChipTabDefaults.EndPaddingWithRedDot)
        else -> PaddingValues(start = ChipTabDefaults.StartPadding, end = ChipTabDefaults.EndPadding)
    }

    val squircleEnabled = isSquircleEnabled()
    val pillShape = remember(squircleEnabled) {
        CapsuleOutlineShape(cornerRadius = ChipTabDefaults.CornerRadius, squircleEnabled = squircleEnabled)
    }

    // With a page backdrop the pill frosts the content behind it, the way the notes app's chips do:
    // a blurred copy of the backdrop plus the two blend colours, and the checked layer only adds a
    // wash instead of the opaque surface, so a checked pill still reads as glass.
    val blurRadius = remember(density) { with(density) { GlassChipDefaults.BlurRadiusPx.toDp() } }
    val blurColors = remember(isDark, checkedLayerAlpha, backdrop) {
        if (backdrop != null) GlassChipDefaults.blurColors(isDark, checkedLayerAlpha) else null
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .onSizeChanged {
                widthPx = it.width.toFloat()
                heightPx = it.height.toFloat()
            }
            .pointerInput(enabled, spotLightSupported) {
                if (!enabled) return@pointerInput
                val touchSlop = viewConfiguration.touchSlop
                coroutineScope {
                    var moveJob: Job? = null
                    var progressJob: Job? = null
                    fun moveTo(position: Offset, snap: Boolean) {
                        if (!spotLightSupported) return
                        val x = position.x / widthPx.coerceAtLeast(1f)
                        val y = position.y / heightPx.coerceAtLeast(1f)
                        moveJob?.cancel()
                        if (snap) {
                            moveJob = launch(start = CoroutineStart.UNDISPATCHED) {
                                lightX.snapTo(x)
                                lightY.snapTo(y)
                            }
                        } else {
                            moveJob = launch {
                                coroutineScope {
                                    launch { lightX.animateTo(x, SpotLightDefaults.MoveSpring) }
                                    launch { lightY.animateTo(y, SpotLightDefaults.MoveSpring) }
                                }
                            }
                        }
                    }
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val pointerId = down.id
                            isPointerPressed = true
                            extinguishing = false
                            moveTo(down.position, snap = true)
                            if (spotLightSupported) {
                                progressJob?.cancel()
                                progressJob = launch { lightProgress.animateTo(1f, SpotLightDefaults.GlowSpring) }
                            }
                            var cancelledByScroll = false
                            try {
                                while (true) {
                                    val initialEvent = awaitPointerEvent(PointerEventPass.Initial)
                                    val initialChange = initialEvent.changes.firstOrNull { it.id == pointerId } ?: break
                                    moveTo(initialChange.position, snap = false)
                                    if (initialChange.changedToUpIgnoreConsumed()) break
                                    val outOfBounds =
                                        initialChange.position.x < -touchSlop ||
                                            initialChange.position.y < -touchSlop ||
                                            initialChange.position.x > size.width + touchSlop ||
                                            initialChange.position.y > size.height + touchSlop
                                    if (outOfBounds) break
                                    val finalEvent = awaitPointerEvent(PointerEventPass.Final)
                                    val finalChange = finalEvent.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (finalChange.changedToUpIgnoreConsumed()) break
                                    if (finalChange.isConsumed && finalChange.position != finalChange.previousPosition) {
                                        cancelledByScroll = true
                                        break
                                    }
                                }
                            } finally {
                                isPointerPressed = false
                                if (spotLightSupported) {
                                    extinguishing = true
                                    progressJob?.cancel()
                                    progressJob = launch { lightProgress.animateTo(0f, SpotLightDefaults.ExtinguishSpring) }
                                }
                            }
                            if (cancelledByScroll) {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                        }
                    }
                }
            }
            .graphicsLayer {
                val scale = pressScaleState.floatValue
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (drawElevation) {
                    Modifier.chipTabElevation(alpha = { checkedLayerAlpha }, cornerRadius = ChipTabDefaults.CornerRadius)
                } else {
                    Modifier
                },
            )
            .then(
                if (blurColors != null) {
                    Modifier.textureBlur(
                        backdrop = backdrop!!,
                        shape = pillShape,
                        blurRadius = blurRadius.value,
                        colors = blurColors,
                    )
                } else {
                    Modifier
                },
            )
            // The native clamps chipCornerRadius to min(width, height) / 2; for the 32dp pill that is
            // the 16dp continuous-curvature system capsule (`CapsuleOutlineShape`), and the fill is
            // drawn by hand along that outline because its layers carry independent animated alphas.
            .drawWithCache {
                val outline = pillShape.createOutline(size, layoutDirection, this@drawWithCache)
                val silhouette = Path().apply { addOutline(outline) }
                val surfaceBrush = Brush.verticalGradient(colorStops = surfaceStops, startY = 0f, endY = size.height)
                onDrawWithContent {
                    if (blurColors == null) {
                        if (checkedLayerAlpha > 0f) {
                            drawPath(path = silhouette, brush = surfaceBrush, alpha = checkedLayerAlpha)
                        }
                        if (checkedLayerAlpha < 1f) {
                            drawOutline(outline, color = container4.copy(alpha = container4.alpha * (1f - checkedLayerAlpha)))
                        }
                    } else if (checkedLayerAlpha > 0f) {
                        // The blurred pill already carries the fill; the opaque checked surface would
                        // cover the blur, so the checked layer only lays its lift on top.
                        clipPath(silhouette) {
                            drawRect(color = GlassChipDefaults.highlightWash(isDark, checkedLayerAlpha))
                        }
                    }
                    // The checked pill's glass rim: the light and shadow the material lays on top of
                    // the fill, inside the silhouette and under the label.
                    if (checkedLayerAlpha > 0f) {
                        clipPath(silhouette) {
                            drawChipTabCheckedRim(silhouette = silhouette, isDark = isDark, alpha = checkedLayerAlpha)
                        }
                    }
                    if (!spotLightSupported && pressMaskProgress.value > 0f) {
                        val pressMask = if (isDark) ChipTabPressedMaskDark else ChipTabPressedMaskLight
                        drawOutline(outline, color = pressMask.copy(alpha = pressMask.alpha * pressMaskProgress.value))
                    }
                    clipPath(silhouette) { this@onDrawWithContent.drawContent() }
                }
            }
            .then(
                if (spotLightSupported) {
                    Modifier.spotLight(
                        position = Offset(lightX.value, lightY.value),
                        radius = lightRadius,
                        intensity = spotLightParams.endIntensity * lightProgress.value,
                        lightHeight = lightHeight,
                        ambientStrength = spotLightParams.ambientStrength,
                        diffuseStrength = spotLightParams.diffuseStrength,
                        noise = spotLightParams.noise,
                        dither = spotLightParams.ditherN,
                        blendMode = spotLightParams.blendMode,
                        enabled = enabled && lightProgress.value > 0f,
                    )
                } else {
                    Modifier
                },
            )
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = ChipTabDefaults.Height)
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(contentColor)
        }
    }
}

/** The label row of a [ChipTab]: the optional icon, the label, the optional count and the red dot. */
@Composable
private fun RowScope.ChipTabLabel(
    label: String,
    contentColor: Color,
    icon: ImageVector?,
    showRedDot: Boolean,
    count: Int?,
) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(ChipTabDefaults.IconSize),
        )
        Spacer(modifier = Modifier.width(ChipTabDefaults.IconEndPadding))
    }
    Text(
        text = label,
        color = contentColor,
        style = ChipTabDefaults.textStyle(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = ChipTabDefaults.TextMaxWidth),
    )
    if (count != null) {
        Text(
            text = count.toString(),
            color = COUITheme.colorScheme.onSurfaceSecondary,
            style = ChipTabDefaults.countTextStyle(),
            maxLines = 1,
            modifier = Modifier.padding(start = ChipTabDefaults.CountStartMargin),
        )
    }
    if (showRedDot) {
        // calculateRedDotBounds anchors the dot to the right of the count, else the text, and
        // 6dp below the top of the chip; the pill is 32dp tall, so the row that carries it is
        // pinned to the top of the pill rather than centred.
        Badge(
            count = 0,
            stroke = true,
            dotDiameter = ChipTabDefaults.RedDotDiameter,
            modifier = Modifier
                .align(Alignment.Top)
                .padding(start = ChipTabDefaults.RedDotOffsetHorizontal)
                .offset(y = ChipTabDefaults.RedDotOffsetVertical),
        )
    }
}

/**
 * The checked elevation of a chip tab: the drop shadow `COUIChipTab.applyShadow` lifts the checked
 * tab with. In dark theme, also casts `coui_caustic_color_3` caustic light (`ChipTabCausticDark`).
 * In light theme, the caustic light is omitted to prevent over-saturation against light page backgrounds.
 *
 * Both fade with the checked layer, as `applyShadow(mCheckedLayerAlpha)` scales them.
 */
@Composable
private fun Modifier.chipTabElevation(alpha: () -> Float, cornerRadius: Dp): Modifier {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val squircleEnabled = isSquircleEnabled()
    val shape = remember(cornerRadius, squircleEnabled) {
        CapsuleOutlineShape(cornerRadius = cornerRadius, squircleEnabled = squircleEnabled)
    }
    val withDropShadow = this.dropShadow(shape) {
        val shadow = ChipTabDefaults.CheckedShadow
        radius = shadow.radius.toPx()
        color = shadow.color
        offset = Offset(shadow.offset.x.toPx(), shadow.offset.y.toPx())
        this.alpha = shadow.alpha * alpha()
    }
    return if (isDark) {
        withDropShadow.causticShadowLight(
            color = ChipTabCausticDark,
            profile = CausticShadowProfile.ChipTab,
            cornerRadius = cornerRadius,
            alpha = alpha,
        )
    } else {
        withDropShadow
    }
}

/** Contains default values used by [ChipTabGroup] and [ChipTab]. */
object ChipTabDefaults {

    /** The height of a tab. COUI `coui_chip_tab_style_height` (`android:minHeight`). */
    val Height = 32.dp

    /** The minimum width of a tab. COUI `coui_chip_default_min_width` (`android:minWidth`). */
    val MinWidth = 52.dp

    /** The maximum width of a tab. COUI `coui_chip_default_max_width` (`android:maxWidth`). */
    val MaxWidth = 300.dp

    /** The maximum width of the label. COUI `coui_chip_default_max_text_width` (`chipTextMaxWidth`). */
    val TextMaxWidth = 200.dp

    /**
     * The corner radius of a tab. `chipCornerRadius` of `Widget.COUI.Chip` is `16777215dp`, which
     * `COUIChipTabDrawable.drawDualLayerBackground` clamps to `min(width, height) / 2` — the 16dp
     * capsule of the 32dp pill.
     */
    val CornerRadius = 16.dp

    /** The size of the leading icon. COUI `coui_chip_tab_style_chip_icon_size` (`chipIconSize`). */
    val IconSize = 16.dp

    /** The gap between the icon and the label. COUI `coui_chip_suggest_style_chip_icon_end_padding`. */
    val IconEndPadding = 4.dp

    /** The padding before the first content of a tab. COUI `coui_chip_suggest_style_chip_horizontal_padding`. */
    val StartPadding = 12.dp

    /** The padding after the last content of a tab. COUI `coui_chip_suggest_style_chip_horizontal_padding`. */
    val EndPadding = 12.dp

    /** The end padding while the red dot is shown. COUI `coui_chip_tab_end_padding_with_red_dot`. */
    val EndPaddingWithRedDot = 8.dp

    /** The margin before the count text. COUI `coui_chip_tab_count_start_margin` (`chipCountStartMargin`). */
    val CountStartMargin = 2.dp

    /** The diameter of the red dot. COUI `coui_dot_diameter_small` (the small `COUIHintRedDot`). */
    val RedDotDiameter = 6.dp

    /** The offset of the red dot from the content it follows. COUI `coui_chip_red_dot_offset_horizontal`. */
    val RedDotOffsetHorizontal = 2.dp

    /** The offset of the red dot from the top of the tab. COUI `coui_chip_red_dot_offset_vertical`. */
    val RedDotOffsetVertical = 6.dp

    /** The spacing between two tabs. COUI `coui_chip_group_default_spacing_horizontal` (`chipSpacingHorizontal`). */
    val GroupSpacing = 8.dp

    /**
     * The width of the fading edge. COUI `ChipMaskHorizontalScrollView.FADING_EDGE_WIDTH`, which the
     * scroll view also reports as its `getHorizontalFadingEdgeLength()`.
     */
    val FadeWidth = 12.dp

    /** The icon size of the icon-only chip. COUI `COUIChipTabLayout.ICON_SIZE_20`. */
    val IconOnlyIconSize = 20.dp

    /** The start / end padding of the icon-only chip. COUI `COUIChipTabLayout.PADDING_HORIZONTAL_6`. */
    val IconOnlyPadding = 6.dp

    /**
     * The drop shadow under a checked tab, which `COUIChipTab.applyShadow` hands to `ShadowUtils`:
     * level 6 (`SHADOW_CAUSTIC_UPPER`, 16dp elevation, `argb(30, 0, 0, 0)`, light at -160dp) where
     * the caustic shadow is enabled, as on ColorOS 17.
     *
     * The values are the rendered profile, not a transfer of those light-source parameters, for the
     * reason [PopupShadow] gives. Fitting `alpha(s) = A · Φ((d − s) / σ)` to the checked tab of the
     * ColorOS 17 Notes app (light theme, 3.875 px/dp, both edges, `s ≥ 4dp` below so the caustic
     * light does not bias the fit) gives `A = 0.0865`, `σ = 13.1dp`, `d = 6.2dp` (rms 0.0014);
     * `σ = 0.57735 · r + 0.5` puts the radius at 21.82dp.
     */
    val CheckedShadow: Shadow = Shadow(
        radius = 21.82.dp,
        color = Color.Black,
        alpha = 0.0865f,
        offset = DpOffset(0.dp, 6.2.dp),
    )

    /**
     * How far [ChipTabGroup] lets the shadows of its tabs reach past the row: three standard
     * deviations of [CheckedShadow] beyond its offset.
     */
    val ElevationOutset = 46.dp

    /**
     * How far [ChipTabGroup] lets its scroller and its fading-edge layer reach past a tab on each
     * side, so the overshoot of [ChipTabPressMotionSpec.releaseSpring] is not clipped flat.
     *
     * A tab scales by `1 - (1 - scale) · progress`; on release `progress` springs below zero, so a
     * tab that was pressed to [ChipTabPressMotionSpec.pressScaleMin] grows to about 1.04 before
     * settling. On the 32dp pill that is under a pixel per side, and 2dp leaves room for the rim.
     */
    val PressOutset = 2.dp

    /**
     * The smallest layer-alpha change that is worth animating, so the settle tail of the spring is
     * cut. COUI `COUIChipTabDrawable.ALPHA_MIN_VISIBLE_CHANGE` (1 / 256).
     */
    val MinVisibleChange = 0.00390625f

    /**
     * The checked / unchecked layer spring: `COUIChipTabDrawable.CheckedLayerAlphaAnimation` and
     * `UncheckedLayerAlphaAnimation` are `COUISpringForce`s at response 0.2 and bounce 0, i.e. a
     * critically damped spring with `stiffness` = (2 * PI / 0.2)^2 ~= 986.96.
     */
    val LayerSpring: SpringSpec<Float> = folmeSpring(
        damping = 1f,
        response = 0.2f,
        visibilityThreshold = MinVisibleChange,
    )

    /**
     * The scroll-into-view spring: `COUIChipTabGroup.SCROLL_ANIMATION_RESPONSE` (0.4) with
     * `SCROLL_ANIMATION_BOUNCE` (0.2), i.e. `dampingRatio` = 1 - bounce, as in the other floating
     * bars of this library.
     */
    val ScrollSpring: SpringSpec<Float> = folmeSpring(damping = 0.8f, response = 0.4f)

    /**
     * The label style of a tab. COUI `Widget.COUI.Chip.ChipTab` sets
     * `android:textAppearance = ?attr/couiTextButtonM`, 14sp in `sans-serif-medium`; the port keeps
     * the theme's 14sp body tier with the system's medium weight.
     */
    @Composable
    fun textStyle(): TextStyle = COUITheme.textStyles.body2.copy(fontWeight = FontWeight.Medium)

    /**
     * The count style of a tab. COUI `countTextAppearance = ?attr/couiTextBodyS`, 13sp, which is the
     * theme's 13sp footnote tier.
     */
    @Composable
    fun countTextStyle(): TextStyle = COUITheme.textStyles.footnote1
}

/**
 * Scrolls the chip at [bounds] into the unoccluded region `[fadeWidthPx, viewportSize - fadeWidthPx]`
 * whenever it lies partially or completely outside that window — matching both `COUIChipTabGroup.checkOcclusion`
 * (partially occluded tap) and `NoteListFragment.scrollSelectedChipIntoView` (programmatic selection from an
 * external category menu).
 *
 * @param bounds The tab's bounds in the scrolled content's own coordinates.
 * @param fadeWidthPx The width of the fading edge, in pixels.
 */
private suspend fun ScrollState.scrollTabIntoView(bounds: Rect, fadeWidthPx: Float) {
    val scrollRange = maxValue
    if (scrollRange <= 0) return
    val scrollX = value
    val visibleLeft = if (scrollX > 0) scrollX + fadeWidthPx else scrollX.toFloat()
    val visibleRight = if (scrollX < scrollRange) {
        scrollX + viewportSize - fadeWidthPx
    } else {
        (scrollX + viewportSize).toFloat()
    }
    val delta = when {
        bounds.left < visibleLeft -> bounds.left - visibleLeft
        bounds.right > visibleRight -> bounds.right - visibleRight
        else -> return
    }
    animateScrollTo(
        value = (scrollX + delta).roundToInt().coerceIn(0, scrollRange),
        animationSpec = ChipTabDefaults.ScrollSpring,
    )
}

/** couiColorContainer4 in light themes (coui_color_container4 #0A000000): the unchecked pill overlay. */
private val ChipTabContainer4Light = Color(0x0A000000)

/**
 * The unchecked pill in dark themes at the alpha the notes app actually lands on: 8.2% white over
 * the `#303030` page is `#414141`, the measured value.
 */
private val ChipTabContainer4Dark = Color(0x15FFFFFF)

/** The checked pill in light themes: `#F5F6F7`, the measured body of the notes app's checked chip. */
private val ChipTabCheckedSurfaceLight = Color(0xFFF5F6F7)

/** The checked pill in dark themes: `#4B4B4B`, the measured body of the notes app's checked chip. */
private val ChipTabCheckedSurfaceDark = Color(0xFF4B4B4B)

/**
 * The checked pill's body, top to bottom: the glass sits a level brighter under the top rim (`#F6F7F8`)
 * and two levels brighter where the bottom glow meets it (`#F8F9F9`), the measured values on the
 * notes app.
 */
private val ChipTabSurfaceStopsLight: Array<Pair<Float, Color>> = arrayOf(
    0.00f to Color(0xFFF7F8F9),
    0.05f to Color(0xFFF6F7F8),
    0.20f to ChipTabCheckedSurfaceLight,
    0.80f to ChipTabCheckedSurfaceLight,
    0.88f to Color(0xFFF6F7F8),
    0.96f to Color(0xFFF8F9F9),
    1.00f to Color(0xFFF8F9F9),
)

/** The same lift in dark themes, one level each way around `#4B4B4B`. */
private val ChipTabSurfaceStopsDark: Array<Pair<Float, Color>> = arrayOf(
    0.00f to Color(0xFF4C4C4C),
    0.20f to ChipTabCheckedSurfaceDark,
    0.85f to ChipTabCheckedSurfaceDark,
    1.00f to Color(0xFF4D4D4D),
)

/** coui_caustic_color_3 in dark themes (#33FFFFFF). */
private val ChipTabCausticDark = Color(0x33FFFFFF)

/**
 * The checked pill's glass rim, measured on the notes app at 620dpi (3.875 px/dp).
 *
 * The native recipe is the `TYPE_CONTENT_CHIP_TAB_SELECTED` glass bloom over the
 * `TYPE_FRAMEWORK_CAPSULE_2` stroke. Both are drawn here instead of compositing a backdrop: a
 * `COUIChipTab` lives inside the scroller's offscreen fading-edge layer, and sampling the page
 * backdrop from inside that layer re-enters the node tree on the render thread.
 *
 * The bloom hugs the pill's outline rather than sitting in a horizontal band: the white is 3px thick
 * along the top edge and 4px along the bottom, and at the rounded ends it follows the arc down,
 * fading out over ~35px from the top and ~46px from the bottom before it reaches the straight sides
 * (light; the dark rim is 2px thick and fades over ~33px / ~69px). [ChipTabRimStopsLight] and
 * [ChipTabRimStopsDark] are those fades, sampled off the outline every 2px.
 */
private val ChipTabRimWidthLight = 0.8.dp
private val ChipTabRimWidthDark = 0.55.dp
private val ChipTabRimBleedWidth = 0.52.dp
private val ChipTabRimBleed = 0.15f
private val ChipTabRimBleedDark = 0.05f

/** White alpha along the outline, from the top edge (0f) to the bottom edge (1f). */
private val ChipTabRimStopsLight: Array<Pair<Float, Float>> = arrayOf(
    0.000f to 1.00f,
    0.048f to 1.00f,
    0.097f to 0.95f,
    0.145f to 0.78f,
    0.194f to 0.55f,
    0.242f to 0.32f,
    0.290f to 0.12f,
    0.355f to 0.02f,
    0.420f to 0.00f,
    0.560f to 0.00f,
    0.620f to 0.05f,
    0.680f to 0.15f,
    0.740f to 0.40f,
    0.800f to 0.66f,
    0.870f to 0.90f,
    0.930f to 1.00f,
    1.000f to 1.00f,
)

/** White alpha along the outline in dark themes, over the `#4B4B4B` fill. */
private val ChipTabRimStopsDark: Array<Pair<Float, Float>> = arrayOf(
    0.000f to 0.270f,
    0.016f to 0.270f,
    0.032f to 0.160f,
    0.048f to 0.125f,
    0.065f to 0.100f,
    0.081f to 0.078f,
    0.097f to 0.055f,
    0.113f to 0.050f,
    0.161f to 0.028f,
    0.226f to 0.011f,
    0.274f to 0.000f,
    0.440f to 0.000f,
    0.540f to 0.006f,
    0.605f to 0.011f,
    0.669f to 0.022f,
    0.734f to 0.033f,
    0.798f to 0.050f,
    0.831f to 0.061f,
    0.895f to 0.083f,
    0.927f to 0.111f,
    0.976f to 0.167f,
    1.000f to 0.206f,
)

private val ChipTabBottomRampLight = 1.03.dp
private val ChipTabBottomSolidLight = 1.03.dp
private val ChipTabBottomRampDark = 0.78.dp
private val ChipTabBottomAlphaDark = 0.15f
private val ChipTabBottomAlphaLight = 0.72f
private val ChipTabSideShadeWidth = 15.5.dp
private val ChipTabSideShadeAlpha = 0.016f
private val ChipTabSideLightAlpha = 0.15f

/** Paints the checked pill's rim; the caller clips it to the pill's silhouette. */
private fun DrawScope.drawChipTabCheckedRim(silhouette: Path, isDark: Boolean, alpha: Float) {
    if (alpha <= 0f) return
    val strength = alpha.coerceIn(0f, 1f)
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    if (!isDark) {
        val side = ChipTabSideShadeWidth.toPx().coerceAtMost(width / 2f)
        if (side > 0f) {
            // The notes app's light comes from the upper right: the straight edges shade to 242 on
            // the left and brighten to 247 on the right, over a #F5F6F7 fill.
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black.copy(alpha = ChipTabSideShadeAlpha * strength), Color.Transparent),
                    startX = 0f,
                    endX = side,
                ),
                size = Size(side, height),
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = ChipTabSideLightAlpha * strength)),
                    startX = width - side,
                    endX = width,
                ),
                topLeft = Offset(width - side, 0f),
                size = Size(side, height),
            )
        }
    }

    // The bloom wraps the outline: a stroke keeps the highlight the same thickness around the
    // rounded ends instead of cutting it off where a horizontal band would leave the silhouette.
    // Half of the stroke falls outside and is clipped away by the caller.
    val rim = (if (isDark) ChipTabRimWidthDark else ChipTabRimWidthLight).toPx().coerceAtMost(height / 4f)
    if (rim > 0f) {
        val stops = if (isDark) ChipTabRimStopsDark else ChipTabRimStopsLight

        // Two passes over the same pixels must still land on the measured alpha, so the wider
        // bleed pass takes `b` of the target and the stroke takes whatever the bleed leaves:
        // b + m * (1 - b) = a, i.e. m = a * (1 - b) / (1 - a * b).
        fun rimBrush(scale: (Float) -> Float) = Brush.verticalGradient(
            colorStops = Array(stops.size) { index ->
                stops[index].first to Color.White.copy(alpha = scale(stops[index].second) * strength)
            },
            startY = 0f,
            endY = height,
        )
        // The bloom seeps into the surface rather than stopping dead at the stroke: a wider second
        // pass at a fifth of the strength carries it ~2px further in, which is what keeps the
        // rounded ends from reading as a ring drawn on top.
        val bleed = if (isDark) ChipTabRimBleedDark else ChipTabRimBleed
        drawPath(
            path = silhouette,
            brush = rimBrush { a -> a * bleed },
            style = Stroke(width = rim * 2f + ChipTabRimBleedWidth.toPx() * 2f),
        )
        drawPath(path = silhouette, brush = rimBrush { a -> a * (1f - bleed) / (1f - a * bleed) }, style = Stroke(width = rim * 2f))
    }

    // The glow under the bottom edge sits *above* the rim, so the two never paint the same pixels
    // twice: dark relies on the rim's own bottom stop, light adds the extra 4px of solid white.
    val solid = (if (isDark) 0.dp else ChipTabBottomSolidLight).toPx()
    val base = max(solid, rim)
    val ramp = (if (isDark) ChipTabBottomRampDark else ChipTabBottomRampLight).toPx()
        .coerceAtMost((height - base).coerceAtLeast(0f))
    val rampAlpha = (if (isDark) ChipTabBottomAlphaDark else ChipTabBottomAlphaLight) * strength
    if (ramp > 0f) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = rampAlpha)),
                startY = height - base - ramp,
                endY = height - base,
            ),
            topLeft = Offset(0f, height - base - ramp),
            size = Size(width, ramp),
        )
    }
    if (solid > 0f) {
        drawRect(
            color = Color.White.copy(alpha = strength),
            topLeft = Offset(0f, height - solid),
            size = Size(width, solid),
        )
    }
}

/** COUIPressFeedbackHelper / PressParamsExtKt.pressParamsDefault tuned for COUIChipTab (no drag/rubber). */
private val ChipTabPressMotionSpec = COUITouchMotionSpec(
    pressScaleMin = 0.85f,
    pressScaleMax = 0.97f,
    pressSpring = folmeSpring(damping = 1f, response = 0.25f, visibilityThreshold = 0.001f),
    releaseSpring = folmeSpring(damping = 0.4f, response = 0.5f, visibilityThreshold = 0.001f),
    candyPressSpring = folmeSpring(damping = 1f, response = 0.3f, visibilityThreshold = 0.001f),
    candyReleaseSpring = folmeSpring(damping = 0.5f, response = 0.5f, visibilityThreshold = 0.001f),
)

/** COUIMaskEffectDrawable press tint spring (response 0.3f, bounce 0f). */
private val ChipTabPressMaskSpring = folmeSpring<Float>(damping = 1f, response = 0.3f)

/** couiColorPress in light themes (#1F000000), fallback when runtime shaders are unavailable. */
private val ChipTabPressedMaskLight = Color(0x1F000000)

/** couiColorPress in dark themes (#33FFFFFF), fallback when runtime shaders are unavailable. */
private val ChipTabPressedMaskDark = Color(0x33FFFFFF)

/** COUIMaskEffectDrawable DEFAULT_MIN_PROGRESS_FOR_TOUCH_ENTER_ANIMATION. */
private const val CHIP_TAB_PRESSED_MASK_MIN_VISIBLE_PROGRESS = 0.7f

private class ChipTabPressMaskThresholdReached : CancellationException("Press mask reached the minimum visible progress")
