// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.squircle.squircleBackground
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.COUIResponsiveUtils
import io.github.suqi8.coui.kmp.utils.COUIWindowSizeClass

/**
 * The ColorOS 17 floating edit toolbar (`COUIBottomFloatingEditToolbar`).
 *
 * A capsule of [BottomFloatingEditToolbarItem]s that slides in from below the screen edge and back
 * out again. It is the toolbar sibling of [BottomFloatingNavigationBar]: both are
 * `BaseBottomFloatingMenuView`s drawn as a glass capsule, but the edit toolbar gives its items a
 * different arrangement and a different height above 600dp.
 *
 * `COUIBottomFloatingEditToolbar.onMeasure` picks a padding of
 * [BottomFloatingEditToolbarDefaults.PaddingHorizontalLargeScreen] on a large screen,
 * `..._medium_screen` on a medium one and `..._small_screen` on a small one, and hands the menu view
 * `horizontalStyle = !isSmallScreen`. That flag selects the menu height, the item's arrangement and
 * the item's own height and title size, so the tier is published to the items through
 * [LocalBottomFloatingEditToolbarStyle].
 *
 * The capsule carries the glass surface of [couiGlassSurface]; its corner radius is the menu height
 * halved, the `mBackgroundBounds.height() / 2` the native `updateSmoothRoundPath` uses.
 *
 * Show and hide are the native `showMenuView` / `hideMenuView`: the toolbar is translated down by
 * `getMenuViewHiddenTranslationY()` — the container height minus the menu view's top edge, i.e. the
 * distance that puts the capsule just past the bottom edge — and back to zero, on the
 * `TOOLBAR_TRANSLATION_Y_SPRING_RESPONSE` (0.38) / `TOOLBAR_TRANSLATION_Y_SPRING_BOUNCE` (0.25)
 * spring.
 *
 * @param modifier The modifier to be applied to the toolbar.
 * @param containerColor The background color of the capsule.
 * @param backdrop The content blurred behind the capsule, defaulting to [LocalBackdrop]. `null` keeps
 *   it opaque.
 * @param visible Whether the toolbar is on screen; toggling it plays the slide motion above.
 * @param content The content of the toolbar, usually [BottomFloatingEditToolbarItem]s.
 */
