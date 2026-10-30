// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.BlurColors
import io.github.suqi8.coui.kmp.blur.BlurDefaults
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.squircle.squircleBackground
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.COUIResponsiveUtils

/**
 * The ColorOS 17 floating text-selection toolbar (`COUIBottomFloatingToolbar`).
 *
 * One or more capsules of [BottomFloatingToolbarItem]s that float over the content, sliding up from
 * below the screen edge and back out again. It is the text-selection and note sibling of
 * [BottomFloatingNavigationBar] and [BottomFloatingScrollToolbar]: the same
 * `BaseBottomFloatingMenuView` slide motion, but the items are grouped — a selection toolbar shows
 * *cut / copy / paste* in one capsule and *select all / translate* in another — and each capsule
 * carries the ColorOS 17 material edge of [Modifier.materialShadowEdge] rather than the glass
 * highlight the navigation bar uses.
 *
 * Groups are placed like the native `COUIBottomFloatingToolbar.onLayout`: a single group is centred,
 * two groups take the start and end edges, and three or more keep the outermost groups on the edges
 * with the rest spread between them. The edge inset is `coui_floating_toolbar_group_margin_*`,
 * picked by the width tier like the native `COUIBottomFloatingToolbar.onMeasure`.
 *
 * Show and hide are the native `show` / `hide`: the toolbar is translated down by the distance that
 * puts the capsule just past the container's bottom edge and back to zero, on the
 * `MENU_VIEW_TRANSLATION_Y_SPRING_RESPONSE` (0.38) / `..._BOUNCE` (0.75) spring.
 *
 * Not ported, because no consumer in this library reaches them:
 * - the framework-only `OplusMaterialEdgeParams` path (`TYPE_FRAMEWORK_CAPSULE_5`), used when the
 *   runtime shader is unavailable; the opaque [containerColor] stands in for it;
 * - the group insert / remove animation (`COUIBottomFloatingToolbarAction`) and the XML menu
 *   inflation (`inflateMenu`); content is composed directly.
 *
 * @param modifier The modifier to be applied to the toolbar.
 * @param containerColor The background color of each capsule when the blur is unavailable, and the
 *   tint laid over the blurred backdrop.
 * @param backdrop The content blurred behind the capsules, defaulting to [LocalBackdrop]. `null`
 *   keeps them opaque.
 * @param visible Whether the toolbar is on screen; toggling it plays the slide motion above.
 * @param content The content of the toolbar, one or more [BottomFloatingToolbarGroup]s.
 * @see BottomFloatingScrollToolbar for the labelled, scrolling bar of the Photos editor.
 */
