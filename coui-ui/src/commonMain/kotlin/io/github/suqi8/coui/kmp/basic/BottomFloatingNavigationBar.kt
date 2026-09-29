// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.squircle.squircleBackground
import io.github.suqi8.coui.kmp.theme.COUITheme

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
 * (`COUINavigationPopupMenu`), the large-icon item style (40dp icon with 14dp overflow), the red-dot
 * badge, the radial-gradient touch highlight (`TouchEffectRadius`), and the responsive large-screen
 * and horizontal branches.
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BottomFloatingNavigationBarDefaults.MinHeightWithoutPaddingVertical)
            .padding(horizontal = BottomFloatingNavigationBarDefaults.PaddingHorizontal),
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottomFloatingNavigationBarDefaults.MinHeightWithoutPaddingVertical)
            .padding(horizontal = BottomFloatingNavigationBarDefaults.PaddingHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        // COUI narrows the menu view by exactly the expansion view's footprint plus the side spacing
        // while the expansion view is active, and hands it the full width again once it is inactive
        // (`COUIBottomFloatingNavigationBar.onMeasure`). Deriving the bound from the animated values
        // keeps the capsule and the blob in step through the transition.
        val capsuleMaxWidth = (maxWidth - gap - expansionFootprint).coerceAtLeast(0.dp)

        // The blob is a match-parent sibling behind the row, so the row's size is the bar's size and
        // the particles below are expressed in exactly those coordinates.
        Box {
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
                        .onSizeChanged { capsuleWidthPx = it.width },
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
 */
@Composable
fun RowScope.BottomFloatingNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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

    Box(
        modifier = modifier
            .height(BottomFloatingNavigationBarDefaults.ItemHeight)
            .widthIn(max = BottomFloatingNavigationBarDefaults.ItemMaxWidth)
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            ),
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
            Image(
                modifier = Modifier.size(BottomFloatingNavigationBarDefaults.IconSize),
                imageVector = icon,
                contentDescription = label,
                colorFilter = ColorFilter.tint(tint),
            )
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
    }
}

/**
 * Contains default values used by [BottomFloatingNavigationBar] and [BottomFloatingNavigationBarItem].
 */
object BottomFloatingNavigationBarDefaults {
    /** The capsule height. COUI `coui_bottom_floating_navigation_bar_height`. */
    val BarHeight = 56.dp

    /** The minimum outer height of the bar. COUI `coui_bottom_floating_navigation_bar_min_height_without_padding_vertical`. */
    val MinHeightWithoutPaddingVertical = 88.dp

    /** The horizontal padding outside the capsule. COUI `coui_bottom_floating_navigation_bar_padding_horizontal`. */
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
