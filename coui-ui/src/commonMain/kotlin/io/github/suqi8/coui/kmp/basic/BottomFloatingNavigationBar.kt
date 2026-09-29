// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

/**
 * A floating capsule bottom navigation bar.
 *
 * This is the Compose port of the ColorOS 17 native `COUIBottomFloatingNavigationBar`. On CO17 this
 * bar coexists with, and is a different component from, [FloatingNavigationBar], which models the
 * native floating bar with 36dp outside padding, a 52dp height and icon-only items.
 *
 * The default [containerColor] is the native background fallback `coui_tab_navigation_view_bg`,
 * whose shape fill is `?attr/couiColorBar` (`coui_color_bar`); that is the value carried by
 * `COUITheme.colorScheme.bar`. When a [backdrop] is available the capsule draws it blurred, lays
 * [containerColor] over it at [GlassDefaults.ContainerAlpha] and lights the rim with a glass stroke
 * — the ColorOS 17 light-sensing glass. Without a backdrop the capsule keeps its opaque fill.
 *
 * When [expansion] is supplied the bar reserves room for it next to the capsule and draws both
 * silhouettes through one [MetaBallSurface], so the expansion view melts out of the capsule the way
 * the native `COUIMaterialEffect.setMetaBallChain` joins its `{mainView, menuView}` chain. The
 * chain is `{capsule, expansion}` here, driven by [expanded] with the native spring (response 0.55,
 * bounce 0.35). While the expansion view is shown the capsule is measured against the width it
 * leaves over — its footprint plus [BottomFloatingNavigationBarDefaults.SideSpacing] — the same
 * bound `COUIBottomFloatingNavigationBar.onMeasure` gives its menu view, and it takes the full width
 * back once the expansion view is gone.
 *
 * Parts of the native component are still not implemented here: the long-press overflow menu
 * (`COUINavigationPopupMenu`) is supplied by the caller through
 * [BottomFloatingNavigationBarItem]'s `overflowMenu` slot, and the responsive large-screen and
 * horizontal branches of the menu view are not ported.
 *
 * @param modifier The modifier to be applied to the bar.
 * @param containerColor The background color of the capsule.
 * @param backdrop The content blurred behind the capsule, defaulting to [LocalBackdrop]. `null`
 *   keeps the capsule opaque.
 * @param expanded Whether the [expansion] view is shown. Ignored when [expansion] is `null`.
 * @param expansion The view that melts out of the capsule, typically a round action button. When
 *   `null` the bar is a plain capsule and [expanded] has no effect.
 * @param content The content of the bar, usually [BottomFloatingNavigationBarItem]s.
 */
@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
fun BottomFloatingNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = COUITheme.colorScheme.bar,
    backdrop: Backdrop? = LocalBackdrop.current,
    expanded: Boolean = false,
    expansion: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (expansion == null) {
        BottomFloatingNavigationBarCapsule(
            modifier = modifier,
            containerColor = containerColor,
            backdrop = backdrop,
            content = content,
        )
    } else {
        BottomFloatingNavigationBarMeltBall(
            modifier = modifier,
            containerColor = containerColor,
            backdrop = backdrop,
            expanded = expanded,
            expansion = expansion,
            content = content,
        )
    }
}