@Composable
fun BottomFloatingEditToolbar(
    modifier: Modifier = Modifier,
    containerColor: Color = COUITheme.colorScheme.bar,
    backdrop: Backdrop? = LocalBackdrop.current,
    visible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val sizeClass = COUIResponsiveUtils.windowSizeClass(maxWidth)
        val horizontalStyle = sizeClass != COUIWindowSizeClass.Compat
        val horizontalPadding = COUIResponsiveUtils.pick(
            windowSizeClass = sizeClass,
            compat = BottomFloatingEditToolbarDefaults.PaddingHorizontalSmallScreen,
            medium = BottomFloatingEditToolbarDefaults.PaddingHorizontalMediumScreen,
            expanded = BottomFloatingEditToolbarDefaults.PaddingHorizontalLargeScreen,
        )
        val menuHeight = if (horizontalStyle) {
            BottomFloatingEditToolbarDefaults.MenuHeightMediumLarge
        } else {
            BottomFloatingEditToolbarDefaults.MenuHeightSmall
        }
        val innerPadding = if (horizontalStyle) {
            BottomFloatingEditToolbarDefaults.InnerPaddingHorizontal
        } else {
            BottomFloatingEditToolbarDefaults.InnerPaddingVertical
        }

        var containerHeightPx by remember { mutableIntStateOf(0) }
        val menuHeightPx = with(density) { menuHeight.toPx() }
        // COUIBottomFloatingEditToolbar.getMenuViewHiddenTranslationY(): the container height minus
        // the menu view's top edge. The toolbar keeps no vertical padding and centres the menu view,
        // so that top edge is (containerHeight - menuHeight) / 2.
        val hiddenTranslationPx = (containerHeightPx + menuHeightPx) / 2f
        val slide by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = BottomFloatingEditToolbarDefaults.TranslationYSpring,
            label = "editToolbarTranslationY",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BottomFloatingEditToolbarDefaults.MinHeightWithoutPaddingVertical)
                .padding(horizontal = horizontalPadding)
                .onSizeChanged { containerHeightPx = it.height },
            contentAlignment = Alignment.Center,
        ) {
            val cornerRadius = menuHeight / 2
            val squircleEnabled = isSquircleEnabled()
            val shape = remember(cornerRadius, squircleEnabled) {
                SquircleShape(cornerRadius = cornerRadius, squircleEnabled = squircleEnabled)
            }
            val highlight = rememberGlassHighlight(GlassContainer.Medium, backdrop)

            // COUIBottomFloatingEditToolbarMenuView.onMeasure hands the item style down from
            // `setHorizontalStyle(!isSmallScreen)`; the same flag reaches the items here through the
            // composition local instead of a view property.
            CompositionLocalProvider(
                LocalBottomFloatingEditToolbarStyle provides if (horizontalStyle) {
                    BottomFloatingEditToolbarStyle.Horizontal
                } else {
                    BottomFloatingEditToolbarStyle.Vertical
                },
            ) {
                Layout(
                    content = content,
                    modifier = Modifier
                        .height(menuHeight)
                        .graphicsLayer { translationY = (1f - slide) * hiddenTranslationPx }
                        .couiGlassSurface(
                            backdrop = backdrop,
                            shape = shape,
                            containerColor = containerColor,
                            fallback = Modifier.squircleBackground(
                                color = containerColor,
                                cornerRadius = cornerRadius,
                            ),
                            highlight = highlight,
                        )
                        .bottomFloatingTouchHighlight(shape),
                ) { measurables, constraints ->
                    // COUIBottomFloatingEditToolbarMenuView.setHorizontalStyle: a lone item takes the
                    // single-item width, several items share the narrower per-item bound.
                    val itemMaxWidth = if (measurables.size == 1) {
                        if (horizontalStyle) {
                            BottomFloatingEditToolbarDefaults.SingleItemWidthHorizontal
                        } else {
                            BottomFloatingEditToolbarDefaults.SingleItemWidthVertical
                        }
                    } else {
                        if (horizontalStyle) {
                            BottomFloatingEditToolbarDefaults.ItemMaxWidthHorizontal
                        } else {
                            BottomFloatingEditToolbarDefaults.ItemMaxWidthVertical
                        }
                    }
                    val itemHeight = if (horizontalStyle) {
                        BottomFloatingEditToolbarDefaults.ItemHeightHorizontal
                    } else {
                        BottomFloatingEditToolbarDefaults.ItemHeightVertical
                    }
                    val itemHeightPx = itemHeight.roundToPx()
                    val placeables = measurables.map { measurable ->
                        measurable.measure(
                            Constraints(
                                minWidth = 0,
                                maxWidth = itemMaxWidth.roundToPx(),
                                minHeight = itemHeightPx,
                                maxHeight = itemHeightPx,
                            ),
                        )
                    }
                    val spacingPx = BottomFloatingEditToolbarDefaults.ItemSpacing.roundToPx()
                    val innerPaddingPx = innerPadding.roundToPx()
                    val heightPx = menuHeight.roundToPx()
                    val contentWidth = placeables.sumOf { it.width } +
                        spacingPx * (placeables.size - 1).coerceAtLeast(0)
                    layout(
                        constraints.constrainWidth(contentWidth + innerPaddingPx * 2),
                        constraints.constrainHeight(heightPx),
                    ) {
                        var x = innerPaddingPx
                        placeables.forEach { placeable ->
                            placeable.placeRelative(x, (heightPx - placeable.height) / 2)
                            x += placeable.width + spacingPx
                        }
                    }
                }
            }
        }
    }
}

/**
 * The arrangement [BottomFloatingEditToolbar] gives its items, mirroring
 * `COUIBottomFloatingEditToolbarMenuView.setHorizontalStyle`.
 */
enum class BottomFloatingEditToolbarStyle {
    /**
     * Wide layout: icon and a two-line title side by side
     * (`COUIBottomFloatingToolbarItemView.STYLE_EDIT_TOOLBAR_HORIZONTAL`).
     */
    Horizontal,

