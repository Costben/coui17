// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
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
import io.github.suqi8.coui.kmp.squircle.squircleClip
import io.github.suqi8.coui.kmp.theme.COUITheme
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
 * A checked tab is lifted the way `COUIChipTab` lifts it on ColorOS 17: a specular highlight along
 * the top and bottom edges only — the sides stay transparent, so no ring forms around the pill — the
 * [ChipTabDefaults.CheckedShadow] drop shadow and, in dark themes, the level-1 caustic light. All of
 * them fade in with the checked layer. The shadows are drawn by the group, outside the fading edge's
 * layer, so they are not clipped to the row.
 *
 * [leadingIcon] adds the icon-only chip of `COUIChipTabLayout`: a 32dp round chip of a
 * [ChipTabDefaults.IconOnlyIconSize] icon that always wears the checked style, pinned before the
 * scrolled tabs, which then start [ChipTabDefaults.GroupSpacing] after it.
 *
 * Not ported, because no consumer in this library reaches them:
 * - the blur of `COUIMaterialBlurEffect` (`TYPE_CONTENT_CHIP_TAB*`) and the spot-light: like the
 *   other controls of this library the tab keeps the blur-disabled flat fill;
 * - `COUIChipTabGroupFragmentMediator`, the fragment switch and fade-in helper;
 * - `coui_chip_tab_unit` and `coui_chip_tab_position_in_group`: the report proves no chip class
 *   references them (the app owns the pager accessibility text);
 * - the press mask of `drawStateEffects` (`COUIMaskEffectDrawable` / `COUIStrokeDrawable`), whose
 *   geometry the report could not resolve; the tabs are selectable without it.
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
                scrollState = scrollState,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                // FadeRect of ChipMaskHorizontalScrollView: the saveLayer'd group, erased by a white-to-
                // transparent gradient at each edge it can still scroll towards.
                modifier = Modifier.scrollEdgeFade(scrollState, ChipTabDefaults.FadeWidth),
            ) {
                Row(
                    // The padding sits inside the scroll, like the scroller's clipToPadding = false: the
                    // tabs scroll on under it up to the fading edge.
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(start = scrollStartPadding, end = scrollEndPadding),
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
 * - when checked, `coui_color_checked_background_gradient_start` (`#40ffffff`) is painted over the
 *   same silhouette at `mCheckedLayerAlpha` in dark mode, and top/bottom rim specular highlights
 *   are drawn along with a subtle bottom inner glow.
 *
 * `setUncheckedBackgroundColorFromTheme` — `couiColorBackgroundWithCard` composited with
 * `couiColorContainer4` — is the base `COUIChipDrawable` fill; the tab's own drawable overrides the
 * background with the dual layer above and never reads it, so it is not part of this port.
 *
 * Colours are the theme slots closest to the native attributes: the checked fill is
 * `?attr/couiColorBackgroundElevated` in dark mode and `ChipTabCheckedSurfaceLight` in light mode, the label
 * and icon of both states are `?attr/couiColorLabelPrimary` (`onSurface`), a disabled tab is
 * `?attr/couiColorLabelTertiary` (`onSurfaceVariantActions`) and the count text is
 * `?attr/couiColorLabelSecondary` (`onSurfaceSecondary`).
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
) {
    ChipTabSurface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        showRedDot = showRedDot,
        iconOnly = false,
        drawElevation = true,
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
    val checkedSurface = if (isDark) colorScheme.surfaceContainerHighest else ChipTabCheckedSurfaceLight
    val container4 = if (isDark) ChipTabContainer4Dark else ChipTabContainer4Light
    val contentColor = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariantActions
    val interactionSource = remember { MutableInteractionSource() }
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

    Box(
        modifier = modifier
            .then(sizeModifier)
            .then(
                if (drawElevation) {
                    Modifier.chipTabElevation(alpha = { checkedLayerAlpha }, cornerRadius = ChipTabDefaults.CornerRadius)
                } else {
                    Modifier
                },
            )
            // The native clamps chipCornerRadius to min(width, height) / 2; for the 32dp pill that is
            // exactly the 16dp capsule, and the fill is drawn by hand because its two layers carry
            // independent animated alphas, which squircleSurface's single fill cannot express.
            .squircleClip(cornerRadius = ChipTabDefaults.CornerRadius)
            .drawBehind {
                drawRect(color = checkedSurface.copy(alpha = checkedLayerAlpha))
                if (checkedLayerAlpha < 1f) {
                    drawRect(color = container4.copy(alpha = container4.alpha * (1f - checkedLayerAlpha)))
                }
                if (isDark && checkedLayerAlpha > 0f) {
                    drawRect(color = ChipTabCheckedOverlay.copy(alpha = ChipTabCheckedOverlay.alpha * checkedLayerAlpha))
                }
            }
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
        // Upper and lower specular highlights on the checked pill rim, plus a subtle inner glow
        // along the bottom edge, replicating the ColorOS 17 native highlights without side halos.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = checkedLayerAlpha }
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val inset = strokeWidth / 2f
                    val strokeBrush = Brush.verticalGradient(
                        0.0f to (if (isDark) Color.White.copy(alpha = 0.23f) else Color.White),
                        0.30f to Color.Transparent,
                        0.70f to Color.Transparent,
                        1.0f to (if (isDark) Color.White.copy(alpha = 0.14f) else Color.White),
                        startY = 0f,
                        endY = size.height,
                    )
                    drawRoundRect(
                        brush = strokeBrush,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = CornerRadius((size.minDimension / 2f - inset).coerceAtLeast(0f)),
                        style = Stroke(width = strokeWidth),
                    )

                    val glowAlpha = if (isDark) 0.06f else 0.85f
                    val glowHeight = 2.dp.toPx()
                    val glowBrush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = glowAlpha)),
                        startY = size.height - glowHeight,
                        endY = size.height,
                    )
                    drawRect(
                        brush = glowBrush,
                        topLeft = Offset(0f, size.height - glowHeight),
                        size = Size(size.width, glowHeight),
                    )
                },
        )
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
    val shape = RoundedCornerShape(cornerRadius)
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

/** couiColorContainer4 in dark themes (coui_color_container4 (night) #1AFFFFFF). */
private val ChipTabContainer4Dark = Color(0x1AFFFFFF)

/**
 * Checked pill surface in light themes (#F6F6F7), matching COUI's composite TYPE_CONTENT_CHIP_TAB_SELECTED_NO_BLUR
 * outcome so the white rim remains visually distinct against the pill body.
 */
private val ChipTabCheckedSurfaceLight = Color(0xFFF6F6F7)

/** coui_color_checked_background_gradient_start (#40FFFFFF in both themes): the checked pill overlay in dark theme. */
private val ChipTabCheckedOverlay = Color(0x40FFFFFF)

/** coui_caustic_color_3 in dark themes (#33FFFFFF). */
private val ChipTabCausticDark = Color(0x33FFFFFF)