/** The plain capsule bar: the ColorOS 17 glass surface with no expansion view. */
@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
private fun BottomFloatingNavigationBarCapsule(
    containerColor: Color,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val cornerRadius = BottomFloatingNavigationBarDefaults.CornerRadius
    val squircleEnabled = isSquircleEnabled()
    val shape = remember(cornerRadius, squircleEnabled) {
        SquircleShape(cornerRadius = cornerRadius, squircleEnabled = squircleEnabled)
    }
    val highlight = rememberGlassHighlight(GlassContainer.Medium, backdrop)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // COUIBottomFloatingNavigationBar.onMeasure: the 16dp outside padding
        // (coui_bottom_floating_navigation_bar_padding_horizontal) is kept on a small screen only and
        // collapses to zero on the medium and large tiers. The tier is read from the width the bar is
        // laid out into, which is the width the native view measures itself against.
        val horizontalPadding = COUIResponsiveUtils.pick(
            windowSizeClass = COUIResponsiveUtils.windowSizeClass(maxWidth),
            compat = BottomFloatingNavigationBarDefaults.PaddingHorizontal,
            medium = 0.dp,
            expanded = 0.dp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BottomFloatingNavigationBarDefaults.MinHeightWithoutPaddingVertical)
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = BottomFloatingNavigationBarDefaults.BarHeight)
                    .height(BottomFloatingNavigationBarDefaults.BarHeight)
                    .couiGlassSurface(
                        backdrop = backdrop,
                        shape = shape,
                        containerColor = containerColor,
                        fallback = Modifier.squircleBackground(color = containerColor, cornerRadius = cornerRadius),
                        highlight = highlight,
                    )
                    .bottomFloatingTouchHighlight(shape)
                    .then(modifier),
                horizontalArrangement = Arrangement.spacedBy(
                    BottomFloatingNavigationBarDefaults.ItemSpacing,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/**
 * The capsule plus an [expansion] view that melts into it.
 *
 * The capsule keeps its own measured width; the gap and the expansion view's footprint are driven
 * by [expanded] through `progress`, and the merged silhouette is one [MetaBallSurface] whose second
 * particle is exactly the expansion view's current footprint. That keeps the blob and the layout in
 * lockstep: shrinking the particle to nothing leaves the bar as a plain capsule.
 */
@Composable
private fun BottomFloatingNavigationBarMeltBall(
    containerColor: Color,
    backdrop: Backdrop?,
    expanded: Boolean,
    expansion: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalDensity.current
    val barHeight = BottomFloatingNavigationBarDefaults.BarHeight
    val cornerRadius = BottomFloatingNavigationBarDefaults.CornerRadius
    val expansionSize = BottomFloatingNavigationBarDefaults.ExpansionSize
    val sideSpacing = BottomFloatingNavigationBarDefaults.SideSpacing

    // COUI drives the expansion view with a spring at response 0.55 / bounce 0.35 once the melt ball
    // is supported; Compose's spring takes `stiffness` = omega0^2 = (2*pi / 0.55)^2 and
    // `dampingRatio` = 1 - bounce.
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 130f),
        label = "meltBallExpansion",
    )

    var capsuleWidthPx by remember { mutableIntStateOf(0) }
    val gap = sideSpacing * progress
    val expansionFootprint = expansionSize * progress

    val squircleEnabled = isSquircleEnabled()
    val capsuleShape = remember(cornerRadius, squircleEnabled) {
        SquircleShape(cornerRadius = cornerRadius, squircleEnabled = squircleEnabled)
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // COUIBottomFloatingNavigationBar.onMeasure keeps the 16dp outside padding on a small screen
        // only; the medium and large tiers collapse it to zero.
        val horizontalPadding = COUIResponsiveUtils.pick(
            windowSizeClass = COUIResponsiveUtils.windowSizeClass(maxWidth),
            compat = BottomFloatingNavigationBarDefaults.PaddingHorizontal,
            medium = 0.dp,
            expanded = 0.dp,
        )
        // COUI narrows the menu view by exactly the expansion view's footprint plus the side spacing
        // while the expansion view is active, and hands it the full width again once it is inactive
        // (`COUIBottomFloatingNavigationBar.onMeasure`). Deriving the bound from the animated values
        // keeps the capsule and the blob in step through the transition.
        val capsuleMaxWidth =
            (maxWidth - horizontalPadding * 2 - gap - expansionFootprint).coerceAtLeast(0.dp)

        // The blob is a match-parent sibling behind the row, so the row's size is the bar's size and
        // the particles below are expressed in exactly those coordinates.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BottomFloatingNavigationBarDefaults.MinHeightWithoutPaddingVertical)
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            val capsuleWidth = with(density) { capsuleWidthPx.toDp() }
            MetaBallSurface(
                particles = buildList {
                    if (capsuleWidthPx > 0) {
                        add(
                            MetaBallParticle(
                                centerX = capsuleWidth / 2,
                                centerY = barHeight / 2,
                                width = capsuleWidth,
                                height = barHeight,
                                cornerRadius = cornerRadius,
                            ),
                        )
                    }
                    if (expansionFootprint > 0.dp) {
                        add(
                            MetaBallParticle.circle(
                                centerX = capsuleWidth + gap + expansionFootprint / 2,
                                centerY = barHeight / 2,
                                diameter = expansionFootprint,
                            ),
                        )
                    }
                },
                modifier = Modifier.matchParentSize(),
                color = containerColor,
                backdrop = backdrop,
                // The merged outline gets the glass rim; without a backdrop the blob is opaque and
                // the native component draws no stroke either.
                rimColor = if (backdrop != null) Color.White.copy(alpha = 0.55f) else null,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .widthIn(max = capsuleMaxWidth)
                        .height(barHeight)
                        .onSizeChanged { capsuleWidthPx = it.width }
                        .bottomFloatingTouchHighlight(capsuleShape),
                    horizontalArrangement = Arrangement.spacedBy(
                        BottomFloatingNavigationBarDefaults.ItemSpacing,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
                Box(modifier = Modifier.width(gap).height(barHeight))
                Box(
                    modifier = Modifier.width(expansionFootprint).height(barHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    // Laid out at full size and scaled down, so the content keeps its proportions
                    // while the space it occupies animates with `progress`.
                    Box(
                        modifier = Modifier
                            .size(expansionSize)
                            .graphicsLayer {
                                scaleX = progress
                                scaleY = progress
                                alpha = progress
                            },
                    ) {
                        expansion()
                    }
                }
            }
        }
    }
}

