// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Whether the app bar material hands its blur to the platform's own backdrop effect, when the device
 * has one.
 *
 * Defaults to `true`: the platform path matches the system pixel for pixel, and it costs nothing to
 * record. It samples the window content, though, so the effect is tied to the frame the view is
 * composited in and has two limitations inside Compose trees:
 * - While an enclosing backdrop is being recorded the view is left out of that layer
 *   ([TouchTransparentInterop]), and when the last consumer goes away and recording stops, the
 *   system drops the blur for a few frames — the bar flashes each time a popup or dialog that read
 *   the backdrop closes.
 * - Subtrees clipped by Compose's offscreen `DstIn` squircle mask (`squircleClip` /
 *   `absoluteSquircleClip`) cannot clip the platform `AndroidView`'s window-surface backdrop effect,
 *   so the view overflows outside rounded corners as a sharp dark rectangle during transitions.
 *
 * `coui-nav` and `coui-navigation3-ui` `NavDisplay` automatically signal corner-clipped hosts via
 * [io.github.suqi8.coui.kmp.squircle.LocalSquircleClippedHost] so top bars hosted inside them stay
 * on [AppBarMaterial]'s Compose engine without extra configuration. Set this local to `false` on
 * pages outside `NavDisplay` that host glass popups over a blurred bar or apply custom squircle
 * transition clips.
 *
 * The Compose engine only has something to sample when the caller provides a
 * [io.github.suqi8.coui.kmp.blur.LocalBackdrop]; without one [AppBarMaterial] paints the alpha ramp
 * alone.
 */
val LocalPlatformAppBarMaterialEnabled = compositionLocalOf { true }

/**
 * Draws the app bar material through the platform's own backdrop blur.
 *
 * ColorOS dresses `AppBarBlurOverlayView` once — a gradient background drawable plus a gradient-blur
 * backdrop RenderEffect that hwui evaluates on the RenderThread against whatever is already drawn
 * behind the view — and from then on only changes the view's alpha while the page scrolls. A
 * renderer reproduces exactly that, so nothing about the blur is re-recorded or rebuilt per frame.
 */
internal interface AppBarBackdropRenderer {
    /**
     * @param containerColor The base colour of the alpha ramp.
     * @param dark Whether to use the dark theme's two-stop ramp instead of the light three-stop one.
     * @param alpha The material's overall alpha; at `0` the view is `INVISIBLE`.
     * @param modifier The size and position of the material node.
     */
    @Composable
    fun Material(containerColor: Color, dark: Boolean, alpha: () -> Float, modifier: Modifier)
}

/** The platform backdrop renderer, or `null` where the platform has none and COUI blurs itself. */
@Composable
internal expect fun rememberPlatformAppBarBackdropRenderer(): AppBarBackdropRenderer?
