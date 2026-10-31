// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
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
 * `COUITheme.colorScheme.bar`. When a [backdrop] is available the capsule wears the material
 * `BaseBottomFloatingMenuView.updateBlurEffect` requests instead, `TYPE_FRAMEWORK_BOTTOM_BAR`: the
 * backdrop blurred at the native radius ([BottomFloatingNavigationBarDefaults.blurRadius]) and
 * tinted with [BottomFloatingNavigationBarDefaults.BlurColorsLight] /
 * [BottomFloatingNavigationBarDefaults.BlurColorsDark], which keep the backdrop's colour and only
 * remap its lightness; the rim is lit with the `TYPE_FRAMEWORK_CAPSULE_3` stroke and the bar casts
 * the native caustic light. The native view also drops `couiColorBar` once the effect is on, so
 * [containerColor] only fills the capsule when there is no backdrop.
 *
 * The capsule is measured the way `BaseBottomFloatingMenuView.onMeasure` measures its menu view: it
 * is as wide as its items at [BottomFloatingNavigationBarDefaults.ItemMaxWidth] plus the 12dp
 * [BottomFloatingNavigationBarDefaults.ItemInnerPadding] and the gaps between them, and it is
 * squeezed — items and all — into the width the bar leaves over when they do not all fit. The
 * capsule is centred vertically in the bar's 88dp box, as `COUIBottomFloatingNavigationBar.onLayout`
 * centres its menu view; the room below the box belongs to the host, which ColorOS apps fill with
 * [BottomFloatingNavigationBarDefaults.navigationBarPadding] rather than the full navigation inset.
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
 * The long-press overflow menu (`COUINavigationPopupMenu`) is not part of this component: the caller
 * supplies it through [BottomFloatingNavigationBarItem]'s `overflowMenu` slot.
 *
 * `COUIBottomFloatingNavigationBarMenuView` itself has no responsive and no landscape branch — the
 * only orientation-aware code in the whole component is the `onConfigurationChanged` gate that skips
 * the next layout animation, which recomposition replaces here. The parent bar carries the two width
 * tiers that do exist. The first is the 16dp outside padding, kept by `changePadding` on a small
 * screen and dropped above it; that one is ported above. The second is `couiMenuViewCentered`: on a
 * small screen, with the attribute set and exactly two visible items,
 * `measureMenuViewWithCenteredExpansionReserve` leaves the expansion view's footprint plus
 * [BottomFloatingNavigationBarDefaults.SideSpacing] free on *both* sides of the capsule, and
 * `computeExpansionActiveHorizontalPositions` then centres the capsule in what is left with the
 * expansion view pinned to the bar's trailing edge. That branch is deliberately not ported: the
 * attribute defaults to false and no ColorOS 17 artifact this port was written against sets it, so
 * the trailing-side arrangement is the only one reachable here. With the expansion view hidden
 * `layoutWhenExpansionViewInactive` centres the capsule whichever branch was taken, so the two
 * arrangements coincide in the resting state regardless.
 *
 * @param modifier The modifier to be applied to the bar.
 * @param containerColor The opaque fill of the capsule when there is no [backdrop] to blur.
 * @param backdrop The content blurred behind the capsule, defaulting to [LocalBackdrop]. `null`
 *   keeps the capsule opaque.
 * @param expanded Whether the [expansion] view is shown. Ignored when [expansion] is `null`.
 * @param expansion The view that melts out of the capsule, typically a round action button. When
 *   `null` the bar is a plain capsule and [expanded] has no effect.
 * @param shadow The shadow the capsule and the expansion view each cast, or `null` for none. A light
 *   [Shadow.color] on a dark theme turns it into an outer glow. Where the system draws the caustic
 *   light under a bar with an [expansion] view, that caustic already casts the native spot shadow, so
 *   the default [BottomFloatingNavigationBarDefaults.BarShadow] is left out there.
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
    shadow: Shadow? = BottomFloatingNavigationBarDefaults.BarShadow,
    content: @Composable RowScope.() -> Unit,
) {
    if (expansion == null) {
        BottomFloatingNavigationBarCapsule(
            modifier = modifier,
            containerColor = containerColor,
            backdrop = backdrop,
            shadow = shadow,
            content = content,
        )
    } else {
        BottomFloatingNavigationBarMeltBall(
            modifier = modifier,
            containerColor = containerColor,
            backdrop = backdrop,
            expanded = expanded,
            expansion = expansion,
            shadow = shadow,
            content = content,
        )
    }
}

