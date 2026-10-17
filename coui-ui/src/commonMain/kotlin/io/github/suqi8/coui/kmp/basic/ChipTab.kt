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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
 * Not ported, because no consumer in this library reaches them:
 * - the material effect path of `COUIChipTab.applyMaterialEffect` — the blur, the edge stroke and the
 *   spot-light of `COUIMaterialBlurEffect` / `COUIMaterialStrokeEffect`, all of them gated behind
 *   `COUIMaterialStateManager`'s device feature flags and theme booleans. What is drawn here is the
 *   flat dual-layer fill plus the 0.6dp checked stroke the native falls back to when
 *   `isBlurEffectEnabled()` / `isStrokeEffectEnabled()` are false;
 * - the icon-only leading chip of `COUIChipTabLayout` (the 32dp round chip made of a 20dp icon with
 *   6dp start/end padding) and that layout's 16dp scroller padding;
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
 */
@Composable
fun ChipTabGroup(
    tabs: List<ChipTabData>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnSelectedChange = rememberUpdatedState(onSelectedChange)
    val scrollState = rememberScrollState()
    val fadeWidthPx = with(LocalDensity.current) { ChipTabDefaults.FadeWidth.toPx() }
    // Placed in the Row's own coordinates, i.e. the space scrollX is measured in.
    val tabBounds = remember(tabs.size) { mutableMapOf<Int, Rect>() }

    // COUIChipTabGroup only scrolls from its chip click listener, never on the first layout.
    var firstSelection by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (firstSelection) {
            firstSelection = false
            return@LaunchedEffect
        }
        scrollState.scrollTabIntoView(tabBounds[selectedIndex] ?: return@LaunchedEffect, fadeWidthPx)
    }

    Box(
        // FadeRect of ChipMaskHorizontalScrollView: the saveLayer'd group, erased by a white-to-
        // transparent gradient at each edge it can still scroll towards.
        modifier = modifier.scrollEdgeFade(scrollState, ChipTabDefaults.FadeWidth),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(ChipTabDefaults.GroupSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                ChipTab(
                    label = tab.label,
                    selected = index == selectedIndex,
                    onClick = { currentOnSelectedChange.value(index) },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        tabBounds[index] = coordinates.boundsInParent()
                    },
                    icon = tab.icon,
                    enabled = tab.enabled,
                    showRedDot = tab.showRedDot,
                    count = tab.count,
                )
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
 *   same silhouette at `mCheckedLayerAlpha`, and `coui_color_checked_background_stroke` is stroked
 *   as a [ChipTabDefaults.CheckedStrokeWidth] outline inset by half the stroke, with the corner
 *   radius reduced by the same inset.
 *
 * `setUncheckedBackgroundColorFromTheme` — `couiColorBackgroundWithCard` composited with
 * `couiColorContainer4` — is the base `COUIChipDrawable` fill; the tab's own drawable overrides the
 * background with the dual layer above and never reads it, so it is not part of this port.
 *
 * Colours are the theme slots closest to the native attributes: the checked fill is
 * `?attr/couiColorBackgroundElevated` (`COUITheme.colorScheme.surfaceContainerHighest`), the label
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
    val checkedSurface = colorScheme.surfaceContainerHighest
    val container4 = if (isDark) ChipTabContainer4Dark else ChipTabContainer4Light
    val checkedStroke = if (isDark) ChipTabCheckedStrokeDark else ChipTabCheckedStrokeLight
    val contentColor = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariantActions
    val interactionSource = remember { MutableInteractionSource() }
    val endPadding = if (showRedDot) ChipTabDefaults.EndPaddingWithRedDot else ChipTabDefaults.EndPadding

    Box(
        modifier = modifier
            .widthIn(max = ChipTabDefaults.MaxWidth)
            .defaultMinSize(minWidth = ChipTabDefaults.MinWidth, minHeight = ChipTabDefaults.Height)
            // The native clamps chipCornerRadius to min(width, height) / 2; for the 32dp pill that is
            // exactly the 16dp capsule, and the fill is drawn by hand because its two layers carry
            // independent animated alphas, which squircleSurface's single fill cannot express.
            .squircleClip(cornerRadius = ChipTabDefaults.CornerRadius)
            .drawBehind {
                drawRect(color = checkedSurface.copy(alpha = checkedLayerAlpha))
                if (checkedLayerAlpha < 1f) {
                    drawRect(color = container4.copy(alpha = container4.alpha * (1f - checkedLayerAlpha)))
                }
                if (checkedLayerAlpha > 0f) {
                    drawRect(color = ChipTabCheckedOverlay.copy(alpha = ChipTabCheckedOverlay.alpha * checkedLayerAlpha))
                    val strokeWidth = ChipTabDefaults.CheckedStrokeWidth.toPx()
                    val inset = strokeWidth / 2f
                    drawRoundRect(
                        color = checkedStroke.copy(alpha = checkedStroke.alpha * checkedLayerAlpha),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = CornerRadius((size.minDimension / 2f - inset).coerceAtLeast(0f)),
                        style = Stroke(width = strokeWidth),
                    )
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
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = ChipTabDefaults.Height)
                .padding(start = ChipTabDefaults.StartPadding, end = endPadding),
            verticalAlignment = Alignment.CenterVertically,
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
                    color = colorScheme.onSurfaceSecondary,
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

    /** The width of the checked outline. COUI `COUIChipTabDrawable.CHECKED_STROKE_WIDTH_DP`. */
    val CheckedStrokeWidth = 0.6.dp

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
     * the theme's 14sp body tier, like [ChipDefaults.textStyle].
     */
    @Composable
    fun textStyle(): TextStyle = COUITheme.textStyles.body2

    /**
     * The count style of a tab. COUI `countTextAppearance = ?attr/couiTextBodyS`, 13sp, which is the
     * theme's 13sp footnote tier.
     */
    @Composable
    fun countTextStyle(): TextStyle = COUITheme.textStyles.footnote1
}

/**
 * Scrolls a tab into view, mirroring `COUIChipTabGroup.checkOcclusion` and
 * `scrollToChipTabWithAnimation`.
 *
 * The visible rect is the scroll viewport, inset by [fadeWidthPx] on the sides that are scrolled off
 * (`updateContentVisibleRect`); the tab is only scrolled when one of its edges falls inside that rect,
 * and by the distance that puts that edge on the visible edge.
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
        visibleLeft > bounds.left && visibleLeft < bounds.right -> bounds.left - visibleLeft
        visibleRight > bounds.left && visibleRight < bounds.right -> bounds.right - visibleRight
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

/** coui_color_checked_background_gradient_start (#40FFFFFF in both themes): the checked pill overlay. */
private val ChipTabCheckedOverlay = Color(0x40FFFFFF)

/** coui_color_checked_background_stroke in light themes (coui_color_checked_background_stroke #0A000000). */
private val ChipTabCheckedStrokeLight = Color(0x0A000000)

/** coui_color_checked_background_stroke in dark themes (coui_color_checked_background_stroke (night) #0AFFFFFF). */
private val ChipTabCheckedStrokeDark = Color(0x0AFFFFFF)
