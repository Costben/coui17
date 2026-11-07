// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Whether the bottom floating navigation bars hand their blur, stroke, shadow and melt ball to the
 * platform material, when the device has it.
 *
 * Defaults to `true`: where the device exposes the system material (`setBackgroundRenderEffect`
 * plus `setCornerParams` / `setEdgeParams` / `setShadowParams`, the caustic shadow and
 * `setMetaBallChain`), the bar uses it and matches the system pixel for pixel; everywhere else it
 * keeps the Compose renderer. The platform path costs 0.7–1.5 ms more per frame on the reference
 * device (13–16 % of the frame) without losing a frame — both paths sustain the display's refresh
 * rate. Set it to `false` to keep the Compose renderer on ColorOS as well.
 *
 * The platform material is additionally gated on [io.github.suqi8.coui.kmp.blur.LocalBlurEnabled]:
 * with blur switched off — by the user or by the system's power-saving state — the native
 * `RenderEffect` would still blur the backdrop, so the bar goes back to the Compose engine instead.
 */
val LocalPlatformBottomBarMaterialEnabled = compositionLocalOf { true }

/**
 * One silhouette the platform material paints inside a melt-ball chain, in host pixels.
 *
 * The values mirror what the Compose layout already animates: the untransformed rectangle the
 * native view is measured at, the touch-motion scale, and whether the view is on screen at all.
 */
@Immutable
internal data class BottomBarMeltBallChild(
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val scaleX: Float,
    val scaleY: Float,
    val visible: Boolean,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
)

/**
 * The three siblings `COUIBottomFloatingAccessoryNavigationBar` hands to
 * `OplusMaterialUtil.setMetaBallChain`, in host coordinates.
 */
@Immutable
internal data class BottomBarMeltBallGeometry(
    val menu: BottomBarMeltBallChild,
    val start: BottomBarMeltBallChild?,
    val end: BottomBarMeltBallChild?,
    /** Whether the three views should melt into one blob right now. */
    val chainActive: Boolean,
    /** `coui_bottom_floating_accessory_navigation_bar_melt_ball_param_extension`, in pixels. */
    val extensionPx: Int,
)

internal interface BottomBarBackdropRenderer {
    /** Whether the platform stroke replaces the Compose rim highlight. */
    val drawsOwnStroke: Boolean get() = true

    /** Whether the platform outer shadow replaces the Compose drop shadow. */
    val drawsOwnShadow: Boolean get() = true

    /** Whether three sibling silhouettes can be chained into one liquid blob. */
    val supportsMeltBall: Boolean get() = false

    @Composable
    fun CapsuleMaterial(isDark: Boolean, modifier: Modifier)

    @Composable
    fun AccessoryMaterial(isDark: Boolean, modifier: Modifier) {}

    @Composable
    fun MeltBallHost(geometry: BottomBarMeltBallGeometry, isDark: Boolean, modifier: Modifier) {}
}

@Composable
internal expect fun rememberPlatformBottomBarBackdropRenderer(): BottomBarBackdropRenderer?