    /**
     * Narrow layout: icon above a one-line title
     * (`COUIBottomFloatingToolbarItemView.STYLE_EDIT_TOOLBAR_VERTICAL`).
     */
    Vertical,
}

/**
 * The arrangement the enclosing [BottomFloatingEditToolbar] chose for its items.
 *
 * Defaults to [BottomFloatingEditToolbarStyle.Vertical], the style the native menu view starts from
 * before `setHorizontalStyle` is applied.
 */
val LocalBottomFloatingEditToolbarStyle = compositionLocalOf { BottomFloatingEditToolbarStyle.Vertical }

/**
 * An item of a [BottomFloatingEditToolbar].
 *
 * `COUIBottomFloatingToolbarItemView` lays the same content out two ways:
 * `layoutEditToolbarHorizontal` puts an icon next to a
 * [BottomFloatingEditToolbarDefaults.LabelFontSizeHorizontal] title that may run to two lines, and
 * `layoutEditToolbarVertical` stacks them with [BottomFloatingEditToolbarDefaults.IconTopMargin] above
 * the icon and [BottomFloatingEditToolbarDefaults.IconTitleGapVertical] between icon and title. The
 * enclosing toolbar decides which one applies.
 *
 * @param onClick Called when the item is activated.
 * @param icon The icon of the item.
 * @param label The title of the item.
 * @param modifier The modifier to be applied to the item.
 * @param enabled Whether the item can be activated.
 * @param redDot The red dot pinned to the icon's top end corner, or `null` for none. It is anchored
 *   by `COUIBottomFloatingToolbarItemView.layoutRedDotRelativeToIcon` in both edit styles.
 */