@Composable
fun BottomFloatingToolbar(
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
        val groupMargin = COUIResponsiveUtils.pick(
            windowSizeClass = COUIResponsiveUtils.windowSizeClass(maxWidth),
            compat = BottomFloatingToolbarDefaults.GroupMarginSmallScreen,
            medium = BottomFloatingToolbarDefaults.GroupMarginMediumScreen,
            expanded = BottomFloatingToolbarDefaults.GroupMarginLargeScreen,
        )

        var containerHeightPx by remember { mutableIntStateOf(0) }
        val menuHeightPx = with(density) { BottomFloatingToolbarDefaults.MenuViewHeight.toPx() }
        // BaseBottomFloatingMenuView.getMenuViewHiddenTranslationY(): the container height minus the
        // menu view's top edge. The toolbar keeps no vertical padding and centres the menu view, so
        // that top edge is (containerHeight - menuHeight) / 2.
        val hiddenTranslationPx = (containerHeightPx + menuHeightPx) / 2f
        val slide by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = BottomFloatingToolbarDefaults.TranslationYSpring,
            label = "bottomFloatingToolbarTranslationY",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BottomFloatingToolbarDefaults.MinHeight)
                .onSizeChanged { containerHeightPx = it.height },
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalBackdrop provides backdrop,
                LocalBottomFloatingToolbarContainerColor provides containerColor,
            ) {
                Layout(
                    content = content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationY = (1f - slide) * hiddenTranslationPx },
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(Constraints()) }
                    val heightPx = BottomFloatingToolbarDefaults.MenuViewHeight.roundToPx()
                    val width = constraints.maxWidth
                    val marginPx = groupMargin.roundToPx()
                    val minSpacePx = BottomFloatingToolbarDefaults.MinGroupSpace.roundToPx()
                    layout(width, constraints.constrainHeight(heightPx)) {
                        placeRelativeGroups(
                            placeables = placeables,
                            width = width,
                            marginPx = marginPx,
                            minSpacePx = minSpacePx,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Places the measured groups across [width], mirroring `COUIBottomFloatingToolbar.onLayout`.
 *
 * A lone group is centred; two groups take the start and end edges; three or more keep the first
 * and last on the edges and spread the rest evenly between them, never closer than [minSpacePx] to a
 * neighbour. Groups whose width exceeds their slot are pinned to the slot's start edge.
 *
 * @param placeables The measured groups, in declaration order.
 * @param width The available width in pixels.
 * @param marginPx The edge inset of the outermost groups in pixels.
 * @param minSpacePx The minimum gap kept between two neighbouring groups in pixels.
 */
private fun Placeable.PlacementScope.placeRelativeGroups(
    placeables: List<Placeable>,
    width: Int,
    marginPx: Int,
    minSpacePx: Int,
) {
    when (placeables.size) {
        0 -> Unit

        1 -> placeables[0].placeRelative((width - placeables[0].width) / 2, 0)

        2 -> {
            placeables[0].placeRelative(marginPx, 0)
            placeables[1].placeRelative(width - marginPx - placeables[1].width, 0)
        }

        else -> {
            val first = placeables.first()
            val last = placeables.last()
            first.placeRelative(marginPx, 0)
            last.placeRelative(width - marginPx - last.width, 0)

            val start = marginPx + first.width + minSpacePx
            val end = (width - marginPx - last.width - minSpacePx).coerceAtLeast(start)
            val middles = placeables.subList(1, placeables.size - 1)
            val slot = (end - start).coerceAtLeast(0) / middles.size
            middles.forEachIndexed { index, placeable ->
                val slotStart = start + slot * index
                val slotEnd = (start + slot * (index + 1)).coerceAtMost(end)
                val centered = slotStart + (slot - placeable.width) / 2
                placeable.placeRelative(centered.coerceIn(slotStart, slotEnd), 0)
            }
        }
    }
}

/**
 * A capsule of [BottomFloatingToolbarItem]s inside a [BottomFloatingToolbar].
 *
 * The capsule is the visible surface: it is blurred, tinted with the container colour and edged with
 * [Modifier.materialShadowEdge], exactly like `COUIBottomFloatingToolbarMenuView.onDraw`, and it lifts
 * off the content with the system's caustic shadow, or [BottomFloatingToolbarDefaults.Shadow] where
 * the system has none. The whole capsule answers the finger
 * through [couiTouchMotion], the native `COUITouchMotionAnimator` with deformation, rubber band and
 * press scale, and carries its `TYPE_BOTTOM_NAVIGATION` spot light.
 *
 * The layout is the native `onMeasure`: the items sit on one line, separated by
 * [BottomFloatingToolbarDefaults.ItemViewSpace], with
 * [BottomFloatingToolbarDefaults.MenuViewItemBgInsetHorizontal] between them and the capsule's ends.
 *
 * Smooth-corner surfaces use [MaterialShadowEdgeDefaults.SquircleCornerWeight] so the material rim
 * follows the glass silhouette. Plain rounded surfaces retain the native single-item versus
 * multi-item weights; the count comes from the items this group measured.
 *
 * @param modifier The modifier to be applied to the capsule.
 * @param content The content of the capsule, usually [BottomFloatingToolbarItem]s.
 */
@Composable
fun BottomFloatingToolbarGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalBackdrop.current
    val containerColor = LocalBottomFloatingToolbarContainerColor.current ?: COUITheme.colorScheme.bar
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f

    var itemCount by remember { mutableIntStateOf(0) }
    val cornerRadius = BottomFloatingToolbarDefaults.capsuleCornerRadius(
        height = BottomFloatingToolbarDefaults.MenuViewHeight,
    )
    val squircleEnabled = isSquircleEnabled()
    val shape = remember(cornerRadius, squircleEnabled) {
        SquircleShape(cornerRadius = cornerRadius, squircleEnabled = squircleEnabled)
    }
    val stroke = remember(isDark) { MaterialStrokeParams.frameworkCapsule13(isDark) }
    val blurColors = BottomFloatingToolbarDefaults.blurColors(isDark)
    val touchState = rememberCOUITouchMotionState()
    val systemCaustic = rememberPlatformCausticShadowRenderer() != null
    val density = LocalDensity.current
    val height = BottomFloatingToolbarDefaults.MenuViewHeight
    var capsuleWidthPx by remember { mutableIntStateOf(0) }
    val capsuleWidth = with(density) { capsuleWidthPx.toDp() }

    // COUIBottomFloatingToolbarMenuView.setRenderEffect / updateSmoothRoundPath: the blur sits under
    // the edge, and both fill the capsule's own silhouette.
    val blurSurface = if (backdrop != null && isRuntimeShaderSupported() && SystemMaterialState.blurEnabled) {
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = BottomFloatingToolbarDefaults.BlurRadius,
            colors = blurColors,
        )
    } else {
        Modifier.squircleBackground(color = containerColor, cornerRadius = cornerRadius)
    }

    // getDisplayFrame: the union of the visible menu views is the toolbar's popup barrier.
    Box(modifier = modifier.popupBottomBarrier()) {
        // The lift rides the press transform so it stays under the capsule. updateOuterShadow gives
        // the capsule either the system's caustic shadow or the level 2 shadow, never both.
        if (capsuleWidthPx > 0) {
            if (systemCaustic) {
                MaterialCausticShadow(
                    particles = listOf(
                        CausticShadowParticle(
                            shape = MetaBallParticle(
                                centerX = capsuleWidth / 2 + with(density) { touchState.translation.x.toDp() },
                                centerY = height / 2 + with(density) { touchState.translation.y.toDp() },
                                width = capsuleWidth,
                                height = height,
                                scaleX = touchState.scaleX,
                                scaleY = touchState.scaleY,
                                cornerRadius = cornerRadius,
                            ),
                            profile = CausticShadowProfile.Bar,
                        ),
                    ),
                    color = BottomFloatingNavigationBarDefaults.CausticColor,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            translationX = touchState.translation.x
                            translationY = touchState.translation.y
                            scaleX = touchState.scaleX
                            scaleY = touchState.scaleY
                        }
                        .dropShadow(shape = shape, shadow = BottomFloatingToolbarDefaults.Shadow),
                )
            }
        }
        Layout(
            content = content,
            modifier = Modifier
                .height(height)
                .onSizeChanged { capsuleWidthPx = it.width }
                .couiTouchMotion(
                    shape = shape,
                    lightParams = SpotLightDefaults.opaqueMedium2(isDark),
                    motionState = touchState,
                    surface = blurSurface
                        // materialShadowEdge returns a bare fallback when the platform rejects
                        // runtime shaders, so the surface is handed back as its own fallback.
                        .materialShadowEdge(
                            stroke = stroke,
                            cornerWeight = if (squircleEnabled) {
                                // The glass fill and highlight use the same Lp(2.5) squircle. Keeping
                                // the material rim on that geometry prevents bright corner lobes
                                // outside the fill.
                                MaterialShadowEdgeDefaults.SquircleCornerWeight
                            } else if (itemCount == 1) {
                                MaterialShadowEdgeDefaults.SingleItemCornerWeight
                            } else {
                                MaterialShadowEdgeDefaults.CornerWeight
                            },
                            cornerRadius = cornerRadius,
                            fallback = blurSurface,
                        ),
                ),
        ) { measurables, constraints ->
            itemCount = measurables.size
            val heightPx = BottomFloatingToolbarDefaults.MenuViewHeight.roundToPx()
            val insetPx = BottomFloatingToolbarDefaults.MenuViewItemBgInsetHorizontal.roundToPx()
            val spacingPx = BottomFloatingToolbarDefaults.ItemViewSpace.roundToPx()
            val placeables = measurables.map { it.measure(Constraints()) }
            val contentWidth = placeables.sumOf { it.width } +
                spacingPx * (placeables.size - 1).coerceAtLeast(0)
            layout(
                constraints.constrainWidth(contentWidth + insetPx * 2),
                constraints.constrainHeight(heightPx),
            ) {
                var x = insetPx
                placeables.forEach { placeable ->
                    placeable.placeRelative(x, (heightPx - placeable.height) / 2)
                    x += placeable.width + spacingPx
                }
            }
        }
    }
}

