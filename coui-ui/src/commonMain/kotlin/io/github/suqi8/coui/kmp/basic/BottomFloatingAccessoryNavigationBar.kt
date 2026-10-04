// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.squircle.squircleBackground
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.COUIResponsiveUtils

/**
 * A bottom floating navigation bar with an accessory view on each side
 * (`COUIBottomFloatingAccessoryNavigationBar`).
 *
 * It is [BottomFloatingNavigationBar] with two extra round views that sit at the bar's start and end
 * edges. Both are 56dp circles
 * ([BottomFloatingAccessoryNavigationBarDefaults.AccessorySize],
 * `coui_bottom_floating_navigation_bar_height`) separated from the capsule by
 * [BottomFloatingAccessoryNavigationBarDefaults.SideSpacing] (12dp,
 * `mSideViewSpacing`), and the capsule is centred in the space they leave over —
 * `layoutPositionsSmallScreen` reserves `accessorySize + sideSpacing` on each configured side before
 * it centres the menu view.
 *
 * A configured accessory always reserves its space, but only expands into view when its `*Visible`
 * flag is set. The expansion is the native `AccessorySideExpansionController`: the accessory scales up
 * from [BottomFloatingAccessoryNavigationBarDefaults.MinScale] and fades in on their own springs, and
 * slides out of the capsule from `mFakeBounds` to its laid-out position on a third.
 *
 * While either accessory is out, all three silhouettes — start accessory, capsule, end accessory —
 * are drawn through one [MetaBallSurface], the `{startAccessory, endAccessory, menuView}` chain
 * `COUIBottomFloatingAccessoryNavigationBar.setMeltBallParams` hands to
 * `COUIMaterialEffect.setMetaBallChain`. With no accessory configured the bar is a plain glass
 * capsule, exactly like [BottomFloatingNavigationBar].
 *
 * `COUIBottomFloatingAccessoryNavigationBar.changePadding` keeps its 16dp outside padding on a small
 * screen only, so the same rule applies here. Its `onLayout` picks between `layoutPositionsSmallScreen`
 * and `layoutPositionsMediumScreen` on that same tier, but the choice cannot be observed here. Only the
 * small-screen method survives decompilation, and it pins each accessory to its own outer edge and
 * centres the menu between them — which is what the bar below already does, because the capsule is
 * measured the way `BaseBottomFloatingMenuView.onMeasure` measures it: as wide as its items at
 * [BottomFloatingNavigationBarDefaults.ItemMaxWidth] while they fit, and squeezed into the width the
 * accessories leave over when they do not.
 *
 * @param modifier The modifier to be applied to the bar.
 * @param containerColor The background color of the capsule and of the merged blob.
 * @param backdrop The content blurred behind the bar, defaulting to [LocalBackdrop]. `null` keeps it
 *   opaque.
 * @param startAccessoryVisible Whether the [startAccessory] view is expanded.
 * @param endAccessoryVisible Whether the [endAccessory] view is expanded.
 * @param startAccessory The view at the bar's start edge. A `null` slot is not configured and
 *   reserves no space. The slot draws no background of its own: the merged blob already carries the
 *   accessory's circle.
 * @param endAccessory The view at the bar's end edge.
 * @param content The content of the bar, usually [BottomFloatingNavigationBarItem]s.
 */
