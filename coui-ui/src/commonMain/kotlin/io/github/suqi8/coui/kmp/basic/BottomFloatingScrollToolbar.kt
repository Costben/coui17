// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.utils.overScrollHorizontal
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * The scrolling main menu of the ColorOS 17 Photos editor (`picture3d_editor_preview_toolbar`).
 *
 * A pill of [BottomFloatingScrollToolbarItem]s, each an icon over a short label, that scrolls
 * horizontally once they stop fitting. It is the labelled sibling of [BottomFloatingToolbar]: the pill
 * follows the finger through [couiTouchMotion] like the native `MainMenuPillContainerLayout`, with
 * deformation, rubber band and press scale, and without taking the drag away from the list.
 *
 * Items that no longer fit fade out over [BottomFloatingScrollToolbarDefaults.FadingEdgeWidth] on the
 * side that has more to scroll to, as `EditorLinearListView.dispatchDraw` does with a `DST_IN` layer.
 *
 * Activating an item scrolls its neighbour on the nearer edge into view, so the next tool in that
 * direction is always reachable with one tap, like `EditorLinearListView`'s item touch listener.
 *
 * The pill wraps its items and stays centred, up to
 * [BottomFloatingScrollToolbarDefaults.MaxWidth] and the
 * [BottomFloatingScrollToolbarDefaults.MarginHorizontal] on either side.
 *
 * @param modifier The modifier to be applied to the toolbar.
 * @param colors The colours of the pill and its items. The Photos editor is always dark, so the
 *   defaults don't follow the theme.
 * @param scrollState The scroll position of the items.
 * @param content The content of the toolbar, usually [BottomFloatingScrollToolbarItem]s.
 */