/**
 * A [BottomFloatingNavigationBarItem] that is suitable for [BottomFloatingNavigationBar].
 *
 * @param selected Whether the item is selected.
 * @param onClick The callback when the item is clicked.
 * @param icon The icon of the item.
 * @param label The label of the item.
 * @param modifier The modifier to be applied to the [BottomFloatingNavigationBarItem].
 * @param enabled Whether the item is enabled.
 * @param largeIcon Whether to draw the 40dp large-icon form
 *   ([BottomFloatingNavigationBarDefaults.LargeIconSize]) overflowing the item by
 *   [BottomFloatingNavigationBarDefaults.LargeIconOverflow] instead of the 24dp icon
 *   (`COUIBottomFloatingNavigationBarItem.Builder.setLargeIcon`). The title keeps its place, exactly
 *   as `COUIBottomFloatingToolbarItemView.layoutNaviLargeIcon` lays it out below the icon slot.
 * @param redDot The red dot pinned to the icon's top end corner, or `null` for none
 *   (`BaseBottomFloatingItem.setRedDotType` / `setRedDotNumber`).
 * @param overflowMenu The rows of the long-press overflow menu, usually
 *   [NavigationPopupMenuItem]s; they are shown in a [NavigationPopupMenu] anchored to this item.
 *   `null` disables the long press.
 */