@Deprecated(
    message = "Migrated to the overload that adds a shadow parameter.",
    level = DeprecationLevel.HIDDEN,
)
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
    BottomFloatingNavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        backdrop = backdrop,
        expanded = expanded,
        expansion = expansion,
        shadow = BottomFloatingNavigationBarDefaults.BarShadow,
        content = content,
    )
}

/**
 * The capsule's content row: the ColorOS 17 menu view layout, where every item gets an exact width.
 *
 * `BaseBottomFloatingMenuView.onMeasure` computes the capsule's width from the item count and then
 * measures every child with an exact width, so the capsule is either as wide as `n` maximum-width
 * items or is squeezed to whatever the bar leaves over:
 *
 * - `innerPadding` = [BottomFloatingNavigationBarDefaults.ItemInnerPadding] (12dp), `itemSpace` =
 *   [BottomFloatingNavigationBarDefaults.ItemSpacing] (8dp) and `itemMaxWidth` =
 *   [BottomFloatingNavigationBarDefaults.ItemMaxWidth] (70dp);
 * - `totalItemMax = n * itemMaxWidth + 2 * innerPadding + (n - 1) * itemSpace`;
 * - when `totalItemMax` fits the available width the capsule is `totalItemMax` wide and every item is
 *   `itemMaxWidth` wide; otherwise the capsule takes the available width and every item gets
 *   `(available - 2 * innerPadding - (n - 1) * itemSpace) / n`, truncated to whole pixels;
 * - every child is measured with `minWidth == maxWidth == itemWidth` and `maxHeight == capsuleHeight`
 *   and placed at `innerPadding + i * (itemWidth + itemSpace)`, centred vertically.
 *
 * The 12dp inner padding is part of the capsule's own width rather than a Compose padding modifier,
 * so the glass surface and the touch highlight the caller draws on this row span the whole capsule.
 *
 * @param modifier The modifier to be applied to the capsule.
 * @param trailingReserve The width to leave free at the capsule's end for a view measured after it —
 *   the expansion view plus its gap, or the end accessory plus its side spacing. It comes off the
 *   available width, which is the bound `COUIBottomFloatingNavigationBar.onMeasure` puts on its menu
 *   view while such a view is active.
 * @param content The items of the capsule, usually [BottomFloatingNavigationBarItem]s.
 */
@Composable
internal fun FloatingCapsuleRow(
    modifier: Modifier = Modifier,
    trailingReserve: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalDensity.current
    val innerPaddingPx = with(density) { BottomFloatingNavigationBarDefaults.ItemInnerPadding.roundToPx() }
    val itemSpacePx = with(density) { BottomFloatingNavigationBarDefaults.ItemSpacing.roundToPx() }
    val itemMaxWidthPx = with(density) { BottomFloatingNavigationBarDefaults.ItemMaxWidth.roundToPx() }
    val capsuleHeightPx = with(density) { BottomFloatingNavigationBarDefaults.BarHeight.roundToPx() }
    val reservePx = with(density) { trailingReserve.roundToPx() }

    Layout(
        // getDisplayFrame: the menu view is the bar's popup barrier.
        modifier = modifier.popupBottomBarrier(),
        content = {
            CompositionLocalProvider(LocalFloatingMenuTouchOwner provides true) { BottomFloatingCapsuleRowScope.content() }
        },
    ) { measurables, constraints ->
        val count = measurables.size
        if (count == 0) {
            return@Layout layout(0, 0) {}
        }

        // A width that is not bounded has nothing to squeeze into, so the reserve is only taken off a
        // real width and an unbounded one falls into the first branch below, at its natural size.
        val availableWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - reservePx).coerceAtLeast(0)
        } else {
            Constraints.Infinity
        }
        val totalItemMax = count * itemMaxWidthPx + 2 * innerPaddingPx + (count - 1) * itemSpacePx
        val fits = totalItemMax <= availableWidth

        val capsuleWidth = if (fits) totalItemMax else availableWidth
        val itemWidth = if (fits) {
            itemMaxWidthPx
        } else {
            ((availableWidth - 2 * innerPaddingPx - (count - 1) * itemSpacePx) / count).coerceAtLeast(0)
        }

        val placeables = measurables.map { measurable ->
            measurable.measure(
                Constraints(
                    minWidth = itemWidth,
                    maxWidth = itemWidth,
                    minHeight = 0,
                    maxHeight = capsuleHeightPx,
                ),
            )
        }

        layout(capsuleWidth, capsuleHeightPx) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(
                    x = innerPaddingPx + index * (itemWidth + itemSpacePx),
                    y = (capsuleHeightPx - placeable.height) / 2,
                )
            }
        }
    }
}

