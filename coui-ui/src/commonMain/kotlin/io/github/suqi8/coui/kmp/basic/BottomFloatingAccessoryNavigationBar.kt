// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
 * centres the menu between them — which is what the bar below already does, because the native
 * component measures itself to `content + padding` and this port likewise wraps its content.
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

    val startExpansion = rememberAccessoryExpansion(startAccessory != null && startAccessoryVisible)
    val endExpansion = rememberAccessoryExpansion(endAccessory != null && endAccessoryVisible)

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
            contentAlignment = Alignment.Center,
        ) {
            var barWidthPx by remember { mutableIntStateOf(0) }
            var capsuleWidthPx by remember { mutableIntStateOf(0) }
            val barWidth = with(density) { barWidthPx.toDp() }
            val capsuleWidth = with(density) { capsuleWidthPx.toDp() }
            // The melted blob carries the glass; the capsule itself is painted by MetaBallSurface.
            val reserve = if (startAccessory != null) accessorySize + sideSpacing else 0.dp
            val startTuck = (accessorySize + sideSpacing) * startExpansion.tuck
            val endTuck = (accessorySize + sideSpacing) * endExpansion.tuck

            Box(modifier = Modifier.onSizeChanged { barWidthPx = it.width }) {
                MetaBallSurface(
                    particles = buildList {
                        if (capsuleWidthPx > 0) {
                            add(
                                MetaBallParticle(
                                    centerX = reserve + capsuleWidth / 2,
                                    centerY = barHeight / 2,
                                    width = capsuleWidth,
                                    height = barHeight,
                                    cornerRadius = cornerRadius,
                                ),
                            )
                        }
                        if (barWidthPx > 0 && startAccessory != null) {
                            add(
                                MetaBallParticle.circle(
                                    centerX = accessorySize / 2 + startTuck,
                                    centerY = barHeight / 2,
                                    diameter = accessorySize * startExpansion.scale,
                                ),
                            )
                        }
                        if (barWidthPx > 0 && endAccessory != null) {
                            add(
                                MetaBallParticle.circle(
                                    centerX = barWidth - accessorySize / 2 - endTuck,
                                    centerY = barHeight / 2,
                                    diameter = accessorySize * endExpansion.scale,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.matchParentSize(),
                    color = containerColor,
                    backdrop = backdrop,
                    rimColor = if (backdrop != null) Color.White.copy(alpha = 0.55f) else null,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (startAccessory != null) {
                        AccessorySlot(
                            expansion = startExpansion,
                            tuckDirection = 1f,
                            size = accessorySize,
                            sideSpacing = sideSpacing,
                            content = startAccessory,
                        )
                        Spacer(Modifier.width(sideSpacing))
                    }
                    Row(
                        modifier = Modifier
                            .height(barHeight)
                            .onSizeChanged { capsuleWidthPx = it.width },
                        horizontalArrangement = Arrangement.spacedBy(
                            BottomFloatingNavigationBarDefaults.ItemSpacing,
                            Alignment.CenterHorizontally,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        content = content,
                    )
                    if (endAccessory != null) {
                        Spacer(Modifier.width(sideSpacing))
                        AccessorySlot(
                            expansion = endExpansion,
                            tuckDirection = -1f,
                            size = accessorySize,
                            sideSpacing = sideSpacing,
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
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                alpha = expansion.alpha
                scaleX = expansion.scale
                scaleY = expansion.scale
                translationX = tuckDirection * expansion.tuck * (size + sideSpacing).toPx()
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
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
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .height(BottomFloatingNavigationBarDefaults.BarHeight)
                    .couiGlassSurface(
                        backdrop = backdrop,
                        shape = shape,
                        containerColor = containerColor,
                        fallback = Modifier.squircleBackground(color = containerColor, cornerRadius = cornerRadius),
                        highlight = highlight,
                    )
                    .bottomFloatingTouchHighlight(shape),
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