@Composable
fun BottomFloatingScrollToolbar(
    modifier: Modifier = Modifier,
    colors: BottomFloatingScrollToolbarColors = BottomFloatingScrollToolbarDefaults.colors(),
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable () -> Unit,
) {
    val cornerRadius = BottomFloatingScrollToolbarDefaults.CornerRadius
    val squircleEnabled = isSquircleEnabled()
    val shape = remember(cornerRadius, squircleEnabled) {
        CapsuleOutlineShape(cornerRadius = cornerRadius, squircleEnabled = squircleEnabled)
    }
    val radiusPx = with(LocalDensity.current) { cornerRadius.toPx() }
    val stroke = remember { MaterialStrokeParams.frameworkCapsule13(isDark = true) }
    val layoutDirection = LocalLayoutDirection.current
    val paddingPx = with(LocalDensity.current) {
        BottomFloatingScrollToolbarDefaults.ContentPadding.roundToPx()
    }
    val scope = rememberCoroutineScope()
    var viewportPx by remember { mutableIntStateOf(0) }
    val revealNeighbour = remember(scrollState, scope, paddingPx, layoutDirection) {
        { itemStart: Int, itemWidth: Int ->
            val rtl = layoutDirection == LayoutDirection.Rtl
            val effectiveScroll = if (rtl) scrollState.maxValue - scrollState.value else scrollState.value
            val effectiveTarget = scrollToolbarRevealTarget(
                itemStart = itemStart + paddingPx,
                itemWidth = itemWidth,
                scroll = effectiveScroll,
                viewport = viewportPx,
                padding = paddingPx,
            )
            val target = if (rtl) {
                (scrollState.maxValue - effectiveTarget).coerceIn(0, scrollState.maxValue)
            } else {
                effectiveTarget.coerceIn(0, scrollState.maxValue)
            }
            if (target != scrollState.value) {
                scope.launch { scrollState.animateScrollTo(target, BottomFloatingScrollToolbarDefaults.RevealSpec) }
            }
        }
    }
    val pillFill = Modifier
        .background(colors.containerColor, shape)
        .border(BottomFloatingScrollToolbarDefaults.StrokeWidth, colors.strokeColor, shape)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BottomFloatingScrollToolbarDefaults.MarginHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(BottomFloatingScrollToolbarDefaults.Height)
                .widthIn(max = BottomFloatingScrollToolbarDefaults.MaxWidth)
                .couiTouchMotion(
                    shape = shape,
                    // MainMenuPillContainerLayout: TYPE_CAPSULE_1 with a white light.
                    lightParams = SpotLightDefaults.capsuleParams1(isDark = true),
                    consumeDrag = false,
                    // photo_editor_main_menu_pill_bg under the rim of the pill's stroke layer.
                    surface = pillFill.materialShadowEdgeBySize(
                        stroke = stroke,
                        cornerWeightFor = { width, height ->
                            capsuleRimCornerWeight(width, height, radiusPx)
                        },
                        cornerRadius = cornerRadius,
                        fallback = pillFill,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalBottomFloatingScrollToolbarColors provides colors,
                LocalBottomFloatingScrollToolbarReveal provides revealNeighbour,
            ) {
                Row(
                    modifier = Modifier
                        .onSizeChanged { viewportPx = it.width }
                        .clip(shape)
                        .scrollEdgeFade(scrollState, BottomFloatingScrollToolbarDefaults.FadingEdgeWidth)
                        .overScrollHorizontal()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = BottomFloatingScrollToolbarDefaults.ContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(BottomFloatingScrollToolbarDefaults.ItemSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * An item of a [BottomFloatingScrollToolbar]: `picture3d_editor_main_menu_item_layout`, a
 * [BottomFloatingScrollToolbarDefaults.IconSize] glyph over a one-line label, both tinted by the
 * selection. A selected item has no background, only
 * [BottomFloatingScrollToolbarColors.selectedContentColor].
 *
 * @param onClick Called when the item is activated.
 * @param icon The icon of the item.
 * @param label The label of the item.
 * @param modifier The modifier to be applied to the item.
 * @param selected Whether this is the active tool.
 * @param enabled Whether the item can be activated.
 */
@Composable
fun BottomFloatingScrollToolbarItem(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = LocalBottomFloatingScrollToolbarColors.current
        ?: BottomFloatingScrollToolbarDefaults.colors()
    val tint = colors.contentColor(selected = selected, enabled = enabled)
    val reveal = LocalBottomFloatingScrollToolbarReveal.current
    var itemStart by remember { mutableIntStateOf(0) }
    var itemWidth by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .size(
                width = BottomFloatingScrollToolbarDefaults.ItemWidth,
                height = BottomFloatingScrollToolbarDefaults.Height,
            )
            .onPlaced {
                itemStart = it.positionInParent().x.toInt()
                itemWidth = it.size.width
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    onClick()
                    reveal?.invoke(itemStart, itemWidth)
                },
            )
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(BottomFloatingScrollToolbarDefaults.IconSize),
            )
            Spacer(Modifier.height(BottomFloatingScrollToolbarDefaults.IconLabelGap))
            Text(
                text = label,
                modifier = Modifier.width(BottomFloatingScrollToolbarDefaults.LabelWidth),
                color = tint,
                style = BottomFloatingScrollToolbarDefaults.labelStyle(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The colours of a [BottomFloatingScrollToolbar].
 *
 * @param containerColor The fill of the pill.
 * @param strokeColor The hairline around the pill, under its material rim.
 * @param contentColor The icon and label of an idle item.
 * @param selectedContentColor The icon and label of the selected item.
 * @param disabledContentColor The icon and label of a disabled item.
 */
@Immutable
data class BottomFloatingScrollToolbarColors(
    val containerColor: Color,
    val strokeColor: Color,
    val contentColor: Color,
    val selectedContentColor: Color,
    val disabledContentColor: Color,
) {
    @Stable
    internal fun contentColor(selected: Boolean, enabled: Boolean): Color = when {
        selected -> selectedContentColor
        !enabled -> disabledContentColor
        else -> contentColor
    }
}

/** The colours the enclosing [BottomFloatingScrollToolbar] hands its items. */
internal val LocalBottomFloatingScrollToolbarColors = compositionLocalOf<BottomFloatingScrollToolbarColors?> { null }

/** How an item reports its start and width to the enclosing toolbar when it is activated. */
internal val LocalBottomFloatingScrollToolbarReveal = compositionLocalOf<((Int, Int) -> Unit)?> { null }

/**
 * The scroll position that shows the neighbour of a tapped item, in pixels.
 *
 * The neighbour is the item on the side of the nearer viewport edge, and the scroll is the least that
 * brings it inside the content padding, what a `LinearSmoothScroller` does for a target position.
 * Scroll positions past the ends are left for [ScrollState] to clamp.
 *
 * @param itemStart The tapped item's start within the scrolling content.
 * @param itemWidth The tapped item's width. Items are assumed to be equally wide.
 * @param scroll The current scroll position.
 * @param viewport The width of the visible part of the list.
 * @param padding The content padding at either end of the list.
 */
internal fun scrollToolbarRevealTarget(
    itemStart: Int,
    itemWidth: Int,
    scroll: Int,
    viewport: Int,
    padding: Int,
): Int {
    val centre = itemStart + itemWidth / 2 - scroll
    val nearStart = centre < viewport / 2
    val neighbourStart = if (nearStart) itemStart - itemWidth else itemStart + itemWidth
    val visibleStart = scroll + padding
    val visibleEnd = scroll + viewport - padding
    return when {
        neighbourStart < visibleStart -> (neighbourStart - padding).coerceAtLeast(0)
        neighbourStart + itemWidth > visibleEnd -> neighbourStart + itemWidth + padding - viewport
        else -> scroll
    }
}

/** Contains default values used by [BottomFloatingScrollToolbar] and its items. */
object BottomFloatingScrollToolbarDefaults {

    /** The height of the pill and of its items. `photoeditorpage_main_menu_portrait_height`. */
    val Height = 56.dp

    /** The corner radius of the pill. `photo_editor_main_menu_pill_corner_radius`. */
    val CornerRadius = 28.dp

    /** The widest the pill grows. `photo_editor_main_menu_pill_max_size`. */
    val MaxWidth = 460.dp

    /** The margin kept to the toolbar's sides. `photo_editor_main_menu_pill_margin_horizontal`. */
    val MarginHorizontal = 16.dp

    /** The space between the pill's ends and the first and last items, measured from the native list. */
    val ContentPadding = 16.dp

    /** The width of the fade over the items on a scrollable side. `base_editor_fading_edge_width`. */
    val FadingEdgeWidth = 50.dp

    /** The width of an item. `photoeditorpage_main_menu_item_width`. */
    val ItemWidth = 51.dp

    /** The space between two items, none in the native list. */
    val ItemSpacing = 0.dp

    /** The size of an item's icon. `base_editor_main_menu_item_icon_height_width`. */
    val IconSize = 24.dp

    /** The gap between the icon and the label, the label's `dp_1` top margin. */
    val IconLabelGap = 1.dp

    /** The width of the label. `photoeditorpage_main_menu_item_text_width`. */
    val LabelWidth = 49.dp

    /** The size of the label, which the native bar fixes in dp so it doesn't follow the font scale. */
    val LabelSize = 8.dp

    /** The line height of the label, `dp_14`. */
    val LabelLineHeight = 14.dp

    /** The fill of the pill. `photo_editor_main_menu_pill_fill_color`. */
    val ContainerColor = Color(0xFF232323)

    /** The hairline around the pill. `photo_editor_main_menu_pill_stroke_color`. */
    val StrokeColor = Color(0x0DFFFFFF)

    /** The width of the hairline around the pill. `base_editor_menu_item_stroke_width`. */
    val StrokeWidth = 0.67.dp

    /** An idle item. `picture3d_preview_menu_no_select_normal_color`. */
    val ContentColor = Color(0x73FFFFFF)

    /** The selected item. `base_editor_theme_color`. */
    val SelectedContentColor = Color(0xFFFFB200)

    /** A disabled item. `picture3d_preview_menu_no_select_gray_color`. */
    val DisabledContentColor = Color(0x33FFFFFF)

    /** The shadow keeping a label legible, `base_editor_menu_item_text_shadow_color` at radius 12. */
    val LabelShadow = Shadow(color = Color(0x4C000000), blurRadius = 12f)

    /**
     * The scroll revealing a neighbour. `LinearSmoothScroller` stretches the list's 100ms scroll to
     * 298ms for a target it can see, on a `DecelerateInterpolator` of factor 1.5.
     */
    val RevealSpec = tween<Float>(durationMillis = 298, easing = Easing { 1f - (1f - it).pow(3) })

    /** Creates the colours of the toolbar. */
    @Composable
    fun colors(
        containerColor: Color = ContainerColor,
        strokeColor: Color = StrokeColor,
        contentColor: Color = ContentColor,
        selectedContentColor: Color = SelectedContentColor,
        disabledContentColor: Color = DisabledContentColor,
    ): BottomFloatingScrollToolbarColors = remember(
        containerColor,
        strokeColor,
        contentColor,
        selectedContentColor,
        disabledContentColor,
    ) {
        BottomFloatingScrollToolbarColors(
            containerColor = containerColor,
            strokeColor = strokeColor,
            contentColor = contentColor,
            selectedContentColor = selectedContentColor,
            disabledContentColor = disabledContentColor,
        )
    }

    /** The label's text style, medium weight on the fixed size and line height above. */
    @Composable
    fun labelStyle(): TextStyle = with(LocalDensity.current) {
        TextStyle(
            fontSize = LabelSize.toSp(),
            lineHeight = LabelLineHeight.toSp(),
            fontWeight = FontWeight.Medium,
            shadow = LabelShadow,
        )
    }
}