/**
 * The [RowScope] the capsule's content is composed in.
 *
 * `FloatingCapsuleRow` measures every item with an exact width, so `Modifier.weight` and
 * `Modifier.align` cannot do anything: both return the receiver unchanged. That matches the native
 * menu view, which has no weight concept and places each child at its own computed x. The
 * alignment-line entry points are inert for the same reason.
 */
internal val LocalFloatingMenuTouchOwner = staticCompositionLocalOf { false }

private object BottomFloatingCapsuleRowScope : RowScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this

    override fun Modifier.align(alignment: Alignment.Vertical): Modifier = this

    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = this

    override fun Modifier.alignByBaseline(): Modifier = this

    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}

/** The plain capsule bar: the ColorOS 17 glass surface with no expansion view. */
@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
private fun BottomFloatingNavigationBarCapsule(
    containerColor: Color,
    backdrop: Backdrop?,
    shadow: Shadow?,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val cornerRadius = BottomFloatingNavigationBarDefaults.CornerRadius
    val squircleEnabled = isSquircleEnabled()
    // A full-radius bar follows the system capsule curve; a smaller explicit radius keeps the Lp
    // squircle. See CapsuleOutlineShape.
    val shape = remember(cornerRadius, squircleEnabled) {
        CapsuleOutlineShape(cornerRadius = cornerRadius, squircleEnabled = squircleEnabled)
    }
    val highlight = rememberGlassHighlight(GlassContainer.Medium, backdrop)
    val touchState = rememberCOUITouchMotionState()
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f

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
            // `couiGlassSurface` returns its own chain instead of appending to the receiver, so the
            // shadow and the row share a wrapper rather than the row's own modifier chain.
            Box {
                if (shadow != null) {
                    // The shadow rides the press transform so it stays under the capsule it lifts.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                translationX = touchState.translation.x
                                translationY = touchState.translation.y
                                scaleX = touchState.scaleX
                                scaleY = touchState.scaleY
                            }
                            .dropShadow(shape = shape, shadow = shadow),
                    )
                }
                FloatingCapsuleRow(
                    modifier = Modifier
                        .couiTouchMotion(
                            shape = shape,
                            lightParams = SpotLightDefaults.opaqueMedium2(isDark),
                            motionState = touchState,
                            surface = Modifier.couiGlassSurface(
                                backdrop = backdrop,
                                shape = shape,
                                containerColor = containerColor,
                                fallback = Modifier.background(color = containerColor, shape = shape),
                                blurRadius = BottomFloatingNavigationBarDefaults.blurRadius(),
                                blendColors = BottomFloatingNavigationBarDefaults.blurColors(isDark),
                                highlight = highlight,
                            ),
                        )
                        .then(modifier),
                    content = content,
                )
            }
        }
    }
}

/**
 * The capsule plus an [expansion] view that melts into it.
 *
 * The capsule keeps its own measured width; the gap and the expansion view's footprint are driven
 * by [expanded] through `progress`, and the merged silhouette is one [MetaBallSurface] whose second
 * particle is exactly the expansion view's current footprint. That keeps the blob and the layout in
 * lockstep: shrinking the particle to nothing leaves the bar as a plain capsule. The blob is drawn in
 * the capsule's own box, so its particles are expressed on the capsule's centre line.
 */