/**
 * An item of a [BottomFloatingToolbarGroup].
 *
 * `COUIBottomFloatingToolbarItemView` has an icon form and a text form, and this item picks between
 * them the same way: an [icon] gives the icon cell — [BottomFloatingToolbarDefaults.ItemWidth] by
 * [BottomFloatingToolbarDefaults.ItemHeight] with a centred
 * [BottomFloatingToolbarDefaults.IconSize] glyph — while a null [icon] gives the text cell, a
 * [BottomFloatingToolbarDefaults.LabelFontSize] label on one line inside
 * [BottomFloatingToolbarDefaults.ItemTextMinWidth] ..
 * [BottomFloatingToolbarDefaults.ItemTextMaxWidth].
 *
 * @param label The label of the item; it is also the icon's content description when
 *   [contentDescription] is absent.
 * @param onClick Called when the item is activated.
 * @param icon The icon of the item, or `null` for a text-only item.
 * @param enabled Whether the item can be activated.
 * @param contentDescription The icon's content description, or `null` for [label].
 */
@Composable
fun BottomFloatingToolbarItem(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = COUITheme.colorScheme
    // COUIBottomFloatingToolbarItemView takes the item's colours from the same
    // couiColorLabelPrimaryVariant / couiColorOnSurfaceVariantActions family the other floating
    // toolbars use, so the tint mapping is shared with BottomFloatingNavigationBarItem.
    val tint = if (enabled) colorScheme.labelPrimaryVariant else colorScheme.onSurfaceVariantActions
    val clickable = Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onClick = onClick,
    )

    if (icon != null) {
        Box(
            modifier = clickable.size(
                width = BottomFloatingToolbarDefaults.ItemWidth,
                height = BottomFloatingToolbarDefaults.ItemHeight,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = icon,
                contentDescription = contentDescription ?: label,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(BottomFloatingToolbarDefaults.IconSize),
            )
        }
    } else {
        Text(
            text = label,
            modifier = clickable
                .height(BottomFloatingToolbarDefaults.ItemHeight)
                .widthIn(
                    min = BottomFloatingToolbarDefaults.ItemTextMinWidth,
                    max = BottomFloatingToolbarDefaults.ItemTextMaxWidth,
                )
                .padding(
                    horizontal = BottomFloatingToolbarDefaults.ItemTextPaddingHorizontal,
                    vertical = BottomFloatingToolbarDefaults.ItemTextPaddingVertical,
                ),
            color = tint,
            fontSize = BottomFloatingToolbarDefaults.LabelFontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * An item of a [BottomFloatingToolbarGroup] whose icon keeps its own colours, like the gradient AI
 * glyph of the Notes toolbar. The cell is the icon cell of the other overload, without the tint.
 *
 * @param label The label of the item; it is also the icon's content description when
 *   [contentDescription] is absent.
 * @param onClick Called when the item is activated.
 * @param painter The icon of the item, drawn at [BottomFloatingToolbarDefaults.IconSize].
 * @param modifier The modifier to be applied to the item.
 * @param enabled Whether the item can be activated.
 * @param contentDescription The icon's content description, or `null` for [label].
 */
@Composable
fun BottomFloatingToolbarItem(
    label: String,
    onClick: () -> Unit,
    painter: Painter,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val alpha = remember(enabled) { if (enabled) 1f else 0.3f }
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .size(
                width = BottomFloatingToolbarDefaults.ItemWidth,
                height = BottomFloatingToolbarDefaults.ItemHeight,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription ?: label,
            alpha = alpha,
            modifier = Modifier.size(BottomFloatingToolbarDefaults.IconSize),
        )
    }
}

/**
 * The color scheme each [BottomFloatingToolbarGroup] draws its capsule with.
 *
 * The toolbar publishes it so the groups nested in `BottomFloatingToolbar.content` pick up the
 * caller's `BottomFloatingToolbar.containerColor` without having to thread it through themselves.
 * Defaults to `null`, which the group reads as [COUITheme.colorScheme]'s `bar`.
 */
internal val LocalBottomFloatingToolbarContainerColor = compositionLocalOf<Color?> { null }

/** Contains default values used by [BottomFloatingToolbar] and its groups and items. */
object BottomFloatingToolbarDefaults {

    /**
     * The height of a group capsule. COUI `coui_floating_toolbar_menu_view_height`.
     *
     * This is also the height the capsule's corner radius is derived from, via
     * [capsuleCornerRadius].
     */
    val MenuViewHeight = 44.dp

    /**
     * The minimum outer height of the toolbar, i.e. the band the capsules slide in.
     * COUI `coui_floating_toolbar_min_height`.
     */
    val MinHeight = 76.dp

    /**
     * The space around the capsule inside the menu view, which `COUIBottomFloatingToolbar` subtracts
     * again when it places the group. COUI `coui_floating_toolbar_menu_view_padding`.
     */
    val MenuViewPadding = 16.dp

    /**
     * The distance between a capsule's ends and its first and last items.
     * COUI `coui_floating_toolbar_menu_view_item_bg_inset_horizontal`.
     */
    val MenuViewItemBgInsetHorizontal = 6.dp

    /**
     * The shadow lifting a capsule off the content. `BaseBottomFloatingMenuView.updateOuterShadow`
     * gives every floating menu the same level 2 elevation.
     */
    val Shadow: Shadow = BottomFloatingNavigationBarDefaults.BarShadow

    /** The icon cell width. COUI `coui_floating_toolbar_item_width`. */
    val ItemWidth = 44.dp

    /** The icon cell height. COUI `coui_floating_toolbar_item_height`. */
    val ItemHeight = 40.dp

    /** The spacing between items inside a group capsule. COUI `coui_floating_toolbar_item_view_space`. */
    val ItemViewSpace = 4.dp

    /** The icon size. COUI `coui_floating_toolbar_item_icon_width` / `..._icon_height`. */
    val IconSize = 24.dp

    /** The label size. COUI `DEFAULT_TITLE_TEXT_SIZE_SP` (`coui_floating_toolbar_text_size`). */
    val LabelFontSize = 16.sp

    /** The text cell's horizontal padding. COUI `coui_floating_toolbar_item_text_padding_horizontal`. */
    val ItemTextPaddingHorizontal = 16.dp

    /** The text cell's vertical padding. COUI `coui_floating_toolbar_item_text_padding_vertical`. */
    val ItemTextPaddingVertical = 10.dp

    /** The text cell's minimum width. COUI `coui_floating_toolbar_item_text_min_width`. */
    val ItemTextMinWidth = 52.dp

    /** The text cell's maximum width. COUI `coui_floating_toolbar_item_text_max_width`. */
    val ItemTextMaxWidth = 200.dp

    /**
     * The radius of an item's circular touch mask. COUI `coui_floating_toolbar_item_circle_radius`.
     *
     * Exposed for callers that draw their own item background; the built-in items rely on the
     * capsule-wide [BottomFloatingNavigationBarDefaults.TouchEffectRadius] highlight instead.
     */
    val ItemCircleRadius = 20.dp

    /**
     * The radius of the circular mask drawn behind an icon item.
     * COUI `coui_floating_toolbar_mask_view_icon_radius`.
     */
    val MaskViewIconRadius = 20.dp

    /**
     * The radius of the radial press highlight. COUI `coui_floating_toolbar_menu_touch_effect_radius`.
     *
     * The highlight itself comes from the shared
     * [BottomFloatingNavigationBarDefaults.TouchEffectRadius], the value the same
     * `BaseBottomFloatingMenuView` touch effect uses for every floating toolbar.
     */
    val MenuTouchEffectRadius = 40.dp

    /**
     * How far the single-item mask is inset from the capsule's end edges.
     * COUI `coui_floating_toolbar_item_single_mask_inset_horizontal`.
     */
    val ItemSingleMaskInsetHorizontal = 2.dp

    /** The horizontal offset of the red dot from the icon's end corner. COUI `coui_floating_toolbar_red_dot_offset`. */
    val RedDotOffset = 12.dp

    /**
     * The red dot's diameter.
     *
     * COUI `coui_floating_toolbar_red_dot_radius`; as in
     * [BottomFloatingNavigationBarDefaults.RedDotDiameter] the resource name says radius but the
     * value is passed as a diameter.
     */
    val RedDotRadius = 6.dp

    /** The minimum gap kept between two group capsules. COUI `coui_floating_toolbar_min_group_space`. */
    val MinGroupSpace = 16.dp

    /** The edge inset of the outermost capsules on a small screen. COUI `coui_floating_toolbar_group_margin_small_screen`. */
    val GroupMarginSmallScreen = 16.dp

    /** The edge inset of the outermost capsules on a medium screen. COUI `coui_floating_toolbar_group_margin_medium_screen`. */
    val GroupMarginMediumScreen = 24.dp

    /** The edge inset of the outermost capsules on a large screen. COUI `coui_floating_toolbar_group_margin_large_screen`. */
    val GroupMarginLargeScreen = 40.dp

    /**
     * The blur radius in dp.
     *
     * COUI's toolbar stacks two `RenderEffect.createBlurEffect` items, both with a radius of 25
     * pixels; [textureBlur] takes dp, so the same 25 is expressed in dp here, as in
     * [GlassDefaults.BlurRadius].
     */
    val BlurRadius = 25f

    /**
     * The tint layers laid over the blurred backdrop in the light theme.
     *
     * `COUIBottomFloatingToolbar` blends `#99F5F5F5` in `Luminosity` first and `#B3666666` in
     * `ColorDodge` second; the native chain applies its second `RenderEffect` first, and
     * [textureBlur]'s `blendColors` are applied in list order, so the list is reversed here.
     */
    val BlurColorsLight: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0x99F5F5F5), mode = BlurBlendMode.Luminosity),
        BlendColorEntry(color = Color(0xB3666666), mode = BlurBlendMode.ColorDodge),
    )

    /**
     * The tint layers laid over the blurred backdrop in the dark theme.
     *
     * The dark counterparts of [BlurColorsLight]: `#B3262626` in `Luminosity` and `#80666666` in
     * `Overlay`, with the same reversal applied.
     */
    val BlurColorsDark: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0xB3262626), mode = BlurBlendMode.Luminosity),
        BlendColorEntry(color = Color(0x80666666), mode = BlurBlendMode.Overlay),
    )

    /**
     * The show / hide spring: `MENU_VIEW_TRANSLATION_Y_SPRING_RESPONSE` (0.38) with
     * `MENU_VIEW_TRANSLATION_Y_SPRING_BOUNCE` (0.75), cut at the native
     * `MENU_VIEW_TRANSLATION_Y_MIN_VISIBLE_CHANGE` of 0.001f.
     */
    val TranslationYSpring = folmeSpring(
        damping = 0.75f,
        response = 0.38f,
        visibilityThreshold = 0.001f,
    )

    /**
     * The capsule corner radius: the `mBackgroundBounds.height() / 2`
     * `BaseBottomFloatingMenuView.updateSmoothRoundPath` uses, with the native's own top and bottom
     * paddings subtracted first.
     */
    fun capsuleCornerRadius(
        height: Dp,
        paddingTop: Dp = 0.dp,
        paddingBottom: Dp = 0.dp,
    ): Dp = (height - paddingTop - paddingBottom) / 2

    /** The remembered blur tint layers [BottomFloatingToolbarGroup] feeds to [textureBlur]. */
    @Composable
    fun blurColors(isDark: Boolean): BlurColors = BlurDefaults.blurColors(
        blendColors = if (isDark) BlurColorsDark else BlurColorsLight,
    )
}