@Composable
fun BottomFloatingEditToolbarItem(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    redDot: BottomFloatingRedDot? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = COUITheme.colorScheme
    val style = LocalBottomFloatingEditToolbarStyle.current

    // COUIBottomFloatingToolbarItemView takes the item's colours from the same
    // couiColorLabelPrimaryVariant / couiColorLabelPrimary / couiColorLabelTertiary family the
    // navigation items use, so the tint mapping is shared with BottomFloatingNavigationBarItem.
    val tint = if (enabled) colorScheme.labelPrimaryVariant else colorScheme.onSurfaceVariantActions

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (style == BottomFloatingEditToolbarStyle.Horizontal) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    BottomFloatingEditToolbarDefaults.IconTitleGapHorizontal,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EditToolbarIcon(icon = icon, label = label, tint = tint, redDot = redDot)
                Text(
                    text = label,
                    color = tint,
                    fontSize = BottomFloatingEditToolbarDefaults.LabelFontSizeHorizontal,
                    fontWeight = FontWeight.Medium,
                    // applyEditToolbarHorizontalStyle: start-aligned, at most two lines.
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Spacer(Modifier.height(BottomFloatingEditToolbarDefaults.IconTopMargin))
            EditToolbarIcon(icon = icon, label = label, tint = tint, redDot = redDot)
            Spacer(Modifier.height(BottomFloatingEditToolbarDefaults.IconTitleGapVertical))
            Text(
                text = label,
                color = tint,
                fontSize = BottomFloatingEditToolbarDefaults.LabelFontSizeVertical,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The 24dp item icon with its optional red dot, in the toolbar's own tint. */
@Composable
private fun EditToolbarIcon(
    icon: ImageVector,
    label: String,
    tint: Color,
    redDot: BottomFloatingRedDot?,
) {
    Box {
        Image(
            modifier = Modifier.size(BottomFloatingEditToolbarDefaults.IconSize),
            imageVector = icon,
            contentDescription = label,
            colorFilter = ColorFilter.tint(tint),
        )
        if (redDot != null) {
            BottomFloatingRedDotBadge(
                redDot = redDot,
                largeIcon = false,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * Contains default values used by [BottomFloatingEditToolbar] and
 * [BottomFloatingEditToolbarItem].
 */
object BottomFloatingEditToolbarDefaults {

    /** The capsule height on a small screen. COUI `coui_bottom_floating_navigation_bar_height`. */
    val MenuHeightSmall = 56.dp

    /**
     * The capsule height on a medium or large screen.
     * COUI `coui_bottom_floating_edit_toolbar_menu_height_medium_large`.
     */
    val MenuHeightMediumLarge = 44.dp

    /** The minimum outer height. COUI `coui_bottom_floating_edit_toolbar_min_height_without_padding_vertical`. */
    val MinHeightWithoutPaddingVertical = 88.dp

    /** The outside padding on a small screen. COUI `coui_bottom_floating_edit_toolbar_padding_horizontal_small_screen`. */
    val PaddingHorizontalSmallScreen = 16.dp

    /** The outside padding on a medium screen. COUI `coui_bottom_floating_edit_toolbar_padding_horizontal_medium_screen`. */
    val PaddingHorizontalMediumScreen = 24.dp

    /** The outside padding on a large screen. COUI `coui_bottom_floating_edit_toolbar_padding_horizontal_large_screen`. */
    val PaddingHorizontalLargeScreen = 40.dp

    /**
     * The padding between the capsule's end edges and the first and last item.
     * COUI `coui_bottom_floating_edit_toolbar_inner_padding_horizontal`.
     */
    val InnerPaddingHorizontal = 10.dp

    /**
     * The padding between the capsule's end edges and the first and last item.
     * COUI `coui_bottom_floating_edit_toolbar_inner_padding_vertical`.
     */
    val InnerPaddingVertical = 12.dp

    /** The item height in the wide layout. COUI `coui_bottom_floating_edit_toolbar_item_height_horizontal`. */
    val ItemHeightHorizontal = 40.dp

    /** The item height in the narrow layout. COUI `coui_bottom_floating_navigation_bar_item_height`. */
    val ItemHeightVertical = 48.dp

    /** The per-item width bound in the wide layout. COUI `coui_bottom_floating_edit_toolbar_item_max_width_horizontal`. */
    val ItemMaxWidthHorizontal = 98.dp

    /** The per-item width bound in the narrow layout. COUI `coui_bottom_floating_edit_toolbar_item_max_width_vertical`. */
    val ItemMaxWidthVertical = 70.dp

    /** The width a lone item takes in the wide layout. COUI `coui_bottom_floating_edit_toolbar_single_item_width_horizontal`. */
    val SingleItemWidthHorizontal = 152.dp

    /** The width a lone item takes in the narrow layout. COUI `coui_bottom_floating_edit_toolbar_single_item_width_vertical`. */
    val SingleItemWidthVertical = 148.dp

    /** The spacing between items. COUI `coui_bottom_floating_navigation_bar_item_space`. */
    val ItemSpacing = 8.dp

    /** The icon size. COUI `coui_floating_toolbar_item_icon_width` / `..._icon_height`. */
    val IconSize = 24.dp

    /** The top margin of the icon in the narrow layout. COUI `coui_bottom_floating_navigation_bar_item_icon_top_margin`. */
    val IconTopMargin = 4.dp

    /** The vertical gap between icon and title in the narrow layout. COUI `coui_bottom_floating_navigation_bar_item_icon_title_gap_vertical`. */
    val IconTitleGapVertical = 2.dp

    /** The horizontal gap between icon and title in the wide layout. COUI `coui_bottom_floating_navigation_bar_item_icon_title_gap_horizontal`. */
    val IconTitleGapHorizontal = 4.dp

    /** The title size in the wide layout (`applyEditToolbarHorizontalStyle`: `dp2px(14.0f)`). */
    val LabelFontSizeHorizontal = 14.sp

    /** The title size in the narrow layout (`applyEditToolbarVerticalStyle`: `dp2px(10.0f)`). */
    val LabelFontSizeVertical = 10.sp

    /**
     * The show / hide spring: `TOOLBAR_TRANSLATION_Y_SPRING_RESPONSE` (0.38) with
     * `TOOLBAR_TRANSLATION_Y_SPRING_BOUNCE` (0.25), cut at the native
     * `TOOLBAR_TRANSLATION_Y_MIN_VISIBLE_CHANGE` of 0.001f.
     */
    val TranslationYSpring = folmeSpring(
        damping = 0.75f,
        response = 0.38f,
        visibilityThreshold = 0.001f,
    )
}