@Composable
private fun BottomFloatingNavigationBarMeltBall(
    containerColor: Color,
    backdrop: Backdrop?,
    expanded: Boolean,
    expansion: @Composable BoxScope.() -> Unit,
    shadow: Shadow?,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalDensity.current
    val barHeight = BottomFloatingNavigationBarDefaults.BarHeight
    val cornerRadius = BottomFloatingNavigationBarDefaults.CornerRadius
    val squircleEnabled = isSquircleEnabled()
    val shape = remember(cornerRadius, squircleEnabled) {
        CapsuleOutlineShape(cornerRadius = cornerRadius, squircleEnabled = squircleEnabled)
    }
    val expansionSize = BottomFloatingNavigationBarDefaults.ExpansionSize
    val sideSpacing = BottomFloatingNavigationBarDefaults.SideSpacing
    val expansionTouchState = rememberCOUITouchMotionState()
    val menuTouchState = rememberCOUITouchMotionState()
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f

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
        // The blob and the row share one box, so the particles below use the capsule's own centre
        // line, and the whole group is centred in the bar's box as one.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BottomFloatingNavigationBarDefaults.MinHeightWithoutPaddingVertical)
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center,
        ) {
            val capsuleWidth = with(density) { capsuleWidthPx.toDp() }
            // `BaseBottomFloatingMenuView.updateOuterShadow` lifts the menu view with either the
            // caustic shadow (`ShadowUtils` level 7, which casts its own spot shadow) or the level 2
            // spot shadow, never both. Where the system draws the caustic below, the default shadow
            // would darken the light it casts, so only a caller's own shadow is kept.
            val systemCaustic = rememberPlatformCausticShadowRenderer() != null
            val liftShadow = shadow.takeUnless { systemCaustic && it == BottomFloatingNavigationBarDefaults.BarShadow }
            Box {
                // The native menu view and expansion view each cast their own shadow, so the capsule
                // and the ball get one apiece under the blob MetaBallSurface paints over them. Each
                // rides its particle's press transform.
                if (liftShadow != null && capsuleWidthPx > 0) {
                    Box(
                        modifier = Modifier
                            .size(capsuleWidth, barHeight)
                            .graphicsLayer {
                                translationX = menuTouchState.translation.x
                                translationY = menuTouchState.translation.y
                                scaleX = menuTouchState.scaleX
                                scaleY = menuTouchState.scaleY
                            }
                            .dropShadow(shape = shape, shadow = liftShadow),
                    )
                }
                if (liftShadow != null && capsuleWidthPx > 0 && expansionFootprint > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = capsuleWidth + gap, y = (barHeight - expansionFootprint) / 2)
                            .size(expansionFootprint)
                            .graphicsLayer {
                                alpha = progress.coerceIn(0f, 1f)
                                translationX = expansionTouchState.translation.x
                                translationY = expansionTouchState.translation.y
                                scaleX = expansionTouchState.scaleX
                                scaleY = expansionTouchState.scaleY
                            }
                            .dropShadow(shape = CircleShape, shadow = liftShadow),
                    )
                }
                MetaBallSurface(
                    particles = buildList {
                        if (capsuleWidthPx > 0) {
                            add(
                                MetaBallParticle(
                                    centerX = capsuleWidth / 2 + with(density) { menuTouchState.translation.x.toDp() },
                                    centerY = barHeight / 2 + with(density) { menuTouchState.translation.y.toDp() },
                                    width = capsuleWidth,
                                    height = barHeight,
                                    scaleX = menuTouchState.scaleX,
                                    scaleY = menuTouchState.scaleY,
                                    cornerRadius = cornerRadius,
                                ),
                            )
                        }
                        if (expansionFootprint > 0.dp) {
                            add(
                                MetaBallParticle(
                                    centerX = capsuleWidth + gap + expansionFootprint / 2 + with(density) {
                                        expansionTouchState.translation.x.toDp()
                                    },
                                    centerY = barHeight / 2 + with(density) {
                                        expansionTouchState.translation.y.toDp()
                                    },
                                    width = expansionFootprint,
                                    height = expansionFootprint,
                                    scaleX = expansionTouchState.scaleX,
                                    scaleY = expansionTouchState.scaleY,
                                    cornerRadius = expansionFootprint / 2,
                                ),
                            )
                        }
                    },
                    // The capsule wears the native TYPE_FRAMEWORK_BOTTOM_BAR material rather than a
                    // flat tint, so the backdrop's colour still shows through the glass.
                    blurColors = BottomFloatingNavigationBarDefaults.blurColors(isDark),
                    modifier = Modifier.matchParentSize(),
                    color = containerColor,
                    backdrop = backdrop,
                    blurRadius = BottomFloatingNavigationBarDefaults.blurRadius(),
                    // The merged outline gets the glass rim; without a backdrop the blob is opaque and
                    // the native component draws no stroke either. TYPE_FRAMEWORK_CAPSULE_3's near and
                    // far ramps draw a sharp upper line and a weaker lower one.
                    rimColor = if (backdrop != null) {
                        Color.White.copy(
                            alpha = if (isDark) {
                                BottomFloatingNavigationBarDefaults.RIM_ALPHA_DARK
                            } else {
                                BottomFloatingNavigationBarDefaults.RIM_ALPHA_LIGHT
                            },
                        )
                    } else {
                        null
                    },
                    rimBottomScale = 1f,
                    bottomGlowColor = BottomFloatingNavigationBarDefaults.CausticColor,
                    continuousCurve = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FloatingCapsuleRow(
                        modifier = Modifier
                            .onSizeChanged { capsuleWidthPx = it.width }
                            .couiTouchMotion(
                                shape = shape,
                                lightParams = SpotLightDefaults.opaqueMedium2(isDark),
                                motionState = menuTouchState,
                            ),
                        // COUI narrows the menu view by exactly the expansion view's footprint plus the
                        // side spacing while the expansion view is active, and hands it the full width
                        // back once it is inactive (`COUIBottomFloatingNavigationBar.onMeasure`).
                        // Deriving the reserve from the animated values keeps the capsule and the blob
                        // in step through the transition.
                        trailingReserve = gap + expansionFootprint,
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
                                .couiTouchMotion(
                                    enabled = expanded,
                                    shape = CircleShape,
                                    lightParams = SpotLightDefaults.opaqueMedium2(isDark),
                                    motionState = expansionTouchState,
                                    extraTransform = {
                                        scaleX *= progress
                                        scaleY *= progress
                                        alpha = progress
                                    },
                                ),
                        ) {
                            expansion()
                        }
                    }
                }
            }
        }
    }
}