@Composable
fun RowScope.BottomFloatingNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    largeIcon: Boolean = false,
    redDot: BottomFloatingRedDot? = null,
    overflowMenu: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = COUITheme.colorScheme

    // COUI item label/icon colors: couiColorLabelTertiary / couiColorLabelPrimary /
    // couiColorLabelPrimaryVariant. The same color tints both the icon and the label.
    val tint = when {
        !enabled -> colorScheme.onSurfaceVariantActions
        selected -> colorScheme.onSurface
        else -> colorScheme.labelPrimaryVariant
    }

    // COUIBottomFloatingNavigationBarMenuView opens the overflow menu on a long press and the native
    // item's onLongClick returns true, so the click that ends that same gesture is swallowed —
    // `combinedClickable` already gives exactly that pairing.
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(BottomFloatingNavigationBarDefaults.ItemHeight)
            .widthIn(max = BottomFloatingNavigationBarDefaults.ItemMaxWidth)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Tab,
                onLongClick = if (overflowMenu == null) {
                    null
                } else {
                    { menuExpanded = true }
                },
                onClick = onClick,
            )
            .semantics { this.selected = selected },
    ) {
        // The native item pins icon and title to the top of the cell, so the space below the label
        // stays empty on purpose.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = BottomFloatingNavigationBarDefaults.ItemInnerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(BottomFloatingNavigationBarDefaults.IconTopMargin))
            Box(
                modifier = Modifier.offset(
                    // layoutNaviLargeIcon draws the icon at -mLargeIconOverflow from the item's top
                    // edge; the 4dp top margin is already applied by the spacer above.
                    y = if (largeIcon) {
                        -(BottomFloatingNavigationBarDefaults.LargeIconOverflow + BottomFloatingNavigationBarDefaults.IconTopMargin)
                    } else {
                        0.dp
                    },
                ),
            ) {
                Image(
                    modifier = Modifier.size(
                        if (largeIcon) {
                            BottomFloatingNavigationBarDefaults.LargeIconSize
                        } else {
                            BottomFloatingNavigationBarDefaults.IconSize
                        },
                    ),
                    imageVector = icon,
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(tint),
                )
                if (redDot != null) {
                    BottomFloatingRedDotBadge(
                        redDot = redDot,
                        largeIcon = largeIcon,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
            Spacer(Modifier.height(BottomFloatingNavigationBarDefaults.IconTitleGapVertical))
            Text(
                text = label,
                color = tint,
                fontSize = BottomFloatingNavigationBarDefaults.LabelFontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        if (overflowMenu != null) {
            NavigationPopupMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                overflowMenu()
            }
        }
    }
}

/**
 * The red dot pinned to a [BottomFloatingNavigationBarItem]'s icon.
 *
 * `BaseBottomFloatingItem` carries a `redDotType` / `redDotNumber` pair; type 0 hides the dot, type 1
 * draws a plain dot and type 2 draws a number capsule. The plain dot is [Dot] and the capsule is
 * [Count], which covers both of the visible types.
 */
@Immutable
sealed interface BottomFloatingRedDot {

    /** A plain dot (`COUIHintRedDot.POINT_ONLY_MODE`, `redDotType == 1`). */
    data object Dot : BottomFloatingRedDot

    /** A capsule showing [number] (`COUIHintRedDot.POINT_NUM_MODE`, `redDotType == 2`). */
    data class Count(val number: Int) : BottomFloatingRedDot
}

/**
 * The shared red-dot helper for the floating bottom bars, used by [BottomFloatingNavigationBarItem]
 * and [BottomFloatingEditToolbarItem].
 *
 * `COUIBottomFloatingToolbarItemView.layoutRedDotRelativeToIcon` anchors the dot to the icon box: a
 * plain dot sits flush with the icon's top and end edges, a number capsule is pushed out by
 * [BottomFloatingNavigationBarDefaults.RedDotTopOverflow] (4dp) and by
 * [BottomFloatingNavigationBarDefaults.RedDotRightOverflow1Digit] / `2Digit` / `3Digit`
 * (6 / 9 / 10dp, by digit count). `layoutNaviLargeIcon` instead clamps the dot's top to the item's
 * own top edge — the 40dp icon is drawn 14dp above it — and leaves the dot flush with the end edge.
 */
@Composable
internal fun BottomFloatingRedDotBadge(
    redDot: BottomFloatingRedDot,
    largeIcon: Boolean,
    modifier: Modifier = Modifier,
) {
    val count = when (redDot) {
        BottomFloatingRedDot.Dot -> 0
        is BottomFloatingRedDot.Count -> redDot.number
    }
    val rightOverflow: Dp
    val topOverflow: Dp
    if (largeIcon) {
        rightOverflow = 0.dp
        topOverflow = BottomFloatingNavigationBarDefaults.LargeIconOverflow
    } else {
        rightOverflow = when {
            count <= 0 -> 0.dp
            count < 10 -> BottomFloatingNavigationBarDefaults.RedDotRightOverflow1Digit
            count < 100 -> BottomFloatingNavigationBarDefaults.RedDotRightOverflow2Digit
            else -> BottomFloatingNavigationBarDefaults.RedDotRightOverflow3Digit
        }
        topOverflow = if (count > 0) -BottomFloatingNavigationBarDefaults.RedDotTopOverflow else 0.dp
    }

    Badge(
        modifier = modifier.offset(x = rightOverflow, y = topOverflow),
        count = count,
        stroke = false,
        dotDiameter = BottomFloatingNavigationBarDefaults.RedDotDiameter,
    )
}

/**
 * Contains default values used by [BottomFloatingNavigationBar] and [BottomFloatingNavigationBarItem].
 */
object BottomFloatingNavigationBarDefaults {
    /** The capsule height. COUI `coui_bottom_floating_navigation_bar_height`. */
    val BarHeight = 56.dp

    /** The minimum outer height of the bar. COUI `coui_bottom_floating_navigation_bar_min_height_without_padding_vertical`. */
    val MinHeightWithoutPaddingVertical = 88.dp

    /**
     * The horizontal padding outside the capsule on a small screen.
     *
     * COUI `coui_bottom_floating_navigation_bar_padding_horizontal`;
     * `COUIBottomFloatingNavigationBar.onMeasure` keeps it only when
     * `COUIResponsiveUtils.isSmallScreen` and collapses it to zero on the wider tiers.
     */
    val PaddingHorizontal = 16.dp

    /**
     * The footprint of the expansion view that melts out of the capsule.
     *
     * COUI `mFakeViewSize`, the square the expansion view's `mMainView` is bound to when the bar
     * makes room for it; it is also the bar height, so the two silhouettes share a centre line.
     */
    val ExpansionSize = 56.dp

    /** The gap kept between the capsule and the expansion view. COUI `mSideViewSpacing`. */
    val SideSpacing = 12.dp

    /** The item cell height. COUI `coui_bottom_floating_navigation_bar_item_height`. */
    val ItemHeight = 48.dp

    /** The maximum width of an item cell. COUI `coui_bottom_floating_navigation_bar_item_max_width`. */
    val ItemMaxWidth = 70.dp

    /** The spacing between items. COUI `coui_bottom_floating_navigation_bar_item_space`. */
    val ItemSpacing = 8.dp

    /** The horizontal padding inside an item cell. COUI `coui_bottom_floating_navigation_bar_item_padding`. */
    val ItemInnerPadding = 12.dp

    /** The horizontal padding of the item background. COUI `coui_bottom_floating_navigation_bar_item_bg_horizontal_padding`. */
    val ItemBackgroundHorizontalPadding = 8.dp

    /** The icon size. COUI `coui_navigation_icon_size`. */
    val IconSize = 24.dp

    /** The top margin of the icon. COUI `coui_bottom_floating_navigation_bar_item_icon_top_margin`. */
    val IconTopMargin = 4.dp

    /** The vertical gap between the icon and the title. COUI `coui_bottom_floating_navigation_bar_item_icon_title_gap_vertical`. */
    val IconTitleGapVertical = 2.dp

    /** The horizontal gap between the icon and the title. COUI `coui_bottom_floating_navigation_bar_item_icon_title_gap_horizontal`. */
    val IconTitleGapHorizontal = 4.dp

    /**
     * The icon size of the large-icon form.
     * COUI `coui_bottom_floating_navigation_bar_large_icon_size`.
     */
    val LargeIconSize = 40.dp

    /**
     * How far the large-icon form is drawn above the item's top edge.
     * COUI `coui_bottom_floating_navigation_bar_large_icon_overflow`.
     */
    val LargeIconOverflow = 14.dp

    /**
     * The diameter of the plain red dot.
     *
     * COUI `coui_floating_toolbar_red_dot_radius`, passed to `COUIHintRedDot.setDotDiameter`, so
     * despite the resource name the value is the dot's diameter, not its radius.
     */
    val RedDotDiameter = 6.dp

    /** The red dot's end-edge overflow for a one-digit count. COUI `coui_bottom_floating_navigation_bar_red_dot_right_overflow_1_digit`. */
    val RedDotRightOverflow1Digit = 6.dp

    /** The red dot's end-edge overflow for a two-digit count. COUI `coui_bottom_floating_navigation_bar_red_dot_right_overflow_2_digit`. */
    val RedDotRightOverflow2Digit = 9.dp

    /** The red dot's end-edge overflow for a three-digit count. COUI `coui_bottom_floating_navigation_bar_red_dot_right_overflow_3_digit`. */
    val RedDotRightOverflow3Digit = 10.dp

    /** The red dot's top overflow for a number. COUI `coui_bottom_floating_navigation_bar_red_dot_top_overflow`. */
    val RedDotTopOverflow = 4.dp

    /** The radius of the radial touch highlight. COUI `coui_bottom_floating_navigation_bar_touch_effect_radius`. */
    val TouchEffectRadius = 40.dp

    /** The minimum spacing kept with a slide view. COUI `coui_bottom_floating_navigation_bar_Minimum_spacing_with_slide_view`. */
    val MinimumSpacingWithSlideView = 12.dp

    /**
     * The label font size. COUI `coui_navigation_item_text_size`.
     *
     * Inferred, not measured: the native item's textAppearance is `couiTextButtonM` (14sp), while
     * 10sp here is derived from the 48dp item height budget (4 + 24 + 2 leaves room for roughly a
     * 10sp line).
     */
    val LabelFontSize = 10.sp

    /**
     * The capsule corner radius. The native code computes
     * `mRadius = (getHeight() - paddingTop - paddingBottom) / 2.0f`, i.e. a height/2 capsule.
     */
    val CornerRadius = 28.dp
}

/**
 * The centre alpha of the radial touch highlight.
 *
 * `BaseBottomFloatingMenuView.TOUCH_EFFECT_CENTER_ALPHA` = 637534208 = `0x26000000`, OR-ed onto the
 * light colour by `createTouchEffectRadialGradient`, so the gradient starts at 38/255 alpha.
 */
private const val TOUCH_EFFECT_CENTER_ALPHA = 0x26 / 255f

/**
 * The radial touch highlight COUI paints under the menu items while a finger is down.
 *
 * `BaseBottomFloatingMenuView` builds the gradient in `createTouchEffectRadialGradient` and clips it
 * to the menu view's background path in `drawTouchEffectDrawable`: the light colour
 * (`resolveTouchEffectLightColor` = `TOUCH_EFFECT_LIGHT_COLOR`, white) runs from
 * [TOUCH_EFFECT_CENTER_ALPHA] in the centre to fully transparent at
 * [BottomFloatingNavigationBarDefaults.TouchEffectRadius] (40dp), and the whole thing fades in on the
 * `TOUCH_EFFECT_ALPHA_SPRING_RESPONSE` (0.2) / `TOUCH_EFFECT_ALPHA_SPRING_BOUNCE` (0) spring.
 *
 * The native class gates this shader path off (`SHADER_TOUCH_FEEDBACK_ENABLED = false`) in favour of
 * `COUISpotLightEffectDrawable`; the geometry and colours here are the ones the shader path documents.
 *
 * The pointer is tracked in [PointerEventPass.Initial] and consumed nowhere, so the items' own tap
 * and long-press gestures see the whole press.
 *
 * @param shape The capsule silhouette the highlight is clipped to.
 */
@Composable
internal fun Modifier.bottomFloatingTouchHighlight(shape: Shape): Modifier {
    val radius = BottomFloatingNavigationBarDefaults.TouchEffectRadius
    var touchCenter by remember { mutableStateOf<Offset?>(null) }
    var pressed by remember { mutableStateOf(false) }
    val highlightAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = folmeSpring(damping = 1f, response = 0.2f),
        label = "bottomFloatingTouchHighlight",
    )

    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    touchCenter = down.position
                    pressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    pressed = false
                }
            }
        }
        .drawWithContent {
            val center = touchCenter
            if (center != null && highlightAlpha > 0f) {
                val radiusPx = radius.toPx()
                clipPath(shape.createOutline(size, layoutDirection, this).toPath()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = TOUCH_EFFECT_CENTER_ALPHA * highlightAlpha),
                                Color.White.copy(alpha = 0f),
                            ),
                            center = center,
                            radius = radiusPx,
                        ),
                        radius = radiusPx,
                        center = center,
                    )
                }
            }
            // The highlight sits above the glass but below the items, exactly like the native onDraw.
            drawContent()
        }
}

/** The [Outline] as a [Path], so a draw scope can clip to it. */
private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