@Composable
fun BottomFloatingAccessoryNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = COUITheme.colorScheme.bar,
    backdrop: Backdrop? = LocalBackdrop.current,
    startAccessoryVisible: Boolean = true,
    endAccessoryVisible: Boolean = true,
    startAccessory: (@Composable BoxScope.() -> Unit)? = null,
    endAccessory: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    if (startAccessory == null && endAccessory == null) {
        // No chain to melt: COUI leaves the menu view's own background in place.
        BottomFloatingAccessoryNavigationBarCapsule(
            modifier = modifier,
            containerColor = containerColor,
            backdrop = backdrop,
            content = content,
        )
        return
    }

    val density = LocalDensity.current
    val barHeight = BottomFloatingNavigationBarDefaults.BarHeight
    val accessorySize = BottomFloatingAccessoryNavigationBarDefaults.AccessorySize
    val sideSpacing = BottomFloatingAccessoryNavigationBarDefaults.SideSpacing
    val cornerRadius = BottomFloatingNavigationBarDefaults.CornerRadius
    // The rim's weight is a property of the theme, the way `rememberGlassHighlight` picks its
    // stroke: a white bloom reads much stronger over the dark bar colour than over the light one.
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f

    val startAccessoryActive = startAccessory != null && startAccessoryVisible
    val endAccessoryActive = endAccessory != null && endAccessoryVisible
    val startExpansion = rememberAccessoryExpansion(startAccessoryActive)
    val endExpansion = rememberAccessoryExpansion(endAccessoryActive)
    val startTouchState = if (startAccessory != null) rememberCOUITouchMotionState() else null
    val endTouchState = if (endAccessory != null) rememberCOUITouchMotionState() else null
    val menuTouchState = rememberCOUITouchMotionState()

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // COUIBottomFloatingAccessoryNavigationBar.changePadding: 16dp on a small screen only.
        val horizontalPadding = COUIResponsiveUtils.pick(
            windowSizeClass = COUIResponsiveUtils.windowSizeClass(maxWidth),
            compat = BottomFloatingAccessoryNavigationBarDefaults.PaddingHorizontal,
            medium = 0.dp,
            expanded = 0.dp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BottomFloatingNavigationBarDefaults.MinHeightWithoutPaddingVertical)
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.BottomCenter,
        ) {
            var barWidthPx by remember { mutableIntStateOf(0) }
            var capsuleWidthPx by remember { mutableIntStateOf(0) }
            val barWidth = with(density) { barWidthPx.toDp() }
            val capsuleWidth = with(density) { capsuleWidthPx.toDp() }
            // The melted blob carries the glass; the capsule itself is painted by MetaBallSurface.
            val reserve = if (startAccessory != null) accessorySize + sideSpacing else 0.dp
            val startTuck = (accessorySize + sideSpacing) * startExpansion.tuck
            val endTuck = (accessorySize + sideSpacing) * endExpansion.tuck

            val menuBounds = Rect(
                with(density) { (reserve + capsuleWidth / 2).toPx() } + menuTouchState.translation.x - capsuleWidthPx * menuTouchState.scaleX / 2,
                with(density) { barHeight.toPx() } / 2 + menuTouchState.translation.y - with(density) { barHeight.toPx() } * menuTouchState.scaleY / 2,
                with(density) { (reserve + capsuleWidth / 2).toPx() } + menuTouchState.translation.x + capsuleWidthPx * menuTouchState.scaleX / 2,
                with(density) { barHeight.toPx() } / 2 + menuTouchState.translation.y + with(density) { barHeight.toPx() } * menuTouchState.scaleY / 2,
            )
            fun accessoryAlpha(center: Dp, expansion: AccessoryExpansion, motion: COUITouchMotionState?): Float {
                if (capsuleWidthPx == 0) return expansion.alpha
                val centreX = with(density) { center.toPx() } + (motion?.translation?.x ?: 0f)
                val centreY = with(density) { barHeight.toPx() } / 2 + (motion?.translation?.y ?: 0f)
                val halfWidth = with(density) { accessorySize.toPx() } * expansion.scale * (motion?.scaleX ?: 1f) / 2
                val halfHeight = with(density) { accessorySize.toPx() } * expansion.scale * (motion?.scaleY ?: 1f) / 2
                return couiAccessoryOverlapAlpha(Rect(centreX - halfWidth, centreY - halfHeight, centreX + halfWidth, centreY + halfHeight), menuBounds)
            }
            val startAlpha = accessoryAlpha(accessorySize / 2 + startTuck, startExpansion, startTouchState)
            val endAlpha = accessoryAlpha(barWidth - accessorySize / 2 - endTuck, endExpansion, endTouchState)

            // The blob and the row share this box, so the particles below are expressed in the
            // capsule's own coordinates, and the whole group is lifted off the bar's bottom edge.
            Box(
                modifier = Modifier
                    .padding(bottom = BottomFloatingNavigationBarDefaults.MenuToScreenNaviDistance)
                    .onSizeChanged { barWidthPx = it.width },
            ) {
                // The device lifts the bar with COUI's `SHADOW_LV_B` spot shadow. Each view casts its
                // own — `ShadowUtils.setElevationToView(this, 2, ...)` runs both in
                // `COUIBottomFloatingAccessoryView` and in `BaseBottomFloatingMenuView` — so the
                // capsule and the two accessories each get a shadow layer of their own, under the
                // blob that MetaBallSurface paints over them.
                val barShadow = BottomFloatingAccessoryNavigationBarDefaults.BarShadow
                Box(modifier = Modifier.matchParentSize()) {
                    if (capsuleWidthPx > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = reserve, y = 0.dp)
                                .size(capsuleWidth, barHeight)
                                .graphicsLayer {
                                    translationX = menuTouchState.translation.x
                                    translationY = menuTouchState.translation.y
                                    scaleX = menuTouchState.scaleX
                                    scaleY = menuTouchState.scaleY
                                }
                                .dropShadow(
                                    shape = RoundedCornerShape(cornerRadius),
                                    shadow = barShadow,
                                ),
                        )
                    }
                    if (barWidthPx > 0 && startAccessory != null) {
                        val diameter = accessorySize * startExpansion.scale
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = accessorySize / 2 + startTuck - diameter / 2,
                                    y = (barHeight - diameter) / 2,
                                )
                                .size(diameter)
                                // The shadow rides the same touch transform as the particle the blob
                                // draws, so a dragged ball keeps its shadow under it instead of
                                // leaving it behind on the resting position.
                                .graphicsLayer {
                                    alpha = startAlpha
                                    translationX = startTouchState?.translation?.x ?: 0f
                                    translationY = startTouchState?.translation?.y ?: 0f
                                    scaleX = startTouchState?.scaleX ?: 1f
                                    scaleY = startTouchState?.scaleY ?: 1f
                                }
                                .dropShadow(shape = CircleShape, shadow = barShadow),
                        )
                    }
                    if (barWidthPx > 0 && endAccessory != null) {
                        val diameter = accessorySize * endExpansion.scale
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = barWidth - accessorySize / 2 - endTuck - diameter / 2,
                                    y = (barHeight - diameter) / 2,
                                )
                                .size(diameter)
                                .graphicsLayer {
                                    alpha = endAlpha
                                    translationX = endTouchState?.translation?.x ?: 0f
                                    translationY = endTouchState?.translation?.y ?: 0f
                                    scaleX = endTouchState?.scaleX ?: 1f
                                    scaleY = endTouchState?.scaleY ?: 1f
                                }
                                .dropShadow(shape = CircleShape, shadow = barShadow),
                        )
                    }
                }
                MetaBallSurface(
                    particles = buildList {
                        if (capsuleWidthPx > 0) {
                            add(
                                MetaBallParticle(
                                    centerX = reserve + capsuleWidth / 2 + with(density) { menuTouchState.translation.x.toDp() },
                                    centerY = barHeight / 2 + with(density) { menuTouchState.translation.y.toDp() },
                                    width = capsuleWidth,
                                    height = barHeight,
                                    scaleX = menuTouchState.scaleX,
                                    scaleY = menuTouchState.scaleY,
                                    cornerRadius = cornerRadius,
                                ),
                            )
                        }
                        if (barWidthPx > 0 && startAccessory != null) {
                            add(
                                MetaBallParticle(
                                    centerX = accessorySize / 2 + startTuck + with(density) { startTouchState?.translation?.x?.toDp() ?: 0.dp },
                                    centerY = barHeight / 2 + with(density) { startTouchState?.translation?.y?.toDp() ?: 0.dp },
                                    width = accessorySize * startExpansion.scale,
                                    height = accessorySize * startExpansion.scale,
                                    scaleX = startTouchState?.scaleX ?: 1f,
                                    scaleY = startTouchState?.scaleY ?: 1f,
                                    alpha = startAlpha,
                                    cornerRadius = accessorySize * startExpansion.scale / 2,
                                ),
                            )
                        }
                        if (barWidthPx > 0 && endAccessory != null) {
                            add(
                                MetaBallParticle(
                                    centerX = barWidth - accessorySize / 2 - endTuck + with(density) { endTouchState?.translation?.x?.toDp() ?: 0.dp },
                                    centerY = barHeight / 2 + with(density) { endTouchState?.translation?.y?.toDp() ?: 0.dp },
                                    width = accessorySize * endExpansion.scale,
                                    height = accessorySize * endExpansion.scale,
                                    scaleX = endTouchState?.scaleX ?: 1f,
                                    scaleY = endTouchState?.scaleY ?: 1f,
                                    alpha = endAlpha,
                                    cornerRadius = accessorySize * endExpansion.scale / 2,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.matchParentSize(),
                    color = containerColor,
                    backdrop = backdrop,
                    containerAlpha = BottomFloatingAccessoryNavigationBarDefaults.containerAlpha(isDark),
                    // The highlight is the same bloom the other glass surfaces get, so it is only as
                    // strong as `Highlight.GlassStroke*` would make it. Measured on Gallery's dark
                    // accessory bar the top edge lands at 89 over a 41 body, which a 0.24 white
                    // stroke reproduces; the old 0.55 drew a ring the device does not have.
                    rimColor = if (backdrop != null) {
                        Color.White.copy(alpha = if (isDark) 0.24f else 0.45f)
                    } else {
                        null
                    },
                    // Gallery's material sends a weak, directional bloom through the lower edge;
                    // keep it separate from the top rim so the capsule does not become a white ring.
                    bottomGlowColor = if (backdrop != null) {
                        Color.White.copy(alpha = if (isDark) 0.08f else 0.12f)
                    } else {
                        null
                    },
                    bottomGlowBlur = 8.dp,
                    // COUI strokes both long edges of a silhouette — `GradientStrokeStyleSpec` carries
                    // a width and an alpha per edge — so the capsule wears a lower line as well as the
                    // upper one. Measured on Gallery's accessory bar the two read the same height
                    // (both ~+40/255 over the body), which is what a weight of 1 gives; the sides stay
                    // dark because both terms are gated on the vertical axis alone.
                    rimBottomScale = 1f,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (startAccessory != null) {
                        AccessorySlot(
                            expansion = startExpansion.copy(alpha = startAlpha),
                            tuckDirection = 1f,
                            size = accessorySize,
                            sideSpacing = sideSpacing,
                            motionState = startTouchState,
                            content = startAccessory,
                        )
                        Spacer(Modifier.width(sideSpacing))
                    }
                    FloatingCapsuleRow(
                        modifier = Modifier
                            .onSizeChanged { capsuleWidthPx = it.width }
                            .couiTouchMotion(
                                shape = RoundedCornerShape(cornerRadius),
                                lightParams = SpotLightDefaults.opaqueMedium2(isDark),
                                motionState = menuTouchState,
                            ),
                        // `layoutPositionsSmallScreen` reserves the end accessory's footprint plus its
                        // side spacing out of the menu view's width, and centres the capsule in what is
                        // left over.
                        trailingReserve = if (endAccessory != null) accessorySize + sideSpacing else 0.dp,
                        content = content,
                    )
                    if (endAccessory != null) {
                        Spacer(Modifier.width(sideSpacing))
                        AccessorySlot(
                            expansion = endExpansion.copy(alpha = endAlpha),
                            tuckDirection = -1f,
                            size = accessorySize,
                            sideSpacing = sideSpacing,
                            motionState = endTouchState,
                            content = endAccessory,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One side's accessory view: a fixed [size] square whose content scales, fades and slides out of the
 * capsule exactly like the native `AccessorySideExpansionController` drives its accessory view.
 *
 * @param expansion The animated alpha / scale / tuck of this side.
 * @param tuckDirection `+1` for the start side, `-1` for the end side: the sign that points from the
 *   accessory's resting place back towards the capsule.
 */
@Composable
private fun AccessorySlot(
    expansion: AccessoryExpansion,
    tuckDirection: Float,
    size: Dp,
    sideSpacing: Dp,
    motionState: COUITouchMotionState?,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .couiTouchMotion(
                enabled = expansion.alpha > 0.01f,
                shape = CircleShape,
                lightParams = SpotLightDefaults.opaqueMedium2(
                    COUITheme.colorScheme.background.luminance() < 0.5f,
                ),
                motionState = motionState,
                extraTransform = {
                    alpha = expansion.alpha
                    scaleX *= expansion.scale
                    scaleY *= expansion.scale
                    translationX += tuckDirection * expansion.tuck * (size + sideSpacing).toPx()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (expansion.alpha > 0.01f) content()
    }
}

/** The animated alpha / scale / tuck of one accessory view. */
private data class AccessoryExpansion(
    /** The view's alpha, on the native alpha spring. */
    val alpha: Float,
    /** The view's scale, running from [BottomFloatingAccessoryNavigationBarDefaults.MinScale] to 1. */
    val scale: Float,
    /** How far the view is tucked back under the capsule; 1 when collapsed, 0 when expanded. */
    val tuck: Float,
)

/**
 * Remembers the three springs `AccessorySideExpansionController` runs for one side.
 *
 * The alpha is `HIGH_END_ALPHA_SPRING_RESPONSE` (0.35) / `EXPANSION_VIEW_ALPHA_SPRING_BOUNCE` (0);
 * the scale and the translationX share `HIGH_END_SCALE_SPRING_RESPONSE` /
 * `HIGH_END_TRANSLATION_X_SPRING_RESPONSE` (0.55) and the matching 0.35 bounce, cut at
 * `TRANSLATION_X_MIN_VISIBLE_CHANGE` (0.001f).
 */
@Composable
private fun rememberAccessoryExpansion(visible: Boolean): AccessoryExpansion {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = BottomFloatingAccessoryNavigationBarDefaults.AlphaSpring,
        label = "accessoryAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else BottomFloatingAccessoryNavigationBarDefaults.MinScale,
        animationSpec = BottomFloatingAccessoryNavigationBarDefaults.ScaleSpring,
        label = "accessoryScale",
    )
    val tuck by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = BottomFloatingAccessoryNavigationBarDefaults.TranslationXSpring,
        label = "accessoryTranslationX",
    )
    return AccessoryExpansion(alpha = alpha, scale = scale, tuck = tuck)
}

/** The plain glass capsule, used when neither accessory slot is configured. */
@Composable
private fun BottomFloatingAccessoryNavigationBarCapsule(
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
    val touchState = rememberCOUITouchMotionState()
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val horizontalPadding = COUIResponsiveUtils.pick(
            windowSizeClass = COUIResponsiveUtils.windowSizeClass(maxWidth),
            compat = BottomFloatingAccessoryNavigationBarDefaults.PaddingHorizontal,
            medium = 0.dp,
            expanded = 0.dp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BottomFloatingNavigationBarDefaults.MinHeightWithoutPaddingVertical)
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // `couiGlassSurface` returns its own chain instead of appending to the receiver, so a
            // modifier placed ahead of it is discarded. The lift therefore cannot ride on the row's
            // own modifier chain and needs a wrapper the row is measured inside.
            Box(
                modifier = Modifier
                    .padding(bottom = BottomFloatingNavigationBarDefaults.MenuToScreenNaviDistance),
            ) {
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
                                fallback = Modifier.squircleBackground(color = containerColor, cornerRadius = cornerRadius),
                                highlight = highlight,
                            ),
                        ),
                    content = content,
                )
            }
        }
    }
}

/** Native BottomFloatingOverlapHelper: fade only after 50% of the scaled view is covered. */
internal fun couiAccessoryOverlapAlpha(accessory: Rect, menu: Rect): Float {
    val area = accessory.width * accessory.height
    if (area <= 0f || menu.isEmpty) return 1f
    val overlapWidth = (minOf(accessory.right, menu.right) - maxOf(accessory.left, menu.left)).coerceAtLeast(0f)
    val overlapHeight = (minOf(accessory.bottom, menu.bottom) - maxOf(accessory.top, menu.top)).coerceAtLeast(0f)
    val ratio = (overlapWidth * overlapHeight / area).coerceIn(0f, 1f)
    return if (ratio <= 0.5f) 1f else 2f * (1f - ratio)
}

/**
 * Contains default values used by [BottomFloatingAccessoryNavigationBar].
 */
object BottomFloatingAccessoryNavigationBarDefaults {

    /**
     * The accessory view's diameter. COUI `mAccessorySize`, initialised from
     * `coui_bottom_floating_navigation_bar_height`.
     */
    val AccessorySize = 56.dp

    /** The gap between the capsule and an accessory view. COUI `mSideViewSpacing`. */
    val SideSpacing = 12.dp

    /** The outside padding on a small screen. COUI `coui_bottom_floating_navigation_bar_padding_horizontal`. */
    val PaddingHorizontal = 16.dp

    /**
     * The alpha the bar colour keeps over the blur, per theme.
     *
     * The device does not paint this bar from a colour token: `COUIBottomFloatingAccessoryView`
     * blurs it with `COUIMaterialEffect.setViewBlurEffect(..., TYPE_FRAMEWORK_BOTTOM_BAR)` and only
     * falls back to `couiColorBar` when the effect is off. The blend therefore has to be matched by
     * measurement, and the two themes do not land in the same place. Over an even page the device's
     * light capsule reads 248 against the page's 240 — `#FAFAFA` at [GlassDefaults.ContainerAlpha]
     * gives exactly that — while its dark capsule reads 39 against the page's 47, so it keeps about
     * two thirds of `#1E1E1E`. At 0.8 the dark bar came out at 34 over a page of 51 and read as a
     * black slab; at 0.35 it came out at 58 and read as a light one.
     */
    fun containerAlpha(isDark: Boolean): Float = if (isDark) 0.65f else GlassDefaults.ContainerAlpha

    /**
     * The spot shadow the capsule and each accessory cast, COUI `SHADOW_LV_B` (level 2 of
     * `ShadowUtils.setElevationToView`).
     *
     * Both `COUIBottomFloatingAccessoryView` and `BaseBottomFloatingMenuView` call
     * `ShadowUtils.setElevationToView(this, 2, support_shadow_size_level_three,
     * coui_bottom_floating_navigation_bar_menu_shadow_color)`, and that overload branches on
     * `checkOPlusViewElevationSDK()`:
     *
     * - with the OPlus sub-SDK the level 2 branch resolves `coui_shadow_elevation_level3b` (350dp),
     *   `coui_shadow_light_y_level3b` (-160dp), `coui_shadow_light_z_level3b` (10000dp) and
     *   `coui_shadow_light_r_level3b` (2000dp), whose Skia spot metrics would be a 72.54dp radius at
     *   +5.80dp — a halo wide enough to darken the page more than 50px above the bar;
     * - without it the call falls through to `setElevationToViewFromLower(view, i2, i4, i3)`, i.e.
     *   `view.setElevation(12dp)` under the `#8A000000` spot colour it was handed.
     *
     * The device picks the second reading: sampled above Gallery's bar the page stays at 47 (dark) /
     * 231 (light) right up to the capsule's top edge, and the falloff outside the bar runs about
     * 6–8dp, which a 350dp halo would not leave.
     *
     * What the device *renders* is nevertheless far weaker than the `#8A000000` it hands over. hwui
     * does not paint the spot colour at its own alpha — `SkShadowUtils` scales it by the light
     * geometry before drawing — so a plain [Shadow] carrying `0x8A` draws a halo about five times
     * deeper than the device's. Sampled on the device's light page (240) the native halo is −2 at
     * the capsule's top edge and −13 below its bottom edge, gone within ~8dp; the same `dropShadow`
     * with the handed-over colour reads −40 above and −67 below and still runs at 12dp. The radius
     * and alpha here are therefore the measured ones, not the handed-over ones.
     */
    val BarShadow: Shadow = Shadow(
        radius = 6.dp,
        color = Color.Black.copy(alpha = 0x1E / 255f),
        spread = 0.dp,
        offset = DpOffset(0.dp, 2.dp),
    )

    /** The scale the accessory view starts from. COUI `EXPANSION_VIEW_MIN_SCALE`. */
    val MinScale = 0.5f

    /**
     * The alpha spring: `HIGH_END_ALPHA_SPRING_RESPONSE` (0.35) with
     * `EXPANSION_VIEW_ALPHA_SPRING_BOUNCE` (0).
     */
    val AlphaSpring = folmeSpring<Float>(damping = 1f, response = 0.35f)

    /**
     * The scale spring: `HIGH_END_SCALE_SPRING_RESPONSE` (0.55) with
     * `HIGH_END_SCALE_SPRING_BOUNCE` (0.35).
     */
    val ScaleSpring = folmeSpring<Float>(damping = 0.65f, response = 0.55f)

    /**
     * The translationX spring: `HIGH_END_TRANSLATION_X_SPRING_RESPONSE` (0.55) with
     * `HIGH_END_TRANSLATION_X_SPRING_BOUNCE` (0.35), cut at
     * `TRANSLATION_X_MIN_VISIBLE_CHANGE` (0.001f).
     */
    val TranslationXSpring = folmeSpring(
        damping = 0.65f,
        response = 0.55f,
        visibilityThreshold = 0.001f,
    )

    /**
     * The low-end fallback springs, used by `AccessorySideExpansionController` when the device
     * reports a low animation level: alpha at `LOW_END_ALPHA_SPRING_RESPONSE` (0.2), scale at 0.5 /
     * bounce 0.3 and translationX at 0.5 / bounce 0.3.
     */
    val LowEndAlphaSpring = folmeSpring<Float>(damping = 1f, response = 0.2f)

    /** The low-end scale spring (`LOW_END_SCALE_SPRING_RESPONSE` 0.5 / bounce 0.3). */
    val LowEndScaleSpring = folmeSpring<Float>(damping = 0.7f, response = 0.5f)

    /** The low-end translationX spring (`LOW_END_TRANSLATION_X_SPRING_RESPONSE` 0.5 / bounce 0.3). */
    val LowEndTranslationXSpring = folmeSpring(
        damping = 0.7f,
        response = 0.5f,
        visibilityThreshold = 0.001f,
    )
}