/**
 * A [BottomFloatingNavigationBarItem] that is suitable for [BottomFloatingNavigationBar].
 *
 * The bar measures the item with an exact width, so the item's own maximum width
 * ([BottomFloatingNavigationBarDefaults.ItemMaxWidth]) is only a bound for a caller that places the
 * item outside a bar.
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
 * @param motionSpec The touch-motion parameters of the item's own [couiTouchMotion]. A nav item is a
 *   very wide, very short silhouette, so `COUITouchMotionSpec`'s area and aspect terms put its press
 *   scale at the small end (`couiPressScale`).
 * @param lightSpec The spotlight preset the item wears while pressed. COUI hands
 *   `COUIBottomFloatingToolbarItemView` no press background at all — `shouldStripPressedForContainerTouch`
 *   drops `state_pressed` — so this is the whole of the item's press feedback, alongside the label
 *   tint.
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
    selectedIcon: ImageVector? = null,
    motionSpec: COUITouchMotionSpec = COUITouchMotionSpec(),
    lightSpec: SpotLightParams = SpotLightDefaults.opaqueMedium2(
        COUITheme.colorScheme.background.luminance() < 0.5f,
    ),
) {
    val colorScheme = COUITheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

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
            .couiTouchMotion(enabled = !LocalFloatingMenuTouchOwner.current, spec = motionSpec, lightParams = lightSpec)
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
        // stays empty on purpose. The item carries no horizontal padding of its own:
        // `BaseBottomFloatingMenuView.onMeasure` spends `mInnerPadding` on the capsule's own width
        // and measures every child at its full item width, so a second inset here would squeeze the
        // icon and ellipsize the label. Measured on the device, Clock's 62.06dp item holds a 24dp
        // icon and a 62.06dp-wide label, and Gallery's 50.7dp item a 50.7dp-wide one.
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
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
                    imageVector = selectedIcon.takeIf { selected } ?: icon,
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
                // The native title is sized in pixels, so a larger system font leaves it as it is.
                fontSize = with(LocalDensity.current) { BottomFloatingNavigationBarDefaults.LabelFontSize.value.dp.toSp() },
                // A TextView pads its line out to the font's top and bottom metrics, which drops the
                // glyph about 2.8dp below the title's top edge; an unpadded line sits it 1dp below.
                lineHeight = with(LocalDensity.current) { LabelLineHeight.toSp() },
                style = COUITheme.textStyles.main.copy(lineHeightStyle = LabelLineHeightStyle),
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
 * The shared red-dot helper for the floating bottom bars, used by
 * [BottomFloatingNavigationBarItem] and [BottomFloatingToolbarItem].
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
     * The bottom padding a ColorOS host gives the bar under gesture navigation.
     *
     * COUI `coui_bottom_floating_navigation_bar_menu_to_screen_navi_distance`, and the same 8dp as
     * the Gallery's `common_bottom_gesture_height`, which `TemplateFragment` sets as the bar's
     * bottom padding when the navigation bar is a gesture handle. The handle's own inset is not
     * added on top: the bar floats over it.
     */
    val MenuToScreenNaviDistance = 8.dp

    /**
     * The bottom padding a ColorOS host gives the bar under three-button navigation: the Gallery's
     * `common_bottom_navigation_height`, which replaces the navigation inset rather than adding to it.
     */
    val ButtonNavigationDistance = 36.dp

    /**
     * The bottom padding ColorOS 17 hosts put under a floating bar, in place of the navigation
     * inset: [MenuToScreenNaviDistance] under gesture navigation, [ButtonNavigationDistance] under
     * three-button navigation and nothing without a navigation bar — the branches of the Gallery's
     * `TemplateFragment.getBottomPadding`. With it a 56dp capsule ends 24dp above the screen's
     * bottom edge under gesture navigation, as the Gallery's does.
     */
    @Composable
    fun navigationBarPadding(): Dp {
        val density = LocalDensity.current
        return when {
            WindowInsets.tappableElement.getBottom(density) > 0 -> ButtonNavigationDistance
            WindowInsets.navigationBars.getBottom(density) > 0 -> MenuToScreenNaviDistance
            else -> 0.dp
        }
    }

    /**
     * The footprint of the expansion view that melts out of the capsule.
     *
     * COUI `mFakeViewSize`, the square the expansion view's `mMainView` is bound to when the bar
     * makes room for it; it is also the bar height, so the two silhouettes share a centre line.
     */
    val ExpansionSize = 56.dp

    /** The gap kept between the capsule and the expansion view. COUI `mSideViewSpacing`. */
    val SideSpacing = 12.dp

    /**
     * The spot shadow the capsule and the expansion view each cast, COUI `SHADOW_LV_B`.
     *
     * `BaseBottomFloatingMenuView` lifts the menu view with the same
     * `ShadowUtils.setElevationToView(this, 2, ...)` call as the accessory bar, so this is the
     * measured [BottomFloatingAccessoryNavigationBarDefaults.BarShadow].
     */
    val BarShadow: Shadow = BottomFloatingAccessoryNavigationBarDefaults.BarShadow

    /**
     * The blur radius `COUIMaterialBlurEffect` hands `TYPE_FRAMEWORK_BOTTOM_BAR`, in pixels.
     *
     * `BaseBottomFloatingMenuView.updateBlurEffect` sets that type on the capsule, and the material
     * passes this raw 25 straight to `RenderEffect.createBlurEffect`, so it is a pixel radius, not a
     * dp one; see [blurRadius] for the conversion.
     */
    const val NATIVE_BLUR_RADIUS_PX = 25f

    /**
     * The tint layers of `FRAMEWORK_BOTTOM_BAR_PARAMS_LIGHT`, in the order they are applied.
     *
     * The material wraps the blur in two `createColorFilterEffect` calls, layer 1 innermost, so
     * `#B3666666` in `ColorDodge` brightens the blur first and `#99F5F5F5` in `Luminosity` then lifts
     * its lightness while keeping the backdrop's hue and saturation.
     */
    val BlurColorsLight: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0xB3666666), mode = BlurBlendMode.ColorDodge),
        BlendColorEntry(color = Color(0x99F5F5F5), mode = BlurBlendMode.Luminosity),
    )

    /**
     * The tint layers of `FRAMEWORK_BOTTOM_BAR_PARAMS_DARK`: `#80666666` in `Overlay`, then
     * `#B3262626` in `Luminosity`, which darkens the blur but leaves its colour in place.
     */
    val BlurColorsDark: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0x80666666), mode = BlurBlendMode.Overlay),
        BlendColorEntry(color = Color(0xB3262626), mode = BlurBlendMode.Luminosity),
    )

    /**
     * The alpha of the light traced along the capsule's rim on a light theme.
     *
     * `BaseBottomFloatingMenuView.updateStrokeEffect` requests `TYPE_FRAMEWORK_CAPSULE_3`, whose
     * `FRAMEWORK_CAPSULE_PARAMS_3_LIGHT` edge alpha is 0.25 at a 2.5dp line width.
     */
    const val RIM_ALPHA_LIGHT = 0.25f

    /** The rim alpha on a dark theme, `FRAMEWORK_CAPSULE_PARAMS_3_DARK`'s 0.8. */
    const val RIM_ALPHA_DARK = 0.8f

    /**
     * The caustic light cast under the capsule, COUI `coui_caustic_color_2` (`#42FFFFFF`).
     *
     * `BaseBottomFloatingMenuView.setCausticShadowEffect` hands this colour to
     * `setOutlineCausticShadowColor` with the level 4 layout, so it is the same in both themes: it
     * only reads as light on a dark page.
     */
    val CausticColor = Color(0x42FFFFFF)

    /** The tint layers of the bar's material for the current theme. */
    fun blurColors(isDark: Boolean): List<BlendColorEntry> = if (isDark) BlurColorsDark else BlurColorsLight

    /**
     * [NATIVE_BLUR_RADIUS_PX] as the dp radius `textureBlur` needs for the same Gaussian.
     *
     * `COUIMaterialBlurEffect.setViewBlurEffect` hands the 25 straight to
     * `RenderEffect.createBlurEffect(25, 25, MIRROR)`, whose radius is a Skia blur **sigma in
     * pixels** — no radius-to-sigma conversion, and no density scaling. `textureBlur` takes a dp
     * radius and derives its sigma as `0.45 * px`, so the matching dp radius is `25 / 0.45` px
     * converted to dp, i.e. a 25px sigma at any density.
     */
    @Composable
    fun blurRadius(): Float = with(LocalDensity.current) {
        (NATIVE_BLUR_RADIUS_PX / TEXTURE_BLUR_SIGMA_SCALE).toDp().value
    }

    /** The item cell height. COUI `coui_bottom_floating_navigation_bar_item_height`. */
    val ItemHeight = 48.dp

    /** The maximum width of an item cell. COUI `coui_bottom_floating_navigation_bar_item_max_width`. */
    val ItemMaxWidth = 70.dp

    /** The spacing between items. COUI `coui_bottom_floating_navigation_bar_item_space`. */
    val ItemSpacing = 8.dp

    /**
     * The padding the capsule keeps at each end, inside its own shape. COUI `mInnerPadding`, from
     * `coui_bottom_floating_navigation_bar_item_padding`.
     *
     * It is spent on the capsule's width, not on each item's content: `BaseBottomFloatingMenuView`
     * counts it once per end in `totalItemSpace` and then measures every child at the full item
     * width. `COUIBottomFloatingNavigationBarItem` adds no inset of its own.
     */
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
     * The label size.
     *
     * The item's textAppearance is `couiTextButtonM` (14sp), but
     * `COUIBottomFloatingToolbarItemView.applyNaviVerticalStyle` overrides it with
     * `setTextSize(COMPLEX_UNIT_PX, dp2px(10f))`, so the label is 10 *pixels-per-dp*: it ignores the
     * font scale. [BottomFloatingNavigationBarItem] therefore draws this value as dp. Measured on the
     * device, Gallery's two-character label is 65px wide at its 3.53x density, about 18.4dp — two
     * 10dp glyphs, where 14 would be 28dp.
     */
    val LabelFontSize = 10.sp

    /**
     * The capsule corner radius. The native code computes
     * `mRadius = (getHeight() - paddingTop - paddingBottom) / 2.0f`, i.e. a height/2 capsule.
     */
    val CornerRadius = 28.dp

    /**
     * The horizontal outset of the item's press mask on each side.
     *
     * `COUIBottomFloatingToolbarItemView.updateBackgroundMaskRect` builds the navi item's mask rect
     * as `(-mBackgroundHorizontalPadding, 0, width + mBackgroundHorizontalPadding, height)`, with
     * `mBackgroundHorizontalPadding` read from
     * `coui_bottom_floating_navigation_bar_item_bg_horizontal_padding` — the same 8dp as
     * [ItemBackgroundHorizontalPadding].
     */
    val PressMaskHorizontalOutset = 8.dp

    /**
     * The spring the item's press mask fades in and out on.
     *
     * `COUIMaskEffectDrawable` drives its press mask with
     * `DEFAULT_SPRING_RESPONSE` (0.3) / `DEFAULT_SPRING_BOUNCE` (0), and `COUIMaskRippleDrawable`
     * hands the same pair to its own state animators.
     */
    val PressMaskSpring = folmeSpring<Float>(
        damping = 1f,
        response = 0.3f,
        visibilityThreshold = 0.001f,
    )

    /**
     * The press scale the smallest surfaces spring to. COUI `PressParams.scaleRangeMin`.
     *
     * `COUIPressAnimatorUnit.initPressEngineConfig` takes the range from `mPressParams`, whose
     * provider hands out `PressParams`' defaults.
     */
    val PressScaleMin = 0.85f

    /** The press scale the largest surfaces spring to. COUI `PressParams.scaleRangeMax`. */
    val PressScaleMax = 0.97f

    /** The square side whose area is the low end of the press scale mapping. COUI `PressParams.minAreaWidthDp`. */
    val PressAreaMinWidthDp = 20f

    /** The square side whose area is the high end of the press scale mapping. COUI `PressParams.maxAreaWidthDp`. */
    val PressAreaMaxWidthDp = 150f

    /** The aspect ratio above which the second press-scale channel engages. COUI `PressParams.aspectActivationThreshold`. */
    val PressAspectActivationThreshold = 2f

    /** The aspect ratio at which the second channel reaches [PressAspectExtremeScale]. COUI `PressParams.aspectExtremeRatio`. */
    val PressAspectExtremeRatio = 15f

    /** The scale of the second channel at [PressAspectActivationThreshold]. COUI `PressParams.aspectBaseScale`. */
    val PressAspectBaseScale = 0.85f

    /** The scale of the second channel at [PressAspectExtremeRatio]. COUI `PressParams.aspectExtremeScale`. */
    val PressAspectExtremeScale = 1f

    /** The smallest scale a pressed surface may reach. COUI `PressPhysicsEngine.TARGET_SCALE_MIN`. */
    val PressScaleClampMin = 0.4f

    /** The largest scale a pressed surface may reach. COUI `PressPhysicsEngine.TARGET_SCALE_MAX_SHRINK`. */
    val PressScaleClampMax = 1f

    /**
     * The spring a pressed capsule scales down on.
     *
     * `COUIPressAnimatorUnit.initPressEngineConfig` picks the jelly springs, and switches to the
     * candy pair when the view is more than [PressAspectActivationThreshold] times wider than tall
     * (`useCandyMaterialRealtime`). A capsule with two or more items is always in that case, so the
     * candy pair is the one the bar uses: `PressParams.candyPressSpring` is bounce 0 / response 0.3.
     */
    val PressScalePressSpring = folmeSpring<Float>(
        damping = 1f,
        response = 0.3f,
        visibilityThreshold = 0.001f,
    )

    /**
     * The spring a released capsule scales back up on: `PressParams.candyHandUpSpring`, bounce 0.5 /
     * response 0.5, cut at `CommonParams.iolPressHandUpMinimumVisibleChange` (1.0E-4).
     */
    val PressScaleReleaseSpring = folmeSpring<Float>(
        damping = 0.5f,
        response = 0.5f,
        visibilityThreshold = 1.0E-4f,
    )

    /** `coui_color_press` in light themes (`couiColorPress`, `#1F000000`). */
    val PressColorLight = Color(0x1F000000)

    /** `coui_color_press` in dark themes (`couiColorPress`, `#33FFFFFF`). */
    val PressColorDark = Color(0x33FFFFFF)

    /** The press colour for a bar painted over [background]. */
    fun pressColor(background: Color): Color = if (background.luminance() < 0.5f) PressColorDark else PressColorLight

    /**
     * The scale a `width` x `height` surface springs to while pressed.
     *
     * `COUIPressAnimatorUnit.initTargetScale` asks `PressPhysicsEngine.calculateTargetScaleBreakdown`
     * for the target, handing it the [PressScaleMin] / [PressScaleMax] range, a clamp of
     * [PressScaleClampMin] / [PressScaleClampMax], and a `PressScaleTuning` built from the
     * `PressParams` defaults. `PressCalc.calculateUniformScaleBreakdown` then blends two channels:
     *
     * - the area channel maps the area from [PressAreaMinWidthDp]^2 to [PressAreaMaxWidthDp]^2 onto
     *   [PressScaleMin]..[PressScaleMax] through the `PressParams` bezier — `(0, 0, 0, 1)`, whose
     *   control points make `y = x`, so the mapping is linear;
     * - the aspect channel engages once the longer side is more than
     *   [PressAspectActivationThreshold] times the shorter one, coasts from [PressAspectBaseScale] to
     *   [PressAspectExtremeScale] between that threshold and [PressAspectExtremeRatio], and is blended
     *   into the area scale at half weight.
     *
     * The area bounds are taken in dp, the way the native tuning converts
     * `minAreaWidthDp` / `maxAreaWidthDp` with `TouchAnimatorParamProvider.dp2px` and squares them:
     * both ends carry the same density factor, so the interpolation is density independent.
     */
    fun pressScale(width: Dp, height: Dp): Float {
        val w = width.value
        val h = height.value
        if (w <= 0f || h <= 0f) return 1f
        val minArea = PressAreaMinWidthDp * PressAreaMinWidthDp
        val maxArea = PressAreaMaxWidthDp * PressAreaMaxWidthDp
        val area = (w * h).coerceIn(minArea, maxArea)
        val areaScale = PressScaleMin + (PressScaleMax - PressScaleMin) * ((area - minArea) / (maxArea - minArea))
        val aspect = maxOf(w / h, h / w)
        val scale = if (aspect <= PressAspectActivationThreshold) {
            areaScale
        } else {
            val aspectProgress = (
                (aspect - PressAspectActivationThreshold) /
                    (PressAspectExtremeRatio - PressAspectActivationThreshold)
                ).coerceIn(0f, 1f)
            val aspectScale = PressAspectBaseScale +
                (PressAspectExtremeScale - PressAspectBaseScale) * aspectProgress
            (areaScale + aspectScale) / 2f
        }
        return scale.coerceIn(PressScaleClampMin, PressScaleClampMax)
    }
}

/**
 * The height of the native title line: a 10dp `TextView` with its default font padding, measured as
 * 48px at Gallery's 3.53x density.
 */
private val LabelLineHeight = 13.6.dp

/** Spreads the padded line over the glyph's ascent and descent the way the font padding does. */
private val LabelLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Proportional,
    trim = LineHeightStyle.Trim.None,
)

/** coui-blur's radius-to-sigma factor (`BLUR_RADIUS_TO_SIGMA`, internal to that module). */
internal const val TEXTURE_BLUR_SIGMA_SCALE = 0.45f

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
